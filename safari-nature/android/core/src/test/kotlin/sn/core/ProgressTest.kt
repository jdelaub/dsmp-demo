package sn.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sn.core.catalog.Animal
import sn.core.catalog.Catalog
import sn.core.catalog.Group
import sn.core.catalog.Habitat
import sn.core.progress.Challenges
import sn.core.progress.Levels
import sn.core.progress.Medals
import sn.core.progress.Progress
import sn.core.progress.Question
import sn.core.progress.Sighting
import sn.core.progress.Summary
import java.time.LocalDate
import java.time.LocalDateTime

/** Stars, levels, medals, the day's mission and the parents' summary, on a small made-up catalogue. */
class ProgressTest {
    private fun animal(id: String, g: Group, vararg h: Habitat, rarity: Int = 1) =
        Animal(id, id.replaceFirstChar { it.uppercase() }, "Genus $id", id, id, g, h.toList(), rarity, "1 cm", "Tout", "Toujours", "Regarde.", "Vrai.", "🐾")

    private val cat = Catalog(listOf(
        animal("merle", Group.BIRD, Habitat.CITY, Habitat.FOREST),
        animal("mesange", Group.BIRD, Habitat.CITY),
        animal("pie", Group.BIRD, Habitat.CITY, Habitat.FIELDS),
        animal("heron", Group.BIRD, Habitat.WATER, rarity = 2),
        animal("renard", Group.MAMMAL, Habitat.FOREST, rarity = 2),
        animal("lynx", Group.MAMMAL, Habitat.MOUNTAIN, rarity = 3),
        animal("coccinelle", Group.INSECT, Habitat.CITY),
        animal("abeille", Group.INSECT, Habitat.FIELDS),
        animal("fourmi", Group.INSECT, Habitat.CITY),
        animal("crabe", Group.CRUSTACEAN, Habitat.SEA),
    ))
    private val t0 = LocalDateTime.of(2026, 5, 2, 10, 0)
    private var nextId = 1L
    private fun s(animal: String?, at: LocalDateTime = t0, photo: String? = null, solved: Boolean = false) =
        Sighting(nextId++, animal, at, photo, solved = solved)

    @Test fun starsAreEarnedOncePerSpeciesByRarity() {
        val p = Progress(cat, listOf(s("merle"), s("merle"), s("renard"), s("lynx"), s(null, photo = "x.jpg"), s("unknown-id")))
        assertEquals(1 + 2 + 3, p.stars)
        assertEquals(setOf("merle", "renard", "lynx"), p.discovered.keys)
        assertEquals(2, p.count("merle"))
        assertEquals(1, p.mysteries.size)
        assertEquals(Levels.all[1], p.level) // 6 stars: Petit curieux
        assertEquals(Levels.all[2], p.nextLevel)
        assertEquals(1f / 10f, p.levelProgress, 0.001f)
    }

    @Test fun levelsCoverEveryCount() {
        assertEquals("Explorateur en herbe", Levels.of(0).title)
        assertEquals("Légende vivante", Levels.of(10_000).title)
        assertNull(Levels.next(10_000))
        assertTrue(Levels.all.zipWithNext().all { (a, b) -> a.stars < b.stars && a.rank + 1 == b.rank })
    }

    @Test fun anOutcomeTellsWhatIsNew() {
        val start = Progress(cat, listOf(s("merle"), s("mesange"), s("pie"), s("coccinelle")))
        val (after, o) = start.with(s("renard", photo = "r.jpg"))
        assertTrue(o.firstTime)
        assertEquals(2, o.stars)
        assertEquals("Petit curieux", o.levelUp?.title)
        assertEquals(1, o.times)
        assertEquals(6, after.stars)

        val (_, again) = after.with(s("renard"))
        assertFalse(again.firstTime)
        assertEquals(0, again.stars)
        assertNull(again.levelUp)
        assertEquals(2, again.times)
        assertTrue(again.medals.isEmpty())
    }

    @Test fun medalsFollowTheAlbum() {
        val (_, first) = Progress(cat, emptyList()).with(s("merle"))
        assertEquals(listOf("first"), first.medals.map { it.id })

        val birds = Progress(cat, listOf(s("merle"), s("mesange"), s("pie"), s("heron")))
        assertEquals(4, Medals["birds10"]!!.value(birds))
        assertFalse(Medals["birds10"]!!.won(birds))

        val (_, lucky) = birds.with(s("lynx", at = t0.withHour(7)))
        assertTrue(lucky.medals.map { it.id }.containsAll(listOf("lucky", "early")))

        val (_, solved) = birds.with(s("crabe", photo = "c.jpg", solved = true))
        assertTrue("detective" in solved.medals.map { it.id })
    }

    @Test fun theDaysMissionIsStableAndWinnable() {
        val day = LocalDate.of(2026, 1, 15)
        assertEquals(Challenges.of(day, cat).text, Challenges.of(day, cat).text)
        // In winter, no mission about insects (they hide): every January day.
        for (d in 1..31) assertFalse(Challenges.of(LocalDate.of(2026, 1, d), cat).text.contains("insecte"))
        val texts = (1..60).map { Challenges.of(LocalDate.of(2026, 5, 1).plusDays(it.toLong()), cat).text }.toSet()
        assertTrue("missions vary: $texts", texts.size >= 4)

        val c = Challenges.of(LocalDate.of(2026, 5, 2), cat)
        val match = cat.animals.first { c.matches(it) }
        assertFalse(c.done(Progress(cat, emptyList())))
        assertTrue(c.done(Progress(cat, listOf(s(match.id)))))
        assertFalse(c.done(Progress(cat, listOf(s(match.id, at = t0.minusDays(1))))))
    }

    @Test fun theParentsSummaryTellsTheDay() {
        val p = Progress(cat, listOf(
            s("merle", t0.minusDays(3)),
            s("merle", t0.withHour(14)), s("merle", t0.withHour(15), "m.jpg"), s("heron", t0.withHour(16)), s(null, t0.withHour(9), "x.jpg"),
        ))
        val text = Summary.of("Maylis", t0.toLocalDate(), p)
        assertTrue(text, text.contains("samedi 2 mai"))
        assertTrue(text, text.contains("Maylis a fait 4 observations : Merle (×2), Heron."))
        assertTrue(text, text.contains("Nouvelle découverte : Heron ⭐⭐"))
        assertTrue(text, text.contains("1 animal mystère"))
        assertTrue(text, text.contains("2 photos prises"))
        assertTrue(text, text.contains("l'après-midi"))
        assertTrue(Summary.of("Maylis", t0.toLocalDate().plusDays(1), p).contains("pas encore observé"))
    }

    @Test fun statsForTheParents() {
        val st = Progress(cat, listOf(s("merle", t0.minusDays(2)), s("merle"), s("renard", photo = "r.jpg"))).stats()
        assertEquals(3, st.sightings); assertEquals(2, st.species); assertEquals(1, st.photos)
        assertEquals(t0.toLocalDate().minusDays(2), st.first)
        assertEquals("merle", st.top.first().first.id)
        assertEquals(listOf(Group.MAMMAL to 1, Group.BIRD to 1), st.byGroup)
    }

    @Test fun theParentsQuestion() {
        repeat(200) {
            val q = Question.random()
            assertTrue(q.a in 12..19 && q.b in 3..9)
            assertEquals(q.a * q.b, q.answer)
        }
    }
}
