package net.opatry.google.keep

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import net.opatry.google.keep.entity.ListNotesResponse
import net.opatry.google.keep.entity.Note
import net.opatry.google.keep.entity.Role

// TODO scopes
//  https://www.googleapis.com/auth/keep
//  https://www.googleapis.com/auth/keep.readonly
class KeepService(
    private val httpClient: HttpClient
) {
    /**
     * [Creates a new note](https://developers.google.com/keep/api/reference/rest/v1/notes/create).
     *
     * @param note The request body contains an instance of Note.
     *
     * @return If successful, the response body contains a newly created instance of [Note].
     */
    suspend fun create(note: Note): Note {
        val response = httpClient.post("v1/notes") {
            contentType(ContentType.Application.Json)
            setBody(note)
        }
        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    /**
     * [Deletes a note](https://developers.google.com/keep/api/reference/rest/v1/notes/delete). Caller must have the [Role.Owner] role on the note to delete. Deleting a note removes the resource immediately and cannot be undone. Any collaborators will lose access to the note.
     *
     * @param name Name of the note to delete.
     */
    suspend fun delete(name: String) {
        val response = httpClient.delete("v1/{name=notes/*}") {
            parameter("name", name)
        }

        if (!response.status.isSuccess()) {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    /**
     * [Gets a note](https://developers.google.com/keep/api/reference/rest/v1/notes/get).
     *
     * @param name Name of the resource.
     *
     * @return If successful, the response body contains an instance of [Note].
     */
    suspend fun get(name: String): Note {
        val response = httpClient.get("v1/{name=notes/*}") {
            contentType(ContentType.Application.Json)
            parameter("name", name)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    /**
     * [Lists notes](https://developers.google.com/keep/api/reference/rest/v1/notes/list).
     *
     * Every list call returns a page of results with [pageSize] as the upper bound of returned items. A [pageSize] of zero allows the server to choose the upper bound.
     *
     * The [ListNotesResponse] contains at most [pageSize] entries. If there are more things left to list, it provides a [ListNotesResponse.nextPageToken] value. (Page tokens are opaque values.)
     *
     * To get the next page of results, copy the result's [ListNotesResponse.nextPageToken] into the next request's [pageToken]. Repeat until the [ListNotesResponse.nextPageToken] returned with a page of results is empty.
     *
     * @param pageSize
     * @param pageToken
     * @param filter Filter for list results. If no filter is supplied, the `trashed` filter is applied by default. Valid fields to filter by are: `createTime`, `updateTime`, `trashTime`, and `trashed`. Filter syntax follows the [Google AIP filtering spec](https://aip.dev/160).
     */
    suspend fun list(pageSize: Int, pageToken: String, filter: String = "trashed"): ListNotesResponse {
        val response = httpClient.get("v1/notes") {
            contentType(ContentType.Application.Json)
            parameter("pageSize", pageSize)
            parameter("pageToken", pageToken)
            parameter("filter", filter)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    suspend fun download() {
        // TODO
    }

    suspend fun batchCreatePermissions() {
        // TODO
    }

    suspend fun batchDeletePermissions() {
        // TODO
    }
}