package com.example.guestgallery.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.guestgallery.data.GalleryRepository
import com.example.guestgallery.data.MediaItem
import com.example.guestgallery.theme.LocalAdaptiveUiConfig
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getShareableUri(context: Context, uriString: String): Uri {
    if (uriString.startsWith("file:///android_asset/")) {
        try {
            val assetPath = uriString.substringAfter("file:///android_asset/")
            val cacheFile = File(context.cacheDir, assetPath.substringAfterLast("/"))
            if (!cacheFile.exists()) {
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            return FileProvider.getUriForFile(
                context,
                "com.example.guestgallery.fileprovider",
                cacheFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return Uri.parse(uriString)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    albumId: String,
    selectedPhotoId: String,
    repository: GalleryRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = LocalAdaptiveUiConfig.current
    val mediaItems by repository.getMediaForAlbum(albumId).collectAsState(initial = emptyList())

    val startIndex = remember(mediaItems, selectedPhotoId) {
        val idx = mediaItems.indexOfFirst { it.id == selectedPhotoId }
        if (idx != -1) idx else 0
    }

    if (mediaItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { mediaItems.size }
    )
    
    var showInfoPanel by remember { mutableStateOf(false) }
    var chromeVisible by remember { mutableStateOf(true) }
    val favorites = remember { mutableStateMapOf<String, Boolean>() }
    var showDeleteWarningDialog by remember { mutableStateOf(false) }

    val triggerShare = { photo: MediaItem ->
        val shareableUri = getShareableUri(context, photo.uri)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = photo.mimeType ?: "image/*"
            putExtra(Intent.EXTRA_STREAM, shareableUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Share Photo"))
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Google Photos style delete warning modal
    if (showDeleteWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteWarningDialog = false },
            title = { Text("Delete from Photos?") },
            text = { Text("This item is stored in local storage. Removing it will not delete cloud copies or photos backed up to other devices.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteWarningDialog = false
                    val currentItem = mediaItems.getOrNull(pagerState.currentPage)
                    currentItem?.let { item ->
                        if (item.isDecoy) {
                            repository.deleteDecoyMedia(item.id)
                        } else {
                            repository.removeShowcasedMedia(listOf(item.uri))
                        }
                        Toast.makeText(context, "Moved to Trash", Toast.LENGTH_SHORT).show()
                        onBack() // Navigate back to grid so they see it is gone
                    }
                }) {
                    Text("Move to Trash", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteWarningDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(config.cornerRadiusDp.dp)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp
        ) { page ->
            val item = mediaItems.getOrNull(page)
            if (item != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { chromeVisible = !chromeVisible },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isVideo) {
                        VideoPlayerItem(uri = item.uri)
                    } else {
                        ZoomableImageItem(uri = item.uri, contentDescription = item.name)
                    }
                }
            }
        }

        // Top Header (Google Photos style layout)
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                
                val currentItem = mediaItems.getOrNull(pagerState.currentPage)
                if (currentItem != null) {
                    val isFav = favorites[currentItem.id] ?: false
                    
                    // Chromecast Button
                    IconButton(onClick = { Toast.makeText(context, "Looking for Cast devices...", Toast.LENGTH_SHORT).show() }) {
                        Icon(imageVector = Icons.Default.Tv, contentDescription = "Cast", tint = Color.White)
                    }
                    // Star (Favorite) Button
                    IconButton(onClick = { favorites[currentItem.id] = !isFav }) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) Color(0xFFFBC02D) else Color.White
                        )
                    }
                    // Delete Button
                    IconButton(onClick = { showDeleteWarningDialog = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                    // Info Button
                    IconButton(onClick = { showInfoPanel = true }) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                    }
                }
            }
        }

        // Bottom Navigation Actions (Google Photos style Share / Edit / Lens / Delete)
        AnimatedVisibility(
            visible = chromeVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .navigationBarsPadding()
                    .height(84.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentItem = mediaItems.getOrNull(pagerState.currentPage)
                if (currentItem != null) {
                    BottomActionItem(icon = Icons.Default.Share, label = "Share") {
                        triggerShare(currentItem)
                    }
                    BottomActionItem(icon = Icons.Default.Edit, label = "Edit") {
                        Toast.makeText(context, "Photos editor requires sign-in.", Toast.LENGTH_SHORT).show()
                    }
                    BottomActionItem(icon = Icons.Default.CenterFocusWeak, label = "Lens") {
                        Toast.makeText(context, "Lens requires internet connection.", Toast.LENGTH_SHORT).show()
                    }
                    BottomActionItem(icon = Icons.Default.Delete, label = "Delete") {
                        showDeleteWarningDialog = true
                    }
                }
            }
        }

        // Info Details Panel
        val currentItem = mediaItems.getOrNull(pagerState.currentPage)
        AnimatedVisibility(
            visible = showInfoPanel && currentItem != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            currentItem?.let { item ->
                InfoPanel(item = item, onClose = { showInfoPanel = false })
            }
        }
    }
}

@Composable
fun BottomActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

suspend fun PointerInputScope.detectZoomPanGestures(
    isZoomed: () -> Boolean,
    onGesture: (pan: androidx.compose.ui.geometry.Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = androidx.compose.ui.geometry.Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                
                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = Math.abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()
                    
                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }
                
                if (pastTouchSlop) {
                    val activeZoom = isZoomed() || zoomChange != 1f
                    if (activeZoom) {
                        onGesture(panChange, zoomChange)
                        event.changes.forEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}

@Composable
fun ZoomableImageItem(
    uri: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { centroid ->
                            val targetScale = if (scale.value > 1.5f) 1f else 3f
                            coroutineScope.launch {
                                if (targetScale == 1f) {
                                    launch { scale.animateTo(1f) }
                                    launch { offsetX.animateTo(0f) }
                                    launch { offsetY.animateTo(0f) }
                                } else {
                                    launch { scale.animateTo(3f) }
                                    val centerX = widthPx / 2f
                                    val centerY = heightPx / 2f
                                    val maxOffX = (widthPx * 3f - widthPx) / 2f
                                    val maxOffY = (heightPx * 3f - heightPx) / 2f
                                    val targetX = ((centerX - centroid.x) * 2f).coerceIn(-maxOffX, maxOffX)
                                    val targetY = ((centerY - centroid.y) * 2f).coerceIn(-maxOffY, maxOffY)
                                    launch { offsetX.animateTo(targetX) }
                                    launch { offsetY.animateTo(targetY) }
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectZoomPanGestures(
                        isZoomed = { scale.value > 1.05f },
                        onGesture = { pan, zoom ->
                            val currentScale = scale.value
                            val newScale = (currentScale * zoom).coerceIn(1f, 5f)
                            
                            coroutineScope.launch {
                                scale.snapTo(newScale)
                                
                                val maxX = (widthPx * newScale - widthPx) / 2f
                                val maxY = (heightPx * newScale - heightPx) / 2f
                                
                                val targetX = (offsetX.value + pan.x).coerceIn(-maxX, maxX)
                                val targetY = (offsetY.value + pan.y).coerceIn(-maxY, maxY)
                                
                                offsetX.snapTo(targetX)
                                offsetY.snapTo(targetY)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = uri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value,
                        translationX = offsetX.value,
                        translationY = offsetY.value
                    )
            )
        }
    }
}

@Composable
fun VideoPlayerItem(
    uri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(context, uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(Uri.parse(uri)))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
    }
}

@Composable
fun InfoPanel(
    item: MediaItem,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalAdaptiveUiConfig.current
    val dateString = remember(item.dateAdded) {
        val sdf = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        sdf.format(Date(item.dateAdded * 1000))
    }
    
    val sizeString = remember(item.size) {
        if (item.size <= 0) "Unknown size"
        else if (item.size < 1024 * 1024) String.format(Locale.US, "%.1f KB", item.size / 1024f)
        else String.format(Locale.US, "%.2f MB", item.size / (1024f * 1024f))
    }

    Card(
        shape = RoundedCornerShape(topStart = config.cornerRadiusDp.dp, topEnd = config.cornerRadiusDp.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Details",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            DetailRow(label = "Title", value = item.name)
            DetailRow(label = "Date", value = dateString)
            DetailRow(label = "Size", value = sizeString)
            if (item.width > 0 && item.height > 0) {
                DetailRow(label = "Resolution", value = "${item.width} x ${item.height}")
            }
            DetailRow(label = "Mime Type", value = item.mimeType)
            DetailRow(
                label = "Location", 
                value = if (item.isDecoy) "Built-in Asset" else "System Reference"
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClose,
                shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            modifier = Modifier.widthIn(max = 240.dp)
        )
    }
}
