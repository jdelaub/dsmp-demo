package app.safarinature.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import app.safarinature.R
import sn.core.catalog.Group
import sn.core.catalog.Habitat

/**
 * A sunny sticker-book look: cream paper, one strong colour per place and per family, chunky buttons
 * with a darker "lip" so they look pressable. Always light: it is used outdoors, in daylight.
 */
object C {
    val Paper = Color(0xFFFFF6E5)
    val Card = Color.White
    val Ink = Color(0xFF2E2A36)
    val Soft = Color(0xFF6B6577)
    val Line = Color(0xFFEDE3CF)
    val Sand = Color(0xFFFCEBC9)

    val Orange = Color(0xFFFF7A3D)
    val OrangeLip = Color(0xFFD65A22)
    val Green = Color(0xFF3DB45A)
    val GreenLip = Color(0xFF2A8A42)
    val Blue = Color(0xFF3D9BE9)
    val BlueLip = Color(0xFF2675BD)
    val Yellow = Color(0xFFFFC83D)
    val YellowLip = Color(0xFFD9A11C)
    val Pink = Color(0xFFFF6FA5)
    val Purple = Color(0xFF8E6CEF)
    val PurpleLip = Color(0xFF6A4BC9)
    val Red = Color(0xFFE5484D)
    val Star = Color(0xFFFFB800)
    val Caution = Color(0xFFFFF0C2)
    val CautionInk = Color(0xFF8A5300)

    fun of(h: Habitat): Color = when (h) {
        Habitat.FOREST -> Color(0xFF3E9B4F)
        Habitat.FIELDS -> Color(0xFFE0A91B)
        Habitat.CITY -> Color(0xFFF2803A)
        Habitat.WATER -> Color(0xFF22A9B8)
        Habitat.SEA -> Color(0xFF2F86DE)
        Habitat.MOUNTAIN -> Color(0xFF8C72E0)
        Habitat.FARM -> Color(0xFFE0645A)
    }

    fun of(g: Group): Color = when (g) {
        Group.MAMMAL -> Color(0xFFE07A3E)
        Group.BIRD -> Color(0xFF3D9BE9)
        Group.INSECT -> Color(0xFFE5484D)
        Group.SPIDER -> Color(0xFF7A6A8F)
        Group.REPTILE -> Color(0xFF58A846)
        Group.AMPHIBIAN -> Color(0xFF2EAF8B)
        Group.FISH -> Color(0xFF2F78C8)
        Group.MOLLUSC -> Color(0xFFC9852F)
        Group.CRUSTACEAN -> Color(0xFFE2603F)
        Group.OTHER -> Color(0xFF9B62D9)
        Group.TREE -> Color(0xFF2E8B57)
        Group.SHRUB -> Color(0xFF6B9E3A)
        Group.FRUIT -> Color(0xFFD94A5A)
        Group.FLOWER -> Color(0xFFD9669F)
        Group.SEAWEED -> Color(0xFF7C8A3A)
        Group.FERN -> Color(0xFF4F9A6B)
        Group.CROP -> Color(0xFFE39A2D)
    }

    /** The darker edge under a coloured block. */
    fun lip(c: Color) = Color(c.red * 0.78f, c.green * 0.78f, c.blue * 0.78f, c.alpha)
}

val Fredoka = FontFamily(
    Font(R.font.fredoka_regular, FontWeight.Normal),
    Font(R.font.fredoka_medium, FontWeight.Medium),
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
    Font(R.font.fredoka_bold, FontWeight.Bold),
)

/** Big letters: many readers are 6. */
object T {
    val Hero = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, color = C.Ink)
    val Title = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, color = C.Ink)
    val Heading = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 26.sp, color = C.Ink)
    val Button = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp, color = Color.White)
    val Body = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 25.sp, color = C.Ink)
    val Label = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp, color = C.Ink)
    val Small = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp, color = C.Soft)
    val Emoji = TextStyle(fontSize = 30.sp, lineHeight = 36.sp)
}

@Composable
fun SafariTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        (view.context as? android.app.Activity)?.window?.let { w ->
            WindowCompat.getInsetsController(w, view).run { isAppearanceLightStatusBars = true; isAppearanceLightNavigationBars = true }
        }
    }
    val scheme = lightColorScheme(
        primary = C.Orange, onPrimary = Color.White, secondary = C.Green, background = C.Paper, onBackground = C.Ink,
        surface = C.Card, onSurface = C.Ink, surfaceVariant = C.Sand, onSurfaceVariant = C.Soft, outline = C.Line,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalContentColor provides C.Ink, content = content)
    }
}
