package com.example.guestgallery.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.guestgallery.data.Album
import com.example.guestgallery.data.GalleryRepository
import com.example.guestgallery.data.MediaItem
import com.example.guestgallery.theme.LocalAdaptiveUiConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumGridScreen(
    repository: GalleryRepository,
    onAlbumClick: (Album) -> Unit,
    onPhotoClick: (String, String) -> Unit, // albumId, photoId
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = LocalAdaptiveUiConfig.current
    val isGuestMode by repository.isGuestMode.collectAsState()
    val isDecoyEditMode by repository.isDecoyEditMode.collectAsState()
    
    val albumsList by repository.albums.collectAsState(initial = emptyList())
    val allMediaList by repository.allMedia.collectAsState(initial = emptyList())
    
    var selectedTab by remember { mutableStateOf(0) } // 0: Photos, 1: Search, 2: Sharing, 3: Library
    var showAccountDialog by remember { mutableStateOf(false) }

    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    var albumToDelete by remember { mutableStateOf<Album?>(null) }

    // Multi-select state variables for main timeline
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<String>() }
    var showTimelineDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Fast local gallery picker launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = mutableListOf<String>()
            val data = result.data
            if (data != null) {
                val clipData = data.clipData
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        uris.add(clipData.getItemAt(i).uri.toString())
                    }
                } else {
                    data.data?.let { uris.add(it.toString()) }
                }
            }
            if (uris.isNotEmpty()) {
                repository.addShowcasedMedia(uris)
                repository.setGuestMode(true) // Switch to guest mode instantly
            }
        }
    }

    val triggerShowcasePicker = {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickerLauncher.launch(intent)
    }

    if (isMultiSelectMode) {
        BackHandler {
            isMultiSelectMode = false
            selectedItems.clear()
        }
    } else if (isGuestMode) {
        BackHandler {
            onExitClick()
        }
    }

    // Dialog: Delete Selected Items Confirmation for Timeline
    if (showTimelineDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showTimelineDeleteConfirmDialog = false },
            title = { Text("Delete selected items") },
            text = { Text("Are you sure you want to delete these ${selectedItems.size} items? (They will be hidden from the guest timeline/folders, leaving actual files intact.)") },
            confirmButton = {
                TextButton(onClick = {
                    for (id in selectedItems) {
                        val item = allMediaList.find { it.id == id }
                        if (item != null) {
                            if (item.isDecoy) {
                                repository.deleteDecoyMedia(item.id)
                            } else {
                                repository.removeShowcasedMedia(listOf(item.uri))
                            }
                        }
                    }
                    selectedItems.clear()
                    isMultiSelectMode = false
                    showTimelineDeleteConfirmDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimelineDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(config.cornerRadiusDp.dp)
        )
    }

    // Dialog: Create Album
    if (showCreateAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showCreateAlbumDialog = false; newAlbumName = "" },
            title = { Text("New Album") },
            text = {
                OutlinedTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    label = { Text("Album Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(config.cornerRadiusDp.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newAlbumName.trim().isNotEmpty()) {
                        repository.createDecoyAlbum(newAlbumName.trim())
                        showCreateAlbumDialog = false
                        newAlbumName = ""
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAlbumDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(config.cornerRadiusDp.dp)
        )
    }

    // Dialog: Delete Album
    albumToDelete?.let { album ->
        AlertDialog(
            onDismissRequest = { albumToDelete = null },
            title = { Text("Delete Album") },
            text = { Text("Are you sure you want to delete '${album.name}'? This will permanently delete this decoy folder and all its contents.") },
            confirmButton = {
                TextButton(onClick = {
                    repository.deleteDecoyAlbum(album.id)
                    albumToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { albumToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(config.cornerRadiusDp.dp)
        )
    }

    // Google Account Settings / PIN Dialog Trigger
    if (showAccountDialog) {
        GoogleAccountDialog(
            isGuestMode = isGuestMode,
            onDismiss = { showAccountDialog = false },
            onSettingsClick = {
                showAccountDialog = false
                onSettingsClick()
            },
            onExitClick = {
                showAccountDialog = false
                onExitClick()
            },
            onEnterClick = {
                showAccountDialog = false
                repository.setGuestMode(true)
            }
        )
    }

    Scaffold(
        topBar = {
            if (isMultiSelectMode) {
                TopAppBar(
                    title = { Text("${selectedItems.size} selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            isMultiSelectMode = false
                            selectedItems.clear()
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTimelineDeleteConfirmDialog = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            } else {
                PhotosTopBar(
                    onAddClick = triggerShowcasePicker,
                    onProfileClick = { showAccountDialog = true }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Photo, contentDescription = "Photos") },
                    label = { Text("Photos") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.MailOutline, contentDescription = "Sharing") },
                    label = { Text("Sharing") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Collections, contentDescription = "Library") },
                    label = { Text("Library") }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Decoy Edit Mode top bar banner
            if (isDecoyEditMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Decoy Edit Mode (CMS active)",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        TextButton(
                            onClick = { repository.setDecoyEditMode(false) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("DONE", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> PhotosTab(
                        allMediaList = allMediaList,
                        isMultiSelectMode = isMultiSelectMode,
                        selectedItems = selectedItems,
                        onItemClick = { item ->
                            if (isMultiSelectMode) {
                                if (selectedItems.contains(item.id)) {
                                    selectedItems.remove(item.id)
                                    if (selectedItems.isEmpty()) {
                                        isMultiSelectMode = false
                                    }
                                } else {
                                    selectedItems.add(item.id)
                                }
                            } else {
                                onPhotoClick("recent", item.id)
                            }
                        },
                        onItemLongClick = { item ->
                            if (!isMultiSelectMode) {
                                isMultiSelectMode = true
                                selectedItems.add(item.id)
                            }
                        },
                        onAddClick = triggerShowcasePicker,
                        isGuestMode = isGuestMode,
                        isDecoyEditMode = isDecoyEditMode
                    )
                    1 -> SearchTab(repository = repository)
                    2 -> SharingTab()
                    3 -> LibraryTab(
                        albumsList = albumsList,
                        isDecoyEditMode = isDecoyEditMode,
                        onCreateAlbumClick = { showCreateAlbumDialog = true },
                        onDeleteAlbumClick = { album -> albumToDelete = album },
                        onAlbumClick = onAlbumClick
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Photos Header
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosTopBar(
    onAddClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Photos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Photos"
                )
            }
        },
        actions = {
            // Profile switcher icon (stealth entry point)
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

// ----------------------------------------------------
// Photos Tab (Google-style date-grouped timeline)
// ----------------------------------------------------
@Composable
fun PhotosTab(
    allMediaList: List<MediaItem>,
    isMultiSelectMode: Boolean,
    selectedItems: List<String>,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    onAddClick: () -> Unit,
    isGuestMode: Boolean,
    isDecoyEditMode: Boolean
) {
    val config = LocalAdaptiveUiConfig.current
    val groupedMedia = remember(allMediaList) {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val yesterdayStr = sdf.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
        
        allMediaList.groupBy { item ->
            val dateStr = sdf.format(Date(item.dateAdded * 1000))
            when (dateStr) {
                todayStr -> "Today"
                yesterdayStr -> "Yesterday"
                else -> dateStr
            }
        }.toList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (allMediaList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No photos here yet",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    if (!isGuestMode && !isDecoyEditMode) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onAddClick) {
                            Text("Select Photos to Showcase")
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(config.gridColumns),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(config.gridSpacingDp.dp),
                verticalArrangement = Arrangement.spacedBy(config.gridSpacingDp.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                groupedMedia.forEach { (date, itemsForDate) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = date,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(itemsForDate) { item ->
                        val isSelected = selectedItems.contains(item.id)
                        PhotoGridItem(
                            item = item,
                            onClick = { onItemClick(item) },
                            isSelected = isSelected,
                            isSelectionModeActive = isMultiSelectMode,
                            onLongClick = { onItemLongClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGridItem(
    item: MediaItem,
    onClick: () -> Unit,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalAdaptiveUiConfig.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(config.photoCornerRadiusDp.dp))
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            )
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Render checkbox indicator if in select mode
        if (isSelectionModeActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) Color.Black.copy(alpha = 0.3f) else Color.Transparent)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF1A73E8) else Color.White.copy(alpha = 0.8f))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Video",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    
                    val durationString = item.duration?.let { formatDuration(it) } ?: ""
                    if (durationString.isNotEmpty()) {
                        Text(
                            text = durationString,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

@Composable
fun SearchTab(repository: GalleryRepository) {
    val searchPeople by repository.searchPeople.collectAsState()
    val searchPlaces by repository.searchPlaces.collectAsState()
    val searchThings by repository.searchThings.collectAsState()
    val searchThumbnails by repository.searchThumbnails.collectAsState()

    val peopleList = remember(searchPeople) {
        searchPeople.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    val placesList = remember(searchPlaces) {
        searchPlaces.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    val thingsList = remember(searchThings) {
        searchThings.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    val scrollState = rememberLazyGridState()
    LazyVerticalGrid(
        state = scrollState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            // Search Input Mock
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search your photos") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(24.dp)
            )
        }

        // People & Pets Section
        if (peopleList.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "People & Pets",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(peopleList) { name ->
                            val customThumbnail = searchThumbnails[name]
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (customThumbnail != null) {
                                        AsyncImage(
                                            model = customThumbnail,
                                            contentDescription = name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        // Pick a color deterministically based on name hash code
                                        val colors = listOf(
                                            Color(0xFF4285F4),
                                            Color(0xFF34A853),
                                            Color(0xFFEA4335),
                                            Color(0xFFFBBC05),
                                            Color(0xFF6750A4),
                                            Color(0xFF007A87)
                                        )
                                        val colorIndex = Math.abs(name.hashCode()) % colors.size
                                        val color = colors[colorIndex]
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(color.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name.take(1).uppercase(),
                                                color = color,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 64.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Places Section
        if (placesList.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "Places",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(placesList) { place ->
                            val customThumbnail = searchThumbnails[place]
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(80.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (customThumbnail != null) {
                                        AsyncImage(
                                            model = customThumbnail,
                                            contentDescription = place,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        // Dark gradient overlay for text readability
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.3f))
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = place,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (customThumbnail != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Things Section
        if (thingsList.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Things",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(thingsList) { thing ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = thing,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Sharing Tab (Mock Shared Album Link Lists)
// ----------------------------------------------------
@Composable
fun SharingTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Share photos & albums",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create albums to share with friends, family or your partner easily.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {}) {
            Text("Create Shared Album")
        }
    }
}

// ----------------------------------------------------
// Library Tab (Categories & Custom Folders)
// ----------------------------------------------------
@Composable
fun LibraryTab(
    albumsList: List<Album>,
    isDecoyEditMode: Boolean,
    onCreateAlbumClick: () -> Unit,
    onDeleteAlbumClick: (Album) -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    val config = LocalAdaptiveUiConfig.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Google Library top shortcut buttons (Favorites, Utilities, Archive, Trash)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryTile(modifier = Modifier.weight(1f), icon = Icons.Default.StarBorder, label = "Favorites")
                LibraryTile(modifier = Modifier.weight(1f), icon = Icons.Default.Settings, label = "Utilities")
                LibraryTile(modifier = Modifier.weight(1f), icon = Icons.Default.Archive, label = "Archive")
                LibraryTile(modifier = Modifier.weight(1f), icon = Icons.Default.Delete, label = "Trash")
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Photos on device",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Create Album card if in Decoy Edit Mode (CMS)
        if (isDecoyEditMode) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCreateAlbumClick() }
                ) {
                    Card(
                        shape = RoundedCornerShape(config.albumCornerRadiusDp.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Album",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create Album",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Add decoy folder",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }

        if (albumsList.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No albums created yet",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(albumsList) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album) },
                    showDelete = isDecoyEditMode,
                    onDeleteClick = { onDeleteAlbumClick(album) }
                )
            }
        }
    }
}

@Composable
fun LibraryTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = modifier.height(60.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    showDelete: Boolean = false,
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val config = LocalAdaptiveUiConfig.current
    val cardRadius = config.albumCornerRadiusDp.dp
    val isEmpty = album.coverPhotoUri.isBlank()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Card(
            shape = RoundedCornerShape(cardRadius),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = "Empty album",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else {
                    AsyncImage(
                        model = album.coverPhotoUri,
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (showDelete && album.id != "recent") {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Album",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = album.name,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (album.mediaCount == 0) "Empty" else album.mediaCount.toString(),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

// ----------------------------------------------------
// Google Account Profile Picker Switcher dialog
// ----------------------------------------------------
@Composable
fun GoogleAccountDialog(
    isGuestMode: Boolean,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    onEnterClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header (Close + Google Logo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Google",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.weight(1.3f))
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Profile Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "User",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "user@example.com",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Backup Status Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Backup complete",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "15 GB of 15 GB storage used",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // List Items
                AccountMenuItem(
                    icon = Icons.Default.Upload,
                    title = "Backup settings",
                    onClick = {}
                )
                AccountMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Photos settings",
                    onClick = onSettingsClick
                )

                if (isGuestMode) {
                    AccountMenuItem(
                        icon = Icons.Default.Delete,
                        title = "Exit Guest Mode",
                        onClick = onExitClick
                    )
                } else {
                    AccountMenuItem(
                        icon = Icons.Default.Check,
                        title = "Enter Guest Mode",
                        onClick = onEnterClick
                    )
                }
            }
        }
    }
}

@Composable
fun AccountMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
