package de.gianttree.discord.w2g.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.withSuspendTransaction

/**
 * Executes the given suspending [statement] in the current transaction.
 * If there is no current transaction, a new one will be created.
 *
 * If the databases of the transactions differ, a new nested transaction will
 * be created.
 */
suspend fun <T> suspendedInTransaction(
    database: Database,
    statement: suspend Transaction.() -> T
): T {
    val transaction = TransactionManager.currentOrNull()
    return if (transaction != null && (transaction.db == database)) {
        transaction.withSuspendTransaction(null, statement)
    } else {
        newSuspendedTransaction(null, database, statement = statement)
    }
}
