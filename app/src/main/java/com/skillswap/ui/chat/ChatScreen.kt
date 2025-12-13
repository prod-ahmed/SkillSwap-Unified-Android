package com.skillswap.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.skillswap.model.Message
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.ChatViewModel
import com.skillswap.viewmodel.CallViewModel
import org.webrtc.VideoTrack
import org.webrtc.EglBase
import android.widget.Toast

// Colors matching iOS
val ChatOrangeStart = Color(0xFFFF6B35)
val ChatOrangeEnd = Color(0xFFFFB347)

@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
    callViewModel: CallViewModel = viewModel()
) {
    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.sendTyping(false) }
    }

    val messages by viewModel.messages.collectAsState()
    val partnerName by viewModel.activePartnerName.collectAsState()
    val partnerInitials by viewModel.activePartnerInitials.collectAsState()
    val callState by callViewModel.state.collectAsState()
    val localVideo by callViewModel.localVideoTrack.collectAsState()
    val remoteVideo by callViewModel.remoteVideoTrack.collectAsState()
    val partnerTyping by viewModel.partnerTyping.collectAsState()
    val socketConnected by viewModel.socketConnected.collectAsState()
    val chatError by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var pendingVideo by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(callState.error) {
        callState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            callViewModel.clearError()
        }
    }
    LaunchedEffect(chatError) {
        chatError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allow = grants.values.all { it }
        pendingVideo?.let { requestedVideo ->
            if (allow) {
                callViewModel.startCall(partnerName ?: "", requestedVideo, conversationId)
            }
        }
        pendingVideo = null
    }

    fun launchCall(video: Boolean) {
        val required = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (video) add(Manifest.permission.CAMERA)
        }
        val allGranted = required.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            callViewModel.startCall(partnerName ?: "", video, conversationId)
        } else {
            pendingVideo = video
            permissionLauncher.launch(required.toTypedArray())
        }
    }
    
    // Auto-scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)) // System Grouped Background
    ) {
        // Custom Gradient Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(ChatOrangeStart, ChatOrangeEnd)
                    )
                )
                .padding(top = 48.dp, bottom = 16.dp) // Status bar padding roughly
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.White)
                }

                Spacer(Modifier.width(12.dp))

                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(partnerInitials.ifBlank { "?" }, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.width(12.dp))

                // Name & Status
                Column(modifier = Modifier.weight(1f)) {
                    Text(partnerName ?: "Conversation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        when {
                            callState.isInCall -> "En appel"
                            partnerTyping -> "Écrit..."
                            else -> if (socketConnected) "En ligne" else "Reconnexion…"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderIcon(Icons.Default.Videocam) {
                        launchCall(video = true)
                    }
                    HeaderIcon(Icons.Default.Call) {
                        launchCall(video = false)
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = ChatOrangeStart) }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aucun message encore. Démarrez la conversation !", color = Color.Gray)
                        }
                    }
                }
            } else {
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ChatOrangeStart) }
                    }
                }
                items(messages) { message ->
                    ChatBubble(message)
                }
            }
        }

        // Composer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                chatError?.let {
                    ChatStatusBanner(text = it, onDismiss = { viewModel.clearError() })
                    Spacer(Modifier.height(8.dp))
                }
                if (!socketConnected && !isLoading) {
                    ChatStatusBanner(text = "Reconnexion en cours…", onDismiss = { /* keep banner until reconnect */ })
                    Spacer(Modifier.height(8.dp))
                }
             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Attach Button
                IconButton(
                    onClick = { /* attachment placeholder */ },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ChatOrangeStart.copy(alpha = 0.1f))
                ) {
                Icon(Icons.Default.AttachFile, contentDescription = "Ajouter une pièce jointe", tint = ChatOrangeStart)
                }

                // Text Input
                TextField(
                    value = inputText,
                    onValueChange = { input ->
                        inputText = input
                        viewModel.sendTyping(input.isNotBlank())
                    },
                    placeholder = { Text("Écrivez votre message...", color = Color.Gray) },
                    modifier = Modifier
                        .weight(1f),
                       // .heightIn(min = 44.dp, max = 100.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF2F2F7),
                        unfocusedContainerColor = Color(0xFFF2F2F7),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // Send Button
                val isSendEnabled = inputText.isNotBlank()
                IconButton(
                    onClick = {
                        if (isSendEnabled) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                             if (isSendEnabled) Brush.linearGradient(listOf(ChatOrangeStart, ChatOrangeEnd))
                             else SolidColor(Color.LightGray)
                        )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Plan Session Button
            Button(
                onClick = { /* navigate to sessions or open scheduler */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7)),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("📅 Planifier une session", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (callState.isInCall || callState.isRinging) {
        CallOverlay(
            partner = callState.partnerName,
            isVideo = callState.isVideo,
            muted = callState.muted,
            speakerOn = callState.speakerOn,
            isRinging = callState.isRinging,
            ended = callState.ended,
            connectionStatus = callState.connectionStatus,
            localSdp = callState.localSdp,
            iceCandidates = callState.iceCandidates,
            localVideoTrack = localVideo,
            remoteVideoTrack = remoteVideo,
            callDurationSec = callState.callDurationSec,
            eglBaseContext = callViewModel.eglBaseContext,
            onHangup = { callViewModel.hangUp() },
            onAccept = { callViewModel.acceptIncomingCall() },
            onDecline = { callViewModel.declineIncomingCall() },
            onToggleMute = { callViewModel.toggleMute() },
            onToggleSpeaker = { callViewModel.toggleSpeaker() },
            onToggleVideo = { callViewModel.toggleVideo() },
            onSwitchCamera = { callViewModel.switchCamera() },
            videoEnabled = callState.videoEnabled,
            onDismissEnded = { callViewModel.clearEnded() }
        )
    }
}

@Composable
fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = "Action chat", tint = Color.White)
    }
}

@Composable
fun ChatBubble(message: Message) {
    val isMe = message.isMe
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (isMe) Brush.linearGradient(listOf(ChatOrangeStart, ChatOrangeEnd))
                    else SolidColor(Color.White)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                color = if (isMe) Color.White else Color.Black,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(Modifier.height(4.dp))
        
        Text(
            text = message.time,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun CallOverlay(
    partner: String,
    isVideo: Boolean,
    muted: Boolean,
    speakerOn: Boolean,
    isRinging: Boolean,
    ended: Boolean,
    connectionStatus: String,
    localSdp: String?, // kept for parity/debug display if needed
    iceCandidates: List<com.skillswap.model.CallIceCandidate>, // kept for future signaling UI
    localVideoTrack: VideoTrack?,
    remoteVideoTrack: VideoTrack?,
    callDurationSec: Int,
    eglBaseContext: org.webrtc.EglBase.Context,
    onHangup: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onSwitchCamera: () -> Unit,
    videoEnabled: Boolean,
    onDismissEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .semantics { contentDescription = if (ended) "Appel terminé" else "Overlay d'appel" }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (ended) {
                    Text("Appel terminé", fontWeight = FontWeight.Bold)
                    StatusChip(text = "Déconnecté")
                    Button(onClick = onDismissEnded) { Text("Fermer") }
                } else {
                    Text(
                        text = when {
                            isRinging && isVideo -> "Appel vidéo entrant"
                            isRinging -> "Appel audio entrant"
                            isVideo -> "Appel vidéo"
                            else -> "Appel audio"
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Text(partner, style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            isRinging -> "Sonnerie…"
                            connectionStatus.equals("connected", ignoreCase = true) -> "Connecté"
                            connectionStatus.equals("completed", ignoreCase = true) -> "Connecté"
                            else -> connectionStatus
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    if (!isRinging && callDurationSec > 0) {
                        StatusChip(text = formatDuration(callDurationSec))
                    }
                    if (isVideo) {
                        RemoteVideoRenderer(remoteVideoTrack, eglBaseContext)
                        Spacer(Modifier.height(8.dp))
                        LocalVideoRenderer(localVideoTrack, eglBaseContext)
                    }
                    if (!isRinging) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(text = connectionStatus.ifBlank { "Connexion..." })
                            StatusChip(text = if (videoEnabled) "Vidéo active" else "Vidéo coupée")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconButton(onClick = onToggleMute) {
                                Icon(
                                    if (muted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = if (muted) "Micro coupé" else "Micro actif"
                                )
                            }
                            if (isVideo) {
                                IconButton(onClick = onToggleVideo) {
                                    Icon(
                                        imageVector = if (videoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                        contentDescription = if (videoEnabled) "Vidéo active" else "Vidéo coupée"
                                    )
                                }
                                IconButton(onClick = onSwitchCamera) {
                                    Icon(Icons.Default.Cameraswitch, contentDescription = "Basculer caméra")
                                }
                            }
                            IconButton(onClick = onToggleSpeaker) {
                                Icon(
                                    imageVector = if (speakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = if (speakerOn) "Haut-parleur activé" else "Haut-parleur désactivé"
                                )
                            }
                            IconButton(onClick = onHangup) {
                                Icon(Icons.Default.CallEnd, contentDescription = "Raccrocher", tint = Color.Red)
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onAccept) {
                                Text("Accepter")
                            }
                            OutlinedButton(onClick = onDecline) {
                                Text("Refuser")
                            }
                        }
                    }
                }
            }
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
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
        }
    }
    DisposableEffect(renderer, track) {
        track?.addSink(renderer)
        onDispose { track?.removeSink(renderer) }
    }
    AndroidView(
        factory = { renderer },
        modifier = Modifier
            .size(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    )
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

@Composable
private fun StatusChip(text: String) {
    Surface(
        color = Color(0xFFF2F2F7),
        shape = RoundedCornerShape(50),
        shadowElevation = 0.dp
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun ChatStatusBanner(text: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEDEC)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text, color = Color(0xFFB3261E), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color(0xFFB3261E))
            }
        }
    }
}
