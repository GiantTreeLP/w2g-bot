package de.gianttree.discord.w2g.monitoring

import de.gianttree.discord.w2g.Context
import de.gianttree.discord.w2g.database.Guilds
import de.gianttree.discord.w2g.database.Room
import de.gianttree.discord.w2g.database.suspendedInTransaction
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import java.lang.management.ManagementFactory
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun launchMonitoringServer(context: Context): CIOApplicationEngine {
    return embeddedServer(
        CIO,
        port = context.config.httpPort,
        module = { this.w2GMonitoringModule(context) },
    ).also { it.start() }
}

@ExperimentalTime
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

@ExperimentalTime
fun Application.w2GMonitoringModule(context: Context) {
    val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
    install(ContentNegotiation) {
        json()
    }
    routing {
        get("/") {
            suspendedInTransaction(context.database) {
                call.respond(
                    Status(
                        Clock.System.now(),
                        Instant.fromEpochMilliseconds(
                            runtimeMXBean.startTime
                        ).periodUntil(
                            Instant.fromEpochMilliseconds(runtimeMXBean.startTime)
                                .plus(runtimeMXBean.uptime, DateTimeUnit.MILLISECOND),
                            TimeZone.currentSystemDefault()
                        ),
                        context.client.resources.shards.totalShards,
                        context.client.gateway.gateways.size,
                        context.client.gateway.gateways.mapValues { it.value.ping.value?.toDateTimePeriod() },
                        Guilds.getActiveCount(),
                        Guilds.getMemberCountSum(),
                        Room.count()
                    )
                )
            }
        }
    }
}
