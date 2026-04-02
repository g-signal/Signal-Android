package org.whispersystems.signalservice.api.groups

import org.signal.libsignal.protocol.logging.Log
import org.whispersystems.signalservice.api.NetworkResult
import org.whispersystems.signalservice.internal.push.GExtGroupProfileResponse
import org.whispersystems.signalservice.internal.push.PushServiceSocket

/**
 * Network API for server-side group extension (GExt) endpoints.
 *
 * GET /v1/gext/group/profile/{groupId} — fetch the tag profile for a group.
 */
class GExtGroupApi(private val pushServiceSocket: PushServiceSocket) {

  companion object {
    private val TAG = "GExtGroupApi"
  }

  /**
   * Fetch the GExt tag profile for a group.
   *
   * GET /v1/gext/group/profile/{groupId}
   * - 200: Success, returns [GExtGroupProfileResponse] containing the list of tags
   * - 401: Unauthorized
   * - 404: Group not found
   */
  fun getGroupProfile(groupId: String): NetworkResult<GExtGroupProfileResponse> {
    Log.i(TAG, "Request groupId: $groupId")
    val result = NetworkResult.fromFetch {
      pushServiceSocket.getGExtGroupProfile(groupId)
    }

    when (result) {
      is NetworkResult.Success -> {
        Log.i(TAG, "Response Success: tags count=${result.result.extTags.size}")
        result.result.extTags.forEachIndexed { index, tag ->
        //Log.i(TAG, "Tag[$index]: tagId=${tag.tagId}, tagType=${tag.tagType}, text=${tag.text}, imgBase64=${tag.imgBase64?.take(20)}..., cssBackgroundColor=${tag.cssBackgroundColor}, cssColor=${tag.cssColor}")
        }
      }
      is NetworkResult.StatusCodeError -> Log.w(TAG, "Response Error: code=${result.code}")
      is NetworkResult.NetworkError -> Log.w(TAG, "Network Error: ${result.exception.message}")
      is NetworkResult.ApplicationError -> Log.w(TAG, "Application Error: ${result.throwable.message}")
    }

    return result
  }
}
