package org.whispersystems.signalservice.internal.push

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response for GET /v1/gext/group/profile/{groupId}
 */
class GExtGroupProfileResponse {

  @JsonProperty("gextTags")
  val extTags: List<GExtGroupTag> = emptyList()

  class GExtGroupTag {
    @JsonProperty("tagId")
    val tagId: String = ""

    @JsonProperty("tagType")
    val tagType: Int = 0

    @JsonProperty("text")
    val text: String? = null

    @JsonProperty("imgBase64")
    val imgBase64: String? = null

    @JsonProperty("cssBackgroundColor")
    val cssBackgroundColor: String? = null

    @JsonProperty("cssColor")
    val cssColor: String? = null

    @JsonProperty("cssOpacity")
    val cssOpacity: Float = 1.0f

    @JsonProperty("cssBorderWidth")
    val cssBorderWidth: Int = 0

    @JsonProperty("cssBorderRadius")
    val cssBorderRadius: Int = 0

    @JsonProperty("cssBorderColor")
    val cssBorderColor: String? = null

    /**
     * CSS border style from server. Possible values: solid, dashed, dotted, double, groove, ridge,
     * inset, outset, none, hidden. Each platform may not support all values — callers must map this
     * to a platform-specific style and ignore unsupported values.
     */
    @JsonProperty("cssBorderStyle")
    val cssBorderStyle: String? = null
  }
}
