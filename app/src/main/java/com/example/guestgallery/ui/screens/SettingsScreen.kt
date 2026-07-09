package com.example.guestgallery.ui.screens

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RestartAlt
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
import com.example.guestgallery.R
import com.example.guestgallery.data.GalleryRepository
import com.example.guestgallery.theme.LocalAdaptiveUiConfig
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: GalleryRepository,
    onBack: () -> Unit,
    onAuthClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = LocalAdaptiveUiConfig.current
    val scrollState = rememberScrollState()
    
    val areDecoysEnabled by repository.areDecoysEnabled.collectAsState()
    val activeAppIcon by repository.activeAppIcon.collectAsState()
    val customIconPath by repository.customIconPath.collectAsState()
    val defaultPickerSource by repository.defaultPickerSource.collectAsState()
    
    val uiOverrideEnabled by repository.uiOverrideEnabled.collectAsState()
    val uiOverrideColumns by repository.uiOverrideColumns.collectAsState()
    val uiOverrideSpacingDp by repository.uiOverrideSpacingDp.collectAsState()
    val uiOverridePhotoCornersDp by repository.uiOverridePhotoCornersDp.collectAsState()
    val uiOverrideAlbumCornersDp by repository.uiOverrideAlbumCornersDp.collectAsState()
    val uiOverrideThemeBrand by repository.uiOverrideThemeBrand.collectAsState()
    
    var showPinResetDialog by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }
    var pinConfirmValue by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }

    var showCleanSessionDialog by remember { mutableStateOf(false) }

    val dpm = remember(context) { context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember(context) { ComponentName(context, com.example.guestgallery.MyDeviceAdminReceiver::class.java) }
    var isDeviceAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }

    LaunchedEffect(Unit) {
        isDeviceAdminActive = dpm.isAdminActive(adminComponent)
    }

    // Picker launcher for custom launcher icon
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            val file = java.io.File(context.filesDir, "custom_icon.png")
                            java.io.FileOutputStream(file).use { out ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                            }
                            repository.setCustomIconPath(file.absolutePath)
                            repository.setActiveAppIcon("custom")
                            
                            // Pin shortcut on home screen (Android 8.0+)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val shortcutManager = context.getSystemService(android.content.pm.ShortcutManager::class.java)
                                if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                                    val launchIntent = Intent(context, com.example.guestgallery.MainActivity::class.java).apply {
                                        action = Intent.ACTION_MAIN
                                    }
                                    val icon = android.graphics.drawable.Icon.createWithBitmap(bitmap)
                                    val shortcutInfo = android.content.pm.ShortcutInfo.Builder(context, "custom_gallery_shortcut")
                                        .setIcon(icon)
                                        .setShortLabel("Gallery")
                                        .setIntent(launchIntent)
                                        .build()
                                    shortcutManager.requestPinShortcut(shortcutInfo, null)
                                    android.widget.Toast.makeText(context, "Shortcut pinned to Home screen!", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Failed to set custom icon", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val triggerIconPicker = {
        repository.setLaunchingPicker(true)
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        iconPickerLauncher.launch(intent)
    }

    // Search Mock Customizations flow collection
    val searchPeople by repository.searchPeople.collectAsState()
    val searchPlaces by repository.searchPlaces.collectAsState()
    val searchThings by repository.searchThings.collectAsState()
    val searchThumbnails by repository.searchThumbnails.collectAsState()

    var activeSearchItemNameForThumbnail by remember { mutableStateOf<String?>(null) }
    val searchThumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            activeSearchItemNameForThumbnail?.let { name ->
                if (uri != null) {
                    repository.setSearchThumbnail(name, uri.toString())
                }
            }
        }
        activeSearchItemNameForThumbnail = null
    }

    val triggerSearchThumbnailPicker = { itemName: String ->
        activeSearchItemNameForThumbnail = itemName
        repository.setLaunchingPicker(true)
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        searchThumbnailPickerLauncher.launch(intent)
    }

    // PIN Reset Dialog
    if (showPinResetDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPinResetDialog = false 
                pinValue = ""
                pinConfirmValue = ""
                pinError = ""
            },
            title = { Text("Reset Owner PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter new 4-digit PIN:")
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinValue = it },
                        singleLine = true,
                        shape = RoundedCornerShape(config.cornerRadiusDp.dp)
                    )
                    Text("Confirm new PIN:")
                    OutlinedTextField(
                        value = pinConfirmValue,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinConfirmValue = it },
                        singleLine = true,
                        shape = RoundedCornerShape(config.cornerRadiusDp.dp)
                    )
                    if (pinError.isNotEmpty()) {
                        Text(text = pinError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pinValue.length == 4 && pinValue == pinConfirmValue) {
                        repository.savePin(pinValue)
                        showPinResetDialog = false
                        pinValue = ""
                        pinConfirmValue = ""
                        pinError = ""
                    } else if (pinValue.length != 4) {
                        pinError = "PIN must be exactly 4 digits"
                    } else {
                        pinError = "PIN codes do not match"
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinResetDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(config.cornerRadiusDp.dp)
        )
    }

    // Clean Session Dialog
    if (showCleanSessionDialog) {
        AlertDialog(
            onDismissRequest = { showCleanSessionDialog = false },
            title = { Text("Session Cleanup") },
            text = { Text("Remove showcased photos from Guest View?") },
            confirmButton = {
                TextButton(onClick = {
                    repository.clearAllShowcasedMedia()
                    repository.setGuestMode(false)
                    showCleanSessionDialog = false
                    onBack()
                }) {
                    Text("YES")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCleanSessionDialog = false 
                    repository.setGuestMode(false)
                    onBack()
                }) {
                    Text("NO")
                }
            },
            shape = RoundedCornerShape(config.cornerRadiusDp.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Owner Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go back")
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // General Settings Card
            Card(
                shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "General Preferences",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Decoy Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                            Column {
                                Text("Decoy Albums", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Display folders when active", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                        Switch(
                            checked = areDecoysEnabled,
                            onCheckedChange = { repository.setDecoysEnabled(it) }
                        )
                    }

                    HorizontalDivider()

                    // Authentication Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                            Column {
                                Text("Default Photo Picker", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("App to launch when pressing the + button", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                        TextButton(
                            onClick = {
                                if (defaultPickerSource == "native") {
                                    repository.setDefaultPickerSource("google")
                                } else {
                                    repository.setDefaultPickerSource("native")
                                }
                            }
                        ) {
                            Text(
                                text = if (defaultPickerSource == "google") "Google Photos" else "System Gallery",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                            Column {
                                Text("Physically Lock Screen", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Turn off and lock screen physically when a guest tries to exit or minimize the app (requires Device Admin permission)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                        Switch(
                            checked = isDeviceAdminActive,
                            onCheckedChange = { active ->
                                if (active) {
                                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required to physically lock the phone screen when a guest tries to exit the app.")
                                    }
                                    context.startActivity(intent)
                                } else {
                                    try {
                                        dpm.removeActiveAdmin(adminComponent)
                                        isDeviceAdminActive = false
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        )
                    }

                    HorizontalDivider()

                    // Cleanup Session Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCleanSessionDialog = true }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.error)
                        Column {
                            Text("Cleanup Guest Session", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.error)
                            Text("Clear showcases and return to normal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // Custom UI Overrides Card
            Card(
                shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "UI Style Override",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tweak columns, margins, and corners via slider",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = uiOverrideEnabled,
                            onCheckedChange = { repository.setUiOverrideEnabled(it) }
                        )
                    }

                    if (uiOverrideEnabled) {
                        HorizontalDivider()

                        // Grid Columns Slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Grid Columns", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${uiOverrideColumns} Columns", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = uiOverrideColumns.toFloat(),
                                onValueChange = { repository.setUiOverrideColumns(it.roundToInt()) },
                                valueRange = 2f..6f,
                                steps = 3
                            )
                        }

                        // Grid Spacing Slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Timeline Photo Spacing", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(String.format(Locale.US, "%.1f dp", uiOverrideSpacingDp), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = uiOverrideSpacingDp,
                                onValueChange = { repository.setUiOverrideSpacingDp(it) },
                                valueRange = 0f..12f
                            )
                        }

                        // Photo Corners Slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Timeline Image Corner Radius", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(if (uiOverridePhotoCornersDp == 0) "Square" else "${uiOverridePhotoCornersDp} dp", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = uiOverridePhotoCornersDp.toFloat(),
                                onValueChange = { repository.setUiOverridePhotoCornersDp(it.roundToInt()) },
                                valueRange = 0f..24f
                            )
                        }

                        // Album Corners Slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Album Cover Card Corners", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${uiOverrideAlbumCornersDp} dp", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = uiOverrideAlbumCornersDp.toFloat(),
                                onValueChange = { repository.setUiOverrideAlbumCornersDp(it.roundToInt()) },
                                valueRange = 0f..32f
                            )
                        }


                    }
                }
            }

            // App Launcher Icon Card (Now with Preview & Dynamic Custom Picker)
            Card(
                shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "App Launcher Icon",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Icon Preview
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (activeAppIcon == "custom" && customIconPath != null) {
                                AsyncImage(
                                    model = customIconPath,
                                    contentDescription = "Custom Icon",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // Always show the purple default icon
                                AsyncImage(
                                    model = R.drawable.ic_launcher_default,
                                    contentDescription = "Active Icon",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Launcher Icon",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (activeAppIcon == "custom") "Custom Image" else "Guest Gallery (Default)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Upload Custom Icon Button
                    Button(
                        onClick = triggerIconPicker,
                        shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("Upload Custom Icon from Gallery", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            // Add Decoy Mode trigger Card (CMS entry)
            Card(
                shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Add Decoy Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Enter Decoy Edit Mode using fingerprint security. In this mode, you can create/delete folders and load photos to build the customized decoy database that guests will see.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { onAuthClick("decoy_edit") },
                        shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Enter Add Decoy Mode", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Custom Search Mockups Card
            Card(
                shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Customize Search Mockups",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = searchPeople,
                        onValueChange = { repository.setSearchPeople(it) },
                        label = { Text("People & Pets (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(config.cornerRadiusDp.dp)
                    )

                    OutlinedTextField(
                        value = searchPlaces,
                        onValueChange = { repository.setSearchPlaces(it) },
                        label = { Text("Places (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(config.cornerRadiusDp.dp)
                    )

                    OutlinedTextField(
                        value = searchThings,
                        onValueChange = { repository.setSearchThings(it) },
                        label = { Text("Things (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(config.cornerRadiusDp.dp)
                    )

                    HorizontalDivider()

                    Text(
                        text = "Search Page Thumbnails",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    val allCustomizableItems = remember(searchPeople, searchPlaces) {
                        val peopleList = searchPeople.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val placesList = searchPlaces.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        (peopleList + placesList).distinct()
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allCustomizableItems.forEach { name ->
                            val currentUri = searchThumbnails[name]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mini Thumbnail Preview
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentUri != null) {
                                        AsyncImage(
                                            model = currentUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                TextButton(
                                    onClick = { triggerSearchThumbnailPicker(name) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Set Image", fontSize = 12.sp)
                                }

                                if (currentUri != null) {
                                    IconButton(
                                        onClick = { repository.setSearchThumbnail(name, null) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear image",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Adaptive UI profile display
            Card(
                shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = "Adaptive UI Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    ProfileItem(label = "Android OS SDK", value = "Android ${config.androidVersion} (API level)")
                    ProfileItem(label = "Device Manufacturer", value = config.manufacturer.uppercase())
                    ProfileItem(label = "Corner Radius", value = "${config.cornerRadiusDp} dp")
                    ProfileItem(label = "Navigation Mode", value = if (config.isGestureNavigation) "Gestural (sleek)" else "3-Button (classic)")
                    ProfileItem(label = "System Dark Theme", value = if (config.isSystemDark) "Enabled" else "Disabled")
                    ProfileItem(label = "Display Density", value = String.format(Locale.US, "%.2f x", config.density))
                    ProfileItem(label = "Font Scaling", value = String.format(Locale.US, "%.2f x", config.fontScale))
                    ProfileItem(label = "Screen Resolution", value = "${config.screenWidthDp} x ${config.screenHeightDp} dp")
                    ProfileItem(label = "Animation Scale", value = String.format(Locale.US, "%.2f x", config.animationScale))
                }
            }
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
