package de.gianttree.discord.w2g.monitoring

import de.gianttree.discord.w2g.Context
import de.gianttree.discord.w2g.database.Guilds
import de.gianttree.discord.w2g.database.Room
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlinx.datetime.toDateTimePeriod
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.lang.management.ManagementFactory
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

fun launchMonitoringServer(context: Context): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> {
    return embeddedServer(
        CIO,
        port = context.config.httpPort,
        module = { this.w2GMonitoringModule(context) },
    ).also { it.start() }
}

@Serializable
data class Status(
    val timestamp: Instant,
    val uptime: DateTimePeriod,
    val numShards: Int,
    val numGateways: Int,
    val pings: Map<Int, DateTimePeriod?>,
    val numGuilds: Int,
    val approxMemberCount: Int,
    val roomCount: Long,
)

fun Application.w2GMonitoringModule(context: Context) {
    val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
    install(ContentNegotiation) {
        json()
    }
    routing {
        get("/") {
            call.respond(
                transaction(context.database) {
                    Status(
                        Clock.System.now(),
                        Instant.fromEpochMilliseconds(
                            runtimeMXBean.startTime
                        ).periodUntil(
                            Instant.fromEpochMilliseconds(runtimeMXBean.startTime)
                                .plus(runtimeMXBean.uptime.milliseconds),
                            TimeZone.currentSystemDefault()
                        ),
                        context.client.resources.shards.totalShards,
                        context.client.gateway.gateways.size,
                        context.client.gateway.gateways.mapValues { it.value.ping.value?.toDateTimePeriod() },
                        Guilds.getActiveCount(),
                        Guilds.getMemberCountSum(),
                        Room.count()
                    )
                }
            )
        }
    }
}
