package com.skillswap.ui.promos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.skillswap.model.MediaPayload
import com.skillswap.model.Promo
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.PromosViewModel
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import com.skillswap.ui.components.SkillSelectionComposable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPromoScreen(
    promo: Promo,
    onBack: () -> Unit,
    onSave: () -> Unit,
    viewModel: PromosViewModel = viewModel()
) {
    val context = LocalContext.current
    
    var title by remember { mutableStateOf(promo.title) }
    var description by remember { mutableStateOf(promo.description ?: "") }
    var skills by remember { mutableStateOf<List<String>>(emptyList()) }
    var discountText by remember { mutableStateOf(promo.discount.toString()) }
    var promoCode by remember { mutableStateOf(promo.promoCode ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var mediaPayload by remember { mutableStateOf<MediaPayload?>(null) }
    
    // Parse existing validTo date
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val initialDate = try {
        promo.validUntil.let { dateFormat.parse(it) } ?: Date()
    } catch (e: Exception) {
        Date()
    }
    
    var selectedDate by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            imageUri = it
            // Convert URI to MediaPayload
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (bytes != null) {
                    val fileName = "promo_${System.currentTimeMillis()}.jpg"
                    val mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
                    mediaPayload = MediaPayload(
                        filename = fileName,
                        mimeType = mimeType,
                        bytes = bytes
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Handle success
    LaunchedEffect(success) {
        if (success != null) {
            onSave()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier la promo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp)
                        )
                    } else {
                        TextButton(
                            onClick = {
                                val discount = discountText.toIntOrNull() ?: 0
                                val validToString = dateFormat.format(selectedDate)
                                
                                viewModel.updatePromo(
                                    id = promo.id,
                                    title = title,
                                    description = description,
                                    discount = discount,
                                    validTo = validToString,
                                    validFrom = promo.validFrom,
                                    promoCode = promoCode.ifEmpty { null },
                                    media = mediaPayload
                                )
                            },
                            enabled = title.isNotBlank() && description.isNotBlank()
                        ) {
                            Text("Enregistrer", color = OrangePrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
            
            // Skills
            SkillSelectionComposable(
                selectedSkills = skills,
                onSkillsChanged = { skills = it },
                title = "Compétences concernées",
                placeholder = "Rechercher des compétences...",
                maxSelections = 3
            )
            
            // Discount
            OutlinedTextField(
                value = discountText,
                onValueChange = { discountText = it.filter { char -> char.isDigit() } },
                label = { Text("Réduction (%)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Promo Code
            OutlinedTextField(
                value = promoCode,
                onValueChange = { promoCode = it },
                label = { Text("Code promo (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Valid Until Date
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Valable jusqu'au: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)}")
            }
            
            // Image Section
            Text("Image", style = MaterialTheme.typography.titleMedium)
            
            Button(
                onClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Changer l'image")
            }
            
            // Display selected or current image
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else if (promo.imageUrl != null) {
                AsyncImage(
                    model = promo.imageUrl,
                    contentDescription = "Current image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
            
            // Error message
            error?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.time
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = Date(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
