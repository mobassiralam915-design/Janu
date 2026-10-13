package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.persona.ZoyaPersona
import com.example.ui.components.ActionToastCard
import com.example.ui.components.AssistantVisualState
import com.example.ui.components.MoodSelectorSheet
import com.example.ui.components.SettingsDialog
import com.example.ui.components.WaveformVisualizer
import com.example.ui.components.ZoyaHolographicOrb
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepAbyss
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlowingAmber
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ZoyaViewModel

@Composable
fun ZoyaScreen(
    viewModel: ZoyaViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.startListening()
        } else {
            Toast.makeText(context, "Microphone permission required for voice interaction", Toast.LENGTH_SHORT).show()
        }
    }

    fun vibrateShort() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (_: Exception) {}
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ButtonPulse")
    val buttonGlowPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepAbyss
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DeepAbyss,
                            DarkSurface.copy(alpha = 0.95f),
                            DeepAbyss
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top App Bar Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo & Sassy Persona Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurfaceVariant.copy(alpha = 0.7f))
                            .clickable {
                                vibrateShort()
                                viewModel.setMoodSheetOpen(true)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("mood_badge_chip")
                    ) {
                        Text(
                            text = uiState.activeMood.emoji,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Column {
                            Text(
                                text = "ZOYA",
                                color = uiState.activeMood.primaryColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = uiState.activeMood.title,
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Top Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Battery & Energy level check
                        IconButton(
                            onClick = {
                                vibrateShort()
                                viewModel.checkBatteryAndEnergy()
                            },
                            modifier = Modifier.testTag("check_battery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Check Battery & Energy",
                                tint = GlowingAmber
                            )
                        }

                        // Hands-free toggle badge
                        IconButton(
                            onClick = {
                                vibrateShort()
                                viewModel.toggleHandsFreeMode()
                            },
                            modifier = Modifier.testTag("hands_free_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isHandsFreeMode) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.AutoAwesome,
                                contentDescription = "Toggle Hands Free",
                                tint = if (uiState.isHandsFreeMode) ElectricCyan else TextSecondary
                            )
                        }

                        IconButton(
                            onClick = {
                                vibrateShort()
                                viewModel.setSettingsOpen(true)
                            },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                // Action Notification Toast (if any tool executed)
                ActionToastCard(
                    result = uiState.lastToolResult,
                    onDismiss = { viewModel.clearToolResult() },
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Central Hypnotic Holographic Orb
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ZoyaHolographicOrb(
                        state = uiState.visualState,
                        mood = uiState.activeMood,
                        audioAmplitude = uiState.audioAmplitude,
                        modifier = Modifier
                            .clickable {
                                vibrateShort()
                                if (uiState.visualState == AssistantVisualState.SPEAKING) {
                                    viewModel.interrupt()
                                } else {
                                    if (hasAudioPermission) {
                                        viewModel.toggleListening()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                            .testTag("holographic_orb")
                    )
                }

                // Live Dynamic Subtitle / Response Glass HUD
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Pill
                    Text(
                        text = uiState.statusText,
                        color = when (uiState.visualState) {
                            AssistantVisualState.LISTENING -> ElectricCyan
                            AssistantVisualState.SPEAKING -> uiState.activeMood.primaryColor
                            AssistantVisualState.THINKING -> uiState.activeMood.secondaryColor
                            AssistantVisualState.IDLE -> TextMuted
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Spoken Subtitle Box
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("subtitle_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkSurfaceVariant.copy(alpha = 0.65f)
                        ),
                        border = BorderStroke(0.8.dp, uiState.activeMood.primaryColor.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            if (uiState.userSpeech.isNotBlank() && uiState.visualState == AssistantVisualState.LISTENING) {
                                Text(
                                    text = "“${uiState.userSpeech}”",
                                    color = ElectricCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Text(
                                text = uiState.assistantResponse,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Audio Equalizer Waveform
                    WaveformVisualizer(
                        amplitude = uiState.audioAmplitude,
                        primaryColor = uiState.activeMood.primaryColor,
                        secondaryColor = uiState.activeMood.secondaryColor,
                        isActive = uiState.visualState == AssistantVisualState.LISTENING ||
                                uiState.visualState == AssistantVisualState.SPEAKING
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Prompt Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZoyaPersona.QUICK_PROMPTS.forEachIndexed { index, prompt ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    vibrateShort()
                                    viewModel.sendQuickPrompt(prompt)
                                }
                                .testTag("quick_prompt_chip_$index"),
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurfaceVariant.copy(alpha = 0.8f),
                            border = BorderStroke(0.5.dp, Color(0x33FFFFFF))
                        ) {
                            Text(
                                text = prompt,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Controls: Central Mic / Interruption Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isListening = uiState.visualState == AssistantVisualState.LISTENING
                    val isSpeaking = uiState.visualState == AssistantVisualState.SPEAKING

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(88.dp)
                    ) {
                        // Pulsing outer ripple when listening
                        if (isListening) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp * buttonGlowPulse)
                                    .clip(CircleShape)
                                    .background(ElectricCyan.copy(alpha = 0.25f))
                            )
                        } else if (isSpeaking) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp * buttonGlowPulse)
                                    .clip(CircleShape)
                                    .background(NeonMagenta.copy(alpha = 0.25f))
                            )
                        }

                        // Main Button
                        Surface(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable {
                                    vibrateShort()
                                    if (isSpeaking) {
                                        viewModel.interrupt()
                                    } else {
                                        if (hasAudioPermission) {
                                            viewModel.toggleListening()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                                .testTag("central_mic_button"),
                            shape = CircleShape,
                            color = when {
                                isListening -> ElectricCyan
                                isSpeaking -> NeonMagenta
                                else -> uiState.activeMood.primaryColor
                            },
                            shadowElevation = 8.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = when {
                                        isSpeaking -> Icons.Default.Stop
                                        isListening -> Icons.Default.MicOff
                                        else -> Icons.Default.Mic
                                    },
                                    contentDescription = when {
                                        isSpeaking -> "Stop speaking"
                                        isListening -> "Stop listening"
                                        else -> "Start talking"
                                    },
                                    tint = if (isListening) Color.Black else Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Mood Selector Sheet
    if (uiState.isMoodSheetOpen) {
        MoodSelectorSheet(
            currentMood = uiState.activeMood,
            onMoodSelected = { mood ->
                vibrateShort()
                viewModel.changeMood(mood)
            },
            onDismiss = { viewModel.setMoodSheetOpen(false) }
        )
    }

    // Settings Dialog
    if (uiState.isSettingsOpen) {
        SettingsDialog(
            initialApiKey = uiState.customApiKey,
            isHandsFreeMode = uiState.isHandsFreeMode,
            onSaveApiKey = { key ->
                viewModel.setCustomApiKey(key)
            },
            onToggleHandsFree = {
                viewModel.toggleHandsFreeMode()
            },
            onDismiss = { viewModel.setSettingsOpen(false) }
        )
    }
}
