package com.example.guestgallery.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.guestgallery.data.GalleryRepository
import com.example.guestgallery.data.MediaItem
import com.example.guestgallery.theme.LocalAdaptiveUiConfig
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    albumName: String,
    repository: GalleryRepository,
    onPhotoClick: (MediaItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = LocalAdaptiveUiConfig.current
    val isDecoyEditMode by repository.isDecoyEditMode.collectAsState()
    val mediaItems by repository.getMediaForAlbum(albumId).collectAsState(initial = emptyList())

    // Selection state variables
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<String>() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Intercept back presses in multi-select mode
    if (isMultiSelectMode) {
        BackHandler {
            isMultiSelectMode = false
            selectedItems.clear()
        }
    }

    // Fast local gallery picker launcher
    val decoyPickerLauncher = rememberLauncherForActivityResult(
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
                repository.addDecoyMedia(albumId, uris)
            }
        }
    }

    val triggerDecoyPicker = {
        repository.setLaunchingPicker(true)
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        decoyPickerLauncher.launch(intent)
    }

    // Dialog: Delete Selected Items Confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete selected items") },
            text = { Text("Are you sure you want to delete these ${selectedItems.size} items from this decoy folder?") },
            confirmButton = {
                TextButton(onClick = {
                    for (id in selectedItems) {
                        repository.deleteDecoyMedia(id)
                    }
                    selectedItems.clear()
                    isMultiSelectMode = false
                    showDeleteConfirmDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(config.cornerRadiusDp.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = if (isMultiSelectMode) "${selectedItems.size} selected" else albumName, 
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (!isMultiSelectMode) {
                            Text(
                                text = "${mediaItems.size} items",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = {
                            isMultiSelectMode = false
                            selectedItems.clear()
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go back")
                        }
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                        }
                    } else if (isDecoyEditMode) {
                        TextButton(onClick = triggerDecoyPicker) {
                            Text("ADD PHOTOS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
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
                    Text(
                        text = if (isMultiSelectMode) "Select items to delete them" else "Decoy Edit Mode - Long press to select and delete photos",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (mediaItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "This album is empty",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(config.gridColumns),
                        contentPadding = PaddingValues(config.gridSpacingDp.dp),
                        horizontalArrangement = Arrangement.spacedBy(config.gridSpacingDp.dp),
                        verticalArrangement = Arrangement.spacedBy(config.gridSpacingDp.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(mediaItems) { item ->
                            val isSelected = selectedItems.contains(item.id)
                            PhotoGridItem(
                                item = item,
                                onClick = {
                                    if (isMultiSelectMode) {
                                        if (isSelected) {
                                            selectedItems.remove(item.id)
                                            if (selectedItems.isEmpty()) {
                                                isMultiSelectMode = false
                                            }
                                        } else {
                                            selectedItems.add(item.id)
                                        }
                                    } else {
                                        onPhotoClick(item)
                                    }
                                },
                                isDecoyEditMode = isDecoyEditMode,
                                isSelected = isSelected,
                                isSelectionModeActive = isMultiSelectMode,
                                onLongClick = {
                                    if (!isMultiSelectMode) {
                                        isMultiSelectMode = true
                                        selectedItems.add(item.id)
                                    }
                                }
                            )
                    }
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
    isDecoyEditMode: Boolean,
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
                onLongClick = {
                    if (isDecoyEditMode) {
                        onLongClick()
                    }
                },
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

