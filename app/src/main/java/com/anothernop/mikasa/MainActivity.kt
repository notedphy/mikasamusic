package com.anothernop.mikasa

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.anothernop.mikasa.playback.MikasaPlaybackService
import com.decent.usbaudio.UsbAudioPermissionHelper
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.lang.InterruptedException

class MainActivity : ComponentActivity() {
    private val playerState = androidx.compose.runtime.mutableStateOf<MediaController?>(null)
    private val connectionErrorState = androidx.compose.runtime.mutableStateOf<String?>(null)
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handleUsbIntent(intent)
        connectPlaybackService()
        setContent {
            MikasaApp(
                player = playerState.value,
                playbackConnectionError = connectionErrorState.value
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUsbIntent(intent)
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        playerState.value = null
        super.onDestroy()
    }

    private fun connectPlaybackService() {
        val token = SessionToken(
            this,
            ComponentName(this, MikasaPlaybackService::class.java)
        )
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    playerState.value = future.get()
                } catch (error: ExecutionException) {
                    Log.e(TAG, "Unable to connect to the playback service", error)
                    connectionErrorState.value =
                        "Playback could not start: ${error.cause?.message ?: error.message}"
                } catch (error: CancellationException) {
                    Log.e(TAG, "Playback service connection was cancelled", error)
                    connectionErrorState.value = "Playback service connection was cancelled."
                } catch (error: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.e(TAG, "Playback service connection was interrupted", error)
                    connectionErrorState.value = "Playback service connection was interrupted."
                }
            },
            mainExecutor
        )
    }

    private fun handleUsbIntent(intent: Intent?) {
        if (intent != null) {
            UsbAudioPermissionHelper.handleIntent(applicationContext, intent)
        }
    }

    private companion object {
        const val TAG = "MikasaMusic"
    }
}
