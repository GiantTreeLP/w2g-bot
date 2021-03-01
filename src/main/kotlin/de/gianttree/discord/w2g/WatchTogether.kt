package de.gianttree.discord.w2g

import de.gianttree.discord.w2g.api.WatchTogetherRequest
import de.gianttree.discord.w2g.api.WatchTogetherResponse
import de.gianttree.discord.w2g.logging.W2GFormatter
import dev.kord.common.entity.AllowedMentionType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.enableEvent
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildDeleteEvent
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*
import java.util.logging.*

const val W2G_API_URL = "https://w2g.tv/rooms/create.json"

private val json = Json {
    encodeDefaults = true
    prettyPrint = true
    ignoreUnknownKeys = true
}

internal val urlRegex =
    "(?:(?:(?:https?|ftp):)?//)(?:\\S+(?::\\S*)?@)?(?:(?!(?:10|127)(?:\\.\\d{1,3}){3})(?!(?:169\\.254|192\\.168)(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z0-9\\u00a1-\\uffff][a-z0-9\\u00a1-\\uffff_-]{0,62})?[a-z0-9\\u00a1-\\uffff]\\.)+(?:[a-z\\u00a1-\\uffff]{2,}\\.?))(?::\\d{2,5})?(?:[/?#]\\S*)?".toRegex(
        RegexOption.IGNORE_CASE
    )

private val logger = Logger.getLogger("w2g").apply {
    this.useParentHandlers = false
    this.addHandler(ConsoleHandler().apply {
        this.formatter = W2GFormatter()
    })
}

@FlowPreview
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

    val httpClient = HttpClient(CIO) {
        install(JsonFeature) {
            this.serializer = KotlinxSerializer(json)
        }
    }

    client.on<ReadyEvent> {
        logger.info(
            "Invite this bot to your guild: https://discord.com/api/oauth2/authorize?client_id=${client.selfId.asString}&scope=bot&permissions=${
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

            val answer = httpClient.post<WatchTogetherResponse>(W2G_API_URL) {
                contentType(ContentType.Application.Json)
                body = WatchTogetherRequest(config.w2gToken, url)
            }

            message.reply {
                content =
                    "${this@on.user.mention} Room created! Watch here: <https://w2g.tv/rooms/${answer.streamKey}>!"
                allowedMentions {
                    repliedUser = false
                    add(AllowedMentionType.UserMentions)
                }
            }

            logger.info("Room ${answer.streamKey} created for guild ${this.guildId?.asString}")

        }
    }

    client.on<GuildCreateEvent> {
        logger.info("Guild became available: ${this.guild.name}")
    }

    client.on<GuildDeleteEvent> {
        logger.info("Guild became unavailable: ${this.guild?.name} (${this.guildId.asString}, unavailable: ${this.unavailable})")
    }

    client.events
        .filter { it is GuildCreateEvent || it is GuildDeleteEvent }
        .debounce(1000)
        .onEach { client.updatePresence() }
        .launchIn(client)

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

private suspend fun Kord.updatePresence() {
    this.editPresence {
        this.watching("together on ${this@updatePresence.guilds.count()} guilds! 📺")
    }
}

@Serializable
data class Config(
    val discordToken: String = "YOUR_DISCORD_TOKEN_HERE",
    val w2gToken: String = "YOUR_W2G_TOKEN_HERE"
)
