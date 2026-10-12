package app.safarinature

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.safarinature.flow.Flow
import app.safarinature.flow.Mode
import app.safarinature.flow.Screen
import app.safarinature.ui.App
import app.safarinature.ui.SafariTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import sn.core.catalog.Group
import sn.core.catalog.Habitat
import java.io.File
import java.time.LocalDateTime

/**
 * A child's first outing, screen by screen, driven through the flow as the buttons drive it; a screenshot of
 * every screen goes to build/screens/ (the UI is reviewed from those, no emulator needed).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w392dp-h850dp-xxhdpi")
class ScreensTest {
    @get:Rule val rule = createComposeRule()
    private val out = File("build/screens").apply { deleteRecursively(); mkdirs() }
    private var n = 0
    private var now = LocalDateTime.of(2026, 5, 2, 10, 30)

    private fun shot(name: String) {
        // Pictures decode on an IO thread: give them real time before the capture.
        repeat(8) { Thread.sleep(60); tick(40) }
        val b = rule.onRoot().captureToImage().asAndroidBitmap()
        File(out, "%02d_%s.png".format(n++, name)).outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tick(ms: Long) { rule.waitForIdle(); rule.mainClock.advanceTimeBy(ms); rule.waitForIdle() }

    /** Photos are settled on an IO thread: wait on the real clock while the UI clock ticks. */
    private fun until(maxMs: Long = 15000, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + maxMs
        while (!ok() && System.currentTimeMillis() < end) { Thread.sleep(50); tick(50) }
        assertTrue(ok())
    }

    /** A "camera" photo: one of the reference pictures, written as the camera writes a JPEG. */
    private fun photo(f: Flow, from: String): File {
        val ctx = RuntimeEnvironment.getApplication()
        val bmp = ctx.assets.open("photos/$from.webp").use { BitmapFactory.decodeStream(it) }
        return f.photos.fresh().also { file -> file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) } }
    }

    @Test fun aFirstOuting() {
        val f = Flow(RuntimeEnvironment.getApplication(), dbName = null)
        f.clock = { now }
        rule.mainClock.autoAdvance = false
        rule.setContent { SafariTheme { App(f) } }

        tick(1200); shot("welcome")
        f.welcome("Maylis", "🦊"); tick(800); shot("home_empty")

        // The camera: the explorer's rule first, then (no camera here) the grown-up's permission screen.
        f.openCamera(); tick(500); shot("camera_rule")
        f.hideCameraTip(); tick(300); shot("camera_no_permission")

        f.photoTaken(photo(f, "renard-roux")); until { f.screen == Screen.Review }; tick(900); shot("review")
        f.keepPhoto(); tick(500); shot("where")
        f.choosePlace(Mode.IDENTIFY, Habitat.FOREST); tick(500); shot("family")
        f.chooseFamily(Mode.IDENTIFY, Habitat.FOREST, Group.MAMMAL); until { true }; tick(1500); shot("which_one")
        f.open("renard-roux", Mode.IDENTIFY); tick(1500); shot("is_it_him")
        f.confirm("renard-roux"); tick(400); shot("celebrate_start"); tick(2600); shot("celebrate")
        assertEquals(true, f.outcome?.firstTime)
        assertEquals("first", f.outcome?.medals?.single()?.id)

        // More animals over the day, with and without photos; one mystery.
        f.home()
        for ((i, id) in listOf("merle-noir", "mesange-charbonniere", "pie-bavarde", "pigeon-ramier", "coccinelle-a-sept-points", "heron-cendre").withIndex()) {
            now = now.plusMinutes(20L * (i + 1)); f.seenWithoutPhoto(id); f.home()
        }
        now = now.withHour(16)
        f.openCamera("crabe-vert"); f.photoTaken(photo(f, "crabe-vert"), "crabe-vert"); until { f.screen == Screen.Review }
        f.keepPhoto(); tick(3000); shot("celebrate_target")
        f.home()
        f.openCamera(); f.photoTaken(photo(f, "merle-noir")); until { f.screen == Screen.Review }
        f.keepPhoto(); tick(300); f.keepMystery(); tick(600); shot("home_after_day")
        assertEquals(1, f.progress.mysteries.size)
        assertEquals(8, f.progress.discovered.size)

        f.go(Screen.Album); tick(1500); shot("album")
        f.back(); f.go(Screen.Medals); tick(800); shot("medals")
        f.back(); f.go(Screen.Photos); tick(1500); shot("photos")
        val mystery = f.progress.mysteries.single()
        f.go(Screen.Photo(mystery.id)); tick(1200); shot("photo_mystery")
        f.solve(mystery); tick(400); f.choosePlace(Mode.IDENTIFY, Habitat.CITY); f.chooseFamily(Mode.IDENTIFY, Habitat.CITY, Group.BIRD); tick(1500); shot("solve_birds")
        f.confirm("merle-noir"); tick(3000); shot("solved_again")
        assertEquals("detective", f.outcome?.medals?.single()?.id)
        assertEquals(0, f.progress.mysteries.size)
        f.home()

        // The encyclopedia.
        f.go(Screen.Places(Mode.EXPLORE)); tick(600); shot("explore")
        f.choosePlace(Mode.EXPLORE, Habitat.SEA); tick(600); shot("explore_sea")
        f.chooseFamily(Mode.EXPLORE, Habitat.SEA, null); tick(1500); shot("explore_sea_all")
        f.open("vipere-aspic", Mode.EXPLORE); tick(1500); shot("card_caution")
        f.back(); f.open("renard-roux", Mode.EXPLORE); tick(1500); shot("card_seen")
        f.home(); f.go(Screen.Search(Mode.EXPLORE)); tick(400)
        rule.onNodeWithContentDescription("Nom de l'animal").performTextInput("herisson"); tick(1500); shot("search")
        assertEquals("herisson-d-europe", f.catalog.search("herisson").first().id)

        // The grown-ups: a wrong answer keeps the door shut, the right one opens it.
        f.home(); f.openGate(); tick(400); shot("gate")
        f.answer(-1); assertEquals(Screen.Gate, f.screen)
        f.answer(f.question.answer); tick(600); shot("parents")
        assertTrue(f.summary().contains("Maylis a fait"))
        f.go(Screen.Credits); tick(400); shot("credits")
        f.home()

        // Back from the review throws the photo away; "home" keeps an unnamed photo as a mystery.
        f.openCamera(); val dropped = photo(f, "renard-roux"); f.photoTaken(dropped); until { f.screen == Screen.Review }
        f.back(); assertNull(f.pending); assertTrue(!dropped.exists())
        f.openCamera(); f.photoTaken(photo(f, "renard-roux")); until { f.screen == Screen.Review }
        f.keepPhoto(); f.home(); assertEquals(1, f.progress.mysteries.size)
        assertNotNull(f.progress.mysteries.single().photo)
        tick(500); shot("home_end")

        // A tap on the home screen's big button opens the camera.
        rule.onNodeWithContentDescription("Photographier un animal").performClick(); tick(300)
        assertTrue(f.screen is Screen.Camera)
    }
}
