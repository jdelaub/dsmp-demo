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
import sn.core.catalog.Animal
import sn.core.catalog.Group
import sn.core.catalog.Habitat

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

@Composable
fun PlacesScreen(f: Flow, mode: Mode) {
    val identify = mode == Mode.IDENTIFY
    val p = f.progress
    Column(Modifier.fillMaxSize()) {
        TopBar(f, if (identify) "Où es-tu ?" else "Explorer") {
            RoundButton("🔎", "Chercher", size = 48.dp, color = C.Sand) { f.go(Screen.Search(mode)) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            if (identify) PhotoStrip(f, "Où as-tu vu cet animal ?")
            else Text("Choisis un endroit pour découvrir ses animaux.", style = T.Body.copy(color = C.Soft), modifier = Modifier.padding(horizontal = 18.dp))
            Spacer(Modifier.height(12.dp))
            val places: List<Habitat?> = Habitat.entries + listOf(null)
            for (row in places.chunked(2)) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (h in row) {
                        val all = f.catalog.inHabitat(h)
                        if (h == null) Tile("🧭", if (identify) "Je ne sais pas" else "Partout", C.Soft, Modifier.weight(1f),
                            if (identify) "Tous les animaux" else "${all.size} animaux", f = f) { f.choosePlace(mode, null) }
                        else Tile(h.emoji, h.label, C.of(h), Modifier.weight(1f),
                            if (identify) "${all.size} animaux" else "${all.count { p.seen(it.id) }} / ${all.size} trouvés",
                            mark = if (identify && h == f.lastHabitat) "📍" else null, f = f) { f.choosePlace(mode, h) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (identify && f.pending?.photo != null && f.pending?.solving == null) {
            Chunky("Je ne sais pas qui c'est", Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), C.Purple, "❓", f = f) { f.keepMystery() }
        }
    }
}

@Composable
fun FamiliesScreen(f: Flow, mode: Mode, h: Habitat?) {
    val identify = mode == Mode.IDENTIFY
    val p = f.progress
    val groups = f.catalog.groupsIn(h)
    Column(Modifier.fillMaxSize()) {
        TopBar(f, h?.let { "${it.emoji} ${it.label}" } ?: "Tous les animaux", h?.let { C.lip(C.of(it)) } ?: C.Ink) {
            RoundButton("🔎", "Chercher", size = 48.dp, color = C.Sand) { f.go(Screen.Search(mode)) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            if (identify) PhotoStrip(f, "C'est quel genre d'animal ?")
            else Text("Choisis une famille d'animaux.", style = T.Body.copy(color = C.Soft), modifier = Modifier.padding(horizontal = 18.dp))
            Spacer(Modifier.height(12.dp))
            val tiles: List<Pair<Group, Int>?> = groups + listOf(null)
            for (row in tiles.chunked(2)) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (t in row) {
                        if (t == null) Tile("👀", if (identify) "Je ne sais pas" else "Tout voir", C.Soft, Modifier.weight(1f),
                            "${f.catalog.inHabitat(h).size} animaux", f = f) { f.chooseFamily(mode, h, null) }
                        else {
                            val (g, n) = t
                            val found = f.catalog.of(h, g).count { p.seen(it.id) }
                            Tile(g.emoji, g.many, C.of(g), Modifier.weight(1f),
                                if (identify) "$n animaux" else "$found / $n trouvés",
                                mark = if (!identify && found == n) "🏆" else null, f = f) { f.chooseFamily(mode, h, g) }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AnimalsScreen(f: Flow, mode: Mode, h: Habitat?, g: Group?) {
    val identify = mode == Mode.IDENTIFY
    // To name an animal, the most common ones come first: they are the likeliest.
    val list = f.catalog.of(h, g).let { l -> if (identify) l.sortedBy { it.rarity } else l }
    val title = when {
        g != null && h != null -> "${g.emoji} ${g.many}"
        g != null -> "${g.emoji} ${g.many}"
        h != null -> "${h.emoji} ${h.label}"
        else -> "Tous les animaux"
    }
    Column(Modifier.fillMaxSize()) {
        TopBar(f, title, g?.let { C.lip(C.of(it)) } ?: C.Ink) {
            RoundButton("🔎", "Chercher", size = 48.dp, color = C.Sand) { f.go(Screen.Search(mode)) }
        }
        if (identify) PhotoStrip(f, "Lequel ressemble à ton animal ?")
        AnimalGrid(f, list, mode, Modifier.weight(1f))
    }
}

@Composable
fun AnimalGrid(f: Flow, list: List<Animal>, mode: Mode, modifier: Modifier = Modifier, header: (@Composable () -> Unit)? = null) {
    LazyVerticalGrid(
        GridCells.Adaptive(160.dp), modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
        items(list, key = { it.id }) { a -> AnimalCell(f, a) { f.tap(); f.open(a.id, mode) } }
    }
}

/** A sticker: the reference photo, the name, the stars; a green tick once found. */
@Composable
fun AnimalCell(f: Flow, a: Animal, onClick: () -> Unit) {
    val seen = f.progress.seen(a.id)
    Box(Modifier.semantics { contentDescription = a.name }) {
        Column(
            Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Color.White)
                .clickable(role = Role.Button, onClick = onClick),
        ) {
            AnimalPicture(f, a, Modifier.fillMaxWidth().aspectRatio(1.15f), own = false, px = 360)
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
        TopBar(f, "Chercher un animal")
        BasicTextField(
            q, { q = it.take(30) }, singleLine = true, textStyle = T.Heading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).focusRequester(focus).semantics { contentDescription = "Nom de l'animal" },
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth().height(60.dp).shadow(2.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)).background(Color.White).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔎", style = T.Emoji.copy(fontSize = 22.sp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (q.isEmpty()) Text("renard, mésange, crabe…", style = T.Heading.copy(color = C.Soft.copy(alpha = 0.5f)))
                        inner()
                    }
                    if (q.isNotEmpty()) Text("✕", style = T.Heading.copy(color = C.Soft), modifier = Modifier.clip(CircleShape).clickable { q = "" }.padding(8.dp))
                }
            },
        )
        if (mode == Mode.IDENTIFY) PhotoStrip(f, if (q.isBlank()) "Écris le nom de l'animal." else "Lequel ressemble à ton animal ?")
        if (q.isNotBlank() && found.isEmpty()) Text(
            "Aucun animal ne s'appelle comme ça. Essaie un autre mot !", style = T.Body.copy(color = C.Soft), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(28.dp),
        )
        AnimalGrid(f, found, mode, Modifier.weight(1f))
    }
}
