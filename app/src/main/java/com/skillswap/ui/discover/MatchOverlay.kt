package com.skillswap.ui.discover

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.skillswap.model.User
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.ui.theme.OrangeGradientStart
import com.skillswap.ui.theme.OrangeGradientEnd
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MatchOverlay(
    user: User,
    onDismiss: () -> Unit,
    onMessage: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
        ) {
            // Floating Hearts Animation
            FloatingHearts()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "C'est un Match ! 🎉",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Vous et ${user.username} êtes intéressés l'un par l'autre.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Avatar with glow
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(OrangeGradientStart, OrangeGradientEnd)
                                )
                            )
                    )
                    
                    if (!user.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                user.username.take(1).uppercase(),
                                fontSize = 60.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    user.username,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Buttons
                Button(
                    onClick = onMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.ChatBubble,
                        contentDescription = null,
                        tint = OrangePrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Envoyer un message",
                        color = OrangePrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        "Continuer à découvrir",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingHearts() {
    val hearts = remember { List(15) { Random.nextFloat() } }
    
    Box(modifier = Modifier.fillMaxSize()) {
        hearts.forEach { randomStart ->
            AnimatedHeart(randomStart)
        }
    }
}

@Composable
fun AnimatedHeart(randomStart: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "heart")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 1000f,
        targetValue = -200f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000 + (randomStart * 2000).toInt(),
                easing = LinearEasing
            ),
            initialStartOffset = StartOffset((randomStart * 3000).toInt())
        ),
        label = "y"
    )
    
    val xOffset = remember { (randomStart - 0.5f) * 500f }
    val scale = remember { 0.5f + randomStart }
    val color = remember { 
        listOf(Color(0xFFFF6B6B), Color(0xFFFF8E8E), Color(0xFFFFD166)).random() 
    }

    Icon(
        Icons.Default.Favorite,
        contentDescription = null,
        tint = color.copy(alpha = 0.6f),
        modifier = Modifier
            .offset(x = xOffset.dp, y = yOffset.dp)
            .scale(scale)
            .size(32.dp)
    )
}
