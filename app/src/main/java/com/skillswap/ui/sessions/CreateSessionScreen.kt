package com.skillswap.ui.sessions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.model.User
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.SessionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateSessionScreen(
    onBack: () -> Unit,
    onSessionCreated: () -> Unit,
    viewModel: SessionsViewModel = viewModel()
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSkills by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customSkillInput by remember { mutableStateOf("") }
    
    var selectedDate by remember { mutableStateOf(Date()) }
    var selectedTime by remember { mutableStateOf(Date()) }
    var duration by remember { mutableIntStateOf(60) }
    
    var sessionMode by remember { mutableIntStateOf(0) } // 0 = Online, 1 = In-person
    var meetingLink by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    
    var studentEmail by remember { mutableStateOf("") }
    var selectedMembers by remember { mutableStateOf<List<UserChip>>(emptyList()) }
    var formError by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val availableSkills = listOf("Design", "Développement", "Marketing", "Photoshop", "Musique", "Autre")
    val durationOptions = listOf(30, 60, 90, 120)
    val stepTitles = listOf("Session", "Planning", "Invitations")
    
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            onSessionCreated()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (currentStep > 1) currentStep-- else onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "Retour", tint = Color.Black)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "SkillSwapTN",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "Créer une session",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Organisez une session d'échange",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                stepTitles.forEachIndexed { index, stepTitle ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index + 1 <= currentStep) OrangePrimary else Color(0xFFF5F5F5)
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
                                    if (index + 1 < currentStep) OrangePrimary else Color(0xFFE0E0E0)
                                )
                        )
                    }
                }
            }
        }
        
        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (currentStep) {
                1 -> Step1Content(
                    title = title,
                    onTitleChange = { title = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    selectedSkills = selectedSkills,
                    onToggleSkill = { skill ->
                        selectedSkills = if (selectedSkills.contains(skill)) {
                            selectedSkills - skill
                        } else {
                            selectedSkills + skill
                        }
                    },
                    availableSkills = availableSkills,
                    customSkillInput = customSkillInput,
                    onCustomSkillInputChange = { customSkillInput = it },
                    onAddCustomSkill = {
                        val trimmed = customSkillInput.trim()
                        if (trimmed.isNotEmpty() && !selectedSkills.contains(trimmed)) {
                            selectedSkills = selectedSkills + trimmed
                            customSkillInput = ""
                        }
                    }
                )
                
                2 -> Step2Content(
                    selectedDate = selectedDate,
                    onDateChange = { selectedDate = it },
                    selectedTime = selectedTime,
                    onTimeChange = { selectedTime = it },
                    duration = duration,
                    onDurationChange = { duration = it },
                    durationOptions = durationOptions,
                    context = context
                )
                
                3 -> Step3Content(
                    sessionMode = sessionMode,
                    onSessionModeChange = { sessionMode = it },
                    meetingLink = meetingLink,
                    onMeetingLinkChange = { meetingLink = it },
                    location = location,
                    onLocationChange = { location = it },
                    studentEmail = studentEmail,
                    onStudentEmailChange = { 
                        studentEmail = it
                        if (it.length >= 2) viewModel.searchUsers(it)
                    },
                    selectedMembers = selectedMembers,
                    onRemoveMember = { member ->
                        selectedMembers = selectedMembers.filter { it.id != member.id }
                    },
                    onAddMember = { user ->
                        if (selectedMembers.none { it.id == user.id }) {
                            selectedMembers = selectedMembers + user
                            studentEmail = ""
                            viewModel.clearSearchResults()
                        }
                    },
                    searchResults = searchResults,
                    isSearching = isSearching
                )
            }
            
            // Form error
            formError?.let { error ->
                Text(
                    error,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
        
        // Bottom Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimary),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, OrangePrimary),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Retour", fontWeight = FontWeight.Bold)
                }
            }
            
            Button(
                onClick = {
                    formError = null
                    when {
                        currentStep == 1 && title.isBlank() -> {
                            formError = "Le titre est requis"
                        }
                        currentStep == 1 && selectedSkills.isEmpty() -> {
                            formError = "Sélectionnez au moins une compétence"
                        }
                        currentStep == 3 && selectedMembers.isEmpty() && studentEmail.isBlank() -> {
                            formError = "Ajoutez au moins un participant"
                        }
                        currentStep < 3 -> {
                            currentStep++
                        }
                        else -> {
                            // Submit
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val calendar = Calendar.getInstance().apply {
                                time = selectedDate
                                set(Calendar.HOUR_OF_DAY, Calendar.getInstance().apply { time = selectedTime }.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, Calendar.getInstance().apply { time = selectedTime }.get(Calendar.MINUTE))
                            }
                            
                            val participantEmail = selectedMembers.firstOrNull()?.email ?: studentEmail
                            
                            viewModel.createSession(
                                title = title,
                                skill = selectedSkills.joinToString(", "),
                                studentEmail = participantEmail,
                                date = dateFormat.format(calendar.time),
                                duration = duration,
                                meetingLink = if (sessionMode == 0) meetingLink.takeIf { it.isNotBlank() } else null,
                                notes = description.takeIf { it.isNotBlank() },
                                addToCalendar = true
                            )
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(18.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (currentStep < 3) "Suivant" else "Créer la session ✨",
                        fontWeight = FontWeight.Bold
                    )
                    if (currentStep < 3) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ============ Step 1: Session Details ============
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step1Content(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    selectedSkills: Set<String>,
    onToggleSkill: (String) -> Unit,
    availableSkills: List<String>,
    customSkillInput: String,
    onCustomSkillInputChange: (String) -> Unit,
    onAddCustomSkill: () -> Unit
) {
    SessionCard {
        SectionHeader("Titre de la session")
        StyledTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = "Ex: Initiation à Photoshop"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SectionHeader("Description")
        StyledTextField(
            value = description,
            onValueChange = onDescriptionChange,
            placeholder = "Décrivez le contenu de la session...",
            minHeight = 100.dp,
            singleLine = false
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SectionHeader("Compétences")
        
        // Skill chips grid
        val allSkills = (availableSkills + selectedSkills.filter { it !in availableSkills }).distinct()
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            allSkills.forEach { skill ->
                val isSelected = selectedSkills.contains(skill)
                Surface(
                    onClick = { onToggleSkill(skill) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) OrangePrimary else Color(0xFFF5F5F5)
                ) {
                    Text(
                        skill,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = if (isSelected) Color.White else Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        
        SectionHeader("Ajouter une compétence")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StyledTextField(
                    value = customSkillInput,
                    onValueChange = onCustomSkillInputChange,
                    placeholder = "Nouvelle compétence..."
                )
            }
            
            IconButton(
                onClick = onAddCustomSkill,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(OrangePrimary),
                enabled = customSkillInput.isNotBlank()
            ) {
                Icon(Icons.Default.Check, contentDescription = "Ajouter", tint = Color.White)
            }
        }
    }
}

// ============ Step 2: Planning ============
@Composable
private fun Step2Content(
    selectedDate: Date,
    onDateChange: (Date) -> Unit,
    selectedTime: Date,
    onTimeChange: (Date) -> Unit,
    duration: Int,
    onDurationChange: (Int) -> Unit,
    durationOptions: List<Int>,
    context: android.content.Context
) {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.FRENCH)
    
    SessionCard {
        SectionHeader("Date de la session")
        Surface(
            onClick = {
                calendar.time = selectedDate
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        onDateChange(calendar.time)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF5F5F5)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = OrangePrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(dateFormat.format(selectedDate), fontSize = 15.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        
        SectionHeader("Heure de début")
        Surface(
            onClick = {
                calendar.time = selectedTime
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        onTimeChange(calendar.time)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF5F5F5)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = OrangePrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(timeFormat.format(selectedTime), fontSize = 15.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        
        SectionHeader("Durée")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            durationOptions.forEach { dur ->
                val isSelected = duration == dur
                Surface(
                    onClick = { onDurationChange(dur) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) OrangePrimary else Color(0xFFF5F5F5)
                ) {
                    Text(
                        "${dur}min",
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                        color = if (isSelected) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

// ============ Step 3: Invitations & Mode ============
@Composable
private fun Step3Content(
    sessionMode: Int,
    onSessionModeChange: (Int) -> Unit,
    meetingLink: String,
    onMeetingLinkChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    studentEmail: String,
    onStudentEmailChange: (String) -> Unit,
    selectedMembers: List<UserChip>,
    onRemoveMember: (UserChip) -> Unit,
    onAddMember: (UserChip) -> Unit,
    searchResults: List<User>,
    isSearching: Boolean
) {
    // Members Card
    SessionCard {
        SectionHeader("Participants", subtitle = "Recherchez ou ajoutez des membres")
        
        // Selected members chips
        if (selectedMembers.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                selectedMembers.forEach { member ->
                    MemberChipView(
                        member = member,
                        onRemove = { onRemoveMember(member) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Email input with search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, OrangePrimary, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = studentEmail,
                onValueChange = onStudentEmailChange,
                placeholder = { Text("Rechercher par email ou nom...") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )
            
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else if (studentEmail.isNotBlank()) {
                IconButton(onClick = {
                    // Add email directly as member
                    if (studentEmail.contains("@")) {
                        onAddMember(UserChip(
                            id = UUID.randomUUID().toString(),
                            username = studentEmail.substringBefore("@"),
                            email = studentEmail
                        ))
                    }
                }) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Ajouter", tint = OrangePrimary)
                }
            }
        }
        
        // Search results suggestions
        if (searchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Column {
                    val displayResults = searchResults.take(5)
                    displayResults.forEachIndexed { index, user ->
                        SearchResultItem(
                            user = user,
                            onAddMember = onAddMember,
                            showDivider = index < displayResults.size - 1
                        )
                    }
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Session Mode Card
    SessionCard {
        SectionHeader("Mode de session")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeButton(
                title = "En ligne",
                icon = Icons.Default.Videocam,
                isSelected = sessionMode == 0,
                onClick = { onSessionModeChange(0) },
                modifier = Modifier.weight(1f)
            )
            ModeButton(
                title = "En personne",
                icon = Icons.Default.People,
                isSelected = sessionMode == 1,
                onClick = { onSessionModeChange(1) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        
        if (sessionMode == 0) {
            SectionHeader("Lien de réunion")
            StyledTextField(
                value = meetingLink,
                onValueChange = onMeetingLinkChange,
                placeholder = "https://meet.google.com/...",
                leadingIcon = Icons.Default.Link
            )
        } else {
            SectionHeader("Lieu de la session")
            StyledTextField(
                value = location,
                onValueChange = onLocationChange,
                placeholder = "Adresse ou lieu...",
                leadingIcon = Icons.Default.LocationOn
            )
        }
    }
}

// ============ Reusable Components ============

@Composable
private fun SessionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column {
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        subtitle?.let {
            Text(it, fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
    singleLine: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF5F5F5)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (minHeight > 0.dp) minHeight else 52.dp)
                .padding(horizontal = 16.dp, vertical = if (singleLine) 0.dp else 12.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
        ) {
            leadingIcon?.let {
                Icon(it, contentDescription = null, tint = OrangePrimary)
                Spacer(modifier = Modifier.width(10.dp))
            }
            
            if (singleLine) {
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(placeholder, color = Color.Gray, fontSize = 15.sp)
                            }
                            innerTextField()
                        }
                    }
                )
            } else {
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(placeholder, color = Color.Gray, fontSize = 15.sp)
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ModeButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) OrangePrimary else Color(0xFFF5F5F5)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else Color.Black,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun MemberChipView(member: UserChip, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = OrangePrimary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(OrangePrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    member.username.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(member.username, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Default.Close,
                contentDescription = "Supprimer",
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onRemove() },
                tint = Color.Gray
            )
        }
    }
}

// Data class for member chips
data class UserChip(
    val id: String,
    val username: String,
    val email: String
)

@Composable
private fun SearchResultItem(
    user: User,
    onAddMember: (UserChip) -> Unit,
    showDivider: Boolean
) {
    val safeUsername = user.username ?: "Inconnu"
    val safeEmail = user.email ?: ""
    val safeId = user.id ?: ""
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onAddMember(UserChip(safeId, safeUsername, safeEmail))
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OrangePrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    safeUsername.take(1).uppercase(),
                    color = OrangePrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(safeUsername, fontWeight = FontWeight.SemiBold)
                Text(safeEmail, fontSize = 12.sp, color = Color.Gray)
            }
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(start = 60.dp))
        }
    }
}
