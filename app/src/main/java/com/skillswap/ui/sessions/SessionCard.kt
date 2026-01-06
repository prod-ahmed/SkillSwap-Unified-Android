package com.skillswap.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.skillswap.model.Session
import com.skillswap.ui.components.TagChip
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionCard(
    session: Session,
    isCreator: Boolean,
    currentUserId: String,
    onCardClick: () -> Unit,
    onPostpone: () -> Unit = {},
    onProposeReschedule: () -> Unit = {},
    onRate: () -> Unit = {},
    onShowPlan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        session.status == "completed" -> Color(0xFF4CAF50)
        session.status == "postponed" -> Color(0xFFFF9800)
        session.status == "cancelled" -> Color(0xFFF44336)
        else -> Color(0xFF2196F3)
    }
    
    val statusText = when {
        session.status == "completed" -> "Terminée"
        session.status == "postponed" -> "Reportée"
        session.status == "cancelled" -> "Annulée"
        else -> "À venir"
    }
    
    Card(
        onClick = onCardClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (session.mode == "online") Icons.Default.VideoCall else Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = session.title.ifEmpty { "Session sans titre" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }
                
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Date & Time Row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatSessionDate(session.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatSessionTime(session.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "• ${session.duration} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Skills Tags
            if (!session.skills.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    session.skills.take(3).forEach { skill ->
                        TagChip(
                            text = skill,
                            color = Color(0xFF12947D),
                            removable = false
                        )
                    }
                    if (session.skills.size > 3) {
                        TagChip(
                            text = "+${session.skills.size - 3}",
                            color = Color.Gray,
                            removable = false
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            
            // Members Avatars (Stacked)
            if (session.members.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        session.members.take(5).forEachIndexed { index, member ->
                            MemberAvatar(
                                imageUrl = member.displayImage,
                                initials = member.username.take(2).uppercase(),
                                modifier = Modifier.offset(x = (index * 20).dp)
                            )
                        }
                    }
                    Spacer(Modifier.width((session.members.take(5).size * 20 + 20).dp))
                    Text(
                        text = if (session.members.size > 5) {
                            "${session.members.size} participants"
                        } else {
                            "${session.members.size} participant${if (session.members.size > 1) "s" else ""}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            
            // Location/Link
            if (session.mode == "online" && !session.link.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Lien de réunion disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2196F3)
                    )
                }
            } else if (session.mode == "in-person" && !session.location.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color(0xFF5C52BF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = session.location ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }
            
            // Action Buttons (if any)
            if (isCreator || session.status == "completed" || session.status == "upcoming") {
                Spacer(Modifier.height(12.dp))
                Divider(color = Color(0xFFE0E0E0))
                Spacer(Modifier.height(8.dp))
                
                // AI Plan Generator CTA (prominent for upcoming sessions)
                if (session.status == "upcoming") {
                    Button(
                        onClick = onShowPlan,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800).copy(alpha = 0.1f),
                            contentColor = Color(0xFFFF9800)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Générer un plan avec IA", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (session.status == "completed") {
                        TextButton(
                            onClick = onRate,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Évaluer")
                        }
                    }
                    
                    if (isCreator && session.status == "upcoming") {
                        TextButton(
                            onClick = onProposeReschedule,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Replanifier")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberAvatar(
    imageUrl: String?,
    initials: String,
    modifier: Modifier = Modifier
) {
    if (!imageUrl.isNullOrEmpty()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .background(Color(0xFFFF9800).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFF9800),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatSessionDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString)
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}

private fun formatSessionTime(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString)
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        ""
    }
}
