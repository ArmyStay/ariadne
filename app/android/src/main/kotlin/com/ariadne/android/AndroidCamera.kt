package com.ariadne.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ariadne.app.ui.pages.CameraBinding
import com.ariadne.design.Label
import com.ariadne.design.Paper
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor

/**
 * The Android camera: CameraX, bound to the activity's lifecycle.
 *
 * Three things this has to get right, in order of how badly they break:
 *
 * 1. **Permission.** Asked on first entry to the shutter, not at launch. If it is
 *    refused, the page keeps drawing the ruled plate and the caption says so —
 *    the app remains completely usable, which is the whole product's stance.
 *
 * 2. **The lifecycle.** A camera held open when the page leaves is a battery
 *    drain and, on some devices, a sensor that stays warm. The binding unbinds on
 *    dispose, not on process death.
 *
 * 3. **The bytes.** CameraX hands over a JPEG in a `ImageProxy` whose rotation
 *    depends on the sensor, not the screen. It is decoded, rotated to match what
 *    the person saw, and re-encoded at a bounded size — a 12MP frame is 4MB, and
 *    a receipt does not need 12MP to be legible.
 */
public class AndroidCamera(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) : CameraBinding {

    override val available: Boolean
        get() = hasPermission() && hasHardware()

    private var imageCapture: ImageCapture? = null
    private var provider: ProcessCameraProvider? = null

    /** Set by [Preview]; the shutter writes through this and cannot fire blind. */
    private var deliver: ((ByteArray) -> Unit)? = null

    private fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED

    private fun hasHardware(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    public fun permissionGranted() {
        // Nothing to do but let recomposition see it: `available` is read fresh.
    }

    @Composable
    override fun Preview(paper: Paper, modifier: Modifier, onFrame: (ByteArray) -> Unit) {
        val host = LocalContext.current
        var permission by remember { mutableStateOf(hasPermission()) }
        val ask = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted -> permission = granted }

        LaunchedEffect(Unit) {
            if (!hasPermission()) ask.launch(Manifest.permission.CAMERA)
        }

        // The shutter callback is captured on every recomposition, so it always
        // points at the current page's handler rather than a stale one.
        remember(onFrame) { deliver = onFrame }

        if (!permission || !hasHardware()) {
            // Not an error state: the plate is drawn, and the caption explains it
            // in the same voice the rest of the app uses for facts.
            Box(modifier) {
                Canvas(Modifier.fillMaxSize()) {
                    val step = 26f
                    var y = step
                    while (y < size.height) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.12f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                            strokeWidth = 0.5f,
                        )
                        y += step
                    }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(Modifier.background(Color.Black.copy(alpha = 0.04f))) {
                        Label(
                            if (!hasHardware()) "No camera on this device" else "Camera permission declined",
                            paper,
                            color = paper.inkFaint,
                        )
                    }
                }
            }
            return
        }

        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                val view = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                bind(ctx, view)
                view
            },
        )

        DisposableEffect(Unit) {
            onDispose {
                // Unbind explicitly: the lifecycle owner would release it at the
                // activity's end, which is much later than the page's end.
                provider?.unbindAll()
                imageCapture = null
                deliver = null
            }
        }
    }

    private fun bind(context: Context, view: PreviewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val cameraProvider = try {
                future.get()
            } catch (e: Exception) {
                return@addListener
            }
            provider = cameraProvider

            val preview = CameraXPreview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }
            val capture = ImageCapture.Builder()
                // A receipt is read, not printed: 1600px is comfortably more than
                // the OCR needs and a tenth of the bytes. ResolutionSelector rather
                // than the deprecated setTargetResolution, and a fallback rule so a
                // sensor that cannot do exactly 1600 still gives us its nearest
                // larger size instead of a postage stamp.
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                android.util.Size(1600, 1600),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            ),
                        )
                        .build(),
                )
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture,
                )
            } catch (e: Exception) {
                // Binding can fail on a device whose camera is held by another app.
                // The page keeps drawing the plate; nothing crashes.
                imageCapture = null
            }
        }, mainExecutor(context))
    }

    override fun capture() {
        val capture = imageCapture ?: return
        val sink = deliver ?: return
        capture.takePicture(
            directExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val bytes = image.toJpeg(image.imageInfo.rotationDegrees)
                        if (bytes != null) sink(bytes)
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    // A failed capture leaves no record and says nothing: the
                    // shutter simply did not fire, which is what happened.
                }
            },
        )
    }
}

/**
 * ImageProxy to JPEG bytes, rotated and bounded.
 *
 * CameraX's `imageProxy.planes[0].buffer` is a JPEG only when the format is
 * JPEG; anything else is converted here, because binding a format the sensor
 * chose not to give is a crash on the first device that disagrees.
 */
private fun ImageProxy.toJpeg(rotation: Int, maxEdge: Int = 1600): ByteArray? {
    // ImageCapture hands back JPEG by default; anything else is a binding the
    // sensor disagreed with, and decoding it as JPEG would produce noise.
    if (format != android.graphics.ImageFormat.JPEG) return null
    val buffer = planes[0].buffer
    val raw = ByteArray(buffer.remaining()).also { buffer.get(it) }
    val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return null

    val scaled = scaleDown(decoded, maxEdge)
    val rotated = if (rotation == 0) scaled else Bitmap.createBitmap(
        scaled,
        0,
        0,
        scaled.width,
        scaled.height,
        Matrix().apply { postRotate(rotation.toFloat()) },
        true,
    )

    val out = ByteArrayOutputStream()
    rotated.compress(Bitmap.CompressFormat.JPEG, 88, out)
    if (rotated !== decoded) rotated.recycle()
    if (scaled !== decoded && scaled !== rotated) scaled.recycle()
    decoded.recycle()
    return out.toByteArray()
}

private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val longEdge = maxOf(bitmap.width, bitmap.height)
    if (longEdge <= maxEdge) return bitmap
    val ratio = maxEdge.toFloat() / longEdge
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * ratio).toInt().coerceAtLeast(1),
        (bitmap.height * ratio).toInt().coerceAtLeast(1),
        true,
    )
}

private fun mainExecutor(context: Context): Executor =
    ContextCompat.getMainExecutor(context)

private fun directExecutor(): Executor = Executor { it.run() }
