package app.safarinature.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.safarinature.flow.Flow
import app.safarinature.flow.Mode
import app.safarinature.flow.Screen
import sn.core.catalog.Group
import sn.core.catalog.Kingdom
import sn.core.progress.Medals
import sn.core.progress.Sighting
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The celebration: confetti for a new animal, stars landing one by one, a new level or medal if earned. */
@Composable
fun CelebrateScreen(f: Flow) {
    val o = f.outcome ?: return
    val a = o.animal ?: return
    val s = f.celebrated
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFE7B8), C.Paper)))) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (o.firstTime) "Nouvelle découverte !" else if (a.isPlant) "Tu l'as revue !" else "Tu l'as revu !", style = T.Hero.copy(color = C.OrangeLip), textAlign = TextAlign.Center, modifier = Modifier.popIn())
            Spacer(Modifier.height(16.dp))
            val pic = s?.photo?.let { FilePic(f.photoFile(it).path) } ?: refPic(a)
            Box(Modifier.popIn(150)) {
                Picture(pic, Modifier.size(250.dp).shadow(10.dp, CircleShape).clip(CircleShape).border(8.dp, Color.White, CircleShape).background(C.Sand), px = 700) {
                    Text(a.emoji, style = T.Emoji.copy(fontSize = 90.sp, lineHeight = 100.sp))
                }
                Box(Modifier.align(Alignment.BottomEnd).size(76.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Text(a.emoji, style = T.Emoji.copy(fontSize = 40.sp, lineHeight = 46.sp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.name, style = T.Title.copy(fontSize = 30.sp, lineHeight = 34.sp), textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(10.dp))
                Listen(f, if (o.firstTime) "Nouvelle découverte : ${a.name} ! ${a.fact}" else "${a.name}. ${a.fact}")
            }
            Spacer(Modifier.height(10.dp))
            if (o.firstTime) {
                Row {
                    repeat(o.stars) { i -> Text("⭐", style = T.Emoji.copy(fontSize = 44.sp, lineHeight = 52.sp), modifier = Modifier.popIn(500 + i * 280)) }
                }
                Text("+${o.stars} étoile${if (o.stars > 1) "s" else ""}", style = T.Heading.copy(color = C.CautionInk))
            } else {
                Text("Tu l'as ${if (a.isPlant) "vue" else "vu"} ${o.times} fois ! 👏", style = T.Heading.copy(color = C.CautionInk))
            }
            o.levelUp?.let { l ->
                Spacer(Modifier.height(16.dp))
                Paper(Modifier.fillMaxWidth().popIn(1200), color = C.Purple) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(l.emoji, style = T.Emoji.copy(fontSize = 44.sp, lineHeight = 50.sp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Nouveau niveau !", style = T.Label.copy(color = Color.White.copy(alpha = 0.85f)))
                            Text(l.title, style = T.Title.copy(color = Color.White))
                        }
                    }
                }
            }
            for ((i, m) in o.medals.withIndex()) {
                Spacer(Modifier.height(12.dp))
                Paper(Modifier.fillMaxWidth().popIn(1500 + i * 300), color = C.Yellow) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.emoji, style = T.Emoji.copy(fontSize = 44.sp, lineHeight = 50.sp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Nouvelle médaille !", style = T.Label.copy(color = C.CautionInk))
                            Text(m.title, style = T.Title.copy(fontSize = 24.sp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Paper(Modifier.fillMaxWidth()) {
                Column {
                    Text("💡 Le savais-tu ?", style = T.Heading.copy(color = C.PurpleLip, fontSize = 19.sp))
                    Spacer(Modifier.height(4.dp))
                    Text(a.fact, style = T.Body)
                }
            }
            Spacer(Modifier.height(22.dp))
            Chunky("Super !", Modifier.fillMaxWidth(), C.Green, "🎉", f = f) { f.home() }
            Spacer(Modifier.height(12.dp))
            Chunky("Voir sa fiche", Modifier.fillMaxWidth(), C.Blue, "📖", f = f) { f.go(Screen.Card(a.id, Mode.EXPLORE)) }
        }
        if (o.firstTime) Confetti()
    }
}

/** The sticker album: every animal has its place; found ones in colour with the child's own photo. */
@Composable
fun AlbumScreen(f: Flow) {
    val p = f.progress
    // Two albums in one: the animals' and the plants' (a tab each).
    var kingdom by rememberSaveable { mutableStateOf(Kingdom.ANIMAL) }
    var only by rememberSaveable { mutableStateOf<Group?>(null) }
    val list = f.catalog.of(kingdom, g = only)
    val total = f.catalog.of(kingdom).size
    val found = p.discoveredIn(kingdom)
    Column(Modifier.fillMaxSize()) {
        TopBar(f, "Mon album")
        Row(Modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(RoundedCornerShape(50)).background(C.Sand).padding(4.dp)) {
            for (k in Kingdom.entries) {
                val on = k == kingdom
                Text(
                    "${k.emoji} ${k.many.replaceFirstChar { it.uppercase() }}", style = T.Label.copy(fontSize = 18.sp, color = if (on) Color.White else C.Ink), textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (on) (if (k == Kingdom.ANIMAL) C.Orange else C.Green) else Color.Transparent)
                        .clickable(role = Role.Tab) { f.tap(); kingdom = k; only = null }.padding(vertical = 12.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Bar(found.toFloat() / total.coerceAtLeast(1), C.Green, Modifier.weight(1f), 18.dp)
                Spacer(Modifier.width(12.dp))
                Text("$found / $total", style = T.Heading.copy(color = C.GreenLip))
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Tous", if (only == null) C.Ink else Color.White, ink = if (only == null) Color.White else C.Ink) { only = null }
            for (g in Group.entries.filter { it.kingdom == kingdom }) {
                val on = only == g
                Pill("${g.emoji} ${p.discoveredIn(g)}/${f.catalog.of(g = g).size}", if (on) C.of(g) else Color.White, ink = if (on) Color.White else C.Ink) { only = g }
            }
        }
        LazyVerticalGrid(
            GridCells.Adaptive(104.dp), Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (only != null) item(span = { GridItemSpan(maxLineSpan) }) {
                Text(only!!.many, style = T.Heading.copy(color = C.lip(C.of(only!!))))
            }
            items(list, key = { it.id }) { a ->
                val seen = p.seen(a.id)
                Column(
                    Modifier.clip(RoundedCornerShape(18.dp)).background(if (seen) Color.White else Color.White.copy(alpha = 0.55f))
                        .border(if (seen) 3.dp else 1.dp, if (seen) C.of(a.group) else C.Line, RoundedCornerShape(18.dp))
                        .clickable(role = Role.Button) { f.open(a.id, Mode.EXPLORE) }.semantics { contentDescription = a.name },
                ) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                        SpeciesPicture(f, a, Modifier.fillMaxSize(), own = true, faded = !seen, px = 260)
                        if (!seen) Text("?", style = T.Hero.copy(color = Color.White, fontSize = 40.sp), modifier = Modifier.align(Alignment.Center))
                    }
                    Text(
                        a.name, style = T.Small.copy(color = if (seen) C.Ink else C.Soft.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 15.sp),
                        maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun MedalsScreen(f: Flow) {
    val p = f.progress
    val won = Medals.all.count { it.won(p) }
    Column(Modifier.fillMaxSize()) {
        TopBar(f, "Mes médailles") { Pill("$won / ${Medals.all.size}", C.lip(C.Yellow)) }
        LazyVerticalGrid(
            GridCells.Adaptive(160.dp), Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Won first, then the closest to being won.
            items(Medals.all.sortedWith(compareByDescending<sn.core.progress.Medal> { it.won(p) }.thenByDescending { it.value(p).toFloat() / it.target }), key = { it.id }) { m ->
                val ok = m.won(p)
                Column(
                    Modifier.shadow(if (ok) 3.dp else 0.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
                        .background(if (ok) Color.White else Color.White.copy(alpha = 0.6f))
                        .clickable { f.say("${m.title}. ${m.goal}.") }.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(78.dp).clip(CircleShape).background(if (ok) C.Yellow else C.Line).border(5.dp, if (ok) C.lip(C.Yellow) else Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(m.emoji, style = T.Emoji.copy(fontSize = 38.sp, lineHeight = 44.sp), modifier = Modifier.alpha(if (ok) 1f else 0.3f))
                        if (!ok) Text("🔒", style = T.Emoji.copy(fontSize = 20.sp), modifier = Modifier.align(Alignment.BottomEnd))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(m.title, style = T.Label.copy(fontSize = 17.sp), textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                    Text(m.goal, style = T.Small, textAlign = TextAlign.Center, maxLines = 3, minLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    if (ok) Text("Gagnée ! 🎉", style = T.Label.copy(color = C.GreenLip))
                    else Row(verticalAlignment = Alignment.CenterVertically) {
                        Bar(m.value(p).toFloat() / m.target, C.Star, Modifier.weight(1f), 12.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("${m.value(p)}/${m.target}", style = T.Small)
                    }
                }
            }
        }
    }
}

private val dayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

fun dayLabel(d: LocalDate, today: LocalDate): String = when (d) {
    today -> "Aujourd'hui"
    today.minusDays(1) -> "Hier"
    else -> dayFormat.format(d).replaceFirstChar { it.uppercase() }
}

/** Every photo, by day; mystery animals marked, ready to be named. */
@Composable
fun PhotosScreen(f: Flow) {
    val p = f.progress
    val today = f.clock().toLocalDate()
    val days = p.days.map { (d, list) -> d to list.filter { it.photo != null }.reversed() }.filter { it.second.isNotEmpty() }
    Column(Modifier.fillMaxSize()) {
        TopBar(f, "Mes photos")
        if (days.isEmpty()) Column(Modifier.fillMaxSize().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("🖼️", style = T.Emoji.copy(fontSize = 80.sp, lineHeight = 90.sp))
            Text("Pas encore de photo.", style = T.Title, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Appuie sur le gros bouton 📸 pour photographier ton premier animal ou ta première plante !", style = T.Body, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Chunky("Prendre une photo", Modifier.fillMaxWidth(), C.Orange, "📸", f = f) { f.openCamera() }
        } else LazyVerticalGrid(
            GridCells.Adaptive(108.dp), Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (p.mysteries.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Paper(Modifier.fillMaxWidth(), color = Color(0xFFEDE6FF)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🕵️", style = T.Emoji.copy(fontSize = 34.sp))
                        Spacer(Modifier.width(12.dp))
                        Text("${p.mysteries.size} mystère${if (p.mysteries.size > 1) "s" else ""} : touche une photo ❓ pour trouver son nom !", style = T.Label.copy(color = C.PurpleLip), modifier = Modifier.weight(1f))
                    }
                }
            }
            for ((d, list) in days) {
                item(span = { GridItemSpan(maxLineSpan) }, key = d.toString()) {
                    Text(dayLabel(d, today), style = T.Heading, modifier = Modifier.padding(top = 6.dp))
                }
                items(list, key = { it.id }) { s -> PhotoCell(f, s) }
            }
        }
    }
}

@Composable
private fun PhotoCell(f: Flow, s: Sighting) {
    val a = f.catalog[s.animal]
    Box(Modifier.aspectRatio(1f).shadow(2.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).clickable { f.go(Screen.Photo(s.id)) }) {
        Picture(FilePic(f.photoFile(s.photo!!).path), Modifier.fillMaxSize().background(C.Sand), px = 320)
        Text(
            a?.name ?: "❓ Mystère", style = T.Small.copy(color = Color.White, fontSize = 12.sp, lineHeight = 14.sp), maxLines = 2,
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))).padding(6.dp),
        )
    }
}

/** One photo, big. A mystery can be named from here; a photo can be removed (after asking). */
@Composable
fun PhotoScreen(f: Flow, id: Long) {
    val s = f.progress.sightings.firstOrNull { it.id == id } ?: return
    val a = f.catalog[s.animal]
    var asking by remember { mutableStateOf(false) }
    LaunchedEffect(id) { a?.let { f.say(it.name) } }
    Box(Modifier.fillMaxSize().background(Color(0xFF111015))) {
        s.photo?.let { Picture(FilePic(f.photoFile(it).path), Modifier.fillMaxSize(), px = 1400, scale = ContentScale.Fit) }
        Row(Modifier.fillMaxWidth().padding(top = 40.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton("←", "Retour") { f.back() }
            Spacer(Modifier.weight(1f))
            RoundButton("🗑️", "Supprimer la photo", size = 48.dp) { asking = true }
        }
        Paper(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).padding(bottom = 24.dp)) {
            Column {
                Text(a?.name ?: "Mystère", style = T.Title)
                Text(
                    "${dayLabel(s.at.toLocalDate(), f.clock().toLocalDate())} à ${s.at.hour} h ${"%02d".format(s.at.minute)}" + (s.habitat?.let { " · ${it.emoji} ${it.label}" } ?: ""),
                    style = T.Small.copy(fontSize = 15.sp),
                )
                Spacer(Modifier.height(12.dp))
                if (a == null) Chunky("Trouver son nom", Modifier.fillMaxWidth(), C.Purple, "🕵️", f = f) { f.solve(s) }
                else Chunky("Voir sa fiche", Modifier.fillMaxWidth(), C.Blue, "📖", f = f) { f.open(a.id, Mode.EXPLORE) }
            }
        }
    }
    if (asking) AlertDialog(
        onDismissRequest = { asking = false },
        title = { Text("Supprimer cette photo ?", style = T.Title) },
        text = { Text("Elle disparaîtra de ton album.", style = T.Body) },
        confirmButton = { TextButton({ asking = false; f.delete(s) }) { Text("Supprimer", style = T.Label.copy(color = C.Red)) } },
        dismissButton = { TextButton({ asking = false }) { Text("Garder", style = T.Label.copy(color = C.GreenLip)) } },
    )
}
