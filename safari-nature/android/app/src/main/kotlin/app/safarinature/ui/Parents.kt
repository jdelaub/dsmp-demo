package app.safarinature.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.safarinature.BuildConfig
import app.safarinature.flow.Flow
import app.safarinature.flow.Screen
import sn.core.catalog.Kingdom
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The grown-ups' door: a multiplication, typed on big keys. */
@Composable
fun GateScreen(f: Flow) {
    var typed by remember(f.question) { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        TopBar(f, "Espace parents")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Cet espace est réservé aux adultes.", style = T.Body.copy(color = C.Soft), textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Text("Combien font ${f.question.text} ?", style = T.Hero, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.width(180.dp).height(70.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).border(3.dp, C.Line, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                Text(typed.ifEmpty { "?" }, style = T.Hero.copy(color = if (typed.isEmpty()) C.Line else C.Ink))
            }
            Spacer(Modifier.height(18.dp))
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "OK")
            for (row in keys.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    for (k in row) Box(
                        Modifier.size(84.dp, 64.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (k == "OK") C.Green else Color.White).border(2.dp, C.Line, RoundedCornerShape(18.dp))
                            .clickable(role = Role.Button) {
                                when (k) {
                                    "⌫" -> typed = typed.dropLast(1)
                                    "OK" -> f.answer(typed.toIntOrNull())
                                    else -> if (typed.length < 3) typed += k
                                }
                            }.semantics { contentDescription = k },
                        contentAlignment = Alignment.Center,
                    ) { Text(k, style = T.Title.copy(color = if (k == "OK") Color.White else C.Ink)) }
                }
            }
        }
    }
}

/** For the parents: the day to share, the figures, the settings, the credits. */
@Composable
fun ParentsScreen(f: Flow) {
    val ctx = LocalContext.current
    val st = f.progress.stats()
    var name by remember { mutableStateOf(f.name) }
    var wipe by remember { mutableStateOf(false) }
    val date = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)
    fun share(intent: Intent, title: String) = runCatching { ctx.startActivity(Intent.createChooser(intent, title)) }
    Column(Modifier.fillMaxSize()) {
        TopBar(f, "Espace parents")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            Heading("Aujourd'hui")
            val text = f.summary()
            Paper(Modifier.fillMaxWidth()) { Text(text, style = T.Body.copy(fontSize = 16.sp, lineHeight = 22.sp)) }
            Spacer(Modifier.height(10.dp))
            Chunky("Partager le résumé", Modifier.fillMaxWidth(), C.Blue, "📤") {
                share(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Envoyer le résumé")
            }
            val today = f.progress.on(f.clock().toLocalDate()).mapNotNull { it.photo }
            if (today.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Chunky("Partager les photos du jour (${today.size})", Modifier.fillMaxWidth(), C.Purple, "🖼️") {
                    val uris = ArrayList(today.map { FileProvider.getUriForFile(ctx, "${ctx.packageName}.photos", f.photoFile(it)) })
                    share(Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/jpeg").putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                        .putExtra(Intent.EXTRA_TEXT, text).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Envoyer les photos")
                }
            }

            Heading("Statistiques")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${st.sightings}", "observations", Modifier.weight(1f))
                Stat("${st.species}", "espèces", Modifier.weight(1f))
                Stat("${st.photos}", "photos", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Paper(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Line("Première observation", st.first?.let(date::format) ?: "—")
                    Line("Dernière observation", st.last?.let(date::format) ?: "—")
                    Line("Niveau", "${f.progress.level.emoji} ${f.progress.level.title} (${f.progress.stars} ⭐)")
                    if (st.top.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Les plus observés", style = T.Label.copy(color = C.Soft))
                        for ((a, n) in st.top) Line("${a.emoji} ${a.name}", "$n")
                    }
                    if (st.byGroup.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Par famille", style = T.Label.copy(color = C.Soft))
                        for ((g, n) in st.byGroup) Line("${g.emoji} ${g.many}", "$n / ${f.catalog.of(g = g).size}")
                    }
                }
            }

            Heading("Réglages")
            Text("Prénom de l'explorateur", style = T.Label.copy(color = C.Soft))
            Spacer(Modifier.height(6.dp))
            NameField(name) { name = it; f.rename(it) }
            Spacer(Modifier.height(14.dp))
            Text("Son animal", style = T.Label.copy(color = C.Soft))
            Spacer(Modifier.height(6.dp))
            AvatarPicker(f.avatar) { f.chooseAvatar(it) }
            Spacer(Modifier.height(14.dp))
            Toggle("🔔 Sons", "Petits sons de réussite", f.sounds) { f.turnSounds(it) }
            Spacer(Modifier.height(10.dp))
            Toggle("🔊 Lecture à voix haute", "Les noms et les fiches lus par le téléphone", f.voiceOn) { f.turnVoice(it) }

            Heading("À propos")
            Paper(Modifier.fillMaxWidth()) {
                Text(
                    "🔒 Tout reste sur ce téléphone : l'application n'a pas accès à Internet. Les photos ne quittent le téléphone que si vous les partagez vous-même.\n\n" +
                        "📚 ${f.catalog.of(Kingdom.ANIMAL).size} animaux et ${f.catalog.of(Kingdom.PLANT).size} plantes de France et d'Europe de l'Ouest. Photos de référence : Wikimedia Commons et iNaturalist, sous licences libres.",
                    style = T.Body.copy(fontSize = 16.sp, lineHeight = 22.sp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Chunky("Crédits des photos", Modifier.fillMaxWidth(), C.Soft, "📜") { f.go(Screen.Credits) }
            Spacer(Modifier.height(10.dp))
            Chunky("Recommencer à zéro", Modifier.fillMaxWidth(), C.Red, "🗑️") { wipe = true }
            Spacer(Modifier.height(14.dp))
            Text("Safari Nature ${BuildConfig.VERSION_NAME}", style = T.Small, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
    if (wipe) AlertDialog(
        onDismissRequest = { wipe = false },
        title = { Text("Tout effacer ?", style = T.Title) },
        text = { Text("Les ${st.sightings} observations et les ${st.photos} photos seront supprimées. Le prénom est gardé.", style = T.Body) },
        confirmButton = { TextButton({ wipe = false; f.reset() }) { Text("Tout effacer", style = T.Label.copy(color = C.Red)) } },
        dismissButton = { TextButton({ wipe = false }) { Text("Annuler", style = T.Label) } },
    )
}

@Composable
private fun Heading(text: String) {
    Text(text, style = T.Title.copy(fontSize = 22.sp), modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Paper(modifier, padding = 12.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = T.Title.copy(color = C.OrangeLip))
            Text(label, style = T.Small)
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row {
        Text(label, style = T.Body.copy(fontSize = 16.sp), modifier = Modifier.weight(1f))
        Text(value, style = T.Label.copy(fontSize = 16.sp))
    }
}

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Paper(Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChange(!on) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = T.Label.copy(fontSize = 18.sp))
                Text(sub, style = T.Small)
            }
            Box(Modifier.size(64.dp, 36.dp).clip(RoundedCornerShape(50)).background(if (on) C.Green else C.Line)) {
                Box(Modifier.offset(x = if (on) 31.dp else 3.dp, y = 3.dp).size(30.dp).clip(CircleShape).background(Color.White))
            }
        }
    }
}

/** Who took each reference photo, and under which licence (required by CC BY and CC BY-SA). */
@Composable
fun CreditsScreen(f: Flow) {
    val rows = f.catalog.all.mapNotNull { a -> f.catalog.photo(a.id)?.let { a to it } }
    Column(Modifier.fillMaxSize()) {
        TopBar(f, "Crédits")
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(
                    "Les photos de référence viennent de Wikimedia Commons et d'iNaturalist. Merci à leurs auteurs ! Chaque photo est réduite pour l'application ; " +
                        "l'original et sa licence sont sur la page indiquée.",
                    style = T.Body.copy(fontSize = 16.sp, lineHeight = 22.sp),
                )
            }
            items(rows, key = { it.first.id }) { (a, p) ->
                Column {
                    Text(a.name, style = T.Label)
                    Text("${p.author} — ${p.license} — ${photoHome(p.source)}", style = T.Small.copy(color = C.Ink))
                    Text(p.source, style = T.Small.copy(fontSize = 11.sp, lineHeight = 13.sp))
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Police Fredoka : The Fredoka Project Authors, SIL Open Font License 1.1.\nSons : créés pour Safari Nature.\nTextes des fiches : écrits pour Safari Nature.",
                    style = T.Small.copy(color = C.Ink),
                )
            }
        }
    }
}
