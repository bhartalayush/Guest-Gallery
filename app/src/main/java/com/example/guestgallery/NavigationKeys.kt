package com.example.guestgallery

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Setup : NavKey
@Serializable data object AlbumGrid : NavKey
@Serializable data class AlbumDetail(val albumId: String, val albumName: String) : NavKey
@Serializable data class PhotoDetail(val albumId: String, val selectedPhotoId: String) : NavKey
@Serializable data class Auth(val targetAction: String) : NavKey // targetAction can be "settings" or "exit"
@Serializable data object Settings : NavKey
