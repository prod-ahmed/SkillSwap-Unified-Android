package com.skillswap.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.skillswap.ui.theme.OrangeGradientEnd
import com.skillswap.ui.theme.OrangeGradientStart
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.ProfileViewModel
import com.skillswap.auth.AuthenticationManager

import androidx.navigation.NavController

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent

import com.skillswap.Screen

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val authManager = remember { AuthenticationManager.getInstance(context) }
    var showReferralModal by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }
    
    val user by viewModel.user.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 80.dp) // Add bottom padding for navigation bar
    ) {
        // Header Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                         Brush.linearGradient(
                            colors = listOf(OrangeGradientStart, OrangeGradientEnd)
                        )
                    )
            )
            
            // Edit Profile Icon Button
            IconButton(
                onClick = { navController.navigate(Screen.ProfileEdit.route) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Modifier le profil",
                    tint = Color.White
                )
            }
            
            // Avatar
            Box(
                 modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-30).dp)
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp) // Border
                    .clip(CircleShape)
                    .background(OrangePrimary.copy(alpha = 0.2f)),
                 contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user?.username?.take(1)?.uppercase() ?: "U",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
            }
        }
        
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = user?.username ?: "Utilisateur",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = "Localisation", tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(user?.location?.city ?: "Localisation inconnue", color = Color.Gray)
            }
            
            Text(
                user?.bio ?: "Aucune bio renseignée",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard("XP", "${user?.credits ?: 0}", Modifier.weight(1f))
                StatCard("Crédits", "${user?.credits ?: 0}", Modifier.weight(1f))
                StatCard("Note", String.format("%.1f", user?.ratingAvg ?: 0.0), Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileActionButton("target", "Objectif Hebdo", Color(0xFF00A8A8)) {
                    navController.navigate("weekly_objective")
                }
                ProfileActionButton("trophy", "Récompenses", Color(0xFFFFD700)) {
                    navController.navigate("rewards")
                }
                ProfileActionButton("sparkles", "Sessions pour vous", Color.Magenta) {
                    navController.navigate("sessions_pour_vous")
                }
                ProfileActionButton("gift.fill", "Parrainer un ami", OrangePrimary) {
                    showReferralModal = true
                }
                ProfileActionButton("book.closed", "Quiz AI", Color(0xFF6A4CFF)) {
                    navController.navigate("quizzes")
                }
                ProfileActionButton("bell.fill", "Notifications", Color(0xFFFF6B35)) {
                    navController.navigate("notifications")
                }
                ProfileActionButton("calendar", "Calendrier", Color(0xFF2196F3)) {
                    navController.navigate("calendar")
                }
                
                // New Features
                ProfileActionButton("tag.fill", "Mes Promos", Color(0xFFFF2D55)) {
                    navController.navigate("my_promos")
                }
                ProfileActionButton("megaphone.fill", "Mes Annonces", Color.Green) {
                    navController.navigate("my_annonces")
                }
                ProfileActionButton("shield", "Modération", Color(0xFF6A1B9A)) {
                    navController.navigate("moderation")
                }

                ProfileActionButton("gearshape.fill", "Paramètres", Color.Gray) {
                    navController.navigate("settings")
                }
                
                ProfileActionButton("pencil", "Modifier le profil", Color.Blue) {
                    navController.navigate(Screen.ProfileEdit.route)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = { 
                        authManager.logout()
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
                ) {
                    Text("Déconnexion")
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Section("Compétences enseignées") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (user?.skillsTeach.isNullOrEmpty()) {
                        Text("Aucune compétence", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        user?.skillsTeach?.forEach { skill ->
                            TagChip(skill, OrangePrimary)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Section("Compétences en apprentissage") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (user?.skillsLearn.isNullOrEmpty()) {
                        Text("Aucune compétence", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        user?.skillsLearn?.forEach { skill ->
                            TagChip(skill, Color(0xFF008080))
                        }
                    }
                }
            }
            
             Spacer(Modifier.height(24.dp))
             
             // Share Profile Button
             Button(
                onClick = { 
                    val shareIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, 
                            "Rejoignez-moi sur SkillSwap ! Utilisateur: ${user?.username ?: ""}")
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Partager mon profil"))
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
             ) {
                 Icon(Icons.Default.Share, contentDescription = "Partager le profil")
                 Spacer(Modifier.width(8.dp))
                 Text("Partager mon profil")
             }
             
             Spacer(Modifier.height(32.dp))
        }
    }
    
    // Inline Referral Modal
    if (showReferralModal) {
        val referralCode = user?.referralCode ?: "SKILL${user?.id?.take(6)?.uppercase() ?: "XXX"}"
        
        AlertDialog(
            onDismissRequest = { showReferralModal = false },
            title = { 
                Text(
                    "🎁 Parrainer un ami",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Partagez votre code et gagnez des récompenses !",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    
                    // Code display
                    Surface(
                        color = OrangePrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                referralCode,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Referral Code", referralCode))
                            }) {
                                Icon(Icons.Default.Code, "Copier", tint = OrangePrimary)
                            }
                        }
                    }
                    
                    // Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("0", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("Parrainés", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("0", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("XP gagnés", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Rejoins SkillSwap et utilise mon code: $referralCode pour obtenir des bonus!")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Partager"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Partager")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReferralModal = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun TagChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ProfileActionButton(
    icon: String,
    title: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                // Simplified icon handling
                val imageVector = when(icon) {
                    "target" -> androidx.compose.material.icons.Icons.Default.Star
                    "trophy" -> androidx.compose.material.icons.Icons.Default.EmojiEvents
                    "sparkles" -> androidx.compose.material.icons.Icons.Default.Face
                    "gift.fill" -> androidx.compose.material.icons.Icons.Default.CardGiftcard
                    "tag.fill" -> Icons.AutoMirrored.Filled.Label
                    "megaphone.fill" -> androidx.compose.material.icons.Icons.Default.Campaign
                    "bell.fill" -> androidx.compose.material.icons.Icons.Default.Notifications
                    "shield" -> androidx.compose.material.icons.Icons.Default.Shield
                    "gearshape.fill" -> androidx.compose.material.icons.Icons.Default.Settings
                    "pencil" -> androidx.compose.material.icons.Icons.Default.Edit
                    else -> androidx.compose.material.icons.Icons.Default.Circle
                }
                Icon(imageVector, contentDescription = "Action profil", tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(title, color = Color.Black, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ouvrir", tint = Color.LightGray)
        }
    }
}
