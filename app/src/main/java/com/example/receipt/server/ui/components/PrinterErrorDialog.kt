package com.example.receipt.server.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class PrinterError(
    val errorType: ErrorType,
    val message: String,
    val details: String,
    val stackTrace: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestions: List<String> = emptyList()
)

enum class ErrorType {
    LIBRARY_NOT_FOUND,
    CONNECTION_FAILED,
    PRINTER_OFFLINE,
    INITIALIZATION_FAILED,
    PRINT_FAILED,
    UNKNOWN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterErrorDialog(
    error: PrinterError,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFEF4444).copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 24.sp
                            )
                            Text(
                                text = "Printer Error",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Error type badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = error.errorType.name.replace("_", " "),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFCA5A5),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Error message
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F0F0F).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Error Message",
                                fontSize = 12.sp,
                                color = Color(0xFF9CA3AF),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error.message,
                                fontSize = 14.sp,
                                color = Color(0xFFE5E5E5)
                            )
                        }
                    }
                    
                    // Details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F0F0F).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Technical Details",
                                fontSize = 12.sp,
                                color = Color(0xFF9CA3AF),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error.details,
                                fontSize = 13.sp,
                                color = Color(0xFFE5E5E5),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    // Suggestions
                    if (error.suggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF065F46).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "💡",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Troubleshooting Steps",
                                        fontSize = 12.sp,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                error.suggestions.forEachIndexed { index, suggestion ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}.",
                                            fontSize = 13.sp,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = suggestion,
                                            fontSize = 13.sp,
                                            color = Color(0xFFE5E5E5)
                                        )
                                    }
                                    if (index < error.suggestions.size - 1) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                    
                    // Stack trace (collapsible)
                    error.stackTrace?.let { trace ->
                        var expanded by remember { mutableStateOf(false) }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0F0F0F).copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            onClick = { expanded = !expanded }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Stack Trace",
                                        fontSize = 12.sp,
                                        color = Color(0xFF9CA3AF),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (expanded) "▼" else "▶",
                                        fontSize = 12.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                                
                                if (expanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFF000000).copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = trace.take(1000), // Limit display
                                            fontSize = 11.sp,
                                            color = Color(0xFFEF4444),
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dismiss button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF9CA3AF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Dismiss",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Retry button (if available)
                    onRetry?.let { retryAction ->
                        Button(
                            onClick = {
                                retryAction()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Retry",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper function to create printer errors with appropriate suggestions
 */
fun createPrinterError(exception: Exception): PrinterError {
    val errorType: ErrorType
    val suggestions = mutableListOf<String>()
    
    when {
        exception is UnsatisfiedLinkError || exception.message?.contains("libepos2.so") == true || 
        exception.message?.contains("native libraries", ignoreCase = true) == true -> {
            errorType = ErrorType.LIBRARY_NOT_FOUND
            suggestions.addAll(listOf(
                "📥 Download Epson ePOS2 SDK for Android from developer.epson-biz.com",
                "📂 Extract SDK and find 'libs' folder with .so files for each architecture",
                "📱 Copy libepos2.so to: app/src/main/jniLibs/[device-abi]/",
                "🔧 Common ABIs: arm64-v8a (modern phones), armeabi-v7a (older 32-bit)",
                "🔄 After adding files: Clean project → Rebuild → Reinstall app",
                "💡 Alternative: Use HTTP/network printing instead of USB/Bluetooth"
            ))
        }
        exception.message?.contains("connect", ignoreCase = true) == true || 
        exception.message?.contains("ERR_CONNECT", ignoreCase = true) == true -> {
            errorType = ErrorType.CONNECTION_FAILED
            suggestions.addAll(listOf(
                "🔌 Check that the printer is connected via USB cable",
                "⚡ Verify the printer is powered on",
                "📱 Grant USB permission when prompted",
                "🔄 Try unplugging and reconnecting the USB cable",
                "⚙️ Go to Settings → Connected devices to check USB status",
                "🖨️ Ensure printer model is TM-T88 or compatible"
            ))
        }
        exception.message?.contains("offline", ignoreCase = true) == true -> {
            errorType = ErrorType.PRINTER_OFFLINE
            suggestions.addAll(listOf(
                "Check the printer power and cable connections",
                "Verify the printer status LED",
                "Try power cycling the printer",
                "Check for paper jams or empty paper roll"
            ))
        }
        exception.message?.contains("initialize", ignoreCase = true) == true -> {
            errorType = ErrorType.INITIALIZATION_FAILED
            suggestions.addAll(listOf(
                "Restart the application",
                "Check printer model compatibility (expecting TM-T88)",
                "Verify Epson SDK version compatibility",
                "Try resetting the printer to factory settings"
            ))
        }
        else -> {
            errorType = ErrorType.UNKNOWN
            suggestions.addAll(listOf(
                "Check the application logs for more details",
                "Restart both the application and printer",
                "Contact technical support with the error details"
            ))
        }
    }
    
    return PrinterError(
        errorType = errorType,
        message = exception.message ?: "Unknown error occurred",
        details = "${exception.javaClass.simpleName}: ${exception.message}",
        stackTrace = exception.stackTraceToString(),
        suggestions = suggestions
    )
}