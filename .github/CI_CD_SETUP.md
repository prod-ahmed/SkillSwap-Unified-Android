# CI/CD Setup Guide

This document explains the continuous integration and deployment setup for SkillSwap Android.

## GitHub Actions Workflow

The `.github/workflows/android-ci.yml` file defines our CI/CD pipeline.

### Triggers

The workflow runs on:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Only when Android-related files change

### Jobs

#### 1. Build & Test
- Runs on every push/PR
- Executes linting
- Runs unit tests
- Builds debug APK
- Uploads artifacts

#### 2. Release Build
- Runs only on `main` branch
- Requires build job to pass
- Creates signed release APK
- Uploads to artifacts (30-day retention)

## Required Secrets

Configure these in GitHub repository settings (Settings → Secrets and variables → Actions):

### API Configuration
- `API_BASE_URL` - Backend API URL (e.g., `https://api.skillswap.tn`)
- `MAPS_API_KEY` - Google Maps API key

### Release Signing (Optional)
- `KEYSTORE_BASE64` - Base64-encoded keystore file
- `KEYSTORE_PASSWORD` - Keystore password
- `KEY_ALIAS` - Key alias
- `KEY_PASSWORD` - Key password

## Setting Up Release Signing

### 1. Generate Keystore

```bash
keytool -genkey -v -keystore release.keystore \
  -alias skillswap \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### 2. Encode Keystore to Base64

```bash
base64 release.keystore > keystore.base64.txt
```

### 3. Add to GitHub Secrets

1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Add each secret:
   - Name: `KEYSTORE_BASE64`
   - Value: Contents of `keystore.base64.txt`
   - Repeat for other secrets

### 4. Update build.gradle.kts

Ensure release signing configuration exists:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release.keystore")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

## Local Testing

Test the workflow locally before pushing:

### Using Test Script

```bash
./test-build.sh
```

Options:
- `--skip-tests` - Skip unit tests
- `--skip-lint` - Skip linting
- `--release` - Build release APK

### Manual Testing

```bash
# Clean
./gradlew clean

# Lint
./gradlew lint

# Test
./gradlew test

# Build debug
./gradlew assembleDebug

# Build release (requires keystore)
./gradlew assembleRelease
```

## Viewing Build Results

### GitHub Actions

1. Go to repository → Actions tab
2. Click on workflow run
3. View logs and download artifacts

### Artifacts

Build artifacts are available:
- Debug APK: 7 days retention
- Release APK: 30 days retention
- Test results: 7 days
- Lint reports: 7 days

## Troubleshooting

### Build Fails: "Permission denied"

Ensure gradlew is executable:
```bash
chmod +x gradlew
git add gradlew
git commit -m "Make gradlew executable"
```

### Build Fails: "Missing API keys"

Add secrets in GitHub repository settings:
- Settings → Secrets and variables → Actions → New repository secret

### Tests Fail

Check test reports in artifacts:
1. Go to Actions → Workflow run
2. Download "test-results" artifact
3. Open HTML reports locally

### Lint Fails

Lint is set to `continue-on-error: true`, so it won't block builds.
Download lint reports from artifacts to review issues.

## Advanced Configuration

### Branch-Specific Builds

Modify workflow triggers:

```yaml
on:
  push:
    branches: [ main, develop, feature/* ]
```

### Conditional Steps

Run steps only for specific branches:

```yaml
- name: Deploy to Staging
  if: github.ref == 'refs/heads/develop'
  run: ./deploy-staging.sh
```

### Matrix Builds

Test multiple configurations:

```yaml
strategy:
  matrix:
    api-level: [29, 30, 31, 33]
```

## Best Practices

1. **Never commit secrets** - Always use GitHub Secrets
2. **Test locally first** - Use `test-build.sh` before pushing
3. **Review artifacts** - Check APK size and structure
4. **Monitor build times** - Optimize if builds take >5 minutes
5. **Keep dependencies updated** - Regularly update actions versions

## Deployment to Google Play

### Using GitHub Actions

Add deployment step:

```yaml
- name: Deploy to Play Store
  uses: r0adkll/upload-google-play@v1
  with:
    serviceAccountJsonPlainText: ${{ secrets.SERVICE_ACCOUNT_JSON }}
    packageName: com.skillswap
    releaseFiles: app/build/outputs/bundle/release/app-release.aab
    track: internal
```

### Manual Upload

1. Build release bundle:
   ```bash
   ./gradlew bundleRelease
   ```

2. Upload to Play Console:
   - Go to Google Play Console
   - Select app → Release → Internal testing
   - Upload `app/build/outputs/bundle/release/app-release.aab`

## Support

For issues with CI/CD:
1. Check workflow logs in Actions tab
2. Review this guide
3. Consult GitHub Actions documentation: https://docs.github.com/actions

---

*Last Updated: 2025-12-14*
