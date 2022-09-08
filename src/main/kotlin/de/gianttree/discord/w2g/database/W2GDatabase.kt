package de.gianttree.discord.w2g.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.gianttree.discord.w2g.Config
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import java.util.logging.Logger

fun setupDatabaseConnection(config: Config, logger: Logger): Database {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.databaseConnection.jdbcUrl
        driverClassName = config.databaseConnection.driver
        username = config.databaseConnection.username
        password = config.databaseConnection.password
        minimumIdle = config.databaseConnection.minIdle
        maximumPoolSize = config.databaseConnection.maxPoolSize
    }

    val hikariDatasource = HikariDataSource(hikariConfig)

    val database = Database.connect(hikariDatasource, databaseConfig = DatabaseConfig {
        if (config.debugMode) {
            sqlLogger = W2GSQLLogger(logger)
        }
    })
    return database
}

class W2GSQLLogger(private val logger: Logger) : SqlLogger {
    override fun log(context: StatementContext, transaction: Transaction) {
        logger.finest("SQL: " + context.expandArgs(transaction))
    }
}
