package com.skillswap.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillswap.ui.theme.OrangePrimary

data class LocationSuggestion(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    onLocationSelected: (String, Double, Double) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<LocationSuggestion?>(null) }
    
    val suggestions = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            listOf(
                LocationSuggestion("1", "Tunis Centre", "Avenue Habib Bourguiba, Tunis", 36.8065, 10.1815),
                LocationSuggestion("2", "Ariana", "Ariana, Tunisia", 36.8625, 10.1956),
                LocationSuggestion("3", "La Marsa", "La Marsa, Tunisia", 36.8781, 10.3246),
                LocationSuggestion("4", "Sousse", "Sousse, Tunisia", 35.8256, 10.6369),
                LocationSuggestion("5", "Sfax", "Sfax, Tunisia", 34.7406, 10.7603),
            )
        } else {
            listOf(
                LocationSuggestion("1", "Tunis Centre", "Avenue Habib Bourguiba, Tunis", 36.8065, 10.1815),
                LocationSuggestion("2", "Ariana", "Ariana, Tunisia", 36.8625, 10.1956),
            ).filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.address.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choisir une localisation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    if (selectedLocation != null) {
                        TextButton(
                            onClick = {
                                selectedLocation?.let {
                                    onLocationSelected(it.address, it.lat, it.lng)
                                }
                                onBack()
                            }
                        ) {
                            Text("Confirmer", color = OrangePrimary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Rechercher une ville...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Map placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = OrangePrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            selectedLocation?.name ?: "Carte interactive",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        selectedLocation?.address?.let { address ->
                            Text(
                                address,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Suggestions list
            Text(
                "Suggestions",
                modifier = Modifier.padding(horizontal = 16.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { suggestion ->
                    LocationItem(
                        suggestion = suggestion,
                        isSelected = selectedLocation?.id == suggestion.id,
                        onClick = { selectedLocation = suggestion }
                    )
                }
            }
        }
    }
}

@Composable
fun LocationItem(
    suggestion: LocationSuggestion,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) OrangePrimary.copy(alpha = 0.1f) else Color.White
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isSelected) OrangePrimary else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    suggestion.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = if (isSelected) OrangePrimary else Color.Black
                )
                Text(
                    suggestion.address,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = OrangePrimary
                )
            }
        }
    }
}
