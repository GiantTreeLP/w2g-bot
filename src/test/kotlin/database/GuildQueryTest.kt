package de.gianttree.discord.w2g.database

import dev.kord.common.entity.Snowflake
import kotlin.time.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.logging.Logger
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GuildQueryTest {

    private lateinit var database: Database

    @BeforeAll
    fun setupDatabase() {
        this.database = setupDatabaseConnection(config, Logger.getAnonymousLogger())
        transaction(this.database) {
            Guilds.deleteAll()
            Guild.new(Snowflake(0)) {
                this.name = "Test Guild"
                this.active = true
                this.approxMemberCount = 0
                this.lastUpdate = Clock.System.now().toEpochMilliseconds()
            }
        }
    }

    @Test
    fun testLeastRecentUpdate() {
        transaction(this.database) {
            assertNotNull(Guilds.getGuildLeastRecentUpdate())
        }
    }

    @Test
    fun testGetActiveCount() {
        transaction(this.database) {
            assertTrue(Guilds.getActiveCount() > 0)
        }
    }
}
