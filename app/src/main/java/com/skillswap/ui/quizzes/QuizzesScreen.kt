package com.skillswap.ui.quizzes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.data.QuizQuestion
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizzesScreen(
    viewModel: QuizViewModel = viewModel()
) {
    val isGenerating by viewModel.isGenerating.collectAsState()
    val quizQuestions by viewModel.quizQuestions.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val selectedAnswers by viewModel.selectedAnswers.collectAsState()
    val showResults by viewModel.showResults.collectAsState()
    val score by viewModel.score.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val unlockedLevel by viewModel.unlockedLevel.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    
    var selectedSubject by remember { mutableStateOf("") }
    var showRoadmap by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quizzes IA") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangePrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (quizQuestions.isEmpty() && !isGenerating) {
                // Quiz setup screen
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE082))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = OrangePrimary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Quizzes générés par IA",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Testez vos connaissances",
                                        fontSize = 14.sp,
                                        color = Color.Black.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        Text(
                            "Choisissez un sujet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    items(subjects.size) { index ->
                        val subject = subjects[index]
                        SubjectCard(
                            subject = subject,
                            isSelected = selectedSubject == subject,
                            onClick = { selectedSubject = subject }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Niveau de difficulté: ${selectedLevel ?: 1}/10",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    item {
                        LevelSlider(
                            level = selectedLevel ?: 1,
                            onLevelChange = { viewModel.selectLevel(it) }
                        )
                    }
                    
                    item {
                        Button(
                            onClick = {
                                viewModel.generateQuiz(selectedSubject, selectedLevel ?: 1)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Générer le quiz", fontSize = 16.sp)
                        }
                    }
                    
                    errorMessage?.let { error ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                            ) {
                                Text(
                                    error,
                                    modifier = Modifier.padding(16.dp),
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                }
            } else if (isGenerating) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = OrangePrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Génération du quiz en cours...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Cela peut prendre quelques secondes",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else if (showResults) {
                // Results screen
                ResultsScreen(
                    score = score,
                    totalQuestions = quizQuestions.size,
                    onRetry = { viewModel.resetQuiz() }
                )
            } else {
                // Quiz questions screen
                QuizQuestionsScreen(
                    questions = quizQuestions,
                    currentIndex = currentQuestionIndex,
                    selectedAnswers = selectedAnswers,
                    onAnswerSelected = { index -> viewModel.selectAnswer(index) },
                    onNext = { viewModel.nextQuestion() },
                    onSubmit = { viewModel.submitQuiz(selectedSubject, selectedLevel ?: 1) }
                )
            }
        }
    }
}

@Composable
fun SubjectCard(
    subject: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) OrangePrimary.copy(alpha = 0.1f) else Color.White
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, OrangePrimary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = if (isSelected) OrangePrimary else Color.Gray
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                subject,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) OrangePrimary else Color.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = OrangePrimary
                )
            }
        }
    }
}

@Composable
fun LevelSlider(
    level: Int,
    onLevelChange: (Int) -> Unit
) {
    Column {
        Slider(
            value = level.toFloat(),
            onValueChange = { onLevelChange(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = OrangePrimary,
                activeTrackColor = OrangePrimary
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Débutant", fontSize = 12.sp, color = Color.Gray)
            Text("Intermédiaire", fontSize = 12.sp, color = Color.Gray)
            Text("Expert", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun QuizQuestionsScreen(
    questions: List<QuizQuestion>,
    currentIndex: Int,
    selectedAnswers: Map<Int, Int>,
    onAnswerSelected: (Int) -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    if (currentIndex >= questions.size) return
    
    val question = questions[currentIndex]
    val selectedAnswer = selectedAnswers[currentIndex]
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = (currentIndex + 1) / questions.size.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            color = OrangePrimary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Question ${currentIndex + 1}/${questions.size}",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OrangePrimary.copy(alpha = 0.1f))
        ) {
            Text(
                question.question,
                modifier = Modifier.padding(16.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        question.options.forEachIndexed { index, option ->
            AnswerOption(
                text = option,
                index = index,
                isSelected = selectedAnswer == index,
                onClick = { onAnswerSelected(index) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = {
                if (currentIndex == questions.size - 1) {
                    onSubmit()
                } else {
                    onNext()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedAnswer != null,
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
        ) {
            Text(
                if (currentIndex == questions.size - 1) "Terminer" else "Suivant",
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun AnswerOption(
    text: String,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) OrangePrimary else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = if (isSelected) OrangePrimary else Color.LightGray
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text,
                fontSize = 16.sp,
                color = if (isSelected) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun ResultsScreen(
    score: Int,
    totalQuestions: Int,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = OrangePrimary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Résultat final",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OrangePrimary.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "$score / $totalQuestions",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
                Text(
                    "${(score * 100 / totalQuestions)}% de réussite",
                    fontSize = 18.sp,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Nouveau quiz", fontSize = 16.sp)
        }
    }
}
