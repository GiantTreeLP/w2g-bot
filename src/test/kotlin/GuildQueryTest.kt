package de.gianttree.discord.w2g

import de.gianttree.discord.w2g.database.Guilds
import de.gianttree.discord.w2g.database.setupDatabaseConnection
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.logging.Logger
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GuildQueryTest {

    lateinit var database: Database

    @OptIn(ExperimentalSerializationApi::class)
    @BeforeAll
    fun setupDatabase() {
        val config = readConfig()
        runBlocking {
            this@GuildQueryTest.database = setupDatabaseConnection(config, Logger.getAnonymousLogger())
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
