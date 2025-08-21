#!/bin/bash

# Receipt Printer Server - Network ADB Install Script
# Installs the app to Android device at 192.168.29.2

set -e  # Exit on error

# Configuration
DEVICE_IP="192.168.29.2"
DEVICE_PORT="5555"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
APP_PACKAGE="com.example.receipt.server"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Receipt Printer Server - ADB Installer${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Function to check if device is connected
check_device_connected() {
    adb devices | grep -q "${DEVICE_IP}:${DEVICE_PORT}"
    return $?
}

# Step 1: Check if APK exists
echo -e "${YELLOW}[1/6] Checking for APK...${NC}"
if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}✗ APK not found at $APK_PATH${NC}"
    echo -e "${YELLOW}  Building APK...${NC}"
    ./gradlew :app:assembleDebug
    if [ ! -f "$APK_PATH" ]; then
        echo -e "${RED}✗ Failed to build APK${NC}"
        exit 1
    fi
fi
echo -e "${GREEN}✓ APK found: $(ls -lh $APK_PATH | awk '{print $5}')${NC}"
echo ""

# Step 2: Connect to device over network
echo -e "${YELLOW}[2/6] Connecting to device at $DEVICE_IP...${NC}"

# Disconnect any existing connection to this device
adb disconnect "${DEVICE_IP}:${DEVICE_PORT}" 2>/dev/null || true

# Connect to device
if adb connect "${DEVICE_IP}:${DEVICE_PORT}" | grep -q "connected"; then
    echo -e "${GREEN}✓ Connected to $DEVICE_IP:$DEVICE_PORT${NC}"
else
    echo -e "${RED}✗ Failed to connect to device${NC}"
    echo -e "${YELLOW}  Make sure:${NC}"
    echo -e "${YELLOW}  1. Device is on the same network${NC}"
    echo -e "${YELLOW}  2. ADB over network is enabled on device${NC}"
    echo -e "${YELLOW}  3. Run 'adb tcpip 5555' on device via USB first${NC}"
    exit 1
fi
echo ""

# Step 3: Verify device is connected
echo -e "${YELLOW}[3/6] Verifying device connection...${NC}"
if check_device_connected; then
    DEVICE_MODEL=$(adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell getprop ro.product.model | tr -d '\r\n')
    ANDROID_VERSION=$(adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell getprop ro.build.version.release | tr -d '\r\n')
    echo -e "${GREEN}✓ Device: $DEVICE_MODEL (Android $ANDROID_VERSION)${NC}"
else
    echo -e "${RED}✗ Device not found in adb devices${NC}"
    exit 1
fi
echo ""

# Step 4: Uninstall existing app (if exists)
echo -e "${YELLOW}[4/6] Checking for existing installation...${NC}"
if adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell pm list packages | grep -q "$APP_PACKAGE"; then
    echo -e "${YELLOW}  Found existing app, uninstalling...${NC}"
    if adb -s "${DEVICE_IP}:${DEVICE_PORT}" uninstall "$APP_PACKAGE" >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Previous version uninstalled${NC}"
    else
        echo -e "${YELLOW}⚠ Could not uninstall previous version (may not exist)${NC}"
    fi
else
    echo -e "${GREEN}✓ No previous installation found${NC}"
fi
echo ""

# Step 5: Install APK with streaming
echo -e "${YELLOW}[5/6] Installing APK (streaming)...${NC}"
echo -e "${YELLOW}  This may take a moment...${NC}"

if adb -s "${DEVICE_IP}:${DEVICE_PORT}" install -r -t -g "$APK_PATH"; then
    echo -e "${GREEN}✓ APK installed successfully!${NC}"
else
    echo -e "${RED}✗ Installation failed${NC}"
    echo -e "${YELLOW}  Trying without streaming...${NC}"
    if adb -s "${DEVICE_IP}:${DEVICE_PORT}" install -r "$APK_PATH"; then
        echo -e "${GREEN}✓ APK installed successfully (standard method)${NC}"
    else
        echo -e "${RED}✗ Installation failed completely${NC}"
        exit 1
    fi
fi
echo ""

# Step 6: Launch the app
echo -e "${YELLOW}[6/6] Launching app...${NC}"
if adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell monkey -p "$APP_PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; then
    echo -e "${GREEN}✓ App launched!${NC}"
else
    echo -e "${YELLOW}⚠ Could not auto-launch app, please launch manually${NC}"
fi
echo ""

# Success message
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✅ Installation Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e ""
echo -e "App Package: ${GREEN}$APP_PACKAGE${NC}"
echo -e "Device: ${GREEN}$DEVICE_IP${NC}"
echo -e ""
echo -e "${YELLOW}Server Info:${NC}"
echo -e "  • Ktor server will run on port 8080"
echo -e "  • Access dashboard at http://$DEVICE_IP:8080/admin/dashboard"
echo -e "  • Submit interpreters to http://$DEVICE_IP:8080/submit"
echo -e ""
echo -e "${YELLOW}Tips:${NC}"
echo -e "  • Check device logs: ${GREEN}adb -s $DEVICE_IP:$DEVICE_PORT logcat | grep receipt${NC}"
echo -e "  • Reconnect if lost: ${GREEN}adb connect $DEVICE_IP:$DEVICE_PORT${NC}"
echo -e "  • Port forward:     ${GREEN}adb -s $DEVICE_IP:$DEVICE_PORT forward tcp:8080 tcp:8080${NC}"
echo ""