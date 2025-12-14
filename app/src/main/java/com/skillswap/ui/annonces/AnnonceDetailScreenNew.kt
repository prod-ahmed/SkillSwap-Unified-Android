package com.skillswap.ui.annonces

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.skillswap.model.Annonce
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.AnnoncesViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnonceDetailScreen(
    annonceId: String,
    onBack: () -> Unit,
    onContact: (String) -> Unit,
    viewModel: AnnoncesViewModel = viewModel()
) {
    var annonce by remember { mutableStateOf<Annonce?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showContactDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(annonceId) {
        // Load annonce details
        // viewModel.loadAnnonce(annonceId)
        // For now, mock data
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails de l'annonce") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, "Partager")
                    }
                    IconButton(onClick = { /* Favorite */ }) {
                        Icon(Icons.Default.FavoriteBorder, "Favori")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Image
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = annonce?.imageUrl ?: "https://via.placeholder.com/400x250",
                                contentDescription = "Image de l'annonce",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Category badge
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                color = OrangePrimary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = annonce?.category ?: "Cours",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    
                    // Title and author
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = annonce?.title ?: "Titre de l'annonce",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.Gray
                            )
                            Text(
                                text = "Publié par ${annonce?.user?.username ?: "Utilisateur"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.Gray
                            )
                            Text(
                                text = formatDate(annonce?.createdAt ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        
                        // Price (Annonce model doesn't have price field, removing)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Description
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = annonce?.description ?: "Description de l'annonce...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.DarkGray
                        )
                        
                        // Skills (Annonce model doesn't have skills, removing this section)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        annonce?.city?.let { city ->
                            if (city.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = OrangePrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = city,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                
                // Contact button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = { showContactDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Contacter l'auteur",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    
    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("Contacter l'auteur") },
            text = {
                Text("Souhaitez-vous envoyer un message à ${annonce?.user?.username ?: "cet utilisateur"} ?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val user = annonce?.user
                        if (user != null) {
                            onContact(user._id)
                        }
                        showContactDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Envoyer un message")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = OffsetDateTime.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    } catch (e: Exception) {
        "Date inconnue"
    }
}
