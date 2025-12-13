package com.skillswap.ui.sessions

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.skillswap.model.Session
import com.skillswap.ui.theme.OrangeGradientEnd
import com.skillswap.ui.theme.OrangeGradientStart
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.SessionsViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.foundation.background
import androidx.compose.material3.HorizontalDivider
import coil.compose.AsyncImage
import kotlin.math.absoluteValue

@Suppress("UnusedParameter")
@Composable
fun SessionsScreen(
    navController: NavController,
    viewModel: SessionsViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadSessions()
    }

    val sessions by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val lessonPlan by viewModel.lessonPlan.collectAsState()
    val planLoading by viewModel.planLoading.collectAsState()
    val planError by viewModel.planError.collectAsState()
    
    var selectedFilter by remember { mutableStateOf(0) } // 0=Upcoming, 1=Completed, 2=Postponed
    var showCreate by remember { mutableStateOf(false) }
    var rescheduleTarget by remember { mutableStateOf<Session?>(null) }
    var rescheduleDate by remember { mutableStateOf("") }
    var rescheduleTime by remember { mutableStateOf("") }
    var rescheduleNote by remember { mutableStateOf("") }
    var ratingTarget by remember { mutableStateOf<Session?>(null) }
    var ratingValue by remember { mutableStateOf(4f) }
    var ratingComment by remember { mutableStateOf("") }
    var planTarget by remember { mutableStateOf<Session?>(null) }

    val filteredSessions = remember(sessions, selectedFilter) {
        when (selectedFilter) {
            0 -> sessions.filter { it.status == "upcoming" }
            1 -> sessions.filter { it.status == "completed" }
            2 -> sessions.filter { it.status == "reportee" || it.status == "postponed" || it.rescheduleRequest?.isActive == true }
            else -> sessions
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = OrangePrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Session")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Text(
                "Mes Sessions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            // Gradient Summary Card
            SummaryCard(filteredCount = filteredSessions.size, filterType = selectedFilter)

            // Tabs
            Row(modifier = Modifier.padding(16.dp)) {
                FilterTab("À venir", 0, selectedFilter) { selectedFilter = 0 }
                Spacer(Modifier.width(8.dp))
                FilterTab("Terminées", 1, selectedFilter) { selectedFilter = 1 }
                Spacer(Modifier.width(8.dp))
                FilterTab("Reportées", 2, selectedFilter) { selectedFilter = 2 }
            }

            successMessage?.let {
                StatusBanner(
                    text = it,
                    background = Color(0xFFE6F4EA),
                    content = Color(0xFF1B5E20),
                    onDismiss = { viewModel.clearMessages() }
                )
            }
            errorMessage?.let {
                StatusBanner(
                    text = it,
                    background = Color(0xFFFFEDEC),
                    content = Color(0xFFB3261E),
                    onDismiss = { viewModel.clearMessages() }
                )
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else if (filteredSessions.isEmpty()) {
                EmptySessionsState(onRefresh = { viewModel.loadSessions() })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredSessions) { session ->
                        SessionCard(
                            session = session,
                            onPostpone = { viewModel.postponeSession(session.id) },
                            onProposeReschedule = {
                                rescheduleTarget = session
                                rescheduleDate = session.date
                                rescheduleTime = "10:00"
                                rescheduleNote = ""
                            },
                            onRespondReschedule = { accept ->
                                viewModel.respondToReschedule(session.id, accept)
                            },
                            onRate = {
                                ratingTarget = session
                                ratingValue = 4f
                                ratingComment = ""
                            },
                            onOpenPlan = {
                                planTarget = session
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateSessionDialog(
            onDismiss = { showCreate = false },
            onCreate = { title, skill, studentEmail, date, duration, meetingLink, notes ->
                viewModel.createSession(title, skill, studentEmail, date, duration, meetingLink, notes)
                showCreate = false
            }
        )
    }

    if (rescheduleTarget != null) {
        RescheduleDialog(
            date = rescheduleDate,
            time = rescheduleTime,
            note = rescheduleNote,
            onDateChange = { rescheduleDate = it },
            onTimeChange = { rescheduleTime = it },
            onNoteChange = { rescheduleNote = it },
            onDismiss = { rescheduleTarget = null },
            onConfirm = {
                rescheduleTarget?.let {
                    viewModel.proposeReschedule(it.id, rescheduleDate, rescheduleTime, rescheduleNote)
                }
                rescheduleTarget = null
            }
        )
    }

    if (ratingTarget != null) {
        RatingDialog(
            rating = ratingValue,
            comment = ratingComment,
            onRatingChange = { ratingValue = it },
            onCommentChange = { ratingComment = it },
            onDismiss = { ratingTarget = null },
            onConfirm = {
                ratingTarget?.let { session ->
                    viewModel.rateSession(session, ratingValue.toInt(), ratingComment)
                }
                ratingTarget = null
            }
        )
    }

    planTarget?.let { target ->
        LaunchedEffect(target.id) {
            viewModel.loadLessonPlan(target.id)
        }
        LessonPlanDialog(
            sessionTitle = target.title,
            planLoading = planLoading,
            planError = planError,
            lessonPlan = lessonPlan,
            onGenerate = { viewModel.generateLessonPlan(target.id, level = null, goal = null) },
            onDismiss = { planTarget = null }
        )
    }
}

@Composable
fun SummaryCard(filteredCount: Int, filterType: Int) {
    val title = when(filterType) {
        0 -> "À venir"
        1 -> "Terminées"
        else -> "Reportées"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(OrangeGradientStart, OrangeGradientEnd)
                    )
                )
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday, 
                        contentDescription = "Résumé des sessions", 
                        tint = Color.White,
                        modifier = Modifier.size(40.dp).padding(end = 16.dp)
                    )
                    Column {
                        Text(title, color = Color.White.copy(alpha = 0.9f))
                        Text("$filteredCount sessions", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                if (filterType == 2) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Replanification", color = Color.White) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(text: String, background: Color, content: Color, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
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
private fun EmptySessionsState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Aucune session trouvée", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRefresh) { Text("Rafraîchir") }
    }
}

@Composable
fun CreateSessionDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, Int, String?, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var skill by remember { mutableStateOf("") }
    var studentEmail by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    var link by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onCreate(
                    title,
                    skill,
                    studentEmail,
                    date,
                    duration.toIntOrNull() ?: 60,
                    link.ifBlank { null },
                    notes.ifBlank { null }
                )
            }) { Text("Créer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Nouvelle session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") })
                OutlinedTextField(value = skill, onValueChange = { skill = it }, label = { Text("Compétence") })
                OutlinedTextField(value = studentEmail, onValueChange = { studentEmail = it }, label = { Text("Email élève") })
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (ISO)") })
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Durée (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("Lien de réunion (optionnel)") })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
            }
        }
    )
}

@Composable
fun FilterTab(text: String, index: Int, selectedIndex: Int, onClick: () -> Unit) {
    val isSelected = index == selectedIndex
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) OrangePrimary else Color.Transparent,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun SessionCard(
    session: Session,
    onPostpone: () -> Unit,
    onProposeReschedule: () -> Unit,
    onRespondReschedule: (Boolean) -> Unit,
    onRate: () -> Unit,
    onOpenPlan: () -> Unit
) {
    val context = LocalContext.current
    val formattedDate = remember(session.date) { formatSessionDate(session.date) }
    val formattedTime = remember(session.date) { formatSessionTime(session.date) }
    val teacherInitial = session.teacher.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5C52BF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (session.teacher.avatarUrl?.isNotBlank() == true) {
                        AsyncImage(
                            model = session.teacher.avatarUrl,
                            contentDescription = "Avatar de ${session.teacher.username}",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(teacherInitial, fontWeight = FontWeight.Bold, color = Color(0xFF5C52BF))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(session.teacher.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(session.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(formattedDate, style = MaterialTheme.typography.bodyMedium)
                    Text(formattedTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${session.duration} min", style = MaterialTheme.typography.bodyMedium)
                    StatusChip(session.status)
                }
            }

            session.rescheduleRequest?.let { req ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "Proposé: ${req.proposedDate ?: ""} ${req.proposedTime ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                req.note?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
                if (req.isActive == true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onRespondReschedule(true) }) {
                            Text("Accepter")
                        }
                        OutlinedButton(onClick = { onRespondReschedule(false) }) {
                            Text("Refuser")
                        }
                    }
                }
            }

            session.meetingLink?.takeIf { it.isNotBlank() }?.let { link ->
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = { openMeetingLink(context, link) },
                    label = { Text("Lien de réunion", color = OrangePrimary) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFFFF5E5),
                        labelColor = OrangePrimary
                    )
                )
            }

            Spacer(Modifier.height(12.dp))
            if (session.meetingLink?.isNotBlank() == true && session.status == "upcoming") {
                Button(
                    onClick = { openMeetingLink(context, session.meetingLink) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Rejoindre", color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onProposeReschedule, modifier = Modifier.weight(1f)) {
                    Text("Proposer")
                }
                OutlinedButton(onClick = onPostpone, modifier = Modifier.weight(1f)) {
                    Text("Reporter")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onRate, modifier = Modifier.weight(1f)) {
                    Text("Noter")
                }
                OutlinedButton(onClick = onOpenPlan, modifier = Modifier.weight(1f)) {
                    Text("Plan de cours")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (label, color) = when (status) {
        "upcoming" -> "À venir" to Color(0xFF12947D)
        "completed" -> "Terminée" to Color(0xFF34C759)
        "reportee", "postponed" -> "Reportée" to Color(0xFFF28F24)
        else -> status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } to MaterialTheme.colorScheme.onSurfaceVariant
    }
    AssistChip(
        onClick = {},
        label = { Text(label, color = color) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = color
        )
    )
}

private fun openMeetingLink(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}

@Composable
fun RescheduleDialog(
    date: String,
    time: String,
    note: String,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text("Envoyer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Proposer un créneau") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = onDateChange,
                    label = { Text("Date (ISO)") }
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = onTimeChange,
                    label = { Text("Heure (HH:mm)") }
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    label = { Text("Note (optionnel)") }
                )
            }
        }
    )
}

@Composable
fun RatingDialog(
    rating: Float,
    comment: String,
    onRatingChange: (Float) -> Unit,
    onCommentChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text("Envoyer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Noter la session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Note: ${rating.toInt()}/5")
                Slider(
                    value = rating,
                    onValueChange = onRatingChange,
                    valueRange = 1f..5f,
                    steps = 3
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    label = { Text("Commentaire (optionnel)") }
                )
            }
        }
    )
}

@Composable
fun LessonPlanDialog(
    sessionTitle: String,
    planLoading: Boolean,
    planError: String?,
    lessonPlan: com.skillswap.model.LessonPlan?,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onGenerate) {
                Text(if (lessonPlan == null) "Générer" else "Régénérer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
        title = { Text("Plan de cours - $sessionTitle") },
        text = {
            when {
                planLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Chargement...")
                    }
                }
                planError != null -> Text(planError, color = Color.Red)
                lessonPlan != null -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(lessonPlan.plan)
                        Text("Objectif: ${lessonPlan.goal}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Ressources: ${lessonPlan.resources.joinToString()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                else -> Text("Aucun plan disponible")
            }
        }
    )
}

private fun formatSessionDate(raw: String): String {
    return try {
        val odt = OffsetDateTime.parse(raw)
        odt.toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    } catch (_: DateTimeParseException) {
        raw
    }
}

private fun formatSessionTime(raw: String): String {
    return try {
        val odt = OffsetDateTime.parse(raw)
        odt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: DateTimeParseException) {
        ""
    }
}
