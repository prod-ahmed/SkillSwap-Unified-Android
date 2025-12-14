package com.skillswap.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.viewmodel.ProfileSetupViewModel
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.Color
import com.skillswap.ui.components.SkillSelectionComposable

@Composable
fun ProfileSetupScreen(onDone: () -> Unit, viewModel: ProfileSetupViewModel = viewModel()) {
    val state = viewModel.state
    var skillsTeach by remember { mutableStateOf<List<String>>(emptyList()) }
    var skillsLearn by remember { mutableStateOf<List<String>>(emptyList()) }
    var city by remember { mutableStateOf("") }
    var marketingOptIn by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Complétez votre profil", fontWeight = FontWeight.Bold)
        Text("Aidez-nous à personnaliser vos suggestions en précisant ce que vous enseignez, ce que vous apprenez et où vous êtes basé.")
        
        SkillSelectionComposable(
            selectedSkills = skillsTeach,
            onSkillsChanged = { skillsTeach = it },
            title = "Compétences à enseigner",
            placeholder = "Rechercher des compétences à enseigner..."
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SkillSelectionComposable(
            selectedSkills = skillsLearn,
            onSkillsChanged = { skillsLearn = it },
            title = "Compétences à apprendre",
            placeholder = "Rechercher des compétences à apprendre..."
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Ville") }, modifier = Modifier.fillMaxWidth())

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F7))) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Communications", fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Offres et mises à jour")
                        Text("Recevoir des infos marketing", color = Color.Gray)
                    }
                    Switch(
                        checked = marketingOptIn,
                        onCheckedChange = { marketingOptIn = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFF6B35))
                    )
                }
            }
        }

        Button(
            onClick = {
                if (skillsTeach.isEmpty() || skillsLearn.isEmpty() || city.isBlank()) {
                    formError = "Merci de remplir toutes les informations obligatoires."
                } else {
                    formError = null
                    viewModel.completeProfile(
                        skillsTeach.joinToString(","), 
                        skillsLearn.joinToString(","), 
                        city, 
                        marketingOptIn, 
                        onDone
                    )
                }
            },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuer")
        }

        state.error?.let { Text(it, color = androidx.compose.ui.graphics.Color.Red) }
        formError?.let { Text(it, color = Color.Red) }
    }
}
