package app.safarinature

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import app.safarinature.flow.Flow
import app.safarinature.ui.App
import app.safarinature.ui.SafariTheme

class MainActivity : ComponentActivity() {
    private val flow by lazy { ViewModelProvider(this)[Flow::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        keepCrashes()
        setContent { SafariTheme { App(flow) } }
        offerLastCrash()
    }

    private val crashFile get() = java.io.File(filesDir, "last-crash.txt")

    /** If the app stops, what stopped it is written down first: a child's phone leaves no other trace. */
    private fun keepCrashes() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is CrashKeeper) return
        Thread.setDefaultUncaughtExceptionHandler(CrashKeeper(crashFile, previous))
    }

    private class CrashKeeper(val file: java.io.File, val previous: Thread.UncaughtExceptionHandler?) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread, e: Throwable) {
            runCatching { file.writeText("Safari Nature ${BuildConfig.VERSION_NAME}, ${java.time.LocalDateTime.now()}, thread ${t.name}\n\n${e.stackTraceToString()}") }
            previous?.uncaughtException(t, e)
        }
    }

    /** The app stopped last time: offer the details to a grown-up, to send to us. */
    private fun offerLastCrash() {
        val f = crashFile
        if (!f.exists()) return
        val text = runCatching { f.readText() }.getOrNull().orEmpty()
        f.delete()
        if (text.isBlank()) return
        android.app.AlertDialog.Builder(this)
            .setTitle("Safari Nature s'est arrêté la dernière fois")
            .setMessage("Pour les parents : vous pouvez envoyer les détails pour aider à corriger le problème.")
            .setPositiveButton("Envoyer les détails") { _, _ ->
                runCatching {
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, "Safari Nature : arrêt").putExtra(Intent.EXTRA_TEXT, text), "Envoyer les détails"))
                }
            }
            .setNegativeButton("Fermer", null)
            .show()
    }
}
