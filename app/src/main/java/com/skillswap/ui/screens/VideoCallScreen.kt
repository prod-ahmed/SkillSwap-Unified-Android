package com.skillswap.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.skillswap.services.CallManager
import com.skillswap.ui.theme.PrimaryOrange
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import kotlinx.coroutines.delay
import org.webrtc.EglBase

@Composable
fun VideoCallScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val callManager = remember { CallManager.getInstance(context) }
    var showControls by remember { mutableStateOf(true) }
    var controlsTimer by remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (System.currentTimeMillis() - controlsTimer > 5000) {
                showControls = false
            }
        }
    }
    
    LaunchedEffect(callManager.isCallActive) {
        if (!callManager.isCallActive) {
            onDismiss()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                showControls = !showControls
                controlsTimer = System.currentTimeMillis()
            }
    ) {
        // Remote Video (Full Screen)
        if (callManager.remoteVideoTrack != null) {
            AndroidView(
                factory = { ctx ->
                    VideoTextureViewRenderer(ctx).apply {
                        init(EglBase.create().eglBaseContext, null)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        callManager.remoteVideoTrack?.addSink(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
                        text = callManager.remoteUser?.username ?: "Unknown",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    
                    Text(
                        text = callManager.callStatus,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Local Video (Picture in Picture)
        if (callManager.isVideoEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(120.dp, 160.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoTextureViewRenderer(ctx).apply {
                            init(EglBase.create().eglBaseContext, null)
                            setMirror(true)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Controls Overlay
        if (showControls) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = callManager.remoteUser?.username ?: "Video Call",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = callManager.callStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    if (callManager.isVideoEnabled) {
                        IconButton(
                            onClick = { callManager.switchCamera() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Bottom Controls
                if (callManager.callStatus == "Incoming call...") {
                    IncomingCallControls(callManager)
                } else {
                    ActiveCallControls(callManager)
                }
            }
        }
    }
}

@Composable
private fun IncomingCallControls(callManager: CallManager) {
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
}

@Composable
private fun ActiveCallControls(callManager: CallManager) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
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
            
            // Video
            CallControlButton(
                icon = if (callManager.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                label = "Video",
                isActive = callManager.isVideoEnabled,
                onClick = { callManager.toggleVideo() }
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
                .size(56.dp)
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
