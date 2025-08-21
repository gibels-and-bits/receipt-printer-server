# Receipt Printer Server - Deployment Guide

## Quick Start

Deploy the Receipt Printer Server app to your Android device at `192.168.29.2`:

### 1. Initial Setup (One-time with USB)
```bash
# Connect device via USB first
./setup-adb-network.sh
```
This enables ADB over network on port 5555.

### 2. Install App
```bash
# Install app over network (no USB needed)
./install-to-device.sh
```
This will:
- Build the APK if needed
- Connect to device at 192.168.29.2
- Install with streaming for faster deployment
- Launch the app automatically

### 3. Monitor & Debug
```bash
# Interactive monitoring menu
./monitor-device.sh
```
Features:
- Live filtered logs
- Printer-specific debugging
- App status and control
- Port forwarding setup

## Scripts Overview

### 📦 `install-to-device.sh`
Main deployment script with:
- Automatic APK building
- Network ADB connection
- Streaming install (faster for large APKs)
- Previous version cleanup
- Auto-launch after install

### 🔍 `monitor-device.sh`
Interactive monitoring tool:
1. **Live Logs** - Filtered for receipt/printer/server logs
2. **All Logs** - Complete app output
3. **Printer Logs** - Epson-specific debugging
4. **Clear & Restart** - Fresh log session
5. **App Status** - Check if running, memory usage
6. **Force Stop** - Kill the app
7. **Restart App** - Stop and relaunch
8. **Port Forward** - Access server from localhost:8080
9. **Network Check** - Verify connectivity

### 🔧 `setup-adb-network.sh`
One-time setup to enable wireless ADB:
- Detects device on USB
- Enables TCP/IP mode on port 5555
- Verifies network connection
- Shows device IP address

## Manual Commands

### Connect to Device
```bash
adb connect 192.168.29.2:5555
```

### Install APK
```bash
adb -s 192.168.29.2:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```

### View Logs
```bash
# Filtered logs
adb -s 192.168.29.2:5555 logcat | grep -i receipt

# Printer logs
adb -s 192.168.29.2:5555 logcat | grep -i epson
```

### Port Forwarding
```bash
# Forward device port 8080 to localhost
adb -s 192.168.29.2:5555 forward tcp:8080 tcp:8080

# Access server at: http://localhost:8080
```

## Server Endpoints

Once deployed, the server runs on port 8080:

- **Submit Interpreter**: `POST http://192.168.29.2:8080/submit`
- **Print Receipt**: `POST http://192.168.29.2:8080/print/{teamId}`
- **Update Interpreter**: `PUT http://192.168.29.2:8080/submit/{teamId}`
- **Check Status**: `GET http://192.168.29.2:8080/status/{teamId}`

## Troubleshooting

### Cannot Connect to Device
1. Ensure device is on same network
2. Check WiFi IP: `adb shell ip addr show wlan0`
3. Re-run setup: `./setup-adb-network.sh`

### Installation Fails
1. Check available space on device
2. Enable "Install via USB" in Developer Options
3. Try without streaming: Remove `-t` flag in install script

### App Crashes
1. Run `./monitor-device.sh` and select option 1
2. Look for red error messages
3. Check printer connection if Epson-related

### Server Not Accessible
1. Check if app is running: `./monitor-device.sh` → Option 5
2. Verify port 8080 is listening: Option 9
3. Try port forwarding: Option 8

## Tips

- Keep `monitor-device.sh` running in a separate terminal for real-time debugging
- The app will show "Mock Printer" if no real Epson printer is connected
- Server automatically starts when app launches
- Printer access must be enabled per team from the admin dashboard