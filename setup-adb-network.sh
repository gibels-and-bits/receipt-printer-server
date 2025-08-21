#!/bin/bash

# Receipt Printer Server - ADB Network Setup Script
# One-time setup to enable ADB over network on the device

set -e  # Exit on error

# Configuration
DEVICE_IP="192.168.29.2"
DEVICE_PORT="5555"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}ADB Network Setup for Android Device${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

echo -e "${YELLOW}This script will enable ADB over network on your Android device.${NC}"
echo -e "${YELLOW}You need to run this ONCE with the device connected via USB.${NC}"
echo ""

# Step 1: Check for USB connected devices
echo -e "${YELLOW}[1/5] Checking for USB connected devices...${NC}"
USB_DEVICES=$(adb devices | grep -v "List" | grep -v "^$" | grep -v "$DEVICE_IP" | wc -l)

if [ "$USB_DEVICES" -eq 0 ]; then
    echo -e "${RED}✗ No USB devices found${NC}"
    echo -e "${YELLOW}  Please connect your Android device via USB cable${NC}"
    echo -e "${YELLOW}  Make sure USB debugging is enabled in Developer Options${NC}"
    exit 1
fi

DEVICE_SERIAL=$(adb devices | grep -v "List" | grep -v "^$" | grep -v "$DEVICE_IP" | head -1 | awk '{print $1}')
echo -e "${GREEN}✓ Found USB device: $DEVICE_SERIAL${NC}"
echo ""

# Step 2: Get device info
echo -e "${YELLOW}[2/5] Getting device information...${NC}"
DEVICE_MODEL=$(adb -s "$DEVICE_SERIAL" shell getprop ro.product.model | tr -d '\r\n')
ANDROID_VERSION=$(adb -s "$DEVICE_SERIAL" shell getprop ro.build.version.release | tr -d '\r\n')
CURRENT_IP=$(adb -s "$DEVICE_SERIAL" shell ip addr show wlan0 2>/dev/null | grep "inet " | awk '{print $2}' | cut -d/ -f1 | tr -d '\r\n')

echo -e "${GREEN}✓ Device: $DEVICE_MODEL (Android $ANDROID_VERSION)${NC}"
echo -e "${GREEN}✓ Current WiFi IP: $CURRENT_IP${NC}"
echo ""

# Step 3: Enable TCP/IP mode
echo -e "${YELLOW}[3/5] Enabling ADB over TCP/IP (port $DEVICE_PORT)...${NC}"
if adb -s "$DEVICE_SERIAL" tcpip "$DEVICE_PORT"; then
    echo -e "${GREEN}✓ TCP/IP mode enabled on port $DEVICE_PORT${NC}"
else
    echo -e "${RED}✗ Failed to enable TCP/IP mode${NC}"
    exit 1
fi
echo ""

# Wait a moment for the device to restart ADB
sleep 2

# Step 4: Connect over network
echo -e "${YELLOW}[4/5] Connecting to device over network...${NC}"
echo -e "${YELLOW}  Attempting to connect to: $CURRENT_IP:$DEVICE_PORT${NC}"

if [ -n "$CURRENT_IP" ]; then
    if adb connect "$CURRENT_IP:$DEVICE_PORT" | grep -q "connected"; then
        echo -e "${GREEN}✓ Connected to $CURRENT_IP:$DEVICE_PORT${NC}"
        CONNECTED_IP="$CURRENT_IP"
    else
        echo -e "${YELLOW}⚠ Could not connect to detected IP${NC}"
    fi
fi

# Try the configured IP if different
if [ "$CURRENT_IP" != "$DEVICE_IP" ]; then
    echo -e "${YELLOW}  Attempting configured IP: $DEVICE_IP:$DEVICE_PORT${NC}"
    if adb connect "$DEVICE_IP:$DEVICE_PORT" | grep -q "connected"; then
        echo -e "${GREEN}✓ Connected to $DEVICE_IP:$DEVICE_PORT${NC}"
        CONNECTED_IP="$DEVICE_IP"
    else
        echo -e "${YELLOW}⚠ Could not connect to configured IP${NC}"
    fi
fi
echo ""

# Step 5: Verify connection
echo -e "${YELLOW}[5/5] Verifying network connection...${NC}"
adb devices
echo ""

# Success message
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✅ ADB Network Setup Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

if [ -n "$CONNECTED_IP" ]; then
    echo -e "${GREEN}Device is now accessible at: $CONNECTED_IP:$DEVICE_PORT${NC}"
    echo ""
    echo -e "${CYAN}You can now:${NC}"
    echo -e "  1. Disconnect the USB cable"
    echo -e "  2. Run: ${GREEN}./install-to-device.sh${NC} to install the app"
    echo -e "  3. Run: ${GREEN}./monitor-device.sh${NC} to monitor logs"
else
    echo -e "${YELLOW}⚠ Network connection not verified${NC}"
    echo -e "${YELLOW}  The device IP might be: $CURRENT_IP${NC}"
    echo -e "${YELLOW}  Update DEVICE_IP in the scripts if needed${NC}"
fi

echo ""
echo -e "${YELLOW}Notes:${NC}"
echo -e "  • ADB over network will remain enabled until device restart"
echo -e "  • To reconnect: ${GREEN}adb connect $DEVICE_IP:$DEVICE_PORT${NC}"
echo -e "  • To disconnect: ${GREEN}adb disconnect $DEVICE_IP:$DEVICE_PORT${NC}"
echo ""