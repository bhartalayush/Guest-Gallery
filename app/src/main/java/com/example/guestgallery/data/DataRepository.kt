package com.example.guestgallery.data

import android.content.Context
import android.content.SharedPreferences
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.absoluteValue

interface GalleryRepository {
    val isSetupCompleted: Boolean
    val isGuestMode: StateFlow<Boolean>
    fun setGuestMode(enabled: Boolean)
    
    fun savePin(pin: String)
    fun verifyPin(pin: String): Boolean
    fun clearPin()
    
    val albums: Flow<List<Album>>
    val allMedia: Flow<List<MediaItem>>
    fun getMediaForAlbum(albumId: String): Flow<List<MediaItem>>
    
    val showcasedMedia: StateFlow<List<MediaItem>>
    fun addShowcasedMedia(uris: List<String>)
    fun removeShowcasedMedia(uris: List<String>)
    fun clearAllShowcasedMedia()
    
    val areDecoysEnabled: StateFlow<Boolean>
    fun setDecoysEnabled(enabled: Boolean)

    // Dynamic Decoy CMS (Add Decoy Mode)
    val isDecoyEditMode: StateFlow<Boolean>
    fun setDecoyEditMode(enabled: Boolean)

    val decoyAlbums: StateFlow<List<Album>>
    val decoyMedia: StateFlow<List<MediaItem>>

    fun createDecoyAlbum(name: String)
    fun deleteDecoyAlbum(albumId: String)
    fun addDecoyMedia(albumId: String, uris: List<String>)
    fun deleteDecoyMedia(mediaId: String)

    // App icon — persists which icon style is active (default or custom uploaded)
    val activeAppIcon: StateFlow<String>
    fun setActiveAppIcon(iconName: String)

    val customIconPath: StateFlow<String?>
    fun setCustomIconPath(path: String?)

    // UI Customization Preferences
    val uiOverrideEnabled: StateFlow<Boolean>
    val uiOverrideColumns: StateFlow<Int>
    val uiOverrideSpacingDp: StateFlow<Float>
    val uiOverridePhotoCornersDp: StateFlow<Int>
    val uiOverrideAlbumCornersDp: StateFlow<Int>
    val uiOverrideThemeBrand: StateFlow<String>

    fun setUiOverrideEnabled(enabled: Boolean)
    fun setUiOverrideColumns(columns: Int)
    fun setUiOverrideSpacingDp(spacing: Float)
    fun setUiOverridePhotoCornersDp(corners: Int)
    fun setUiOverrideAlbumCornersDp(corners: Int)
    fun setUiOverrideThemeBrand(brand: String)

    // Search Page Customizations
    val searchPeople: StateFlow<String>
    val searchPlaces: StateFlow<String>
    val searchThings: StateFlow<String>
    val searchThumbnails: StateFlow<Map<String, String>>

    fun setSearchPeople(names: String)
    fun setSearchPlaces(names: String)
    fun setSearchThings(names: String)
    fun setSearchThumbnail(name: String, uri: String?)

    // Security Options
    val requireAuthOnExit: StateFlow<Boolean>
    fun setRequireAuthOnExit(enabled: Boolean)

    val isAppLocked: StateFlow<Boolean>
    fun setAppLocked(locked: Boolean)
}

class DefaultGalleryRepository(private val context: Context) : GalleryRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("guest_gallery_prefs", Context.MODE_PRIVATE)
    
    private val _isGuestMode = MutableStateFlow(prefs.getBoolean("is_guest_mode", false))
    override val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    private val _areDecoysEnabled = MutableStateFlow(prefs.getBoolean("are_decoys_enabled", true))
    override val areDecoysEnabled: StateFlow<Boolean> = _areDecoysEnabled.asStateFlow()

    private val _showcasedMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    override val showcasedMedia: StateFlow<List<MediaItem>> = _showcasedMedia.asStateFlow()

    // Add Decoy Mode edit state
    private val _isDecoyEditMode = MutableStateFlow(false)
    override val isDecoyEditMode: StateFlow<Boolean> = _isDecoyEditMode.asStateFlow()

    // Dynamic Decoy Database states
    private val _decoyAlbums = MutableStateFlow<List<Album>>(emptyList())
    override val decoyAlbums: StateFlow<List<Album>> = _decoyAlbums.asStateFlow()

    private val _decoyMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    override val decoyMedia: StateFlow<List<MediaItem>> = _decoyMedia.asStateFlow()

    private val _uiOverrideEnabled = MutableStateFlow(prefs.getBoolean("ui_override_enabled", false))
    override val uiOverrideEnabled: StateFlow<Boolean> = _uiOverrideEnabled.asStateFlow()

    private val _uiOverrideColumns = MutableStateFlow(prefs.getInt("ui_override_columns", 3))
    override val uiOverrideColumns: StateFlow<Int> = _uiOverrideColumns.asStateFlow()

    private val _uiOverrideSpacingDp = MutableStateFlow(prefs.getFloat("ui_override_spacing_dp", 1.5f))
    override val uiOverrideSpacingDp: StateFlow<Float> = _uiOverrideSpacingDp.asStateFlow()

    private val _uiOverridePhotoCornersDp = MutableStateFlow(prefs.getInt("ui_override_photo_corners_dp", 0))
    override val uiOverridePhotoCornersDp: StateFlow<Int> = _uiOverridePhotoCornersDp.asStateFlow()

    private val _uiOverrideAlbumCornersDp = MutableStateFlow(prefs.getInt("ui_override_album_corners_dp", 16))
    override val uiOverrideAlbumCornersDp: StateFlow<Int> = _uiOverrideAlbumCornersDp.asStateFlow()

    private val _uiOverrideThemeBrand = MutableStateFlow(prefs.getString("ui_override_theme_brand", "samsung") ?: "samsung")
    override val uiOverrideThemeBrand: StateFlow<String> = _uiOverrideThemeBrand.asStateFlow()

    private val _activeAppIcon = MutableStateFlow(prefs.getString("active_app_icon", "default") ?: "default")
    override val activeAppIcon: StateFlow<String> = _activeAppIcon.asStateFlow()

    private val _customIconPath = MutableStateFlow(prefs.getString("custom_icon_path", null))
    override val customIconPath: StateFlow<String?> = _customIconPath.asStateFlow()

    // Search Mock Customizations Flow states
    private val _searchPeople = MutableStateFlow(prefs.getString("search_people_names", "Me, Dad, Mom, Buddy, Emma") ?: "Me, Dad, Mom, Buddy, Emma")
    override val searchPeople: StateFlow<String> = _searchPeople.asStateFlow()

    private val _searchPlaces = MutableStateFlow(prefs.getString("search_places_names", "New York, Paris, Tokyo, London") ?: "New York, Paris, Tokyo, London")
    override val searchPlaces: StateFlow<String> = _searchPlaces.asStateFlow()

    private val _searchThings = MutableStateFlow(prefs.getString("search_things_names", "Forests, Beaches, Sky, Animals, Selfies, Screenshots") ?: "Forests, Beaches, Sky, Animals, Selfies, Screenshots")
    override val searchThings: StateFlow<String> = _searchThings.asStateFlow()

    private val _searchThumbnails = MutableStateFlow<Map<String, String>>(emptyMap())
    override val searchThumbnails: StateFlow<Map<String, String>> = _searchThumbnails.asStateFlow()

    private val _requireAuthOnExit = MutableStateFlow(prefs.getBoolean("require_auth_on_exit", true))
    override val requireAuthOnExit: StateFlow<Boolean> = _requireAuthOnExit.asStateFlow()

    private val _isAppLocked = MutableStateFlow(prefs.getString("owner_pin", null) != null)
    override val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    override val isSetupCompleted: Boolean
        get() = prefs.getString("owner_pin", null) != null

    init {
        // Load persisted decoy database from disk on startup
        if (!prefs.contains("decoy_albums_json")) {
            saveDecoyAlbumsToDisk(emptyList())
            saveDecoyMediaToDisk(emptyList())
        }
        _decoyAlbums.value = loadDecoyAlbumsFromDisk()
        _decoyMedia.value = loadDecoyMediaFromDisk()

        // Load custom thumbnails map
        _searchThumbnails.value = parseSearchThumbnails(prefs.getString("search_thumbnails_json", null))
    }

    override fun setGuestMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_guest_mode", enabled).apply()
        _isGuestMode.value = enabled
    }

    override fun savePin(pin: String) {
        prefs.edit().putString("owner_pin", pin).apply()
    }

    override fun verifyPin(pin: String): Boolean {
        val saved = prefs.getString("owner_pin", null)
        return saved != null && saved == pin
    }

    override fun clearPin() {
        prefs.edit().remove("owner_pin").apply()
    }

    override fun setDecoysEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("are_decoys_enabled", enabled).apply()
        _areDecoysEnabled.value = enabled
    }

    override fun setDecoyEditMode(enabled: Boolean) {
        _isDecoyEditMode.value = enabled
    }

    override val allMedia: Flow<List<MediaItem>> = combine(
        _showcasedMedia,
        _areDecoysEnabled,
        _decoyMedia
    ) { showcased, decoysEnabled, decoyMediaList ->
        val list = mutableListOf<MediaItem>()
        // Add showcased first (Recent)
        list.addAll(showcased)
        if (decoysEnabled) {
            list.addAll(decoyMediaList)
        }
        // Sort by date added, newest first
        list.sortedByDescending { it.dateAdded }
    }

    override val albums: Flow<List<Album>> = combine(
        _decoyAlbums,
        allMedia
    ) { decoyAlbumsList, media ->
        val albumsList = mutableListOf<Album>()
        
        // "Recent" album compiles all showcased and decoy items sorted by date
        if (media.isNotEmpty()) {
            albumsList.add(
                Album(
                    id = "recent",
                    name = "Recent",
                    coverPhotoUri = media.first().uri,
                    mediaCount = media.size
                )
            )
        }
        
        // Append decoy folders — thumbnail = most recently added photo in that folder
        for (album in decoyAlbumsList) {
            val albumMedia = media
                .filter { it.albumId == album.id }
                .sortedByDescending { it.dateAdded } // newest first, respects any device sort preference
            val count = albumMedia.size
            if (count > 0 || _areDecoysEnabled.value) {
                albumsList.add(
                    album.copy(
                        mediaCount = count,
                        // Use most recently added image as cover; empty string if folder is empty
                        coverPhotoUri = albumMedia.firstOrNull()?.uri ?: ""
                    )
                )
            }
        }
        albumsList
    }

    override fun getMediaForAlbum(albumId: String): Flow<List<MediaItem>> {
        return allMedia.map { media ->
            if (albumId == "recent") {
                media
            } else {
                media.filter { it.albumId == albumId }
            }
        }
    }

    override fun addShowcasedMedia(uris: List<String>) {
        val current = _showcasedMedia.value.toMutableList()
        val newItems = uris.map { queryUriMetadata(it) }
        current.addAll(newItems)
        _showcasedMedia.value = current
    }

    override fun removeShowcasedMedia(uris: List<String>) {
        val current = _showcasedMedia.value.toMutableList()
        current.removeAll { it.uri in uris }
        _showcasedMedia.value = current
    }

    override fun clearAllShowcasedMedia() {
        _showcasedMedia.value = emptyList()
    }

    // Dynamic Decoy CMS Database Modifiers
    override fun createDecoyAlbum(name: String) {
        val current = _decoyAlbums.value.toMutableList()
        val newAlbum = Album(
            id = "custom_album_" + System.currentTimeMillis(),
            name = name,
            coverPhotoUri = "", // dynamically derived from folder contents at render time
            mediaCount = 0
        )
        current.add(newAlbum)
        _decoyAlbums.value = current
        saveDecoyAlbumsToDisk(current)
    }

    override fun deleteDecoyAlbum(albumId: String) {
        val currentAlbums = _decoyAlbums.value.toMutableList()
        currentAlbums.removeAll { it.id == albumId }
        _decoyAlbums.value = currentAlbums
        saveDecoyAlbumsToDisk(currentAlbums)

        // Remove media items associated with deleted folder
        val currentMedia = _decoyMedia.value.toMutableList()
        currentMedia.removeAll { it.albumId == albumId }
        _decoyMedia.value = currentMedia
        saveDecoyMediaToDisk(currentMedia)
    }

    override fun addDecoyMedia(albumId: String, uris: List<String>) {
        // Take persistable read permissions
        for (uriStr in uris) {
            try {
                val uri = android.net.Uri.parse(uriStr)
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val currentMedia = _decoyMedia.value.toMutableList()
        val newItems = uris.mapIndexed { index, uriStr ->
            queryUriMetadata(uriStr).copy(
                id = "decoy_media_" + System.currentTimeMillis() + "_" + index,
                albumId = albumId,
                isDecoy = true
            )
        }
        currentMedia.addAll(newItems)
        _decoyMedia.value = currentMedia
        saveDecoyMediaToDisk(currentMedia)
    }

    override fun deleteDecoyMedia(mediaId: String) {
        val current = _decoyMedia.value.toMutableList()
        current.removeAll { it.id == mediaId }
        _decoyMedia.value = current
        saveDecoyMediaToDisk(current)
    }

    override fun setActiveAppIcon(iconName: String) {
        prefs.edit().putString("active_app_icon", iconName).apply()
        _activeAppIcon.value = iconName
        // No alias switching needed — only one launcher alias (MainActivityDefault) remains
    }

    override fun setCustomIconPath(path: String?) {
        prefs.edit().putString("custom_icon_path", path).apply()
        _customIconPath.value = path
    }

    override fun setUiOverrideEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("ui_override_enabled", enabled).apply()
        _uiOverrideEnabled.value = enabled
    }

    override fun setUiOverrideColumns(columns: Int) {
        prefs.edit().putInt("ui_override_columns", columns).apply()
        _uiOverrideColumns.value = columns
    }

    override fun setUiOverrideSpacingDp(spacing: Float) {
        prefs.edit().putFloat("ui_override_spacing_dp", spacing).apply()
        _uiOverrideSpacingDp.value = spacing
    }

    override fun setUiOverridePhotoCornersDp(corners: Int) {
        prefs.edit().putInt("ui_override_photo_corners_dp", corners).apply()
        _uiOverridePhotoCornersDp.value = corners
    }

    override fun setUiOverrideAlbumCornersDp(corners: Int) {
        prefs.edit().putInt("ui_override_album_corners_dp", corners).apply()
        _uiOverrideAlbumCornersDp.value = corners
    }

    override fun setUiOverrideThemeBrand(brand: String) {
        prefs.edit().putString("ui_override_theme_brand", brand).apply()
        _uiOverrideThemeBrand.value = brand
    }

    override fun setSearchPeople(names: String) {
        prefs.edit().putString("search_people_names", names).apply()
        _searchPeople.value = names
    }

    override fun setSearchPlaces(names: String) {
        prefs.edit().putString("search_places_names", names).apply()
        _searchPlaces.value = names
    }

    override fun setSearchThings(names: String) {
        prefs.edit().putString("search_things_names", names).apply()
        _searchThings.value = names
    }

    override fun setSearchThumbnail(name: String, uri: String?) {
        val current = _searchThumbnails.value.toMutableMap()
        if (uri == null) {
            current.remove(name)
        } else {
            current[name] = uri
        }
        _searchThumbnails.value = current
        saveSearchThumbnails(current)
    }

    private fun parseSearchThumbnails(json: String?): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (json.isNullOrEmpty()) return map
        try {
            val obj = JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getString(key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun saveSearchThumbnails(map: Map<String, String>) {
        val obj = JSONObject()
        for ((k, v) in map) {
            obj.put(k, v)
        }
        prefs.edit().putString("search_thumbnails_json", obj.toString()).apply()
    }

    override fun setRequireAuthOnExit(enabled: Boolean) {
        prefs.edit().putBoolean("require_auth_on_exit", enabled).apply()
        _requireAuthOnExit.value = enabled
    }

    override fun setAppLocked(locked: Boolean) {
        _isAppLocked.value = locked
    }


    private fun queryUriMetadata(uriString: String): MediaItem {
        val uri = android.net.Uri.parse(uriString)
        var name = "IMG_" + uriString.hashCode().absoluteValue
        var size = 0L
        var mimeType = "image/jpeg"
        var dateAdded = System.currentTimeMillis() / 1000
        var duration: Long? = null
        var width = 0
        var height = 0
        
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
            mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return MediaItem(
            id = "media_" + uriString.hashCode().absoluteValue,
            uri = uriString,
            name = name,
            dateAdded = dateAdded,
            mimeType = mimeType,
            size = size,
            duration = duration,
            width = width,
            height = height,
            isDecoy = false
        )
    }

    // Disk Serialization helpers
    private fun saveDecoyAlbumsToDisk(albumsList: List<Album>) {
        val array = JSONArray()
        for (a in albumsList) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("coverPhotoUri", a.coverPhotoUri)
            }
            array.put(obj)
        }
        prefs.edit().putString("decoy_albums_json", array.toString()).apply()
    }

    private fun loadDecoyAlbumsFromDisk(): List<Album> {
        val json = prefs.getString("decoy_albums_json", null) ?: return emptyList()
        val list = mutableListOf<Album>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Album(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        coverPhotoUri = obj.getString("coverPhotoUri"),
                        mediaCount = 0
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveDecoyMediaToDisk(mediaList: List<MediaItem>) {
        val array = JSONArray()
        for (m in mediaList) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("uri", m.uri)
                put("name", m.name)
                put("albumId", m.albumId ?: "")
                put("dateAdded", m.dateAdded)
                put("mimeType", m.mimeType ?: "image/jpeg")
                put("size", m.size)
                put("width", m.width)
                put("height", m.height)
                put("isDecoy", m.isDecoy)
            }
            array.put(obj)
        }
        prefs.edit().putString("decoy_media_json", array.toString()).apply()
    }

    private fun loadDecoyMediaFromDisk(): List<MediaItem> {
        val json = prefs.getString("decoy_media_json", null) ?: return emptyList()
        val list = mutableListOf<MediaItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MediaItem(
                        id = obj.getString("id"),
                        uri = obj.getString("uri"),
                        name = obj.getString("name"),
                        dateAdded = obj.getLong("dateAdded"),
                        mimeType = obj.optString("mimeType", "image/jpeg"),
                        size = obj.getLong("size"),
                        width = obj.getInt("width"),
                        height = obj.getInt("height"),
                        isDecoy = obj.optBoolean("isDecoy", true),
                        albumId = obj.optString("albumId", "").ifEmpty { null }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
