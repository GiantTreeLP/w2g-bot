package de.gianttree.discord.w2g.monitoring

import de.gianttree.discord.w2g.Config
import dev.kord.core.Kord
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.count
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import java.lang.management.ManagementFactory
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun launchMonitoringServer(config: Config, client: Kord) {
    val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
    embeddedServer(CIO, port = config.httpPort) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            get("/") {
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
                        client.resources.shards.totalShards,
                        client.gateway.gateways.size,
                        client.gateway.gateways.mapValues { it.value.ping.value?.toDateTimePeriod() },
                        client.guilds.count()
                    )
                )
            }
        }
    }.start()
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
)
