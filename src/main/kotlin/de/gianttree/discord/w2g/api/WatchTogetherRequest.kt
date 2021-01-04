package de.gianttree.discord.w2g.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchTogetherRequest(
    @SerialName("w2g_api_key") val apiToken: String,
    @SerialName("share") val url: String,
    @SerialName("bg_color") val backgroundColor: String = "#000000",
    @SerialName("bg_opacity") val backgroundOpacity: String = "100"
)
