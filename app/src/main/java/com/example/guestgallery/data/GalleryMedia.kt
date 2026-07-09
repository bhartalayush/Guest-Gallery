package com.example.guestgallery.data

import kotlinx.serialization.Serializable

@Serializable
data class MediaItem(
    val id: String,
    val uri: String,
    val name: String,
    val dateAdded: Long,
    val mimeType: String,
    val duration: Long? = null, // In milliseconds, null for images
    val size: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val isDecoy: Boolean = false,
    val albumId: String? = null
) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")
}

@Serializable
data class Album(
    val id: String,
    val name: String,
    val coverPhotoUri: String,
    val mediaCount: Int
)
