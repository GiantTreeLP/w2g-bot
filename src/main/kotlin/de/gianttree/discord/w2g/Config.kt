package de.gianttree.discord.w2g

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

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
    val httpPort: Int = 12345
)

@ExperimentalSerializationApi
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
