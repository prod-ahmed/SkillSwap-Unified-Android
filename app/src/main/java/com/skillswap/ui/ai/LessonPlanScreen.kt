package com.skillswap.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.model.LessonPlan
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.LessonPlanViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlanScreen(
    sessionId: String,
    isTeacher: Boolean,
    onBack: () -> Unit,
    viewModel: LessonPlanViewModel = viewModel()
) {
    val lessonPlan by viewModel.lessonPlan.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    
    LaunchedEffect(sessionId) {
        viewModel.loadLessonPlan(sessionId)
    }
    
    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessages() },
            title = { Text("Erreur") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessages() }) {
                    Text("OK")
                }
            }
        )
    }
    
    successMessage?.let { success ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessages() },
            title = { Text("Succès") },
            text = { Text(success) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessages() }) {
                    Text("OK")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan de cours") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    if (isTeacher && lessonPlan != null) {
                        TextButton(
                            onClick = {
                                viewModel.regenerateLessonPlan(sessionId)
                            }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = OrangePrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Régénérer",
                                color = OrangePrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = OrangePrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Génération du plan de cours...",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                lessonPlan != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LessonPlanHeader(lessonPlan!!)
                        
                        LessonPlanTabs(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                        
                        when (selectedTab) {
                            0 -> LessonPlanOutlineView(lessonPlan!!.plan)
                            1 -> LessonPlanChecklistView(
                                checklist = lessonPlan!!.checklist,
                                progress = lessonPlan!!.progress,
                                sessionId = sessionId,
                                viewModel = viewModel
                            )
                            2 -> LessonPlanResourcesView(lessonPlan!!.resources)
                            3 -> LessonPlanHomeworkView(lessonPlan!!.homework)
                        }
                    }
                }
                
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Aucun plan de cours",
                            fontSize = 20.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.generateLessonPlan(sessionId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Générer le plan de cours")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonPlanHeader(plan: LessonPlan) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrangePrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Plan de cours IA",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                plan.createdAt?.let { dateStr ->
                    val formattedDate = remember(dateStr) {
                        try {
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            val date = inputFormat.parse(dateStr)
                            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
                            "Généré le ${outputFormat.format(date ?: Date())}"
                        } catch (e: Exception) {
                            "Généré récemment"
                        }
                    }
                    Text(
                        formattedDate,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun LessonPlanTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(
        Triple("Plan", Icons.Default.List, 0),
        Triple("Étapes", Icons.Default.CheckCircle, 1),
        Triple("Ressources", Icons.Default.Folder, 2),
        Triple("Devoirs", Icons.Default.School, 3)
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEach { (title, icon, index) ->
                val isSelected = selectedTab == index
                val textColor = if (isSelected) OrangePrimary else Color.Gray
                val backgroundColor = if (isSelected) OrangePrimary.copy(alpha = 0.1f) else Color.Transparent
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(index) }
                        .background(backgroundColor)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = textColor
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(OrangePrimary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LessonPlanOutlineView(plan: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = plan,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun LessonPlanChecklistView(
    checklist: List<String>,
    progress: Map<String, Boolean>,
    sessionId: String,
    viewModel: LessonPlanViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Objectifs de la session",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        checklist.forEachIndexed { index, item ->
            val isCompleted = progress["$index"] == true
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = {
                        viewModel.updateProgress(sessionId, index, !isCompleted)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (isCompleted) Color(0xFF4CAF50) else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "${index + 1}. $item",
                    fontSize = 14.sp,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    color = if (isCompleted) Color.Gray else Color.Black,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LessonPlanResourcesView(resources: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Ressources",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        resources.forEach { resource ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(OrangePrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = OrangePrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            resource,
                            fontSize = 14.sp,
                            maxLines = 2
                        )
                        Text(
                            "LINK",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = OrangePrimary
                    )
                }
            }
        }
    }
}

@Composable
fun LessonPlanHomeworkView(homework: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Devoirs",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        Text(
            text = homework,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}
