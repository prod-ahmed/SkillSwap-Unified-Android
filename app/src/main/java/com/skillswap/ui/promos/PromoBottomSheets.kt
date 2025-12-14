package com.skillswap.ui.promos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.model.Promo
import com.skillswap.ui.components.*
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.PromosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePromoBottomSheet(
    onDismiss: () -> Unit,
    onPromoCreated: () -> Unit = {},
    viewModel: PromosViewModel = viewModel()
) {
    val sheetState = rememberStandardBottomSheetState()
    val context = LocalContext.current
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("") }
    var promoCode by remember { mutableStateOf("") }
    var validUntil by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }
    
    BottomSheetContainer(
        sheetState = sheetState,
        onDismiss = onDismiss,
        title = "Créer une promotion",
        subtitle = "Offrez une réduction à vos clients"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BottomSheetTextField(
                value = title,
                onValueChange = { title = it },
                label = "Titre",
                placeholder = "Ex: Promo Black Friday",
                leadingIcon = { Icon(Icons.Default.LocalOffer, null) }
            )
            
            BottomSheetTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                placeholder = "Décrivez votre promotion...",
                singleLine = false,
                maxLines = 3,
                leadingIcon = { Icon(Icons.Default.Description, null) }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BottomSheetTextField(
                    value = discount,
                    onValueChange = { if (it.all { c -> c.isDigit() }) discount = it },
                    label = "Réduction (%)",
                    placeholder = "20",
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Percent, null) }
                )
                
                BottomSheetTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it.uppercase() },
                    label = "Code promo",
                    placeholder = "PROMO20",
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Code, null) }
                )
            }
            
            BottomSheetTextField(
                value = validUntil,
                onValueChange = { validUntil = it },
                label = "Valable jusqu'au (YYYY-MM-DD)",
                placeholder = "2025-12-31",
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) }
            )
            
            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(if (selectedImageUri != null) Icons.Default.CheckCircle else Icons.Default.Image, null)
                Spacer(Modifier.width(8.dp))
                Text(if (selectedImageUri != null) "Image sélectionnée" else "Ajouter une image")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    isSubmitting = true
                    viewModel.createPromo(
                        title = title,
                        description = description,
                        discount = discount.toIntOrNull() ?: 0,
                        validTo = validUntil,
                        promoCode = promoCode.ifEmpty { null },
                        media = null
                    )
                    onPromoCreated()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && title.isNotBlank() && discount.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Créer la promotion")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPromoBottomSheet(
    promo: Promo,
    onDismiss: () -> Unit,
    onPromoUpdated: () -> Unit = {},
    viewModel: PromosViewModel = viewModel()
) {
    val sheetState = rememberStandardBottomSheetState()
    val context = LocalContext.current
    
    var title by remember { mutableStateOf(promo.title) }
    var description by remember { mutableStateOf(promo.description) }
    var discount by remember { mutableStateOf(promo.discount.toString()) }
    var promoCode by remember { mutableStateOf(promo.promoCode ?: "") }
    var validUntil by remember { mutableStateOf(promo.validUntil) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }
    
    BottomSheetContainer(
        sheetState = sheetState,
        onDismiss = onDismiss,
        title = "Modifier la promotion",
        subtitle = "Mettez à jour votre promotion"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BottomSheetTextField(
                value = title,
                onValueChange = { title = it },
                label = "Titre",
                leadingIcon = { Icon(Icons.Default.LocalOffer, null) }
            )
            
            BottomSheetTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                singleLine = false,
                maxLines = 3,
                leadingIcon = { Icon(Icons.Default.Description, null) }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BottomSheetTextField(
                    value = discount,
                    onValueChange = { if (it.all { c -> c.isDigit() }) discount = it },
                    label = "Réduction (%)",
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Percent, null) }
                )
                
                BottomSheetTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it.uppercase() },
                    label = "Code promo",
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Code, null) }
                )
            }
            
            BottomSheetTextField(
                value = validUntil,
                onValueChange = { validUntil = it },
                label = "Valable jusqu'au (YYYY-MM-DD)",
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) }
            )
            
            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(if (selectedImageUri != null) Icons.Default.CheckCircle else Icons.Default.Image, null)
                Spacer(Modifier.width(8.dp))
                Text(if (selectedImageUri != null) "Image sélectionnée" else "Changer l'image")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    isSubmitting = true
                    viewModel.updatePromo(
                        id = promo.id,
                        title = title,
                        description = description,
                        discount = discount.toIntOrNull() ?: 0,
                        validTo = validUntil,
                        promoCode = promoCode.ifEmpty { null },
                        media = null
                    )
                    onPromoUpdated()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Sauvegarder")
                }
            }
        }
    }
}
