package com.innovatv.app.config

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object AppConfig {

    var nombreApp: String = "Innova TV"
    var eslogan: String = "Disfruta de una buena programación 24/7"
    var colorPrimario: Int = Color.parseColor("#58A6FF")
    var colorFondo: Int = Color.parseColor("#0D1117")
    var logoUrl: String = ""
    var aviso: String = ""
    var servidorDefault: String = "https://innovatv.dpdns.org"
    var telegramSoporte: String = ""
    var whatsappSoporte: String = ""

    private var cargado = false

    suspend fun cargar(servidor: String) {
        if (cargado) return
        try {
            val url = "${servidor.trimEnd('/')}/app_config.json"
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(url).build()
            val json = withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { it.body?.string() ?: "" }
            }
            if (json.isNotEmpty()) {
                val obj = JsonParser.parseString(json).asJsonObject
                nombreApp = obj.get("nombre_app")?.asString ?: nombreApp
                eslogan = obj.get("eslogan")?.asString ?: eslogan
                val cp = obj.get("color_primario")?.asString ?: ""
                if (cp.startsWith("#")) colorPrimario = Color.parseColor(cp)
                val cf = obj.get("color_fondo")?.asString ?: ""
                if (cf.startsWith("#")) colorFondo = Color.parseColor(cf)
                logoUrl = obj.get("logo_url")?.asString ?: ""
                aviso = obj.get("aviso")?.asString ?: ""
                servidorDefault = obj.get("servidor_default")?.asString ?: servidorDefault
                telegramSoporte = obj.get("telegram_soporte")?.asString ?: ""
                whatsappSoporte = obj.get("whatsapp_soporte")?.asString ?: ""
                cargado = true
            }
        } catch (e: Exception) {
            // Silencioso: se usan los valores por defecto
        }
    }

    fun limpiarCache() {
        cargado = false
    }
}