package de.gianttree.discord.w2g

import de.gianttree.discord.w2g.monitoring.GuildMemberCount
import de.gianttree.discord.w2g.monitoring.RoomCounter
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import io.ktor.client.*
import org.jetbrains.exposed.sql.Database
import java.util.logging.Logger

data class Context(
    val logger: Logger,
    val config: Config,
    val guildMembers: MutableMap<Snowflake, GuildMemberCount>,
    val roomCounter: RoomCounter,
    val client: Kord,
    val httpClient: HttpClient,
    val database: Database,
)
