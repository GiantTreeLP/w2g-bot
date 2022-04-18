package de.gianttree.discord.w2g.api

import de.gianttree.discord.w2g.Config
import de.gianttree.discord.w2g.W2G_API_URL
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object CreateRoom {
    @Serializable
    data class Request(
        @SerialName("w2g_api_key") val apiToken: String,
        @SerialName("share") val url: String,
        @SerialName("bg_color") val backgroundColor: String = "#000000",
        @SerialName("bg_opacity") val backgroundOpacity: String = "100"
    )

    @Serializable
    data class Response(
        val id: Int,
        @SerialName("streamkey") val streamKey: String,
        @SerialName("created_at") val createdAt: String,
        val persistent: Boolean,
        @SerialName("persistent_name") val persistentName: String?,
        val deleted: Boolean,
        val moderated: Boolean,
        val location: String,
        @SerialName("stream_created") val streamCreated: Boolean,
        val background: String?,
        @SerialName("moderated_background") val moderatedBackground: Boolean,
        @SerialName("moderated_playlist") val moderatedPlaylist: Boolean,
        @SerialName("bg_color") val backgroundColor: String,
        @SerialName("bg_opacity") val backgroundOpacity: Double,
        @SerialName("moderated_item") val moderatedItem: Boolean,
        @SerialName("theme_bg") val themeBackground: String?,
        @SerialName("playlist_id") val playlistId: Int,
        @SerialName("members_only") val membersOnly: Boolean,
        @SerialName("moderated_suggestions") val moderatedSuggestions: Boolean,
        @SerialName("moderated_chat") val moderatedChat: Boolean,
        @SerialName("moderated_user") val moderatedUser: Boolean,
        @SerialName("moderated_cam") val moderatedCam: Boolean
    )

    suspend fun call(httpClient: HttpClient, config: Config, url: String): Response {
        return httpClient.post(W2G_API_URL) {
            contentType(ContentType.Application.Json)
            setBody(Request(config.w2gToken, url))
        }.body()
    }
}
