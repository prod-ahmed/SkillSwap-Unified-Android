package com.skillswap.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.model.CalendarEvent
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.CalendarViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onEventClick: (String) -> Unit,
    viewModel: CalendarViewModel = viewModel()
) {
    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isGoogleConnected by viewModel.isGoogleConnected.collectAsState()
    val syncInProgress by viewModel.syncInProgress.collectAsState()
    
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showGoogleSettings by remember { mutableStateOf(false) }
    
    LaunchedEffect(currentMonth) {
        viewModel.loadEventsForMonth(
            currentMonth.get(Calendar.YEAR),
            currentMonth.get(Calendar.MONTH)
        )
        viewModel.checkGoogleCalendarStatus()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendrier") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showGoogleSettings = true }) {
                        Icon(
                            Icons.Default.Sync,
                            "Google Calendar",
                            tint = if (isGoogleConnected) Color.Green else Color.Gray
                        )
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Créer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangePrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = OrangePrimary
            ) {
                Icon(Icons.Default.Add, "Nouvel événement", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF2F2F7))
        ) {
            // Messages
            errorMessage?.let {
                Surface(
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(it, color = Color.Red, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearMessages() }) {
                            Icon(Icons.Default.Close, null, tint = Color.Red)
                        }
                    }
                }
            }
            
            successMessage?.let {
                Surface(
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(it, color = Color(0xFF2E7D32), modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearMessages() }) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFF2E7D32))
                        }
                    }
                }
            }
            
            // Month Navigation
            MonthHeader(
                currentMonth = currentMonth,
                onPreviousMonth = {
                    currentMonth = (currentMonth.clone() as Calendar).apply {
                        add(Calendar.MONTH, -1)
                    }
                },
                onNextMonth = {
                    currentMonth = (currentMonth.clone() as Calendar).apply {
                        add(Calendar.MONTH, 1)
                    }
                }
            )
            
            // Calendar Grid
            CalendarGrid(
                currentMonth = currentMonth,
                events = events,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Events List
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else {
                val displayEvents = selectedDate?.let { date ->
                    viewModel.getEventsForDate(date)
                } ?: events
                
                if (displayEvents.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Event,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Aucun événement", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayEvents) { event ->
                            EventCard(event = event, onClick = { onEventClick(event.id) })
                        }
                    }
                }
            }
        }
    }
    
    // Create Event Dialog
    if (showCreateDialog) {
        CreateEventDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, desc, start, end, location ->
                viewModel.createEvent(
                    title = title,
                    description = desc,
                    startTime = start,
                    endTime = end,
                    location = location,
                    participants = null,
                    syncToGoogle = isGoogleConnected
                ) {
                    showCreateDialog = false
                }
            }
        )
    }
    
    // Google Settings Dialog
    if (showGoogleSettings) {
        GoogleCalendarSettingsDialog(
            isConnected = isGoogleConnected,
            syncInProgress = syncInProgress,
            onDismiss = { showGoogleSettings = false },
            onConnect = {
                viewModel.getGoogleAuthUrl { url ->
                    // Open URL in browser
                }
            },
            onSync = { viewModel.syncWithGoogle() },
            onDisconnect = { viewModel.disconnectGoogleCalendar() }
        )
    }
}

@Composable
private fun MonthHeader(
    currentMonth: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.FRENCH)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Mois précédent")
        }
        
        Text(
            monthFormat.format(currentMonth.time).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Mois suivant")
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: Calendar,
    events: List<CalendarEvent>,
    selectedDate: Date?,
    onDateSelected: (Date) -> Unit
) {
    val daysOfWeek = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
    val calendar = currentMonth.clone() as Calendar
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    
    val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        var dayCounter = 1
        for (week in 0..5) {
            if (dayCounter > daysInMonth) break
            
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in 0..6) {
                    if (week == 0 && dayOfWeek < firstDayOfWeek || dayCounter > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = (currentMonth.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, dayCounter)
                        }.time
                        
                        val hasEvents = events.any { event ->
                            event.startTime.startsWith(
                                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
                            )
                        }
                        
                        val isSelected = selectedDate?.let {
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it) ==
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
                        } ?: false
                        
                        val isToday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) ==
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
                        
                        DayCell(
                            day = dayCounter,
                            hasEvents = hasEvents,
                            isSelected = isSelected,
                            isToday = isToday,
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                        
                        dayCounter++
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    hasEvents: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> OrangePrimary
                    isToday -> OrangePrimary.copy(alpha = 0.2f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$day",
                color = when {
                    isSelected -> Color.White
                    else -> Color.Black
                },
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (hasEvents) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else OrangePrimary)
                )
            }
        }
    }
}

@Composable
private fun EventCard(
    event: CalendarEvent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(50.dp)
                    .background(OrangePrimary, RoundedCornerShape(2.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                val timeFormat = SimpleDateFormat("HH:mm", Locale.FRENCH)
                val startTime = try {
                    val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(event.startTime)
                    timeFormat.format(parsed!!)
                } catch (e: Exception) {
                    event.startTime.takeLast(5)
                }
                
                Text(startTime, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                event.location?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp), Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
            
            if (event.googleEventId != null) {
                Icon(Icons.Default.Sync, "Synced", tint = Color.Green, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun CreateEventDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?, String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel événement") },
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
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Début (YYYY-MM-DD HH:mm) *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("Fin (YYYY-MM-DD HH:mm) *") },
                    modifier = Modifier.fillMaxWidth()
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
                    if (title.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                        onCreate(title, description.ifBlank { null }, startTime, endTime, location.ifBlank { null })
                    }
                },
                enabled = title.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun GoogleCalendarSettingsDialog(
    isConnected: Boolean,
    syncInProgress: Boolean,
    onDismiss: () -> Unit,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onDisconnect: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Google Calendar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Sync, null,
                        tint = if (isConnected) Color.Green else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(if (isConnected) "Connecté" else "Non connecté")
                }
                
                if (isConnected) {
                    Text("Vos événements sont synchronisés.", color = Color.Gray)
                    if (syncInProgress) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Synchronisation...")
                        }
                    }
                } else {
                    Text("Connectez Google pour synchroniser.", color = Color.Gray)
                }
            }
        },
        confirmButton = {
            if (isConnected) {
                Row {
                    TextButton(onClick = onSync, enabled = !syncInProgress) { Text("Sync") }
                    TextButton(onClick = onDisconnect) { Text("Déconnecter", color = Color.Red) }
                }
            } else {
                Button(onClick = onConnect) { Text("Connecter") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}
