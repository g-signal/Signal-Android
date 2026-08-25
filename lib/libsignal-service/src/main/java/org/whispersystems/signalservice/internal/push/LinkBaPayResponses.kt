package org.whispersystems.signalservice.internal.push

import com.fasterxml.jackson.annotation.JsonProperty

/** Status codes for the BAXS link flow as defined in /v1/gext/linkbapay docs. */
enum class LinkBaStatus(val code: Int) {
  PENDING_SCAN(1),
  SCANNED(2),
  LINKED(3),
  FAILED(4),
  TIMEOUT(5),
  REQUESTED(6),
  UNKNOWN(-1);

  companion object {
    @JvmStatic
    fun fromCode(code: Int?): LinkBaStatus = values().firstOrNull { it.code == code } ?: UNKNOWN
  }
}

/** Request body for POST /v1/gext/linkbapay/link/getBaUserInfo and /link/getLinkResult. */
class LinkBaLinkIdRequest(
  @JsonProperty("linkId") val linkId: String
)

/** Request body for POST /v1/gext/linkbapay/link/requestLink. */
class LinkBaRequestLinkRequest(
  @JsonProperty("linkId") val linkId: String,
  @JsonProperty("userName") val userName: String,
  @JsonProperty("confirmResult") val confirmResult: Boolean
)

/** Response for POST /v1/gext/linkbapay/link/getBaUserInfo. */
class LinkBaUserInfoResponse {
  @JsonProperty("linkId") val linkId: String = ""
  @JsonProperty("optId") val optId: String? = null
  @JsonProperty("memberId") val memberId: String? = null
  @JsonProperty("optName") val optName: String? = null
  @JsonProperty("email") val email: String? = null
  @JsonProperty("mobile") val mobile: String? = null
  @JsonProperty("linkStatus") val linkStatus: Int = 0
  @JsonProperty("linkStatusName") val linkStatusName: String? = null
  @JsonProperty("canRequestLink") val canRequestLink: Boolean = false
  @JsonProperty("failReason") val failReason: String? = null
  @JsonProperty("expireTime") val expireTime: String? = null

  val status: LinkBaStatus get() = LinkBaStatus.fromCode(linkStatus)
}

/** Response for POST /v1/gext/linkbapay/link/requestLink. */
class LinkBaRequestLinkResponse {
  @JsonProperty("linkId") val linkId: String = ""
  @JsonProperty("optId") val optId: String? = null
  @JsonProperty("memberId") val memberId: String? = null
  @JsonProperty("baxsAppUserId") val baxsAppUserId: String? = null
  @JsonProperty("linkStatus") val linkStatus: Int = 0
  @JsonProperty("linkStatusName") val linkStatusName: String? = null
  @JsonProperty("expireTime") val expireTime: String? = null

  val status: LinkBaStatus get() = LinkBaStatus.fromCode(linkStatus)
}

/** Response for POST /v1/gext/linkbapay/link/getLinkResult. */
class LinkBaLinkResultResponse {
  @JsonProperty("linkId") val linkId: String = ""
  @JsonProperty("optId") val optId: String? = null
  @JsonProperty("memberId") val memberId: String? = null
  @JsonProperty("baxsAppUserId") val baxsAppUserId: String? = null
  @JsonProperty("linkStatus") val linkStatus: Int = 0
  @JsonProperty("linkStatusName") val linkStatusName: String? = null
  @JsonProperty("confirmTime") val confirmTime: String? = null
  @JsonProperty("failReason") val failReason: String? = null

  val status: LinkBaStatus get() = LinkBaStatus.fromCode(linkStatus)
  val isTerminal: Boolean get() = status == LinkBaStatus.LINKED || status == LinkBaStatus.FAILED || status == LinkBaStatus.TIMEOUT
}

/** Response for POST /v1/gext/linkbapay/link/getLinkedBaUserInfo. */
class LinkBaLinkedUserInfoResponse {
  @JsonProperty("baxsAppUserId") val baxsAppUserId: String? = null
  @JsonProperty("linkbaxsOptId") val linkbaxsOptId: String? = null
  @JsonProperty("linkbaxsMemberId") val linkbaxsMemberId: String? = null
  @JsonProperty("linkbaxsOptName") val linkbaxsOptName: String? = null
  @JsonProperty("linkbaxsOptEmail") val linkbaxsOptEmail: String? = null
  @JsonProperty("linkbaxsOptMobile") val linkbaxsOptMobile: String? = null
  @JsonProperty("linkbaxsDate") val linkbaxsDate: String? = null

  val isLinked: Boolean get() = !linkbaxsOptId.isNullOrBlank()
}
