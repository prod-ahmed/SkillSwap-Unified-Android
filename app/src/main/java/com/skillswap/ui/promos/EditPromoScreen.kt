package com.skillswap.ui.promos

import androidx.compose.runtime.*
import com.skillswap.model.Promo

@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.runtime.Composable
fun EditPromoScreen(
    promo: Promo,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var title by remember { mutableStateOf(promo.title) }
    var description by remember { mutableStateOf(promo.description ?: "") }
    
    CreatePromoScreen(
        onBack = onBack,
        onPromoCreated = onSave
    )
}
