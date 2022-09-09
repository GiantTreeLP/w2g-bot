package de.gianttree.discord.w2g.database

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import dev.kord.core.entity.Guild as KordGuild

object Guilds : SnowflakeIdTable() {
    val name = varchar("name", 100).index()
    val approxMemberCount = integer("approx_member_count")
    val lastUpdate = timestamp("last_update").defaultExpression(CurrentTimestamp()).index()
}

class Guild(id: EntityID<Snowflake>) : SnowflakeEntity(id) {
    var name by Guilds.name
    var approxMemberCount by Guilds.approxMemberCount
    var lastUpdate by Guilds.lastUpdate

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

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + approxMemberCount
        result = 31 * result + lastUpdate.hashCode()
        return result
    }

    override fun toString(): String {
        return "Guild(name='$name', approxMemberCount=$approxMemberCount, lastUpdate=$lastUpdate)"
    }

}
