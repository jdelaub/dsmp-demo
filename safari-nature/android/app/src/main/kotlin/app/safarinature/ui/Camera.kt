package app.safarinature.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.safarinature.flow.Flow
import kotlinx.coroutines.launch

private fun cameraAllowed(ctx: Context) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/**
 * The child's camera: the picture fills the screen, one big round button, zoom in big steps for shy birds.
 * Nothing else to tap by mistake. A grown-up allows the camera once; the gallery and "no photo" stay possible.
 */
@Composable
fun CameraScreen(f: Flow, target: String?) {
    val ctx = LocalContext.current
    val hasCamera = remember { ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var allowed by remember { mutableStateOf(cameraAllowed(ctx)) }
    var asked by remember { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it; asked = true }
    LaunchedEffect(Unit) { if (hasCamera && !allowed) ask.launch(Manifest.permission.CAMERA) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) f.photoPicked(uri, target) }
    val gallery = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val animal = f.catalog[target]

    Box(Modifier.fillMaxSize().background(Color(0xFF111015))) {
        if (hasCamera && allowed) LiveCamera(f, target, gallery)
        else Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("📷", style = T.Emoji.copy(fontSize = 80.sp, lineHeight = 90.sp))
            Spacer(Modifier.height(12.dp))
            Text(
                if (!hasCamera) "Ce téléphone n'a pas d'appareil photo." else "Pour prendre des photos, demande à un adulte d'appuyer sur « Autoriser ».",
                style = T.Heading.copy(color = Color.White), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            if (hasCamera) Chunky("Autoriser l'appareil photo", Modifier.fillMaxWidth(), C.Orange, "🔓", f = f) {
                if (asked) runCatching {
                    ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", ctx.packageName, null)))
                } else ask.launch(Manifest.permission.CAMERA)
            }
            Spacer(Modifier.height(14.dp))
            Chunky("Choisir dans la galerie", Modifier.fillMaxWidth(), C.Blue, "🖼️", f = f) { gallery() }
            Spacer(Modifier.height(14.dp))
            if (animal == null) Chunky("Continuer sans photo", Modifier.fillMaxWidth(), C.Purple, "👀", f = f) { f.withoutPhoto() }
            else Chunky("Je l'ai vu sans photo", Modifier.fillMaxWidth(), C.Purple, "👀", f = f) { f.seenWithoutPhoto(animal.id) }
        }

        // Top: close, and what we are photographing.
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton("✕", "Fermer", color = Color.White.copy(alpha = 0.9f)) { f.back() }
            Spacer(Modifier.width(12.dp))
            Text(
                animal?.let { "Photographie : ${it.name}" } ?: "Vise l'animal !",
                style = T.Heading.copy(color = Color.White), modifier = Modifier.weight(1f),
            )
        }

        if (f.cameraTip) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.82f)).clickable { f.hideCameraTip() }, contentAlignment = Alignment.Center) {
            Paper(Modifier.padding(28.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🤫", style = T.Emoji.copy(fontSize = 56.sp, lineHeight = 64.sp))
                    Text("La règle de l'explorateur", style = T.Title, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("Approche doucement, sans bruit. On regarde, on photographie… mais on ne touche pas et on ne dérange pas les animaux !", style = T.Body, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Listen(f, "La règle de l'explorateur. Approche doucement, sans bruit. On regarde, on photographie, mais on ne touche pas et on ne dérange pas les animaux !")
                        Spacer(Modifier.width(12.dp))
                        Chunky("Promis !", Modifier.weight(1f), C.Green, "🤝", f = f) { f.hideCameraTip() }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCamera(f: Flow, target: String?, gallery: () -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val controller = remember {
        LifecycleCameraController(ctx).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }
    }
    // Bound while this screen shows: leaving it turns the camera off.
    DisposableEffect(owner) {
        controller.bindToLifecycle(owner)
        onDispose { controller.unbind() }
    }
    var zoom by remember { mutableFloatStateOf(1f) }
    var front by remember { mutableStateOf(false) }
    var shooting by remember { mutableStateOf(false) }
    val flash = remember { Animatable(0f) }

    AndroidView({ PreviewView(it).apply { this.controller = controller; scaleType = PreviewView.ScaleType.FILL_CENTER } }, Modifier.fillMaxSize())
    Box(Modifier.fillMaxSize().alpha(flash.value).background(Color.White))

    Column(
        Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 18.dp),
        verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Zoom in big, easy steps.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (z in listOf(1f, 2f, 4f)) {
                val on = zoom == z
                Text(
                    "${z.toInt()}×", style = T.Label.copy(color = if (on) C.Ink else Color.White, fontSize = 18.sp),
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(if (on) C.Yellow else Color.Black.copy(alpha = 0.45f))
                        .clickable(role = Role.Button) {
                            val max = controller.zoomState.value?.maxZoomRatio ?: 1f
                            zoom = z; controller.setZoomRatio(z.coerceAtMost(max))
                        }
                        .padding(top = 14.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 26.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RoundButton("🖼️", "Galerie", color = Color.White.copy(alpha = 0.9f), size = 60.dp) { gallery() }
                    Text("Galerie", style = T.Small.copy(color = Color.White))
                }
                // The shutter: a big white ring.
                Box(
                    Modifier.size(104.dp).clip(CircleShape).border(7.dp, Color.White, CircleShape).padding(12.dp).clip(CircleShape)
                        .background(if (shooting) C.Yellow else C.Orange)
                        .clickable(enabled = !shooting, role = Role.Button) {
                            shooting = true
                            f.voice.shutter()
                            scope.launch { flash.snapTo(0.9f); flash.animateTo(0f, tween(350)) }
                            val file = f.photos.fresh()
                            controller.takePicture(
                                ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(ctx),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(r: ImageCapture.OutputFileResults) { shooting = false; f.photoTaken(file, target) }
                                    override fun onError(e: ImageCaptureException) { shooting = false; f.toast = "Oups, la photo n'a pas marché. Réessaie !" }
                                },
                            )
                        }
                        .semantics { contentDescription = "Prendre la photo" },
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RoundButton("🔄", "Changer de caméra", color = Color.White.copy(alpha = 0.9f), size = 60.dp) {
                        val next = if (front) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                        if (controller.hasCamera(next)) { controller.cameraSelector = next; front = !front; zoom = 1f }
                    }
                    Text("Tourner", style = T.Small.copy(color = Color.White))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        val animal = f.catalog[target]
        Text(
            "👀 Pas de photo ? Continuer", style = T.Label.copy(color = Color.White),
            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.35f))
                .clickable { if (animal != null) f.seenWithoutPhoto(animal.id) else f.withoutPhoto() }.padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

/** The photo just taken, big: keep it or take it again. */
@Composable
fun ReviewScreen(f: Flow) {
    val p = f.pending ?: return
    val animal = f.catalog[p.target]
    Column(Modifier.fillMaxSize().background(C.Ink).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Elle est bien, ta photo ?", style = T.Title.copy(color = Color.White), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
        Picture(
            p.photo?.let { FilePic(f.photoFile(it).path) },
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.Black).popIn(),
            px = 1200, scale = androidx.compose.ui.layout.ContentScale.Fit,
        )
        Spacer(Modifier.height(18.dp))
        Chunky(if (animal != null) "Garder pour : ${animal.name}" else "Oui, je la garde !", Modifier.fillMaxWidth(), C.Green, "✅", f = f) { f.keepPhoto() }
        Spacer(Modifier.height(12.dp))
        Chunky("Reprendre la photo", Modifier.fillMaxWidth(), C.Blue, "🔄", f = f) { f.back() }
    }
}
