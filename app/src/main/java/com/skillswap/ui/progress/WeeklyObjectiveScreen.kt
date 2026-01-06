package com.skillswap.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.model.WeeklyObjective
import com.skillswap.viewmodel.WeeklyObjectiveViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.skillswap.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyObjectiveScreen(viewModel: WeeklyObjectiveViewModel = viewModel(), onBack: (() -> Unit)? = null) {
    LaunchedEffect(Unit) { viewModel.load() }
    val current by viewModel.current.collectAsState()
    val history by viewModel.history.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Objectif hebdo") },
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Filled.Close, contentDescription = "Retour")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
            message?.let {
                StatusBanner(text = it, background = Color(0xFFE6F4EA), content = Color(0xFF1B5E20)) { viewModel.clearMessage() }
            }
            error?.let {
                StatusBanner(text = it, background = Color(0xFFFFEDEC), content = Color(0xFFB3261E)) { viewModel.clearError() }
            }

            current?.let { objective ->
                ObjectiveCard(
                    objective = objective,
                    onToggleTask = { idx, done -> viewModel.toggleTask(idx, done) },
                    onComplete = { viewModel.completeObjective() },
                    onDelete = { showDeleteConfirmation = true }
                )
            } ?: run {
                Text(
                    "Aucun objectif en cours.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Button(
                    onClick = { showCreate = true },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) { Text("Créer un objectif") }
            }

            if (history.isNotEmpty()) {
                Text("Historique", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { item ->
                        HistoryRow(item.title, item.status, item.progressPercent)
                    }
                }
            } else if (!loading) {
                EmptyHistory()
            }
        }
    }

    if (showCreate) {
        CreateObjectiveDialog(
            onDismiss = { showCreate = false },
            onCreate = { title, hours, start, end, tasks ->
                viewModel.createObjective(title, hours, start, end, tasks)
                showCreate = false
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_goal)) },
            text = { Text(stringResource(R.string.delete_event_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteObjective()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun StatusBanner(text: String, background: Color, content: Color, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = background),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text, color = content, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = content)
            }
        }
    }
}

@Composable
private fun HistoryRow(title: String, status: String, progress: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(status, color = Color.Gray)
            }
            Text("$progress%", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
        }
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Aucun historique pour l'instant", color = Color.Gray)
    }
}
@Composable
private fun ObjectiveCard(
    objective: WeeklyObjective,
    onToggleTask: (Int, Boolean) -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(objective.title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${objective.completedHours}h / ${objective.targetHours}h (${objective.progressPercent}%)", color = Color.Gray)
            Spacer(Modifier.height(12.dp))
            objective.dailyTasks.forEachIndexed { index, task ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleTask(index, it) })
                    Column {
                        Text(task.day, fontWeight = FontWeight.SemiBold)
                        Text(task.task, color = Color.Gray)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onComplete) { Text("Terminer") }
                TextButton(onClick = onDelete) { Text("Supprimer", color = Color.Red) }
            }
        }
    }
}

@Composable
private fun CreateObjectiveDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int, String, String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetHours by remember { mutableStateOf("5") }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var tasksText by remember { mutableStateOf("") }
    var userGoal by remember { mutableStateOf("") }
    var aiSuggestion by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val tasks = tasksText.lines().filter { it.isNotBlank() }
                    onCreate(
                        title,
                        targetHours.toIntOrNull() ?: 0,
                        start,
                        end,
                        tasks
                    )
                },
                enabled = title.isNotBlank() && tasksText.lines().filter { it.isNotBlank() }.size == 7
            ) { Text("Créer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Nouvel objectif") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // AI Generation Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖 ", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                            Text(
                                "AI-Powered Planning",
                                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            "Enter your learning goal and let AI create a plan",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        
                        OutlinedTextField(
                            value = userGoal,
                            onValueChange = { userGoal = it },
                            label = { Text("Your Goal") },
                            placeholder = { Text("e.g., Learn Kotlin, Master Android...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Button(
                            onClick = {
                                isGenerating = true
                                aiError = null
                                scope.launch {
                                    try {
                                        val plan = com.skillswap.ai.GeminiAIService.generateWeeklyPlan(userGoal)
                                        title = plan.title
                                        targetHours = plan.hours.toString()
                                        aiSuggestion = plan.suggestion
                                        tasksText = plan.tasks.joinToString("\n")
                                    } catch (e: Exception) {
                                        aiError = e.message ?: "AI generation failed"
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            },
                            enabled = !isGenerating && userGoal.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A8A8)
                            )
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Generating...")
                            } else {
                                Text("✨ Generate with AI")
                            }
                        }
                        
                        if (aiSuggestion.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("💡 ", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                    Text(
                                        aiSuggestion,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6D4C00)
                                    )
                                }
                            }
                        }
                        
                        if (aiError != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("⚠️ ", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                    Text(
                                        aiError!!,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFC62828)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Manual Entry Section
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetHours,
                    onValueChange = { targetHours = it },
                    label = { Text("Heures cibles") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Début (ISO)") },
                    placeholder = { Text("2024-12-13T00:00:00.000Z") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("Fin (ISO)") },
                    placeholder = { Text("2024-12-20T00:00:00.000Z") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tasksText,
                    onValueChange = { tasksText = it },
                    label = { Text("Tâches (7 lignes - 1 par jour)") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    maxLines = 7
                )
            }
        }
    )
}
