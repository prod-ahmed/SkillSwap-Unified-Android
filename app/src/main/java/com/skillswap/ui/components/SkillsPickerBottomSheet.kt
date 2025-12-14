package com.skillswap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillswap.domain.skills.PredefinedSkills
import com.skillswap.domain.skills.Skill
import com.skillswap.domain.skills.SkillCategory
import com.skillswap.ui.theme.OrangePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsPickerBottomSheet(
    selectedSkills: List<String>,
    onSkillsChanged: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    maxSelections: Int? = null,
    allowCustomSkills: Boolean = true,
    title: String = "Sélectionner des compétences"
) {
    val sheetState = rememberStandardBottomSheetState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<SkillCategory?>(null) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var localSelectedSkills by remember { mutableStateOf(selectedSkills) }
    
    val scope = rememberCoroutineScope()
    var debouncedQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(searchQuery) {
        scope.launch {
            delay(300)
            debouncedQuery = searchQuery
        }
    }
    
    val filteredSkills = remember(debouncedQuery, selectedCategory) {
        PredefinedSkills.POPULAR_SKILLS
            .filter { skill ->
                val matchesSearch = debouncedQuery.isEmpty() || 
                    skill.name.contains(debouncedQuery, ignoreCase = true) ||
                    (skill.description?.contains(debouncedQuery, ignoreCase = true) ?: false)
                val matchesCategory = selectedCategory == null || skill.category == selectedCategory
                matchesSearch && matchesCategory
            }
            .sortedByDescending { it.popularity }
    }
    
    val canAddMore = maxSelections == null || localSelectedSkills.size < maxSelections
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }
            
            // Selected count
            if (localSelectedSkills.isNotEmpty()) {
                Text(
                    text = "${localSelectedSkills.size} compétence(s) sélectionnée(s)" + 
                           (maxSelections?.let { " / $it max" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher une compétence...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    focusedLabelColor = OrangePrimary
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Category chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Tous") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(SkillCategory.values().toList()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(getCategoryLabel(category)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Skills grid
            if (filteredSkills.isEmpty() && searchQuery.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucune compétence trouvée",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    
                    if (allowCustomSkills && canAddMore) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddCustomDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajouter \"$searchQuery\"")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredSkills) { skill ->
                        SkillPickerCard(
                            skill = skill,
                            isSelected = localSelectedSkills.contains(skill.name),
                            onClick = {
                                if (localSelectedSkills.contains(skill.name)) {
                                    localSelectedSkills = localSelectedSkills - skill.name
                                } else if (canAddMore) {
                                    localSelectedSkills = localSelectedSkills + skill.name
                                }
                            },
                            enabled = canAddMore || localSelectedSkills.contains(skill.name)
                        )
                    }
                }
            }
            
            // Add custom skill button
            if (allowCustomSkills && canAddMore && filteredSkills.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showAddCustomDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter une compétence personnalisée")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("Annuler")
                }
                
                Button(
                    onClick = {
                        onSkillsChanged(localSelectedSkills)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Valider (${localSelectedSkills.size})")
                }
            }
        }
    }
    
    // Add custom skill dialog
    if (showAddCustomDialog) {
        AddCustomSkillDialogContent(
            initialName = searchQuery,
            existingSkills = localSelectedSkills + PredefinedSkills.POPULAR_SKILLS.map { it.name },
            onDismiss = { showAddCustomDialog = false },
            onAdd = { skillName ->
                if (!localSelectedSkills.contains(skillName) && canAddMore) {
                    localSelectedSkills = localSelectedSkills + skillName
                    searchQuery = ""
                }
                showAddCustomDialog = false
            }
        )
    }
}

@Composable
private fun SkillPickerCard(
    skill: Skill,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) OrangePrimary.copy(alpha = 0.15f) else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, OrangePrimary) 
        else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = skill.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (enabled) Color.Black else Color.Gray,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Sélectionné",
                            tint = OrangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                skill.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCustomSkillDialogContent(
    initialName: String,
    existingSkills: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var skillName by remember { mutableStateOf(initialName) }
    var skillDescription by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter une compétence")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = skillName,
                    onValueChange = { 
                        skillName = it
                        error = null
                    },
                    label = { Text("Nom de la compétence *") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary
                    )
                )
                
                OutlinedTextField(
                    value = skillDescription,
                    onValueChange = { skillDescription = it },
                    label = { Text("Description (optionnelle)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        skillName.isBlank() -> {
                            error = "Le nom est requis"
                        }
                        existingSkills.any { it.equals(skillName, ignoreCase = true) } -> {
                            error = "Cette compétence existe déjà"
                        }
                        else -> {
                            onAdd(skillName.trim())
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

private fun getCategoryLabel(category: SkillCategory): String {
    return when (category) {
        SkillCategory.TECHNOLOGY -> "Tech"
        SkillCategory.DESIGN -> "Design"
        SkillCategory.BUSINESS -> "Business"
        SkillCategory.LANGUAGE -> "Langues"
        SkillCategory.ART -> "Art"
        SkillCategory.MUSIC -> "Musique"
        SkillCategory.SPORT -> "Sport"
        SkillCategory.COOKING -> "Cuisine"
        SkillCategory.CRAFT -> "Artisanat"
        SkillCategory.OTHER -> "Autre"
    }
}
