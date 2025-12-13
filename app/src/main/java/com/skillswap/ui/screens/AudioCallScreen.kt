package com.skillswap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.skillswap.services.CallManager
import com.skillswap.ui.theme.DarkGray
import com.skillswap.ui.theme.PrimaryOrange

@Composable
fun AudioCallScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val callManager = remember { CallManager.getInstance(context) }
    
    LaunchedEffect(callManager.isCallActive) {
        if (!callManager.isCallActive) {
            onDismiss()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF2C3E50)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Caller Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(PrimaryOrange.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = PrimaryOrange
                    )
                }
                
                Text(
                    text = callManager.remoteUser?.username ?: "Unknown User",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                
                Text(
                    text = callManager.callStatus,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Controls
            if (callManager.callStatus == "Incoming call...") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 50.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Decline
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { callManager.endCall() },
                            containerColor = Color.Red,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                Icons.Default.CallEnd,
                                contentDescription = "Decline",
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }
                        Text("Decline", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    // Accept
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { callManager.answerCall() },
                            containerColor = Color.Green,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Accept",
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }
                        Text("Accept", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Mute
                        CallControlButton(
                            icon = if (callManager.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = "Mute",
                            isActive = callManager.isMuted,
                            onClick = { callManager.toggleMute() }
                        )
                        
                        // Speaker
                        CallControlButton(
                            icon = Icons.Default.VolumeUp,
                            label = "Speaker",
                            isActive = callManager.isSpeakerOn,
                            onClick = { callManager.toggleSpeaker() }
                        )
                    }
                    
                    // End Call
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { callManager.endCall() },
                            containerColor = Color.Red,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }
                        Text("End", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
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
                .background(if (isActive) Color.White else Color.White.copy(alpha = 0.2f))
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else Color.White
            )
        }
        Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}
