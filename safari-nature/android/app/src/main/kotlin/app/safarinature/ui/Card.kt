package app.safarinature.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.safarinature.flow.Flow
import app.safarinature.flow.Mode
import app.safarinature.flow.Screen
import sn.core.catalog.Animal

/** Where a reference photo was published: its credit names the site. */
fun photoHome(source: String) = if ("inaturalist" in source) "iNaturalist" else "Wikimedia Commons"

fun rarityWords(r: Int) = when (r) {
    1 -> "Facile à voir"
    2 -> "Il faut un peu de chance"
    else -> "Rare : un vrai trésor !"
}

/** Everything read aloud for a card, in the order it is shown. */
fun spoken(a: Animal) = buildString {
    append("${a.name}. ")
    append("Comment le reconnaître : ${a.look} ")
    append("Le savais-tu ? ${a.fact}")
    a.caution?.let { append(" Attention : $it") }
}

/**
 * An animal's card. In "identify" the child compares their photo with it and says "C'est lui !";
 * in "explore" they read, listen, and can go and photograph it.
 */
@Composable
fun CardScreen(f: Flow, id: String, mode: Mode) {
    val a = f.catalog[id] ?: return
    val p = f.progress
    val identify = mode == Mode.IDENTIFY
    val color = C.of(a.group)
    val credit = f.catalog.photo(a.id)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box {
                if (identify && f.pending?.photo != null) {
                    // Side by side: their photo and the reference.
                    Row(Modifier.fillMaxWidth().padding(top = 76.dp, start = 14.dp, end = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Labelled("Ta photo", Modifier.weight(1f)) {
                            Picture(FilePic(f.photoFile(f.pending!!.photo!!).path), Modifier.fillMaxWidth().aspectRatio(0.9f), px = 600)
                        }
                        Labelled(a.name, Modifier.weight(1f)) { AnimalPicture(f, a, Modifier.fillMaxWidth().aspectRatio(0.9f), own = false, px = 600) }
                    }
                } else {
                    AnimalPicture(f, a, Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)), own = false, px = 900)
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundButton("←", "Retour") { f.back() }
                    Spacer(Modifier.weight(1f))
                    val n = p.count(a.id)
                    if (n > 0) Pill("✓ Vu $n fois", C.Green)
                }
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(a.name, style = T.Hero.copy(fontSize = 31.sp, lineHeight = 35.sp))
                        Text(a.latin, style = T.Small.copy(fontStyle = FontStyle.Italic, fontSize = 15.sp))
                    }
                    Listen(f, spoken(a), size = 58.dp)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("${a.group.emoji} ${a.group.one}", color)
                    for (h in a.habitats) Pill("${h.emoji} ${h.label}", C.of(h).copy(alpha = 0.16f), ink = C.lip(C.lip(C.of(h))))
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stars(a.stars, outOf = 3, size = 24)
                    Spacer(Modifier.width(10.dp))
                    Text(rarityWords(a.rarity), style = T.Label.copy(color = C.Soft))
                }
                a.caution?.let { c ->
                    Spacer(Modifier.height(14.dp))
                    Paper(Modifier.fillMaxWidth(), color = C.Caution) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", style = T.Emoji.copy(fontSize = 28.sp))
                            Spacer(Modifier.width(12.dp))
                            Text(c, style = T.Body.copy(color = C.CautionInk), modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Section("🔍", "Comment le reconnaître", a.look, color)
                Spacer(Modifier.height(12.dp))
                Section("💡", "Le savais-tu ?", a.fact, C.Purple)
                Spacer(Modifier.height(12.dp))
                Paper(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Fact("📏", "Taille", a.size)
                        Fact("🍽️", "Au menu", a.food)
                        Fact("📅", "Quand le voir", a.season)
                    }
                }
                val mine = p.photosOf(a.id)
                if (mine.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("📸 Tes photos (${mine.size})", style = T.Heading)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (s in mine) Picture(
                            FilePic(f.photoFile(s.photo!!).path),
                            Modifier.size(110.dp).clip(RoundedCornerShape(18.dp)).clickable { f.go(Screen.Photo(s.id)) }, px = 300,
                        )
                    }
                }
                if (credit != null) {
                    Spacer(Modifier.height(16.dp))
                    Text("Photo : ${credit.author} · ${credit.license} · ${photoHome(credit.source)}", style = T.Small.copy(fontSize = 12.sp))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        // The action stays at the bottom, always in reach of a thumb.
        Column(Modifier.fillMaxWidth().background(C.Paper).padding(horizontal = 18.dp, vertical = 10.dp)) {
            if (identify) {
                Chunky("C'est lui !", Modifier.fillMaxWidth(), C.Green, "✅", f = f) { f.confirm(a.id) }
                Text(
                    "Non, ce n'est pas lui", style = T.Label.copy(color = C.Soft, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable { f.back() }.padding(12.dp),
                )
            } else {
                Chunky("Je le prends en photo", Modifier.fillMaxWidth(), C.Orange, "📸", f = f) { f.openCamera(a.id) }
                Text(
                    "👀 Je l'ai vu sans photo", style = T.Label.copy(color = C.Soft, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable { f.seenWithoutPhoto(a.id) }.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun Labelled(label: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.shadow(3.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Color.White)) {
        content()
        Text(label, style = T.Label, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(8.dp))
    }
}

@Composable
private fun Section(emoji: String, title: String, text: String, color: Color) {
    Paper(Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = T.Emoji.copy(fontSize = 22.sp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = T.Heading.copy(color = C.lip(color), fontSize = 19.sp))
            }
            Spacer(Modifier.height(6.dp))
            Text(text, style = T.Body)
        }
    }
}

@Composable
private fun Fact(emoji: String, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(C.Sand).border(1.dp, C.Line, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text(emoji, style = T.Emoji.copy(fontSize = 20.sp, lineHeight = 24.sp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = T.Small.copy(fontSize = 14.sp))
            Text(value, style = T.Label.copy(fontSize = 17.sp, lineHeight = 22.sp))
        }
    }
}
