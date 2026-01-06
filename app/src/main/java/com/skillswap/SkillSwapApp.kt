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
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import com.skillswap.network.ChatSocketClient
import com.skillswap.auth.AuthenticationManager
import com.skillswap.security.SecureStorage
import com.skillswap.util.LocalizationManager
import com.skillswap.util.ThemeManager
import com.skillswap.ui.guidedtour.GuidedTourManager
import com.skillswap.ui.guidedtour.TourTargetRegistry
import com.skillswap.ui.guidedtour.CoachMarkOverlay
import androidx.compose.ui.layout.onGloballyPositioned

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
    onClick: () -> Unit,
    tourTargetId: String? = null
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            contentColor = if (isSelected) activeColor else Color.Gray,
            containerColor = if (isSelected) activeColor.copy(alpha = 0.12f) else Color.Transparent
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp, horizontal = 0.dp),
        modifier = Modifier
            .defaultMinSize(minWidth = 1.dp)
            .weight(1f)
            .then(
                if (tourTargetId != null) {
                    Modifier.onGloballyPositioned { coordinates ->
                        TourTargetRegistry.register(tourTargetId, coordinates)
                    }
                } else Modifier
            )
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
    val securePrefs = remember { SecureStorage.getInstance(context) }
    val guidedTourManager = remember { GuidedTourManager.getInstance(context) }
    val currentUser by authManager.currentUser.collectAsState()
    val isAuthenticated by authManager.isAuthenticated
    var hasSeenOnboarding by remember { mutableStateOf(securePrefs.getBoolean("onboarding_done", false)) }
    var hasCompletedProfile by remember { mutableStateOf(securePrefs.getBoolean("profile_completed", false)) }
    val hasValidSession = authManager.hasValidSession()
    
    LaunchedEffect(currentUser) {
        val completedFromUser = currentUser?.let { !(it.skillsTeach.isNullOrEmpty() && it.skillsLearn.isNullOrEmpty()) }
        val storedCompleted = securePrefs.getBoolean("profile_completed", false)
        val resolved = completedFromUser ?: storedCompleted
        if (hasCompletedProfile != resolved) {
            hasCompletedProfile = resolved
            securePrefs.edit().putBoolean("profile_completed", resolved).apply()
        }
    }
    
    LaunchedEffect(hasValidSession) {
        if (!hasValidSession) {
            securePrefs.edit().putBoolean("profile_completed", false).apply()
            hasCompletedProfile = securePrefs.getBoolean("profile_completed", false)
        }
    }
    
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            hasCompletedProfile = securePrefs.getBoolean("profile_completed", false)
        }
    }
    
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
    
    // Register FCM token when authenticated
    LaunchedEffect(isAuthenticated, hasValidSession) {
        if (isAuthenticated && hasValidSession) {
            com.skillswap.service.SkillSwapMessagingService.requestToken { token ->
                if (token != null) {
                    android.util.Log.d("SkillSwapApp", "FCM Token retrieved: ${token.take(20)}...")
                } else {
                    android.util.Log.d("SkillSwapApp", "FCM not available, using local notifications only")
                }
            }
        }
    }
    
    // Observe layout direction for RTL support
    @Suppress("UNUSED_VARIABLE")
    val currentLanguage by localizationManager.currentLanguage
    val layoutDirection = localizationManager.layoutDirection
    
    // Initialize global socket listener for notifications
    LaunchedEffect(isAuthenticated, hasValidSession) {
        val socketClient = ChatSocketClient.getInstance(context)
        if (isAuthenticated && hasValidSession) {
            socketClient.connect()
        } else {
            socketClient.disconnect()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        SkillSwapTheme {
            val navController = rememberNavController()
            var showBottomBar by remember { mutableStateOf(false) }
            val callViewModel: CallViewModel = viewModel()
            val chatViewModel: com.skillswap.viewmodel.ChatViewModel = viewModel()
            val startDestination = when {
                !hasSeenOnboarding -> "onboarding"
                !isAuthenticated || !hasValidSession -> "auth"
                !hasCompletedProfile -> "profile_setup"
                else -> Screen.Discover.route
            }
            
            LaunchedEffect(hasSeenOnboarding, hasCompletedProfile, hasValidSession, isAuthenticated) {
                val target = when {
                    !hasSeenOnboarding -> "onboarding"
                    !isAuthenticated || !hasValidSession -> "auth"
                    !hasCompletedProfile -> "profile_setup"
                    else -> null
                }
                val current = navController.currentDestination?.route
                when {
                    target == null -> {
                        if (current in listOf("auth", "onboarding", "profile_setup")) {
                            navController.navigate(Screen.Discover.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                    current != target -> {
                        navController.navigate(target) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
            
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Handle deep links from notifications
            val deepLinkType by MainActivity.deepLinkType
            val deepLinkData by MainActivity.deepLinkData
            
            LaunchedEffect(deepLinkType, deepLinkData, isAuthenticated, hasValidSession, hasCompletedProfile, hasSeenOnboarding) {
                if (deepLinkType != null) {
                    if (!hasSeenOnboarding) {
                        navController.navigate("onboarding") {
                            popUpTo(0) { inclusive = true }
                        }
                        return@LaunchedEffect
                    }
                    if (!isAuthenticated || !hasValidSession) {
                        if (navController.currentDestination?.route != "auth") {
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        return@LaunchedEffect
                    }
                    if (!hasCompletedProfile) {
                        if (navController.currentDestination?.route != "profile_setup") {
                            navController.navigate("profile_setup") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        return@LaunchedEffect
                    }
                    when (deepLinkType) {
                        "chat", "message" -> {
                            val threadId = deepLinkData["threadId"]
                            if (!threadId.isNullOrEmpty()) {
                                navController.navigate(Screen.ChatDetail.createRoute(threadId))
                            } else {
                                navController.navigate(Screen.Messages.route)
                            }
                        }
                        "session", "new_session", "session_reminder" -> {
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
                        "promo", "promotion" -> {
                            val promoId = deepLinkData["promoId"]
                            if (!promoId.isNullOrEmpty()) {
                                navController.navigate("promo_detail/$promoId")
                            } else {
                                navController.navigate(Screen.MyPromos.route)
                            }
                        }
                        "announce", "announcement" -> {
                            val announcementId = deepLinkData["announcementId"]
                            if (!announcementId.isNullOrEmpty()) {
                                navController.navigate("annonce_detail/$announcementId")
                            } else {
                                navController.navigate(Screen.MyAnnonces.route)
                            }
                        }
                        "skill_match" -> {
                            val userId = deepLinkData["userId"]
                            if (!userId.isNullOrEmpty()) {
                                // Navigate to user profile or chat
                                navController.navigate(Screen.ChatDetail.createRoute(userId))
                            } else {
                                navController.navigate(Screen.Discover.route)
                            }
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
            showBottomBar = isAuthenticated && currentRoute in bottomNavItems.map { it.route }
            
            // Start guided tour when user first reaches main screen
            LaunchedEffect(showBottomBar) {
                if (showBottomBar && guidedTourManager.shouldShowTour) {
                    // Small delay to let UI settle
                    kotlinx.coroutines.delay(500)
                    guidedTourManager.startTour()
                }
            }

        Scaffold(
            // Remove standard bottomBar from Scaffold as we overlay it
        ) { _ ->
            Box(Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController, 
                    startDestination = startDestination,
                    modifier = Modifier.padding(bottom = if (showBottomBar) 100.dp else 0.dp)
                ) {
                    composable("auth") {
                        AuthScreen(onLoginSuccess = {
                            hasCompletedProfile = securePrefs.getBoolean("profile_completed", false)
                            val destination = if (hasCompletedProfile) Screen.Discover.route else "profile_setup"
                            navController.navigate(destination) {
                                popUpTo("auth") { inclusive = true }
                            }
                        })
                    }
                    composable("onboarding") {
                        OnboardingScreen(onFinish = {
                            hasSeenOnboarding = true
                            securePrefs.edit().putBoolean("onboarding_done", true).apply()
                            navController.navigate("auth") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        })
                    }
                    composable("profile_setup") {
                        ProfileSetupScreen(onDone = {
                            hasCompletedProfile = true
                            securePrefs.edit().putBoolean("profile_completed", true).apply()
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
                        var showBottomSheet by remember { mutableStateOf(true) }
                        
                        if (showBottomSheet) {
                            com.skillswap.ui.sessions.CreateSessionBottomSheet(
                                onDismiss = { 
                                    showBottomSheet = false
                                    navController.popBackStack()
                                },
                                onSessionCreated = {
                                    showBottomSheet = false
                                    navController.popBackStack()
                                }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                navController.popBackStack()
                            }
                        }
                    }
                    
                    composable(Screen.Progress.route) {
                        ProgressScreen()
                    }

                    composable(Screen.Messages.route) {
                        ConversationsScreen(
                             onNavigateToChat = { conversationId ->
                                 navController.navigate(Screen.ChatDetail.createRoute(conversationId))
                             },
                             viewModel = chatViewModel
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
                            viewModel = chatViewModel,
                            callViewModel = callViewModel
                        )
                    }

                    composable(Screen.MyPromos.route) {
                        MyPromosScreenContent(navController = navController)
                    }

                    composable(Screen.MyAnnonces.route) {
                        MyAnnoncesScreenContent(navController = navController)
                    }
                    
                    composable(
                        route = "promo_detail/{promoId}",
                        arguments = listOf(navArgument("promoId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val promoId = backStackEntry.arguments?.getString("promoId") ?: return@composable
                        com.skillswap.ui.promos.PromoDetailScreen(
                            promoId = promoId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable(
                        route = "annonce_detail/{annonceId}",
                        arguments = listOf(navArgument("annonceId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val annonceId = backStackEntry.arguments?.getString("annonceId") ?: return@composable
                        com.skillswap.ui.annonces.AnnonceDetailScreen(
                            annonceId = annonceId,
                            onBack = { navController.popBackStack() }
                        )
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
                                    
                                    // Tour target ID for each tab
                                    val tourTargetId = when(screen) {
                                        Screen.Discover -> "tab_discover"
                                        Screen.Messages -> "tab_messages"
                                        Screen.Sessions -> "tab_sessions"
                                        Screen.Progress -> "tab_progress"
                                        Screen.Map -> "tab_map"
                                        Screen.Profile -> "tab_profile"
                                        else -> null
                                    }

                                    CustomBottomNavigationItem(
                                        screen = screen,
                                        isSelected = isSelected,
                                        activeColor = activeColor,
                                        icon = iconVector,
                                        onClick = {
                                            if (!isSelected && !guidedTourManager.isShowingTour) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        tourTargetId = tourTargetId
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
                
                // Guided Tour Overlay
                if (guidedTourManager.isShowingTour) {
                    val currentStep = guidedTourManager.currentStep
                    if (currentStep != null) {
                        val targetRect = TourTargetRegistry.getRect(currentStep.targetId)
                        CoachMarkOverlay(
                            step = currentStep,
                            targetRect = targetRect,
                            onNext = { guidedTourManager.nextStep() },
                            onSkip = { guidedTourManager.skipTour() },
                            isLastStep = guidedTourManager.isLastStep,
                            progress = guidedTourManager.progress
                        )
                    }
                }
            }
        }
        }
    }
}
