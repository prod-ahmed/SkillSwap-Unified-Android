package com.skillswap.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.viewmodel.ProfileViewModel
import com.skillswap.ui.theme.OrangePrimary
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val cities by viewModel.cities.collectAsState()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var teachSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    var learnSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCitySuggestions by remember { mutableStateOf(false) }
    var showTeachSkillDialog by remember { mutableStateOf(false) }
    var showLearnSkillDialog by remember { mutableStateOf(false) }
    var newSkillText by remember { mutableStateOf("") }

    // Initialize from user data
    LaunchedEffect(user) {
        user?.let {
            username = it.username
            email = it.email
            location = it.location?.city ?: ""
            teachSkills = it.skillsTeach ?: emptyList()
            learnSkills = it.skillsLearn ?: emptyList()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
        viewModel.loadCities()
    }

    val filteredCities = remember(location, cities) {
        if (location.isEmpty()) cities
        else cities.filter { it.contains(location, ignoreCase = true) }.take(5)
    }

    val hasChanges = remember(user, username, location, teachSkills, learnSkills) {
        user?.let {
            username != it.username ||
            location != (it.location?.city ?: "") ||
            teachSkills != (it.skillsTeach ?: emptyList()) ||
            learnSkills != (it.skillsLearn ?: emptyList())
        } ?: false
    }

    val canSave = !username.isBlank() && hasChanges && !isSaving

    // Add Teach Skill Dialog
    if (showTeachSkillDialog) {
        AlertDialog(
            onDismissRequest = { 
                showTeachSkillDialog = false
                newSkillText = ""
            },
            title = { Text("Ajouter une compétence à enseigner") },
            text = {
                OutlinedTextField(
                    value = newSkillText,
                    onValueChange = { newSkillText = it },
                    label = { Text("Compétence") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newSkillText.isNotBlank() && !teachSkills.contains(newSkillText)) {
                            teachSkills = teachSkills + newSkillText
                        }
                        showTeachSkillDialog = false
                        newSkillText = ""
                    },
                    enabled = newSkillText.isNotBlank()
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showTeachSkillDialog = false
                    newSkillText = ""
                }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Add Learn Skill Dialog
    if (showLearnSkillDialog) {
        AlertDialog(
            onDismissRequest = { 
                showLearnSkillDialog = false
                newSkillText = ""
            },
            title = { Text("Ajouter une compétence à apprendre") },
            text = {
                OutlinedTextField(
                    value = newSkillText,
                    onValueChange = { newSkillText = it },
                    label = { Text("Compétence") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newSkillText.isNotBlank() && !learnSkills.contains(newSkillText)) {
                            learnSkills = learnSkills + newSkillText
                        }
                        showLearnSkillDialog = false
                        newSkillText = ""
                    },
                    enabled = newSkillText.isNotBlank()
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showLearnSkillDialog = false
                    newSkillText = ""
                }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier le profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.updateProfile(
                                username = username,
                                location = location.ifBlank { null },
                                skillsTeach = teachSkills,
                                skillsLearn = learnSkills
                            )
                        },
                        enabled = canSave
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Sauvegarder",
                                color = if (canSave) OrangePrimary else Color.Gray
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Personal Information Section
                Text(
                    "Informations personnelles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nom d'utilisateur") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location Section
                Text(
                    "Localisation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )

                Column {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { 
                            location = it
                            showCitySuggestions = it.isNotEmpty()
                        },
                        label = { Text("Ville") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (showCitySuggestions && filteredCities.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            LazyColumn {
                                items(filteredCities) { city ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                location = city
                                                showCitySuggestions = false
                                            }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(city)
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (city != filteredCities.last()) {
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Skills Section
                Text(
                    "Compétences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )

                // Teach Skills
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Je peux enseigner",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        IconButton(onClick = { showTeachSkillDialog = true }) {
                            Icon(Icons.Default.Add, "Ajouter", tint = OrangePrimary)
                        }
                    }

                    SkillChipsDisplay(
                        skills = teachSkills,
                        onRemove = { skill -> teachSkills = teachSkills - skill },
                        chipColor = OrangePrimary
                    )
                }

                // Learn Skills
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Je veux apprendre",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        IconButton(onClick = { showLearnSkillDialog = true }) {
                            Icon(Icons.Default.Add, "Ajouter", tint = Color(0xFF26A69A))
                        }
                    }

                    SkillChipsDisplay(
                        skills = learnSkills,
                        onRemove = { skill -> learnSkills = learnSkills - skill },
                        chipColor = Color(0xFF26A69A)
                    )
                }

                // Messages
                errorMessage?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                successMessage?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        )
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                LaunchedEffect(successMessage) {
                    if (successMessage != null) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.clearMessages()
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Chargement...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SkillChipsDisplay(
    skills: List<String>,
    onRemove: (String) -> Unit,
    chipColor: Color
) {
    if (skills.isEmpty()) {
        Text(
            "Aucune compétence ajoutée",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skills.chunked(3).forEach { rowSkills ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSkills.forEach { skill ->
                            InputChip(
                                selected = false,
                                onClick = { },
                                label = { Text(skill) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Supprimer",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onRemove(skill) }
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = chipColor.copy(alpha = 0.1f),
                                    labelColor = chipColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
