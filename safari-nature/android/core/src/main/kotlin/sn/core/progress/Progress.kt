package sn.core.progress

import sn.core.catalog.Species
import sn.core.catalog.Catalog
import sn.core.catalog.Group
import sn.core.catalog.Habitat
import sn.core.catalog.Kingdom
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One time the child saw an animal or a plant ([animal] is its species id, the name kept from the first version).
 * [animal] is null for a "mystery": a photo kept to be
 * named later, with a grown-up; [solved] marks a mystery named afterwards. [habitat] is where the child said they were.
 */
data class Sighting(
    val id: Long,
    val animal: String?,
    val at: LocalDateTime,
    val photo: String? = null,
    val habitat: Habitat? = null,
    val solved: Boolean = false,
)

/** The explorer's rank, earned with stars. */
data class Level(val rank: Int, val title: String, val emoji: String, val stars: Int)

object Levels {
    val all = listOf(
        Level(1, "Explorateur en herbe", "🌱", 0),
        Level(2, "Petit curieux", "🔍", 5),
        Level(3, "Pisteur", "👣", 15),
        Level(4, "Aventurier", "🎒", 30),
        Level(5, "Garde nature", "🧭", 50),
        Level(6, "Naturaliste", "🔭", 80),
        Level(7, "Grand naturaliste", "🏅", 120),
        Level(8, "Maître de la nature", "🌍", 180),
        Level(9, "Légende vivante", "👑", 260),
    )

    fun of(stars: Int): Level = all.last { stars >= it.stars }
    fun next(stars: Int): Level? = all.firstOrNull { it.stars > stars }
}

/** Everything the child has done, read against the catalogue. Immutable: a new sighting makes a new Progress. */
class Progress(val catalog: Catalog, sightings: List<Sighting>) {
    val sightings: List<Sighting> = sightings.sortedBy { it.at }

    /** Each species seen, with the first time it was seen. Mystery photos and unknown ids don't count. */
    val discovered: Map<String, LocalDateTime> = buildMap {
        for (s in this@Progress.sightings) if (s.animal != null && catalog[s.animal] != null && s.animal !in this) put(s.animal, s.at)
    }
    val species: List<Species> get() = discovered.keys.mapNotNull { catalog[it] }

    val stars: Int = discovered.keys.sumOf { catalog[it]?.stars ?: 0 }
    val level: Level get() = Levels.of(stars)
    val nextLevel: Level? get() = Levels.next(stars)

    /** 0..1 along the way to the next level (1 at the last level). */
    val levelProgress: Float get() {
        val next = nextLevel ?: return 1f
        return (stars - level.stars).toFloat() / (next.stars - level.stars)
    }

    val photos: List<Sighting> get() = sightings.filter { it.photo != null }.reversed()
    val mysteries: List<Sighting> get() = sightings.filter { it.animal == null }.reversed()

    fun seen(id: String) = id in discovered
    fun count(id: String) = sightings.count { it.animal == id }
    fun photosOf(id: String): List<Sighting> = sightings.filter { it.animal == id && it.photo != null }.reversed()
    fun lastPhotoOf(id: String): String? = sightings.lastOrNull { it.animal == id && it.photo != null }?.photo

    fun discoveredIn(g: Group) = species.count { it.group == g }
    fun discoveredIn(k: Kingdom) = species.count { it.kingdom == k }
    fun discoveredIn(h: Habitat) = species.count { h in it.habitats }

    /** Days with at least one sighting, newest first, each with its sightings in order. */
    val days: List<Pair<LocalDate, List<Sighting>>> get() =
        sightings.groupBy { it.at.toLocalDate() }.toList().sortedByDescending { it.first }

    fun on(date: LocalDate) = sightings.filter { it.at.toLocalDate() == date }

    /** What one more sighting changes: the celebration screen shows exactly this. */
    fun with(s: Sighting): Pair<Progress, Outcome> {
        val after = Progress(catalog, sightings + s)
        val animal = catalog[s.animal]
        val first = animal != null && !seen(animal.id)
        val wonBefore = Medals.all.filter { it.won(this) }.map { it.id }.toSet()
        return after to Outcome(
            animal = animal,
            firstTime = first,
            stars = after.stars - stars,
            levelUp = after.level.takeIf { it.rank > level.rank },
            medals = Medals.all.filter { it.id !in wonBefore && it.won(after) },
            times = animal?.let { after.count(it.id) } ?: 0,
        )
    }

    /** For the parents: the same figures the first version of the app showed in "Statistiques". */
    fun stats(): Stats = Stats(
        sightings = sightings.size,
        species = discovered.size,
        photos = sightings.count { it.photo != null },
        first = sightings.firstOrNull()?.at?.toLocalDate(),
        last = sightings.lastOrNull()?.at?.toLocalDate(),
        top = sightings.mapNotNull { catalog[it.animal] }.groupingBy { it }.eachCount()
            .toList().sortedWith(compareByDescending<Pair<Species, Int>> { it.second }.thenBy { it.first.name }).take(5),
        byGroup = Group.entries.map { g -> g to discoveredIn(g) }.filter { it.second > 0 },
    )
}

data class Outcome(
    val animal: Species?,
    val firstTime: Boolean,
    val stars: Int,
    val levelUp: Level?,
    val medals: List<Medal>,
    /** How many times this animal has been seen, this one included. */
    val times: Int,
)

data class Stats(
    val sightings: Int,
    val species: Int,
    val photos: Int,
    val first: LocalDate?,
    val last: LocalDate?,
    val top: List<Pair<Species, Int>>,
    val byGroup: List<Pair<Group, Int>>,
)
