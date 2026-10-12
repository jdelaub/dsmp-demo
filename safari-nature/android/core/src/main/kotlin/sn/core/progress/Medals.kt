package sn.core.progress

import sn.core.catalog.Group
import sn.core.catalog.Habitat
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
        Medal(id, emoji, title, "Trouve $n animaux ${h.where}", n) { it.discoveredIn(h) }

    private fun season(m: Month) = when (m) {
        Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> 0
        Month.MARCH, Month.APRIL, Month.MAY -> 1
        Month.JUNE, Month.JULY, Month.AUGUST -> 2
        else -> 3
    }

    val all: List<Medal> = listOf(
        Medal("first", "👣", "Premier pas", "Découvre ton premier animal", 1) { it.discovered.size },
        Medal("photo10", "📸", "Photographe", "Prends 10 photos d'animaux", 10) { p -> p.sightings.count { it.photo != null } },
        Medal("species10", "🗂️", "Collectionneur", "Découvre 10 animaux différents", 10) { it.discovered.size },
        Medal("species25", "🎒", "Grand collectionneur", "Découvre 25 animaux différents", 25) { it.discovered.size },
        Medal("species50", "🏆", "Champion", "Découvre 50 animaux différents", 50) { it.discovered.size },
        Medal("species100", "📚", "Encyclopédie vivante", "Découvre 100 animaux différents", 100) { it.discovered.size },

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

        place("forest", "🌲", "Explorateur de la forêt", Habitat.FOREST, 10),
        place("sea", "🌊", "Loup de mer", Habitat.SEA, 10),
        place("city", "🏙️", "Safari en ville", Habitat.CITY, 10),
        place("fields", "🌾", "Roi des prés", Habitat.FIELDS, 10),
        place("water", "💧", "Aventurier des rivières", Habitat.WATER, 8),
        place("mountain", "⛰️", "Grimpeur des cimes", Habitat.MOUNTAIN, 5),
        place("farm", "🚜", "Petit fermier", Habitat.FARM, 6),

        Medal("everywhere", "🌍", "Tour de France", "Trouve un animal dans chaque endroit", Habitat.entries.size) { p ->
            Habitat.entries.count { p.discoveredIn(it) > 0 }
        },
        Medal("families", "🎨", "Toutes les familles", "Trouve un animal de chaque famille", Group.entries.size) { p ->
            Group.entries.count { p.discoveredIn(it) > 0 }
        },
        Medal("lucky", "🍀", "Chanceux", "Trouve un animal rare ⭐⭐⭐", 1) { p -> p.species.count { it.rarity >= 3 } },
        Medal("rare5", "💎", "Chasseur de trésors", "Trouve 5 animaux rares ⭐⭐⭐", 5) { p -> p.species.count { it.rarity >= 3 } },
        Medal("early", "🌅", "Lève-tôt", "Observe un animal avant 8 heures du matin", 1) { p ->
            p.sightings.count { it.at.hour in 4..7 }
        },
        Medal("night", "🦉", "Oiseau de nuit", "Observe un animal après 21 heures", 1) { p ->
            p.sightings.count { it.at.hour >= 21 || it.at.hour < 4 }
        },
        Medal("days5", "📅", "Explorateur fidèle", "Pars en exploration 5 jours différents", 5) { p ->
            p.sightings.map { it.at.toLocalDate() }.distinct().size
        },
        Medal("seasons", "🍂", "Quatre saisons", "Observe des animaux en hiver, au printemps, en été et en automne", 4) { p ->
            p.sightings.map { season(it.at.month) }.distinct().size
        },
        Medal("detective", "🕵️", "Détective", "Retrouve le nom d'un animal mystère", 1) { p ->
            p.sightings.count { it.solved && it.animal != null }
        },
    )

    operator fun get(id: String): Medal? = all.firstOrNull { it.id == id }
}
