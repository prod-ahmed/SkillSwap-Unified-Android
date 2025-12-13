package com.skillswap.ui.annonces

import androidx.compose.runtime.*
import com.skillswap.model.Annonce

@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.runtime.Composable
fun EditAnnonceScreen(
    annonce: Annonce,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var title by remember { mutableStateOf(annonce.title) }
    var description by remember { mutableStateOf(annonce.description ?: "") }
    
    CreateAnnonceScreen(
        onBack = onBack,
        onAnnonceCreated = onSave
    )
}
