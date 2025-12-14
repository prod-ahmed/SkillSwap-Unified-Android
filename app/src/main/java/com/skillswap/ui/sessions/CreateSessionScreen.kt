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
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.SessionsViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.skillswap.ui.components.SkillSelectionComposable
import com.skillswap.ui.components.LocationPickerScreen
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionScreen(
    onBack: () -> Unit,
    onSessionCreated: () -> Unit,
    viewModel: SessionsViewModel = viewModel()
) {
    var currentStep by remember { mutableStateOf(1) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    
    var selectedDate by remember { mutableStateOf(Date()) }
    var selectedTime by remember { mutableStateOf(Date()) }
    var duration by remember { mutableStateOf(60) }
    
    var locationName by remember { mutableStateOf("") }
    var locationLat by remember { mutableStateOf<Double?>(null) }
    var locationLng by remember { mutableStateOf<Double?>(null) }
    var showLocationPicker by remember { mutableStateOf(false) }
    
    var studentEmail by remember { mutableStateOf("") }
    var meetingLink by remember { mutableStateOf("") }
    
    val durationOptions = listOf(30, 60, 90, 120)
    val stepTitles = listOf("Session", "Planning", "Invitations")
    
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (currentStep > 1) {
                            currentStep--
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "SkillSwapTN",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Créer une session",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Organisez une session d'échange",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    stepTitles.forEachIndexed { index, stepTitle ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index + 1 <= currentStep) OrangePrimary
                                        else Color(0xFFF5F5F5)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${index + 1}",
                                    color = if (index + 1 <= currentStep) Color.White else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                stepTitle,
                                fontSize = 12.sp,
                                color = if (index + 1 <= currentStep) Color.Black else Color.Gray
                            )
                        }
                        
                        if (index < stepTitles.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(2.dp)
                                    .padding(horizontal = 6.dp)
                                    .background(
                                        if (index + 1 < currentStep) OrangePrimary
                                        else Color(0xFFE0E0E0)
                                    )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (currentStep) {
                    1 -> {
                        // Step 1: Session details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Titre de la session",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    placeholder = { Text("Ex: Initiation à Photoshop") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    "Description",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    placeholder = { Text("Décrivez le contenu...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    maxLines = 4
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                SkillSelectionComposable(
                                    selectedSkills = selectedSkills,
                                    onSkillsChanged = { selectedSkills = it },
                                    title = "Compétences de la session",
                                    placeholder = "Rechercher des compétences...",
                                    maxSelections = 3
                                )
                            }
                        }
                    }
                    
                    2 -> {
                        // Step 2: Planning
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Date de la session",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
                                Text(
                                    dateFormat.format(selectedDate),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                        .padding(16.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    "Heure de début",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val timeFormat = SimpleDateFormat("HH:mm", Locale.FRENCH)
                                Text(
                                    timeFormat.format(selectedTime),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                        .padding(16.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    "Durée (minutes)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    durationOptions.forEach { dur ->
                                        val isSelected = duration == dur
                                        Button(
                                            onClick = { duration = dur },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) OrangePrimary
                                                else Color(0xFFF5F5F5)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                "$dur min",
                                                color = if (isSelected) Color.White else Color.Black,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    "Localisation (optionnel)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedButton(
                                    onClick = { showLocationPicker = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (locationName.isNotBlank()) locationName else "Choisir une localisation"
                                    )
                                }
                            }
                        }
                    }
                    
                    3 -> {
                        // Step 3: Invitations with User Search
                        val searchResults by viewModel.searchResults.collectAsState()
                        val isSearching by viewModel.isSearching.collectAsState()
                        val availabilityStatus by viewModel.availabilityStatus.collectAsState()
                        var selectedUserId by remember { mutableStateOf<String?>(null) }
                        var searchQuery by remember { mutableStateOf("") }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Rechercher un participant",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { 
                                        searchQuery = it
                                        viewModel.searchUsers(it)
                                    },
                                    placeholder = { Text("Nom ou email...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        if (isSearching) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                )
                                
                                // Search Results
                                if (searchResults.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        searchResults.take(5).forEach { user ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        studentEmail = user.email
                                                        selectedUserId = user.id
                                                        searchQuery = user.username
                                                        viewModel.clearSearchResults()
                                                        // Check availability
                                                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                                        viewModel.checkAvailability(user.id, dateFormat.format(selectedDate))
                                                    }
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(OrangePrimary.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        user.username.take(1).uppercase(),
                                                        color = OrangePrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(user.username, fontWeight = FontWeight.Medium)
                                                    Text(user.email, fontSize = 12.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // Availability Status
                                availabilityStatus?.let { status ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (status.available) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (status.available) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (status.available) Color(0xFF4CAF50) else Color(0xFFF44336)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            status.message ?: if (status.available) "Disponible" else "Non disponible",
                                            color = if (status.available) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    "Ou entrer l'email directement",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = studentEmail,
                                    onValueChange = { studentEmail = it },
                                    placeholder = { Text("etudiant@example.com") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    "Lien de réunion (optionnel)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = meetingLink,
                                    onValueChange = { meetingLink = it },
                                    placeholder = { Text("https://meet.google.com/...") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep < 3) {
                    Button(
                        onClick = { currentStep++ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Text("Suivant")
                    }
                } else {
                    val isLoading by viewModel.isLoading.collectAsState()
                    val successMessage by viewModel.successMessage.collectAsState()
                    
                    LaunchedEffect(successMessage) {
                        if (successMessage != null) {
                            onSessionCreated()
                        }
                    }
                    
                    Button(
                        onClick = {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val calendar = Calendar.getInstance().apply {
                                time = selectedDate
                                set(Calendar.HOUR_OF_DAY, Calendar.getInstance().apply { time = selectedTime }.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, Calendar.getInstance().apply { time = selectedTime }.get(Calendar.MINUTE))
                            }
                            
                            viewModel.createSession(
                                title = title,
                                skill = selectedSkills.firstOrNull() ?: "",
                                studentEmail = studentEmail,
                                date = dateFormat.format(calendar.time),
                                duration = duration,
                                meetingLink = meetingLink.takeIf { it.isNotBlank() },
                                notes = description.takeIf { it.isNotBlank() },
                                addToCalendar = true
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        enabled = title.isNotBlank() && studentEmail.isNotBlank() && selectedSkills.isNotEmpty() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Créer la session")
                        }
                    }
                }
            }
        }
    }
    
    if (showLocationPicker) {
        LocationPickerScreen(
            onLocationSelected = { name, lat, lng ->
                locationName = name
                locationLat = lat
                locationLng = lng
                showLocationPicker = false
            },
            onBack = { showLocationPicker = false }
        )
    }
}
