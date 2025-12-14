package com.skillswap.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.skillswap.model.Annonce
import com.skillswap.model.Promo
import com.skillswap.model.User
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.DiscoverSegment
import com.skillswap.viewmodel.DiscoverViewModel
import kotlin.math.roundToInt
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField

// Colors matching iOS
val SkillCoral = Color(0xFFFF6B6B)
val SkillCoralLight = Color(0xFFFF8E8E)
val SkillTurquoise = Color(0xFF4ECDC4)
val SkillGold = Color(0xFFFFD166)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onNavigateToChat: (String) -> Unit = {},
    viewModel: DiscoverViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadForCurrentSegment()
    }

    val segment by viewModel.segment.collectAsState()
    val users by viewModel.users.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val annonces by viewModel.annonces.collectAsState()
    val promos by viewModel.promos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val cities by viewModel.cities.collectAsState()
    val skills by viewModel.skills.collectAsState()
    val selectedCity by viewModel.cityFilter.collectAsState()
    val selectedSkill by viewModel.skillFilter.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.categoryFilter.collectAsState()

    var filterText by remember { mutableStateOf("") }
    var showAnnonceDialog by remember { mutableStateOf(false) }
    var showPromoDialog by remember { mutableStateOf(false) }
    var showMatchDialog by remember { mutableStateOf(false) }
    var matchedUser by remember { mutableStateOf<User?>(null) }

    val filteredAnnonces = remember(annonces, filterText, selectedCategory, selectedCity) {
        var list = if (filterText.isBlank()) annonces else annonces.filter {
            it.title.contains(filterText, ignoreCase = true) || (it.city ?: "").contains(filterText, ignoreCase = true)
        }
        if (selectedCategory != null) {
            list = list.filter { it.category == selectedCategory }
        }
        if (selectedCity != null) {
            list = list.filter { it.city == selectedCity }
        }
        list
    }
    val filteredPromos = remember(promos, filterText) {
        if (filterText.isBlank()) promos else promos.filter {
            it.title.contains(filterText, ignoreCase = true) || (it.promoCode ?: "").contains(filterText, ignoreCase = true)
        }
    }
    // Main Container
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(SkillCoral, SkillCoralLight)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 48.dp, bottom = 24.dp)
                    .padding(horizontal = 16.dp)
            ) {
                // Title & Icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Découvrir",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         // Menu/Filter Button (Placeholder)
                         IconButton(
                             onClick = { viewModel.loadForCurrentSegment() },
                             modifier = Modifier
                                 .clip(CircleShape)
                                 .background(Color.White.copy(alpha = 0.2f))
                                 .size(40.dp)
                         ) {
                             Icon(
                                 Icons.Default.Refresh,
                                 contentDescription = "Rafraîchir",
                                 tint = Color.White
                             )
                         }

                         // Add Button
                        if (segment != DiscoverSegment.PROFILS) {
                            IconButton(
                                onClick = { if (segment == DiscoverSegment.ANNONCES) showAnnonceDialog = true else showPromoDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = SkillCoral
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Segmented Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SegmentButton("Profils", DiscoverSegment.PROFILS, segment) { viewModel.setSegment(DiscoverSegment.PROFILS); filterText = "" }
                    Spacer(Modifier.width(16.dp))
                    SegmentButton("Annonces", DiscoverSegment.ANNONCES, segment) { viewModel.setSegment(DiscoverSegment.ANNONCES); filterText = "" }
                    Spacer(Modifier.width(16.dp))
                    SegmentButton("Promos", DiscoverSegment.PROMOS, segment) { viewModel.setSegment(DiscoverSegment.PROMOS); filterText = "" }
                }
            }
        }

        // Content
        Box(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            successMessage?.let {
                StatusBanner(
                    text = it,
                    background = Color(0xFFE6F4EA),
                    content = Color(0xFF1B5E20),
                    onDismiss = { viewModel.clearMessages() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            errorMessage?.let {
                StatusBanner(
                    text = it,
                    background = Color(0xFFFFEDEC),
                    content = Color(0xFFB3261E),
                    onDismiss = { viewModel.clearMessages() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    color = SkillCoral,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                when (segment) {
                    DiscoverSegment.PROFILS -> {
                        if (users.isNotEmpty() && currentIndex < users.size) {
                            val currentUser = users[currentIndex]
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(Modifier.weight(1f))
                                key(currentUser.id) {
                                    SwipeableProfileCard(
                                        user = currentUser,
                                        onSwipe = { viewModel.nextProfile() }
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                ActionButtons(
                                    onPass = { viewModel.swipeLeft() },
                                    onLike = {
                                        viewModel.swipeRight(currentUser) { matchUser ->
                                            matchedUser = matchUser
                                            showMatchDialog = true
                                        }
                                    },
                                    onMessage = {
                                        viewModel.startChatWithUser(currentUser.id) { threadId ->
                                            onNavigateToChat(threadId)
                                        }
                                    }
                                )
                                Spacer(Modifier.height(30.dp))
                            }
                        } else {
                            EmptyDiscoverState(message = "Plus de profils pour le moment", action = { viewModel.loadForCurrentSegment() })
                        }
                    }
                    DiscoverSegment.ANNONCES -> {
                        Column {
                            OutlinedTextField(
                                value = filterText,
                                onValueChange = { filterText = it },
                                label = { Text("Filtrer par titre/ville") },
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                            )
                            
                            // City Filter
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCity == null,
                                        onClick = { viewModel.setCityFilter(null) },
                                        label = { Text("Toutes villes") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SkillTurquoise.copy(alpha = 0.2f),
                                            selectedLabelColor = SkillTurquoise
                                        )
                                    )
                                }
                                items(cities) { city ->
                                    FilterChip(
                                        selected = selectedCity == city,
                                        onClick = { viewModel.setCityFilter(if (selectedCity == city) null else city) },
                                        label = { Text(city) },
                                        leadingIcon = if (selectedCity == city) {
                                            { Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SkillTurquoise.copy(alpha = 0.2f),
                                            selectedLabelColor = SkillTurquoise
                                        )
                                    )
                                }
                            }
                            
                            // Category Filter
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCategory == null,
                                        onClick = { viewModel.setCategoryFilter(null) },
                                        label = { Text("Toutes") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SkillCoral.copy(alpha = 0.2f),
                                            selectedLabelColor = SkillCoral
                                        )
                                    )
                                }
                                items(categories) { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { viewModel.setCategoryFilter(if (selectedCategory == cat) null else cat) },
                                        label = { Text(cat) },
                                        leadingIcon = if (selectedCategory == cat) {
                                            { Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SkillCoral.copy(alpha = 0.2f),
                                            selectedLabelColor = SkillCoral
                                        )
                                    )
                                }
                            }
                        }
                        
                        if (filteredAnnonces.isEmpty()) {
                            EmptyDiscoverState(message = "Aucune annonce trouvée", action = { viewModel.loadForCurrentSegment() })
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredAnnonces) { annonce ->
                                    AnnonceDiscoverCard(annonce)
                                }
                            }
                        }
                    }
                    DiscoverSegment.PROMOS -> {
                        Column {
                            OutlinedTextField(
                                value = filterText,
                                onValueChange = { filterText = it },
                                label = { Text("Filtrer par titre/code") },
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                            )
                            
                            // Discount Range Filter
                            var showDiscountFilter by remember { mutableStateOf(false) }
                            var minDiscount by remember { mutableStateOf(0) }
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = minDiscount == 0,
                                        onClick = { minDiscount = 0 },
                                        label = { Text("Toutes") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SkillGold.copy(alpha = 0.2f),
                                            selectedLabelColor = SkillGold
                                        )
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = minDiscount == 10,
                                        onClick = { minDiscount = 10 },
                                        label = { Text("10%+") },
                                        leadingIcon = if (minDiscount == 10) {
                                            { Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SkillGold.copy(alpha = 0.2f),
                                            selectedLabelColor = SkillGold
                                        )
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = minDiscount == 25,
                                        onClick = { minDiscount = 25 },
                                        label = { Text("25%+") },
                                        leadingIcon = if (minDiscount == 25) {
                                            { Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SkillGold.copy(alpha = 0.2f),
                                            selectedLabelColor = SkillGold
                                        )
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = minDiscount == 50,
                                        onClick = { minDiscount = 50 },
                                        label = { Text("50%+") },
                                        leadingIcon = if (minDiscount == 50) {
                                            { Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SkillGold.copy(alpha = 0.2f),
                                            selectedLabelColor = SkillGold
                                        )
                                    )
                                }
                            }
                            
                            val discountFilteredPromos = remember(filteredPromos, minDiscount) {
                                if (minDiscount == 0) filteredPromos
                                else filteredPromos.filter { 
                                    it.discount >= minDiscount
                                }
                            }
                            
                            if (discountFilteredPromos.isEmpty()) {
                                EmptyDiscoverState(message = "Aucune promotion active", action = { viewModel.loadForCurrentSegment() })
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(discountFilteredPromos) { promo ->
                                        PromoDiscoverCard(promo)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (segment == DiscoverSegment.PROFILS) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Cities Filter
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCity == null,
                            onClick = { viewModel.setCityFilter(null) },
                            label = { Text("Toutes villes") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SkillCoral.copy(alpha = 0.2f),
                                selectedLabelColor = SkillCoral
                            )
                        )
                    }
                    items(cities) { city ->
                        FilterChip(
                            selected = selectedCity == city,
                            onClick = { viewModel.setCityFilter(if (selectedCity == city) null else city) },
                            label = { Text(city) },
                            leadingIcon = if (selectedCity == city) {
                                { Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SkillCoral.copy(alpha = 0.2f),
                                selectedLabelColor = SkillCoral
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Skills Filter
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedSkill == null,
                            onClick = { viewModel.setSkillFilter(null) },
                            label = { Text("Toutes compétences") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SkillTurquoise.copy(alpha = 0.2f),
                                selectedLabelColor = SkillTurquoise
                            )
                        )
                    }
                    items(skills) { skill ->
                        FilterChip(
                            selected = selectedSkill == skill,
                            onClick = { viewModel.setSkillFilter(if (selectedSkill == skill) null else skill) },
                            label = { Text(skill) },
                            leadingIcon = if (selectedSkill == skill) {
                                { Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SkillTurquoise.copy(alpha = 0.2f),
                                selectedLabelColor = SkillTurquoise
                            )
                        )
                    }
                }
            }
        }

        if (showAnnonceDialog) {
            CreateAnnonceDialog(
                onDismiss = { showAnnonceDialog = false },
                onCreate = { title, desc, city ->
                    viewModel.createAnnonce(title, desc, city)
                    showAnnonceDialog = false
                }
            )
        }
        if (showPromoDialog) {
            CreatePromoDialog(
                onDismiss = { showPromoDialog = false },
                onCreate = { title, desc, discount, validTo, validFrom, code ->
                    viewModel.createPromo(title, desc, discount, validTo, validFrom, code)
                    showPromoDialog = false
                }
            )
        }
        
        if (showMatchDialog && matchedUser != null) {
            MatchOverlay(
                user = matchedUser!!,
                onDismiss = {
                    showMatchDialog = false
                    viewModel.nextProfile()
                },
                onMessage = {
                    showMatchDialog = false
                    viewModel.startChatWithUser(matchedUser!!.id) { threadId ->
                        onNavigateToChat(threadId)
                    }
                    viewModel.nextProfile()
                }
            )
        }
    }
}

@Composable
fun CreateAnnonceDialog(onDismiss: () -> Unit, onCreate: (String, String, String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onCreate(title, description, city.ifBlank { null }) }) {
                Text("Publier")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Nouvelle annonce") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Ville") })
            }
        }
    )
}

@Composable
fun CreatePromoDialog(onDismiss: () -> Unit, onCreate: (String, String, Int, String, String?, String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("10") }
    var validFrom by remember { mutableStateOf("") }
    var validTo by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onCreate(
                    title,
                    description,
                    discount.toIntOrNull() ?: 0,
                    validTo,
                    validFrom.ifBlank { null },
                    code.ifBlank { null }
                )
            }) { Text("Publier") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Nouvelle promo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Réduction (%)") })
                OutlinedTextField(value = validFrom, onValueChange = { validFrom = it }, label = { Text("Début (ISO)") })
                OutlinedTextField(value = validTo, onValueChange = { validTo = it }, label = { Text("Fin (ISO)") })
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code promo (optionnel)") })
            }
        }
    )
}

@Composable
fun SegmentButton(text: String, targetSegment: DiscoverSegment, currentSegment: DiscoverSegment, onClick: () -> Unit) {
    val isSelected = targetSegment == currentSegment
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon mapping
            val icon = when(targetSegment) {
                DiscoverSegment.PROFILS -> Icons.Default.Group
                DiscoverSegment.ANNONCES -> Icons.Default.Campaign
                DiscoverSegment.PROMOS -> Icons.Default.LocalOffer // Label/Tag
            }
            Icon(icon, contentDescription = "Segment $text", tint = if(isSelected) Color.White else Color.White.copy(alpha=0.6f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                color = if(isSelected) Color.White else Color.White.copy(alpha=0.6f),
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(3.dp)
                .width(40.dp)
                .background(if (isSelected) Color.White else Color.Transparent, CircleShape)
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
    Surface(
        color = background,
        contentColor = content,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = content)
            }
        }
    }
}

@Composable
private fun EmptyDiscoverState(message: String, action: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = action) { Text("Rafraîchir") }
    }
}

@Composable
fun SwipeableProfileCard(user: User, onSwipe: () -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    var rotation by remember { mutableStateOf(0f) }
    val teaching = remember(user.skillsTeach) { user.skillsTeach?.takeIf { it.isNotEmpty() }?.joinToString(", ") }
    val learning = remember(user.skillsLearn) { user.skillsLearn?.takeIf { it.isNotEmpty() }?.joinToString(", ") }
    val rating = user.ratingAvg?.let { String.format("%.1f", it) }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(550.dp) // Fixed height to match iOS look roughly
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .graphicsLayer(rotationZ = rotation)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(offsetX) > 150) {
                            onSwipe()
                            offsetX = 0f
                            rotation = 0f
                        } else {
                            offsetX = 0f
                            rotation = 0f
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    rotation = offsetX / 20
                }
            },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            // Image Area
            Box(modifier = Modifier.height(350.dp).fillMaxWidth()) {
                if (user.avatarUrl?.isNotBlank() == true) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = "Avatar ${user.username}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF5C52BF).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user.username.firstOrNull()?.uppercase() ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5C52BF)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha=0.4f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                     Box(Modifier.size(8.dp).background(Color.Green, CircleShape))
                     Spacer(Modifier.width(6.dp))
                     Text("En ligne", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
            
            // Text Info
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${user.username}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Surface(color = SkillGold.copy(alpha=0.1f), shape = CircleShape) {
                        Row(Modifier.padding(horizontal=12.dp, vertical=6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription=null, tint=SkillGold, modifier=Modifier.size(16.dp))
                            Text(rating ?: "N/A", color = SkillGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                
                Text(
                     user.bio ?: "Passionné par le partage de connaissances.",
                     color = Color.Gray,
                     maxLines = 3,
                     modifier = Modifier.padding(top = 8.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Skills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                     SkillTag("Enseigne: ${teaching ?: "—"}", SkillCoral)
                     SkillTag("Apprend: ${learning ?: "—"}", SkillTurquoise)
                }
            }
        }
    }
}

@Composable
fun SkillTag(text: String, color: Color) {
    Surface(color = color.copy(alpha=0.1f), shape = RoundedCornerShape(8.dp)) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal=8.dp, vertical=4.dp))
    }
}

@Composable
fun ActionButtons(onPass: () -> Unit, onLike: () -> Unit, onMessage: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(30.dp), verticalAlignment = Alignment.CenterVertically) {
        // Pass
        FloatingActionButton(
            onClick = onPass,
            containerColor = Color.White,
            contentColor = Color.Red,
            elevation = FloatingActionButtonDefaults.elevation(8.dp),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Pass")
        }

        // Message
        FloatingActionButton(
            onClick = onMessage,
            containerColor = SkillTurquoise,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(8.dp),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(Icons.Default.ChatBubble, contentDescription = "Message")
        }

        // Like
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(colors = listOf(SkillCoral, SkillCoralLight)))
                .clickable(onClick = onLike),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Favorite, contentDescription = "Like", tint = Color.White)
        }
    }
}

// Reusable Cards for Lists
@Composable
fun AnnonceDiscoverCard(annonce: Annonce) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
             Box(Modifier.height(150.dp).fillMaxWidth().background(Color.LightGray)) // Placeholder img
             Column(Modifier.padding(16.dp)) {
                 Text(annonce.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                 Text(annonce.description, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                 Spacer(Modifier.height(4.dp))
                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                     annonce.city?.let { city ->
                         Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                             Text(city, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                         }
                     }
                     annonce.category?.let { category ->
                         Surface(color = SkillCoral.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                             Text(category, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = SkillCoral, style = MaterialTheme.typography.labelSmall)
                         }
                     }
                 }
             }
        }
    }
}

@Composable
fun PromoDiscoverCard(promo: Promo) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
         Column {
             Box(Modifier.height(150.dp).fillMaxWidth().background(Color.LightGray)) // Placeholder img
             Column(Modifier.padding(16.dp)) {
                 Text(promo.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                 Row {
                      Surface(color = Color.Green.copy(alpha=0.1f), shape = RoundedCornerShape(50)) {
                          Text("-${promo.discount}%", color=Color.Green, modifier=Modifier.padding(horizontal=8.dp, vertical=4.dp))
                      }
                 }
                if (promo.validUntil.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Valide jusqu'au ${promo.validUntil}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
             }
        }
    }
}

// Extension to support spacing in Row (since we don't have Arragement.spacedBy inside Row constructor easily without modifier)
// wait, Row has horizontalArrangement. We used Row(spacing=8.dp) which is invalid compose syntax.
// I must fix `Row(spacing = 8.dp)` to standard Row with Arrangement.
