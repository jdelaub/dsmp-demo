package app.safarinature.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.safarinature.flow.Flow
import app.safarinature.flow.Mode
import app.safarinature.flow.Screen
import sn.core.catalog.Species
import sn.core.catalog.Group
import sn.core.catalog.Habitat
import sn.core.catalog.Kingdom
import sn.core.catalog.counted

/** While naming: the child's photo stays in sight, to compare with the pictures. */
@Composable
fun PhotoStrip(f: Flow, question: String) {
    val photo = f.pending?.photo
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (photo != null) {
            Picture(FilePic(f.photoFile(photo).path), Modifier.size(78.dp).shadow(3.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).border(3.dp, Color.White, RoundedCornerShape(18.dp)), px = 300)
            Spacer(Modifier.width(14.dp))
        }
        Text(question, style = T.Heading, modifier = Modifier.weight(1f))
        Listen(f, question)
    }
}

/** The questions, in the words of what the child found. */
private fun whereQuestion(k: Kingdom?) = when (k) {
    Kingdom.ANIMAL -> "Où as-tu vu cet animal ?"
    Kingdom.PLANT -> "Où as-tu trouvé cette plante ?"
    null -> "Où étais-tu ?"
}

private fun familyQuestion(k: Kingdom?) = when (k) {
    Kingdom.ANIMAL -> "C'est quel genre d'animal ?"
    Kingdom.PLANT -> "C'est quel genre de plante ?"
    null -> "C'est de quelle famille ?"
}

private fun whichQuestion(k: Kingdom?) = when (k) {
    Kingdom.ANIMAL -> "Lequel ressemble à ton animal ?"
    Kingdom.PLANT -> "Laquelle ressemble à ta plante ?"
    null -> "Lequel ressemble à ta photo ?"
}

/** The "I don't know" button that keeps the photo as a mystery, while a new photo waits for its name. */
@Composable
private fun MysteryButton(f: Flow) {
    if (f.pending?.photo != null && f.pending?.solving == null) {
        Chunky("Je ne sais pas ce que c'est", Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), C.Purple, "❓", f = f) { f.keepMystery() }
    }
}

/** First question: an animal or a plant? Two big doors, and "I don't know" for both. */
@Composable
fun KindsScreen(f: Flow, mode: Mode) {
    val identify = mode == Mode.IDENTIFY
    val p = f.progress
    Column(Modifier.fillMaxSize()) {
        TopBar(f, if (identify) "Qu'as-tu trouvé ?" else "Explorer") {
            RoundButton("🔎", "Chercher", size = 48.dp, color = C.Sand) { f.go(Screen.Search(mode)) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 16.dp)) {
            if (identify) PhotoStrip(f, "C'est un animal ou une plante ?")
            else Text("Que veux-tu découvrir ?", style = T.Body.copy(color = C.Soft))
            Spacer(Modifier.height(14.dp))
            for (k in Kingdom.entries) {
                val all = f.catalog.of(k)
                Tile(
                    if (k == Kingdom.ANIMAL) "🦊" else "🌳", k.label, if (k == Kingdom.ANIMAL) C.Orange else C.Green, Modifier.fillMaxWidth(),
                    if (identify) counted(all.size, k) else "✓ ${all.count { p.seen(it.id) }} / ${counted(all.size, k)}", f = f,
                ) { f.chooseKind(mode, k) }
                Spacer(Modifier.height(14.dp))
            }
            Tile("🧭", if (identify) "Je ne sais pas" else "Les deux", C.Soft, Modifier.fillMaxWidth(), counted(f.catalog.all.size, null), f = f) {
                f.chooseKind(mode, null)
            }
        }
        if (identify) MysteryButton(f)
    }
}

@Composable
fun PlacesScreen(f: Flow, mode: Mode, k: Kingdom?) {
    val identify = mode == Mode.IDENTIFY
    val p = f.progress
    Column(Modifier.fillMaxSize()) {
        TopBar(f, if (identify) "Où es-tu ?" else k?.let { "${it.emoji} ${it.label}" } ?: "Explorer") {
            RoundButton("🔎", "Chercher", size = 48.dp, color = C.Sand) { f.go(Screen.Search(mode)) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            if (identify) PhotoStrip(f, whereQuestion(k))
            else Text("Choisis un endroit.", style = T.Body.copy(color = C.Soft), modifier = Modifier.padding(horizontal = 18.dp))
            Spacer(Modifier.height(12.dp))
            // Only the places where this kingdom lives (no plants on the farm).
            val places: List<Habitat?> = Habitat.entries.filter { f.catalog.of(k, it).isNotEmpty() } + listOf(null)
            for (row in places.chunked(2)) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (h in row) {
                        val all = f.catalog.of(k, h)
                        if (h == null) Tile("🧭", if (identify) "Je ne sais pas" else "Partout", C.Soft, Modifier.weight(1f),
                            counted(all.size, k), f = f) { f.choosePlace(mode, k, null) }
                        else Tile(h.emoji, h.label, C.of(h), Modifier.weight(1f),
                            if (identify) counted(all.size, k) else "✓ ${all.count { p.seen(it.id) }} / ${all.size}",
                            mark = if (identify && h == f.lastHabitat) "📍" else null, f = f) { f.choosePlace(mode, k, h) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (identify) MysteryButton(f)
    }
}

@Composable
fun FamiliesScreen(f: Flow, mode: Mode, k: Kingdom?, h: Habitat?) {
    val identify = mode == Mode.IDENTIFY
    val p = f.progress
    val groups = f.catalog.groupsIn(k, h)
    Column(Modifier.fillMaxSize()) {
        TopBar(f, h?.let { "${it.emoji} ${it.label}" } ?: "Partout", h?.let { C.lip(C.of(it)) } ?: C.Ink) {
            RoundButton("🔎", "Chercher", size = 48.dp, color = C.Sand) { f.go(Screen.Search(mode)) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            if (identify) PhotoStrip(f, familyQuestion(k))
            else Text("Choisis une famille.", style = T.Body.copy(color = C.Soft), modifier = Modifier.padding(horizontal = 18.dp))
            Spacer(Modifier.height(12.dp))
            val tiles: List<Pair<Group, Int>?> = groups + listOf(null)
            for (row in tiles.chunked(2)) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (t in row) {
                        if (t == null) Tile("👀", if (identify) "Je ne sais pas" else "Tout voir", C.Soft, Modifier.weight(1f),
                            counted(f.catalog.of(k, h).size, k), f = f) { f.chooseFamily(mode, k, h, null) }
                        else {
                            val (g, n) = t
                            val found = f.catalog.of(k, h, g).count { p.seen(it.id) }
                            Tile(g.emoji, g.many, C.of(g), Modifier.weight(1f),
                                if (identify) counted(n, g.kingdom) else "✓ $found / $n",
                                mark = if (!identify && found == n) "🏆" else null, f = f) { f.chooseFamily(mode, k, h, g) }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SpeciesScreen(f: Flow, mode: Mode, k: Kingdom?, h: Habitat?, g: Group?) {
    val identify = mode == Mode.IDENTIFY
    // To name a find, the most common ones come first: they are the likeliest.
    val list = f.catalog.of(k, h, g).let { l -> if (identify) l.sortedBy { it.rarity } else l }
    val title = when {
        g != null -> "${g.emoji} ${g.many}"
        h != null -> "${h.emoji} ${h.label}"
        k != null -> "${k.emoji} ${k.label}"
        else -> "Tout"
    }
    Column(Modifier.fillMaxSize()) {
        TopBar(f, title, g?.let { C.lip(C.of(it)) } ?: C.Ink) {
            RoundButton("🔎", "Chercher", size = 48.dp, color = C.Sand) { f.go(Screen.Search(mode)) }
        }
        if (identify) PhotoStrip(f, whichQuestion(k))
        SpeciesGrid(f, list, mode, Modifier.weight(1f))
    }
}

@Composable
fun SpeciesGrid(f: Flow, list: List<Species>, mode: Mode, modifier: Modifier = Modifier, header: (@Composable () -> Unit)? = null) {
    LazyVerticalGrid(
        GridCells.Adaptive(160.dp), modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
        items(list, key = { it.id }) { a -> SpeciesCell(f, a) { f.tap(); f.open(a.id, mode) } }
    }
}

/** A sticker: the reference photo, the name, the stars; a green tick once found. */
@Composable
fun SpeciesCell(f: Flow, a: Species, onClick: () -> Unit) {
    val seen = f.progress.seen(a.id)
    Box(Modifier.semantics { contentDescription = a.name }) {
        Column(
            Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Color.White)
                .clickable(role = Role.Button, onClick = onClick),
        ) {
            SpeciesPicture(f, a, Modifier.fillMaxWidth().aspectRatio(1.15f), own = false, px = 360)
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(a.name, style = T.Label.copy(fontSize = 17.sp, lineHeight = 20.sp), maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                Stars(a.stars, size = 14)
            }
        }
        if (seen) Box(
            Modifier.align(Alignment.TopEnd).padding(8.dp).size(34.dp).clip(CircleShape).background(C.Green).border(3.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("✓", style = T.Label.copy(color = Color.White, fontSize = 18.sp)) }
    }
}

@Composable
fun SearchScreen(f: Flow, mode: Mode) {
    var q by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val found = remember(q) { f.catalog.search(q) }
    Column(Modifier.fillMaxSize()) {
        TopBar(f, "Chercher")
        BasicTextField(
            q, { q = it.take(30) }, singleLine = true, textStyle = T.Heading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).focusRequester(focus).semantics { contentDescription = "Nom à chercher" },
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth().height(60.dp).shadow(2.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)).background(Color.White).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔎", style = T.Emoji.copy(fontSize = 22.sp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (q.isEmpty()) Text("renard, chêne, crabe…", style = T.Heading.copy(color = C.Soft.copy(alpha = 0.5f)))
                        inner()
                    }
                    if (q.isNotEmpty()) Text("✕", style = T.Heading.copy(color = C.Soft), modifier = Modifier.clip(CircleShape).clickable { q = "" }.padding(8.dp))
                }
            },
        )
        if (mode == Mode.IDENTIFY) PhotoStrip(f, if (q.isBlank()) "Écris son nom." else "Lequel ressemble à ta photo ?")
        if (q.isNotBlank() && found.isEmpty()) Text(
            "Rien ne s'appelle comme ça. Essaie un autre mot !", style = T.Body.copy(color = C.Soft), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(28.dp),
        )
        SpeciesGrid(f, found, mode, Modifier.weight(1f))
    }
}
