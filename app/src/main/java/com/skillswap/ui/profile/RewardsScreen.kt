package com.skillswap.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.ui.theme.OrangeGradientStart
import com.skillswap.ui.theme.OrangeGradientEnd

import com.skillswap.viewmodel.RewardsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(navController: NavController, viewModel: RewardsViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        viewModel.loadRewards()
    }
    
    val user by viewModel.user.collectAsState()
    val referralsData by viewModel.referralsData.collectAsState()
    
    val totalPoints = user?.xp ?: 0
    val level = (totalPoints / 500) + 1
    val referralsCount = referralsData?.inviterReferrals?.size ?: 0
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Récompenses") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangePrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Points Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(OrangeGradientStart, OrangeGradientEnd)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "$totalPoints Points",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Niveau $level",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Stats Section
            Text(
                "Vos statistiques",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RewardStatCard(
                    icon = Icons.Default.CardGiftcard,
                    title = "Parrainages",
                    value = "$referralsCount",
                    modifier = Modifier.weight(1f)
                )
                RewardStatCard(
                    icon = Icons.Default.Star,
                    title = "Sessions",
                    value = "${user?.credits ?: 0}", // Using credits as proxy for now
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Achievements Section
            Text(
                "Badges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AchievementItem(
                        icon = "🎓",
                        title = "Premier pas",
                        description = "Complétez votre profil",
                        isUnlocked = true
                    )
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    AchievementItem(
                        icon = "🌟",
                        title = "Mentor actif",
                        description = "Donnez 10 sessions",
                        isUnlocked = true
                    )
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    AchievementItem(
                        icon = "🎯",
                        title = "Objectif atteint",
                        description = "Complétez 5 objectifs hebdomadaires",
                        isUnlocked = false
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // How to earn points
            Text(
                "Comment gagner des points",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PointsEarnItem("Complétez une session", "+50 points")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    PointsEarnItem("Parrainez un ami", "+100 points")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    PointsEarnItem("Recevez 5 étoiles", "+25 points")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    PointsEarnItem("Objectif hebdo complété", "+75 points")
                }
            }
        }
    }
}

@Composable
fun RewardStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun AchievementItem(
    icon: String,
    title: String,
    description: String,
    isUnlocked: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isUnlocked) OrangePrimary.copy(alpha = 0.1f)
                    else Color.Gray.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                icon,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.alpha(if (isUnlocked) 1f else 0.3f)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isUnlocked) Color.Black else Color.Gray
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        if (isUnlocked) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Débloqué",
                tint = OrangePrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PointsEarnItem(activity: String, points: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            activity,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            points,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = OrangePrimary
        )
    }
}

@Composable
private fun Modifier.alpha(alpha: Float): Modifier = this.graphicsLayer { this.alpha = alpha }
