package com.skillswap.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.CalendarViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: CalendarViewModel = viewModel()
) {
    val event by viewModel.selectedEvent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    LaunchedEffect(eventId) {
        viewModel.loadEventDetail(eventId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détail de l'événement") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, "Modifier")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, "Supprimer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangePrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OrangePrimary)
                    }
                }
                event == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Événement non trouvé", color = Color.Gray)
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Messages
                        errorMessage?.let {
                            Surface(
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(it, color = Color.Red, modifier = Modifier.padding(12.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        successMessage?.let {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(it, color = Color(0xFF2E7D32), modifier = Modifier.padding(12.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Event Details Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Title
                                Text(
                                    event!!.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Time
                                val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
                                val timeFormat = SimpleDateFormat("HH:mm", Locale.FRENCH)
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                                
                                val startDate = try {
                                    inputFormat.parse(event!!.startTime)
                                } catch (e: Exception) {
                                    null
                                }
                                val endDate = try {
                                    inputFormat.parse(event!!.endTime)
                                } catch (e: Exception) {
                                    null
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        null,
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        startDate?.let {
                                            Text(
                                                dateFormat.format(it).replaceFirstChar { c -> c.uppercase() },
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "${timeFormat.format(it)} - ${endDate?.let { e -> timeFormat.format(e) } ?: ""}",
                                                color = Color.Gray
                                            )
                                        } ?: Text(event!!.startTime)
                                    }
                                }
                                
                                // Location
                                event!!.location?.let { loc ->
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            null,
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(loc)
                                    }
                                }
                                
                                // Description
                                event!!.description?.let { desc ->
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Description", fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(desc, color = Color.Gray)
                                }
                                
                                // Google Sync Status
                                if (event!!.googleEventId != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Sync,
                                            null,
                                            tint = Color.Green,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Synchronisé avec Google Calendar",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Green
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Participants
                        event!!.participants?.let { participants ->
                            if (participants.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            "Participants (${participants.size})",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        participants.forEach { participant ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    null,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(participant.name ?: participant.email)
                                                    Text(
                                                        participant.email,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.Gray
                                                    )
                                                }
                                                Surface(
                                                    color = when (participant.status) {
                                                        "accepted" -> Color(0xFFE8F5E9)
                                                        "declined" -> Color(0xFFFFEBEE)
                                                        else -> Color(0xFFFFF3E0)
                                                    },
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        when (participant.status) {
                                                            "accepted" -> "Accepté"
                                                            "declined" -> "Refusé"
                                                            else -> "En attente"
                                                        },
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer l'événement ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEvent(eventId) {
                            showDeleteConfirm = false
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }
    
    // Edit Dialog
    if (showEditDialog && event != null) {
        EditEventDialog(
            currentTitle = event!!.title,
            currentDescription = event!!.description,
            currentLocation = event!!.location,
            onDismiss = { showEditDialog = false },
            onSave = { title, description, location ->
                viewModel.updateEvent(
                    eventId = eventId,
                    title = title,
                    description = description,
                    location = location
                ) {
                    showEditDialog = false
                }
            }
        )
    }
}

@Composable
private fun EditEventDialog(
    currentTitle: String,
    currentDescription: String?,
    currentLocation: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?) -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }
    var description by remember { mutableStateOf(currentDescription ?: "") }
    var location by remember { mutableStateOf(currentLocation ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier l'événement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lieu") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, description.ifBlank { null }, location.ifBlank { null })
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
