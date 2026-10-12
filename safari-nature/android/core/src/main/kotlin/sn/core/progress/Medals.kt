package sn.core.progress

import sn.core.catalog.Group
import sn.core.catalog.Habitat
import sn.core.catalog.Kingdom
import java.time.Month

/**
 * A medal: a goal a child can read ("Trouve 10 oiseaux"), a counter towards it, and the moment it is won.
 * [measure] counts from the whole progress, so medals are never stored: they follow the album.
 */
class Medal(
    val id: String,
    val emoji: String,
    val title: String,
    val goal: String,
    val target: Int,
    private val measure: (Progress) -> Int,
) {
    fun value(p: Progress): Int = measure(p).coerceAtMost(target)
    fun won(p: Progress): Boolean = measure(p) >= target
    override fun toString() = id
}

object Medals {
    private fun group(id: String, emoji: String, title: String, g: Group, n: Int, what: String) =
        Medal(id, emoji, title, "Trouve $n $what", n) { it.discoveredIn(g) }

    private fun place(id: String, emoji: String, title: String, h: Habitat, n: Int) =
        Medal(id, emoji, title, if (h == Habitat.FARM) "Trouve $n animaux ${h.where}" else "Fais $n découvertes ${h.where}", n) { it.discoveredIn(h) }

    private fun season(m: Month) = when (m) {
        Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> 0
        Month.MARCH, Month.APRIL, Month.MAY -> 1
        Month.JUNE, Month.JULY, Month.AUGUST -> 2
        else -> 3
    }

    private val animalFamilies = Group.entries.filter { it.kingdom == Kingdom.ANIMAL }
    private val plantFamilies = Group.entries.filter { it.kingdom == Kingdom.PLANT }

    val all: List<Medal> = listOf(
        Medal("first", "👣", "Premier pas", "Fais ta première découverte", 1) { it.discovered.size },
        Medal("photo10", "📸", "Photographe", "Prends 10 photos d'animaux ou de plantes", 10) { p -> p.sightings.count { it.photo != null } },
        Medal("species10", "🗂️", "Collectionneur", "Découvre 10 animaux ou plantes différents", 10) { it.discovered.size },
        Medal("species25", "🎒", "Grand collectionneur", "Découvre 25 animaux ou plantes différents", 25) { it.discovered.size },
        Medal("species50", "🏆", "Champion", "Découvre 50 animaux ou plantes différents", 50) { it.discovered.size },
        Medal("species100", "📚", "Encyclopédie vivante", "Découvre 100 animaux ou plantes différents", 100) { it.discovered.size },

        group("birds10", "🐦", "Ami des oiseaux", Group.BIRD, 10, "oiseaux"),
        group("birds25", "🦅", "Roi du ciel", Group.BIRD, 25, "oiseaux"),
        group("mammals5", "🦊", "Ami des mammifères", Group.MAMMAL, 5, "mammifères"),
        group("insects10", "🐞", "Ami des insectes", Group.INSECT, 10, "insectes"),
        group("butterflies", "🦋", "Petit entomologiste", Group.INSECT, 25, "insectes"),
        group("frogs", "🐸", "Copain des grenouilles", Group.AMPHIBIAN, 3, "grenouilles, crapauds ou tritons"),
        group("dragons", "🐉", "Dompteur de dragons", Group.REPTILE, 3, "lézards, serpents ou tortues"),
        group("fish", "🎣", "Pêcheur d'images", Group.FISH, 5, "poissons"),
        Medal("beach", "🦀", "Trésors de la plage", "Trouve 8 crabes, coquillages ou étoiles de mer", 8) { p ->
            p.species.count { it.group in setOf(Group.CRUSTACEAN, Group.MOLLUSC, Group.OTHER) && Habitat.SEA in it.habitats }
        },
        Medal("spiders", "🕸️", "Sans peur", "Trouve 3 araignées", 3) { it.discoveredIn(Group.SPIDER) },

        group("trees10", "🌳", "Ami des arbres", Group.TREE, 10, "arbres"),
        group("flowers10", "🌼", "Bouquet de fleurs", Group.FLOWER, 10, "fleurs ou herbes"),
        group("fruits5", "🍎", "Roi du verger", Group.FRUIT, 5, "arbres fruitiers ou plantes à baies"),
        group("shrubs5", "🌿", "Gardien des haies", Group.SHRUB, 5, "arbustes ou lianes"),
        group("seaweeds5", "🏖️", "Jardinier des mers", Group.SEAWEED, 5, "algues ou plantes de la plage"),
        group("garden5", "🥕", "Petit jardinier", Group.CROP, 5, "plantes des champs ou du potager"),
        group("ferns3", "🌱", "Sous-bois secret", Group.FERN, 3, "fougères, mousses ou lichens"),
        Medal("plants25", "📗", "Herbier vivant", "Découvre 25 plantes différentes", 25) { it.discoveredIn(Kingdom.PLANT) },

        place("forest", "🌲", "Explorateur de la forêt", Habitat.FOREST, 10),
        place("sea", "🌊", "Loup de mer", Habitat.SEA, 10),
        place("city", "🏙️", "Safari en ville", Habitat.CITY, 10),
        place("fields", "🌾", "Roi des prés", Habitat.FIELDS, 10),
        place("water", "💧", "Aventurier des rivières", Habitat.WATER, 8),
        place("mountain", "⛰️", "Grimpeur des cimes", Habitat.MOUNTAIN, 5),
        place("farm", "🚜", "Petit fermier", Habitat.FARM, 6),

        Medal("everywhere", "🌍", "Tour de France", "Fais une découverte dans chaque endroit", Habitat.entries.size) { p ->
            Habitat.entries.count { p.discoveredIn(it) > 0 }
        },
        Medal("families", "🎨", "Toutes les familles", "Trouve un animal de chaque famille", animalFamilies.size) { p ->
            animalFamilies.count { p.discoveredIn(it) > 0 }
        },
        Medal("botanist", "🔎", "Petit botaniste", "Trouve une plante de chaque famille", plantFamilies.size) { p ->
            plantFamilies.count { p.discoveredIn(it) > 0 }
        },
        Medal("lucky", "🍀", "Chanceux", "Trouve un trésor rare ⭐⭐⭐", 1) { p -> p.species.count { it.rarity >= 3 } },
        Medal("rare5", "💎", "Chasseur de trésors", "Trouve 5 trésors rares ⭐⭐⭐", 5) { p -> p.species.count { it.rarity >= 3 } },
        Medal("early", "🌅", "Lève-tôt", "Fais une découverte avant 8 heures du matin", 1) { p ->
            p.sightings.count { it.at.hour in 4..7 }
        },
        Medal("night", "🦉", "Oiseau de nuit", "Fais une découverte après 21 heures", 1) { p ->
            p.sightings.count { it.at.hour >= 21 || it.at.hour < 4 }
        },
        Medal("days5", "📅", "Explorateur fidèle", "Pars en exploration 5 jours différents", 5) { p ->
            p.sightings.map { it.at.toLocalDate() }.distinct().size
        },
        Medal("seasons", "🍂", "Quatre saisons", "Explore la nature en hiver, au printemps, en été et en automne", 4) { p ->
            p.sightings.map { season(it.at.month) }.distinct().size
        },
        Medal("detective", "🕵️", "Détective", "Retrouve le nom d'un mystère", 1) { p ->
            p.sightings.count { it.solved && it.animal != null }
        },
    )

    operator fun get(id: String): Medal? = all.firstOrNull { it.id == id }
}
