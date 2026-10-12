# Safari Nature

An Android app for children aged 6 to 10. They photograph the animals and plants they meet: in the forest, at the beach, in town, in the fields, by a river, in the mountains, on a farm. The app helps them find each one's name, then fills a sticker album. The catalogue covers mainland France and Western Europe. The app is in French.

It follows *Observations de Maylis*, the first Python/Tkinter version. Every animal and plant on Maylis's list is still here. The SMS gateway is gone: the phone's share sheet sends the day's summary instead, so the app needs no key, no account and no Internet access.

## What a child does with it

1. Taps the big **📸 Photo !** button. A full-screen camera opens with one shutter button and zoom in big steps (1×, 2×, 4×). They can also pick a photo from the gallery, or say "Pas de photo ?" if the animal ran away first.
2. **C'est un animal ou une plante ?** Two big doors, plus "Je ne sais pas" for both.
3. **Où es-tu ?** They pick the place: Forêt, Champs & prairies, Ville & jardin, Rivières & étangs, Mer & plage, Montagne, Ferme (animals only), or "Je ne sais pas".
4. **C'est quel genre d'animal (de plante) ?** They pick the family. Animals: birds, mammals, insects, and so on. Plants: Arbres, Arbustes & lianes, Fruitiers & baies, Fleurs & herbes, Algues & plantes de la plage, Fougères & mousses, Champs & potager.
5. **Lequel ressemble à ta photo ?** They see a grid of reference photos, the most common first, with their own photo always in view. Opening a card shows both photos side by side, then **C'est lui !** (or **C'est elle !** for a plant).
6. **Celebration.** Confetti, the stars the find is worth (⭐ common, ⭐⭐ needs luck, ⭐⭐⭐ rare), a new level or medal when one is earned, and a "Le savais-tu ?" fact, read aloud.

If they don't know what it is, **❓ Je ne sais pas ce que c'est** keeps the photo as a *mystery*, to name later with a grown-up (that earns the "Détective" medal).

On the home screen they also find:

- **Mon album**: two tabs, Animaux and Plantes, with every species in its place. Ones they have found are in colour with their own photo; the rest are greyed out with a "?".
- **Médailles**: goals for animals and plants ("Trouve 10 oiseaux", "Ami des arbres", "Petit botaniste", "Lève-tôt", "Tour de France", and so on), each with a progress bar.
- **Explorer**: the encyclopedia, by kingdom, place and family, with search. Search ignores accents and plurals, so "herisson" finds "Hérisson d'Europe".
- **Mes photos**: their photos, day by day.
- **Défi du jour**: one small mission a day ("Trouve un oiseau !", "Trouve un arbre !"). Winter missions never ask for insects, reptiles or flowers.

Every card has a 🔊 button that reads it aloud with the phone's French voice, for children who can't read yet. Animals that sting, bite or are venomous, and plants that are toxic, stinging, thorny or protected, have a calm ⚠️ safety note. Every other plant card reminds children never to taste a plant, berry or seaweed without a grown-up. Before the first photo, the camera shows the explorer's rule: approach quietly, don't disturb the animals, don't pull up plants, taste nothing without a grown-up.

## Grown-ups

The 🔒 in a corner of the home screen opens the parents' area. The door asks a multiplication, a standard parental gate. Inside:

- the day's summary, shared by SMS, WhatsApp or e-mail, as the first version's SMS did, plus the day's photos;
- statistics (the same figures as the first version's "Statistiques"): observations, animals, photos, first and last day, most-seen animals, counts by family;
- the explorer's first name and avatar, and switches for sounds and reading aloud;
- photo credits and licences;
- "Recommencer à zéro".

**Privacy:** the app has no Internet permission, and CI fails if a library adds one. Photos are re-saved without EXIF, so they carry no location and no phone model. The album lives in the app's private storage and is included in Android's own backup when a new phone is set up.

## Layout

```
safari-nature/
  android/            Kotlin/Compose app, same toolchain as Vault Field and Paroissien
    core/             pure Kotlin: catalogue, progress (stars, levels, medals), daily challenge, parents' summary
      src/main/resources/catalog/animals.json   the animals (348 species)
      src/main/resources/catalog/plants.json    the plants and seaweeds (223 species)
      src/main/resources/catalog/photos.json    author + licence of every reference photo
    app/              Compose UI, CameraX, SQLite store, text-to-speech
      src/main/assets/photos/<id>.webp          reference photos (Wikimedia Commons, else iNaturalist)
  tools/
    fetch_photos.py   reference photos + credits from Wikimedia Commons, else iNaturalist (free licences only)
    make_sounds.py    the app's sounds, synthesised (nothing to credit)
    deliver-apk.sh    build and drop the APKs in ~/VM-Exchange/APK/SafariNature/
```

The root `.github/workflows/safari-nature.yml` workflow runs the tests, renders every screen and builds the APKs. The debug APK and the screen captures are uploaded as artefacts.

## Build

JDK 21, as for the other apps:

```bash
cd android && JAVA_HOME=~/Android/jdk-21 ./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
```

`:app:testDebugUnitTest` opens the app as the phone does (`LaunchTest`: the launcher starts `MainActivity`, which gets its state from the system's view-model factory), then plays a child's first outing with Robolectric and writes a capture of every screen to `app/build/screens/`.

To deliver a test build, bump `versionCode` and `versionName` in `android/app/build.gradle.kts` first:

```bash
tools/deliver-apk.sh
```

The debug build (`app.safarinature.dev`) installs alongside the release (`app.safarinature`). The debug key is committed in `android/keystore/debug.keystore` (not a secret), so a CI build installs over a local one without losing the album. The release key stays outside the repo, in `~/.gradle/gradle.properties`:

```
SAFARI_NATURE_KEYSTORE=/home/<you>/.android-keys/safari-nature-release.jks
SAFARI_NATURE_KEYSTORE_PASSWORD=…
SAFARI_NATURE_KEY_ALIAS=safari-nature
SAFARI_NATURE_KEY_PASSWORD=…
```

## The catalogue

`core/src/main/resources/catalog/animals.json` and `plants.json` hold one entry per species, in the same format:

- the French name, Latin name and English name;
- its family and its places (one to four, the most typical first; the farm is for animals only);
- its rarity (1 to 3), which is also the stars it is worth;
- size, food (for a plant: its fruits and seeds) and when to see it;
- "Comment le reconnaître" (field marks a child can check) and "Le savais-tu ?";
- an emoji, and a safety note when there is a real one.

`CatalogTest` checks every card: unique ids that match the name, unique Latin names, complete texts within length limits, a freely licensed photo with its author for every species, enough plants in every place, and Maylis's animals and plants all present.

To add an animal or a plant:

1. Add its entry to `animals.json` or `plants.json`. The id is the slug of the French name.
2. Run `python3 -I tools/fetch_photos.py`. It takes the lead image of the species' French Wikipedia article, or a freely licensed iNaturalist photo when there is none.
3. If that photo is not a good one, put a Commons file name in `tools/photo_overrides.json` (`{"id": "File_name.jpg"}`) and run `python3 -I tools/fetch_photos.py --redo <id>`.

## Credits

- Reference photos: Wikimedia Commons, or iNaturalist when Commons has no free picture. Each one shows its author and licence on its card and in the app's credits (CC0, public domain, CC BY, CC BY-SA).
- Font: Fredoka, SIL Open Font License 1.1 (`app/src/main/assets/licences/OFL-Fredoka.txt`).
- Sounds: synthesised by `tools/make_sounds.py`.
