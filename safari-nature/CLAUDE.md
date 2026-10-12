# Safari Nature

A French Android app (Kotlin/Compose) for children aged 6 to 10: they photograph animals, name them, and fill an album. See README.md for the full picture.

## Build

- JDK 21 (`JAVA_HOME=~/Android/jdk-21` on the VM). Run one Gradle build at a time, and never an emulator.
- Fast checks, run in `android/`: `./gradlew :core:test`.
- Every screen: `./gradlew :app:testDebugUnitTest`. It runs Robolectric and writes the captures to `app/build/screens/`; look at them after any UI change.
- Delivery: bump `versionCode` and `versionName` in `android/app/build.gradle.kts`, then run `tools/deliver-apk.sh`.

## Rules

- No Internet permission, ever: it is a children's app, so nothing leaves the phone except what a parent shares. `deliver-apk.sh` and the CI check this.
- All UI text is French, written for a 6-year-old: short words, `tu`, and a 🔊 button wherever there is text to read. Touch targets are at least 56 dp; main buttons are at least 64 dp.
- Never lose a child's photo. An unnamed photo becomes a "mystery animal". Deleting always asks first.
- The catalogue is `android/core/src/main/resources/catalog/animals.json`:
  - The id is the slug of the French name.
  - Facts must be true. Cautions are calm, and only for real dangers.
  - `CatalogTest` must pass.
- Reference photos come only from `tools/fetch_photos.py`: free licences only, credits in `photos.json`. If a photo is wrong, fix it with `tools/photo_overrides.json` and `--redo <id>`; never put a file in by hand.
- `core` is pure Kotlin (no Android). The app's state lives in `flow/Flow.kt`; screens only read it and call its methods.
