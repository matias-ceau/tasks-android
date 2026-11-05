package fr.matiasceau.tasks.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.IOException
import java.util.concurrent.TimeUnit

interface TasksApi {
    @GET("v1/tasks")
    suspend fun list(): TaskList

    // Response bodies of the write endpoints are ignored: the list is re-fetched afterwards.
    @POST("v1/tasks")
    suspend fun create(@Body task: NewTask)

    @POST("v1/tasks/{id}/done")
    suspend fun done(@Path("id") id: String)

    @DELETE("v1/tasks/{id}")
    suspend fun delete(@Path("id") id: String)

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false // omit optional fields instead of sending "null"
        }

        /** Throws IllegalArgumentException if [baseUrl] is not a valid http(s) URL. */
        fun build(baseUrl: String, apiKey: String): TasksApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder().header("X-API-Key", apiKey).build()
                    )
                }
                .build()
            return Retrofit.Builder()
                .baseUrl(baseUrl.trim().trimEnd('/') + "/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TasksApi::class.java)
        }
    }
}

fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> {
        val detail = runCatching {
            val body = response()?.errorBody()?.string().orEmpty()
            when (val d = (TasksApi.json.parseToJsonElement(body) as JsonObject)["detail"]) {
                is JsonPrimitive -> d.content
                null -> null
                else -> d.toString()
            }
        }.getOrNull()
        when (code()) {
            401 -> "Clé API manquante ou invalide"
            403 -> "Accès refusé (scope insuffisant)"
            502 -> "Service indisponible (502)"
            else -> "Erreur ${code()}" + (detail?.let { " : $it" } ?: "")
        }
    }
    is IOException -> "Réseau indisponible"
    is IllegalArgumentException -> "URL de base invalide"
    else -> "Erreur inattendue : ${message ?: javaClass.simpleName}"
}
