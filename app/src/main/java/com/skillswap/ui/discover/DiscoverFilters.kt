package com.skillswap.ui.discover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Sort options matching iOS
enum class AnnonceSortOption(val label: String) {
    TITLE_ASC("Titre (A→Z)"),
    TITLE_DESC("Titre (Z→A)"),
    DATE_DESC("Plus récent"),
    DATE_ASC("Plus ancien")
}

enum class PromoSortOption(val label: String) {
    END_DATE_ASC("Date de fin (proche)"),
    END_DATE_DESC("Date de fin (loin)"),
    DISCOUNT_DESC("Réduction (max)"),
    DISCOUNT_ASC("Réduction (min)")
}

/**
 * Filter state for Annonces
 */
data class AnnonceFilterState(
    val searchText: String = "",
    val selectedCategory: String? = null,
    val selectedCity: String? = null,
    val withImageOnly: Boolean = false,
    val sortOption: AnnonceSortOption = AnnonceSortOption.TITLE_ASC
)

/**
 * Filter state for Promos
 */
data class PromoFilterState(
    val searchText: String = "",
    val showOnlyActive: Boolean = false,
    val minDiscount: Int = 0,
    val sortOption: PromoSortOption = PromoSortOption.END_DATE_ASC
)

/**
 * Bottom sheet for Annonce filters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnonceFilterSheet(
    currentState: AnnonceFilterState,
    availableCategories: List<String> = emptyList(),
    availableCities: List<String> = emptyList(),
    onApply: (AnnonceFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var filterState by remember { mutableStateOf(currentState) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filtres & Tri",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sort Section
            Text(
                "Trier par",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            AnnonceSortOption.values().forEach { option ->
                FilterChip(
                    selected = filterState.sortOption == option,
                    onClick = { filterState = filterState.copy(sortOption = option) },
                    label = { Text(option.label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Category Filter
            if (availableCategories.isNotEmpty()) {
                Text(
                    "Catégorie",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                FilterChip(
                    selected = filterState.selectedCategory == null,
                    onClick = { filterState = filterState.copy(selectedCategory = null) },
                    label = { Text("Toutes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                
                availableCategories.forEach { category ->
                    FilterChip(
                        selected = filterState.selectedCategory == category,
                        onClick = { filterState = filterState.copy(selectedCategory = category) },
                        label = { Text(category) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // City Filter
            if (availableCities.isNotEmpty()) {
                Text(
                    "Ville",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                FilterChip(
                    selected = filterState.selectedCity == null,
                    onClick = { filterState = filterState.copy(selectedCity = null) },
                    label = { Text("Toutes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                
                availableCities.take(10).forEach { city ->
                    FilterChip(
                        selected = filterState.selectedCity == city,
                        onClick = { filterState = filterState.copy(selectedCity = city) },
                        label = { Text(city) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // With Image Only
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Avec image uniquement")
                Switch(
                    checked = filterState.withImageOnly,
                    onCheckedChange = { filterState = filterState.copy(withImageOnly = it) }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        filterState = AnnonceFilterState()
                        onApply(filterState)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Réinitialiser")
                }
                
                Button(
                    onClick = {
                        onApply(filterState)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Appliquer")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Bottom sheet for Promo filters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoFilterSheet(
    currentState: PromoFilterState,
    onApply: (PromoFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var filterState by remember { mutableStateOf(currentState) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filtres & Tri",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sort Section
            Text(
                "Trier par",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            PromoSortOption.values().forEach { option ->
                FilterChip(
                    selected = filterState.sortOption == option,
                    onClick = { filterState = filterState.copy(sortOption = option) },
                    label = { Text(option.label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Active Only
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Promos actives uniquement")
                Switch(
                    checked = filterState.showOnlyActive,
                    onCheckedChange = { filterState = filterState.copy(showOnlyActive = it) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Min Discount Filter
            Text(
                "Réduction minimum: ${filterState.minDiscount}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val discountOptions = listOf(0, 10, 25, 50, 70, 90)
            discountOptions.forEach { discount ->
                FilterChip(
                    selected = filterState.minDiscount == discount,
                    onClick = { filterState = filterState.copy(minDiscount = discount) },
                    label = { Text(if (discount == 0) "Toutes" else "≥ $discount%") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        filterState = PromoFilterState()
                        onApply(filterState)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Réinitialiser")
                }
                
                Button(
                    onClick = {
                        onApply(filterState)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Appliquer")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
