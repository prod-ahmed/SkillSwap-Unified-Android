package com.skillswap.ui.call

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillswap.ui.theme.OrangePrimary
import kotlinx.coroutines.delay

@Composable
fun VideoCallScreen(
    participantName: String,
    callStatus: String = "En cours...",
    isVideoEnabled: Boolean = true,
    isAudioEnabled: Boolean = true,
    isFrontCamera: Boolean = true,
    onEndCall: () -> Unit,
    onToggleVideo: () -> Unit,
    onToggleAudio: () -> Unit,
    onSwitchCamera: () -> Unit
) {
    var showControls by remember { mutableStateOf(true) }
    var callDurationSeconds by remember { mutableStateOf(0) }
    
    // Auto-hide controls after 5 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }
    
    // Call duration timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            callDurationSeconds++
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Remote Video View (Full Screen)
        // In a real implementation, this would be a WebRTC VideoRenderer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder for remote video
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = OrangePrimary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    participantName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    callStatus,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        
        // Local Video View (Picture-in-Picture)
        if (isVideoEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = "Local Video",
                        modifier = Modifier.size(40.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
        
        // Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                participantName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                formatDuration(callDurationSeconds),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        
                        if (isVideoEnabled) {
                            IconButton(
                                onClick = onSwitchCamera
                            ) {
                                Icon(
                                    Icons.Default.Cameraswitch,
                                    contentDescription = "Switch Camera",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Bottom Controls
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute/Unmute Button
                        CallControlButton(
                            icon = if (isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            label = if (isAudioEnabled) "Mute" else "Unmute",
                            backgroundColor = if (isAudioEnabled) Color.White.copy(alpha = 0.2f) else Color.Red,
                            onClick = onToggleAudio
                        )
                        
                        // End Call Button
                        CallControlButton(
                            icon = Icons.Default.CallEnd,
                            label = "End",
                            backgroundColor = Color.Red,
                            onClick = onEndCall
                        )
                        
                        // Video On/Off Button
                        CallControlButton(
                            icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            label = if (isVideoEnabled) "Video" else "Video Off",
                            backgroundColor = if (isVideoEnabled) Color.White.copy(alpha = 0.2f) else Color.Red,
                            onClick = onToggleVideo
                        )
                    }
                }
            }
        }
        
        // Tap to toggle controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!showControls) {
                        Modifier.clickable { showControls = true }
                    } else {
                        Modifier
                    }
                )
        )
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(backgroundColor)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            label,
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
