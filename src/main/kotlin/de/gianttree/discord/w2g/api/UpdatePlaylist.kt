package de.gianttree.discord.w2g.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object UpdatePlaylist {
    @Serializable
    data class Request(
        @SerialName("w2g_api_key") val apiToken: String,
        @SerialName("add_items") val additionalItems: List<UrlObject>
    ) {
        @Serializable
        data class UrlObject(val url: String)

        constructor(apiToken: String, urls: Collection<String>) : this(apiToken, urls.map { UrlObject(it) })
    }

    suspend fun call(client: HttpClient, streamKey: String, request: Request) {
        client.post("https://w2g.tv/rooms/$streamKey/playlists/current/playlist_items/sync_update") {
            contentType(ContentType.Application.Json)
            headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body<String>().also { println(it) }
    }
}
