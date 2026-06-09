package org.thoughtcrime.securesms.linkbaaccount

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.compose.ComposeFragment
import org.thoughtcrime.securesms.permissions.Permissions
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.util.VibrateUtil
import org.whispersystems.signalservice.internal.push.LinkBaStatus

class LinkBaQrScanFragment : ComposeFragment() {

  companion object {
    private val TAG = Log.tag(LinkBaQrScanFragment::class.java)
    private const val VIBRATE_DURATION_MS = 50L
    private const val QR_SCHEME = "baxs"
    private const val QR_HOST = "linkba"
    private const val QR_PARAM_LINK_ID = "linkId"
    private const val POLL_INTERVAL_MS = 3000L
    private const val POLL_TIMEOUT_MS = 60_000L
  }

  private var showFrontCamera by mutableStateOf(false)
  private var lastScannedData: String? = null
  private var dialog by mutableStateOf<LinkBaScanDialog>(LinkBaScanDialog.None)
  private var linkingJob: Job? = null

  @OptIn(ExperimentalPermissionsApi::class)
  @Composable
  override fun FragmentContent() {
    val cameraPermissionState: PermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    Scaffolds.Settings(
      title = "",
      onNavigationClick = { findNavController().popBackStack() },
      navigationIcon = ImageVector.vectorResource(id = R.drawable.ic_x),
      navigationContentDescription = stringResource(id = R.string.Material3SearchToolbar__close),
      actions = {
        IconButton(onClick = { showFrontCamera = !showFrontCamera }) {
          Icon(painterResource(id = R.drawable.symbol_switch_24), contentDescription = null)
        }
      }
    ) { contentPadding: PaddingValues ->
      LinkBaQrScanScreen(
        hasPermission = cameraPermissionState.status.isGranted,
        onRequestPermissions = { askCameraPermission() },
        showFrontCamera = showFrontCamera,
        scanningEnabled = isScanningEnabled(dialog),
        onQrCodeScanned = { data -> onQrCodeScanned(data) },
        dialog = dialog,
        onDialogRetry = ::dismissDialogAndAllowRescan,
        onDialogDismiss = ::dismissDialogAndAllowRescan,
        onDialogConfirm = ::onConfirmLink,
        onTimeoutScanAgain = ::dismissDialogAndAllowRescan,
        modifier = Modifier.padding(contentPadding)
      )
    }
  }

  private fun isScanningEnabled(dialog: LinkBaScanDialog): Boolean {
    return dialog == LinkBaScanDialog.None
  }

  private fun dismissDialogAndAllowRescan() {
    cancelLinkingJob()
    dialog = LinkBaScanDialog.None
    lastScannedData = null
  }

  private fun cancelLinkingJob() {
    linkingJob?.cancel()
    linkingJob = null
  }

  override fun onDestroyView() {
    super.onDestroyView()
    cancelLinkingJob()
  }

  private fun onConfirmLink(linkId: String) {
    if (linkId.isBlank()) {
      Log.w(TAG, "onConfirmLink: linkId is blank, ignoring")
      dialog = LinkBaScanDialog.None
      return
    }
    Log.i(TAG, "Confirm link clicked: linkId=$linkId")
    dialog = LinkBaScanDialog.Linking
    val userName = currentSelfDisplayName()
    linkingJob = lifecycleScope.launch {
      runRequestLinkAndPoll(linkId, userName)
    }
  }

  private fun currentSelfDisplayName(): String {
    return runCatching {
      val profile = Recipient.self().profileName
      listOf(profile.givenName, profile.familyName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
    }.getOrElse {
      Log.w(TAG, "currentSelfDisplayName failed", it)
      ""
    }
  }

  private suspend fun runRequestLinkAndPoll(linkId: String, userName: String) {
    val defaultFailReason = getString(R.string.link_ba_platform_failed_default_reason)
    Log.i(TAG, "requestLink: linkId=$linkId, userName=$userName")
    when (val result = LinkBaAccountRepository.requestLink(linkId, userName, confirmResult = true)) {
      is LinkBaAccountRepository.Result.Success -> {
        val info = result.data
        Log.i(TAG, "requestLink success: linkStatus=${info.linkStatus} (${info.linkStatusName})")
      }
      is LinkBaAccountRepository.Result.HttpError -> {
        Log.w(TAG, "requestLink http error: code=${result.code}")
        dialog = LinkBaScanDialog.Failed(reason = defaultFailReason)
        return
      }
      is LinkBaAccountRepository.Result.NetworkError -> {
        Log.w(TAG, "requestLink network error", result.cause)
        dialog = LinkBaScanDialog.Failed(reason = defaultFailReason)
        return
      }
      is LinkBaAccountRepository.Result.ApplicationError -> {
        Log.w(TAG, "requestLink app error", result.cause)
        dialog = LinkBaScanDialog.Failed(reason = defaultFailReason)
        return
      }
    }

    val terminal: TerminalResult? = withTimeoutOrNull(POLL_TIMEOUT_MS) {
      pollLinkResult(linkId)
    }

    when (terminal) {
      null -> {
        Log.w(TAG, "Polling timed out locally after ${POLL_TIMEOUT_MS}ms")
        dialog = LinkBaScanDialog.Timeout
      }
      is TerminalResult.Linked -> {
        Log.i(TAG, "Polling reached LINKED, popping back to LinkBaAccountFragment")
        dialog = LinkBaScanDialog.None
        if (isAdded) {
          findNavController().popBackStack()
        }
      }
      is TerminalResult.Failed -> {
        Log.w(TAG, "Polling reached terminal status=${terminal.status}, reason=${terminal.reason}")
        dialog = LinkBaScanDialog.Failed(
          reason = terminal.reason.ifBlank { defaultFailReason }
        )
      }
    }
    linkingJob = null
  }

  private sealed interface TerminalResult {
    data object Linked : TerminalResult
    data class Failed(val status: LinkBaStatus, val reason: String) : TerminalResult
  }

  private suspend fun pollLinkResult(linkId: String): TerminalResult {
    while (true) {
      delay(POLL_INTERVAL_MS)
      when (val result = LinkBaAccountRepository.getLinkResult(linkId)) {
        is LinkBaAccountRepository.Result.Success -> {
          val info = result.data
          Log.i(TAG, "getLinkResult: linkStatus=${info.linkStatus} (${info.linkStatusName}), failReason=${info.failReason}")
          when (info.status) {
            LinkBaStatus.LINKED -> return TerminalResult.Linked
            LinkBaStatus.FAILED, LinkBaStatus.TIMEOUT -> {
              return TerminalResult.Failed(info.status, info.failReason.orEmpty())
            }
            else -> Unit
          }
        }
        is LinkBaAccountRepository.Result.HttpError -> Log.w(TAG, "getLinkResult http error: code=${result.code}")
        is LinkBaAccountRepository.Result.NetworkError -> Log.w(TAG, "getLinkResult network error", result.cause)
        is LinkBaAccountRepository.Result.ApplicationError -> Log.w(TAG, "getLinkResult app error", result.cause)
      }
    }
  }

  private fun onQrCodeScanned(data: String) {
    if (dialog != LinkBaScanDialog.None) return
    if (data == lastScannedData) return
    lastScannedData = data
    if (VibrateUtil.isHapticFeedbackEnabled(requireContext())) {
      VibrateUtil.vibrate(requireContext(), VIBRATE_DURATION_MS.toInt())
    }
    Log.i(TAG, "BA QR scanned: $data")

    val linkId = parseLinkId(data)
    if (linkId.isNullOrBlank()) {
      Log.w(TAG, "QR code is not a valid BAXS link: $data")
      dialog = LinkBaScanDialog.InvalidQr
      return
    }

    fetchBaUserInfo(linkId)
  }

  private fun parseLinkId(data: String): String? {
    return try {
      val uri = Uri.parse(data)
      if (!QR_SCHEME.equals(uri.scheme, ignoreCase = true)) return null
      if (!QR_HOST.equals(uri.host, ignoreCase = true)) return null
      uri.getQueryParameter(QR_PARAM_LINK_ID)?.takeIf { it.isNotBlank() }
    } catch (t: Throwable) {
      Log.w(TAG, "Failed to parse QR data", t)
      null
    }
  }

  private fun fetchBaUserInfo(linkId: String) {
    lifecycleScope.launch {
      when (val result = LinkBaAccountRepository.getBaUserInfo(linkId)) {
        is LinkBaAccountRepository.Result.Success -> {
          val info = result.data
          Log.i(
            TAG,
            "getBaUserInfo success: linkId=${info.linkId}, " +
              "optId=${info.optId}, memberId=${info.memberId}, " +
              "optName=${info.optName}, email=${info.email}, mobile=${info.mobile}, " +
              "linkStatus=${info.linkStatus} (${info.linkStatusName}), " +
              "canRequestLink=${info.canRequestLink}, failReason=${info.failReason}, " +
              "expireTime=${info.expireTime}"
          )
          if (info.canRequestLink) {
            val resolvedLinkId = info.linkId.takeIf { it.isNotBlank() } ?: linkId
            dialog = LinkBaScanDialog.ConfirmLink(optName = info.optName.orEmpty(), linkId = resolvedLinkId)
          } else {
            dialog = LinkBaScanDialog.CannotLink(reason = info.failReason.orEmpty())
          }
        }
        is LinkBaAccountRepository.Result.HttpError -> Log.w(TAG, "getBaUserInfo http error: code=${result.code}")
        is LinkBaAccountRepository.Result.NetworkError -> Log.w(TAG, "getBaUserInfo network error", result.cause)
        is LinkBaAccountRepository.Result.ApplicationError -> Log.w(TAG, "getBaUserInfo app error", result.cause)
      }
    }
  }

  private fun askCameraPermission() {
    Permissions.with(this)
      .request(Manifest.permission.CAMERA)
      .ifNecessary()
      .withPermanentDenialDialog(
        getString(R.string.CameraXFragment_signal_needs_camera_access_scan_qr_code),
        null,
        R.string.CameraXFragment_allow_access_camera,
        R.string.CameraXFragment_to_scan_qr_codes,
        parentFragmentManager
      )
      .onAnyDenied {
        Toast.makeText(requireContext(), R.string.CameraXFragment_signal_needs_camera_access_scan_qr_code, Toast.LENGTH_LONG).show()
      }
      .execute()
  }

  @SuppressLint("MissingSuperCall")
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
  }
}
