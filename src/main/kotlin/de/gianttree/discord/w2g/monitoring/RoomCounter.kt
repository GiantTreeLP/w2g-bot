package de.gianttree.discord.w2g.monitoring

import java.io.File

private const val ROOM_COUNTER_FILENAME = "roomcounter.txt"

class RoomCounter(rooms: Int = 0) {
    var rooms: Int = rooms
        private set

    fun addRoom() {
        rooms++
    }

    fun save() {
        File(ROOM_COUNTER_FILENAME).writeText(rooms.toString())
    }

    companion object {
        fun load(): RoomCounter {
            val fileContent = File(ROOM_COUNTER_FILENAME).takeIf { it.isFile }?.readText() ?: "0"
            return if (fileContent.matches(Regex("[0-9]+"))) {
                RoomCounter(fileContent.toInt())
            } else {
                RoomCounter()
            }
        }
    }

}
