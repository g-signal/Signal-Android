package org.thoughtcrime.securesms.recipients

import com.fasterxml.jackson.annotation.JsonProperty

data class GextTag(
  @JsonProperty("tagId") val tagId: String = "",
  @JsonProperty("tagType") val tagType: Int = 0,
  @JsonProperty("text") val text: String? = null,
  @JsonProperty("imgBase64") val imgBase64: String? = null,
  @JsonProperty("cssBackgroundColor") val cssBackgroundColor: String? = null,
  @JsonProperty("cssColor") val cssColor: String? = null,
  @JsonProperty("cssOpacity") val cssOpacity: Float = 1.0f,
  @JsonProperty("cssBorderWidth") val cssBorderWidth: Int = 0,
  @JsonProperty("cssBorderRadius") val cssBorderRadius: Int = 0,
  @JsonProperty("cssBorderColor") val cssBorderColor: String? = null,
  @JsonProperty("cssBorderStyle") val cssBorderStyle: String? = null
)
