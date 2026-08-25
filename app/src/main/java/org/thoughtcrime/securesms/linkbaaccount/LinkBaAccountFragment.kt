package org.thoughtcrime.securesms.linkbaaccount

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.BiometricDeviceAuthentication
import org.thoughtcrime.securesms.BiometricDeviceLockContract
import org.thoughtcrime.securesms.DevicePinAuthEducationSheet
import org.thoughtcrime.securesms.R
import org.signal.core.ui.R as CoreUiR
import org.thoughtcrime.securesms.compose.ComposeFragment
import org.thoughtcrime.securesms.permissions.Permissions
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import org.whispersystems.signalservice.internal.push.LinkBaLinkedUserInfoResponse

class LinkBaAccountFragment : ComposeFragment() {

  companion object {
    private val TAG = Log.tag(LinkBaAccountFragment::class.java)
  }

  private var linkedInfo by mutableStateOf<LinkBaLinkedUserInfoResponse?>(null)
  private var isLoading by mutableStateOf(false)
  private var hasLoaded by mutableStateOf(false)

  private lateinit var biometricAuth: BiometricDeviceAuthentication
  private lateinit var biometricDeviceLockLauncher: ActivityResultLauncher<String>

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    biometricDeviceLockLauncher = registerForActivityResult(BiometricDeviceLockContract()) { result: Int ->
      if (result == BiometricDeviceAuthentication.AUTHENTICATED) {
        onAuthenticatedForScan()
      }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setAllowedAuthenticators(BiometricDeviceAuthentication.ALLOWED_AUTHENTICATORS)
      .setTitle(getString(R.string.unlock_link_ba_platform_scan_qr_code))
      .setConfirmationRequired(true)
      .build()
    biometricAuth = BiometricDeviceAuthentication(
      BiometricManager.from(requireActivity()),
      BiometricPrompt(requireActivity(), BiometricAuthenticationListener()),
      promptInfo
    )
  }

  override fun onResume() {
    super.onResume()
    fetchLinkedBaUserInfo()
  }

  override fun onPause() {
    super.onPause()
    biometricAuth.cancelAuthentication()
  }

  private fun fetchLinkedBaUserInfo() {
    isLoading = true
    lifecycleScope.launch {
      try {
        when (val result = LinkBaAccountRepository.getLinkedBaUserInfo()) {
          is LinkBaAccountRepository.Result.Success -> {
            val info = result.data
            Log.i(
              TAG,
              "getLinkedBaUserInfo success: isLinked=${info.isLinked}, " +
                "baxsAppUserId=${info.baxsAppUserId}, " +
                "linkbaxsOptId=${info.linkbaxsOptId}, " +
                "linkbaxsMemberId=${info.linkbaxsMemberId}, " +
                "linkbaxsOptName=${info.linkbaxsOptName}, " +
                "linkbaxsOptEmail=${info.linkbaxsOptEmail}, " +
                "linkbaxsOptMobile=${info.linkbaxsOptMobile}, " +
                "linkbaxsDate=${info.linkbaxsDate}"
            )
            linkedInfo = info
          }
          is LinkBaAccountRepository.Result.HttpError -> {
            Log.w(TAG, "getLinkedBaUserInfo http error: code=${result.code}")
            linkedInfo = null
          }
          is LinkBaAccountRepository.Result.NetworkError -> {
            Log.w(TAG, "getLinkedBaUserInfo network error", result.cause)
            linkedInfo = null
          }
          is LinkBaAccountRepository.Result.ApplicationError -> {
            Log.w(TAG, "getLinkedBaUserInfo app error", result.cause)
            linkedInfo = null
          }
        }
      } finally {
        isLoading = false
        hasLoaded = true
      }
    }
  }

  private fun onScanQrClicked() {
    if (biometricAuth.shouldShowEducationSheet(requireContext())) {
      DevicePinAuthEducationSheet.show(getString(R.string.LinkDeviceFragment__before_linking), parentFragmentManager)
      parentFragmentManager.setFragmentResultListener(DevicePinAuthEducationSheet.REQUEST_KEY, viewLifecycleOwner) { _, _ ->
        runBiometricOrCredentialAuth()
      }
    } else {
      runBiometricOrCredentialAuth()
    }
  }

  private fun runBiometricOrCredentialAuth() {
    val authStarted = biometricAuth.authenticate(requireContext(), true) {
      biometricDeviceLockLauncher.launch(getString(R.string.unlock_link_ba_platform_scan_qr_code))
    }
    if (!authStarted) {
      onAuthenticatedForScan()
    }
  }

  private fun onAuthenticatedForScan() {
    requestCameraPermissionAndScan()
  }

  private fun requestCameraPermissionAndScan() {
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
      .onAllGranted { launchQrScanner() }
      .onAnyDenied {
        Toast.makeText(requireContext(), R.string.CameraXFragment_signal_needs_camera_access_scan_qr_code, Toast.LENGTH_LONG).show()
      }
      .execute()
  }

  private fun launchQrScanner() {
    findNavController().safeNavigate(R.id.action_linkBaAccountFragment_to_linkBaQrScanFragment)
  }

  @SuppressLint("MissingSuperCall")
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
  }

  @Composable
  override fun FragmentContent() {
    Scaffolds.Settings(
      title = stringResource(id = R.string.link_ba_platform_account_title),
      onNavigationClick = {
        if (!findNavController().popBackStack()) {
          requireActivity().finishAfterTransition()
        }
      },
      navigationIcon = ImageVector.vectorResource(id = CoreUiR.drawable.symbol_arrow_start_24),
      navigationContentDescription = stringResource(id = R.string.Material3SearchToolbar__close)
    ) { contentPadding: PaddingValues ->
      val info = linkedInfo
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(contentPadding)
      ) {
        when {
          !hasLoaded || isLoading -> {
            CircularProgressIndicator(
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier
                .size(36.dp)
                .align(Alignment.Center)
            )
          }
          info == null || !info.isLinked -> UnlinkedContent(onScanClick = ::onScanQrClicked)
          else -> LinkedContent(info = info)
        }
      }
    }
  }

  private inner class BiometricAuthenticationListener : BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationError(errorCode: Int, errorString: CharSequence) {
      Log.w(TAG, "Authentication error: $errorCode")
      if (errorCode == BiometricPrompt.ERROR_CANCELED) {
        biometricDeviceLockLauncher.launch(getString(R.string.unlock_link_ba_platform_scan_qr_code))
      }
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
      Log.i(TAG, "Authentication succeeded")
      onAuthenticatedForScan()
    }

    override fun onAuthenticationFailed() {
      Log.w(TAG, "Unable to authenticate")
    }
  }
}

@Composable
private fun UnlinkedContent(onScanClick: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 16.dp)
  ) {
    Text(
      text = stringResource(id = R.string.link_ba_platform_unlinked_section_title),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 12.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
      onClick = onScanClick
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Icon(
          painter = painterResource(id = R.drawable.symbol_bapay_24),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(24.dp)
        )

        Text(
          text = stringResource(id = R.string.link_ba_platform_scan_qr_code),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f)
        )

        Icon(
          imageVector = ImageVector.vectorResource(id = R.drawable.symbol_chevron_right_24),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Text(
      text = stringResource(id = R.string.link_ba_platform_scan_footer),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp)
    )
  }
}

@Composable
private fun LinkedContent(info: LinkBaLinkedUserInfoResponse) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 16.dp)
  ) {
    Text(
      text = stringResource(id = R.string.link_ba_platform_linked_section_title),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 12.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      )
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        LinkedInfoRow(
          label = stringResource(id = R.string.link_ba_platform_user_id),
          value = info.linkbaxsOptId.orEmpty()
        )
        LinkedInfoDivider()
        LinkedInfoRow(
          label = stringResource(id = R.string.link_ba_platform_user_name),
          value = info.linkbaxsOptName.orEmpty()
        )
        LinkedInfoDivider()
        LinkedInfoRow(
          label = stringResource(id = R.string.link_ba_platform_operator_email),
          value = info.linkbaxsOptEmail.orEmpty()
        )
        LinkedInfoDivider()
        LinkedInfoRow(
          label = stringResource(id = R.string.link_ba_platform_operator_mobile),
          value = info.linkbaxsOptMobile.orEmpty()
        )
      }
    }
  }
}

@Composable
private fun LinkedInfoDivider() {
  HorizontalDivider(
    thickness = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant,
    modifier = Modifier.padding(horizontal = 16.dp)
  )
}

@Composable
private fun LinkedInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.End,
      modifier = Modifier.weight(1f)
    )
  }
}
