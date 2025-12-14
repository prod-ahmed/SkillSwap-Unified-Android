package com.skillswap.ui.sessions

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.SessionsViewModel
import com.skillswap.ui.components.*
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionBottomSheet(
    onDismiss: () -> Unit,
    onSessionCreated: () -> Unit,
    viewModel: SessionsViewModel = viewModel()
) {
    val sheetState = rememberStandardBottomSheetState()
    var currentStep by remember { mutableIntStateOf(1) }
    
    // Step 1: Session details
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Step 2: Date & Time
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var duration by remember { mutableIntStateOf(60) }
    var sessionMode by remember { mutableIntStateOf(0) } // 0 = Online, 1 = In-person
    var meetingLink by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    
    // Step 3: Participants
    var studentEmails by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // UI states
    var showSkillsPicker by remember { mutableStateOf(false) }
    var showAIPlanGenerator by remember { mutableStateOf(false) }
    var generatedPlan by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val scope = rememberCoroutineScope()
    
    val stepTitles = listOf("Détails", "Planning", "Participants")
    
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            onSessionCreated()
            onDismiss()
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    IconButton(onClick = { currentStep-- }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                } else {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }
                
                Text(
                    text = "Créer une session",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "$currentStep/3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OrangePrimary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            // Progress indicator
            LinearProgressIndicator(
                progress = { currentStep / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                color = OrangePrimary,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
            
            Text(
                text = stepTitles[currentStep - 1],
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Content area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (currentStep) {
                    1 -> Step1Content(
                        title = title,
                        onTitleChange = { title = it },
                        description = description,
                        onDescriptionChange = { description = it },
                        selectedSkills = selectedSkills,
                        onShowSkillsPicker = { showSkillsPicker = true }
                    )
                    2 -> Step2Content(
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        selectedTime = selectedTime,
                        onTimeSelected = { selectedTime = it },
                        duration = duration,
                        onDurationSelected = { duration = it },
                        sessionMode = sessionMode,
                        onSessionModeChange = { sessionMode = it },
                        meetingLink = meetingLink,
                        onMeetingLinkChange = { meetingLink = it },
                        location = location,
                        onLocationChange = { location = it }
                    )
                    3 -> Step3Content(
                        studentEmails = studentEmails,
                        onStudentEmailsChange = { studentEmails = it },
                        notes = notes,
                        onNotesChange = { notes = it },
                        onShowAIPlanGenerator = { showAIPlanGenerator = true },
                        generatedPlan = generatedPlan
                    )
                }
                
                formError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ $it",
                            color = Color.Red,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep < 3) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("Annuler")
                    }
                    
                    Button(
                        onClick = {
                            formError = when (currentStep) {
                                1 -> if (title.isBlank()) "Le titre est requis" 
                                     else if (selectedSkills.isEmpty()) "Sélectionnez au moins une compétence"
                                     else null
                                2 -> if (selectedDate == null) "Sélectionnez une date"
                                     else if (selectedTime == null) "Sélectionnez une heure"
                                     else if (sessionMode == 0 && meetingLink.isBlank()) "Le lien de réunion est requis"
                                     else if (sessionMode == 1 && location.isBlank()) "L'adresse est requise"
                                     else null
                                else -> null
                            }
                            if (formError == null) currentStep++
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Text("Suivant")
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                    }
                } else {
                    Button(
                        onClick = {
                            if (title.isBlank() || selectedDate == null || selectedTime == null) {
                                formError = "Veuillez remplir tous les champs obligatoires"
                                return@Button
                            }
                            
                            val dateTimeString = "${selectedDate}T${selectedTime}:00"
                            viewModel.createSession(
                                title = title,
                                skill = selectedSkills.firstOrNull() ?: "",
                                studentEmail = studentEmails.split(",").firstOrNull()?.trim() ?: "",
                                studentEmails = studentEmails.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                date = dateTimeString,
                                duration = duration,
                                meetingLink = if (sessionMode == 0) meetingLink else null,
                                location = if (sessionMode == 1) location else null,
                                notes = notes.ifBlank { null }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
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
    
    // Skills Picker Bottom Sheet
    if (showSkillsPicker) {
        SkillsPickerBottomSheet(
            selectedSkills = selectedSkills,
            onSkillsChanged = { selectedSkills = it },
            onDismiss = { showSkillsPicker = false },
            maxSelections = 3,
            title = "Compétences de la session"
        )
    }
    
    // AI Plan Generator
    if (showAIPlanGenerator) {
        AIPlanGeneratorBottomSheet(
            skill = selectedSkills.firstOrNull() ?: "",
            duration = duration,
            onPlanGenerated = { plan ->
                generatedPlan = plan
                notes = if (notes.isBlank()) plan else "$notes\n\nPlan suggéré:\n$plan"
            },
            onDismiss = { showAIPlanGenerator = false }
        )
    }
}

@Composable
private fun Step1Content(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    selectedSkills: List<String>,
    onShowSkillsPicker: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BottomSheetTextField(
            value = title,
            onValueChange = onTitleChange,
            label = "Titre de la session *",
            placeholder = "Ex: Cours de design UI/UX",
            leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) }
        )
        
        BottomSheetTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = "Description",
            placeholder = "Décrivez le contenu de la session...",
            singleLine = false,
            maxLines = 4,
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
        )
        
        BottomSheetSection(title = "Compétences *") {
            OutlinedButton(
                onClick = onShowSkillsPicker,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (selectedSkills.isEmpty()) "Sélectionner des compétences" 
                    else "${selectedSkills.size} sélectionnée(s)"
                )
            }
            
            if (selectedSkills.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedSkills.forEach { skill ->
                        AssistChip(
                            onClick = {},
                            label = { Text(skill) },
                            leadingIcon = {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OrangePrimary)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step2Content(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    selectedTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    duration: Int,
    onDurationSelected: (Int) -> Unit,
    sessionMode: Int,
    onSessionModeChange: (Int) -> Unit,
    meetingLink: String,
    onMeetingLinkChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DatePickerField(
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            label = "Date de la session *",
            minDate = LocalDate.now()
        )
        
        TimePickerField(
            selectedTime = selectedTime,
            onTimeSelected = onTimeSelected,
            label = "Heure de début *"
        )
        
        DurationPicker(
            selectedDuration = duration,
            onDurationSelected = onDurationSelected,
            label = "Durée",
            durationOptions = listOf(30, 60, 90, 120, 180)
        )
        
        BottomSheetSection(title = "Mode de session") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = sessionMode == 0,
                    onClick = { onSessionModeChange(0) },
                    label = { Text("En ligne") },
                    leadingIcon = if (sessionMode == 0) {
                        { Icon(Icons.Default.VideoCall, contentDescription = null) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangePrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                FilterChip(
                    selected = sessionMode == 1,
                    onClick = { onSessionModeChange(1) },
                    label = { Text("Présentiel") },
                    leadingIcon = if (sessionMode == 1) {
                        { Icon(Icons.Default.LocationOn, contentDescription = null) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangePrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        if (sessionMode == 0) {
            BottomSheetTextField(
                value = meetingLink,
                onValueChange = onMeetingLinkChange,
                label = "Lien de réunion *",
                placeholder = "https://meet.google.com/...",
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
            )
        } else {
            BottomSheetTextField(
                value = location,
                onValueChange = onLocationChange,
                label = "Adresse *",
                placeholder = "Ex: Tunis, Centre-ville",
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun Step3Content(
    studentEmails: String,
    onStudentEmailsChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onShowAIPlanGenerator: () -> Unit,
    generatedPlan: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BottomSheetTextField(
            value = studentEmails,
            onValueChange = onStudentEmailsChange,
            label = "Emails des participants",
            placeholder = "email1@example.com, email2@example.com",
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            supportingText = "Séparez plusieurs emails par des virgules"
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Notes et plan de cours",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onShowAIPlanGenerator) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Générer un plan avec IA",
                    tint = OrangePrimary
                )
            }
        }
        
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            placeholder = { Text("Ajoutez des notes ou un plan de cours...") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp),
            maxLines = 8,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                focusedLabelColor = OrangePrimary
            )
        )
        
        if (generatedPlan != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Plan généré par IA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        generatedPlan,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIPlanGeneratorBottomSheet(
    skill: String,
    duration: Int,
    onPlanGenerated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberStandardBottomSheetState()
    var isGenerating by remember { mutableStateOf(false) }
    var generatedPlan by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var level by remember { mutableStateOf("intermediate") }
    var goal by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    val levels = listOf("beginner" to "Débutant", "intermediate" to "Intermédiaire", "advanced" to "Avancé")
    
    BottomSheetContainer(
        sheetState = sheetState,
        onDismiss = onDismiss,
        title = "Générer un plan de cours IA",
        subtitle = "Créez un plan structuré avec l'IA"
    ) {
        BottomSheetSection(title = "Paramètres") {
            DropdownPickerField(
                selectedValue = level,
                onValueSelected = { level = it },
                options = levels.map { it.first },
                label = "Niveau",
                displayText = { lvl -> levels.find { it.first == lvl }?.second ?: lvl },
                leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            BottomSheetTextField(
                value = goal,
                onValueChange = { goal = it },
                label = "Objectif spécifique (optionnel)",
                placeholder = "Ex: Apprendre les bases de Figma",
                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (generatedPlan != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Plan de cours généré:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        generatedPlan ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            error = null
                            try {
                                val plan = com.skillswap.ai.CloudflareAIService.generateLessonPlan(
                                    skill = skill,
                                    duration = duration,
                                    level = level
                                )
                                generatedPlan = plan
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isGenerating
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Régénérer")
                }
                
                Button(
                    onClick = {
                        generatedPlan?.let { onPlanGenerated(it) }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Utiliser")
                }
            }
        } else {
            BottomSheetButton(
                text = "Générer le plan de cours",
                onClick = {
                    scope.launch {
                        isGenerating = true
                        error = null
                        try {
                            val plan = com.skillswap.ai.CloudflareAIService.generateLessonPlan(
                                skill = skill,
                                duration = duration,
                                level = level
                            )
                            generatedPlan = plan
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                isLoading = isGenerating
            )
        }
        
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚠️ $it",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
}
