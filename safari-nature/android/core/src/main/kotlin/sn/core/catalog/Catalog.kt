package sn.core.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.text.Normalizer

/** The family a child sorts an animal into: words they know, not the zoologist's classes. */
@Serializable
enum class Group(val one: String, val many: String, val emoji: String) {
    @SerialName("mammal") MAMMAL("Mammifère", "Mammifères", "🦊"),
    @SerialName("bird") BIRD("Oiseau", "Oiseaux", "🐦"),
    @SerialName("insect") INSECT("Insecte", "Insectes", "🐞"),
    @SerialName("spider") SPIDER("Araignée", "Araignées & co", "🕷️"),
    @SerialName("reptile") REPTILE("Reptile", "Lézards & serpents", "🦎"),
    @SerialName("amphibian") AMPHIBIAN("Amphibien", "Grenouilles & tritons", "🐸"),
    @SerialName("fish") FISH("Poisson", "Poissons", "🐟"),
    @SerialName("mollusc") MOLLUSC("Mollusque", "Escargots & coquillages", "🐌"),
    @SerialName("crustacean") CRUSTACEAN("Crustacé", "Crabes & crevettes", "🦀"),
    @SerialName("other") OTHER("Petite bête", "Étoiles, vers & méduses", "⭐"),
}

/** Where the child is standing. An animal lives in one to four of them, the most typical first. */
@Serializable
enum class Habitat(val label: String, val where: String, val emoji: String) {
    @SerialName("forest") FOREST("Forêt", "dans la forêt", "🌲"),
    @SerialName("fields") FIELDS("Champs & prairies", "dans les champs", "🌾"),
    @SerialName("city") CITY("Ville & jardin", "en ville ou au jardin", "🏡"),
    @SerialName("water") WATER("Rivières & étangs", "au bord de l'eau", "🦆"),
    @SerialName("sea") SEA("Mer & plage", "à la mer", "🌊"),
    @SerialName("mountain") MOUNTAIN("Montagne", "à la montagne", "⛰️"),
    @SerialName("farm") FARM("Ferme", "à la ferme", "🚜"),
}

/** One species' card. Texts are French, written for 6–10 year olds. */
@Serializable
data class Animal(
    val id: String,
    val name: String,
    val latin: String,
    val en: String,
    val wiki: String,
    val group: Group,
    val habitats: List<Habitat>,
    /** 1 easy to see, 2 needs some luck, 3 rare or very discreet. Also the stars it is worth. */
    val rarity: Int,
    val size: String,
    val food: String,
    @SerialName("when") val season: String,
    val look: String,
    val fact: String,
    val emoji: String,
    val caution: String? = null,
) {
    val stars get() = rarity.coerceIn(1, 3)
}

/** Where a reference photo comes from, shown on the card and in the credits (Wikimedia Commons licences). */
@Serializable
data class Photo(
    val id: String,
    val file: String,
    val author: String,
    val license: String,
    val licenseUrl: String = "",
    val source: String,
)

class Catalog(animals: List<Animal>, photos: List<Photo> = emptyList()) {
    /** Alphabetical as a French reader expects (É with E, Œ with OE). */
    val animals: List<Animal> = animals.sortedBy { fold(it.name) }
    val photos: Map<String, Photo> = photos.associateBy { it.id }
    private val byId = this.animals.associateBy { it.id }

    operator fun get(id: String?): Animal? = id?.let { byId[it] }
    fun photo(id: String): Photo? = photos[id]

    fun inHabitat(h: Habitat?): List<Animal> = if (h == null) animals else animals.filter { h in it.habitats }
    fun of(h: Habitat?, g: Group?): List<Animal> = inHabitat(h).filter { g == null || it.group == g }

    /** The families found in a place, with how many animals each holds, in the enum's order. */
    fun groupsIn(h: Habitat?): List<Pair<Group, Int>> =
        inHabitat(h).groupingBy { it.group }.eachCount().toList().sortedBy { it.first.ordinal }

    /** Typed by a child: accents, case and plural "s" don't matter; names first, then words, then Latin or English. */
    fun search(query: String): List<Animal> {
        val q = fold(query).trim().removeSuffix("s")
        if (q.isEmpty()) return emptyList()
        fun rank(a: Animal): Int? {
            val n = fold(a.name)
            return when {
                n.startsWith(q) -> 0
                n.split(' ', '-', '\'').any { it.startsWith(q) } -> 1
                n.contains(q) -> 2
                fold(a.latin).contains(q) || fold(a.en).contains(q) -> 3
                fold(a.group.many).contains(q) || fold(a.group.one).startsWith(q) -> 4
                else -> null
            }
        }
        return animals.mapNotNull { a -> rank(a)?.let { it to a } }.sortedBy { it.first }.map { it.second }
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true }

        fun from(animalsJson: String, photosJson: String? = null) = Catalog(
            json.decodeFromString(ListSerializer(Animal.serializer()), animalsJson),
            photosJson?.let { json.decodeFromString(ListSerializer(Photo.serializer()), it) }.orEmpty(),
        )

        /** The catalogue shipped in this module's resources: the same file on the phone and in the tests. */
        fun load(): Catalog = from(resource("catalog/animals.json")!!, resource("catalog/photos.json"))

        private fun resource(path: String): String? =
            Catalog::class.java.classLoader?.getResourceAsStream(path)?.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}

/** Lower case, no accents, œ/æ spelled out: "Épervier d'Europe" and "epervier d europe" meet. */
fun fold(s: String): String =
    Normalizer.normalize(s.lowercase().replace("œ", "oe").replace("æ", "ae"), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace('’', '\'')
