package com.innovatv.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.innovatv.app.api.XtreamClient
import com.innovatv.app.config.AppConfig
import com.innovatv.app.databinding.ActivityMainBinding
import com.innovatv.app.models.Canal
import com.innovatv.app.models.Categoria
import com.innovatv.app.models.Pelicula
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var client: XtreamClient
    private var servidor = ""
    private var usuario = ""
    private var password = ""

    private var canales: List<Canal> = emptyList()
    private var peliculas: List<Pelicula> = emptyList()
    private var categorias: List<Categoria> = emptyList()
    private var mostrandoCanales = true
    private var categoriaActual: String = "Todas"
    private var mapaEPG: Map<String, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        servidor = intent.getStringExtra("servidor") ?: ""
        usuario = intent.getStringExtra("usuario") ?: ""
        password = intent.getStringExtra("password") ?: ""

        client = XtreamClient()
        client.configurar(servidor, usuario, password)

        binding.menuUsuario.text = usuario
        binding.menuServidor.text = servidor

        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.btnBuscar.setOnClickListener {
            abrirBusqueda()
        }

        binding.menuInicio.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.menuAjustes.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, AjustesActivity::class.java)
            startActivity(intent)
        }

        binding.menuCerrarSesion.setOnClickListener {
            val prefs = getSharedPreferences("innovatv", MODE_PRIVATE)
            prefs.edit().clear().apply()
            AppConfig.limpiarCache()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.tabLiveContainer.setOnClickListener { cambiarPestana(true) }
        binding.tabMoviesContainer.setOnClickListener { cambiarPestana(false) }

        // Conectar busqueda
        binding.editBusqueda.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                buscar(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.btnCancelarBusqueda.setOnClickListener {
            cerrarBusqueda()
        }

        // Cargar config del servidor y aplicar
        lifecycleScope.launch {
            AppConfig.limpiarCache()
            AppConfig.cargar(servidor)
            aplicarColores()
            construirChips()
        }

        cargarTodo()
    }

    private fun aplicarColores() {
        // Fondo del contenedor
        binding.containerPrincipal.setBackgroundColor(AppConfig.colorFondo)

        // Barra superior con gradiente del color primario
        val gradienteBarra = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(AppConfig.colorPrimario, oscurecerColor(AppConfig.colorPrimario))
        )
        binding.barraSuperior.background = gradienteBarra

        // Cabecera del menu lateral
        val gradienteMenu = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(AppConfig.colorPrimario, oscurecerColor(AppConfig.colorPrimario))
        )
        binding.cabeceraMenu.background = gradienteMenu

        // Nombre de la app
        binding.textNombreApp.text = AppConfig.nombreApp
        binding.textNombreApp.setTextColor(Color.WHITE)

        // Color de las pestañas
        binding.tabLive.setTextColor(AppConfig.colorPrimario)
        binding.indicadorLive.setBackgroundColor(AppConfig.colorPrimario)
        binding.indicadorMovies.setBackgroundColor(AppConfig.colorPrimario)

        // Info del menu lateral
        binding.menuUsuario.text = usuario
        binding.menuServidor.text = servidor

        // Mostrar vencimiento
        actualizarVencimiento()
    }

    private fun actualizarVencimiento() {
        val prefs = getSharedPreferences("innovatv", MODE_PRIVATE)
        val expDate = prefs.getString("exp_date", "") ?: ""
        if (expDate.isEmpty()) {
            binding.menuVencimiento.visibility = View.GONE
            return
        }
        try {
            val formato = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val fechaVencimiento = formato.parse(expDate.split(" ")[0]) ?: return
            val hoy = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time
            val diff = fechaVencimiento.time - hoy.time
            val dias = (diff / (1000 * 60 * 60 * 24)).toInt()
            binding.menuVencimiento.visibility = View.VISIBLE
            if (dias > 1) {
                binding.menuVencimiento.text = "Vence en $dias dias"
                binding.menuVencimiento.setTextColor(android.graphics.Color.WHITE)
            } else if (dias == 1) {
                binding.menuVencimiento.text = "Vence manana"
                binding.menuVencimiento.setTextColor(android.graphics.Color.parseColor("#FFC107"))
            } else if (dias == 0) {
                binding.menuVencimiento.text = "Vence hoy"
                binding.menuVencimiento.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            } else {
                binding.menuVencimiento.text = "Vencida"
                binding.menuVencimiento.setTextColor(android.graphics.Color.parseColor("#F85149"))
            }
        } catch (e: Exception) {
            binding.menuVencimiento.visibility = View.GONE
        }
    }

    private fun oscurecerColor(color: Int): Int {
        val r = (Color.red(color) * 0.6).toInt()
        val g = (Color.green(color) * 0.6).toInt()
        val b = (Color.blue(color) * 0.6).toInt()
        return Color.rgb(r, g, b)
    }

    private fun cambiarPestana(esLive: Boolean) {
        mostrandoCanales = esLive
        if (esLive) {
            binding.tabLive.setTextColor(AppConfig.colorPrimario)
            binding.tabMovies.setTextColor(getColor(R.color.text_secondary))
            binding.indicadorLive.visibility = View.VISIBLE
            binding.indicadorMovies.visibility = View.INVISIBLE
        } else {
            binding.tabMovies.setTextColor(AppConfig.colorPrimario)
            binding.tabLive.setTextColor(getColor(R.color.text_secondary))
            binding.indicadorMovies.visibility = View.VISIBLE
            binding.indicadorLive.visibility = View.INVISIBLE
        }
        categoriaActual = "Todas"
        construirChips()
        actualizarLista()
    }

    private fun cargarTodo() {
        binding.progressMain.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                categorias = client.obtenerCategorias()
                canales = client.obtenerCanales()
                peliculas = client.obtenerPeliculas()
                mapaEPG = client.obtenerBulkEPG()
                canales = canales.map { c ->
                    c.copy(epgActual = mapaEPG[c.num.toString()] ?: "")
                }

                binding.progressMain.visibility = View.GONE
                construirChips()
                actualizarLista()
            } catch (e: Exception) {
                binding.progressMain.visibility = View.GONE
            }
        }
    }

    private fun construirChips() {
        binding.containerCategorias.removeAllViews()
        val nombres = mutableListOf("Todas")
        if (mostrandoCanales) {
            nombres.addAll(categorias.map { it.nombre })
        }
        for (nombre in nombres) {
            val activo = nombre == categoriaActual
            val chip = TextView(this).apply {
                text = nombre
                setPadding(40, 18, 40, 18)
                setTextColor(if (activo) Color.WHITE else getColor(R.color.text_primary))
                textSize = 13f
                isAllCaps = false
                val bg = GradientDrawable().apply {
                    cornerRadius = 60f
                    if (activo) {
                        setColor(AppConfig.colorPrimario)
                    } else {
                        setColor(getColor(R.color.surface_variant))
                        setStroke(2, getColor(R.color.border))
                    }
                }
                background = bg
                setOnClickListener {
                    categoriaActual = nombre
                    construirChips()
                    actualizarLista()
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(6, 4, 6, 4)
            chip.layoutParams = params
            binding.containerCategorias.addView(chip)
        }
    }

    private fun actualizarLista() {
        if (mostrandoCanales) {
            val filtrados = if (categoriaActual == "Todas") {
                canales
            } else {
                val catId = categorias.firstOrNull { it.nombre == categoriaActual }?.id ?: ""
                canales.filter { it.categoriaId == catId }
            }
            mostrarCanales(filtrados)
        } else {
            mostrarPeliculas(peliculas)
        }
    }

    private fun mostrarCanales(lista: List<Canal>) {
        binding.textVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerCanales.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerCanales.adapter = CanalesAdapter(lista) { canal ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("tipo", "live")
            intent.putExtra("streamId", canal.streamId)
            intent.putExtra("nombre", canal.nombre)
            startActivity(intent)
        }
    }

    private fun mostrarPeliculas(lista: List<Pelicula>) {
        binding.textVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerCanales.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerCanales.adapter = PeliculasAdapter(lista) { peli ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("tipo", "vod")
            intent.putExtra("streamId", peli.id)
            intent.putExtra("nombre", peli.titulo)
            intent.putExtra("extension", peli.extension)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        verificarVencimiento()
    }

    private var ultimaVerificacion: Long = 0

    private fun verificarVencimiento() {
        // Limitar a 1 verificacion cada 5 minutos
        val ahora = System.currentTimeMillis()
        if (ahora - ultimaVerificacion < 5 * 60 * 1000) return
        ultimaVerificacion = ahora

        val prefs = getSharedPreferences("innovatv", MODE_PRIVATE)
        val usuario = prefs.getString("usuario", "") ?: ""
        val password = prefs.getString("password", "") ?: ""
        if (usuario.isEmpty() || password.isEmpty()) return

        lifecycleScope.launch {
            try {
                val cliente = XtreamClient()
                cliente.configurar(servidor, usuario, password)
                val resultado = cliente.loginCompleto()
                if (!resultado.exito) {
                    // Fallo el login -> cuenta invalida o vencida
                    irAVencida(resultado.expDate, resultado.contactoReseller)
                    return@launch
                }
                // Verificar vencimiento con la fecha del servidor
                if (estaVencido(resultado.expDate)) {
                    irAVencida(resultado.expDate, resultado.contactoReseller)
                    return@launch
                }
                // Actualizar exp_date y contacto en prefs
                prefs.edit()
                    .putString("exp_date", resultado.expDate)
                    .putString("contacto_reseller", resultado.contactoReseller)
                    .apply()
            } catch (e: Exception) {
                // Sin internet o error -> no hacer nada
            }
        }
    }

    private fun estaVencido(expDate: String): Boolean {
        if (expDate.isEmpty()) return false
        return try {
            val formato = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val fechaVencimiento = formato.parse(expDate.split(" ")[0]) ?: return false
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

    private fun irAVencida(expDate: String, contacto: String) {
        val prefs = getSharedPreferences("innovatv", MODE_PRIVATE)
        prefs.edit().clear().apply()
        AppConfig.limpiarCache()
        val intent = Intent(this, VencidaActivity::class.java)
        intent.putExtra("fecha", expDate)
        intent.putExtra("contacto", contacto)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun abrirBusqueda() {
        binding.overlayBusqueda.visibility = View.VISIBLE
        binding.editBusqueda.text.clear()
        binding.textSinResultados.text = "Escribí para buscar..."
        binding.textSinResultados.visibility = View.VISIBLE
        binding.recyclerBusqueda.adapter = null
        binding.editBusqueda.requestFocus()
        // Abrir teclado
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.editBusqueda, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun cerrarBusqueda() {
        binding.overlayBusqueda.visibility = View.GONE
        // Cerrar teclado
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.editBusqueda.windowToken, 0)
    }

    private fun buscar(texto: String) {
        val query = texto.trim().lowercase()
        if (query.isEmpty()) {
            binding.textSinResultados.text = "Escribí para buscar..."
            binding.textSinResultados.visibility = View.VISIBLE
            binding.recyclerBusqueda.adapter = null
            return
        }

        if (mostrandoCanales) {
            val resultados = canales.filter { it.nombre.lowercase().contains(query) }
            if (resultados.isEmpty()) {
                binding.textSinResultados.text = "No se encontraron canales"
                binding.textSinResultados.visibility = View.VISIBLE
                binding.recyclerBusqueda.adapter = null
            } else {
                binding.textSinResultados.visibility = View.GONE
                binding.recyclerBusqueda.layoutManager = GridLayoutManager(this, 2)
                binding.recyclerBusqueda.adapter = CanalesAdapter(resultados) { canal ->
                    cerrarBusqueda()
                    val intent = Intent(this, PlayerActivity::class.java)
                    intent.putExtra("tipo", "live")
                    intent.putExtra("streamId", canal.streamId)
                    intent.putExtra("nombre", canal.nombre)
                    startActivity(intent)
                }
            }
        } else {
            val resultados = peliculas.filter { it.titulo.lowercase().contains(query) }
            if (resultados.isEmpty()) {
                binding.textSinResultados.text = "No se encontraron películas"
                binding.textSinResultados.visibility = View.VISIBLE
                binding.recyclerBusqueda.adapter = null
            } else {
                binding.textSinResultados.visibility = View.GONE
                binding.recyclerBusqueda.layoutManager = GridLayoutManager(this, 2)
                binding.recyclerBusqueda.adapter = PeliculasAdapter(resultados) { peli ->
                    cerrarBusqueda()
                    val intent = Intent(this, PlayerActivity::class.java)
                    intent.putExtra("tipo", "vod")
                    intent.putExtra("streamId", peli.id)
                    intent.putExtra("nombre", peli.titulo)
                    intent.putExtra("extension", peli.extension)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (binding.overlayBusqueda.visibility == View.VISIBLE) {
            cerrarBusqueda()
        } else if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}