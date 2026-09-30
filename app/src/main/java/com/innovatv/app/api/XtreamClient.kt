package com.innovatv.app.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.innovatv.app.models.Canal
import com.innovatv.app.models.Categoria
import com.innovatv.app.models.Pelicula
import com.innovatv.app.models.ProgramitaEPG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class XtreamClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private var servidor: String = ""
    private var usuario: String = ""
    private var password: String = ""

    fun configurar(servidor: String, usuario: String, password: String) {
        this.servidor = servidor.trimEnd('/')
        this.usuario = usuario
        this.password = password
    }

    private fun baseUrl(): String = "$servidor/player_api.php?username=$usuario&password=$password"

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "InnovaTV/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }
    }

    suspend fun verificarLogin(): Boolean {
        return try {
            val json = get(baseUrl())
            val obj = JsonParser.parseString(json).asJsonObject
            val auth = obj.getAsJsonObject("user_info")?.get("auth")?.asInt ?: 0
            auth == 1
        } catch (e: Exception) {
            false
        }
    }

    suspend fun obtenerCategorias(): List<Categoria> {
        return try {
            val json = get("${baseUrl()}&action=get_live_categories")
            val array = JsonParser.parseString(json).asJsonArray
            array.mapNotNull { el ->
                val obj = el.asJsonObject
                Categoria(
                    id = obj.get("category_id")?.asString ?: "",
                    nombre = obj.get("category_name")?.asString ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun obtenerCanales(): List<Canal> {
        return try {
            val json = get("${baseUrl()}&action=get_live_streams")
            val array = JsonParser.parseString(json).asJsonArray
            array.mapNotNull { el ->
                try {
                    val obj = el.asJsonObject
                    Canal(
                        num = obj.get("num")?.asInt ?: 0,
                        nombre = obj.get("name")?.asString ?: "",
                        categoria = "",
                        categoriaId = obj.get("category_id")?.asString ?: "",
                        logo = obj.get("stream_icon")?.asString ?: "",
                        streamId = obj.get("stream_id")?.asInt ?: 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun obtenerPeliculas(): List<Pelicula> {
        return try {
            val json = get("${baseUrl()}&action=get_vod_streams")
            val array = JsonParser.parseString(json).asJsonArray
            array.mapNotNull { el ->
                try {
                    val obj = el.asJsonObject
                    Pelicula(
                        id = obj.get("stream_id")?.asInt ?: 0,
                        titulo = obj.get("name")?.asString ?: "",
                        caratula = obj.get("stream_icon")?.asString ?: "",
                        categoria = "",
                        anio = "",
                        extension = obj.get("container_extension")?.asString ?: "mp4"
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun obtenerEPG(streamId: Int): List<ProgramitaEPG> {
        return try {
            val json = get("${baseUrl()}&action=get_short_epg&stream_id=$streamId")
            val obj = JsonParser.parseString(json).asJsonObject
            val array = obj.getAsJsonArray("epg_listings") ?: return emptyList()
            array.mapNotNull { el ->
                try {
                    val p = el.asJsonObject
                    ProgramitaEPG(
                        titulo = p.get("title")?.asString ?: "",
                        descripcion = p.get("description")?.asString ?: "",
                        inicio = p.get("start")?.asString ?: "",
                        fin = p.get("end")?.asString ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun obtenerBulkEPG(): Map<String, String> {
        return try {
            val json = get("${baseUrl()}&action=get_bulk_epg")
            val obj = JsonParser.parseString(json).asJsonObject
            val resultado = mutableMapOf<String, String>()
            for (key in obj.keySet()) {
                resultado[key] = obj.get(key)?.asString ?: ""
            }
            resultado
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun urlStream(streamId: Int): String {
        return "$servidor/live/$usuario/$password/$streamId.m3u8"
    }

    fun urlPelicula(id: Int, ext: String): String {
        return "$servidor/movie/$usuario/$password/$id.$ext"
    }
}