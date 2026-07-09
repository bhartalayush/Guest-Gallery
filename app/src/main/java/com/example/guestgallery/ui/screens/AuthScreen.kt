package com.example.guestgallery.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.guestgallery.data.GalleryRepository
import com.example.guestgallery.theme.LocalAdaptiveUiConfig

@Composable
fun AuthScreen(
    action: String,
    repository: GalleryRepository,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = LocalAdaptiveUiConfig.current
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val activity = remember(context) {
        var c = context
        while (c is ContextWrapper) {
            if (c is FragmentActivity) break
            c = c.baseContext
        }
        c as? FragmentActivity
    }

    val triggerBiometrics = {
        activity?.let { act ->
            val executor = ContextCompat.getMainExecutor(act)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    errorMessage = ""
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (action == "exit" || action == "unlock") {
                        onCancel()
                    }
                }
            }

            val biometricPrompt = BiometricPrompt(act, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm Owner Identity")
                .setSubtitle("Authenticate to access owner settings")
                .setAllowedAuthenticators(
                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                // Biometrics not available
            }
        }
    }

    // Proactively launch biometrics on enter
    LaunchedEffect(Unit) {
        triggerBiometrics()
    }

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
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = "Guest View Lock",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (action == "settings") "Enter PIN to access settings" else "Enter PIN to exit guest mode",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            // Dot Indicators
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    repeat(4) { index ->
                        val active = index < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                                )
                        )
                    }
                }
                
                AnimatedVisibility(
                    visible = errorMessage.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween((300 * config.animationScale).toInt())),
                    exit = fadeOut(animationSpec = tween((300 * config.animationScale).toInt()))
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // Numeric PIN pad
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                val rowModifier = Modifier.fillMaxWidth()
                val buttonShape = RoundedCornerShape(config.cornerRadiusDp.dp)
                
                val pinKeyPress: (String) -> Unit = { key ->
                    if (pinInput.length < 4) {
                        errorMessage = ""
                        pinInput += key
                        if (pinInput.length == 4) {
                            if (repository.verifyPin(pinInput)) {
                                onSuccess()
                            } else {
                                errorMessage = "Incorrect PIN code"
                                pinInput = ""
                            }
                        }
                    }
                }

                @Composable
                fun PinButton(text: String, onClick: () -> Unit) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.5f)
                            .clip(buttonShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onClick() }
                    ) {
                        Text(
                            text = text,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Row 1-3
                for (row in 0 until 3) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = rowModifier
                    ) {
                        for (col in 1..3) {
                            val num = (row * 3 + col).toString()
                            PinButton(text = num) { pinKeyPress(num) }
                        }
                    }
                }

                // Bottom row: Cancel/Biometrics, 0, Backspace
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = rowModifier
                ) {
                    // Left Action (Cancel or Biometrics retry)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.5f)
                            .clip(buttonShape)
                            .background(Color.Transparent)
                            .clickable {
                                if (activity != null) triggerBiometrics() else onCancel()
                            }
                    ) {
                        if (activity != null) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Retry biometrics",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Text(
                                text = "Cancel",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Zero key
                    PinButton(text = "0") { pinKeyPress("0") }

                    // Backspace key
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.5f)
                            .clip(buttonShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable {
                                if (pinInput.isNotEmpty()) {
                                    pinInput = pinInput.dropLast(1)
                                } else {
                                    onCancel()
                                }
                            }
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
