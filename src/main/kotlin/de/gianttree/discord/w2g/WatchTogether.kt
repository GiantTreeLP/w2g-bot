package de.gianttree.discord.w2g

import de.gianttree.discord.w2g.api.CreateRoom
import de.gianttree.discord.w2g.api.UpdatePlaylist
import de.gianttree.discord.w2g.logging.W2GFormatter
import de.gianttree.discord.w2g.monitoring.launchMonitoringServer
import dev.kord.common.entity.AllowedMentionType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.enableEvent
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on
import dev.kord.gateway.Intents
import dev.kord.rest.builder.message.create.allowedMentions
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

const val W2G_API_URL = "https://w2g.tv/rooms/create.json"

//language=Markdown
const val HELP_TEXT = """
__**📺 Usage:**__

1. Send a message containing at least one link/url.
2. React to that message using the 📺 emote.

I will then answer with a link to your private w2g.tv room.

If you want to share suggestions for improvement or have any issues with the bot, join the support guild
(<https://discord.com/invite/aNYCTeEDNp>).  

"""

private val json = Json {
    encodeDefaults = true
    prettyPrint = true
    ignoreUnknownKeys = true
}

@Suppress("RegExpUnnecessaryNonCapturingGroup")
internal val urlRegex =
    ("""(?:(?:(?:https?|ftp):)?//)(?:\S+(?::\S*)?@)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})""" +
            """(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})""" +
            """(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}""" +
            """(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|""" +
            """(?:(?:[a-z0-9\u00a1-\uffff][a-z0-9\u00a1-\uffff_-]{0,62})?[a-z0-9\u00a1-\uffff]\.)+""" +
            """(?:[a-z\u00a1-\uffff]{2,}\.?))(?::\d{2,5})?(?:[/?#]\S*)?""")
        .toRegex(
            RegexOption.IGNORE_CASE
        )

internal val TV_REACTION = ReactionEmoji.Unicode("📺")

private val logger = Logger.getLogger("w2g").apply {
    this.useParentHandlers = false
    this.level = Level.ALL
    this.addHandler(ConsoleHandler().apply {
        this.formatter = W2GFormatter()
        this.level = Level.ALL
    })
}

internal const val GUILD_UPDATE_DELAY_MINUTES = 5
internal const val MESSAGE_CACHE_SIZE = 100
internal const val URLS_PER_UPDATE = 50
internal const val SUPPORT_GUILD = 854032399145762856

private val debugGuild = Snowflake(SUPPORT_GUILD)


@ExperimentalTime
@ExperimentalSerializationApi
suspend fun main() {
    val config = readConfig()
    val client = Kord(config.discordToken) {
        cache {
            messages(lruCache(MESSAGE_CACHE_SIZE))
        }
    }

    val httpClient = HttpClient(CIO) {
        install(JsonFeature) {
            this.serializer = KotlinxSerializer(json)
        }
    }

    launchMonitoringServer(config, client)

    client.on<ReadyEvent>(consumer = ReadyEvent::sendReadyMessage)

    client.on<MessageCreateEvent>(consumer = MessageCreateEvent::sendHelp)

    client.on<ReactionAddEvent> {
        handleTvReaction(httpClient, config)
    }

    client.on<GuildCreateEvent>(consumer = GuildCreateEvent::logGuildCreate)

    client.on<GuildDeleteEvent>(consumer = GuildDeleteEvent::logGuildDelete)

    client.on<ReadyEvent> {
        launch {
            while (this.isActive && !config.debugMode) {
                val guildUpdateDelay = Duration.minutes(GUILD_UPDATE_DELAY_MINUTES)
                delay(guildUpdateDelay)
                client.updatePresence()
            }
        }
    }

    client.login {
        intents = Intents {
            enableEvent<GuildCreateEvent>()
            enableEvent<MessageCreateEvent>()
            enableEvent<ReactionAddEvent>()
        }
        if (!config.debugMode) {
            presence {
                watching("your 📺 reactions!")
            }
        }
    }
}

private suspend fun ReactionAddEvent.handleTvReaction(
    httpClient: HttpClient,
    config: Config
) {
    if (config.debugMode && this.guildId != debugGuild) {
        return
    }

    if (this.emoji != TV_REACTION || this.userId == this.kord.selfId) {
        return
    }

    val message = this.getMessage()
    val matches = urlRegex.findAll(message.content).toMutableList()

    if (matches.isNotEmpty()) {

        val url = matches.removeFirst().value
        val response = CreateRoom.call(httpClient, config, url)

        matches.map { it.value }.windowed(URLS_PER_UPDATE, URLS_PER_UPDATE, partialWindows = true).forEach {
            UpdatePlaylist.call(
                httpClient,
                response.streamKey,
                UpdatePlaylist.Request(config.w2gToken, it)
            )
        }

        message.reply {
            content =
                "${this@handleTvReaction.user.mention} Room created! " +
                        "Watch here: <https://w2g.tv/rooms/${response.streamKey}>!" +
                        if (config.debugMode) " (debug)" else ""
            allowedMentions {
                repliedUser = false
                add(AllowedMentionType.UserMentions)
            }
        }

        message.addReaction(TV_REACTION)

        logger.info(
            "Room ${response.streamKey} created for guild " +
                    "${this.guildId?.asString} (${this.getGuild()?.name}) and user ${this.user.mention}"
        )
    } else {
        message.reply {
            content = "${this@handleTvReaction.user.mention} I could not find a url in the message you reacted to!"
            allowedMentions {
                repliedUser = false
                add(AllowedMentionType.UserMentions)
            }
        }
        logger.info("No room created to message '${message.content}'!")
    }
}

private fun ReadyEvent.sendReadyMessage() {
    logger.info(
        "Invite this bot to your guild: https://discord.com/api/oauth2/authorize?client_id=" +
                "${this.kord.selfId.asString}&scope=bot&permissions=${
                    Permissions(
                        Permission.ViewChannel,
                        Permission.SendMessages,
                        Permission.ReadMessageHistory,
                    ).code.value
                }"
    )
}

private suspend fun MessageCreateEvent.sendHelp() {
    val self = this.kord.getSelf()
    when (this.message.content) {
        self.mention, "<@!${self.id.asString}>" -> {
            this.message.reply {
                content = HELP_TEXT
            }
        }
    }
}

private fun GuildCreateEvent.logGuildCreate() {
    logger.info(
        "Guild became available: ${this.guild.name} " +
                "(${this.guild.id.asString}, ${this.guild.memberCount ?: 0} members)"
    )
}

private fun GuildDeleteEvent.logGuildDelete() {
    logger.info(
        "Guild became unavailable: ${this.guild?.name}" +
                " (${this.guildId.asString}, unavailable: ${this.unavailable})"
    )
}


@ExperimentalSerializationApi
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
        val guildCount = this@updatePresence.guilds.count()
        this.watching("together on $guildCount guilds! 📺")
    }
}

