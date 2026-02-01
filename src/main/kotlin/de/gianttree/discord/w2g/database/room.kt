package de.gianttree.discord.w2g.database

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import kotlin.time.Clock

const val MAX_ROOM_ID_LENGTH = 255

object Rooms : IntIdTable() {
    val guild = reference("guild", Guilds).index()
    val createdAt = long("created_at").clientDefault { Clock.System.now().toEpochMilliseconds() }.index()
    val w2gId = varchar("w2g_id", MAX_ROOM_ID_LENGTH)
}

class Room(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Room>(Rooms)

    var guild by Guild referencedOn Rooms.guild
    var createdAt by Rooms.createdAt
    var w2gId by Rooms.w2gId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Room) return false

        if (guild != other.guild) return false
        if (createdAt != other.createdAt) return false
        if (w2gId != other.w2gId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = guild.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + w2gId.hashCode()
        return result
    }

    override fun toString(): String {
        return "Room(guild=$guild, createdAt=$createdAt, w2gId='$w2gId')"
    }
}
