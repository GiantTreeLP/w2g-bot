package de.gianttree.discord.w2g.database

import de.gianttree.discord.w2g.Config
import de.gianttree.discord.w2g.DatabaseConnection

val config = Config(
    debugMode = true,
    databaseConnection = DatabaseConnection(
        jdbcUrl = "jdbc:sqlite::memory:",
        driver = "org.sqlite.JDBC",
        maxPoolSize = 1,
        minIdle = 1,
    )
)
