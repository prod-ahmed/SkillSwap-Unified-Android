# Android Feature Parity - Quick Integration Guide

## New Features Added

### 1. Profile Edit Screen

**Navigation:**
```kotlin
// From ProfileSettingsScreen
onEditProfile = {
    // Navigate to ProfileEditScreen
    navController.navigate("profile_edit")
}
```

**Usage:**
```kotlin
ProfileEditScreen(
    onBack = { navController.popBackStack() }
)
```

**Features:**
- Username editing
- Location with city autocomplete
- Skills management (teach/learn)
- Real-time validation
- Success/error messages

---

### 2. Referral Code Generation

**Navigation:**
```kotlin
// From ReferralScreen or ProfileScreen
navController.navigate("referral_code_generation")
```

**Usage:**
```kotlin
ReferralCodeGenerationScreen(
    onBack = { navController.popBackStack() }
)
```

**Features:**
- Create referral codes
- Copy to clipboard
- Share via system intent
- View referral stats
- Track invitations and rewards

---

### 3. Local Notifications

**Initialization (in MainActivity or Application):**
```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var notificationManager: LocalNotificationManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationManager = LocalNotificationManager.getInstance(this)
    }
}
```

**Usage:**
```kotlin
// Show a simple notification
notificationManager.showNotification(
    title = "New Message",
    body = "You have a new message from John"
)

// Show a message notification
notificationManager.showMessageNotification(
    threadId = "thread_123",
    senderName = "John Doe",
    messageText = "Hey, are you available?"
)

// Show a call notification
notificationManager.showCallNotification(
    callerId = "user_456",
    callerName = "Jane Smith",
    callType = "video"
)
```

---

### 4. AI Image Generation for Promos

**In CreatePromoScreen:**
```kotlin
val viewModel: PromosViewModel = viewModel()
val generatingImage by viewModel.generatingImage.collectAsState()
val generatedImageUrl by viewModel.generatedImageUrl.collectAsState()

Button(
    onClick = {
        scope.launch {
            val imageUrl = viewModel.generatePromoImage(
                prompt = "A promotional banner for $promoTitle"
            )
            // Use imageUrl in promo creation
        }
    },
    enabled = !generatingImage
) {
    if (generatingImage) {
        CircularProgressIndicator()
    } else {
        Text("Generate AI Image")
    }
}
```

---

### 5. Quiz History

**Viewing History:**
```kotlin
val viewModel: QuizViewModel = viewModel()
val quizHistory by viewModel.quizHistory.collectAsState()

LazyColumn {
    items(quizHistory) { result ->
        QuizHistoryItem(result)
    }
}
```

**Clearing History:**
```kotlin
Button(onClick = { viewModel.clearHistory() }) {
    Text("Clear History")
}
```

---

## ViewModel Enhancements

### ProfileViewModel

**New Methods:**
```kotlin
// Load cities for autocomplete
viewModel.loadCities()
val cities by viewModel.cities.collectAsState()

// Update profile
viewModel.updateProfile(
    username = "newUsername",
    location = "Tunis",
    skillsTeach = listOf("Kotlin", "Android"),
    skillsLearn = listOf("Swift", "iOS")
)

// Check states
val isSaving by viewModel.isSaving.collectAsState()
val errorMessage by viewModel.errorMessage.collectAsState()
val successMessage by viewModel.successMessage.collectAsState()
```

---

### ReferralViewModel

**New Methods:**
```kotlin
// Create referral code
viewModel.createCode(
    usageLimit = 10,
    expiresAt = null,
    campaign = "spring_2024"
)

// Check states
val generatedCode by viewModel.generatedCode.collectAsState()
val creatingCode by viewModel.creatingCode.collectAsState()
```

---

### PromosViewModel

**New Methods:**
```kotlin
// Generate AI image
scope.launch {
    val imageUrl = viewModel.generatePromoImage(
        prompt = "Modern tech promo banner"
    )
}

// Check states
val generatingImage by viewModel.generatingImage.collectAsState()
val generatedImageUrl by viewModel.generatedImageUrl.collectAsState()
```

---

### QuizViewModel

**New Methods:**
```kotlin
// Access quiz history
val quizHistory by viewModel.quizHistory.collectAsState()

// Clear all history
viewModel.clearHistory()

// History is automatically saved when quiz is submitted
viewModel.submitQuiz(subject = "Kotlin", level = 3)
```

---

## API Endpoints Added

### Profile & Cities
```kotlin
// Get cities for autocomplete
val cities = NetworkService.api.getCities()

// Update profile with location
val updatedUser = NetworkService.api.updateProfile(
    token = "Bearer $token",
    body = mapOf(
        "username" to "newName",
        "location" to mapOf(
            "city" to "Tunis",
            "lat" to null,
            "lon" to null
        ),
        "skillsTeach" to listOf("Kotlin"),
        "skillsLearn" to listOf("Swift")
    )
)
```

### Referrals
```kotlin
// Create referral code
val codeResponse = NetworkService.api.createReferralCode(
    token = "Bearer $token",
    body = mapOf(
        "usageLimit" to 10,
        "expiresAt" to "2024-12-31T23:59:59Z"
    )
)
// Returns: { code: "ABC123", codeId: "...", expiresAt: "..." }
```

### AI Image Generation
```kotlin
// Generate AI image
val imageResponse = NetworkService.api.generateImage(
    token = "Bearer $token",
    body = mapOf("prompt" to "Promotional banner")
)
// Returns: { url: "https://..." }
```

---

## Navigation Integration

### Add to NavGraph
```kotlin
composable("profile_edit") {
    ProfileEditScreen(
        onBack = { navController.popBackStack() }
    )
}

composable("referral_code_generation") {
    ReferralCodeGenerationScreen(
        onBack = { navController.popBackStack() }
    )
}
```

### Update ProfileSettingsScreen
```kotlin
ProfileSettingsScreen(
    onBack = { navController.popBackStack() },
    onLogout = { 
        // Handle logout
    },
    onEditProfile = {
        navController.navigate("profile_edit")
    }
)
```

---

## Required Permissions

### AndroidManifest.xml
```xml
<!-- Notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Internet for API calls -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Location (if implementing maps) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### Request at Runtime
```kotlin
// Notification permission (Android 13+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle permission result
    }
    
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
```

---

## Testing Checklist

### Profile Edit
- [ ] Update username
- [ ] Select city from autocomplete
- [ ] Add/remove teach skills
- [ ] Add/remove learn skills
- [ ] See success message
- [ ] Handle network errors

### Referral Code
- [ ] Generate new code
- [ ] Copy to clipboard
- [ ] Share via system intent
- [ ] View invitation count
- [ ] View rewards count

### Notifications
- [ ] Receive message notification
- [ ] Receive call notification
- [ ] Click notification to open app
- [ ] Notification appears in status bar

### Quiz History
- [ ] Complete quiz
- [ ] View in history
- [ ] History persists after app restart
- [ ] Clear history

### AI Image Generation
- [ ] Generate image for promo
- [ ] See loading state
- [ ] Handle errors
- [ ] Use generated URL

---

## Troubleshooting

### Profile Edit Not Updating
**Solution:** Check that auth token is valid in SharedPreferences
```kotlin
val token = prefs.getString("auth_token", null)
```

### Cities Not Loading
**Solution:** Verify API endpoint is accessible
```kotlin
// Test endpoint
curl https://your-api/locations/cities
```

### Notifications Not Showing
**Solution:** Check permission and channel creation
```kotlin
notificationManager.requestPermissionIfNeeded()
```

### Quiz History Not Persisting
**Solution:** Ensure SharedPreferences key is correct
```kotlin
// Default: "quiz_history"
prefs.getString("quiz_history", null)
```

---

## Performance Tips

### Use remember for Expensive Operations
```kotlin
val filteredCities = remember(location, cities) {
    if (location.isEmpty()) cities
    else cities.filter { it.contains(location, ignoreCase = true) }
}
```

### Debounce Text Input
```kotlin
LaunchedEffect(searchText) {
    delay(300) // Wait 300ms before searching
    viewModel.search(searchText)
}
```

### Limit List Sizes
```kotlin
cities.take(10).forEach { city ->
    // Only show first 10 cities
}
```

---

## Migration from Old Code

### Before (No Profile Edit)
```kotlin
// Profile was read-only
ProfileScreen(user = user)
```

### After (With Profile Edit)
```kotlin
// Profile can be edited
ProfileScreen(
    user = user,
    onEdit = { navController.navigate("profile_edit") }
)
```

---

## Support & Documentation

- **Feature Parity Audit:** `feature_parity_audit.md`
- **Execution Summary:** `FEATURE_PARITY_EXECUTION_SUMMARY.md`
- **iOS Reference:** `SkillSwap-Unified-iOS/` for feature comparison
- **Backend API:** Check `SkillSwap-Unified-Backend/` for endpoint specs
