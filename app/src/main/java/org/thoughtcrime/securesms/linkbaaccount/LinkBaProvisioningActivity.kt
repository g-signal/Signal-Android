package org.thoughtcrime.securesms.linkbaaccount

import android.os.Bundle
import android.view.Window
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.PassphraseRequiredActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.settings.app.AppSettingsActivity

/**
 * Entry point activity for the `baxs://linkba?linkId=...` deeplink.
 *
 * Mirrors [org.thoughtcrime.securesms.DeviceProvisioningActivity]: shows a confirmation dialog,
 * and on Continue forwards the user to [LinkBaAccountFragment] inside the settings flow. The
 * actual `linkId` from the deeplink is intentionally ignored — once the user lands on the BA
 * settings page they must scan the QR code with the in-app scanner.
 */
class LinkBaProvisioningActivity : PassphraseRequiredActivity() {

  companion object {
    private val TAG = Log.tag(LinkBaProvisioningActivity::class.java)
  }

  override fun onPreCreate() {
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
  }

  override fun onCreate(bundle: Bundle?, ready: Boolean) {
    Log.i(TAG, "Received link-ba deeplink: ${intent?.data}")
    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.link_ba_platform_confirm_title)
      .setMessage(R.string.photo_capture_link_ba_qr_code_found_message)
      .setPositiveButton(R.string.MediaCaptureFragment_device_link_dialog_continue) { _, _ ->
        startActivity(AppSettingsActivity.linkBaAccount(this))
        finish()
      }
      .setNegativeButton(android.R.string.cancel) { dialog, _ ->
        dialog.dismiss()
        finish()
      }
      .setOnDismissListener { finish() }
      .create()
      .show()
  }
}
