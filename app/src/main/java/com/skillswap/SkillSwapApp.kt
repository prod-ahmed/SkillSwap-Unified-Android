package com.skillswap

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skillswap.ui.auth.AuthScreen
import com.skillswap.ui.call.VideoCallScreen
import com.skillswap.ui.chat.ChatScreen
import com.skillswap.ui.chat.ConversationsScreen
import com.skillswap.ui.profile.ProfileScreen
import com.skillswap.viewmodel.CallViewModel
import com.skillswap.ui.promos.MyPromosScreenContent
import com.skillswap.ui.annonces.MyAnnoncesScreenContent
import com.skillswap.ui.progress.ProgressScreen
import com.skillswap.ui.sessions.SessionsScreen
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.ui.theme.SkillSwapTheme
import com.skillswap.ui.profile.ReferralScreen
import com.skillswap.ui.profile.ReferralCodeGenerationScreen
import com.skillswap.ui.auth.OnboardingScreen
import com.skillswap.ui.auth.ProfileSetupScreen
import com.skillswap.ui.progress.WeeklyObjectiveScreen
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import com.skillswap.network.ChatSocketClient
import com.skillswap.auth.AuthenticationManager
import com.skillswap.util.LocalizationManager
import com.skillswap.util.ThemeManager
import com.skillswap.util.DeepLinkHandler

sealed class Screen(val route: String, val title: String, val icon: String) {
    object Discover : Screen("discover", "Découvrir", "house.fill")
    object Messages : Screen("messages", "Messages", "message.fill")
    object Sessions : Screen("sessions", "Sessions", "calendar")
    object Progress : Screen("progress", "Progrès", "chart.bar.fill")
    object Map     : Screen("map", "Carte", "map.fill")
    object Profile : Screen("profile", "Profil", "person.fill")
    object WeeklyObjective : Screen("weekly_objective", "Objectif", "")
    object Quizzes : Screen("quizzes", "Quiz", "")
    object Notifications : Screen("notifications", "Notifications", "bell.fill")
    object Moderation : Screen("moderation", "Modération", "")
    object Login : Screen("login", "Login", "")
    object ChatDetail : Screen("chat/{conversationId}", "Chat", "") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object MyPromos : Screen("my_promos", "Mes Promos", "")
    object MyAnnonces : Screen("my_annonces", "Mes Annonces", "")
    object ReferralCodeGeneration : Screen("referral_code_generation", "Code Parrainage", "")
    object ProfileEdit : Screen("profile_edit", "Modifier Profil", "")
}
@Composable
fun RowScope.CustomBottomNavigationItem(
    screen: Screen,
    isSelected: Boolean,
    activeColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            contentColor = if (isSelected) activeColor else Color.Gray,
            containerColor = if (isSelected) activeColor.copy(alpha = 0.12f) else Color.Transparent
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp, horizontal = 0.dp),
        modifier = Modifier.defaultMinSize(minWidth = 1.dp).weight(1f)
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = screen.title,
                modifier = Modifier.size(20.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = screen.title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
            )
        }
    }
}

@Composable
fun SkillSwapApp() {
    val context = LocalContext.current
    
    // Initialize managers
    val authManager = remember { AuthenticationManager.getInstance(context) }
    val localizationManager = remember { LocalizationManager.getInstance(context) }
    val themeManager = remember { ThemeManager.getInstance(context) }
    
    // Initialize Cloudflare AI Workers
    LaunchedEffect(Unit) {
        try {
            val cloudflareAccountId = BuildConfig.CLOUDFLARE_ACCOUNT_ID
            val cloudflareApiKey = BuildConfig.CLOUDFLARE_WORKERS_AI_API_KEY
            if (cloudflareAccountId.isNotBlank() && cloudflareApiKey.isNotBlank()) {
                com.skillswap.ai.CloudflareAIService.initialize(cloudflareAccountId, cloudflareApiKey)
            }
        } catch (e: Exception) {
            // Silently fail if keys are not configured
        }
    }
    
    // Observe layout direction for RTL support
    val currentLanguage by localizationManager.currentLanguage
    val layoutDirection = localizationManager.layoutDirection
    
    // Initialize global socket listener for notifications
    LaunchedEffect(Unit) {
        val token = authManager.getToken()
        if (!token.isNullOrEmpty()) {
            ChatSocketClient.getInstance(context).connect()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        SkillSwapTheme {
            val navController = rememberNavController()
            var showBottomBar by remember { mutableStateOf(false) }
            val callViewModel: CallViewModel = viewModel()
            val startDestination = remember {
                val prefs = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
                val hasSeenOnboarding = prefs.getBoolean("onboarding_done", false)
                val hasProfile = prefs.getBoolean("profile_completed", false)
                val token = authManager.getToken()
                
                when {
                    !hasSeenOnboarding -> "onboarding"
                    token.isNullOrEmpty() || com.skillswap.util.TokenUtils.isTokenExpired(token) -> "auth"
                    !hasProfile -> "profile_setup"
                    else -> Screen.Discover.route
                }
            }
        
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        // Handle deep links from notifications
        val deepLinkType by MainActivity.deepLinkType
        val deepLinkData by MainActivity.deepLinkData
        
        LaunchedEffect(deepLinkType, deepLinkData) {
            if (deepLinkType != null) {
                when (deepLinkType) {
                    "chat" -> {
                        val threadId = deepLinkData["threadId"]
                        if (!threadId.isNullOrEmpty()) {
                            navController.navigate(Screen.ChatDetail.createRoute(threadId))
                        } else {
                            navController.navigate(Screen.Messages.route)
                        }
                    }
                    "session" -> {
                        val sessionId = deepLinkData["sessionId"]
                        if (!sessionId.isNullOrEmpty()) {
                            navController.navigate("session_detail/$sessionId")
                        } else {
                            navController.navigate(Screen.Sessions.route)
                        }
                    }
                    "notification" -> {
                        navController.navigate("notifications")
                    }
                }
                // Clear after handling
                MainActivity.deepLinkType.value = null
                MainActivity.deepLinkData.value = emptyMap()
            }
        }
        
        val bottomNavItems = listOf(
            Screen.Discover,
            Screen.Messages,
            Screen.Sessions,
            Screen.Progress,
            Screen.Map,
            Screen.Profile
        )

        // Show BottomBar only on main screens
        showBottomBar = currentRoute in bottomNavItems.map { it.route }

        Scaffold(
            // Remove standard bottomBar from Scaffold as we overlay it
        ) {
            Box(Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController, 
                    startDestination = startDestination,
                    modifier = Modifier.padding(bottom = if (showBottomBar) 0.dp else 0.dp) // Handle padding manually if needed, or let content flow behind
                ) {
                    composable("auth") {
                        AuthScreen(onLoginSuccess = {
                            navController.navigate(Screen.Discover.route) {
                                popUpTo("auth") { inclusive = true }
                            }
                            context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
                                .edit().putBoolean("profile_completed", false).apply()
                        })
                    }
                    composable("onboarding") {
                        OnboardingScreen(onFinish = {
                            val prefs = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("onboarding_done", true).apply()
                            navController.navigate("auth") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        })
                    }
                    composable("profile_setup") {
                        ProfileSetupScreen(onDone = {
                            val prefs = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("profile_completed", true).apply()
                            navController.navigate(Screen.Discover.route) {
                                popUpTo("profile_setup") { inclusive = true }
                            }
                        })
                    }
                    
                    composable(Screen.Discover.route) {
                        com.skillswap.ui.discover.DiscoverScreen(
                            onNavigateToChat = { userId ->
                                navController.navigate(Screen.ChatDetail.createRoute(userId)) // Using userId as convId for now
                            }
                        )
                    }

                    composable(Screen.Sessions.route) {
                        SessionsScreen(navController)
                    }

                    composable("create_session") {
                        com.skillswap.ui.sessions.CreateSessionScreen(
                            onBack = { navController.popBackStack() },
                            onSessionCreated = {
                                navController.popBackStack()
                                // Optionally refresh sessions here via viewmodel share or result
                            }
                        )
                    }
                    
                    composable(Screen.Progress.route) {
                        ProgressScreen()
                    }

                    composable(Screen.Messages.route) {
                        ConversationsScreen(
                             onNavigateToChat = { conversationId ->
                                 navController.navigate(Screen.ChatDetail.createRoute(conversationId))
                             }
                        )
                    }
                    
                    composable(Screen.Map.route) {
                        com.skillswap.ui.map.MapScreen()
                    }
                    
                    composable(Screen.Profile.route) {
                        ProfileScreen(navController)
                    }
                    composable(Screen.WeeklyObjective.route) {
                        WeeklyObjectiveScreen()
                    }
                    composable(Screen.Quizzes.route) {
                        com.skillswap.ui.quizzes.QuizzesScreen()
                    }
                    composable("referral") {
                        ReferralScreen(
                            onNavigateToCodeGeneration = {
                                navController.navigate(Screen.ReferralCodeGeneration.route)
                            }
                        )
                    }
                    composable(Screen.ReferralCodeGeneration.route) {
                        ReferralCodeGenerationScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Notifications.route) {
                        com.skillswap.ui.notifications.NotificationsScreen(
                            onCallStart = { name -> callViewModel.startCall(name, video = false) },
                            onNavigateToChat = { threadId ->
                                navController.navigate(Screen.ChatDetail.createRoute(threadId))
                            },
                            onNavigateToSession = {
                                navController.navigate(Screen.Sessions.route)
                            }
                        )
                    }
                    composable(Screen.Moderation.route) {
                        com.skillswap.ui.moderation.ModerationScreen()
                    }
                    
                    composable("settings") {
                        com.skillswap.ui.profile.SettingsScreen(navController)
                    }
                    
                    composable("calendar") {
                        com.skillswap.ui.calendar.CalendarScreen(
                            onBack = { navController.popBackStack() },
                            onEventClick = { eventId -> 
                                navController.navigate("calendar_event/$eventId")
                            }
                        )
                    }
                    
                    composable(
                        "calendar_event/{eventId}",
                        arguments = listOf(navArgument("eventId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                        com.skillswap.ui.calendar.EventDetailScreen(
                            eventId = eventId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("privacy") {
                        com.skillswap.ui.profile.PrivacyScreen(onBack = { navController.popBackStack() })
                    }
                    
                    composable(Screen.ProfileEdit.route) {
                        com.skillswap.ui.profile.ProfileEditScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("rewards") {
                        com.skillswap.ui.profile.RewardsScreen(navController)
                    }
                    
                    composable("sessions_pour_vous") {
                        com.skillswap.ui.sessions.SessionsPourVousScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable(
                        route = "session_detail/{sessionId}",
                        arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                        com.skillswap.ui.sessions.SessionDetailScreen(
                            sessionId = sessionId,
                            onBack = { navController.popBackStack() },
                            onOpenLessonPlan = { sessionId ->
                                navController.navigate("lesson_plan/$sessionId")
                            },
                            onRate = { session ->
                                // Handle rating
                            }
                        )
                    }
                    
                    composable(
                        route = Screen.ChatDetail.route,
                        arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
                        ChatScreen(
                            conversationId = conversationId,
                            onBack = { navController.popBackStack() },
                            onPlanSession = { navController.navigate("create_session") },
                            callViewModel = callViewModel
                        )
                    }

                    composable(Screen.MyPromos.route) {
                        MyPromosScreenContent(navController = navController)
                    }

                    composable(Screen.MyAnnonces.route) {
                        MyAnnoncesScreenContent(navController = navController)
                    }
                }

                // Custom Floating Bottom Bar
                if (showBottomBar) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                    ) {
                        Surface(
                            shadowElevation = 18.dp,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                bottomNavItems.forEach { screen ->
                                    val isSelected = currentRoute == screen.route
                                    // Mapping colors based on screen
                                    val activeColor = when(screen) {
                                        Screen.Discover -> Color(0xFFFF9800) // Orange
                                        Screen.Messages -> Color(0xFFFA5940) // Red-Orange
                                        Screen.Sessions -> Color(0xFF12947D) // Teal
                                        Screen.Progress -> Color(0xFFF28F24) // Orange
                                        Screen.Map -> Color(0xFF5C52BF)      // Purple
                                        else -> OrangePrimary
                                    }

                                    val iconVector = when (screen.icon) {
                                        "house.fill" -> Icons.Default.Home
                                        "message.fill" -> Icons.Default.Email // Mapping bubble.left.. to Email/Chat
                                        "calendar" -> Icons.Default.DateRange
                                        "chart.bar.fill" -> Icons.Default.Info
                                        "map.fill" -> Icons.Default.LocationOn
                                        "person.fill" -> Icons.Default.Person
                                        "bell.fill" -> Icons.Default.Notifications
                                        else -> Icons.Default.Home
                                    }

                                    CustomBottomNavigationItem(
                                        screen = screen,
                                        isSelected = isSelected,
                                        activeColor = activeColor,
                                        icon = iconVector,
                                        onClick = {
                                            if (!isSelected) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                    if (screen != bottomNavItems.last()) {
                                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                val callState = callViewModel.state.collectAsState().value
                if (callState.isInCall || callState.isRinging || callState.ended) {
                    VideoCallScreen(
                        callState = callState,
                        localVideoTrack = callViewModel.localVideoTrack.collectAsState().value,
                        remoteVideoTrack = callViewModel.remoteVideoTrack.collectAsState().value,
                        eglBaseContext = callViewModel.eglBaseContext,
                        onHangup = { callViewModel.hangUp() },
                        onAccept = { callViewModel.acceptIncomingCall() },
                        onDecline = { callViewModel.declineIncomingCall() },
                        onToggleMute = { callViewModel.toggleMute() },
                        onToggleSpeaker = { callViewModel.toggleSpeaker() },
                        onToggleVideo = { callViewModel.toggleVideo() },
                        onSwitchCamera = { callViewModel.switchCamera() },
                        onDismissEnded = { callViewModel.clearEnded() }
                    )
                }
            }
        }
    }
}

}
