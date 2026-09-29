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
            // TODO: busqueda
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

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}