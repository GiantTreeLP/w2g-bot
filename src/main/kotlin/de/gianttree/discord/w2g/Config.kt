package de.gianttree.discord.w2g

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val discordToken: String = "YOUR_DISCORD_TOKEN_HERE",
    val w2gToken: String = "YOUR_W2G_TOKEN_HERE",
    val httpPort: Int = 12345
)
