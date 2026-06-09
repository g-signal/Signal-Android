package org.thoughtcrime.securesms.linkbaaccount

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.signal.core.ui.compose.Dialogs
import org.signal.core.util.logging.Log
import org.signal.qr.QrScannerView
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.mediasend.camerax.CameraXModelBlocklist
import org.thoughtcrime.securesms.qr.QrScanScreens
import java.util.concurrent.TimeUnit

private const val TAG = "LinkBaQrScanScreen"

sealed interface LinkBaScanDialog {
  data object None : LinkBaScanDialog
  data object InvalidQr : LinkBaScanDialog
  data class CannotLink(val reason: String) : LinkBaScanDialog
  data class ConfirmLink(val optName: String, val linkId: String) : LinkBaScanDialog
  data object Linking : LinkBaScanDialog
  data object Timeout : LinkBaScanDialog
  data class Failed(val reason: String) : LinkBaScanDialog
  data object ServerError : LinkBaScanDialog
}

@Composable
fun LinkBaQrScanScreen(
  hasPermission: Boolean,
  onRequestPermissions: () -> Unit,
  showFrontCamera: Boolean,
  scanningEnabled: Boolean,
  onQrCodeScanned: (String) -> Unit,
  dialog: LinkBaScanDialog = LinkBaScanDialog.None,
  onDialogRetry: () -> Unit = {},
  onDialogDismiss: () -> Unit = {},
  onDialogConfirm: (linkId: String) -> Unit = {},
  onDialogDecline: (linkId: String) -> Unit = {},
  onTimeoutScanAgain: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  val scanningEnabledState = rememberUpdatedState(scanningEnabled)
  val onQrCodeScannedState = rememberUpdatedState(onQrCodeScanned)

  when (dialog) {
    LinkBaScanDialog.None -> Unit
    LinkBaScanDialog.InvalidQr -> {
      Dialogs.SimpleAlertDialog(
        title = stringResource(id = R.string.link_ba_platform_account_scan_fail_title),
        body = stringResource(id = R.string.link_ba_platform_invalid_qr_message),
        confirm = stringResource(id = R.string.AddLinkDeviceFragment__retry),
        onConfirm = onDialogRetry,
        dismiss = stringResource(id = android.R.string.cancel),
        onDismiss = onDialogDismiss
      )
    }
    is LinkBaScanDialog.CannotLink -> {
      Dialogs.SimpleAlertDialog(
        title = stringResource(id = R.string.link_ba_platform_account_scan_fail_title),
        body = dialog.reason,
        confirm = stringResource(id = R.string.AddLinkDeviceFragment__retry),
        onConfirm = onDialogRetry,
        dismiss = stringResource(id = android.R.string.cancel),
        onDismiss = onDialogDismiss
      )
    }
    is LinkBaScanDialog.ConfirmLink -> {
      val confirm = dialog
      Dialogs.SimpleAlertDialog(
        title = stringResource(id = R.string.link_ba_platform_confirm_title),
        body = stringResource(id = R.string.link_ba_platform_confirm_message_format, confirm.optName),
        confirm = stringResource(id = R.string.link_ba_platform_confirm_bt_next),
        onConfirm = { onDialogConfirm(confirm.linkId) },
        dismiss = stringResource(id = R.string.link_ba_platform_confirm_bt_cancel),
        onDismiss = onDialogDismiss,
        onDeny = { onDialogDecline(confirm.linkId) }
      )
    }
    LinkBaScanDialog.Linking -> {
      Dialogs.IndeterminateProgressDialog(
        message = "",
        dismiss = stringResource(id = R.string.link_ba_platform_confirm_bt_cancel),
        onDismiss = onDialogDismiss
      )
    }
    LinkBaScanDialog.Timeout -> {
      Dialogs.SimpleAlertDialog(
        title = stringResource(id = R.string.link_ba_platform_failed_title),
        body = stringResource(id = R.string.link_ba_platform_timeout_message),
        confirm = stringResource(id = R.string.link_ba_platform_scan_again),
        onConfirm = onTimeoutScanAgain,
        dismiss = stringResource(id = R.string.link_ba_platform_confirm_bt_cancel),
        onDismiss = onDialogDismiss
      )
    }
    is LinkBaScanDialog.Failed -> {
      Dialogs.SimpleAlertDialog(
        title = stringResource(id = R.string.link_ba_platform_failed_title),
        body = dialog.reason,
        confirm = stringResource(id = R.string.AddLinkDeviceFragment__retry),
        onConfirm = onDialogRetry,
        dismiss = stringResource(id = android.R.string.cancel),
        onDismiss = onDialogDismiss
      )
    }
    LinkBaScanDialog.ServerError -> {
      Dialogs.SimpleAlertDialog(
        title = stringResource(id = R.string.link_ba_platform_error_title),
        body = stringResource(id = R.string.link_ba_platform_server_fail),
        confirm = stringResource(id = R.string.link_ba_platform_scan_again),
        onConfirm = onDialogRetry,
        dismiss = stringResource(id = R.string.link_ba_platform_confirm_bt_cancel),
        onDismiss = onDialogDismiss
      )
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .fillMaxHeight()
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f, true)
    ) {
      QrScanScreens.QrScanScreen(
        factory = { factoryContext ->
          Log.i(TAG, "Creating QrScannerView")
          val view = QrScannerView(factoryContext)
          view.qrData
            .throttleFirst(3000, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
              { data ->
                val enabled = scanningEnabledState.value
                Log.i(TAG, "qrData onNext, len=${data.length}, scanningEnabled=$enabled")
                if (enabled) {
                  try {
                    onQrCodeScannedState.value(data)
                  } catch (t: Throwable) {
                    Log.w(TAG, "onQrCodeScanned threw", t)
                  }
                }
              },
              { t -> Log.w(TAG, "qrData error", t) }
            )
          view
        },
        update = { view: QrScannerView ->
          view.start(lifecycleOwner = lifecycleOwner, forceLegacy = CameraXModelBlocklist.isBlocklisted())
          if (showFrontCamera) {
            view.toggleCamera()
          }
        },
        hasPermission = hasPermission,
        onRequestPermissions = onRequestPermissions,
        qrHeaderLabelString = stringResource(R.string.link_ba_platform_scan_footer)
      )
    }
  }
}
