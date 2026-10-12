package sn.core.progress

import sn.core.catalog.Species
import sn.core.catalog.Catalog
import sn.core.catalog.Group
import sn.core.catalog.Habitat
import java.time.LocalDate
import java.time.Month

/** Today's little mission on the home screen. The same for the whole day, different the next. */
data class Challenge(val date: LocalDate, val emoji: String, val text: String, private val test: (Species) -> Boolean) {
    fun matches(a: Species) = test(a)
    fun done(p: Progress): Boolean = p.on(date).any { s -> p.catalog[s.animal]?.let(::matches) == true }
}

object Challenges {
    private val cold = setOf(Month.NOVEMBER, Month.DECEMBER, Month.JANUARY, Month.FEBRUARY)
    /** What hides or wilts in the cold months: no mission a child can't win in January. */
    private val sleepers = setOf(Group.INSECT, Group.SPIDER, Group.REPTILE, Group.AMPHIBIAN, Group.FLOWER, Group.FRUIT, Group.CROP)

    fun of(date: LocalDate, catalog: Catalog): Challenge {
        val winter = date.month in cold
        val pool = buildList {
            // Families with enough easy animals that any outing can find one.
            for (g in Group.entries) {
                if (winter && g in sleepers) continue
                if (catalog.all.count { it.group == g && it.rarity == 1 } < 3) continue
                add(Challenge(date, g.emoji, "Trouve ${article(g)} !") { it.group == g })
            }
            for (h in listOf(Habitat.CITY, Habitat.FIELDS, Habitat.FOREST, Habitat.WATER)) {
                add(Challenge(date, h.emoji, "Fais une découverte ${h.where} !") { h in it.habitats })
            }
            add(Challenge(date, "⭐", "Trouve un animal ou une plante à 2 étoiles ou plus !") { it.rarity >= 2 })
        }
        // A fixed shuffle of the days: neighbouring days rarely get the same kind of mission.
        val i = Math.floorMod(date.toEpochDay() * 7919L + 13, pool.size.toLong()).toInt()
        return pool[i]
    }

    private fun article(g: Group) = when (g) {
        Group.MAMMAL -> "un mammifère"
        Group.BIRD -> "un oiseau"
        Group.INSECT -> "un insecte"
        Group.SPIDER -> "une araignée"
        Group.REPTILE -> "un lézard, un serpent ou une tortue"
        Group.AMPHIBIAN -> "une grenouille, un crapaud ou un triton"
        Group.FISH -> "un poisson"
        Group.MOLLUSC -> "un escargot ou un coquillage"
        Group.CRUSTACEAN -> "un crabe ou une crevette"
        Group.OTHER -> "une étoile de mer, un ver ou une méduse"
        Group.TREE -> "un arbre"
        Group.SHRUB -> "un arbuste ou une liane"
        Group.FRUIT -> "un arbre fruitier ou une plante à baies"
        Group.FLOWER -> "une fleur sauvage"
        Group.SEAWEED -> "une algue ou une plante de la plage"
        Group.FERN -> "une fougère, une mousse ou un lichen"
        Group.CROP -> "une plante des champs ou du potager"
    }
}
