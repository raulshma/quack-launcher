package com.raulshma.minkoa.gallery

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class GalleryImage(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val bucketId: String?,
    val bucketName: String?,
    val width: Int,
    val height: Int
) {
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) {
            width.toFloat() / height.toFloat()
        } else 1f
}

data class GalleryAlbum(
    val bucketId: String,
    val bucketName: String,
    val coverUri: Uri?,
    val count: Int
)

class GalleryRepository(private val context: Context) {

    fun queryImages(): List<GalleryImage> {
        val images = mutableListOf<GalleryImage>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(uri, projection, null, null, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val bucketIdCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    images.add(
                        GalleryImage(
                            id = id,
                            uri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            ),
                            displayName = cursor.getString(nameCol) ?: "",
                            dateAdded = cursor.getLong(dateCol),
                            bucketId = cursor.getString(bucketIdCol),
                            bucketName = cursor.getString(bucketNameCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol)
                        )
                    )
                }
            }

        return images
    }

    fun computeAlbums(images: List<GalleryImage>): List<GalleryAlbum> {
        return images
            .groupBy { it.bucketId }
            .mapNotNull { (bucketId, bucketImages) ->
                if (bucketId == null) return@mapNotNull null
                GalleryAlbum(
                    bucketId = bucketId,
                    bucketName = bucketImages.firstOrNull()?.bucketName ?: "Unknown",
                    coverUri = bucketImages.firstOrNull()?.uri,
                    count = bucketImages.size
                )
            }
            .sortedByDescending { it.count }
    }
}
