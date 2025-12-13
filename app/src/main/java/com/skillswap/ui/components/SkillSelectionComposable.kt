package com.skillswap.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.skillswap.domain.skills.Skill
import com.skillswap.domain.skills.SkillCategory
import com.skillswap.domain.skills.PredefinedSkills
import com.skillswap.ui.theme.OrangePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillSelectionComposable(
    selectedSkills: List<String>,
    onSkillsChanged: (List<String>) -> Unit,
    title: String = "Sélectionner des compétences",
    placeholder: String = "Rechercher ou ajouter une compétence...",
    modifier: Modifier = Modifier,
    maxSelections: Int? = null,
    allowCustomSkills: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<SkillCategory?>(null) }
    var showAddSkillDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    
    // Debounced search
    val scope = rememberCoroutineScope()
    var debouncedQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(searchQuery) {
        scope.launch {
            delay(300) // Debounce delay
            debouncedQuery = searchQuery
        }
    }
    
    // Filter skills based on search and category
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
    
    // Check if max selections reached
    val canAddMore = maxSelections == null || selectedSkills.size < maxSelections
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .semantics { contentDescription = title }
    ) {
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Selected skills chips
        if (selectedSkills.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .semantics { contentDescription = "Selected skills: ${selectedSkills.joinToString()}" }
            ) {
                items(selectedSkills) { skillName ->
                    SelectedSkillChip(
                        skillName = skillName,
                        onRemove = {
                            onSkillsChanged(selectedSkills - skillName)
                            errorMessage = null
                        }
                    )
                }
            }
        }
        
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search icon")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Search skills field" },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )
        
        // Error message
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        
        // Max selections warning
        if (!canAddMore) {
            Text(
                text = "Maximum de $maxSelections compétences atteint",
                color = Color(0xFFFF6B35),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Category filter
        CategoryFilter(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Skills grid
        if (filteredSkills.isEmpty() && searchQuery.isNotEmpty()) {
            // No results - offer to add custom skill
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aucune compétence trouvée",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
                if (allowCustomSkills && canAddMore) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showAddSkillDialog = true },
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
                columns = GridCells.Adaptive(minSize = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(filteredSkills) { skill ->
                    SkillCard(
                        skill = skill,
                        isSelected = selectedSkills.contains(skill.name),
                        onClick = {
                            if (selectedSkills.contains(skill.name)) {
                                onSkillsChanged(selectedSkills - skill.name)
                                errorMessage = null
                            } else if (canAddMore) {
                                onSkillsChanged(selectedSkills + skill.name)
                                errorMessage = null
                            } else {
                                errorMessage = "Maximum atteint"
                            }
                        },
                        enabled = canAddMore || selectedSkills.contains(skill.name)
                    )
                }
            }
        }
        
        // Add custom skill button
        if (allowCustomSkills && canAddMore && filteredSkills.isNotEmpty()) {
            TextButton(
                onClick = { showAddSkillDialog = true },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter une compétence personnalisée")
            }
        }
    }
    
    // Add custom skill dialog
    if (showAddSkillDialog) {
        AddCustomSkillDialog(
            initialName = searchQuery,
            existingSkills = selectedSkills + PredefinedSkills.POPULAR_SKILLS.map { it.name },
            onDismiss = { showAddSkillDialog = false },
            onAdd = { skillName ->
                if (!selectedSkills.contains(skillName)) {
                    onSkillsChanged(selectedSkills + skillName)
                    searchQuery = ""
                    errorMessage = null
                }
                showAddSkillDialog = false
            }
        )
    }
}

@Composable
private fun SelectedSkillChip(
    skillName: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = OrangePrimary,
        modifier = Modifier.semantics { contentDescription = "Selected skill: $skillName, double tap to remove" }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Text(
                text = skillName,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove $skillName",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryFilter(
    selectedCategory: SkillCategory?,
    onCategorySelected: (SkillCategory?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("Tous") }
            )
        }
        items(SkillCategory.values()) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(getCategoryLabel(category)) }
            )
        }
    }
}

@Composable
private fun SkillCard(
    skill: Skill,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = "${skill.name}, ${if (isSelected) "selected" else "not selected"}" },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) OrangePrimary.copy(alpha = 0.1f) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, OrangePrimary) 
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (enabled) Color.Black else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = OrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            skill.description?.let {
                Spacer(modifier = Modifier.height(4.dp))
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

@Composable
private fun AddCustomSkillDialog(
    initialName: String,
    existingSkills: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var skillName by remember { mutableStateOf(initialName) }
    var skillDescription by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Ajouter une compétence",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = skillDescription,
                    onValueChange = { skillDescription = it },
                    label = { Text("Description (optionnelle)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                }
            }
        }
    }
}

private fun getCategoryLabel(category: SkillCategory): String {
    return when (category) {
        SkillCategory.TECHNOLOGY -> "Technologie"
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
