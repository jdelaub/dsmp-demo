package app.safarinature.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.safarinature.flow.Flow
import app.safarinature.flow.Mode
import app.safarinature.flow.Screen
import sn.core.progress.Medals

val Avatars = listOf("🦊", "🦉", "🐸", "🦋", "🐞", "🦔", "🐿️", "🐢", "🐬", "🐝", "🦌", "🐻")

@Composable
fun WelcomeScreen(f: Flow) {
    var name by rememberSaveable { mutableStateOf(f.name) }
    var avatar by rememberSaveable { mutableStateOf(f.avatar) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Text(avatar, style = T.Emoji.copy(fontSize = 92.sp, lineHeight = 104.sp), modifier = Modifier.popIn())
        Text("Safari Nature", style = T.Hero.copy(color = C.Orange, fontSize = 40.sp, lineHeight = 46.sp))
        Text("Pars à la chasse aux photos d'animaux et de plantes !", style = T.Body, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Text("Comment t'appelles-tu ?", style = T.Heading)
        Spacer(Modifier.height(10.dp))
        NameField(name) { name = it }
        Spacer(Modifier.height(26.dp))
        Text("Choisis ton animal porte-bonheur :", style = T.Heading, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        AvatarPicker(avatar) { avatar = it; f.tap() }
        Spacer(Modifier.height(30.dp))
        Chunky("C'est parti !", Modifier.fillMaxWidth().breathing(), C.Green, "🚀", f = f) { f.welcome(name, avatar) }
        Spacer(Modifier.height(12.dp))
        Text("Tu pourras changer ton prénom plus tard.", style = T.Small)
    }
}

@Composable
fun NameField(value: String, onChange: (String) -> Unit) {
    BasicTextField(
        value, { onChange(it.take(20)) },
        singleLine = true,
        textStyle = T.Title.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Ton prénom" },
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().height(66.dp).shadow(2.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
                    .background(Color.White).border(3.dp, C.Orange.copy(alpha = 0.5f), RoundedCornerShape(22.dp)).padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) Text("Ton prénom", style = T.Title.copy(color = C.Soft.copy(alpha = 0.5f)))
                inner()
            }
        },
    )
}

@Composable
fun AvatarPicker(selected: String, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in Avatars.chunked(4)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (a in row) Box(
                Modifier.size(68.dp).clip(CircleShape).background(if (a == selected) C.Yellow else Color.White)
                    .border(if (a == selected) 4.dp else 2.dp, if (a == selected) C.Orange else C.Line, CircleShape)
                    .clickable(role = Role.Button) { onPick(a) },
                contentAlignment = Alignment.Center,
            ) { Text(a, style = T.Emoji.copy(fontSize = 34.sp, lineHeight = 40.sp)) }
        }
    }
}

@Composable
fun HomeScreen(f: Flow) {
    val p = f.progress
    val total = f.catalog.all.size
    val challenge = f.challenge
    val done = challenge.done(p)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        // Who is exploring, and the grown-ups' door (small, out of the way).
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(f.avatar, 62.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Salut ${f.explorer} !", style = T.Title, maxLines = 1)
                Text("${p.level.emoji} ${p.level.title}", style = T.Label.copy(color = C.Soft))
            }
            RoundButton("🔒", "Espace parents", size = 44.dp, color = C.Sand) { f.openGate() }
        }
        Spacer(Modifier.height(14.dp))

        // Stars and the way to the next level.
        Paper(Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐", style = T.Emoji.copy(fontSize = 30.sp))
                    Spacer(Modifier.width(6.dp))
                    Text("${p.stars}", style = T.Title.copy(color = C.CautionInk))
                    Spacer(Modifier.width(12.dp))
                    Bar(p.levelProgress, C.Star, Modifier.weight(1f), 18.dp)
                }
                Spacer(Modifier.height(6.dp))
                val next = p.nextLevel
                Text(
                    if (next == null) "Tu as atteint le plus haut niveau. Bravo !"
                    else "Encore ${next.stars - p.stars} ⭐ pour devenir ${next.title} ${next.emoji}",
                    style = T.Small.copy(fontSize = 15.sp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // Today's mission.
        Paper(Modifier.fillMaxWidth().clickable { f.say("Défi du jour : ${challenge.text}") }, color = if (done) Color(0xFFE3F6E7) else Color(0xFFFFF1C9)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (done) "✅" else challenge.emoji, style = T.Emoji.copy(fontSize = 34.sp, lineHeight = 40.sp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (done) "Défi du jour réussi !" else "🎯 Défi du jour", style = T.Label.copy(color = if (done) C.GreenLip else C.CautionInk))
                    Text(challenge.text, style = T.Heading.copy(fontSize = 19.sp, lineHeight = 23.sp))
                }
            }
        }
        Spacer(Modifier.height(22.dp))

        // The one big thing to press.
        CameraButton(f)
        Spacer(Modifier.height(6.dp))
        Text(
            "👀 Je l'ai vu, mais pas de photo", style = T.Label.copy(color = C.Soft, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable { f.withoutPhoto() }.padding(12.dp),
        )
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Tile("📒", "Mon album", C.Green, Modifier.weight(1f), "${p.discovered.size} / $total", f = f) { f.go(Screen.Album) }
            Tile("🏅", "Médailles", C.Yellow.let { C.lip(it) }, Modifier.weight(1f), "${Medals.all.count { it.won(p) }} / ${Medals.all.size}", f = f) { f.go(Screen.Medals) }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Tile("🗺️", "Explorer", C.Blue, Modifier.weight(1f), "$total espèces", f = f) { f.go(Screen.Kinds(Mode.EXPLORE)) }
            Tile("🖼️", "Mes photos", C.Purple, Modifier.weight(1f), "${p.photos.size}", mark = if (p.mysteries.isNotEmpty()) "❓" else null, f = f) { f.go(Screen.Photos) }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CameraButton(f: Flow) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(186.dp).breathing().clip(CircleShape).background(C.OrangeLip)
                .clickable(src, null, role = Role.Button) { f.tap(); f.openCamera() }
                .semantics { contentDescription = "Photographier un animal ou une plante" },
        ) {
            Box(
                Modifier.size(186.dp).offset(y = if (pressed) 0.dp else (-7).dp).clip(CircleShape).background(C.Orange)
                    .border(6.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📸", style = T.Emoji.copy(fontSize = 70.sp, lineHeight = 80.sp))
                    Text("Photo !", style = T.Button.copy(fontSize = 24.sp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Photographie un animal ou une plante", style = T.Heading, textAlign = TextAlign.Center)
    }
}
