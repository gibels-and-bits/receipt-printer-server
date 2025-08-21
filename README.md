# Receipt Printer Server

Android-based Ktor server for the Receipt Designer Hackathon. Accepts Kotlin interpreter submissions and executes them against a real Epson receipt printer.

## Overview

This server allows hackathon participants to:
1. Upload their Kotlin interpreter code
2. Execute it with their JSON DSL
3. Print receipts on real hardware (during judging phase)

## Features

- **Kotlin Script Execution**: Safely runs participant code in sandboxed environment
- **Queue Management**: Limits concurrent executions to prevent overload
- **Mock/Real Printer Modes**: Development with mock, production with real hardware
- **Team Management**: Each team gets a unique endpoint
- **Admin Controls**: Enable real printing per team during judging

## Setup

### Prerequisites
- JDK 17+
- Gradle 8+
- (Optional) Epson receipt printer connected via network/USB

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/receipt-printer-server.git
cd receipt-printer-server
```

2. Build the project:
```bash
./gradlew build
```

3. Run the server:
```bash
./gradlew runServer
```

The server will start on port 8080 by default.

### Docker Deployment (Optional)

```dockerfile
FROM openjdk:17-slim
COPY build/libs/receipt-printer-server-1.0.0-standalone.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

Build and run:
```bash
./gradlew fatJar
docker build -t receipt-printer-server .
docker run -p 8080:8080 receipt-printer-server
```

## API Endpoints

### Public Endpoints

#### POST /submit
Upload interpreter code for a team.

Request:
```json
{
  "teamName": "Team Awesome",
  "interpreterCode": "fun interpret(json: String, printer: EpsonPrinter) { ... }"
}
```

Response:
```json
{
  "endpoint": "/print/team-awesome",
  "status": "ready",
  "teamId": "team-awesome"
}
```

#### POST /print/{teamId}
Execute interpreter with JSON receipt data.

Request body: JSON receipt DSL
```json
{
  "elements": [
    {"type": "text", "content": "Hello World"}
  ]
}
```

Response:
```json
{
  "success": true,
  "message": "Print job completed",
  "mode": "mock"
}
```

#### PUT /submit/{teamId}
Update existing interpreter (before submission freeze).

Request:
```json
{
  "interpreterCode": "// Updated code"
}
```

#### GET /status/{teamId}
Check team status.

Response:
```json
{
  "teamId": "team-awesome",
  "hasInterpreter": true,
  "realPrintEnabled": false,
  "queueStatus": {
    "availableSlots": 2,
    "maxSlots": 3
  }
}
```

#### GET /health
Health check endpoint.

### Admin Endpoints

Require `X-Admin-Key: hackathon2024` header.

#### POST /admin/enable-printer/{teamId}
Enable real printer for a team (judging phase).

#### GET /admin/teams
List all registered teams.

## Development Mode

By default, the server runs in mock mode with console output instead of real printing:

```
[TEXT] Welcome to Byte Burgers [bold=true, size=LARGE]
[BARCODE] 123456789 (type=CODE39)
[CUT] Paper cut

========== RECEIPT OUTPUT ==========
TEXT: Welcome to Byte Burgers [bold=true, size=LARGE]
BARCODE: 123456789 (type=CODE39)
CUT: Paper cut
====================================
```

## Queue Management

The server limits concurrent interpreter executions to prevent overload:
- Maximum 3 concurrent jobs
- Returns HTTP 429 when queue is full
- Automatic cleanup after execution

## Environment Variables

```bash
PORT=8080                           # Server port
ADMIN_KEY=hackathon2024            # Admin authentication key
USE_MOCK_PRINTER=true              # Use mock printer (false for real)
PRINTER_CONNECTION=TCP:192.168.1.100  # Real printer connection
SCRIPT_TIMEOUT=30000               # Script execution timeout (ms)
```

## Testing

### Test with curl

1. Submit interpreter:
```bash
curl -X POST http://localhost:8080/submit \
  -H "Content-Type: application/json" \
  -d '{"teamName": "Test Team", "interpreterCode": "fun interpret(json: String, printer: EpsonPrinter) { printer.addText(\"Test\"); printer.cutPaper(); }"}'
```

2. Test print:
```bash
curl -X POST http://localhost:8080/print/test-team \
  -H "Content-Type: application/json" \
  -d '{"elements": [{"type": "text", "content": "Hello"}]}'
```

## Security Notes

This server is designed for hackathon use with these considerations:
- Kotlin scripts run in controlled environment
- Resource limits prevent infinite loops
- Queue management prevents DoS
- Admin endpoints require authentication
- CORS is open for hackathon (restrict in production)

## Troubleshooting

### Server won't start
- Check Java version: `java -version` (requires 17+)
- Verify port 8080 is available
- Check logs for initialization errors

### Interpreter execution fails
- Validate Kotlin syntax
- Check for required imports
- Ensure `interpret` function signature matches
- Review script timeout settings

### Printer issues
- Verify printer connection string
- Check network connectivity
- Ensure Epson SDK drivers installed
- Test with mock printer first

## Hackathon Workflow

### Phase 1: Development (Mock Printer)
1. Teams develop locally
2. Upload interpreters to server
3. Test with mock printer output
4. Iterate and update code

### Phase 2: Testing (Mock Printer)
1. Teams finalize interpreters
2. Run comprehensive tests
3. Validate JSON processing
4. Check error handling

### Phase 3: Judging (Real Printer)
1. Admin enables real printer per team
2. Teams demonstrate their solutions
3. Receipts print on actual hardware
4. Judges evaluate output quality

## Support

For hackathon support:
- Check server logs: `tail -f logs/server.log`
- Monitor queue status: GET `/health`
- Test endpoints with provided examples
- Contact organizers for printer issues

## License

MIT License - Created for Receipt Designer Hackathon