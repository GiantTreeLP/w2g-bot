package de.gianttree.discord.w2g.database

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

open class SnowflakeIdTable : IdTable<Snowflake>() {
    final override val id = snowflake("id").entityId()

    override val primaryKey = PrimaryKey(id)
}

open class SnowflakeEntity(id: EntityID<Snowflake>) : Entity<Snowflake>(id)

open class SnowflakeEntityClass<out E : SnowflakeEntity>(table: SnowflakeIdTable, entityType: Class<E>? = null) :
    EntityClass<Snowflake, E>(table, entityType)

fun Table.snowflake(name: String): Column<Snowflake> = registerColumn(name, SnowflakeColumnType())

class SnowflakeColumnType : ColumnType<Snowflake>() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.longType()

    override fun valueFromDB(value: Any): Snowflake {
        return when (value) {
            is Snowflake -> value
            is Number -> Snowflake(value.toLong())
            is String -> Snowflake(value.toLong())
            else -> error("Unexpected value of type Snowflake: $value of ${value::class.qualifiedName}")
        }
    }

    override fun notNullValueToDB(value: Snowflake): Any {
        return value.value.toLong()
    }
}
