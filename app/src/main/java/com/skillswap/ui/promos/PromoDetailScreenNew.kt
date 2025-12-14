package com.skillswap.ui.promos

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.skillswap.model.Promo
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.PromosViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoDetailScreenNew(
    promoId: String,
    onBack: () -> Unit,
    viewModel: PromosViewModel = viewModel()
) {
    var promo by remember { mutableStateOf<Promo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val clipboardManager = LocalClipboardManager.current
    var showCopiedSnackbar by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(promoId) {
        // Load promo details
        // viewModel.loadPromo(promoId)
        // For now, mock data
        isLoading = false
    }
    
    LaunchedEffect(showCopiedSnackbar) {
        if (showCopiedSnackbar) {
            snackbarHostState.showSnackbar("Code promo copié!")
            showCopiedSnackbar = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails de la promotion") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, "Partager")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Image with Discount Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    AsyncImage(
                        model = promo?.imageUrl ?: "https://via.placeholder.com/400x300",
                        contentDescription = "Image de la promotion",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                    
                    // Discount badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        color = Color.Red,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = "-${promo?.discount ?: 20}%",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    
                    // Title at bottom
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = promo?.title ?: "Promotion spéciale",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Valable jusqu'au ${formatDate(promo?.validUntil ?: "")}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
                
                // Content
                Column(modifier = Modifier.padding(16.dp)) {
                    // Promo Code Card
                    promo?.promoCode?.let { code ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = OrangePrimary.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Code promo",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangePrimary
                                    )
                                    
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(code))
                                            showCopiedSnackbar = true
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copier le code",
                                            tint = OrangePrimary
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Description
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = promo?.description ?: "Description de la promotion...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.DarkGray
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Conditions
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = OrangePrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Conditions d'utilisation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            ConditionItem("Réduction de ${promo?.discount ?: 20}% sur les services")
                            ConditionItem("Valable jusqu'au ${formatDate(promo?.validUntil ?: "")}")
                            ConditionItem("Non cumulable avec d'autres offres")
                            ConditionItem("Utilisable une seule fois par utilisateur")
                        }
                    }
                    
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Apply Button
                    Button(
                        onClick = { /* Apply promo */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Icon(Icons.Default.LocalOffer, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Utiliser cette promotion",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConditionItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = OrangePrimary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = OffsetDateTime.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (e: Exception) {
        "Date inconnue"
    }
}
