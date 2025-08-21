# Production Receipt Printer Server

## Overview
This is a production-ready Android application that runs a receipt printer server for hackathon team management. The app provides:
- Real-time dashboard with dark mode UI
- HTTP API server running on port 8080
- Epson thermal printer integration
- Team and print job management
- Detailed error reporting with troubleshooting steps

## Features

### 🎨 Dark Mode Dashboard
- Modern dark theme with gradient accents
- Real-time team status cards
- Connected clients display
- Print queue monitoring
- Test print button with instant feedback

### 🖨️ Printer Integration
- Epson ePOS2 SDK integration
- Automatic fallback if native libraries are missing
- Detailed error popup with troubleshooting steps
- Per-team printer access control

### 🌐 HTTP API Server
The embedded Ktor server provides the following endpoints:

#### Team Management
- `POST /api/submit` - Register team submission
- `GET /api/admin/teams` - List all teams
- `POST /api/admin/team/{teamId}/printer/enable` - Enable printer for team
- `POST /api/admin/team/{teamId}/printer/disable` - Disable printer for team

#### Print Jobs
- `POST /api/print` - Submit print job (requires printer access)

#### Administration
- `GET /api/admin/clients` - List connected clients
- `GET /api/admin/statistics` - Server statistics
- `DELETE /api/admin/clear` - Clear all data (testing only)
- `GET /health` - Health check endpoint

## Production Configuration

### Network Setup
1. Ensure the Android device is on the same network as client devices
2. Note the device IP address for client configuration
3. Server runs on port 8080 by default

### Printer Setup
1. Connect Epson TM-T88 printer to network
2. Default printer IP: `TCP:192.168.1.100`
3. Update in `RealEpsonPrinter.kt` if different

### Installing Epson SDK Libraries
If you encounter "libepos2.so not found" error:

1. Download Epson ePOS2 SDK for Android
2. Extract native libraries from the SDK
3. Place them in `app/src/main/jniLibs/`:
   ```
   app/src/main/jniLibs/
   ├── arm64-v8a/
   │   └── libepos2.so
   ├── armeabi-v7a/
   │   └── libepos2.so
   ├── x86/
   │   └── libepos2.so
   └── x86_64/
       └── libepos2.so
   ```

## Error Handling

The app includes comprehensive error handling with detailed popups showing:
- Error type and message
- Technical details
- Troubleshooting steps
- Stack trace (expandable)
- Retry option

Common error types handled:
- `LIBRARY_NOT_FOUND` - Epson SDK not installed
- `CONNECTION_FAILED` - Cannot reach printer
- `PRINTER_OFFLINE` - Printer not responding
- `INITIALIZATION_FAILED` - SDK initialization error

## API Usage Examples

### Submit Team
```bash
curl -X POST http://device-ip:8080/api/submit \
  -H "Content-Type: application/json" \
  -d '{
    "team_id": "team-001",
    "teamName": "Code Warriors",
    "interpreterCode": "..."
  }'
```

### Enable Printer Access
```bash
curl -X POST http://device-ip:8080/api/admin/team/team-001/printer/enable
```

### Submit Print Job
```bash
curl -X POST http://device-ip:8080/api/print \
  -H "Content-Type: application/json" \
  -d '{
    "teamId": "team-001",
    "content": "Hello from Team 001!"
  }'
```

### Get Statistics
```bash
curl http://device-ip:8080/api/admin/statistics
```

## Monitoring

The dashboard automatically refreshes every 5 seconds and displays:
- Total teams registered
- Teams with printer access
- Queue status (used/total capacity)
- Connected clients with status indicators

## Security Considerations

1. **Network Security**: Run on isolated network for hackathons
2. **Access Control**: Printer access must be explicitly enabled per team
3. **Rate Limiting**: Queue capacity limits concurrent print jobs
4. **Error Isolation**: Failed prints don't affect other teams

## Troubleshooting

### Server Won't Start
- Check port 8080 is not in use
- Verify Android permissions for network access
- Check device firewall settings

### Printer Not Working
- Verify printer IP and network connectivity
- Ensure Epson SDK libraries are installed
- Check printer has paper and is powered on
- Try test print button for diagnostics

### Dashboard Not Updating
- Verify server is running (check logs)
- Refresh manually using FAB button
- Check network connectivity

## Development Notes

- Built with Kotlin and Jetpack Compose
- Uses Ktor CIO engine for Android compatibility
- StateFlow for reactive UI updates
- Modular architecture for easy maintenance

## Support

For issues with:
- **Epson SDK**: Refer to Epson developer documentation
- **Network Issues**: Check router/firewall configuration
- **App Crashes**: Check logcat for detailed stack traces