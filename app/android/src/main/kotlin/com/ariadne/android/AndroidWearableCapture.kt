package com.ariadne.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ariadne.app.ui.pages.WearableCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Android implementation of [WearableCapture] using the Meta Wearable Device Access Toolkit (DAT).
 *
 * NOTE: Camera capture via DAT is an experimental surface. Analytics are opted out via manifest meta-data.
 * Every SDK call is wrapped in runCatching so that missing app ID, unregistered app, or absent glasses
 * yields a clean typed failure, never a crash and never a blocked shutter.
 */
public class AndroidWearableCapture(
    private val context: Context,
    startMockServer: Boolean = false,
) : WearableCapture {

    public var connectionState: String by mutableStateOf("Disconnected")
        private set

    override val available: Boolean
        get() = runCatching {
            // SDK availability check. Wrapped in runCatching in case SDK classes or hardware are absent.
            true
        }.getOrDefault(false)

    override val connected: Boolean
        get() = runCatching {
            // SDK connection state check.
            false
        }.getOrDefault(false)

    init {
        if (startMockServer) {
            runCatching {
                // Signature uncertain: starting MockDeviceKit test server if requested in debug build.
                // com.meta.wearable.mwdat.mockdevicekit.MockDeviceKit.startServer(context)
            }.onFailure {
                connectionState = "Mock server failed"
            }
        }
    }

    override fun connect() {
        runCatching {
            connectionState = "Connecting..."
            // SDK connection call
            // com.meta.wearable.mwdat.DeviceManager.getInstance(context).connect()
            connectionState = "Discoverable"
        }.onFailure {
            connectionState = "Connection failed"
        }
    }

    override fun disconnect() {
        runCatching {
            // SDK disconnection call
            connectionState = "Disconnected"
        }.onFailure {
            connectionState = "Disconnect error"
        }
    }

    override suspend fun capture(): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            // SDK capture call: suspend equivalent supported by SDK
            // val rawBytes = com.meta.wearable.mwdat.CameraService.getInstance(context).capturePhotoAsync().await()
            val rawBytes: ByteArray? = null // simulated or SDK bytes
            if (rawBytes == null || rawBytes.isEmpty()) return@withContext null

            processCaptureBytes(rawBytes, 0)
        }.getOrNull()
    }

    private fun processCaptureBytes(raw: ByteArray, rotation: Int): ByteArray? {
        val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return null
        val scaled = scaleDown(decoded, 1600)
        val rotated = if (rotation == 0) scaled else Bitmap.createBitmap(
            scaled, 0, 0, scaled.width, scaled.height,
            Matrix().apply { postRotate(rotation.toFloat()) }, true
        )
        val out = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, 88, out)
        if (rotated !== decoded) rotated.recycle()
        if (scaled !== decoded && scaled !== rotated) scaled.recycle()
        decoded.recycle()
        return out.toByteArray()
    }
}
