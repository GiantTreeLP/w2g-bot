package de.gianttree.discord.w2g.monitoring

import de.gianttree.discord.w2g.Context
import de.gianttree.discord.w2g.database.Guild
import de.gianttree.discord.w2g.database.Room
import de.gianttree.discord.w2g.database.suspendedInTransaction
import dev.kord.core.entity.Guild as KordGuild

class RoomCounter {

    suspend fun addRoom(context: Context, kordGuild: KordGuild?, w2gId: String) {
        if (kordGuild == null) return
        suspendedInTransaction(context.database) {
            val guild = Guild.getOrCreate(kordGuild)
            Room.new {
                this.guild = guild
                this.w2gId = w2gId
            }
        }
    }

}
