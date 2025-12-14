package com.skillswap.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.model.BadgeItem
import com.skillswap.model.ProgressGoalItem
import com.skillswap.model.SkillProgressItem
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.ProgressViewModel

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }
    
    val dashboard by viewModel.dashboard.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var showGoalDialog by remember { mutableStateOf(false) }
    
    if (dashboard == null && isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF2F2F7)) // System Grouped Background
        ) {
            dashboard?.let { data ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF00A8A8), Color(0xFF00B8B8))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                       Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                           StatTile("Cette semaine", "${data.stats.weeklyHours}h")
                           Spacer(Modifier.width(12.dp))
                           StatTile("Compétences", "${data.stats.skillsCount}")
                       }
                       Spacer(Modifier.height(16.dp))
                       
                       Row(verticalAlignment = Alignment.CenterVertically) {
                           Column {
                               Text("XP total", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                               Text("${data.xpSummary.xp}", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                           }
                           Spacer(Modifier.weight(1f))
                           Column(horizontalAlignment = Alignment.End) {
                               data.xpSummary.nextBadge?.let { next ->
                                   Text("Prochain badge", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                                   Text(next.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                   Text("À ${next.threshold} XP", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                               }
                           }
                       }
                    }
                }
                
                // Goals
                SectionHeader("Objectifs")
                if (error != null) {
                    Text(error ?: "", color = Color.Red, modifier = Modifier.padding(horizontal = 16.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Suivez vos efforts hebdo", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    TextButton(onClick = { showGoalDialog = true }) {
                        Text("Créer un objectif")
                    }
                }
                Column(Modifier.padding(horizontal = 16.dp)) {
                    if (data.goals.isEmpty()) {
                        EmptyCard("Aucun objectif", "Ajoutez un objectif pour suivre vos progrès") {
                            showGoalDialog = true
                        }
                    } else {
                        data.goals.forEach { goal ->
                            GoalCard(goal, onDelete = { viewModel.deleteGoal(goal.id) })
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                
                // Weekly Activity
                SectionHeader("Activité de la semaine")
                Box(
                     Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Color.White, RoundedCornerShape(28.dp))
                        .padding(16.dp)
                        .height(180.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val max = data.weeklyActivity.maxOfOrNull { it.hours }?.coerceAtLeast(1.0) ?: 1.0
                        data.weeklyActivity.forEach { point ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val heightRatio = ((point.hours ?: 0.0) / max).toFloat().coerceIn(0.1f, 1f)
                                 Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .fillMaxHeight(heightRatio)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color(0xFFFFB347), Color(0xFFFF6B35))
                                            )
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(point.day, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }
                
                // Skill Progress
                SectionHeader("Compétences en cours")
                Column(Modifier.padding(horizontal = 16.dp)) {
                    data.skillProgress.forEach { skill ->
                        SkillRow(skill)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                
                // Badges
                SectionHeader("Badges")
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (data.badges.isEmpty()) {
                        Text("Aucun badge pour le moment", color = Color.Gray, modifier = Modifier.padding(8.dp))
                    } else {
                        data.badges.take(3).forEach { badge ->
                            BadgeCard(badge, Modifier.weight(1f))
                            if (badge != data.badges.last()) Spacer(Modifier.width(8.dp))
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
            } ?: Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tableau de bord indisponible", fontWeight = FontWeight.Bold)
                    Text("Rechargez pour récupérer vos données", color = Color.Gray)
                    TextButton(onClick = { viewModel.loadDashboard() }) { Text("Rafraîchir") }
                }
            }
        }
    }

    if (showGoalDialog) {
        GoalDialog(
            onDismiss = { showGoalDialog = false },
            onCreate = { title, target, period, due ->
                viewModel.createGoal(title, target, period, due)
                showGoalDialog = false
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title, 
        style = MaterialTheme.typography.titleMedium, 
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun EmptyCard(title: String, subtitle: String, onAction: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAction) { Text("Ajouter") }
        }
    }
}

@Composable
fun StatTile(title: String, value: String) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun GoalCard(goal: ProgressGoalItem, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(goal.title, fontWeight = FontWeight.Bold)
                Surface(
                    color = if(goal.status == "completed") Color.Green.copy(alpha=0.1f) else OrangePrimary.copy(alpha=0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if(goal.period == "week") "Hebdo" else "Mensuel", 
                        modifier = Modifier.padding(horizontal=8.dp, vertical=4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (goal.progressPercent ?: 0) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = OrangePrimary,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(4.dp))
            Text("${goal.currentHours}h / ${goal.targetHours}h", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                Text("Supprimer")
            }
        }
    }
}

@Composable
fun SkillRow(item: SkillProgressItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                Column(Modifier.weight(1f)) {
                    Text(item.skill, fontWeight = FontWeight.Bold)
                    Text("${item.hours}h • ${item.level}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { item.progress / 100f },
                        modifier = Modifier.size(40.dp),
                        color = OrangePrimary,
                        trackColor = Color.LightGray.copy(alpha=0.3f)
                    )
                Text("${item.progress}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BadgeCard(badge: BadgeItem, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White), // Simplify color parsing
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(badge.displayIcon, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(4.dp))
            Text(badge.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GoalDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Double, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("2") }
    var period by remember { mutableStateOf("week") }
    var dueDate by remember { mutableStateOf("") }
    val canCreate = title.isNotBlank() && (target.toDoubleOrNull() ?: 0.0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                onCreate(
                    title,
                    target.toDoubleOrNull() ?: 1.0,
                    period,
                    dueDate.ifBlank { null }
                )
            },
                enabled = canCreate
            ) { Text("Créer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Nouvel objectif") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") })
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Heures cibles") })
                OutlinedTextField(value = period, onValueChange = { period = it }, label = { Text("Période (week/month)") })
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Échéance (optionnel)") })
            }
        }
    )
}
