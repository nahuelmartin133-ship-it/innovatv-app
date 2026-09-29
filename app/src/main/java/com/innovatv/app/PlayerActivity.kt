package com.innovatv.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.innovatv.app.api.XtreamClient
import com.innovatv.app.databinding.ActivityPlayerBinding
import kotlinx.coroutines.launch

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private lateinit var client: XtreamClient

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

        // Cliente OkHttp que sigue redirects cross-protocol (HTTPS -> HTTP)
        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("VLC/3.0.18 LibVLC/3.0.18")
            .setDefaultRequestProperties(mapOf(
                "Accept" to "*/*",
                "Connection" to "keep-alive"
            ))

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()

        binding.playerView.player = player

        val mediaItem = MediaItem.fromUri(url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true

        player?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_READY) {
                    binding.progressPlayer.visibility = View.GONE
                } else if (state == androidx.media3.common.Player.STATE_BUFFERING) {
                    binding.progressPlayer.visibility = View.VISIBLE
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                binding.progressPlayer.visibility = View.GONE
                binding.textErrorPlayer.text = "Error: ${error.message}"
                binding.textErrorPlayer.visibility = View.VISIBLE
                Toast.makeText(this@PlayerActivity, "Error de reproducción", Toast.LENGTH_LONG).show()
            }
        })
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
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}