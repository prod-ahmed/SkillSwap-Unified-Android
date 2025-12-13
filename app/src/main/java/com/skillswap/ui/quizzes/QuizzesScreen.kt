package com.skillswap.ui.quizzes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.skillswap.model.QuizQuestion
import com.skillswap.model.QuizResult
import com.skillswap.viewmodel.QuizViewModel

@Composable
fun QuizzesScreen(viewModel: QuizViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var showHistory by remember { mutableStateOf(false) }
    var subjectInput by remember { mutableStateOf(state.subject) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Header(subject = state.subject, onHistory = { showHistory = true })

        InfoBanner(text = "Les quiz seront disponibles dès que le backend sera exposé. Aucune question générée localement pour éviter les données fictives.")

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sujet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = subjectInput,
                    onValueChange = { subjectInput = it },
                    label = { Text("Ex: Swift, UI/UX, Histoire") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.setSubject(subjectInput) },
                    enabled = subjectInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Valider le sujet") }
                Text("Niveau débloqué: ${state.unlockedLevel}", color = Color.Gray)
            }
        }

        Text("Parcours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LevelRoadmap(
            unlockedLevel = state.unlockedLevel,
            onLevel = { level -> viewModel.startLevel(level) }
        )

        state.currentLevel?.let { level ->
            if (state.questions.isNotEmpty()) {
            QuizDialog(
                level = level,
                question = state.questions[state.currentIndex.coerceAtMost(state.questions.lastIndex)],
                index = state.currentIndex,
                total = state.questions.size,
                score = state.score,
                finished = state.finished,
                onAnswer = { viewModel.answer(it) },
                onDismiss = { viewModel.resetQuiz() }
            )
            }
        }
    }

    if (showHistory) {
        HistoryDialog(history = state.history, onDismiss = { showHistory = false })
    }
}

@Composable
private fun InfoBanner(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8A5300),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun Header(subject: String, onHistory: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF6A4CFF), Color(0xFF9C6BFF)))
            )
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AI Quiz Roadmap", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onHistory) {
                    Icon(Icons.Default.History, contentDescription = "Historique de quiz", tint = Color.White)
                }
            }
            Text(
                if (subject.isBlank()) "Choisissez un sujet pour démarrer" else "Sujet: $subject",
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun LevelRoadmap(unlockedLevel: Int, onLevel: (Int) -> Unit) {
    val levels = (1..10).toList()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(levels) { level ->
            val isUnlocked = level <= unlockedLevel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) { },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE5E5EA)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔒", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Niveau $level", fontWeight = FontWeight.Bold)
                        Text("En attente du backend", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizDialog(
    level: Int,
    question: QuizQuestion,
    index: Int,
    total: Int,
    score: Int,
    finished: Boolean,
    onAnswer: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Niveau $level") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Question ${index + 1} / $total")
                Text(question.question, fontWeight = FontWeight.Bold)
                question.options.forEachIndexed { idx, opt ->
                    OutlinedButton(
                        onClick = { onAnswer(idx) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(opt) }
                }
                Text("Score: $score", color = Color.Gray)
                if (finished) {
                    Text("Terminé !", fontWeight = FontWeight.Bold, color = Color(0xFF34C759))
                    TextButton(onClick = onDismiss) { Text("Fermer") }
                }
            }
        }
    )
}

@Composable
private fun HistoryDialog(history: List<QuizResult>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
        title = { Text("Historique") },
        text = {
            if (history.isEmpty()) {
                Text("Aucun quiz effectué.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.forEach {
                        Text("${it.subject} - L${it.level} - ${it.score}/${it.totalQuestions}")
                    }
                }
            }
        }
    )
}
