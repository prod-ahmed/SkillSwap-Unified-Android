package com.skillswap.ui.annonces

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.skillswap.model.Annonce
import com.skillswap.viewmodel.AnnoncesViewModel
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Context
import android.provider.OpenableColumns
import androidx.compose.runtime.rememberCoroutineScope
import com.skillswap.model.MediaPayload
import kotlinx.coroutines.launch
import androidx.compose.material3.LinearProgressIndicator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MyAnnoncesScreenContent(
    navController: NavController,
    viewModel: AnnoncesViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadAnnonces()
    }
    val annonces by viewModel.annonces.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()
    val uploading by viewModel.uploading.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var editingAnnonce by remember { mutableStateOf<Annonce?>(null) }
    
    // Filtering and Sorting state
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("date") } // date, title, city
    var showFilterSheet by remember { mutableStateOf(false) }
    
    // Filter and sort annonces
    val filteredAnnonces = remember(annonces, searchQuery, selectedCategory, sortBy) {
        var result = annonces
        
        // Apply search filter
        if (searchQuery.isNotBlank()) {
            result = result.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                (it.city?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
        
        // Apply category filter
        if (selectedCategory != null) {
            result = result.filter { it.category == selectedCategory }
        }
        
        // Apply sorting
        when (sortBy) {
            "title" -> result.sortedBy { it.title }
            "city" -> result.sortedBy { it.city ?: "" }
            else -> result.sortedByDescending { it.createdAt } // date (newest first)
        }
    }
    
    val categories = remember(annonces) {
        annonces.mapNotNull { it.category }.distinct()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes Annonces") },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.Close, contentDescription = "Retour")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "Filtres",
                            tint = if (searchQuery.isNotBlank() || selectedCategory != null) 
                                com.skillswap.ui.theme.OrangePrimary 
                            else Color.Gray
                        )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        if (filteredAnnonces.isEmpty()) {
            EmptyAnnoncesState(
                onRefresh = { viewModel.loadAnnonces() },
                message = if (searchQuery.isNotBlank() || selectedCategory != null) 
                    "Aucun résultat trouvé" 
                else 
                    "Aucune annonce publiée",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF2F2F7))
            ) {
                items(filteredAnnonces) { annonce ->
                    AnnonceCard(
                        annonce = annonce,
                        onDelete = { viewModel.deleteAnnonce(annonce.id) },
                        onEdit = { editingAnnonce = annonce },
                        onClick = {
                            // For simplicity, navigate with the annonce in the back stack
                            // In production, you'd use a shared ViewModel or pass serialized data
                        }
                    )
                }
            }
        }
    }

    if (showCreate) {
        AnnonceEditorDialog(
            initial = null,
            onDismiss = { showCreate = false },
            onSave = { title, desc, city, category, image ->
                viewModel.createAnnonce(title, desc, city, category, image)
                if (!uploading) showCreate = false
            }
        )
    }
    editingAnnonce?.let { annonce ->
        AnnonceEditorDialog(
            initial = annonce,
            onDismiss = { editingAnnonce = null },
            onSave = { title, desc, city, category, image ->
                viewModel.updateAnnonce(annonce.id, title, desc, city, category, image)
                if (!uploading) editingAnnonce = null
            }
        )
    }
    
    // Filter Bottom Sheet
    if (showFilterSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Filtres et Tri",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                
                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Rechercher") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, "Recherche") },
                    singleLine = true
                )
                
                // Category filter
                Text("Catégorie", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Toutes") }
                    )
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = if (selectedCategory == category) null else category },
                            label = { Text(category) }
                        )
                    }
                }
                
                // Sorting
                Text("Trier par", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortOption("Date (récent)", "date", sortBy) { sortBy = it }
                    SortOption("Titre", "title", sortBy) { sortBy = it }
                    SortOption("Ville", "city", sortBy) { sortBy = it }
                }
                
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            searchQuery = ""
                            selectedCategory = null
                            sortBy = "date"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Réinitialiser")
                    }
                    Button(
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Appliquer")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SortOption(label: String, value: String, current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(value) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.RadioButton(
            selected = current == value,
            onClick = { onSelect(value) }
        )
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
fun AnnonceCard(annonce: Annonce, onDelete: () -> Unit, onEdit: () -> Unit, onClick: () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            if (annonce.imageUrl?.isNotBlank() == true) {
                AsyncImage(
                    model = annonce.imageUrl,
                    contentDescription = annonce.title,
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
                    Text(annonce.title, fontWeight = FontWeight.Bold)
                }
            }
            
            Column(Modifier.padding(16.dp)) {
                Text(annonce.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(annonce.description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, maxLines = 2)
                
                Spacer(Modifier.height(8.dp))
                Row {
                   if (annonce.city != null) {
                       Text(annonce.city, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
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
private fun EmptyAnnoncesState(onRefresh: () -> Unit, message: String = "Aucune annonce publiée", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = Color.Gray, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRefresh) { Text("Rafraîchir") }
    }
}
@Composable
fun AnnonceEditorDialog(
    initial: Annonce?,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String?, MediaPayload?) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var city by remember { mutableStateOf(initial?.city ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var selectedImage by remember { mutableStateOf<MediaPayload?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val canSave = title.isNotBlank() && description.isNotBlank()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Impossible de lire le fichier")
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val filename = resolveFileName(context, uri) ?: "annonce_${System.currentTimeMillis()}.jpg"
                selectedImage = MediaPayload(bytes, filename, mime)
                previewUri = uri
                imageError = null
            }.onFailure {
                imageError = "Image non chargée: ${it.message}"
                selectedImage = null
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    title,
                    description,
                    city.ifBlank { null },
                    category.ifBlank { null },
                    selectedImage
                )
            }, enabled = canSave) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text(if (initial == null) "Nouvelle annonce" else "Modifier l'annonce") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Ville") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Catégorie") })
                if (previewUri != null || initial?.imageUrl != null) {
                    AsyncImage(
                        model = previewUri ?: initial?.imageUrl,
                        contentDescription = "Aperçu image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color.LightGray),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { selectedImage = null; previewUri = null }) {
                            Text("Retirer l'image")
                        }
                    }
                }
                TextButton(onClick = { picker.launch("image/*") }) { Text("Choisir dans la galerie") }
                imageError?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.labelSmall) }
                if (!canSave) {
                    Text("Titre et description requis", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    )
}

private fun resolveFileName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)
        } else null
    }
}
