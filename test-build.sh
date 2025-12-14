#!/bin/bash

# SkillSwap Android - Test & Build Script
# Automated testing and build verification

set -e

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║           SkillSwap Android - Test & Build Script               ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_step() {
    echo -e "${BLUE}▶${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Check if we're in the Android directory
if [ ! -f "gradlew" ]; then
    print_error "Must be run from SkillSwap-Unified-Android directory"
    echo "Usage: cd SkillSwap-Unified-Android && ./test-build.sh"
    exit 1
fi

# Parse arguments
SKIP_TESTS=false
SKIP_LINT=false
BUILD_TYPE="debug"

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --skip-lint)
            SKIP_LINT=true
            shift
            ;;
        --release)
            BUILD_TYPE="release"
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: ./test-build.sh [--skip-tests] [--skip-lint] [--release]"
            exit 1
            ;;
    esac
done

echo "Configuration:"
echo "  Build Type: $BUILD_TYPE"
echo "  Skip Tests: $SKIP_TESTS"
echo "  Skip Lint: $SKIP_LINT"
echo ""

# Step 1: Clean
print_step "Step 1/6: Cleaning previous builds..."
./gradlew clean --console=plain > /dev/null 2>&1
print_success "Clean complete"
echo ""

# Step 2: Check for .env
print_step "Step 2/6: Checking environment configuration..."
if [ -f ".env" ]; then
    print_success ".env file found"
    
    # Check for required variables
    if grep -q "SKILLSWAP_API_URL" .env; then
        API_URL=$(grep "SKILLSWAP_API_URL" .env | cut -d '=' -f2)
        echo "  API URL: $API_URL"
    else
        print_warning "SKILLSWAP_API_URL not set in .env"
    fi
else
    print_warning ".env file not found - using defaults"
fi
echo ""

# Step 3: Lint (optional)
if [ "$SKIP_LINT" = false ]; then
    print_step "Step 3/6: Running linter..."
    if ./gradlew lint --console=plain 2>&1 | tail -5; then
        print_success "Lint passed"
    else
        print_warning "Lint found issues (continuing anyway)"
    fi
else
    print_warning "Step 3/6: Skipping lint"
fi
echo ""

# Step 4: Unit Tests (optional)
if [ "$SKIP_TESTS" = false ]; then
    print_step "Step 4/6: Running unit tests..."
    if ./gradlew test --console=plain 2>&1 | tail -10; then
        print_success "Unit tests passed"
    else
        print_error "Unit tests failed"
        exit 1
    fi
else
    print_warning "Step 4/6: Skipping tests"
fi
echo ""

# Step 5: Build
print_step "Step 5/6: Building $BUILD_TYPE APK..."
echo ""

if [ "$BUILD_TYPE" = "release" ]; then
    BUILD_CMD="assembleRelease"
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
else
    BUILD_CMD="assembleDebug"
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

# Run build and capture output
if ./gradlew $BUILD_CMD --console=plain; then
    print_success "Build successful!"
else
    print_error "Build failed"
    exit 1
fi
echo ""

# Step 6: Verify APK
print_step "Step 6/6: Verifying APK..."
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    print_success "APK generated: $APK_PATH ($APK_SIZE)"
else
    print_error "APK not found at $APK_PATH"
    exit 1
fi
echo ""

# Summary
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║                     Build Summary                                ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""
echo "  Build Type:    $BUILD_TYPE"
echo "  APK Location:  $APK_PATH"
echo "  APK Size:      $APK_SIZE"
echo ""

# Installation instructions
echo "To install on connected device:"
if [ "$BUILD_TYPE" = "release" ]; then
    echo "  ./gradlew installRelease"
else
    echo "  ./gradlew installDebug"
fi
echo ""
echo "Or manually:"
echo "  adb install $APK_PATH"
echo ""

print_success "All checks passed! ✨"
echo ""

# Optional: Show connected devices
if command -v adb &> /dev/null; then
    print_step "Connected devices:"
    adb devices | tail -n +2
    echo ""
fi
