package com.innovatv.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.innovatv.app.api.XtreamClient
import com.innovatv.app.databinding.ActivityPlayerBinding
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var client: XtreamClient

    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null

    private var servidor = ""
    private var usuario = ""
    private var password = ""
    private var tipo = ""
    private var streamId = 0
    private var nombre = ""
    private var extension = "mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Leer datos guardados
        val prefs = getSharedPreferences("innovatv", MODE_PRIVATE)
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
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Boton volver
        binding.btnVolver.setOnClickListener { finish() }

        // Boton play/pause
        binding.btnPlayPause.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                binding.btnPlayPause.visibility = View.VISIBLE
            } else {
                mediaPlayer?.play()
                binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        // Tocar el video para mostrar/ocultar controles
        binding.videoLayout.setOnClickListener {
            if (binding.btnPlayPause.visibility == View.VISIBLE) {
                binding.btnPlayPause.visibility = View.GONE
            } else {
                binding.btnPlayPause.visibility = View.VISIBLE
            }
        }

        iniciarReproductor()

        // Cargar EPG si es canal en vivo
        if (tipo == "live") {
            cargarEPG()
        }
    }

    private fun iniciarReproductor() {
        binding.progressPlayer.visibility = View.VISIBLE

        val url = if (tipo == "live") {
            client.urlStream(streamId)
        } else {
            client.urlPelicula(streamId, extension)
        }

        // Configurar libVLC
        val opciones = arrayListOf(
            "--no-drop-late-frames",
            "--no-skip-frames",
            "--network-caching=1500",
            "--rtsp-tcp",
            "--http-referrer=" + servidor,
            "--http-user-agent=VLC/3.0.18 LibVLC/3.0.18"
        )

        libVLC = LibVLC(this, opciones)
        mediaPlayer = MediaPlayer(libVLC)

        mediaPlayer?.attachViews(binding.videoLayout, null, false, false)

        mediaPlayer?.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> {
                    binding.progressPlayer.visibility = View.GONE
                    binding.textErrorPlayer.visibility = View.GONE
                    binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                }
                MediaPlayer.Event.Paused -> {
                    binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                    binding.btnPlayPause.visibility = View.VISIBLE
                }
                MediaPlayer.Event.EndReached -> {
                    binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                    binding.btnPlayPause.visibility = View.VISIBLE
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
                    Toast.makeText(this@PlayerActivity, "Error al reproducir", Toast.LENGTH_LONG).show()
                }
            }
        }

        try {
            val media = Media(libVLC, android.net.Uri.parse(url))
            media.setHWDecoderEnabled(true, false)
            media.addOption(":network-caching=1500")
            mediaPlayer?.media = media
            media.release()
            mediaPlayer?.play()
        } catch (e: Exception) {
            binding.progressPlayer.visibility = View.GONE
            binding.textErrorPlayer.text = "Error: ${e.message}"
            binding.textErrorPlayer.visibility = View.VISIBLE
        }
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
        mediaPlayer?.stop()
        mediaPlayer?.detachViews()
        mediaPlayer?.release()
        mediaPlayer = null
        libVLC?.release()
        libVLC = null
    }
}