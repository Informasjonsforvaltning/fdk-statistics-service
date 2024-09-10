package no.digdir.fdk.statistics.utils

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate

fun requestApi(
    path: String,
    port: Int,
    body: String? = null,
    httpMethod: HttpMethod,
    accept: MediaType = MediaType.APPLICATION_JSON
): Map<String, Any> {
    val request = RestTemplate()
    request.requestFactory = HttpComponentsClientHttpRequestFactory()
    val url = "http://localhost:$port$path"
    val headers = HttpHeaders()
    headers.accept = listOf(accept)
    headers.contentType = MediaType.APPLICATION_JSON
    val entity: HttpEntity<String> = HttpEntity(body, headers)

    return try {
        val response = request.exchange(url, httpMethod, entity, String::class.java)
        mapOf(
            "body" to response.body,
            "header" to response.headers,
            "status" to response.statusCode.value()
        )
    } catch (e: HttpClientErrorException) {
        mapOf(
            "status" to e.statusCode.value(),
            "header" to " ",
            "body" to e.responseBodyAsString
        )
    } catch (e: HttpServerErrorException) {
        mapOf(
            "status" to e.statusCode.value(),
            "header" to " ",
            "body" to e.responseBodyAsString
        )
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body" to " "
        )
    }
}
