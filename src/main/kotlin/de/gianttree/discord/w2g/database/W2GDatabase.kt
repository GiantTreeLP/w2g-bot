package de.gianttree.discord.w2g.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.gianttree.discord.w2g.Config
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.expandArgs
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import java.util.logging.Logger
import kotlin.time.Duration.Companion.seconds

fun setupDatabaseConnection(config: Config, logger: Logger): Database {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.databaseConnection.jdbcUrl
        driverClassName = config.databaseConnection.driver
        username = config.databaseConnection.username
        password = config.databaseConnection.password
        minimumIdle = config.databaseConnection.minIdle
        maximumPoolSize = config.databaseConnection.maxPoolSize
        leakDetectionThreshold = 5.seconds.inWholeMilliseconds
    }

    val hikariDatasource = HikariDataSource(hikariConfig)

    val database = Database.connect(hikariDatasource, databaseConfig = DatabaseConfig {
        if (config.debugMode) {
            sqlLogger = W2GSQLLogger(logger)
        }
    })

    transaction(database) {
        logger.info("Setting up database...")
        MigrationUtils.statementsRequiredForDatabaseMigration(Guilds, Rooms, withLogs = true)
            .forEach {
                this.exec(it)
            }
        logger.info("Database setup complete.")
    }

    return database
}

class W2GSQLLogger(private val logger: Logger) : SqlLogger {
    override fun log(context: StatementContext, transaction: Transaction) {
        logger.finest("SQL: " + context.expandArgs(transaction))
    }
}
