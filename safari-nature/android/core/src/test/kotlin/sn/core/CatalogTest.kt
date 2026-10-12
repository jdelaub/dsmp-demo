package sn.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sn.core.catalog.Catalog
import sn.core.catalog.Group
import sn.core.catalog.Habitat
import sn.core.catalog.Kingdom
import sn.core.catalog.fold
import java.io.File

/** The shipped catalogue, checked as data: what a child sees on every card must hold. */
class CatalogTest {
    private val c = Catalog.load()
    private val newEmoji = listOf("🪸", "🪼", "🪿", "🫏", "\u2B1B", "🫎", "🪽", "🫧", "🪺", "🪹", "🫨")

    @Test fun bigEnoughForEveryPlaceAndFamily() {
        assertTrue("${c.of(Kingdom.ANIMAL).size} animals", c.of(Kingdom.ANIMAL).size >= 280)
        assertTrue("${c.of(Kingdom.PLANT).size} plants", c.of(Kingdom.PLANT).size >= 150)
        for (h in Habitat.entries) assertTrue("$h has ${c.of(h = h).size}", c.of(h = h).size >= if (h == Habitat.FARM) 10 else 20)
        for (h in Habitat.entries - Habitat.FARM) assertTrue("$h has ${c.of(Kingdom.PLANT, h).size} plants", c.of(Kingdom.PLANT, h).size >= 10)
        for (g in Group.entries) assertTrue("$g has ${c.of(g = g).size}", c.of(g = g).size >= 6)
    }

    @Test fun idsAreUniqueSlugsOfTheName() {
        assertEquals(c.all.size, c.all.map { it.id }.toSet().size)
        assertEquals(c.all.size, c.all.map { it.latin.lowercase() }.toSet().size)
        for (a in c.all) {
            assertTrue(a.id, Regex("[a-z0-9]+(-[a-z0-9]+)*").matches(a.id))
            assertEquals(a.id, fold(a.name).replace(Regex("[^a-z0-9]+"), "-").trim('-'))
        }
    }

    @Test fun everyCardIsComplete() {
        for (a in c.all) {
            val why = "${a.id}: "
            assertTrue(why + "habitats", a.habitats.size in 1..4 && a.habitats.distinct() == a.habitats)
            assertTrue(why + "rarity", a.rarity in 1..3)
            // Genus then 1 to 4 words: species, "kl.", "subsp.", hybrids with "×" (Platanus × hispanica).
            assertTrue(why + "latin", Regex("[A-Z][a-z]+( (×|[a-z-]+\\.?)){1,4}").matches(a.latin))
            for ((k, v) in listOf("size" to a.size, "food" to a.food, "when" to a.season, "look" to a.look, "fact" to a.fact, "emoji" to a.emoji))
                assertTrue(why + k, v.isNotBlank() && v == v.trim())
            assertTrue(why + "fact too long for a child (${a.fact.length})", a.fact.length <= 260)
            assertTrue(why + "look too long (${a.look.length})", a.look.length <= 180)
            assertTrue(why + "caution", a.caution == null || a.caution.isNotBlank())
            assertTrue(why + "farm is for farm animals", Habitat.FARM !in a.habitats || a.group in setOf(Group.MAMMAL, Group.BIRD))
            // Emoji 14+ shows as an empty box on many children's (older) phones.
            assertTrue(why + "emoji ${a.emoji} too new", newEmoji.none { it in a.emoji })
        }
    }

    /** The species of the first version of the app (Maylis's list, animals and plants) are all still here. */
    @Test fun keepsTheFirstVersionsSpecies() {
        val latin = c.all.map { it.latin }.toSet()
        listOf("Ardea cinerea", "Egretta garzetta", "Numenius arquata", "Tadorna tadorna", "Anarhynchus alexandrinus",
            "Turdus merula", "Parus major", "Accipiter nisus", "Troglodytes troglodytes", "Carcinus maenas",
            "Cerastoderma edule", "Asterias rubens", "Arenicola marina", "Crangon crangon", "Erinaceus europaeus",
            "Oryctolagus cuniculus", "Pipistrellus pipistrellus", "Sus scrofa", "Cicindela hybrida", "Talitrus saltator",
            "Bembix rostrata",
            "Fucus vesiculosus", "Zostera marina", "Salicornia europaea", "Crataegus monogyna", "Pinus pinaster",
            "Quercus robur", "Fraxinus excelsior", "Salix caprea").forEach { assertTrue(it, it in latin) }
        // L'oyat changed genus (Ammophila → Calamagrostis): either name will do.
        assertTrue(latin.any { it.endsWith(" arenaria") && (it.startsWith("Ammophila") || it.startsWith("Calamagrostis")) })
    }

    @Test fun everySpeciesHasAFreelyLicensedPhoto() {
        val dir = File(System.getProperty("photos.dir") ?: "../app/src/main/assets/photos")
        val free = Regex("(?i)^(cc0|public domain|pd|attribution|cc by(-sa)?( [0-9.]+)?( [a-z]+)?)$")
        for (a in c.all) {
            val p = c.photo(a.id)
            assertTrue("${a.id} has no credit", p != null)
            assertTrue("${a.id}: ${p!!.license}", free.matches(p.license))
            assertTrue("${a.id}: author", p.author.isNotBlank())
            assertTrue("${a.id}.webp", File(dir, "${a.id}.webp").isFile)
        }
        assertEquals("credits for species that are gone", emptySet<String>(), c.photos.keys - c.all.map { it.id }.toSet())
    }

    @Test fun searchForgivesAccentsCaseAndPlurals() {
        assertEquals("Épervier d'Europe", c.search("epervier").first().name)
        assertEquals("Hérisson d'Europe", c.search("HERISSONS").first().name)
        assertTrue(c.search("renard").first().name.startsWith("Renard"))
        assertTrue(c.search("oiseau").all { it.group == Group.BIRD })
        assertEquals("Chêne pédonculé", c.search("chene ped").first().name)
        // "arbre" finds the trees by their family (and also the arbre à papillons, a shrub, by its name).
        assertTrue(c.search("arbre").count { it.group == Group.TREE } >= 30)
        assertTrue(c.search("Vulpes").any { it.id == "renard-roux" })
        assertTrue(c.search("  ").isEmpty())
    }

    @Test fun foldMeetsWhatChildrenType() {
        assertEquals("oeuf", fold("Œuf"))
        assertEquals("epervier d'europe", fold("Épervier d’Europe"))
    }
}
