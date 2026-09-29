package com.innovatv.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.innovatv.app.api.XtreamClient
import com.innovatv.app.databinding.ActivityMainBinding
import com.innovatv.app.databinding.ItemCanalBinding
import com.innovatv.app.databinding.ItemPeliculaBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        servidor = intent.getStringExtra("servidor") ?: ""
        usuario = intent.getStringExtra("usuario") ?: ""
        password = intent.getStringExtra("password") ?: ""

        client = XtreamClient()
        client.configurar(servidor, usuario, password)

        binding.btnCanales.setOnClickListener {
            mostrandoCanales = true
            binding.btnCanales.setBackgroundColor(getColor(R.color.primary_dark))
            binding.btnPeliculas.setBackgroundColor(getColor(R.color.surface_variant))
            actualizarLista()
        }

        binding.btnPeliculas.setOnClickListener {
            mostrandoCanales = false
            binding.btnPeliculas.setBackgroundColor(getColor(R.color.primary_dark))
            binding.btnCanales.setBackgroundColor(getColor(R.color.surface_variant))
            actualizarLista()
        }

        cargarTodo()
    }

    private fun cargarTodo() {
        binding.progressMain.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                categorias = client.obtenerCategorias()
                canales = client.obtenerCanales()
                peliculas = client.obtenerPeliculas()

                binding.progressMain.visibility = View.GONE
                binding.spinnerCategorias.visibility = View.VISIBLE
                configurarSpinner()
                actualizarLista()
            } catch (e: Exception) {
                binding.progressMain.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun configurarSpinner() {
        val nombres = mutableListOf("Todas")
        nombres.addAll(categorias.map { it.nombre })
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategorias.adapter = adapter

        binding.spinnerCategorias.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                actualizarLista()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun actualizarLista() {
        val catSeleccionada = binding.spinnerCategorias.selectedItem?.toString() ?: "Todas"

        if (mostrandoCanales) {
            val filtrados = if (catSeleccionada == "Todas") {
                canales
            } else {
                val catId = categorias.firstOrNull { it.nombre == catSeleccionada }?.id ?: ""
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
}