package com.sipscoop.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

/** Avatar id for the user's own photo, and for a photo picked but not saved yet. */
const val PHOTO_ID = "photo"
const val PHOTO_PENDING_ID = "photo_new"

/**
 * The profile photo picked from the gallery. It is cropped to a square, shrunk to 512 px and
 * kept in the app's private files only.
 */
object ProfilePhoto {
    /** Goes up whenever a photo file changes, so pictures on screen refresh. */
    val version = mutableIntStateOf(0)

    private fun saved(context: Context) = File(context.filesDir, "avatar.jpg")
    private fun pending(context: Context) = File(context.filesDir, "avatar_new.jpg")

    fun file(context: Context, id: String): File = if (id == PHOTO_PENDING_ID) pending(context) else saved(context)

    fun hasSaved(context: Context) = saved(context).exists()

    /** Reads, crops and stores a picked photo as the pending one. Call off the main thread. */
    fun importPending(context: Context, uri: Uri): Boolean = runCatching {
        val src = decode(context, uri) ?: return false
        val side = minOf(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val small = Bitmap.createScaledBitmap(square, 512, 512, true)
        FileOutputStream(pending(context)).use { small.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        true
    }.getOrDefault(false)

    /** Saving the profile: the pending photo becomes the real one. */
    fun commit(context: Context) {
        val p = pending(context)
        if (p.exists()) {
            p.copyTo(saved(context), overwrite = true)
            p.delete()
            version.intValue++
        }
    }

    fun discardPending(context: Context) {
        if (pending(context).delete()) version.intValue++
    }

    fun clear(context: Context) {
        saved(context).delete()
        pending(context).delete()
        version.intValue++
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        val cr = context.contentResolver
        return if (Build.VERSION.SDK_INT >= 28) {
            // Turns the photo the right way up and keeps it at a sensible size.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(cr, uri)) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val scale = maxOf(1, minOf(w, h) / 1024)
                decoder.setTargetSize(w / scale, h / scale)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1024) sample *= 2
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        }
    }
}

/** The user's photo in a circle, or the first character if the file is missing. */
@Composable
fun PhotoAvatar(id: String, modifier: Modifier) {
    val context = LocalContext.current
    val v = ProfilePhoto.version.intValue
    val image: ImageBitmap? = remember(id, v) {
        val f = ProfilePhoto.file(context, id)
        if (f.exists()) runCatching { BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() }.getOrNull() else null
    }
    if (image != null) {
        Image(image, contentDescription = "Your photo", contentScale = ContentScale.Crop, modifier = modifier.clip(CircleShape))
    } else {
        AvatarImage(AVATAR_FALLBACK, modifier)
    }
}

private const val AVATAR_FALLBACK = "m1"
