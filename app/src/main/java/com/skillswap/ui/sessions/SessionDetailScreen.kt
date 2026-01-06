package com.skillswap.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.model.Session
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.SessionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    onOpenLessonPlan: (String) -> Unit,
    onRate: (Session) -> Unit,
    viewModel: SessionsViewModel = viewModel()
) {
    val session by viewModel.selectedSession.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showCancelDialog by remember { mutableStateOf(false) }
    var showRescheduleDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(sessionId) {
        viewModel.loadSessionDetail(sessionId)
    }
    
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Annuler la session") },
            text = { Text("Êtes-vous sûr de vouloir annuler cette session ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelSession(sessionId)
                        showCancelDialog = false
                    }
                ) {
                    Text("Annuler", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Retour")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails de la session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, "Partager")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else {
            session?.let { sess ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    sess.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusBadge(sess.status)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    sess.skill,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    formatDate(sess.date),
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${sess.duration} minutes",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            sess.meetingLink?.let { link ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { /* Open meeting link */ },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Rejoindre la réunion")
                                }
                            }
                        }
                    }
                    
                    // Participants Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Participants",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            sess.teacher?.let { teacher ->
                                ParticipantRow(
                                    name = teacher.username,
                                    role = "Enseignant",
                                    imageUrl = teacher.avatarUrl
                                )
                            }
                            
                            sess.student?.let { student ->
                                Spacer(modifier = Modifier.height(8.dp))
                                ParticipantRow(
                                    name = student.username,
                                    role = "Étudiant",
                                    imageUrl = student.avatarUrl
                                )
                            }
                        }
                    }
                    
                    // Lesson Plan Card with AI Generator
                    var showAIPlanGenerator by remember { mutableStateOf(false) }
                    
                    if (sess.status == "confirmed" || sess.status == "completed") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = OrangePrimary.copy(alpha = 0.1f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            "Plan de cours IA",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    IconButton(onClick = { showAIPlanGenerator = true }) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Générer",
                                            tint = OrangePrimary
                                        )
                                    }
                                }
                                
                                if (sess.title.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Plan de cours à venir",
                                        fontSize = 14.sp,
                                        color = Color.DarkGray,
                                        maxLines = 3
                                    )
                                    TextButton(
                                        onClick = { onOpenLessonPlan(sessionId) }
                                    ) {
                                        Text("Voir le plan complet", color = OrangePrimary)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Générez un plan de cours avec l'IA",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                        
                        if (showAIPlanGenerator) {
                            AIPlanGeneratorBottomSheet(
                                skill = sess.skill ?: "",
                                duration = sess.duration,
                                onPlanGenerated = { plan ->
                                    // Update session with generated plan
                                    // viewModel.updateSessionNotes(sessionId, plan)
                                },
                                onDismiss = { showAIPlanGenerator = false }
                            )
                        }
                    }
                    
                    // Actions Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Actions",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            when (sess.status) {
                                "pending" -> {
                                    Button(
                                        onClick = { viewModel.confirmSession(sessionId) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                    ) {
                                        Text("Confirmer la session")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { showCancelDialog = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Annuler", color = Color.Red)
                                    }
                                }
                                "confirmed" -> {
                                    OutlinedButton(
                                        onClick = { showRescheduleDialog = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Replanifier")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { showCancelDialog = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Annuler", color = Color.Red)
                                    }
                                }
                                "completed" -> {
                                    Button(
                                        onClick = { onRate(sess) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Laisser un avis")
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ParticipantRow(name: String, role: String, imageUrl: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OrangePrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.take(2).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(role, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "pending" -> Color(0xFFFFA500) to "En attente"
        "confirmed" -> Color(0xFF2196F3) to "Confirmée"
        "completed" -> Color(0xFF4CAF50) to "Terminée"
        "cancelled" -> Color(0xFFF44336) to "Annulée"
        else -> Color.Gray to status
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        val outputFormat = SimpleDateFormat("dd MMMM yyyy 'à' HH:mm", Locale.FRENCH)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}
