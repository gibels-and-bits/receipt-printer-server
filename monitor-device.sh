#!/bin/bash

# Receipt Printer Server - Device Monitor Script
# Monitors logs and provides debugging tools for the app

set -e  # Exit on error

# Configuration
DEVICE_IP="192.168.29.2"
DEVICE_PORT="5555"
APP_PACKAGE="com.example.receipt.server"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

show_menu() {
    echo -e "${CYAN}========================================${NC}"
    echo -e "${CYAN}Receipt Printer Server - Device Monitor${NC}"
    echo -e "${CYAN}========================================${NC}"
    echo -e ""
    echo -e "${GREEN}Device: $DEVICE_IP:$DEVICE_PORT${NC}"
    echo -e ""
    echo -e "${YELLOW}Options:${NC}"
    echo -e "  ${BLUE}1${NC} - View live logs (filtered)"
    echo -e "  ${BLUE}2${NC} - View all logs"
    echo -e "  ${BLUE}3${NC} - View printer-specific logs"
    echo -e "  ${BLUE}4${NC} - Clear logs and restart monitoring"
    echo -e "  ${BLUE}5${NC} - Check app status"
    echo -e "  ${BLUE}6${NC} - Force stop app"
    echo -e "  ${BLUE}7${NC} - Restart app"
    echo -e "  ${BLUE}8${NC} - Port forward (8080)"
    echo -e "  ${BLUE}9${NC} - Check network connectivity"
    echo -e "  ${BLUE}0${NC} - Exit"
    echo -e ""
    echo -n "Select option: "
}

ensure_connected() {
    if ! adb devices | grep -q "${DEVICE_IP}:${DEVICE_PORT}"; then
        echo -e "${YELLOW}Reconnecting to device...${NC}"
        adb connect "${DEVICE_IP}:${DEVICE_PORT}" >/dev/null 2>&1
        sleep 1
    fi
}

view_filtered_logs() {
    echo -e "${GREEN}Viewing filtered logs (Ctrl+C to stop)...${NC}"
    echo -e "${YELLOW}Filtering: receipt, epson, printer, ktor${NC}"
    echo ""
    ensure_connected
    adb -s "${DEVICE_IP}:${DEVICE_PORT}" logcat -c  # Clear old logs
    adb -s "${DEVICE_IP}:${DEVICE_PORT}" logcat \
        | grep -iE "receipt|epson|printer|ktor|$APP_PACKAGE" \
        | while read -r line; do
            if echo "$line" | grep -q "E/"; then
                echo -e "${RED}$line${NC}"
            elif echo "$line" | grep -q "W/"; then
                echo -e "${YELLOW}$line${NC}"
            elif echo "$line" | grep -q "I/"; then
                echo -e "${GREEN}$line${NC}"
            elif echo "$line" | grep -q "D/"; then
                echo -e "${BLUE}$line${NC}"
            else
                echo "$line"
            fi
        done
}

view_all_logs() {
    echo -e "${GREEN}Viewing all app logs (Ctrl+C to stop)...${NC}"
    echo ""
    ensure_connected
    adb -s "${DEVICE_IP}:${DEVICE_PORT}" logcat -c
    adb -s "${DEVICE_IP}:${DEVICE_PORT}" logcat --pid=$(adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell pidof -s "$APP_PACKAGE")
}

view_printer_logs() {
    echo -e "${GREEN}Viewing printer-specific logs (Ctrl+C to stop)...${NC}"
    echo ""
    ensure_connected
    adb -s "${DEVICE_IP}:${DEVICE_PORT}" logcat -c
    adb -s "${DEVICE_IP}:${DEVICE_PORT}" logcat \
        | grep -iE "epson|printer|epos|print" \
        | while read -r line; do
            if echo "$line" | grep -q "RealEpsonPrinter"; then
                echo -e "${MAGENTA}[EPSON] $line${NC}"
            elif echo "$line" | grep -q "PrinterManager"; then
                echo -e "${CYAN}[MANAGER] $line${NC}"
            else
                echo -e "${BLUE}[PRINT] $line${NC}"
            fi
        done
}

clear_and_restart() {
    echo -e "${YELLOW}Clearing logs...${NC}"
    ensure_connected
    adb -s "${DEVICE_IP}:${DEVICE_PORT}" logcat -c
    echo -e "${GREEN}✓ Logs cleared${NC}"
    echo ""
    view_filtered_logs
}

check_app_status() {
    echo -e "${YELLOW}Checking app status...${NC}"
    ensure_connected
    
    # Check if installed
    if adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell pm list packages | grep -q "$APP_PACKAGE"; then
        echo -e "${GREEN}✓ App is installed${NC}"
        
        # Check if running
        PID=$(adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell pidof -s "$APP_PACKAGE" 2>/dev/null || echo "")
        if [ -n "$PID" ]; then
            echo -e "${GREEN}✓ App is running (PID: $PID)${NC}"
            
            # Get memory info
            MEM_INFO=$(adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell dumpsys meminfo "$APP_PACKAGE" | grep "TOTAL" | head -1)
            echo -e "${BLUE}  Memory: $MEM_INFO${NC}"
        else
            echo -e "${RED}✗ App is not running${NC}"
        fi
        
        # Get version info
        VERSION=$(adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell dumpsys package "$APP_PACKAGE" | grep versionName | head -1 | awk '{print $1}')
        echo -e "${BLUE}  Version: $VERSION${NC}"
    else
        echo -e "${RED}✗ App is not installed${NC}"
    fi
}

force_stop_app() {
    echo -e "${YELLOW}Force stopping app...${NC}"
    ensure_connected
    adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell am force-stop "$APP_PACKAGE"
    echo -e "${GREEN}✓ App stopped${NC}"
}

restart_app() {
    echo -e "${YELLOW}Restarting app...${NC}"
    ensure_connected
    adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell am force-stop "$APP_PACKAGE"
    sleep 1
    adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell monkey -p "$APP_PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
    echo -e "${GREEN}✓ App restarted${NC}"
}

setup_port_forward() {
    echo -e "${YELLOW}Setting up port forwarding...${NC}"
    ensure_connected
    adb -s "${DEVICE_IP}:${DEVICE_PORT}" forward tcp:8080 tcp:8080
    echo -e "${GREEN}✓ Port 8080 forwarded${NC}"
    echo -e "${BLUE}  You can now access: http://localhost:8080${NC}"
}

check_network() {
    echo -e "${YELLOW}Checking network connectivity...${NC}"
    ensure_connected
    
    # Device IP
    WIFI_IP=$(adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell ip addr show wlan0 2>/dev/null | grep "inet " | awk '{print $2}' | cut -d/ -f1)
    echo -e "${BLUE}Device WiFi IP: $WIFI_IP${NC}"
    
    # Check if port 8080 is listening
    if adb -s "${DEVICE_IP}:${DEVICE_PORT}" shell netstat -an 2>/dev/null | grep -q ":8080.*LISTEN"; then
        echo -e "${GREEN}✓ Server listening on port 8080${NC}"
    else
        echo -e "${RED}✗ Server not listening on port 8080${NC}"
    fi
    
    # Check connectivity from host
    if ping -c 1 -W 1 "$DEVICE_IP" >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Device is reachable from host${NC}"
    else
        echo -e "${RED}✗ Cannot ping device${NC}"
    fi
}

# Main loop
while true; do
    clear
    show_menu
    read -r option
    
    case $option in
        1)
            clear
            view_filtered_logs
            ;;
        2)
            clear
            view_all_logs
            ;;
        3)
            clear
            view_printer_logs
            ;;
        4)
            clear
            clear_and_restart
            ;;
        5)
            clear
            check_app_status
            echo ""
            echo "Press Enter to continue..."
            read -r
            ;;
        6)
            clear
            force_stop_app
            echo ""
            echo "Press Enter to continue..."
            read -r
            ;;
        7)
            clear
            restart_app
            echo ""
            echo "Press Enter to continue..."
            read -r
            ;;
        8)
            clear
            setup_port_forward
            echo ""
            echo "Press Enter to continue..."
            read -r
            ;;
        9)
            clear
            check_network
            echo ""
            echo "Press Enter to continue..."
            read -r
            ;;
        0)
            echo -e "${GREEN}Exiting...${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid option${NC}"
            sleep 1
            ;;
    esac
done