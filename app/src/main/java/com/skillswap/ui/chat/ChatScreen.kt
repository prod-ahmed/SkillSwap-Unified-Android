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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest

// Colors matching iOS
val ChatOrangeStart = Color(0xFFFF6B35)
val ChatOrangeEnd = Color(0xFFFFB347)

@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    onVideoCall: () -> Unit = {},
    onAudioCall: () -> Unit = {},
    onPlanSession: () -> Unit = {},
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
    val partnerId by viewModel.activePartnerId.collectAsState()
    val presence by viewModel.presence.collectAsState()
    val callState by callViewModel.state.collectAsState()
    val localVideo by callViewModel.localVideoTrack.collectAsState()
    val remoteVideo by callViewModel.remoteVideoTrack.collectAsState()
    val partnerTyping by viewModel.partnerTyping.collectAsState()
    val socketConnected by viewModel.socketConnected.collectAsState()
    val chatError by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<com.skillswap.model.ThreadMessage?>(null) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var pendingVideo by remember { mutableStateOf<Boolean?>(null) }
    val isPartnerOnline = partnerId?.let { presence[it] == true } ?: false
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
                callViewModel.startCall(partnerName ?: "", requestedVideo, partnerId, conversationId)
            }
        }
        pendingVideo = null
    }

    // File picker for attachments
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadAttachment(it, context)
        }
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
            callViewModel.startCall(partnerName ?: "", video, partnerId, conversationId)
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
            .background(MaterialTheme.colorScheme.background) // System Grouped Background
    ) {
        ChatHeader(
            partnerName = partnerName,
            partnerInitials = partnerInitials,
            partnerId = partnerId,
            isPartnerOnline = isPartnerOnline,
            callStateInCall = callState.isInCall,
            partnerTyping = partnerTyping,
            socketConnected = socketConnected,
            onBack = onBack,
            onVideoCall = { launchCall(true) },
            onAudioCall = { launchCall(false) }
        )

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
                    SwipeableMessageBubble(
                        message = message,
                        isOwnMessage = message.isMe,
                        onReply = { msg ->
                            replyingTo = com.skillswap.model.ThreadMessage(
                                id = msg.id,
                                threadId = conversationId,
                                senderId = "",
                                recipientId = null,
                                type = "text",
                                content = msg.text,
                                read = msg.read,
                                createdAt = msg.time
                            )
                        },
                        onReact = { msg, emoji ->
                            viewModel.reactToMessage(msg.id, emoji)
                        },
                        onImageClick = { imageUrl ->
                            fullScreenImageUrl = imageUrl
                        }
                    )
                }
            }
        }

        // Composer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Reply preview
                replyingTo?.let { reply ->
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(40.dp)
                                    .background(OrangePrimary, RoundedCornerShape(2.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Réponse à",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangePrimary
                                )
                                Text(
                                    text = reply.content.take(50) + if (reply.content.length > 50) "..." else "",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = { replyingTo = null }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
                
                chatError?.let {
                    ChatStatusBanner(text = it, onDismiss = { viewModel.clearError() })
                    Spacer(Modifier.height(8.dp))
                }
                // Reconnection banner disabled
                // if (!socketConnected && !isLoading && messages.isNotEmpty()) {
                //     ChatStatusBanner(
                //         text = "Reconnexion en cours…",
                //         onDismiss = { /* keep banner until reconnect */ }
                //     )
                //     Spacer(Modifier.height(8.dp))
                // }
             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Attach Button
                IconButton(
                    onClick = { filePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ChatOrangeStart.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Ajouter une pièce jointe", tint = ChatOrangeStart)
                }
                
                // Emoji Button
                IconButton(
                    onClick = { showEmojiPicker = !showEmojiPicker },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (showEmojiPicker) ChatOrangeStart.copy(alpha = 0.2f) else ChatOrangeStart.copy(alpha = 0.1f))
                ) {
                    Icon(
                        if (showEmojiPicker) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                        contentDescription = "Emoji",
                        tint = ChatOrangeStart
                    )
                }

                // Text Input
                TextField(
                    value = inputText,
                    onValueChange = { input ->
                        inputText = input
                        viewModel.sendTyping(input.isNotBlank())
                    },
                    placeholder = { Text("Message...", color = Color.Gray) },
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // Send Button
                val isSendEnabled = inputText.isNotBlank()
                // Send or Voice Record Button
                if (isSendEnabled) {
                    IconButton(
                        onClick = {
                            viewModel.sendMessage(inputText, replyTo = replyingTo)
                            inputText = ""
                            replyingTo = null
                            showEmojiPicker = false
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ChatOrangeStart, ChatOrangeEnd)))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                } else {
                    // Voice recorder when no text
                    VoiceRecorderButton(
                        onVoiceRecorded = { file ->
                            viewModel.uploadVoiceMessage(file, context)
                        },
                        accentColor = ChatOrangeStart
                    )
                }
            }
            
            // Emoji Picker
            EmojiPicker(
                visible = showEmojiPicker,
                onEmojiSelected = { emoji ->
                    inputText += emoji
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Plan Session Button
            Button(
                onClick = onPlanSession,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("📅 Planifier une session", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            }
            }
        }
    }
    
    // Full-screen image viewer
    fullScreenImageUrl?.let { imageUrl ->
        Dialog(
            onDismissRequest = { fullScreenImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullScreenImageUrl = null }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Full screen image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
                
                // Close button
                IconButton(
                    onClick = { fullScreenImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
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
private fun ChatHeader(
    partnerName: String?,
    partnerInitials: String,
    partnerId: String?,
    isPartnerOnline: Boolean,
    callStateInCall: Boolean,
    partnerTyping: Boolean,
    socketConnected: Boolean,
    onBack: () -> Unit,
    onVideoCall: () -> Unit,
    onAudioCall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(ChatOrangeStart, ChatOrangeEnd)
                )
            )
            .padding(top = 48.dp, bottom = 16.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
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

            Column(modifier = Modifier.weight(1f)) {
                Text(partnerName ?: "Conversation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        when {
                            callStateInCall -> "En appel"
                            partnerTyping -> "Écrit..."
                            else -> "Connecté"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    if (partnerId != null) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isPartnerOnline) Color(0xFF34C759) else Color.Gray.copy(alpha = 0.6f))
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderIcon(Icons.Default.Videocam) { onVideoCall() }
                HeaderIcon(Icons.Default.Call) { onAudioCall() }
            }
        }
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = message.time,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            if (isMe) {
                val statusColor = if (message.read) Color(0xFF4CAF50) else Color.Gray
                Text(
                    text = if (message.read) "Lu" else "Envoyé",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
        }
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
