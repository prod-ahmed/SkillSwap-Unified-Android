package com.skillswap.ui.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skillswap.ui.components.*
import com.skillswap.ui.theme.OrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleBottomSheet(
    date: String,
    time: String,
    note: String,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberStandardBottomSheetState()

    BottomSheetContainer(
        sheetState = sheetState,
        onDismiss = onDismiss,
        title = "Proposer un créneau",
        subtitle = "Suggérez une nouvelle date pour la session"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BottomSheetTextField(
                value = date,
                onValueChange = onDateChange,
                label = "Date (YYYY-MM-DD)",
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) }
            )
            
            BottomSheetTextField(
                value = time,
                onValueChange = onTimeChange,
                label = "Heure (HH:mm)",
                leadingIcon = { Icon(Icons.Default.Schedule, null) }
            )
            
            BottomSheetTextField(
                value = note,
                onValueChange = onNoteChange,
                label = "Note (optionnel)",
                placeholder = "Raison du report...",
                singleLine = false,
                maxLines = 3,
                leadingIcon = { Icon(Icons.Default.Note, null) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text("Envoyer la proposition")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingBottomSheet(
    rating: Float,
    comment: String,
    onRatingChange: (Float) -> Unit,
    onCommentChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberStandardBottomSheetState()

    BottomSheetContainer(
        sheetState = sheetState,
        onDismiss = onDismiss,
        title = "Noter la session",
        subtitle = "Partagez votre expérience"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Star Rating
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${rating.toInt()}/5",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = OrangePrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = rating,
                    onValueChange = onRatingChange,
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = SliderDefaults.colors(
                        thumbColor = OrangePrimary,
                        activeTrackColor = OrangePrimary
                    )
                )
            }
            
            BottomSheetTextField(
                value = comment,
                onValueChange = onCommentChange,
                label = "Commentaire (optionnel)",
                placeholder = "Qu'avez-vous pensé de cette session ?",
                singleLine = false,
                maxLines = 4,
                leadingIcon = { Icon(Icons.Default.Comment, null) }
            )
            
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text("Envoyer l'évaluation")
            }
        }
    }
}
