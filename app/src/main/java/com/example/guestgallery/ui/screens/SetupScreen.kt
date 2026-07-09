package com.example.guestgallery.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.guestgallery.data.GalleryRepository
import com.example.guestgallery.theme.LocalAdaptiveUiConfig

@Composable
fun SetupScreen(
    repository: GalleryRepository,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalAdaptiveUiConfig.current
    var step by remember { mutableStateOf(1) } // 1: Welcome, 2: PIN Setup, 3: PIN Confirm, 4: Decoy Config
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        step = 2
    }

    var pinFirst by remember { mutableStateOf("") }
    var pinSecond by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }
    
    var decoyPhotosEnabled by remember { mutableStateOf(true) }

    val transitionDuration = (300 * config.animationScale).toInt()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight().widthIn(max = 400.dp)
        ) {
            // Top App Bar / Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    IconButton(onClick = {
                        if (step == 3) {
                            step = 2
                            pinSecond = ""
                            pinError = ""
                        } else {
                            step--
                        }
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                
                Text(
                    text = "Step $step of 4",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Step Content
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    1 -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Welcome to\nGuest View",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                lineHeight = 44.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "A secondary gallery that mimics your native system so perfectly, guests will never suspect a thing.",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    2, 3 -> {
                        val enteringConfirm = step == 3
                        val pinValue = if (enteringConfirm) pinSecond else pinFirst

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (enteringConfirm) "Confirm Owner PIN" else "Create Owner PIN",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (enteringConfirm) "Re-enter your 4-digit PIN" else "This PIN secures your real gallery references",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            
                            // Dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(vertical = 24.dp)
                            ) {
                                repeat(4) { idx ->
                                    val active = idx < pinValue.length
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (active) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                                            )
                                    )
                                }
                            }
                            
                            if (pinError.isNotEmpty()) {
                                Text(
                                    text = pinError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    4 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Decoy Albums",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Populate your gallery with realistic decoy photos (Pets, Vacation, Family, College) so it looks natural from the start.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(config.cornerRadiusDp.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { decoyPhotosEnabled = !decoyPhotosEnabled }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Enable Decoy Albums",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Pre-populates 5 harmless albums",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                                Switch(
                                    checked = decoyPhotosEnabled,
                                    onCheckedChange = { decoyPhotosEnabled = it }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Buttons / PIN Input Panel
            if (step == 1 || step == 4) {
                Button(
                    onClick = {
                        if (step == 1) {
                            permissionLauncher.launch(permissionsToRequest)
                        } else {
                            // Finish Setup!
                            repository.savePin(pinFirst)
                            repository.setDecoysEnabled(decoyPhotosEnabled)
                            onComplete()
                        }
                    },
                    shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = if (step == 1) "Get Started" else "Finish Setup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // PIN Keyboard
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    val rowModifier = Modifier.fillMaxWidth()
                    val buttonShape = RoundedCornerShape(config.cornerRadiusDp.dp)
                    val activePin = if (step == 2) pinFirst else pinSecond

                    val onKeyPress: (String) -> Unit = { digit ->
                        pinError = ""
                        val currentVal = activePin + digit
                        if (step == 2) {
                            if (pinFirst.length < 4) {
                                pinFirst = currentVal
                                if (pinFirst.length == 4) {
                                    step = 3
                                }
                            }
                        } else {
                            if (pinSecond.length < 4) {
                                pinSecond = currentVal
                                if (pinSecond.length == 4) {
                                    if (pinFirst == pinSecond) {
                                        step = 4
                                    } else {
                                        pinError = "PIN codes do not match"
                                        pinSecond = ""
                                    }
                                }
                            }
                        }
                    }

                    val onDeletePress: () -> Unit = {
                        pinError = ""
                        if (step == 2) {
                            if (pinFirst.isNotEmpty()) pinFirst = pinFirst.dropLast(1)
                        } else {
                            if (pinSecond.isNotEmpty()) pinSecond = pinSecond.dropLast(1)
                        }
                    }

                    @Composable
                    fun KeyButton(num: String) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.5f)
                                .clip(buttonShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { onKeyPress(num) }
                        ) {
                            Text(
                                text = num,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Keypad Rows
                    for (r in 0 until 3) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = rowModifier
                        ) {
                            for (c in 1..3) {
                                val num = (r * 3 + c).toString()
                                KeyButton(num)
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = rowModifier
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        KeyButton("0")
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.5f)
                                .clip(buttonShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .clickable { onDeletePress() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
