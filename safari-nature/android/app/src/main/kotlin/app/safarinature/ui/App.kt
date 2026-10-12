package app.safarinature.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.safarinature.flow.Flow
import app.safarinature.flow.Screen

@Composable
fun App(f: Flow) {
    BackHandler(f.stack.size > 1) { f.back() }
    Page {
        // The camera screens hold the camera: no cross-fade into or out of them.
        val cameras = setOf(Screen.Camera::class, Screen.Review::class)
        AnimatedContent(f.screen, transitionSpec = {
            if (initialState::class in cameras || targetState::class in cameras) EnterTransition.None togetherWith ExitTransition.None
            else (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.97f)) togetherWith fadeOut(tween(150))
        }, label = "screen") { s ->
            Box(Modifier.fillMaxSize().then(if (s is Screen.Camera || s is Screen.Photo) Modifier else Modifier.statusBarsPadding().navigationBarsPadding())) {
                when (s) {
                    Screen.Welcome -> WelcomeScreen(f)
                    Screen.Home -> HomeScreen(f)
                    is Screen.Camera -> CameraScreen(f, s.target)
                    Screen.Review -> ReviewScreen(f)
                    is Screen.Places -> PlacesScreen(f, s.mode)
                    is Screen.Families -> FamiliesScreen(f, s.mode, s.habitat)
                    is Screen.Animals -> AnimalsScreen(f, s.mode, s.habitat, s.group)
                    is Screen.Search -> SearchScreen(f, s.mode)
                    is Screen.Card -> CardScreen(f, s.id, s.mode)
                    Screen.Celebrate -> CelebrateScreen(f)
                    Screen.Album -> AlbumScreen(f)
                    Screen.Medals -> MedalsScreen(f)
                    Screen.Photos -> PhotosScreen(f)
                    is Screen.Photo -> PhotoScreen(f, s.sighting)
                    Screen.Gate -> GateScreen(f)
                    Screen.Parents -> ParentsScreen(f)
                    Screen.Credits -> CreditsScreen(f)
                }
            }
        }
        Toast(f, Modifier.statusBarsPadding().padding(top = 12.dp))
        if (f.busy) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = C.Orange, strokeWidth = 6.dp)
        }
    }
}
