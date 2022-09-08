package de.gianttree.discord.w2g.database

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.vendors.currentDialect

open class SnowflakeIdTable : IdTable<Snowflake>() {
    override val id = snowflake("id").entityId()

    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}

open class SnowflakeEntity(id: EntityID<Snowflake>) : Entity<Snowflake>(id)

open class SnowflakeEntityClass<out E : SnowflakeEntity>(table: SnowflakeIdTable, entityType: Class<E>? = null) :
    EntityClass<Snowflake, E>(table, entityType)

fun Table.snowflake(name: String): Column<Snowflake> = registerColumn(name, SnowflakeColumnType())

class SnowflakeColumnType : ColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.longType()

    override fun valueFromDB(value: Any): Snowflake {
        return when (value) {
            is Snowflake -> value
            is Number -> Snowflake(value.toLong())
            is String -> Snowflake(value.toLong())
            else -> error("Unexpected value of type Snowflake: $value of ${value::class.qualifiedName}")
        }
    }

    override fun notNullValueToDB(value: Any): Any {
        val v = if (value is Snowflake) value.value.toLong() else value
        return super.notNullValueToDB(v)
    }
}
