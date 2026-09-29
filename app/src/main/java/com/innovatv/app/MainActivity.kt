package com.innovatv.app

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.innovatv.app.api.XtreamClient
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

        // Info del menu lateral
        binding.menuUsuario.text = usuario
        binding.menuServidor.text = servidor

        // Boton menu
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Boton buscar
        binding.btnBuscar.setOnClickListener {
            // TODO: implementar busqueda
        }

        // Menu lateral
        binding.menuInicio.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.menuAjustes.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            // TODO: pantalla de ajustes
        }

        binding.menuCerrarSesion.setOnClickListener {
            val prefs = getSharedPreferences("innovatv", MODE_PRIVATE)
            prefs.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Pestanas
        binding.tabLive.setOnClickListener { cambiarPestana(true) }
        binding.tabMovies.setOnClickListener { cambiarPestana(false) }

        cargarTodo()
    }

    private fun cambiarPestana(esLive: Boolean) {
        mostrandoCanales = esLive
        if (esLive) {
            binding.tabLive.setTextColor(getColor(R.color.primary))
            binding.tabLive.setBackgroundColor(getColor(R.color.background))
            binding.tabMovies.setTextColor(getColor(R.color.text_secondary))
            binding.tabMovies.setBackgroundColor(getColor(android.R.color.transparent))
        } else {
            binding.tabMovies.setTextColor(getColor(R.color.primary))
            binding.tabMovies.setBackgroundColor(getColor(R.color.background))
            binding.tabLive.setTextColor(getColor(R.color.text_secondary))
            binding.tabLive.setBackgroundColor(getColor(android.R.color.transparent))
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
            val chip = TextView(this).apply {
                text = nombre
                setPadding(32, 16, 32, 16)
                setTextColor(if (nombre == categoriaActual) getColor(R.color.white) else getColor(R.color.text_primary))
                setBackgroundColor(if (nombre == categoriaActual) getColor(R.color.primary_dark) else getColor(R.color.surface))
                textSize = 13f
                setOnClickListener {
                    categoriaActual = nombre
                    construirChips()
                    actualizarLista()
                }
            }
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(4, 0, 4, 0)
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