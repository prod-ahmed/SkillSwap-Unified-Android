package com.skillswap.ui.annonces

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
import com.skillswap.model.Annonce
import com.skillswap.ui.components.*
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.AnnoncesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAnnonceBottomSheet(
    onDismiss: () -> Unit,
    onAnnonceCreated: () -> Unit = {},
    viewModel: AnnoncesViewModel = viewModel()
) {
    val sheetState = rememberStandardBottomSheetState()
    val context = LocalContext.current
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val categories = listOf("Cours", "Formation", "Workshop", "Autre")
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }
    
    BottomSheetContainer(
        sheetState = sheetState,
        onDismiss = onDismiss,
        title = "Créer une annonce",
        subtitle = "Partagez votre savoir-faire"
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
                placeholder = "Ex: Cours de guitare débutant",
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) }
            )
            
            BottomSheetTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                placeholder = "Décrivez votre annonce...",
                singleLine = false,
                maxLines = 4,
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
            )
            
            // Category dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Catégorie") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
            
            BottomSheetTextField(
                value = city,
                onValueChange = { city = it },
                label = "Ville",
                placeholder = "Ex: Tunis",
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
            )
            
            // Image picker button
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
                    viewModel.createAnnonce(
                        title = title,
                        description = description,
                        city = city,
                        category = category.ifEmpty { null },
                        media = null
                    )
                    onAnnonceCreated()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && title.isNotBlank() && description.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Créer l'annonce")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAnnonceBottomSheet(
    annonce: Annonce,
    onDismiss: () -> Unit,
    onAnnonceUpdated: () -> Unit = {},
    viewModel: AnnoncesViewModel = viewModel()
) {
    val sheetState = rememberStandardBottomSheetState()
    val context = LocalContext.current
    
    var title by remember { mutableStateOf(annonce.title) }
    var description by remember { mutableStateOf(annonce.description) }
    var category by remember { mutableStateOf(annonce.category ?: "") }
    var city by remember { mutableStateOf(annonce.city ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val categories = listOf("Cours", "Formation", "Workshop", "Autre")
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }
    
    BottomSheetContainer(
        sheetState = sheetState,
        onDismiss = onDismiss,
        title = "Modifier l'annonce",
        subtitle = "Mettez à jour votre annonce"
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
                leadingIcon = { Icon(Icons.Default.Title, null) }
            )
            
            BottomSheetTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                singleLine = false,
                maxLines = 4,
                leadingIcon = { Icon(Icons.Default.Description, null) }
            )
            
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Catégorie") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
            
            BottomSheetTextField(
                value = city,
                onValueChange = { city = it },
                label = "Ville",
                leadingIcon = { Icon(Icons.Default.LocationOn, null) }
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
                    viewModel.updateAnnonce(
                        id = annonce.id,
                        title = title,
                        description = description,
                        city = city,
                        category = category.ifEmpty { null },
                        media = null
                    )
                    onAnnonceUpdated()
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
