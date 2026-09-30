package com.innovatv.app

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.innovatv.app.api.XtreamClient
import com.innovatv.app.config.AppConfig
import com.innovatv.app.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("innovatv", MODE_PRIVATE)

        // Cargar config del servidor y aplicar
        lifecycleScope.launch {
            val servidorGuardado = prefs.getString("servidor", "https://innovatv.dpdns.org") ?: "https://innovatv.dpdns.org"
            AppConfig.cargar(servidorGuardado)
            aplicarColores()
        }

        // Si ya hay sesion guardada, ir directo al Main
        val usuarioGuardado = prefs.getString("usuario", null)
        val passwordGuardado = prefs.getString("password", null)
        val servidorGuardado = prefs.getString("servidor", null)
        if (!usuarioGuardado.isNullOrEmpty() && !passwordGuardado.isNullOrEmpty() && !servidorGuardado.isNullOrEmpty()) {
            irAlMain(servidorGuardado, usuarioGuardado, passwordGuardado)
            return
        }

        // Precargar servidor
        binding.editServidor.setText("https://innovatv.dpdns.org")

        binding.btnLogin.setOnClickListener {
            val usuario = binding.editUsuario.text.toString().trim()
            val password = binding.editPassword.text.toString().trim()
            val servidor = binding.editServidor.text.toString().trim().trimEnd('/')

            if (usuario.isEmpty() || password.isEmpty() || servidor.isEmpty()) {
                mostrarError("Completa todos los campos")
                return@setOnClickListener
            }

            hacerLogin(servidor, usuario, password)
        }
    }

    private fun aplicarColores() {
        // Fondo
        binding.root.setBackgroundColor(AppConfig.colorFondo)
        window.decorView.setBackgroundColor(AppConfig.colorFondo)

        // Textos dinamicos
        binding.textTitulo.text = AppConfig.nombreApp
        binding.textEslogan.text = AppConfig.eslogan

        // Color del boton
        binding.btnLogin.backgroundTintList = android.content.res.ColorStateList.valueOf(AppConfig.colorPrimario)

        // Color del titulo
        binding.textTitulo.setTextColor(AppConfig.colorPrimario)

        // Color de los bordes de los inputs
        binding.editUsuario.setTextColor(AppConfig.colorPrimario)
        binding.editPassword.setTextColor(AppConfig.colorPrimario)
        binding.editServidor.setTextColor(AppConfig.colorPrimario)
    }

    private fun hacerLogin(servidor: String, usuario: String, password: String) {
        binding.progressLogin.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.textError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                AppConfig.limpiarCache()
                AppConfig.cargar(servidor)

                val client = XtreamClient()
                client.configurar(servidor, usuario, password)
                val resultado = client.loginCompleto()

                binding.progressLogin.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                if (!resultado.exito) {
                    mostrarError(resultado.mensaje.ifEmpty { getString(R.string.login_error) })
                    return@launch
                }

                // Verificar vencimiento
                if (estaVencido(resultado.expDate)) {
                    // Cuenta vencida -> mostrar pantalla
                    val intent = Intent(this@LoginActivity, VencidaActivity::class.java)
                    intent.putExtra("fecha", resultado.expDate)
                    intent.putExtra("contacto", resultado.contactoReseller)
                    startActivity(intent)
                    return@launch
                }

                // Todo bien -> guardar y entrar
                prefs.edit()
                    .putString("servidor", servidor)
                    .putString("usuario", usuario)
                    .putString("password", password)
                    .putString("exp_date", resultado.expDate)
                    .putString("contacto_reseller", resultado.contactoReseller)
                    .apply()
                irAlMain(servidor, usuario, password)
            } catch (e: Exception) {
                binding.progressLogin.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                mostrarError("Error: ${e.message}")
            }
        }
    }

    private fun estaVencido(expDate: String): Boolean {
        if (expDate.isEmpty()) return false
        return try {
            // Formato esperado: "2026-12-31" o "2026-12-31 23:59:59"
            val formato = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val fechaVencimiento = formato.parse(expDate.split(" ")[0]) ?: return false
            // Comparar: vencido si la fecha de vencimiento es anterior a hoy
            val hoy = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time
            fechaVencimiento.before(hoy)
        } catch (e: Exception) {
            false
        }
    }

    private fun mostrarError(msg: String) {
        binding.textError.text = msg
        binding.textError.visibility = View.VISIBLE
    }

    private fun irAlMain(servidor: String, usuario: String, password: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("servidor", servidor)
        intent.putExtra("usuario", usuario)
        intent.putExtra("password", password)
        startActivity(intent)
        finish()
    }
}