package org.thoughtcrime.securesms.linkbaaccount

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.net.SignalNetwork
import org.whispersystems.signalservice.api.NetworkResult
import org.whispersystems.signalservice.internal.push.LinkBaLinkResultResponse
import org.whispersystems.signalservice.internal.push.LinkBaLinkedUserInfoResponse
import org.whispersystems.signalservice.internal.push.LinkBaRequestLinkResponse
import org.whispersystems.signalservice.internal.push.LinkBaStatus
import org.whispersystems.signalservice.internal.push.LinkBaUserInfoResponse

/**
 * UI-facing wrapper around [org.whispersystems.signalservice.api.linkbapay.LinkBaPayApi].
 *
 * Each method runs on [Dispatchers.IO] and returns a [Result] sealed class that the UI / ViewModel
 * layer can pattern-match on without touching libsignal types directly.
 */
object LinkBaAccountRepository {

  private const val TAG = "LinkBaAccountRepository"

  sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    /** Server responded with a non-2xx status (e.g. 401 unauthorized, 502 backend error). */
    data class HttpError(val code: Int) : Result<Nothing>()
    /** Network connectivity / IO error. */
    data class NetworkError(val cause: Throwable) : Result<Nothing>()
    /** Local/parsing error or anything else we didn't expect. */
    data class ApplicationError(val cause: Throwable) : Result<Nothing>()
  }

  /** API 1 — query the BA operator info for a scanned QR code. */
  suspend fun getBaUserInfo(linkId: String): Result<LinkBaUserInfoResponse> = withContext(Dispatchers.IO) {
    SignalNetwork.linkBaPay.getBaUserInfo(linkId).toResult("getBaUserInfo")
  }

  /** API 2 — submit the binding request as the currently signed-in user. */
  suspend fun requestLink(linkId: String, userName: String, confirmResult: Boolean): Result<LinkBaRequestLinkResponse> = withContext(Dispatchers.IO) {
    SignalNetwork.linkBaPay.requestLink(linkId, userName, confirmResult).toResult("requestLink")
  }

  /** API 3 — poll for the final binding result. Stop polling once [LinkBaLinkResultResponse.isTerminal] is true. */
  suspend fun getLinkResult(linkId: String): Result<LinkBaLinkResultResponse> = withContext(Dispatchers.IO) {
    SignalNetwork.linkBaPay.getLinkResult(linkId).toResult("getLinkResult")
  }

  /** API 4 — query whether the current account is already bound to a BA operator. */
  suspend fun getLinkedBaUserInfo(): Result<LinkBaLinkedUserInfoResponse> = withContext(Dispatchers.IO) {
    SignalNetwork.linkBaPay.getLinkedBaUserInfo().toResult("getLinkedBaUserInfo")
  }

  /** Convenience for the UI: linkStatus enum from a fetched user-info response. */
  fun statusOf(response: LinkBaUserInfoResponse): LinkBaStatus = response.status

  private fun <T> NetworkResult<T>.toResult(name: String): Result<T> = when (this) {
    is NetworkResult.Success -> Result.Success(result)
    is NetworkResult.StatusCodeError -> {
      Log.w(TAG, "$name http=${code}")
      Result.HttpError(code)
    }
    is NetworkResult.NetworkError -> {
      Log.w(TAG, "$name network error", exception)
      Result.NetworkError(exception)
    }
    is NetworkResult.ApplicationError -> {
      Log.w(TAG, "$name app error", throwable)
      Result.ApplicationError(throwable)
    }
  }
}
