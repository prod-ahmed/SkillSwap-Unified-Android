package com.skillswap.ui.annonces

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.skillswap.model.Annonce
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.AnnoncesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnonceDetailScreen(
    annonceId: String,
    onBack: () -> Unit,
    onContact: () -> Unit = {},
    viewModel: AnnoncesViewModel = viewModel()
) {
    val context = LocalContext.current
    var annonce by remember { mutableStateOf<Annonce?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(annonceId) {
        isLoading = true
        annonce = viewModel.getAnnonceById(annonceId)
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
                    annonce?.let { ann ->
                        IconButton(onClick = {
                            val shareText = "${ann.title}\n${ann.description}"
                            val intent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Partager"))
                        }) {
                            Icon(Icons.Default.Share, "Partager")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (annonce == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Annonce introuvable")
            }
        } else {
            val currentAnnonce = annonce!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Image Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    if (currentAnnonce.imageUrl?.isNotBlank() == true) {
                        AsyncImage(
                            model = currentAnnonce.imageUrl,
                            contentDescription = currentAnnonce.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                currentAnnonce.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = OrangePrimary
                            )
                        }
                    }
                    
                    // "New" Badge
                    if (currentAnnonce.isNew) {
                        Surface(
                            color = Color(0xFFFF2D55),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "NOUVEAU",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                
                // Details Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            currentAnnonce.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            currentAnnonce.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            lineHeight = 24.sp
                        )
                        
                        HorizontalDivider(color = Color.LightGray)
                        
                        // Location
                        currentAnnonce.city?.let { city ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    city,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // Category
                        currentAnnonce.category?.let { category ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Category,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    category,
                                    fontSize = 15.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        // Author
                        currentAnnonce.user?.let { user ->
                            HorizontalDivider(color = Color.LightGray)
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(OrangePrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (user.image?.isNotBlank() == true) {
                                        AsyncImage(
                                            model = user.image,
                                            contentDescription = user.username,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            user.username.take(1).uppercase(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OrangePrimary
                                        )
                                    }
                                }
                                
                                Column {
                                    Text(
                                        "Publié par",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        user.username,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                        
                        // Date
                        Text(
                            "Publié le ${formatDate(currentAnnonce.createdAt)}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Button
                Button(
                    onClick = onContact,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contacter l'auteur", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        dateString.substringBefore('T')
    } catch (e: Exception) {
        dateString
    }
}
