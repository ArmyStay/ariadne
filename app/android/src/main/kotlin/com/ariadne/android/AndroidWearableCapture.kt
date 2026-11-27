package com.ariadne.android

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.ariadne.app.ui.pages.WearableCapture
import com.meta.wearable.dat.camera.Camera
import com.meta.wearable.dat.camera.addCamera
import com.meta.wearable.dat.camera.types.PhotoData
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.nio.ByteBuffer

/**
 * The glasses, through Meta's Wearables Device Access Toolkit.
 *
 * The whole point of the app is that a photograph is the note. On glasses you do not
 * even take the phone out: press the shutter in the app (the glasses' own button is
 * not available to a DAT app) and the picture the glasses saw is the one that is
 * filed, on the day it was taken.
 *
 * The pairing is one tap and one trip to Meta AI, and it happens once: [connect] opens
 * Meta AI's registration, and then waits for the SDK to hand back a session. Nothing
 * here is invented — every call is the documented DAT call, in the documented order —
 * but it has not been run against a pair of glasses from this machine.
 *
 * Every call is wrapped, because a missing app registration, glasses that are asleep,
 * or a phone without the Meta AI app must be a sentence in the interface, never a
 * crash and never a shutter that hangs.
 */
public class AndroidWearableCapture(
    private val context: android.content.Context,
    startMockServer: Boolean = false,
) : WearableCapture {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activity: WeakReference<Activity>? = null
    private var session: DeviceSession? = null
    private var camera: Camera? = null

    /** What the connect panel prints. One word at a time, in the app's voice. */
    public var connectionState: String by mutableStateOf("Not set up")
        private set

    override val status: String get() = connectionState

    override val available: Boolean = true

    override val connected: Boolean get() = session != null && camera != null

    init {
        // The mock device (debug builds) needs the same entry point as a real pair.
        if (startMockServer) runCatching { enableMock() }
    }

    /** MainActivity hands itself over so registration has an activity to deep-link into. */
    public fun attach(host: Activity) {
        activity = WeakReference(host)
    }

    private fun host(): Activity? = activity?.get()

    override fun connect() {
        val host = host()
        if (host == null) {
            connectionState = "Open Ariadne on the phone"
            return
        }
        if (ContextCompat.checkSelfPermission(host, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            connectionState = "Bluetooth permission needed"
            runCatching {
                host.requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 0x0DA7)
            }
            return
        }

        // One tap, one trip to Meta AI, and glasses arrive on the way back.
        connectionState = "Pairing in Meta AI"
        runCatching { Wearables.startRegistration(host) }

        scope.launch {
            // Registration finishes in Meta AI, so there is no callback here: the
            // session becomes creatable a moment later, and is waited for.
            repeat(45) {
                if (session != null) return@launch
                val created = Wearables.createSession(AutoDeviceSelector()).getOrElse { null }
                if (created != null) {
                    startSession(created)
                    return@launch
                }
                delay(2000)
            }
            connectionState = "No glasses answered"
        }
    }

    override fun disconnect() {
        runCatching { camera?.stream?.stop() }
        runCatching { session?.stop() }
        camera = null
        session = null
        connectionState = "Not paired"
    }

    /**
     * One photograph, from the glasses.
     *
     * Returns null when there is no stream or the glasses refused — the caller treats
     * a null as *nothing was taken* and leaves the day alone.
     */
    override suspend fun capture(): ByteArray? = withContext(Dispatchers.IO) {
        val stream = camera?.stream ?: return@withContext null
        runCatching {
            var bytes: ByteArray? = null
            stream.capturePhoto().fold(
                onSuccess = { photo ->
                    bytes = when (photo) {
                        is PhotoData.Bitmap -> photo.bitmap.toJpeg()
                        is PhotoData.HEIC -> photo.data.asBytes()
                        else -> null
                    }
                },
                onFailure = { _, _ -> bytes = null },
            )
            bytes
        }.getOrNull()
    }

    // ── the session ────────────────────────────────────────────────────────────

    private fun startSession(created: DeviceSession) {
        session = created
        connectionState = "Connecting"
        scope.launch {
            created.state.collect { state ->
                when (state) {
                    DeviceSessionState.STARTED -> attachCamera(created)
                    DeviceSessionState.STOPPED -> {
                        camera = null
                        session = null
                        connectionState = "Not paired"
                    }
                    else -> Unit
                }
            }
        }
        runCatching { created.start() }
    }

    @Suppress("UNUSED_PARAMETER")
    private suspend fun attachCamera(active: DeviceSession) {
        if (camera != null) return
        active.addCamera(
            StreamConfiguration(videoQuality = VideoQuality.MEDIUM, frameRate = 24),
        ).fold(
            onSuccess = { attached ->
                camera = attached
                val started = attached.stream.start()
                started.onFailure { _, _ -> connectionState = "Stream refused" }
                connectionState = "Connected"
            },
            onFailure = { _, _ -> connectionState = "Camera refused" },
        )
    }

    private fun enableMock() {
        // MockDeviceKit is debug-only; the mock server is started by the intent extra
        // `mock_glasses`, and is a no-op in a release build.
        connectionState = "Not set up"
    }

    private fun Bitmap.toJpeg(): ByteArray? = runCatching {
        val out = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 88, out)
        out.toByteArray()
    }.getOrNull()

    private fun ByteBuffer.asBytes(): ByteArray {
        val copy = duplicate()
        val bytes = ByteArray(copy.remaining())
        copy.get(bytes)
        return bytes
    }
}
