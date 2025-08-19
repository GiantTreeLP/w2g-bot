package de.gianttree.discord.w2g.monitoring

import de.gianttree.discord.w2g.Context
import de.gianttree.discord.w2g.database.Guild
import de.gianttree.discord.w2g.database.Room
import org.jetbrains.exposed.sql.transactions.transaction
import dev.kord.core.entity.Guild as KordGuild

class RoomCounter {

    fun addRoom(context: Context, kordGuild: KordGuild?, w2gId: String) {
        if (kordGuild == null) return
        transaction(context.database) {
            val guild = Guild.getOrCreate(kordGuild)
            Room.new {
                this.guild = guild
                this.w2gId = w2gId
            }
        }
    }

}
