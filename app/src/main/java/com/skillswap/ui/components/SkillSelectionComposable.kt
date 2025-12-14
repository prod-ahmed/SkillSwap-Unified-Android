package com.skillswap.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillswap.ui.theme.OrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillSelectionComposable(
    selectedSkills: List<String>,
    onSkillsChanged: (List<String>) -> Unit,
    title: String = "Sélectionner des compétences",
    placeholder: String = "Ajouter des compétences...",
    modifier: Modifier = Modifier,
    maxSelections: Int? = null,
    allowCustomSkills: Boolean = true
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    
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
            ) {
                items(selectedSkills) { skillName ->
                    SelectedSkillChip(
                        skillName = skillName,
                        onRemove = {
                            onSkillsChanged(selectedSkills - skillName)
                        }
                    )
                }
            }
        }
        
        // Add button (triggers bottom sheet)
        OutlinedButton(
            onClick = { showBottomSheet = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = OrangePrimary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (selectedSkills.isEmpty()) placeholder else "Modifier les compétences")
        }
        
        // Max selections warning
        if (maxSelections != null && selectedSkills.size >= maxSelections) {
            Text(
                text = "Maximum de $maxSelections compétences atteint",
                color = Color(0xFFFF6B35),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
    
    if (showBottomSheet) {
        SkillsPickerBottomSheet(
            selectedSkills = selectedSkills,
            onSkillsChanged = onSkillsChanged,
            onDismiss = { showBottomSheet = false },
            maxSelections = maxSelections,
            allowCustomSkills = allowCustomSkills,
            title = title
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
        modifier = Modifier.semantics { contentDescription = "Selected skill: $skillName" }
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
