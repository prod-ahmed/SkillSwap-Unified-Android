package com.skillswap.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.model.Recommendation
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.RecommendationsViewModel

enum class SessionMode {
    ONLINE, IN_PERSON
}

enum class ViewMode {
    LIST, MAP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsPourVousScreen(
    onBack: () -> Unit,
    onProfileClick: (String) -> Unit = {},
    viewModel: RecommendationsViewModel = viewModel()
) {
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var sessionMode by remember { mutableStateOf(SessionMode.ONLINE) }
    
    val recommendations by viewModel.recommendations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Filter recommendations based on mode
    val filteredRecommendations = remember(recommendations, sessionMode) {
        when (sessionMode) {
            SessionMode.ONLINE -> recommendations
            SessionMode.IN_PERSON -> recommendations.filter { it.distance.isNotEmpty() && it.distance != "0 km" }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadRecommendations()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sessions pour vous") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewMode = if (viewMode == ViewMode.LIST) ViewMode.MAP else ViewMode.LIST
                    }) {
                        Icon(
                            if (viewMode == ViewMode.LIST) Icons.Default.Place else Icons.Default.List,
                            contentDescription = "Toggle view"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangePrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            if (viewMode == ViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Yellow banner
                    item {
                        YellowBannerCard(
                            count = filteredRecommendations.size
                        )
                    }
                    
                    // Session mode toggle
                    item {
                        SessionModeToggle(
                            sessionMode = sessionMode,
                            onModeChange = { sessionMode = it }
                        )
                    }
                    
                    // Interest tags section
                    item {
                        InterestTagsSection(currentUser = currentUser)
                    }
                    
                    // Loading or content
                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = OrangePrimary)
                            }
                        }
                    } else if (filteredRecommendations.isEmpty()) {
                        item {
                            EmptyStateView()
                        }
                    } else {
                        items(filteredRecommendations) { recommendation ->
                            RecommendationCard(
                                recommendation = recommendation,
                                onClick = { onProfileClick(recommendation.id) }
                            )
                        }
                    }
                }
            } else {
                // Map View (Placeholder for now)
                MapViewPlaceholder(
                    sessionMode = sessionMode,
                    onModeChange = { sessionMode = it },
                    count = filteredRecommendations.size
                )
            }
        }
    }
}

@Composable
fun YellowBannerCard(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD54F)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Sessions pour vous",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                Text(
                    "Recommandations: $count",
                    fontSize = 14.sp,
                    color = Color.Black.copy(alpha = 0.7f)
                )
                Text(
                    "$count session(s) disponible(s)",
                    fontSize = 14.sp,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Default.ThumbUp,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun SessionModeToggle(
    sessionMode: SessionMode,
    onModeChange: (SessionMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            SessionModeButton(
                text = "En ligne",
                icon = Icons.Default.Computer,
                isSelected = sessionMode == SessionMode.ONLINE,
                onClick = { onModeChange(SessionMode.ONLINE) },
                modifier = Modifier.weight(1f)
            )
            SessionModeButton(
                text = "En personne",
                icon = Icons.Default.Person,
                isSelected = sessionMode == SessionMode.IN_PERSON,
                onClick = { onModeChange(SessionMode.IN_PERSON) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SessionModeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) OrangePrimary else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.Gray
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun InterestTagsSection(currentUser: com.skillswap.model.User?) {
    val interests = currentUser?.skillsLearn ?: listOf("Design", "Développement", "Marketing")
    
    Column {
        Text(
            "Basées sur vos intérêts",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(interests) { interest ->
                InterestChip(
                    text = interest
                )
            }
        }
    }
}

@Composable
fun InterestChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = OrangePrimary,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun RecommendationCard(
    recommendation: Recommendation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with avatar and info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        recommendation.initials,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (recommendation.age > 0) "${recommendation.mentorName}, ${recommendation.age}" 
                        else recommendation.mentorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Skills chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recommendation.skills.take(2).forEach { skill ->
                            Text(
                                skill.trim(),
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Description
            if (recommendation.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    recommendation.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
            }
            
            // Stats row
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (recommendation.distance.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            recommendation.distance,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        String.format("%.1f/5", recommendation.rating),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Text(
                    recommendation.lastActive,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    "${recommendation.sessionsCount} sessions",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            // Availability badge
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    recommendation.availability,
                    fontSize = 14.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Action buttons
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OrangePrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Voir profil", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangePrimary
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Réserver", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = OrangePrimary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Aucune session disponible",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Revenez plus tard pour découvrir de nouvelles sessions",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun MapViewPlaceholder(
    sessionMode: SessionMode,
    onModeChange: (SessionMode) -> Unit,
    count: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F5E9))
    ) {
        // Top controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.9f))
                .padding(16.dp)
        ) {
            SessionModeToggle(
                sessionMode = sessionMode,
                onModeChange = onModeChange
            )
            
            if (sessionMode == SessionMode.IN_PERSON) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tunis, Tunisie", fontSize = 14.sp)
                        }
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White
                    ) {
                        Text(
                            "$count session(s) à proximité",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
        
        // Map placeholder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = OrangePrimary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Vue carte",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
                Text(
                    "Intégration Google Maps à venir",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
