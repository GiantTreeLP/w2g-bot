package de.gianttree.discord.w2g

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.enableEvent
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on
import dev.kord.gateway.Intents
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.io.File

const val CLIENT_ID = "795600891859304458"
const val W2G_API_URL = "https://w2g.tv/rooms/create.json"

private val json = Json {
    encodeDefaults = true
    prettyPrint = true
}

private val urlRegex =
    "^(?:(?:(?:https?|ftp):)?//)(?:\\S+(?::\\S*)?@)?(?:(?!(?:10|127)(?:\\.\\d{1,3}){3})(?!(?:169\\.254|192\\.168)(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z0-9\\u00a1-\\uffff][a-z0-9\\u00a1-\\uffff_-]{0,62})?[a-z0-9\\u00a1-\\uffff]\\.)+(?:[a-z\\u00a1-\\uffff]{2,}\\.?))(?::\\d{2,5})?(?:[/?#]\\S*)?$".toRegex(
        RegexOption.IGNORE_CASE
    )

@KtorExperimentalAPI
suspend fun main() {
    val config = readConfig()
    val client = Kord(config.discordToken) {
        intents = Intents {
            enableEvent<GuildCreateEvent>()
            enableEvent<MessageCreateEvent>()
            enableEvent<ReactionAddEvent>()
        }
    }

    client.on<ReadyEvent> {
        println(
            "Invite this bot to your guild: https://discord.com/api/oauth2/authorize?client_id=$CLIENT_ID&scope=bot&permissions=${
                Permissions(
                    Permission.ViewChannel,
                    Permission.SendMessages,
                    Permission.ReadMessageHistory,
                ).code.value
            }"
        )
    }

    client.on<MessageCreateEvent> {
        val self = client.getSelf()
        when (this.message.content) {
            self.mention, "<@!${self.id.asString}>" -> {
                this.message.reply {
                    //language=Markdown
                    content = """__**📺 Usage:**__
    
1. Send a message containing at least one link/url.
2. React to that message using the 📺 emote.

I will then answer with a link to your private w2g.tv room.""".trimIndent()
                }
            }
        }
    }

    client.on<ReactionAddEvent> {
        if (this.emoji.name != "📺") {
            return@on
        }

        val message = this.getMessage()
        val match = urlRegex.find(message.content)
        if (match != null) {
            val url = match.value

            val httpClient = HttpClient(CIO) {
                install(JsonFeature) {
                    this.serializer = KotlinxSerializer(json)
                }
            }
            val answer = httpClient.post<WatchTogetherResponse>(W2G_API_URL) {
                contentType(ContentType.Application.Json)
                body = WatchTogetherRequest(config.w2gToken, url)
            }

            message.reply {
                content =
                    "${message.author?.mention ?: ""} Room created! Watch here: <https://w2g.tv/rooms/${answer.streamKey}>!"
            }
        }
    }

    client.login {
        watching("your 📺 reactions!")
    }
}


private fun readConfig(): Config {
    val configFile = File("config.json")

    if (configFile.exists() && configFile.isFile) {
        val config = json.decodeFromString<Config>(configFile.readText())

        configFile.writeText(json.encodeToString(config))

        return config
    } else {
        val config = Config()

        if (configFile.exists()) {
            configFile.delete()
        }
        configFile.createNewFile()

        configFile.writeText(
            json.encodeToString(config)
        )

        return config
    }
}

@Serializable
data class Config(
    val discordToken: String = "YOUR_DISCORD_TOKEN_HERE",
    val w2gToken: String = "YOUR_W2G_TOKEN_HERE"
)

@Serializable
data class WatchTogetherRequest(
    @SerialName("w2g_api_key") val apiToken: String,
    @SerialName("share") val url: String,
    @SerialName("bg_color") val backgroundColor: String = "#000000",
    @SerialName("bg_opacity") val backgroundOpacity: String = "100"
)

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
    @SerialName("moderated_user") val moderatedUser: Boolean
)
