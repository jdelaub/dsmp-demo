package sn.core.progress

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The day told to a parent, ready to share by text message or e-mail. It replaces the first version's SMS
 * gateway: the phone's own share sheet sends it, so the app needs no account, key or Internet access.
 */
object Summary {
    private val day = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

    /** "matin" 6–12 h, "après-midi" 12–18 h, "soir" otherwise: the same periods as the first version. */
    fun period(hour: Int) = when (hour) {
        in 6..11 -> "le matin"
        in 12..17 -> "l'après-midi"
        else -> "le soir"
    }

    fun of(name: String, date: LocalDate, p: Progress): String {
        val today = p.on(date)
        val who = name.ifBlank { "Notre explorateur" }
        val heading = "🦊 Safari Nature — ${day.format(date)}"
        if (today.isEmpty()) return "$heading\n$who n'a encore rien observé aujourd'hui."

        val named = today.mapNotNull { p.catalog[it.animal] }
        val counts = named.groupingBy { it }.eachCount()
        val list = counts.entries.joinToString(", ") { (a, n) -> if (n > 1) "${a.name} (×$n)" else a.name }
        val firsts = counts.keys.filter { a -> p.discovered[a.id]?.toLocalDate() == date }
        val mysteries = today.count { it.animal == null }
        val photos = today.count { it.photo != null }
        val busiest = today.groupingBy { period(it.at.hour) }.eachCount().maxByOrNull { it.value }!!.key

        return buildString {
            appendLine(heading)
            append("$who a fait ${today.size} observation${if (today.size > 1) "s" else ""}")
            if (list.isNotEmpty()) append(" : $list")
            appendLine(".")
            if (firsts.isNotEmpty()) appendLine("✨ Nouvelle${if (firsts.size > 1) "s" else ""} découverte${if (firsts.size > 1) "s" else ""} : ${firsts.joinToString(", ") { "${it.name} ${"⭐".repeat(it.stars)}" }}")
            if (mysteries > 0) appendLine("❓ $mysteries mystère${if (mysteries > 1) "s" else ""} à identifier ensemble.")
            if (photos > 0) appendLine("📸 $photos photo${if (photos > 1) "s" else ""} prise${if (photos > 1) "s" else ""}.")
            appendLine("🕒 Le plus d'observations : $busiest.")
            append("${p.level.emoji} Niveau ${p.level.title} — ${p.stars} ⭐, ${p.discovered.size} découverte${if (p.discovered.size > 1) "s" else ""} dans l'album.")
        }
    }
}

/** The grown-ups' door: a multiplication beyond the times tables a 10-year-old knows by heart (the usual parental gate). */
data class Question(val a: Int, val b: Int) {
    val answer get() = a * b
    val text get() = "$a × $b"

    companion object {
        fun random(r: kotlin.random.Random = kotlin.random.Random.Default) = Question(r.nextInt(12, 20), r.nextInt(3, 10))
    }
}
