package com.raulshma.minkoa.files

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.util.Locale

private val blockedDirectoryNames = setOf("android", "lost.dir")

data class FileItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val size: Long,
    val dateModified: Long
) {
    val category: FileCategory
        get() = when {
            mimeType == null -> FileCategory.Other
            mimeType.startsWith("image/") -> FileCategory.Images
            mimeType.startsWith("video/") -> FileCategory.Video
            mimeType.startsWith("audio/") -> FileCategory.Audio
            mimeType == "application/pdf" -> FileCategory.Documents
            mimeType.startsWith("text/") -> FileCategory.Documents
            mimeType.contains("word") -> FileCategory.Documents
            mimeType.contains("spreadsheet") -> FileCategory.Documents
            mimeType.contains("presentation") -> FileCategory.Documents
            mimeType == "application/zip" -> FileCategory.Archives
            mimeType == "application/gzip" -> FileCategory.Archives
            mimeType.contains("rar") -> FileCategory.Archives
            mimeType.contains("tar") -> FileCategory.Archives
            mimeType.contains("7z") -> FileCategory.Archives
            else -> FileCategory.Other
        }
}

data class DirectoryEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val childCount: Int = 0
)

data class StorageDirectory(
    val name: String,
    val path: String,
    val icon: String
) {
    companion object {
        fun getStandardDirectories(): List<StorageDirectory> {
            val base = Environment.getExternalStorageDirectory()
            return listOf(
                StorageDirectory("Downloads", "${base}/Download", "download"),
                StorageDirectory("Documents", "${base}/Documents", "documents"),
                StorageDirectory("Pictures", "${base}/Pictures", "pictures"),
                StorageDirectory("Music", "${base}/Music", "music"),
                StorageDirectory("Videos", "${base}/Movies", "videos"),
                StorageDirectory("Audio", "${base}/Audio", "audio")
            )
        }
    }
}

enum class FileCategory(val label: String) {
    Downloads("Downloads"),
    Documents("Documents"),
    Audio("Audio"),
    Images("Images"),
    Video("Video"),
    Archives("Archives"),
    Other("Other")
}

data class CategorySummary(
    val category: FileCategory,
    val count: Int,
    val totalSize: Long
)

class FilesRepository(private val context: Context) {

    fun queryRecentFiles(limit: Int = 50): List<FileItem> {
        val items = mutableListOf<FileItem>()
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        context.contentResolver.query(uri, projection, null, null, sortOrder)
            ?.use { cursor ->
                val idCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val mimeType = cursor.getString(mimeCol)
                    if (mimeType.isNullOrBlank()) continue
                    val id = cursor.getLong(idCol)
                    items.add(
                        FileItem(
                            id = id,
                            uri = ContentUris.withAppendedId(uri, id),
                            displayName = cursor.getString(nameCol) ?: "Unknown",
                            mimeType = mimeType,
                            size = cursor.getLong(sizeCol),
                            dateModified = cursor.getLong(dateCol)
                        )
                    )
                    count++
                }
            }

        return items
    }

    fun computeCategorySummaries(files: List<FileItem>): List<CategorySummary> {
        val targetCategories = setOf(
            FileCategory.Downloads,
            FileCategory.Documents,
            FileCategory.Audio,
            FileCategory.Archives,
            FileCategory.Images,
            FileCategory.Video
        )
        return files
            .filter { it.category in targetCategories }
            .groupBy { it.category }
            .map { (cat, catFiles) ->
                CategorySummary(
                    category = cat,
                    count = catFiles.size,
                    totalSize = catFiles.sumOf { it.size }
                )
            }
            .sortedByDescending { it.count }
    }

    fun listDirectory(path: String): List<DirectoryEntry> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles()
            ?.asSequence()
            ?.filter(::shouldDisplayEntry)
            ?.map { file ->
                DirectoryEntry(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified() / 1000,
                    childCount = if (file.isDirectory) (file.listFiles()?.size ?: 0) else 0
                )
            }
            ?.sortedWith(compareByDescending<DirectoryEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?.toList()
            ?: emptyList()
    }

    private fun shouldDisplayEntry(file: File): Boolean {
        if (!file.canRead()) return false
        if (file.isHidden) return false
        val name = file.name
        if (name.startsWith('.')) return false
        if (file.isDirectory && name.lowercase(Locale.getDefault()) in blockedDirectoryNames) {
            return false
        }
        return true
    }
}

fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
    else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
}
