package com.skillswap.ui.call

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.CallState
import kotlinx.coroutines.delay
import org.webrtc.VideoTrack
import org.webrtc.EglBase
import androidx.compose.ui.platform.LocalContext

@Composable
fun VideoCallScreen(
    callState: CallState,
    localVideoTrack: VideoTrack?,
    remoteVideoTrack: VideoTrack?,
    eglBaseContext: EglBase.Context?,
    onHangup: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onSwitchCamera: () -> Unit,
    onDismissEnded: () -> Unit
) {
    var showControls by remember { mutableStateOf(true) }
    
    // Auto-hide controls
    LaunchedEffect(showControls, callState.connectionStatus) {
        if (showControls && callState.connectionStatus == "connected") {
            delay(5000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        // Remote Video (Full Screen)
        if (callState.isVideo && remoteVideoTrack != null && eglBaseContext != null) {
            RemoteVideoRenderer(remoteVideoTrack, eglBaseContext)
        } else {
            // Avatar placeholder
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            callState.partnerName.take(1).uppercase(),
                            fontSize = 48.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        callState.partnerName,
                        fontSize = 24.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (callState.isRinging) "Appel entrant..." else callState.connectionStatus,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Local Video (PIP)
        if (callState.isVideo && callState.videoEnabled && localVideoTrack != null && eglBaseContext != null && !callState.ended) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .padding(top = 40.dp) // Avoid status bar
                    .width(100.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
            ) {
                LocalVideoRenderer(localVideoTrack, eglBaseContext)
            }
        }

        // Controls Overlay
        AnimatedVisibility(
            visible = showControls || callState.isRinging || callState.ended,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            callState.partnerName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (callState.callDurationSec > 0) formatDuration(callState.callDurationSec) else "Connexion...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                    if (callState.isVideo) {
                        IconButton(onClick = onSwitchCamera) {
                            Icon(Icons.Default.Cameraswitch, null, tint = Color.White)
                        }
                    }
                }

                // Bottom Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (callState.isRinging) {
                        // Incoming Call Controls
                        CallControlButton(
                            icon = Icons.Default.CallEnd,
                            label = "Refuser",
                            color = Color.Red,
                            onClick = onDecline
                        )
                        CallControlButton(
                            icon = Icons.Default.Call,
                            label = "Accepter",
                            color = Color.Green,
                            onClick = onAccept
                        )
                    } else if (callState.ended) {
                        CallControlButton(
                            icon = Icons.Default.Close,
                            label = "Fermer",
                            color = Color.Gray,
                            onClick = onDismissEnded
                        )
                    } else {
                        // Active Call Controls
                        CallControlButton(
                            icon = if (callState.muted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = "Micro",
                            color = if (callState.muted) Color.White else Color.White.copy(alpha = 0.2f),
                            contentColor = if (callState.muted) Color.Black else Color.White,
                            onClick = onToggleMute
                        )
                        
                        CallControlButton(
                            icon = if (callState.videoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            label = "Vidéo",
                            color = if (!callState.videoEnabled) Color.White else Color.White.copy(alpha = 0.2f),
                            contentColor = if (!callState.videoEnabled) Color.Black else Color.White,
                            onClick = onToggleVideo
                        )
                        
                        CallControlButton(
                            icon = if (callState.speakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            label = "Haut-parleur",
                            color = if (callState.speakerOn) Color.White else Color.White.copy(alpha = 0.2f),
                            contentColor = if (callState.speakerOn) Color.Black else Color.White,
                            onClick = onToggleSpeaker
                        )
                        
                        CallControlButton(
                            icon = Icons.Default.CallEnd,
                            label = "Raccrocher",
                            color = Color.Red,
                            onClick = onHangup
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    contentColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = contentColor),
            modifier = Modifier.size(64.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun RemoteVideoRenderer(track: VideoTrack?, eglBaseContext: EglBase.Context) {
    val context = LocalContext.current
    val renderer = remember {
        org.webrtc.SurfaceViewRenderer(context).apply {
            init(eglBaseContext, null)
            setEnableHardwareScaler(true)
        }
    }
    DisposableEffect(renderer, track) {
        track?.addSink(renderer)
        onDispose { track?.removeSink(renderer) }
    }
    AndroidView(
        factory = { renderer },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun LocalVideoRenderer(track: VideoTrack?, eglBaseContext: EglBase.Context) {
    val context = LocalContext.current
    val renderer = remember {
        org.webrtc.SurfaceViewRenderer(context).apply {
            init(eglBaseContext, null)
            setMirror(true)
            setEnableHardwareScaler(true)
            setZOrderMediaOverlay(true)
        }
    }
    DisposableEffect(renderer, track) {
        track?.addSink(renderer)
        onDispose { track?.removeSink(renderer) }
    }
    AndroidView(
        factory = { renderer },
        modifier = Modifier.fillMaxSize()
    )
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
