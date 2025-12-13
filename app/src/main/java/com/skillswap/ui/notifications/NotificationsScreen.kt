package com.skillswap.ui.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.model.NotificationItem
import com.skillswap.viewmodel.NotificationsViewModel
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NotificationsScreen(
    onCallStart: (String) -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToSession: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: NotificationsViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val message by viewModel.message.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPrefs by remember { mutableStateOf(false) }
    val prefs by viewModel.prefs.collectAsState()
    var showUnreadOnly by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                actions = {
                    IconButton(onClick = { showPrefs = true; onNavigateToSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Préférences")
                    }
                    if (unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllRead() }) {
                            Text("Tout marquer lu")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (message != null) {
            StatusBanner(
                text = message!!,
                background = Color(0xFFE6F4EA),
                content = Color(0xFF1B5E20),
                onDismiss = { viewModel.clearMessage() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        if (error != null) {
            StatusBanner(
                text = error!!,
                background = Color(0xFFFFEDEC),
                content = Color(0xFFB3261E),
                onDismiss = { viewModel.clearError() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Aucune notification")
            }
        } else {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                FilterChip(
                    selected = showUnreadOnly,
                    onClick = { showUnreadOnly = !showUnreadOnly },
                    label = { Text("Non lues") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFF5E5)
                    )
                )
            }
            val listToRender = if (showUnreadOnly) notifications.filter { !it.isRead } else notifications
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF2F2F7))
            ) {
                items(listToRender) { notif ->
                    val threadId = notif.payload?.get("threadId")?.toString()
                        ?: notif.payload?.get("thread")?.toString()
                    val sessionId = notif.payload?.get("sessionId")?.toString()
                        ?: notif.payload?.get("session")?.toString()
                    val meetingLink = notif.meetingUrl ?: notif.payload?.get("meetingUrl")?.toString()
                    val proposedDate = notif.proposedDate ?: notif.payload?.get("proposedDate")?.toString()
                    NotificationCard(
                        item = notif,
                        onAccept = {
                            viewModel.respond(notif.id, true)
                            if (notif.type == "call") {
                                val name = notif.payload?.get("callerName")?.toString() ?: notif.title
                                onCallStart(name)
                            }
                            threadId?.let { onNavigateToChat(it) }
                            sessionId?.let { onNavigateToSession(it) }
                        },
                        onDecline = { viewModel.respond(notif.id, false) },
                        onMarkRead = { viewModel.markRead(notif.id) },
                        onOpen = {
                            viewModel.markRead(notif.id)
                            when {
                        meetingLink?.isNotBlank() == true -> openUrl(context, meetingLink)
                        threadId != null -> onNavigateToChat(threadId)
                        sessionId != null -> onNavigateToSession(sessionId)
                    }
                        },
                        proposedDate = proposedDate,
                        reason = notif.reason ?: notif.payload?.get("reason")?.toString()
                    )
                }
            }
        }
    }

    if (showPrefs) {
        PrefsDialog(
            prefs = prefs,
            onUpdate = { chat, calls, marketing ->
                viewModel.updatePrefs(chat = chat, calls = calls, marketing = marketing)
            },
            onDismiss = { showPrefs = false }
        )
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}

@Composable
private fun StatusBanner(
    text: String,
    background: Color,
    content: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text, color = content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = content)
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: NotificationItem,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onMarkRead: () -> Unit,
    onOpen: () -> Unit,
    proposedDate: String? = null,
    reason: String? = null
) {
    val accent = when (item.type) {
        "match" -> Color(0xFFFA5940)
        "message" -> Color(0xFF5C52BF)
        "reminder" -> Color(0xFF00A8A8)
        "reschedule_request" -> Color(0xFFF28F24)
        "progress" -> Color(0xFF1B5E20)
        else -> Color(0xFFFF6B35)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) Color.White else Color(0xFFFFF5E5)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = "Notification", tint = accent)
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
                if (item.responded == true) {
                    Spacer(Modifier.weight(1f))
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = "Répondu", tint = Color(0xFF1B5E20))
                        },
                        label = { Text("Répondu", color = Color(0xFF1B5E20)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFE6F4EA)
                        )
                    )
                }
            }
            Text(
                item.message,
                color = Color.DarkGray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            item.meetingUrl?.takeIf { it.isNotBlank() }?.let { url ->
                AssistChip(
                    onClick = onOpen,
                    label = {
                        Text(
                            url,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFE8F4FF),
                        labelColor = Color(0xFF1A73E8)
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                item.createdAt,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
            item.senderId?.let {
                Text(
                    "De: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            proposedDate?.let {
                Text(
                    "Proposition: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5C52BF),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            reason?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "Motif: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (item.actionable != false && item.responded != true) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                    ) { Text("Accepter", modifier = Modifier.semantics { contentDescription = "Accepter la demande" }) }
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) { Text("Refuser", modifier = Modifier.semantics { contentDescription = "Refuser la demande" }) }
                }
            }

            if (!item.isRead) {
                TextButton(
                    onClick = onMarkRead,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Marquer comme lu")
                }
            }
        }
    }
}

@Composable
private fun PrefsDialog(
    prefs: com.skillswap.viewmodel.NotificationPrefs,
    onUpdate: (Boolean, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var chat by remember { mutableStateOf(prefs.chat) }
    var calls by remember { mutableStateOf(prefs.calls) }
    var marketing by remember { mutableStateOf(prefs.marketing) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onUpdate(chat, calls, marketing)
                onDismiss()
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Préférences de notification") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Chat")
                    Switch(checked = chat, onCheckedChange = { chat = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Appels")
                    Switch(checked = calls, onCheckedChange = { calls = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Marketing")
                    Switch(checked = marketing, onCheckedChange = { marketing = it })
                }
            }
        }
    )
}
