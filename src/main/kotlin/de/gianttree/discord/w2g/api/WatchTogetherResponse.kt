package de.gianttree.discord.w2g.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchTogetherResponse(
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
