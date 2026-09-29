package com.innovatv.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.innovatv.app.databinding.ActivityAjustesBinding

class AjustesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAjustesBinding
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAjustesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("innovatv", MODE_PRIVATE)

        // Info del usuario
        binding.textUsuario.text = prefs.getString("usuario", "?") ?: "?"
        binding.textServidor.text = prefs.getString("servidor", "?") ?: "?"

        // Cargar preferencias guardadas
        val bufferActual = prefs.getInt("buffer", 1500)
        val aspectActual = prefs.getString("aspect", "fit") ?: "fit"

        // Configurar los radio groups
        when (bufferActual) {
            500 -> binding.radioBufferBajo.isChecked = true
            1500 -> binding.radioBufferMedio.isChecked = true
            3000 -> binding.radioBufferAlto.isChecked = true
        }

        when (aspectActual) {
            "fit" -> binding.radioAspectFit.isChecked = true
            "fill" -> binding.radioAspectFill.isChecked = true
            "16_9" -> binding.radioAspect16_9.isChecked = true
        }

        // Guardar buffer
        binding.radioGroupBuffer.setOnCheckedChangeListener { _, id ->
            val valor = when (id) {
                R.id.radioBufferBajo -> 500
                R.id.radioBufferAlto -> 3000
                else -> 1500
            }
            prefs.edit().putInt("buffer", valor).apply()
            Toast.makeText(this, "Buffer guardado", Toast.LENGTH_SHORT).show()
        }

        // Guardar aspect
        binding.radioGroupAspect.setOnCheckedChangeListener { _, id ->
            val valor = when (id) {
                R.id.radioAspectFill -> "fill"
                R.id.radioAspect16_9 -> "16_9"
                else -> "fit"
            }
            prefs.edit().putString("aspect", valor).apply()
            Toast.makeText(this, "Modo guardado", Toast.LENGTH_SHORT).show()
        }

        // Boton volver
        binding.btnAtras.setOnClickListener { finish() }

        // Boton cerrar sesion
        binding.btnCerrarSesion.setOnClickListener {
            prefs.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}