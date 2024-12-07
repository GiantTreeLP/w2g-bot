@file:OptIn(ExperimentalTime::class)

package de.gianttree.discord.w2g

import dev.kord.common.serialization.DurationInSeconds
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

private val json = Json {
    encodeDefaults = true
    prettyPrint = true
    ignoreUnknownKeys = true
}

@Serializable
data class Config(
    val debugMode: Boolean = false,
    val discordToken: String = "YOUR_DISCORD_TOKEN_HERE",
    val w2gToken: String = "YOUR_W2G_TOKEN_HERE",
    val httpPort: Int = 12345,
    val intervals: Intervals = Intervals(),
    val databaseConnection: DatabaseConnection = DatabaseConnection(),
)

@Serializable
data class DatabaseConnection(
    val jdbcUrl: String = "jdbc:mariadb://localhost:3306/w2g-bot",
    val driver: String = "org.mariadb.jdbc.Driver",
    val username: String = "w2g-bot",
    val password: String = "w2g-bot",
    val maxPoolSize: Int = 2,
    val minIdle: Int = 1,
)

@Serializable
data class Intervals(
    val presenceInterval: DurationInSeconds = 5.minutes,
    val guildMemberUpdateInterval: DurationInSeconds = 2.minutes,
)

fun readConfig(): Config {
    val configFile = File("config.json")

    if (configFile.exists() && configFile.isFile) {
        val config = json.decodeFromString<Config>(configFile.readText())

        configFile.writeText(json.encodeToString(config))

        return config
    } else {
        val config = Config()

        if (configFile.exists()) {
            configFile.delete()
        }
        configFile.createNewFile()

        configFile.writeText(
            json.encodeToString(config)
        )

        return config
    }
}
