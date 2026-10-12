package app.safarinature.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import sn.core.catalog.Habitat
import sn.core.progress.Sighting
import java.time.LocalDateTime

/**
 * The child's sightings, on the phone only. Same shape as the first version's `observations` table
 * (an animal and a time), plus the photo, where the child was, and whether a mystery was solved later.
 */
class Store(ctx: Context, name: String? = "safari.db") : SQLiteOpenHelper(ctx, name, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE sightings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                animal TEXT,
                at TEXT NOT NULL,
                photo TEXT,
                habitat TEXT,
                solved INTEGER NOT NULL DEFAULT 0
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun all(): List<Sighting> = readableDatabase.query("sightings", null, null, null, null, null, "at, id").use { c ->
        buildList {
            while (c.moveToNext()) add(
                Sighting(
                    id = c.getLong(0),
                    animal = c.getString(1),
                    at = LocalDateTime.parse(c.getString(2)),
                    photo = c.getString(3),
                    habitat = c.getString(4)?.let { h -> Habitat.entries.firstOrNull { it.name == h } },
                    solved = c.getInt(5) != 0,
                )
            )
        }
    }

    fun add(animal: String?, at: LocalDateTime, photo: String?, habitat: Habitat?): Sighting {
        val id = writableDatabase.insertOrThrow("sightings", null, ContentValues().apply {
            put("animal", animal)
            put("at", at.withNano(0).toString())
            put("photo", photo)
            put("habitat", habitat?.name)
        })
        return Sighting(id, animal, at.withNano(0), photo, habitat)
    }

    /** A mystery photo gets its name. */
    fun solve(id: Long, animal: String, habitat: Habitat?) {
        writableDatabase.update("sightings", ContentValues().apply {
            put("animal", animal)
            put("solved", 1)
            if (habitat != null) put("habitat", habitat.name)
        }, "id = ?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("sightings", "id = ?", arrayOf(id.toString()))
    }

    fun clear() {
        writableDatabase.delete("sightings", null, null)
    }
}

/** Who is exploring, and how the app should sound. */
class Profile(ctx: Context) {
    private val p = ctx.getSharedPreferences("profile", Context.MODE_PRIVATE)

    var name: String
        get() = p.getString("name", "") ?: ""
        set(v) = p.edit().putString("name", v.trim().take(20)).apply()
    var avatar: String
        get() = p.getString("avatar", "🦊") ?: "🦊"
        set(v) = p.edit().putString("avatar", v).apply()
    var welcomed: Boolean
        get() = p.getBoolean("welcomed", false)
        set(v) = p.edit().putBoolean("welcomed", v).apply()
    var sounds: Boolean
        get() = p.getBoolean("sounds", true)
        set(v) = p.edit().putBoolean("sounds", v).apply()
    var voice: Boolean
        get() = p.getBoolean("voice", true)
        set(v) = p.edit().putBoolean("voice", v).apply()
    /** The place chosen last, offered first next time (a beach day is many beach photos). */
    var lastHabitat: Habitat?
        get() = p.getString("lastHabitat", null)?.let { h -> Habitat.entries.firstOrNull { it.name == h } }
        set(v) = p.edit().putString("lastHabitat", v?.name).apply()
    /** The explorer's rule before the first photo. "cameraTip2": shown once more since it speaks of plants too. */
    var cameraTip: Boolean
        get() = p.getBoolean("cameraTip2", true)
        set(v) = p.edit().putBoolean("cameraTip2", v).apply()
}
