package de.gianttree.discord.w2g

import de.gianttree.discord.w2g.api.CreateRoom
import de.gianttree.discord.w2g.api.UpdatePlaylist
import de.gianttree.discord.w2g.database.setupDatabaseConnection
import de.gianttree.discord.w2g.logging.W2GFormatter
import de.gianttree.discord.w2g.monitoring.GuildMemberCount
import de.gianttree.discord.w2g.monitoring.RoomCounter
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
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger
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

internal const val MESSAGE_CACHE_SIZE = 100

// Set to 50 in accordance with the W2G.TV API: https://community.w2g.tv/t/faq-how-can-i-access-the-watch2gether-api/149410
internal const val URLS_PER_UPDATE = 50
internal const val SUPPORT_GUILD = 854032399145762856

private val debugGuild = Snowflake(SUPPORT_GUILD)


@ExperimentalTime
@ExperimentalSerializationApi
suspend fun main() {

    val guildMembers = mutableMapOf<Snowflake, GuildMemberCount>()
    val config = readConfig()

    logger.level = when (config.debugMode) {
        true -> Level.ALL
        false -> Level.INFO
    }

    if (config.debugMode) {
        logger.info("Debug mode enabled")
        logger.finer("Config: $config")
    }

    val client = Kord(config.discordToken) {
        cache {
            messages(lruCache(MESSAGE_CACHE_SIZE))
        }
    }

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    val database = setupDatabaseConnection(config, logger)

    val context = Context(logger, config, guildMembers, RoomCounter.load(), client, httpClient, database)

    launchMonitoringServer(context)

    registerEvents(context)

    client.login {
        intents = Intents {
            enableEvent<GuildCreateEvent>()
            enableEvent<MessageCreateEvent>()
            enableEvent<ReactionAddEvent>()

        }
        if (!context.config.debugMode) {
            presence {
                watching("your 📺 reactions!")
            }
        }
    }
}

private fun registerEvents(
    context: Context,
) {
    context.client.on<ReadyEvent>(consumer = ReadyEvent::sendReadyMessage)

    context.client.on<MessageCreateEvent>(consumer = MessageCreateEvent::sendHelp)

    context.client.on<ReactionAddEvent> {
        handleTvReaction(context)
    }

    context.client.on<GuildCreateEvent> {
        this.logGuildCreate()
        this.addGuild(context)
    }

    context.client.on<GuildDeleteEvent> {
        this.logGuildDelete()
        this.removeGuild(context)
    }

    context.client.on<ReadyEvent> {
        this.kord.launch {
            while (this.isActive && !context.config.debugMode) {
                delay(context.config.intervals.presenceInterval)
                context.client.updatePresence(context)
                context.roomCounter.save()
            }
        }
        this.kord.launch {
            while (this.isActive) {
                delay(context.config.intervals.guildMemberUpdateInterval)
                context.guildMembers.minWithOrNull(compareBy { it.value.lastUpdate })?.let {
                    logger.finest("Updating guild ${it.key}")
                    context.client.getGuildPreviewOrNull(it.key)?.let { guild ->
                        logger.finest("Guild ${guild.name} (${guild.id}) has ${guild.approximateMemberCount} members")
                        context.guildMembers[guild.id] =
                            GuildMemberCount(Clock.System.now(), guild.approximateMemberCount)
                    }
                }
            }
        }
    }
}

private fun GuildDeleteEvent.removeGuild(context: Context) {
    context.guildMembers.remove(this.guildId)
}

private fun GuildCreateEvent.addGuild(context: Context) {
    context.guildMembers[this.guild.id] = GuildMemberCount(Clock.System.now(), this.guild.memberCount ?: 0)
}

private suspend fun ReactionAddEvent.handleTvReaction(context: Context) {
    if (context.config.debugMode && this.guildId != debugGuild) {
        return
    }

    if (this.emoji != TV_REACTION || this.userId == this.kord.selfId) {
        return
    }

    val message = this.getMessage()
    val matches = urlRegex.findAll(message.content).toMutableList()

    if (matches.isNotEmpty()) {

        val url = matches.removeFirst().value
        val response = CreateRoom.call(context.httpClient, context.config, url)

        matches.map { it.value }.windowed(URLS_PER_UPDATE, URLS_PER_UPDATE, partialWindows = true).forEach {
            UpdatePlaylist.call(
                context.httpClient,
                response.streamKey,
                UpdatePlaylist.Request(context.config.w2gToken, it)
            )
        }

        message.reply {
            content =
                "${this@handleTvReaction.user.mention} Room created! " +
                        "Watch here: <https://w2g.tv/rooms/${response.streamKey}>!" +
                        if (context.config.debugMode) " (debug)" else ""
            allowedMentions {
                repliedUser = false
                add(AllowedMentionType.UserMentions)
            }
        }

        message.addReaction(TV_REACTION)

        context.roomCounter.addRoom()

        logger.info(
            "Room ${response.streamKey} created for guild " +
                    "${this.guildId?.toString()} (${this.getGuild()?.name}) and user ${this.user.mention}"
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
                "${this.kord.selfId}&scope=bot&permissions=${
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
        self.mention, "<@!${self.id}>" -> {
            this.message.reply {
                content = HELP_TEXT
            }
        }
    }
}

private fun GuildCreateEvent.logGuildCreate() {
    logger.info(
        "Guild became available: ${this.guild.name} " +
                "(${this.guild.id}, ${this.guild.memberCount ?: 0} members)"
    )
}

private fun GuildDeleteEvent.logGuildDelete() {
    logger.info(
        "Guild became unavailable: ${this.guild?.name}" +
                " (${this.guildId}, unavailable: ${this.unavailable})"
    )
}


private suspend fun Kord.updatePresence(context: Context) {
    this.editPresence {
        val guildCount = context.guildMembers.count()
        this.watching("together on $guildCount guilds! 📺")
    }
}

