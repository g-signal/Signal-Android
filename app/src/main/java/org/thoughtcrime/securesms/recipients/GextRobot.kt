package org.thoughtcrime.securesms.recipients

import com.fasterxml.jackson.annotation.JsonProperty

data class GextRobot(
  @JsonProperty("robot") val robot: Boolean = false,
  @JsonProperty("msgButtonVisible") val msgButtonVisible: MsgButtonVisible? = null
) {
  data class MsgButtonVisible(
    @JsonProperty("text") val text: Boolean = false,
    @JsonProperty("sticker") val sticker: Boolean = false,
    @JsonProperty("camera") val camera: Boolean = false,
    @JsonProperty("microphone") val microphone: Boolean = false,
    @JsonProperty("photos") val photos: Boolean = false,
    @JsonProperty("gif") val gif: Boolean = false,
    @JsonProperty("file") val file: Boolean = false,
    @JsonProperty("contact") val contact: Boolean = false,
    @JsonProperty("location") val location: Boolean = false,
    @JsonProperty("payment") val payment: Boolean = false,
    @JsonProperty("poll") val poll: Boolean = false
  )
}
