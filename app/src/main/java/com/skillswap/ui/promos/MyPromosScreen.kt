package com.skillswap.ui.promos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.skillswap.model.Promo
import com.skillswap.viewmodel.PromosViewModel
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.skillswap.model.MediaPayload
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
import androidx.compose.material3.LinearProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPromosScreenContent(
    navController: NavController,
    viewModel: PromosViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadPromos()
    }
    val promos by viewModel.promos.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()
    val uploading by viewModel.uploading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var editingPromo by remember { mutableStateOf<Promo?>(null) }
    var pendingImage by remember { mutableStateOf<MediaPayload?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    val out = ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                    out.toByteArray()
                } ?: throw IllegalStateException("Fichier illisible")
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val filename = "promo_${System.currentTimeMillis()}.jpg"
                pendingImage = MediaPayload(bytes, filename, mime)
                imageError = null
            }.onFailure { ex ->
                imageError = "Image non chargée: ${ex.message}"
                pendingImage = null
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes Promos") },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.Close, contentDescription = "Retour")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter")
            }
        }
    ) { padding ->
        error?.let {
            StatusBanner(
                text = it,
                background = Color(0xFFFFEDEC),
                content = Color(0xFFB3261E),
                onDismiss = { viewModel.clearMessages() },
                modifier = Modifier.padding(16.dp)
            )
        }
        success?.let {
            StatusBanner(
                text = it,
                background = Color(0xFFE6F4EA),
                content = Color(0xFF1B5E20),
                onDismiss = { viewModel.clearMessages() },
                modifier = Modifier.padding(16.dp)
            )
        }
        if (uploading) {
            LinearProgressIndicator(
                progress = { uploadProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        if (promos.isEmpty()) {
            EmptyPromosState(onRefresh = { viewModel.loadPromos() }, modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF2F2F7))
            ) {
                items(promos) { promo ->
                    PromoCard(
                        promo,
                        onDelete = { viewModel.deletePromo(promo.id) },
                        onEdit = {
                            editingPromo = promo
                        }
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreatePromoBottomSheet(
            onDismiss = { showCreate = false },
            onPromoCreated = {
                showCreate = false
                viewModel.loadPromos()
            }
        )
    }
    
    editingPromo?.let { promo ->
        EditPromoBottomSheet(
            promo = promo,
            onDismiss = { editingPromo = null },
            onPromoUpdated = {
                editingPromo = null
                viewModel.loadPromos()
            }
        )
    }
}

@Composable
private fun StatusBanner(
    text: String,
    background: Color,
    content: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text, color = content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("OK", color = content) }
        }
    }
}

@Composable
private fun EmptyPromosState(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Aucune promo créée", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRefresh) { Text("Rafraîchir") }
    }
}
@Composable
fun PromoCard(promo: Promo, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box {
                if (promo.imageUrl?.isNotBlank() == true) {
                    AsyncImage(
                        model = promo.imageUrl,
                        contentDescription = promo.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color.LightGray),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color(0xFFF2F2F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(promo.title, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    color = Color.Green,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text("-${promo.discount}%", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal=12.dp, vertical=6.dp))
                }
            }
            
            Column(Modifier.padding(16.dp)) {
                Text(promo.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(promo.description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                Text("Valide jusqu'au ${promo.validUntil}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                
                if (promo.promoCode != null) {
                    Spacer(Modifier.height(8.dp))

                    Surface(
                        color = com.skillswap.ui.theme.OrangePrimary.copy(alpha=0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Code: ${promo.promoCode}", color = Color(0xFFFF6B35), fontWeight=FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onEdit) { Text("Modifier") }
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                        Text("Supprimer")
                    }
                }
            }
        }
    }
}

@Composable
fun PromoEditorDialog(
    initial: Promo?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, String?, String?, String?) -> Unit,
    onPickImage: () -> Unit,
    imageError: String?,
    uploading: Boolean
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var discount by remember { mutableStateOf(initial?.discount?.toString().orEmpty()) }
    var validFrom by remember { mutableStateOf(initial?.validFrom ?: "") }
    var validTo by remember { mutableStateOf(initial?.validUntil ?: "") }
    var code by remember { mutableStateOf(initial?.promoCode.orEmpty()) }
    var image by remember { mutableStateOf(initial?.imageUrl.orEmpty()) }
    val canSave = title.isNotBlank() && description.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    title,
                    description,
                    discount.toIntOrNull() ?: 0,
                    validTo,
                    validFrom.ifBlank { null },
                    code.ifBlank { null },
                    image.ifBlank { null }
                )
            }, enabled = canSave && !uploading) { Text(if (uploading) "Envoi..." else "Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text(if (initial == null) "Nouvelle promo" else "Modifier la promo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Réduction (%)") })
                OutlinedTextField(value = validFrom, onValueChange = { validFrom = it }, label = { Text("Début (ISO)") })
                OutlinedTextField(value = validTo, onValueChange = { validTo = it }, label = { Text("Fin (ISO)") })
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code (optionnel)") })
                OutlinedTextField(
                    value = image,
                    onValueChange = { image = it },
                    label = { Text("Image (URL direct optionnel)") }
                )
                TextButton(onClick = onPickImage) { Text("Choisir dans la galerie") }
                imageError?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.labelSmall) }
                if (!canSave) Text("Titre et description requis", color = Color.Red, style = MaterialTheme.typography.labelSmall)
            }
        }
    )
}
