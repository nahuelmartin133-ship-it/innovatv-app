package com.innovatv.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.innovatv.app.config.AppConfig
import com.innovatv.app.databinding.ActivityVencidaBinding

class VencidaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVencidaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVencidaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fecha = intent.getStringExtra("fecha") ?: ""
        val contacto = intent.getStringExtra("contacto") ?: ""

        // Fondo
        binding.root.setBackgroundColor(AppConfig.colorFondo)
        binding.textTituloVencida.setTextColor(AppConfig.colorPrimario)

        if (fecha.isNotEmpty()) {
            binding.textFechaVencimiento.text = "Venció el $fecha"
        } else {
            binding.textFechaVencimiento.text = "Tu suscripción ya no está activa"
        }

        // Boton de contactar
        if (contacto.isNotEmpty()) {
            binding.btnContactar.visibility = View.VISIBLE
            binding.btnContactar.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contacto))
                    startActivity(intent)
                } catch (e: Exception) {
                    // Ignorar si no hay app para abrirlo
                }
            }
        }

        // Boton cerrar sesion
        binding.btnCerrarSesionVencida.setOnClickListener {
            val prefs = getSharedPreferences("innovatv", MODE_PRIVATE)
            prefs.edit().clear().apply()
            AppConfig.limpiarCache()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}