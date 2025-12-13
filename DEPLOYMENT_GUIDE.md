# Android Feature Parity - Deployment & Configuration Guide

## Overview
This guide covers deployment, configuration, and environment setup for the Android app after implementing feature parity improvements.

---

## Configuration Files

### 1. Build Configuration (`app/build.gradle.kts`)

**Required Dependencies:**
```kotlin
dependencies {
    // Existing dependencies...
    
    // For profile editing and skills management
    implementation("androidx.compose.material3:material3:1.1.2")
    
    // For local notifications
    implementation("androidx.core:core:1.12.0")
    
    // For JSON serialization (quiz history, progress)
    implementation("com.google.code.gson:gson:2.10.1")
    
    // For image loading (if not already included)
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Optional: Google Maps (for Phase 3)
    // implementation("com.google.maps.android:maps-compose:4.3.0")
    // implementation("com.google.android.gms:play-services-maps:18.2.0")
}
```

**Build Config Fields:**
```kotlin
android {
    defaultConfig {
        // API Base URL
        buildConfigField("String", "API_BASE_URL", "\"https://your-api-url.com\"")
        
        // Google Maps API Key (optional, for Phase 3)
        buildConfigField("String", "MAPS_API_KEY", "\"\"")
    }
    
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3000\"") // Android emulator
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.skillswap.tn\"")
            buildConfigField("String", "MAPS_API_KEY", "\"YOUR_PRODUCTION_KEY\"")
        }
    }
}
```

---

### 2. Android Manifest Configuration

**Required Permissions:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Internet for API calls -->
    <uses-permission android:name="android.permission.INTERNET" />
    
    <!-- Notifications (Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <!-- Optional: Location for maps -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
    <!-- Optional: Camera for profile pictures -->
    <uses-permission android:name="android.permission.CAMERA" />
    
    <application
        android:name=".SkillSwapApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:usesCleartextTraffic="true" <!-- Only for debug builds -->
        android:theme="@style/Theme.SkillSwap">
        
        <!-- Main Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.SkillSwap"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            
            <!-- Deep link support for referral codes -->
            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data
                    android:scheme="https"
                    android:host="skillswap.tn"
                    android:pathPrefix="/referral" />
            </intent-filter>
        </activity>
        
    </application>
</manifest>
```

---

### 3. Application Class Setup

**Create/Update SkillSwapApplication.kt:**
```kotlin
package com.skillswap

import android.app.Application
import com.skillswap.utils.LocalNotificationManager

class SkillSwapApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize notification manager
        LocalNotificationManager.getInstance(this)
        
        // Initialize other services as needed
        initializeServices()
    }
    
    private fun initializeServices() {
        // Set up crash reporting, analytics, etc.
        // For production, add Firebase, Sentry, etc.
    }
}
```

---

## Environment Variables

### Development Environment

**Local Backend (Android Emulator):**
```properties
# app/build.gradle.kts (debug)
API_BASE_URL=http://10.0.2.2:3000
```

**Local Backend (Physical Device):**
```properties
# Replace with your computer's IP address
API_BASE_URL=http://192.168.1.XXX:3000
```

### Staging Environment
```properties
API_BASE_URL=https://staging-api.skillswap.tn
MAPS_API_KEY=your_staging_maps_key
```

### Production Environment
```properties
API_BASE_URL=https://api.skillswap.tn
MAPS_API_KEY=your_production_maps_key
```

---

## Backend Alignment

### Required Backend Endpoints

Ensure the following endpoints are available and working:

#### Profile & Users
- `GET /users/me` - Get current user profile
- `PATCH /users/me` - Update user profile
- `GET /locations/cities` - Get city list

#### Referrals
- `GET /referrals/me` - Get user's referrals
- `POST /referrals/codes` - Create referral code
- `POST /referrals/redeem` - Redeem referral code

#### AI & Content
- `POST /ai/generate-image` - Generate AI images
- `POST /moderation/check-image` - Check image safety

#### Sessions & Recommendations
- `GET /sessions/me` - Get user sessions
- `GET /sessions/recommendations` - Get recommended sessions
- `POST /sessions` - Create session

#### Quizzes
- Backend quiz generation endpoint (if not using client-side OpenAI)

---

## Data Migration

### Quiz History Migration

If users have existing quiz data, migrate it to the new format:

```kotlin
// Migration script (run once on app update)
class QuizMigration(private val context: Context) {
    
    fun migrateOldQuizData() {
        val prefs = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
        
        // Check if migration already done
        if (prefs.getBoolean("quiz_migration_done", false)) {
            return
        }
        
        // Migrate old format to new format
        // ... migration logic ...
        
        // Mark migration as complete
        prefs.edit().putBoolean("quiz_migration_done", true).apply()
    }
}
```

### Profile Data Migration

```kotlin
// Ensure backward compatibility with old profile format
class ProfileMigration(private val context: Context) {
    
    fun migrateProfileData() {
        val prefs = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
        
        // Migrate old keys to new keys if needed
        val oldUsername = prefs.getString("old_username_key", null)
        if (oldUsername != null) {
            prefs.edit()
                .putString("username", oldUsername)
                .remove("old_username_key")
                .apply()
        }
    }
}
```

---

## Testing & Quality Assurance

### Pre-Deployment Checklist

#### Functionality Tests
- [ ] Profile edit flow works correctly
- [ ] City autocomplete loads and filters
- [ ] Skills can be added and removed
- [ ] Referral code generation works
- [ ] Referral code sharing works
- [ ] Quiz history persists across app restarts
- [ ] Quiz progress saves correctly
- [ ] Notifications appear properly
- [ ] AI image generation works

#### Network Tests
- [ ] App works with slow network
- [ ] App handles network errors gracefully
- [ ] Retry logic works for failed requests
- [ ] Offline mode shows appropriate messages

#### Edge Cases
- [ ] Empty profile data handled
- [ ] Very long usernames/skills handled
- [ ] Special characters in input handled
- [ ] Large quiz history doesn't crash
- [ ] Multiple rapid quiz submissions handled

#### Performance Tests
- [ ] App launches in < 3 seconds
- [ ] Profile edit screen loads instantly
- [ ] City autocomplete responds quickly
- [ ] Image generation feedback is immediate
- [ ] No memory leaks in long sessions

#### Security Tests
- [ ] Auth tokens stored securely
- [ ] API keys not exposed in logs
- [ ] User data encrypted at rest
- [ ] SSL/TLS used for all API calls

---

## Deployment Steps

### 1. Pre-Deployment

```bash
# 1. Run tests
./gradlew test

# 2. Run lint checks
./gradlew lint

# 3. Build release APK
./gradlew assembleRelease

# 4. Verify APK
ls -lh app/build/outputs/apk/release/
```

### 2. Version Bumping

**In `app/build.gradle.kts`:**
```kotlin
android {
    defaultConfig {
        versionCode = 2  // Increment for each release
        versionName = "1.1.0"  // Semantic versioning
    }
}
```

### 3. Release Build

```bash
# Generate signed APK/AAB
./gradlew bundleRelease

# Output will be in:
# app/build/outputs/bundle/release/app-release.aab
```

### 4. Testing Release Build

```bash
# Install release build on device
adb install -r app/build/outputs/apk/release/app-release.apk

# Check for crashes
adb logcat | grep "SkillSwap"
```

### 5. Store Upload

**Google Play Console:**
1. Upload AAB file
2. Fill release notes
3. Configure rollout percentage
4. Submit for review

**Release Notes Template:**
```
Version 1.1.0 - Feature Parity Update

New Features:
• Full profile editing with city autocomplete
• Referral code generation and sharing
• Quiz history and progress tracking
• AI-powered image generation for promos
• Enhanced notification support

Improvements:
• Better error handling
• Improved performance
• UI polish and bug fixes

Bug Fixes:
• Fixed profile update issues
• Resolved quiz persistence bugs
• Improved network reliability
```

---

## Monitoring & Analytics

### Crash Reporting

**Add Firebase Crashlytics:**
```kotlin
// app/build.gradle.kts
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
}
```

**Initialize in Application:**
```kotlin
class SkillSwapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        }
    }
}
```

### Event Tracking

**Track key user actions:**
```kotlin
// Profile edit completed
analytics.logEvent("profile_edit_completed") {
    param("fields_changed", fieldsChanged.toString())
}

// Referral code generated
analytics.logEvent("referral_code_generated") {
    param("usage_limit", usageLimit.toLong())
}

// Quiz completed
analytics.logEvent("quiz_completed") {
    param("subject", subject)
    param("level", level.toLong())
    param("score", score.toLong())
}

// AI image generated
analytics.logEvent("ai_image_generated") {
    param("prompt_length", prompt.length.toLong())
}
```

---

## Rollback Plan

### If Issues Arise

**1. Emergency Rollback:**
```
# In Google Play Console
1. Go to Release Management
2. Select "Deactivate release"
3. Promote previous version
```

**2. Partial Rollback (Feature Flags):**
```kotlin
object FeatureFlags {
    val PROFILE_EDIT_ENABLED = BuildConfig.DEBUG || checkRemoteConfig("profile_edit_enabled")
    val REFERRAL_GENERATION_ENABLED = BuildConfig.DEBUG || checkRemoteConfig("referral_gen_enabled")
    val AI_IMAGE_GEN_ENABLED = BuildConfig.DEBUG || checkRemoteConfig("ai_image_enabled")
}

// Use in code
if (FeatureFlags.PROFILE_EDIT_ENABLED) {
    // Show profile edit button
}
```

**3. Gradual Rollout:**
```
# Start with 10% of users
# Monitor crashes and errors
# Increase to 50% if stable
# Full rollout if no issues
```

---

## Performance Optimization

### Network Optimization

**Enable HTTP caching:**
```kotlin
val client = OkHttpClient.Builder()
    .cache(Cache(context.cacheDir, 10 * 1024 * 1024)) // 10 MB
    .build()
```

**Compress requests:**
```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .header("Accept-Encoding", "gzip")
            .build()
        chain.proceed(request)
    }
    .build()
```

### UI Performance

**Use lazy loading:**
```kotlin
// In lists, load images lazily
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(imageUrl)
        .crossfade(true)
        .size(200, 200) // Specify size
        .build(),
    contentDescription = null
)
```

**Optimize recompositions:**
```kotlin
// Use keys in lists
LazyColumn {
    items(items, key = { it.id }) { item ->
        ItemRow(item)
    }
}
```

---

## Troubleshooting

### Common Issues

**Issue: "API_BASE_URL not found"**
```
Solution: Rebuild project
./gradlew clean build
```

**Issue: "Notification not showing"**
```
Solution: Check permissions
1. Go to App Settings
2. Enable notifications
3. Verify channel is created
```

**Issue: "Cities not loading"**
```
Solution: Check API endpoint
curl https://your-api/locations/cities
```

**Issue: "Quiz history lost"**
```
Solution: Check SharedPreferences
adb shell
run-as com.skillswap
cat shared_prefs/SkillSwapPrefs.xml
```

---

## Support & Maintenance

### Regular Maintenance Tasks

**Weekly:**
- Check crash reports
- Monitor API error rates
- Review user feedback

**Monthly:**
- Update dependencies
- Review performance metrics
- Analyze user engagement

**Quarterly:**
- Major version update
- Security audit
- Feature roadmap review

---

## Conclusion

This deployment guide ensures a smooth rollout of the feature parity improvements. Follow the checklist carefully and monitor the application closely after deployment.

**For emergency support:**
- Check crash logs in Firebase
- Review API logs in backend
- Contact backend team for API issues
- Roll back if critical issues arise
