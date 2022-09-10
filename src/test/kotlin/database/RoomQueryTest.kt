package de.gianttree.discord.w2g.database

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.logging.Logger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoomQueryTest {

    private lateinit var guild: Guild
    private lateinit var database: Database

    @BeforeAll
    fun setupDatabase() {
        runBlocking {
            this@RoomQueryTest.database = setupDatabaseConnection(config, Logger.getAnonymousLogger())
            suspendedInTransaction(this@RoomQueryTest.database) {
                Guilds.deleteAll()
                this@RoomQueryTest.guild = Guild.new(Snowflake(0)) {
                    this.name = "Test Guild"
                    this.active = true
                    this.approxMemberCount = 0
                    this.lastUpdate = Clock.System.now().toEpochMilliseconds()
                }
                Rooms.deleteAll()
            }
        }
    }

    @Test
    fun createRoom() {
        assertDoesNotThrow {
            transaction {
                Room.new {
                    this.guild = this@RoomQueryTest.guild
                    this.w2gId = "123456789"
                    this.createdAt = Clock.System.now().toEpochMilliseconds()
                }
            }
        }
    }

}
