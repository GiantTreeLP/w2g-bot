package de.gianttree.discord.w2g.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Rooms : IntIdTable() {
    val guild = reference("guild", Guilds).index()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp()).index()
}

class Room(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Room>(Rooms)

    var guild by Guild referencedOn Rooms.guild
    var createdAt by Rooms.createdAt

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Room) return false

        if (guild != other.guild) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = guild.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }

    override fun toString(): String {
        return "Room(guild=$guild, createdAt=$createdAt)"
    }
}
