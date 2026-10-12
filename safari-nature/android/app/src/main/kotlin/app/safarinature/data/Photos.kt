package app.safarinature.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The child's photos, in the app's private storage. Every photo is straightened, made no larger than
 * 1600 px and saved again: the JPEG keeps no EXIF (no place, no phone model), and an album of a few
 * hundred photos stays small.
 */
class Photos(private val ctx: Context) {
    val dir: File = File(ctx.filesDir, "photos").apply { mkdirs() }
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")

    fun file(name: String) = File(dir, name)

    /** Where the camera writes before the photo is kept. */
    fun fresh(): File = File(dir, "IMG-${stamp.format(LocalDateTime.now())}.jpg")

    /** Straighten and shrink a just-taken photo in place. Returns its name, or null if it could not be read. */
    fun settle(f: File): String? {
        val bmp = decode(f.absolutePath, MAX) ?: return null.also { f.delete() }
        val turned = rotate(bmp, orientation(f.absolutePath))
        f.outputStream().use { turned.compress(Bitmap.CompressFormat.JPEG, 86, it) }
        return f.name
    }

    /** A picture picked from the phone's gallery becomes one of the album's photos. */
    fun import(uri: Uri): String? {
        val tmp = fresh()
        runCatching { ctx.contentResolver.openInputStream(uri)?.use { i -> tmp.outputStream().use { i.copyTo(it) } } }.getOrNull() ?: return null
        return settle(tmp)
    }

    fun delete(name: String?) {
        if (name != null) file(name).delete()
    }

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    companion object {
        const val MAX = 1600

        /** Decoded no bigger than [maxPx] on its long side (power-of-two sampling, then an exact scale). */
        fun decode(path: String, maxPx: Int): Bitmap? = decode(maxPx) { o -> BitmapFactory.decodeFile(path, o) }

        fun decode(maxPx: Int, read: (BitmapFactory.Options?) -> Bitmap?): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            read(bounds)
            val long = maxOf(bounds.outWidth, bounds.outHeight)
            if (long <= 0) return null
            var sample = 1
            while (long / (sample * 2) >= maxPx) sample *= 2
            val b = read(BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
            val l = maxOf(b.width, b.height)
            if (l <= maxPx) return b
            val k = maxPx.toFloat() / l
            return Bitmap.createScaledBitmap(b, (b.width * k).toInt().coerceAtLeast(1), (b.height * k).toInt().coerceAtLeast(1), true)
        }

        private fun orientation(path: String) = runCatching {
            ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        private fun rotate(b: Bitmap, o: Int): Bitmap {
            val m = Matrix()
            when (o) {
                ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
                else -> return b
            }
            return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
        }
    }
}
