package com.skillswap.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.skillswap.model.ThreadMessage
import com.skillswap.ui.theme.OrangePrimary

/**
 * Message bubble with long-press menu for reactions, reply, delete
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedMessageBubble(
    message: ThreadMessage,
    isMe: Boolean,
    senderName: String?,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Reply preview if this is a reply
        message.replyTo?.let { replyTo ->
            ReplyPreview(
                replyToContent = replyTo.content,
                isMe = isMe
            )
        }
        
        Box {
            // Message bubble
            Surface(
                modifier = Modifier
                    .combinedClickable(
                        onClick = { },
                        onLongClick = { showContextMenu = true }
                    ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp
                ),
                color = if (isMe) OrangePrimary else Color(0xFFE8E8E8)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (!isMe && senderName != null) {
                        Text(
                            senderName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMe) Color.White else OrangePrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    Text(
                        if (message.isDeleted == true) "🚫 Message supprimé" else message.content,
                        color = if (isMe) Color.White else Color.Black,
                        fontStyle = if (message.isDeleted == true) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                    )
                }
            }
            
            // Context menu
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("👍 Réagir") },
                    onClick = {
                        showContextMenu = false
                        onReact("👍")
                    },
                    leadingIcon = {
                        Icon(Icons.Default.ThumbUp, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("💬 Répondre") },
                    onClick = {
                        showContextMenu = false
                        onReply()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Reply, contentDescription = null)
                    }
                )
                if (isMe && message.isDeleted != true) {
                    DropdownMenuItem(
                        text = { Text("🗑️ Supprimer") },
                        onClick = {
                            showContextMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                        }
                    )
                }
            }
        }
        
        // Reactions display
        message.reactions?.let { reactions ->
            if (reactions.isNotEmpty()) {
                ReactionsList(
                    reactions = reactions,
                    isMe = isMe,
                    onReactionClick = { emoji -> onReact(emoji) }
                )
            }
        }
    }
}

@Composable
fun ReplyPreview(
    replyToContent: String,
    isMe: Boolean
) {
    Surface(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .widthIn(max = 250.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFE0E0E0).copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .background(if (isMe) OrangePrimary else Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "Réponse à:",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    replyToContent,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun ReactionsList(
    reactions: Map<String, List<String>>,
    isMe: Boolean,
    onReactionClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(reactions.toList()) { (emoji, userIds) ->
            ReactionChip(
                emoji = emoji,
                count = userIds.size,
                onClick = { onReactionClick(emoji) }
            )
        }
    }
}

@Composable
fun ReactionChip(
    emoji: String,
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE8E8E8),
        modifier = Modifier.height(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 14.sp)
            if (count > 1) {
                Text(
                    count.toString(),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Reaction picker bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionPickerSheet(
    onDismiss: () -> Unit,
    onReactionSelected: (String) -> Unit
) {
    val commonReactions = listOf(
        "👍", "❤️", "😂", "😮", "😢", "🔥",
        "👏", "🎉", "💯", "✅", "❌", "🤔"
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Ajouter une réaction",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(commonReactions) { emoji ->
                    Surface(
                        onClick = {
                            onReactionSelected(emoji)
                            onDismiss()
                        },
                        shape = CircleShape,
                        color = Color(0xFFF5F5F5),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Reply-to banner shown at bottom when replying
 */
@Composable
fun ReplyingToBanner(
    message: ThreadMessage,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF5F5F5),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Reply,
                contentDescription = "Reply",
                tint = OrangePrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Répondre à:",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    message.content,
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = Color.DarkGray
                )
            }
            IconButton(onClick = onCancel) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = Color.Gray
                )
            }
        }
    }
}
