# Android Feature Parity - Quick Reference

## New Features Added ✨

### 1. Voice/Video Calling
**Files**: `services/CallManager.kt`, `services/SocketService.kt`

```kotlin
// Start a call
val callManager = CallManager.getInstance(context)
callManager.startCall(
    recipientId = "user123",
    recipientName = "Ahmed",
    isVideo = true
)

// Answer incoming call
callManager.answerCall()

// End call
callManager.endCall()

// Toggle controls
callManager.toggleMute()
callManager.toggleVideo()
callManager.switchCamera()
```

### 2. Location Services
**File**: `services/LocationManager.kt`

```kotlin
// Get current location
val locationManager = LocationManager.getInstance(context)
val location = locationManager.getCurrentLocation()
// Returns: LocationData(latitude, longitude, address, city)

// Start live updates
locationManager.startLocationUpdates()

// Stop updates
locationManager.stopLocationUpdates()

// Check permissions
if (locationManager.checkLocationPermission()) {
    // Use location features
}
```

### 3. Settings Screen
**File**: `ui/profile/SettingsScreen.kt`

Navigate from ProfileScreen → "Paramètres" button

Features:
- Language picker (FR/EN/AR)
- Notification toggles
- Privacy settings
- App version

### 4. Rewards Screen
**File**: `ui/profile/RewardsScreen.kt`

Navigate from ProfileScreen → "Récompenses" button

Features:
- Points & level display
- Achievements/badges
- Referral stats
- How to earn guide

---

## Navigation Routes Added

```kotlin
navController.navigate("settings")        // Settings screen
navController.navigate("rewards")         // Rewards screen
navController.navigate("sessions_pour_vous") // Sessions recommendations
```

---

## Socket.IO Authentication

Chat and calling sockets now use auth tokens:

```kotlin
// Stored in SharedPreferences
val prefs = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
val authToken = prefs.getString("auth_token", null)
val userId = prefs.getString("user_id", null)

// Automatically sent on socket connect:
// { userId: "...", token: "..." }
```

---

## Dependencies Added

```gradle
// Location services
implementation("com.google.android.gms:play-services-location:21.1.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
```

---

## Permissions Required

Already in `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## Build & Run

```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Run tests
./gradlew test
```

---

## Environment Configuration

Create `.env` file in project root:

```env
SKILLSWAP_API_URL=https://your-backend.com
SKILLSWAP_MAPS_API_KEY=AIza...
```

These are read in `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"${envValue("SKILLSWAP_API_URL")}\"")
buildConfigField("String", "MAPS_API_KEY", "\"${envValue("SKILLSWAP_MAPS_API_KEY")}\"")
```

---

## Testing Calling Feature

### Setup
1. Start backend with Socket.IO support on `/calling` namespace
2. Ensure backend handles events: `call:offer`, `call:answer`, `ice-candidate`

### Test Flow
```
Device A                    Backend                    Device B
   |                           |                           |
   |------ call:offer -------->|------ call:incoming ----->|
   |                           |                           |
   |<----- call:ringing -------|<----- call:answer --------|
   |                           |                           |
   |<--- ice-candidate ------->|<--- ice-candidate ------->|
   |                           |                           |
   [WebRTC P2P connection established]
```

---

## Common Issues & Solutions

### Issue: Socket not connecting
**Solution**: Check base URL doesn't have trailing `/api`
```kotlin
// In SocketService.kt
val baseUrl = BuildConfig.API_BASE_URL.replace("/api", "")
socket = IO.socket("$baseUrl/calling", opts)
```

### Issue: Location not working
**Solution**: 
1. Check permissions granted at runtime
2. Verify MAPS_API_KEY in .env
3. Test on physical device (emulator location can be flaky)

### Issue: Call video not showing
**Solution**:
```kotlin
// In VideoCallScreen, ensure WebRTC renderers initialized:
val callManager = CallManager.getInstance(LocalContext.current)
val webRTCClient = callManager.getWebRTCClient()

SurfaceViewRenderer(modifier).apply {
    init(webRTCClient?.eglBaseContext, null)
    webRTCClient?.renderLocalVideo(this)
}
```

---

## Code Quality Checks

```bash
# Lint
./gradlew lint

# Check for outdated dependencies
./gradlew dependencyUpdates

# Generate code coverage
./gradlew jacocoTestReport
```

---

## Architecture Notes

### State Management
- ViewModels use Kotlin `StateFlow` for reactive state
- Compose UI automatically recomposes on state changes

### Dependency Injection
- Singleton services via `getInstance(context)` pattern
- Context injected from Application or Activity

### Network Layer
- Retrofit for REST APIs
- Socket.IO for real-time (chat/calling)
- WebRTC for peer-to-peer media

---

## Git Workflow

Recent commits:
```
f1a910d - docs: Add comprehensive feature parity execution report
d078b1f - fix: Resolve compilation errors and complete feature parity  
93e141d - feat: Add Settings and Rewards screens for profile parity
0ff7485 - feat: Implement WebRTC calling and socket.io integration
```

Branch: `main`  
Remote: Check `.git/config` for origin URL

---

## Resources

- [Feature Parity Audit](./feature_parity_audit.md)
- [Final Execution Report](./FEATURE_PARITY_EXECUTION_FINAL_REPORT.md)
- [iOS Codebase](../SkillSwap-Unified-iOS/)
- [Backend Repo](../SkillSwap-Unified-Backend/)

---

**Last Updated**: 2025-12-14  
**Maintainer**: Development Team
