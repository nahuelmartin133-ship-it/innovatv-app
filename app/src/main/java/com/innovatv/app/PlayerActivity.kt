package com.innovatv.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.innovatv.app.api.XtreamClient
import com.innovatv.app.databinding.ActivityPlayerBinding
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var client: XtreamClient
    private lateinit var prefs: android.content.SharedPreferences

    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null

    private var servidor = ""
    private var usuario = ""
    private var password = ""
    private var tipo = ""
    private var streamId = 0
    private var nombre = ""
    private var extension = "mp4"

    // Lista de canales para cambiar con anterior/siguiente
    private var listaCanales: List<com.innovatv.app.models.Canal> = emptyList()
    private var indiceActual = -1

    // Aspect actual
    private var aspectActual = 0  // 0=fit, 1=fill, 2=16:9

    // Auto-hide
    private var ocultarHandler: Handler? = null
    private var ocultarRunnable: Runnable? = null
    private var interfazVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("innovatv", MODE_PRIVATE)
        servidor = prefs.getString("servidor", "") ?: ""
        usuario = prefs.getString("usuario", "") ?: ""
        password = prefs.getString("password", "") ?: ""

        tipo = intent.getStringExtra("tipo") ?: "live"
        streamId = intent.getIntExtra("streamId", 0)
        nombre = intent.getStringExtra("nombre") ?: ""
        extension = intent.getStringExtra("extension") ?: "mp4"

        binding.textNombreCanal.text = nombre

        client = XtreamClient()
        client.configurar(servidor, usuario, password)

        // Mantener pantalla encendida
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Configurar botones
        configurarBotones()

        // Ocultar progreso y volumen si es canal en vivo
        if (tipo == "live") {
            binding.containerProgreso.visibility = View.GONE
        } else {
            binding.containerProgreso.visibility = View.VISIBLE
        }

        iniciarReproductor()

        if (tipo == "live") {
            cargarEPG()
            cargarListaCanales()
        }

        // Mostrar interfaz y programar ocultar
        mostrarInterfaz()
        programarOcultar()
    }

    private fun configurarBotones() {
        binding.btnVolver.setOnClickListener { finish() }

        binding.btnPlayPause.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            } else {
                mediaPlayer?.play()
            }
            programarOcultar()
        }

        binding.btnAnterior.setOnClickListener {
            cambiarCanal(-1)
            programarOcultar()
        }

        binding.btnSiguiente.setOnClickListener {
            cambiarCanal(1)
            programarOcultar()
        }

        binding.btnVolumen.setOnClickListener {
            if (binding.containerVolumen.visibility == View.VISIBLE) {
                binding.containerVolumen.visibility = View.GONE
            } else {
                binding.containerVolumen.visibility = View.VISIBLE
            }
            programarOcultar()
        }

        binding.seekVolumen.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.volume = progress
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                programarOcultar()
            }
        })

        binding.btnAspecto.setOnClickListener {
            aspectActual = (aspectActual + 1) % 3
            aplicarAspecto()
            val nombres = arrayOf("Ajustar", "Llenar", "16:9")
            Toast.makeText(this, nombres[aspectActual], Toast.LENGTH_SHORT).show()
            programarOcultar()
        }

        // Tocar el video para mostrar/ocultar interfaz
        binding.videoLayout.setOnClickListener {
            if (interfazVisible) {
                ocultarInterfaz()
            } else {
                mostrarInterfaz()
                programarOcultar()
            }
        }
    }

    private fun aplicarAspecto() {
        val aspect = when (aspectActual) {
            1 -> "16:9"
            2 -> "16:9"
            else -> null
        }
        mediaPlayer?.aspectRatio = aspect
    }

    private fun cambiarCanal(direccion: Int) {
        if (listaCanales.isEmpty() || indiceActual < 0) return
        var nuevoIndice = indiceActual + direccion
        if (nuevoIndice < 0) nuevoIndice = listaCanales.size - 1
        if (nuevoIndice >= listaCanales.size) nuevoIndice = 0
        indiceActual = nuevoIndice
        val canal = listaCanales[indiceActual]
        streamId = canal.streamId
        nombre = canal.nombre
        binding.textNombreCanal.text = nombre
        binding.textEPG.text = ""
        cargarEPG()
        reiniciarReproductor()
    }

    private fun reiniciarReproductor() {
        mediaPlayer?.stop()
        val url = client.urlStream(streamId)
        try {
            val media = Media(libVLC, android.net.Uri.parse(url))
            media.setHWDecoderEnabled(true, false)
            media.addOption(":network-caching=${prefs.getInt("buffer", 1500)}")
            mediaPlayer?.media = media
            media.release()
            mediaPlayer?.play()
        } catch (e: Exception) {
            // Ignorar
        }
    }

    private fun cargarListaCanales() {
        lifecycleScope.launch {
            try {
                val canales = client.obtenerCanales()
                listaCanales = canales
                indiceActual = canales.indexOfFirst { it.streamId == streamId }
            } catch (e: Exception) {
                // Ignorar
            }
        }
    }

    private fun iniciarReproductor() {
        binding.progressPlayer.visibility = View.VISIBLE

        val url = if (tipo == "live") {
            client.urlStream(streamId)
        } else {
            client.urlPelicula(streamId, extension)
        }

        val bufferMs = prefs.getInt("buffer", 1500)

        val opciones = arrayListOf(
            "--no-drop-late-frames",
            "--no-skip-frames",
            "--network-caching=$bufferMs",
            "--rtsp-tcp",
            "--http-referrer=" + servidor,
            "--http-user-agent=VLC/3.0.18 LibVLC/3.0.18"
        )

        libVLC = LibVLC(this, opciones)
        mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer?.attachViews(binding.videoLayout, null, false, false)
        mediaPlayer?.volume = binding.seekVolumen.progress

        mediaPlayer?.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> {
                    binding.progressPlayer.visibility = View.GONE
                    binding.textErrorPlayer.visibility = View.GONE
                    binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                }
                MediaPlayer.Event.Paused -> {
                    binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                }
                MediaPlayer.Event.Buffering -> {
                    if (event.buffering < 100f) {
                        binding.progressPlayer.visibility = View.VISIBLE
                    } else {
                        binding.progressPlayer.visibility = View.GONE
                    }
                }
                MediaPlayer.Event.EncounteredError -> {
                    binding.progressPlayer.visibility = View.GONE
                    binding.textErrorPlayer.text = "Error de reproducción"
                    binding.textErrorPlayer.visibility = View.VISIBLE
                }
                MediaPlayer.Event.TimeChanged -> {
                    if (tipo != "live") {
                        val tiempo = event.timeChanged
                        binding.seekBar.progress = tiempo.toInt()
                        binding.textTiempoActual.text = formatearTiempo(tiempo)
                    }
                }
                MediaPlayer.Event.LengthChanged -> {
                    if (tipo != "live") {
                        binding.seekBar.max = event.lengthChanged.toInt()
                        binding.textTiempoTotal.text = formatearTiempo(event.lengthChanged)
                    }
                }
            }
        }

        try {
            val media = Media(libVLC, android.net.Uri.parse(url))
            media.setHWDecoderEnabled(true, false)
            media.addOption(":network-caching=$bufferMs")
            mediaPlayer?.media = media
            media.release()
            mediaPlayer?.play()

            if (tipo != "live") {
                binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        mediaPlayer?.time = (seekBar?.progress ?: 0).toLong()
                        programarOcultar()
                    }
                })
            }
        } catch (e: Exception) {
            binding.progressPlayer.visibility = View.GONE
            binding.textErrorPlayer.text = "Error: ${e.message}"
            binding.textErrorPlayer.visibility = View.VISIBLE
        }
    }

    private fun formatearTiempo(ms: Long): String {
        val totalSeg = ms / 1000
        val min = totalSeg / 60
        val seg = totalSeg % 60
        return String.format("%02d:%02d", min, seg)
    }

    private fun cargarEPG() {
        lifecycleScope.launch {
            try {
                val epg = client.obtenerEPG(streamId)
                if (epg.isNotEmpty()) {
                    val actual = epg.firstOrNull()
                    if (actual != null) {
                        binding.textEPG.text = "Ahora: ${actual.titulo}"
                    }
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    private fun mostrarInterfaz() {
        interfazVisible = true
        binding.barraSuperior.visibility = View.VISIBLE
        binding.barraInferior.visibility = View.VISIBLE
    }

    private fun ocultarInterfaz() {
        interfazVisible = false
        binding.barraSuperior.visibility = View.GONE
        binding.barraInferior.visibility = View.GONE
    }

    private fun programarOcultar() {
        ocultarHandler?.removeCallbacks(ocultarRunnable ?: return)
        ocultarRunnable = Runnable { ocultarInterfaz() }
        ocultarHandler = Handler(Looper.getMainLooper())
        ocultarHandler?.postDelayed(ocultarRunnable!!, 5000)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        ocultarHandler?.removeCallbacks(ocultarRunnable ?: return)
        mediaPlayer?.stop()
        mediaPlayer?.detachViews()
        mediaPlayer?.release()
        mediaPlayer = null
        libVLC?.release()
        libVLC = null
    }
}