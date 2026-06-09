package org.whispersystems.signalservice.api.linkbapay

import org.signal.libsignal.protocol.logging.Log
import org.whispersystems.signalservice.api.NetworkResult
import org.whispersystems.signalservice.internal.push.LinkBaLinkResultResponse
import org.whispersystems.signalservice.internal.push.LinkBaLinkedUserInfoResponse
import org.whispersystems.signalservice.internal.push.LinkBaRequestLinkResponse
import org.whispersystems.signalservice.internal.push.LinkBaUserInfoResponse
import org.whispersystems.signalservice.internal.push.PushServiceSocket

/**
 * Network API for the BAXS account-link endpoints under /v1/gext/linkbapay.
 *
 * Flow:
 *  1. Scan QR code → [getBaUserInfo] to fetch the operator info and check whether linking is allowed.
 *  2. User confirms → [requestLink] to submit the binding request.
 *  3. Poll [getLinkResult] until a terminal status (LINKED / FAILED / TIMEOUT).
 *  4. On entry to the BA settings screen, call [getLinkedBaUserInfo] to display the currently bound operator.
 */
class LinkBaPayApi(private val pushServiceSocket: PushServiceSocket) {

  companion object {
    private const val TAG = "LinkBaPayApi"
  }

  /** POST /v1/gext/linkbapay/link/getBaUserInfo */
  fun getBaUserInfo(linkId: String): NetworkResult<LinkBaUserInfoResponse> {
    Log.i(TAG, "getBaUserInfo linkId=$linkId")
    return NetworkResult.fromFetch { pushServiceSocket.getLinkBaUserInfo(linkId) }.also { logResult("getBaUserInfo", it) }
  }

  /** POST /v1/gext/linkbapay/link/requestLink */
  fun requestLink(linkId: String, userName: String, confirmResult: Boolean): NetworkResult<LinkBaRequestLinkResponse> {
    Log.i(TAG, "requestLink linkId=$linkId confirm=$confirmResult")
    return NetworkResult.fromFetch { pushServiceSocket.requestLinkBa(linkId, userName, confirmResult) }.also { logResult("requestLink", it) }
  }

  /** POST /v1/gext/linkbapay/link/getLinkResult */
  fun getLinkResult(linkId: String): NetworkResult<LinkBaLinkResultResponse> {
    return NetworkResult.fromFetch { pushServiceSocket.getLinkBaResult(linkId) }.also { logResult("getLinkResult", it) }
  }

  /** POST /v1/gext/linkbapay/link/getLinkedBaUserInfo */
  fun getLinkedBaUserInfo(): NetworkResult<LinkBaLinkedUserInfoResponse> {
    return NetworkResult.fromFetch { pushServiceSocket.getLinkedBaUserInfo() }.also { logResult("getLinkedBaUserInfo", it) }
  }

  private fun <T> logResult(name: String, result: NetworkResult<T>) {
    when (result) {
      is NetworkResult.Success -> Log.i(TAG, "$name success")
      is NetworkResult.StatusCodeError -> Log.w(TAG, "$name http=${result.code}")
      is NetworkResult.NetworkError -> Log.w(TAG, "$name network error: ${result.exception.message}")
      is NetworkResult.ApplicationError -> Log.w(TAG, "$name app error: ${result.throwable.message}")
    }
  }
}
