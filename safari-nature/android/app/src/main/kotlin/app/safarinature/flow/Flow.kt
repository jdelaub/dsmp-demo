package app.safarinature.flow

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.safarinature.data.Photos
import app.safarinature.data.Profile
import app.safarinature.data.Store
import app.safarinature.voice.Voice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sn.core.catalog.Catalog
import sn.core.catalog.Group
import sn.core.catalog.Habitat
import sn.core.progress.Challenge
import sn.core.progress.Challenges
import sn.core.progress.Outcome
import sn.core.progress.Progress
import sn.core.progress.Question
import sn.core.progress.Sighting
import sn.core.progress.Summary
import java.io.File
import java.time.LocalDateTime

/** Identify: a photo is waiting for its animal. Explore: the encyclopedia, nothing waiting. */
enum class Mode { IDENTIFY, EXPLORE }

sealed interface Screen {
    data object Welcome : Screen
    data object Home : Screen
    /** [target]: the animal already chosen (from its card), so the photo goes straight to it. */
    data class Camera(val target: String? = null) : Screen
    data object Review : Screen
    data class Places(val mode: Mode) : Screen
    data class Families(val mode: Mode, val habitat: Habitat?) : Screen
    data class Animals(val mode: Mode, val habitat: Habitat?, val group: Group?) : Screen
    data class Search(val mode: Mode) : Screen
    data class Card(val id: String, val mode: Mode) : Screen
    data object Celebrate : Screen
    data object Album : Screen
    data object Medals : Screen
    data object Photos : Screen
    data class Photo(val sighting: Long) : Screen
    data object Gate : Screen
    data object Parents : Screen
    data object Credits : Screen
}

/** A photo (or none) waiting for its animal's name. [solving]: a mystery photo named later. [target]: already named. */
data class Pending(val photo: String?, val solving: Long? = null, val target: String? = null)

/**
 * The whole app's state, one screen stack and the album. Screens only read it and call its verbs, so the
 * tests can drive every path without a camera.
 */
class Flow(app: Application, dbName: String? = "safari.db") : AndroidViewModel(app) {
    val catalog: Catalog = Catalog.load()
    val store = Store(app, dbName)
    val profile = Profile(app)
    val photos = Photos(app)
    val voice = Voice(app).apply { soundsOn = profile.sounds; voiceOn = profile.voice }
    /** The clock, for the tests' early birds and night owls. */
    var clock: () -> LocalDateTime = { LocalDateTime.now() }

    var progress by mutableStateOf(Progress(catalog, store.all()))
        private set
    val stack = mutableStateListOf<Screen>(if (profile.welcomed) Screen.Home else Screen.Welcome)
    val screen: Screen get() = stack.last()

    var pending by mutableStateOf<Pending?>(null)
        private set
    /** Where the child said they are, while naming an animal. */
    var habitat by mutableStateOf<Habitat?>(null)
        private set
    var outcome by mutableStateOf<Outcome?>(null)
        private set
    var celebrated by mutableStateOf<Sighting?>(null)
        private set
    var toast by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
        private set

    var name by mutableStateOf(profile.name)
        private set
    var avatar by mutableStateOf(profile.avatar)
        private set
    var sounds by mutableStateOf(profile.sounds)
        private set
    var voiceOn by mutableStateOf(profile.voice)
        private set
    var cameraTip by mutableStateOf(profile.cameraTip)
        private set
    val lastHabitat get() = profile.lastHabitat

    var question by mutableStateOf(Question.random())
        private set
    var wrongAnswers by mutableStateOf(0)
        private set

    val challenge: Challenge get() = Challenges.of(clock().toLocalDate(), catalog)
    val explorer get() = name.ifBlank { "Explorateur" }

    // ——— Navigation ———

    fun go(s: Screen) {
        stack.add(s)
    }

    fun back() {
        if (stack.size <= 1) return
        val leaving = stack.removeAt(stack.lastIndex)
        voice.hush()
        // Leaving the photo's review means "take it again": that photo is not kept.
        if (leaving == Screen.Review) pending?.takeIf { it.solving == null }?.photo?.let { photos.delete(it); pending = null }
        if (leaving is Screen.Places && leaving.mode == Mode.IDENTIFY && pending?.solving != null) pending = null
        if (stack.last() == Screen.Home) pending = null
    }

    /** Back to the start. A photo still waiting for its name is never lost: it becomes a mystery animal. */
    fun home() {
        voice.hush()
        pending?.let { p -> if (p.photo != null && p.solving == null) store.add(null, clock(), p.photo, habitat).also { reload() } }
        pending = null
        habitat = null
        stack.clear()
        stack.add(Screen.Home)
    }

    fun say(text: String) = voice.say(text)
    fun tap() = voice.play(Voice.Sound.TAP)

    // ——— First launch ———

    fun welcome(name: String, avatar: String) {
        profile.name = name; profile.avatar = avatar; profile.welcomed = true
        this.name = profile.name; this.avatar = avatar
        stack.clear(); stack.add(Screen.Home)
        say("Bienvenue ${explorer} ! Prends en photo les animaux que tu rencontres.")
    }

    // ——— Photo, then name ———

    fun openCamera(target: String? = null) {
        pending = null
        habitat = null
        go(Screen.Camera(target))
    }

    fun hideCameraTip() {
        profile.cameraTip = false; cameraTip = false
    }

    /** The camera (or the gallery) gave a picture: straighten it, then show it for keeping or retaking. */
    fun photoTaken(file: File, target: String? = null) {
        busy = true
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { photos.settle(file) }
            busy = false
            if (name == null) { toast = "Oups, la photo n'a pas marché. Réessaie !"; return@launch }
            pending = Pending(name, target = target)
            go(Screen.Review)
        }
    }

    fun photoPicked(uri: android.net.Uri, target: String? = null) {
        busy = true
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { photos.import(uri) }
            busy = false
            if (name == null) { toast = "Cette image ne s'ouvre pas."; return@launch }
            pending = Pending(name, target = target)
            go(Screen.Review)
        }
    }

    /** "Keep it": straight to the animal when it was chosen first, otherwise "where are you?". */
    fun keepPhoto() {
        val p = pending ?: return
        if (p.target != null) confirm(p.target) else go(Screen.Places(Mode.IDENTIFY))
    }

    /** The animal ran away before the photo: name it anyway. */
    fun withoutPhoto() {
        pending = Pending(null)
        go(Screen.Places(Mode.IDENTIFY))
    }

    fun choosePlace(mode: Mode, h: Habitat?) {
        if (mode == Mode.IDENTIFY) habitat = h
        go(Screen.Families(mode, h))
    }

    fun chooseFamily(mode: Mode, h: Habitat?, g: Group?) = go(Screen.Animals(mode, h, g))

    fun open(id: String, mode: Mode) = go(Screen.Card(id, mode))

    /** "C'est lui !": the sighting is written and the celebration shows what it changed. */
    fun confirm(id: String) {
        val p = pending
        val h = habitat
        val result = if (p?.solving != null) {
            val old = progress.sightings.first { it.id == p.solving }
            store.solve(old.id, id, h)
            val named = old.copy(animal = id, solved = true, habitat = h ?: old.habitat)
            Progress(catalog, progress.sightings - old).with(named).also { celebrated = named }
        } else {
            val s = store.add(id, clock(), p?.photo, h)
            celebrated = s
            progress.with(s)
        }
        if (h != null) profile.lastHabitat = h
        pending = null
        habitat = null
        progress = result.first
        outcome = result.second
        stack.clear(); stack.add(Screen.Home); stack.add(Screen.Celebrate)
        val o = result.second
        voice.play(if (o.levelUp != null) Voice.Sound.LEVEL_UP else if (o.firstTime) Voice.Sound.DISCOVER else Voice.Sound.STAR)
        o.animal?.let { a ->
            say(if (o.firstTime) "Bravo ! Nouvelle découverte : ${a.name} !" else "Tu as revu : ${a.name} ! Ça fait ${o.times} fois.")
        }
    }

    /** From an animal's card: seen, but no photo. */
    fun seenWithoutPhoto(id: String) {
        pending = null
        habitat = null
        confirm(id)
    }

    /** "I don't know who it is": the photo waits in "Mes photos" as a mystery animal. */
    fun keepMystery() {
        val p = pending ?: return
        if (p.photo != null && p.solving == null) store.add(null, clock(), p.photo, habitat)
        pending = null
        habitat = null
        reload()
        stack.clear(); stack.add(Screen.Home)
        toast = "Photo gardée ! Trouve son nom plus tard avec un adulte 🕵️"
    }

    /** A mystery photo: look for its name now. */
    fun solve(sighting: Sighting) {
        pending = Pending(sighting.photo, solving = sighting.id)
        habitat = sighting.habitat
        go(Screen.Places(Mode.IDENTIFY))
    }

    fun delete(s: Sighting) {
        store.delete(s.id)
        photos.delete(s.photo)
        reload()
        if (screen == Screen.Photo(s.id)) back()
        toast = "Photo supprimée"
    }

    private fun reload() {
        progress = Progress(catalog, store.all())
    }

    // ——— Grown-ups ———

    fun openGate() {
        question = Question.random()
        wrongAnswers = 0
        go(Screen.Gate)
    }

    fun answer(n: Int?) {
        if (n == question.answer) {
            stack.removeAt(stack.lastIndex)
            go(Screen.Parents)
        } else {
            wrongAnswers++
            question = Question.random()
            toast = "Ce n'est pas ça. Cet espace est pour les adultes !"
        }
    }

    fun summary(): String = Summary.of(name, clock().toLocalDate(), progress)

    fun rename(n: String) { profile.name = n; name = profile.name }
    fun chooseAvatar(a: String) { profile.avatar = a; avatar = a }
    fun turnSounds(on: Boolean) { profile.sounds = on; sounds = on; voice.soundsOn = on }
    fun turnVoice(on: Boolean) { profile.voice = on; voiceOn = on; voice.voiceOn = on; if (!on) voice.hush() }

    /** Everything goes: sightings and photos. The name stays. */
    fun reset() {
        store.clear()
        photos.clear()
        reload()
        stack.clear(); stack.add(Screen.Home)
        toast = "L'album est vide : une nouvelle aventure commence !"
    }

    fun photoFile(name: String): File = photos.file(name)

    override fun onCleared() {
        voice.close()
        store.close()
    }
}
