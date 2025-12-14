package com.skillswap.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillswap.model.Message
import com.skillswap.ui.theme.OrangePrimary
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableMessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    onReply: (Message) -> Unit,
    onReact: (Message, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var showReactions by remember { mutableStateOf(false) }
    val swipeThreshold = 100f
    
    // Animate offset back to 0 when released
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipe"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
        ) {
            // Reply icon (shown when swiping)
            if (!isOwnMessage && animatedOffset > 20f) {
                Icon(
                    imageVector = Icons.Default.Reply,
                    contentDescription = "Reply",
                    tint = OrangePrimary,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 8.dp)
                        .alpha((animatedOffset / swipeThreshold).coerceIn(0f, 1f))
                )
            }
            
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX > swipeThreshold) {
                                    onReply(message)
                                }
                                offsetX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = offsetX + dragAmount
                                // Only allow right swipe for received messages
                                if (!isOwnMessage) {
                                    offsetX = newOffset.coerceIn(0f, swipeThreshold * 1.5f)
                                }
                                // Only allow left swipe for sent messages
                                else {
                                    offsetX = newOffset.coerceIn(-swipeThreshold * 1.5f, 0f)
                                }
                            }
                        )
                    }
                    .combinedClickable(
                        onClick = { /* Normal click */ },
                        onLongClick = {
                            showReactions = true
                        }
                    )
            ) {
                Column(
                    horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
                ) {
                    // Reply indicator if message is a reply
                    message.replyTo?.let { replyTo ->
                        ReplyIndicator(
                            replyToText = replyTo.content,
                            isOwnMessage = isOwnMessage
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    
                    // Message bubble
                    Surface(
                        color = if (isOwnMessage) OrangePrimary else Color(0xFFEEEEEE),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                            bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                        ),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = message.text,
                                color = if (isOwnMessage) Color.White else Color.Black,
                                fontSize = 15.sp
                            )
                            
                            Spacer(Modifier.height(4.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = message.time,
                                    fontSize = 11.sp,
                                    color = if (isOwnMessage) Color.White.copy(alpha = 0.7f) else Color.Gray
                                )
                                
                                if (isOwnMessage) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (message.read) Icons.Default.DoneAll else Icons.Default.Done,
                                        contentDescription = if (message.read) "Read" else "Sent",
                                        tint = if (message.read) Color(0xFF4FC3F7) else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Reactions
                    if (!message.reactions.isNullOrEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        ReactionsRow(
                            reactions = message.reactions,
                            onClick = { emoji -> onReact(message, emoji) }
                        )
                    }
                }
            }
            
            // Reply icon for sent messages (shown when swiping left)
            if (isOwnMessage && animatedOffset < -20f) {
                Icon(
                    imageVector = Icons.Default.Reply,
                    contentDescription = "Reply",
                    tint = OrangePrimary,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 8.dp)
                        .alpha((-animatedOffset / swipeThreshold).coerceIn(0f, 1f))
                )
            }
        }
    }
    
    // Reaction picker dialog
    if (showReactions) {
        ReactionPicker(
            onDismiss = { showReactions = false },
            onReactionSelected = { emoji ->
                onReact(message, emoji)
                showReactions = false
            }
        )
    }
}

@Composable
fun ReplyIndicator(
    replyToText: String,
    isOwnMessage: Boolean
) {
    Row(
        modifier = Modifier
            .padding(bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .background(
                    if (isOwnMessage) Color.White.copy(alpha = 0.5f) else OrangePrimary,
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = "Réponse à",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOwnMessage) Color.White.copy(alpha = 0.7f) else OrangePrimary
            )
            Text(
                text = replyToText.take(50) + if (replyToText.length > 50) "..." else "",
                fontSize = 12.sp,
                color = if (isOwnMessage) Color.White.copy(alpha = 0.6f) else Color.Gray,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ReactionsRow(
    reactions: Map<String, List<String>>,
    onClick: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(start = 4.dp)
    ) {
        reactions.forEach { (emoji, users) ->
            Surface(
                onClick = { onClick(emoji) },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = emoji, fontSize = 14.sp)
                    if (users.size > 1) {
                        Text(
                            text = users.size.toString(),
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReactionPicker(
    onDismiss: () -> Unit,
    onReactionSelected: (String) -> Unit
) {
    val reactions = listOf("❤️", "👍", "👎", "😂", "😮", "😢", "🙏")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Réagir au message") },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                reactions.forEach { emoji ->
                    Surface(
                        onClick = { onReactionSelected(emoji) },
                        shape = CircleShape,
                        color = Color(0xFFF5F5F5),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 24.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

private fun formatMessageTime(timestamp: String): String {
    return try {
        // Simple time formatting - you can enhance this
        val time = timestamp.substringAfter("T").substringBefore(".")
        val parts = time.split(":")
        "${parts[0]}:${parts[1]}"
    } catch (e: Exception) {
        ""
    }
}
