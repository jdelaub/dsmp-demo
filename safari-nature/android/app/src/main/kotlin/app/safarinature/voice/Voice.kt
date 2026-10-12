package app.safarinature.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaActionSound
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import app.safarinature.R
import java.util.Locale

/**
 * Reads names and facts aloud (the phone's own French voice, offline where the phone has it) and plays the
 * app's little sounds. Children who can't read yet tap 🔊 and listen.
 */
class Voice(private val ctx: Context) {
    var soundsOn = true
    var voiceOn = true

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var waiting: String? = null

    private val pool: SoundPool? = runCatching {
        SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        ).build()
    }.getOrNull()
    private val ids: Map<Sound, Int> = pool?.let { p -> Sound.entries.associateWith { p.load(ctx, it.res, 1) } }.orEmpty()
    private val shutter = runCatching { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }.getOrNull()

    enum class Sound(val res: Int) { TAP(R.raw.tap), DISCOVER(R.raw.discover), STAR(R.raw.star), LEVEL_UP(R.raw.levelup) }

    fun play(s: Sound) {
        if (!soundsOn) return
        val id = ids[s] ?: return
        pool?.play(id, 0.8f, 0.8f, 1, 0, 1f)
    }

    fun shutter() {
        if (soundsOn) shutter?.play(MediaActionSound.SHUTTER_CLICK)
    }

    /** Says [text] now, cutting off what was being said. */
    fun say(text: String) {
        if (!voiceOn || text.isBlank()) return
        val t = tts
        if (t == null) {
            waiting = text
            tts = TextToSpeech(ctx.applicationContext) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    tts?.language = Locale.FRANCE
                    tts?.setSpeechRate(0.92f)
                    waiting?.let { speak(it) }
                }
                waiting = null
            }
        } else if (ttsReady) speak(text) else waiting = text
    }

    fun hush() {
        tts?.stop()
    }

    private fun speak(text: String) {
        tts?.speak(text.replace("≈", "environ "), TextToSpeech.QUEUE_FLUSH, null, "sn")
    }

    fun close() {
        tts?.shutdown()
        tts = null
        pool?.release()
        shutter?.release()
    }
}
