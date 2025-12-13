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
import com.skillswap.model.SessionUserSummary
import com.skillswap.ui.theme.OrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingScreen(
    sessionId: String,
    ratedUser: SessionUserSummary,
    skill: String,
    onBack: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var selectedRating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laisser un avis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // User info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(OrangePrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            ratedUser.username.take(2).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            ratedUser.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            skill,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Star rating
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                (1..5).forEach { index ->
                    Icon(
                        if (index <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star $index",
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { selectedRating = index },
                        tint = if (index <= selectedRating) Color(0xFFFFD700) else Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                when (selectedRating) {
                    1 -> "Très mauvais"
                    2 -> "Mauvais"
                    3 -> "Moyen"
                    4 -> "Bien"
                    5 -> "Excellent"
                    else -> "Sélectionnez une note"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selectedRating >= 3) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Comment section
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Votre commentaire",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Partagez votre expérience...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (selectedRating > 0) {
                        onSubmit(selectedRating, comment)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                enabled = selectedRating > 0
            ) {
                Text("Envoyer l'avis", modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
