package com.skillswap.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillswap.ui.theme.OrangePrimary

enum class SortOption(val label: String, val icon: ImageVector) {
    DATE_DESC("Plus récent", Icons.Default.ArrowDownward),
    DATE_ASC("Plus ancien", Icons.Default.ArrowUpward),
    TITLE_ASC("Titre A-Z", Icons.Default.SortByAlpha),
    TITLE_DESC("Titre Z-A", Icons.Default.Sort),
    POPULAR("Populaire", Icons.Default.TrendingUp),
    RATING("Mieux notés", Icons.Default.Star)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSortBar(
    selectedSort: SortOption = SortOption.DATE_DESC,
    onSortSelected: (SortOption) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    showSearch: Boolean = true,
    filterChips: List<FilterChipData> = emptyList(),
    onFilterSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search and Sort Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSearch) {
                if (searchExpanded || searchQuery.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Rechercher...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Effacer")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            focusedLabelColor = OrangePrimary
                        )
                    )
                } else {
                    IconButton(
                        onClick = { searchExpanded = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Rechercher")
                            Text("Rechercher")
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Sort Button
            Box {
                OutlinedButton(
                    onClick = { showSortMenu = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OrangePrimary
                    )
                ) {
                    Icon(
                        selectedSort.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(selectedSort.label)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(option.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Text(option.label)
                                }
                            },
                            onClick = {
                                onSortSelected(option)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (option == selectedSort) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = OrangePrimary
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
        
        // Filter Chips
        if (filterChips.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterChips) { filter ->
                    FilterChip(
                        selected = filter.isSelected,
                        onClick = { onFilterSelected(filter.id) },
                        label = { Text(filter.label) },
                        leadingIcon = filter.icon?.let {
                            {
                                Icon(
                                    it,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangePrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

data class FilterChipData(
    val id: String,
    val label: String,
    val isSelected: Boolean,
    val icon: ImageVector? = null
)

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    icon: ImageVector = Icons.Default.SearchOff,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            if (actionLabel != null && onActionClick != null) {
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun LoadingStateCard(
    message: String = "Chargement...",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = OrangePrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun ErrorStateCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "Une erreur s'est produite",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC62828)
            )
            
            if (onRetry != null) {
                OutlinedButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Réessayer")
                }
            }
        }
    }
}
