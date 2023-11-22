package de.gianttree.discord.w2g.database

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.min
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import dev.kord.core.entity.Guild as KordGuild


const val MAX_GUILD_NAME_LENGTH = 100

object Guilds : SnowflakeIdTable() {
    val name = varchar("name", MAX_GUILD_NAME_LENGTH).index()
    val approxMemberCount = integer("approx_member_count")
    val lastUpdate = long("last_update").clientDefault { Clock.System.now().toEpochMilliseconds() }.index()
    val active = bool("active").default(true).index()

    fun getMemberCountSum(): Int {
        // SUM(approx_member_count)
        val memberCountSum = approxMemberCount.sum()
        // SELECT SUM(approx_member_count) FROM guilds WHERE active = true
        return Guilds.slice(memberCountSum).select { active eq true }.single()[memberCountSum] ?: 0
    }

    fun getGuildLeastRecentUpdate(): Guild? {
        // MIN(last_update)
        val minGroup = lastUpdate.min()

        // SELECT MIN(last_update) FROM guilds WHERE active = true
        val minSelect = Guilds.slice(minGroup)
            .select { active eq true }

        // SELECT * FROM guilds WHERE last_update = minSelect AND active = true
        val resultSet =
            Guilds.select { (lastUpdate eqSubQuery minSelect) and (active eq true) }.limit(1).firstOrNull()
                ?: return null

        return Guild.wrapRow(resultSet)
    }

    fun getActiveCount(): Int {
        return Guilds.select { active eq true }.count().toInt()
    }
}

class Guild(id: EntityID<Snowflake>) : SnowflakeEntity(id) {
    var name by Guilds.name
    var approxMemberCount by Guilds.approxMemberCount
    var lastUpdate by Guilds.lastUpdate
    var active by Guilds.active

    val rooms by Room referrersOn Rooms.guild

    companion object : SnowflakeEntityClass<Guild>(Guilds) {
        fun getOrCreate(kordGuild: KordGuild): Guild {
            return findById(kordGuild.id) ?: new(kordGuild.id) {}.apply {
                this.name = kordGuild.name
                this.approxMemberCount = kordGuild.approximateMemberCount ?: 0
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Guild) return false

        if (name != other.name) return false
        if (approxMemberCount != other.approxMemberCount) return false
        if (lastUpdate != other.lastUpdate) return false
        if (active != other.active) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + approxMemberCount
        result = 31 * result + lastUpdate.hashCode()
        result = 31 * result + active.hashCode()
        return result
    }

    override fun toString(): String {
        return "Guild(" +
                "name='$name', " +
                "approxMemberCount=$approxMemberCount, " +
                "lastUpdate=$lastUpdate, " +
                "active=$active, " +
                "rooms=$rooms" +
                ")"
    }

}
