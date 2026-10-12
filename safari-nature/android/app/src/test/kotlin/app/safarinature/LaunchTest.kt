package app.safarinature

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The app as the phone starts it: the launcher opens MainActivity, which gets its Flow from the system's
 * view-model factory (not built by hand as in ScreensTest). Opening, a first name, then the home screen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w392dp-h850dp-xxhdpi")
class LaunchTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun theAppOpens() {
        rule.onNodeWithText("Safari Nature").assertExists()
        rule.onNodeWithContentDescription("Ton prénom").performTextInput("Maylis")
        rule.onNodeWithContentDescription("C'est parti !").performClick()
        rule.onNodeWithText("Salut Maylis !").assertExists()
        rule.onNodeWithContentDescription("Photographier un animal ou une plante").assertExists()
    }
}
