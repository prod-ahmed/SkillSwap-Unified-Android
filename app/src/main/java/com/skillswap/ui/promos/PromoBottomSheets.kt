package com.skillswap.ui.promos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.skillswap.model.Promo
import com.skillswap.ui.components.*
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.PromosViewModel

import com.skillswap.ai.CloudflareAIService
import com.skillswap.model.MediaPayload
import kotlinx.coroutines.launch

private val GlassWhite = Color.White.copy(alpha = 0.85f)
private val AIButtonColor = Color(0xFF5856D6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePromoBottomSheet(
    onDismiss: () -> Unit,
    onPromoCreated: () -> Unit = {},
    viewModel: PromosViewModel = viewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("") }
    var promoCode by remember { mutableStateOf("") }
    var validUntil by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var generatedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var generatedImageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isGeneratingDescription by remember { mutableStateOf(false) }
    var isGeneratingImage by remember { mutableStateOf(false) }
    
    val error by viewModel.error.collectAsState()
    
    // Date picker state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L // Default: 1 week from now
    )
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> 
        uri?.let {
            selectedImageUri = it
            generatedImageBytes = null
            generatedImageBitmap = null
            context.contentResolver.openInputStream(it)?.use { stream ->
                selectedImageBytes = stream.readBytes()
            }
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            // Format as ISO 8601 with time (end of day)
                            validUntil = "${date}T23:59:59.000Z"
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = OrangePrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = OrangePrimary,
                    todayDateBorderColor = OrangePrimary
                )
            )
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF8F0), Color.White)
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(top = 16.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            )
            
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Nouvelle Promotion",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9500)
                    )
                    Text(
                        "Créez une offre attractive",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                }
            }
            
            // Error message
            error?.let { errorMessage ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFEBEE)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFC62828))
                        Spacer(Modifier.width(8.dp))
                        Text(errorMessage, color = Color(0xFFC62828), fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title - Glassy Card
                GlassInputCard {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titre de la promotion") },
                        placeholder = { Text("Ex: Promo Black Friday") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        leadingIcon = { Icon(Icons.Default.LocalOffer, null, tint = OrangePrimary) }
                    )
                }
                
                // Description with AI Button - Glassy Card
                GlassInputCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Description",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            
                            // AI Generate Button
                            Button(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        scope.launch {
                                            isGeneratingDescription = true
                                            try {
                                                val discountText = if (discount.isNotBlank()) "$discount%" else "spéciale"
                                                description = generatePromoText(title, discountText)
                                            } finally {
                                                isGeneratingDescription = false
                                            }
                                        }
                                    }
                                },
                                enabled = title.isNotBlank() && !isGeneratingDescription,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AIButtonColor,
                                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isGeneratingDescription) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isGeneratingDescription) "..." else "IA",
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Décrivez votre promotion...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                }
                
                // Discount & Code Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassInputCard(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = discount,
                            onValueChange = { if (it.all { c -> c.isDigit() }) discount = it },
                            label = { Text("Réduction %") },
                            placeholder = { Text("20") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            leadingIcon = { Text("🏷️", fontSize = 16.sp) }
                        )
                    }
                    
                    GlassInputCard(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = promoCode,
                            onValueChange = { promoCode = it.uppercase() },
                            label = { Text("Code") },
                            placeholder = { Text("PROMO20") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
                
                // Valid Until - Date Picker
                GlassInputCard {
                    OutlinedTextField(
                        value = if (validUntil.isNotBlank()) validUntil else "",
                        onValueChange = { },
                        label = { Text("Valable jusqu'au") },
                        placeholder = { Text("Sélectionner une date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Transparent,
                            disabledLabelColor = Color.Gray,
                            disabledPlaceholderColor = Color.Gray.copy(alpha = 0.6f),
                            disabledLeadingIconColor = OrangePrimary,
                            disabledTrailingIconColor = OrangePrimary
                        ),
                        leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                        trailingIcon = { 
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.Edit, null, tint = OrangePrimary)
                            }
                        }
                    )
                }
                
                // Image Section
                GlassInputCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Image",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            
                            // AI Generate Image Button
                            Button(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        scope.launch {
                                            isGeneratingImage = true
                                            try {
                                                val prompt = "Promotional banner for: $title, ${discount}% discount offer, modern clean design"
                                                val imageBytes = CloudflareAIService.generateImage(prompt)
                                                generatedImageBytes = imageBytes
                                                // Convert bytes to bitmap for display
                                                generatedImageBitmap = android.graphics.BitmapFactory.decodeByteArray(
                                                    imageBytes, 0, imageBytes.size
                                                )
                                                selectedImageUri = null
                                                selectedImageBytes = null
                                            } catch (e: Exception) {
                                                // Handle error silently
                                                android.util.Log.e("PromoBottomSheet", "Failed to generate image: ${e.message}")
                                            } finally {
                                                isGeneratingImage = false
                                            }
                                        }
                                    }
                                },
                                enabled = title.isNotBlank() && !isGeneratingImage,
                                colors = ButtonDefaults.buttonColors(containerColor = AIButtonColor),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isGeneratingImage) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Générer", fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Image preview or picker
                        when {
                            generatedImageBitmap != null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Image(
                                        bitmap = generatedImageBitmap!!.asImageBitmap(),
                                        contentDescription = "Image IA générée",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // AI Badge
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = AIButtonColor
                                    ) {
                                        Text(
                                            "✨ IA",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = { 
                                            generatedImageBytes = null
                                            generatedImageBitmap = null
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White)
                                    }
                                }
                            }
                            generatedImageBytes != null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Gray.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Fallback: Show placeholder if bitmap conversion failed
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("✨ Image IA générée", color = AIButtonColor, fontWeight = FontWeight.Medium)
                                    }
                                    IconButton(
                                        onClick = { generatedImageBytes = null },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                                    }
                                }
                            }
                            selectedImageUri != null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { 
                                            selectedImageUri = null
                                            selectedImageBytes = null
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White)
                                    }
                                }
                            }
                            else -> {
                                OutlinedButton(
                                    onClick = { imagePicker.launch("image/*") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddPhotoAlternate, null, tint = OrangePrimary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Choisir une image", color = Color.Gray, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Submit Button
            Button(
                onClick = {
                    isSubmitting = true
                    val media = when {
                        generatedImageBytes != null -> MediaPayload(
                            bytes = generatedImageBytes!!,
                            filename = "ai_promo_${System.currentTimeMillis()}.jpg",
                            mimeType = "image/jpeg"
                        )
                        selectedImageBytes != null -> MediaPayload(
                            bytes = selectedImageBytes!!,
                            filename = "promo_${System.currentTimeMillis()}.jpg",
                            mimeType = "image/jpeg"
                        )
                        else -> null
                    }
                    
                    // Get current date as validFrom in ISO format
                    val today = java.time.LocalDate.now().toString() + "T00:00:00.000Z"
                    
                    viewModel.createPromo(
                        title = title,
                        description = description,
                        discount = discount.toIntOrNull() ?: 0,
                        validTo = validUntil.ifBlank { 
                            // Default to 30 days from now if not set
                            java.time.LocalDate.now().plusDays(30).toString() + "T23:59:59.000Z"
                        },
                        validFrom = today,
                        promoCode = promoCode.ifEmpty { null },
                        media = media
                    )
                    // Note: Don't dismiss here - wait for success state
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(56.dp),
                enabled = !isSubmitting && title.isNotBlank() && discount.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Publish, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publier la promotion", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
    
    // Observe success to dismiss
    val success by viewModel.success.collectAsState()
    LaunchedEffect(success) {
        if (success != null) {
            onPromoCreated()
            onDismiss()
            viewModel.clearMessages()
        }
    }
}

@Composable
private fun GlassInputCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

private suspend fun generatePromoText(title: String, discount: String): String {
    kotlinx.coroutines.delay(800)
    return """🎉 Profitez de notre offre $discount sur "$title"!

✨ Offre limitée - Ne manquez pas cette opportunité exceptionnelle!

📅 Réservez maintenant et bénéficiez de cette réduction exclusive."""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPromoBottomSheet(
    promo: Promo,
    onDismiss: () -> Unit,
    onPromoUpdated: () -> Unit = {},
    viewModel: PromosViewModel = viewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Modifier la promotion",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary
            )
            
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = discount,
                    onValueChange = { if (it.all { c -> c.isDigit() }) discount = it },
                    label = { Text("Réduction %") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it.uppercase() },
                    label = { Text("Code") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            OutlinedTextField(
                value = validUntil,
                onValueChange = { validUntil = it },
                label = { Text("Valable jusqu'au") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSubmitting && title.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Sauvegarder", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
