package app.safarinature.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.safarinature.data.Photos
import app.safarinature.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sn.core.catalog.Animal
import kotlin.random.Random

// ——— Pictures ———

/** A reference photo shipped in the app, or one of the child's. */
sealed interface Pic { val key: String }
data class AssetPic(val path: String) : Pic { override val key get() = "a:$path" }
data class FilePic(val path: String) : Pic { override val key get() = "f:$path" }

fun refPic(a: Animal) = AssetPic("photos/${a.id}.webp")

object Pictures {
    private val cache = object : LruCache<String, ImageBitmap>(40 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    fun cached(p: Pic, px: Int): ImageBitmap? = cache.get("${p.key}@$px")

    suspend fun load(ctx: Context, p: Pic, px: Int): ImageBitmap? = cached(p, px) ?: withContext(Dispatchers.IO) {
        runCatching {
            val bmp = when (p) {
                is AssetPic -> Photos.decode(px) { o -> ctx.assets.open(p.path).use { BitmapFactory.decodeStream(it, null, o) } }
                is FilePic -> Photos.decode(p.path, px)
            }
            bmp?.asImageBitmap()?.also { cache.put("${p.key}@$px", it) }
        }.getOrNull()
    }

    fun forget(p: Pic) = cache.snapshot().keys.filter { it.startsWith(p.key) }.forEach { cache.remove(it) }
}

private val grey = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** A photo filling its box, loaded off the main thread; [fallback] shows while it loads or if it is missing. */
@Composable
fun Picture(
    pic: Pic?,
    modifier: Modifier = Modifier,
    px: Int = 640,
    faded: Boolean = false,
    scale: ContentScale = ContentScale.Crop,
    fallback: @Composable BoxScope.() -> Unit = {},
) {
    val ctx = LocalContext.current
    val img by produceState(pic?.let { Pictures.cached(it, px) }, pic, px) {
        if (pic != null && value == null) value = Pictures.load(ctx, pic, px)
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val i = img
        if (i == null) fallback()
        else Image(i, null, Modifier.fillMaxSize(), contentScale = scale,
            colorFilter = if (faded) grey else null, alpha = if (faded) 0.42f else 1f)
    }
}

/** The animal's picture: the child's own photo when there is one, otherwise the reference photo. */
@Composable
fun AnimalPicture(f: Flow, a: Animal, modifier: Modifier = Modifier, own: Boolean = true, faded: Boolean = false, px: Int = 480) {
    val mine = if (own) f.progress.lastPhotoOf(a.id) else null
    Picture(mine?.let { FilePic(f.photoFile(it).path) } ?: refPic(a), modifier.background(C.of(a.group).copy(alpha = 0.18f)), px, faded) {
        Text(a.emoji, style = T.Emoji.copy(fontSize = 40.sp))
    }
}

// ——— Buttons ———

/**
 * The app's button: a fat coloured block on a darker lip that sinks when pressed, with a little "pop".
 * At least 64 dp tall: small fingers, outdoors, sometimes gloves.
 */
@Composable
fun Chunky(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = C.Orange,
    emoji: String? = null,
    textColor: Color = Color.White,
    height: Dp = 64.dp,
    style: TextStyle = T.Button,
    f: Flow? = null,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val lip = 5.dp
    Box(
        modifier
            .height(height + lip)
            .clip(RoundedCornerShape(22.dp))
            .background(C.lip(color))
            .clickable(src, null, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                f?.tap()
                onClick()
            }
            .semantics { contentDescription = text },
    ) {
        Row(
            Modifier.fillMaxWidth().height(height).offset(y = if (pressed) lip else 0.dp)
                .clip(RoundedCornerShape(22.dp)).background(color).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (emoji != null) { Text(emoji, style = T.Emoji.copy(fontSize = 26.sp)); Spacer(Modifier.width(10.dp)) }
            Text(text, style = style.copy(color = textColor), maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A round button with one symbol: back, listen, close. */
@Composable
fun RoundButton(symbol: String, label: String, modifier: Modifier = Modifier, color: Color = C.Card, size: Dp = 56.dp, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier.size(size).shadow(3.dp, CircleShape).clip(CircleShape).background(color)
            .clickable(role = Role.Button) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Text(symbol, style = T.Title.copy(fontSize = (size.value * 0.46f).sp, lineHeight = (size.value * 0.55f).sp)) }
}

/** 🔊: reads [text] aloud. */
@Composable
fun Listen(f: Flow, text: String, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    if (!f.voiceOn) return
    RoundButton("🔊", "Écouter", modifier, C.Sand, size) { f.say(text) }
}

/** The bar at the top of every screen: back, title, and an optional extra on the right. */
@Composable
fun TopBar(f: Flow, title: String, color: Color = C.Ink, speak: String? = null, right: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundButton("←", "Retour") { f.back() }
        Spacer(Modifier.width(14.dp))
        Text(title, style = T.Title.copy(color = color), modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (speak != null) { Spacer(Modifier.width(8.dp)); Listen(f, speak) }
        right()
    }
}

/**
 * A big coloured square to pick a place or a family: an emoji in a white disc, a label, a counter.
 * [mark] puts a small badge in the corner (📍 the place chosen last time, ✓ done).
 */
@Composable
fun Tile(emoji: String, label: String, color: Color, modifier: Modifier = Modifier, sub: String? = null, mark: String? = null, f: Flow? = null, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "tile")
    Box(modifier.scale(s)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(C.lip(color)).padding(bottom = 5.dp)
                .clip(RoundedCornerShape(26.dp)).background(color)
                .clickable(src, null, role = Role.Button) { f?.tap(); onClick() }
                .padding(vertical = 16.dp, horizontal = 10.dp)
                .semantics { contentDescription = label },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                Text(emoji, style = T.Emoji.copy(fontSize = 38.sp, lineHeight = 44.sp))
            }
            Spacer(Modifier.height(10.dp))
            Text(label, style = T.Heading.copy(color = Color.White, fontSize = 19.sp, lineHeight = 22.sp), textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
            if (sub != null) Text(sub, style = T.Small.copy(color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp), textAlign = TextAlign.Center)
        }
        if (mark != null) Box(
            Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-6).dp).size(36.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center,
        ) { Text(mark, style = T.Emoji.copy(fontSize = 18.sp, lineHeight = 22.sp)) }
    }
}

/** A white card with rounded corners and a soft shadow. */
@Composable
fun Paper(modifier: Modifier = Modifier, color: Color = C.Card, shape: Shape = RoundedCornerShape(24.dp), padding: Dp = 16.dp, content: @Composable () -> Unit) {
    Box(modifier.shadow(2.dp, shape).clip(shape).background(color).padding(padding)) { content() }
}

@Composable
fun Stars(n: Int, modifier: Modifier = Modifier, size: Int = 20, outOf: Int = 0) {
    Row(modifier) {
        repeat(n) { Text("⭐", style = T.Emoji.copy(fontSize = size.sp, lineHeight = (size + 4).sp)) }
        // Colour emoji ignore the text colour: the missing stars are faded instead.
        repeat((outOf - n).coerceAtLeast(0)) { Text("⭐", style = T.Emoji.copy(fontSize = size.sp, lineHeight = (size + 4).sp), modifier = Modifier.alpha(0.18f)) }
    }
}

/** A fat, rounded progress bar. */
@Composable
fun Bar(value: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 16.dp, track: Color = C.Line) {
    val v by animateFloatAsState(value.coerceIn(0f, 1f), tween(700), label = "bar")
    Box(modifier.height(height).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(v).clip(RoundedCornerShape(50)).background(color))
    }
}

/** A small rounded label. */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, ink: Color = Color.White, onClick: (() -> Unit)? = null) {
    Text(
        text, style = T.Label.copy(color = ink, fontSize = 15.sp), maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(50)).background(color)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** A gentle, endless "breathing": the one thing to press on a screen. */
@Composable
fun Modifier.breathing(): Modifier {
    val t = rememberInfiniteTransition(label = "breathe")
    val s by t.animateFloat(1f, 1.06f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")
    return this.scale(s)
}

/** Pops in with a bounce the first time it is drawn. */
@Composable
fun Modifier.popIn(delayMs: Int = 0): Modifier {
    val a = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        a.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f))
    }
    return this.scale(a.value)
}

/** Paper confetti raining over the screen for a few seconds. */
@Composable
fun Confetti(modifier: Modifier = Modifier, seconds: Float = 3.5f) {
    val colors = listOf(C.Orange, C.Yellow, C.Green, C.Blue, C.Pink, C.Purple)
    val bits = remember {
        val r = Random(7)
        List(90) { Bit(r.nextFloat(), -0.05f - r.nextFloat() * 0.5f, 0.18f + r.nextFloat() * 0.3f, (r.nextFloat() - 0.5f) * 0.12f, r.nextFloat() * 360f, 120f + r.nextFloat() * 360f, colors[it % colors.size]) }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(seconds, tween((seconds * 1000).toInt(), easing = { it })) }
    if (t.value >= seconds) return
    Canvas(modifier.fillMaxSize()) {
        val time = t.value
        for (b in bits) {
            val x = (b.x + b.drift * time) * size.width
            val y = (b.y + b.fall * time + 0.03f * time * time) * size.height
            if (y > size.height || y < -40f) continue
            rotate(b.spin + b.turn * time, Offset(x, y)) {
                drawRect(b.color, Offset(x - 9f, y - 5f), Size(18f, 10f))
            }
        }
    }
}

private class Bit(val x: Float, val y: Float, val fall: Float, val drift: Float, val spin: Float, val turn: Float, val color: Color)

/** A bubble of text at the top of the screen for a moment. */
@Composable
fun Toast(f: Flow, modifier: Modifier = Modifier) {
    val text = f.toast ?: return
    LaunchedEffect(text) { kotlinx.coroutines.delay(2800); f.toast = null }
    Box(modifier.fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            text, style = T.Label.copy(color = Color.White, fontSize = 17.sp), textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp).shadow(6.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
                .background(C.Ink).padding(horizontal = 20.dp, vertical = 14.dp).popIn(),
        )
    }
}

/** The ring around an avatar emoji. */
@Composable
fun Avatar(emoji: String, size: Dp = 56.dp, ring: Color = C.Orange, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(Color.White).border(4.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
        Text(emoji, style = T.Emoji.copy(fontSize = (size.value * 0.5f).sp, lineHeight = (size.value * 0.6f).sp))
    }
}

/** A filling screen with the cream paper behind. */
@Composable
fun Page(modifier: Modifier = Modifier, color: Color = C.Paper, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(color), content = content)
}
