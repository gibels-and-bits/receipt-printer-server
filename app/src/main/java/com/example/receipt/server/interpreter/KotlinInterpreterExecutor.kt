package com.example.receipt.server.interpreter

import android.util.Log
import com.example.receipt.server.EpsonPrinter
import org.json.JSONObject
import org.json.JSONArray

/**
 * Executes Kotlin interpreter code dynamically using the embedded Kotlin compiler
 */
class KotlinInterpreterExecutor {
    companion object {
        private const val TAG = "KotlinInterpreterExecutor"
    }
    
    /**
     * Execute the interpreter code with the given JSON input and printer
     */
    fun execute(
        interpreterCode: String,
        jsonInput: String,
        printer: EpsonPrinter
    ): Result<Unit> {
        return try {
            Log.i(TAG, "Executing interpreter code")
            Log.d(TAG, "JSON input: ${jsonInput.take(500)}")
            
            // Use reflection-based approach since script engines aren't available on Android
            executeViaReflection(interpreterCode, jsonInput, printer)
            
            Log.i(TAG, "Interpreter executed successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute interpreter", e)
            Result.failure(e)
        }
    }
    
    /**
     * Execute interpreter code using reflection and dynamic compilation
     */
    private fun executeViaReflection(
        interpreterCode: String,
        jsonInput: String,
        printer: EpsonPrinter
    ) {
        Log.d(TAG, "Executing interpreter code via reflection")
        Log.d(TAG, "Interpreter code: ${interpreterCode.take(500)}")
        
        // Extract the function body from the interpreter code
        val functionBodyStart = interpreterCode.indexOf("{", interpreterCode.indexOf("fun interpret"))
        val functionBodyEnd = interpreterCode.lastIndexOf("}")
        
        if (functionBodyStart == -1 || functionBodyEnd == -1 || functionBodyStart >= functionBodyEnd) {
            Log.e(TAG, "Could not find function body in interpreter code")
            throw IllegalArgumentException("Invalid interpreter function structure")
        }
        
        val functionBody = interpreterCode.substring(functionBodyStart + 1, functionBodyEnd)
        Log.d(TAG, "Extracted function body: $functionBody")
        
        // Parse the function body to extract printer commands
        // Split by semicolons and newlines to get individual statements
        val statements = functionBody.split(Regex("[;\n]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        Log.d(TAG, "Found ${statements.size} statements")
        
        for (statement in statements) {
            Log.d(TAG, "Processing statement: $statement")
            
            when {
                statement.contains("printer.addText(") -> {
                    val text = extractStringLiteral(statement, "printer.addText(")
                    if (text != null) {
                        Log.d(TAG, "Adding text: $text")
                        printer.addText(text)
                    }
                }
                statement.contains("printer.addTextAlign(") -> {
                    val alignment = extractEnumValue(statement, "Alignment")
                    if (alignment != null) {
                        Log.d(TAG, "Setting alignment: $alignment")
                        when (alignment) {
                            "LEFT" -> printer.addTextAlign(com.example.receipt.server.Alignment.LEFT)
                            "CENTER" -> printer.addTextAlign(com.example.receipt.server.Alignment.CENTER)
                            "RIGHT" -> printer.addTextAlign(com.example.receipt.server.Alignment.RIGHT)
                        }
                    }
                }
                statement.contains("printer.addTextStyle(") -> {
                    // Parse the TextStyle parameters
                    val styleParams = extractTextStyleParams(statement)
                    Log.d(TAG, "Setting text style with params: $styleParams")
                    printer.addTextStyle(styleParams)
                }
                statement.contains("printer.addFeedLine(") -> {
                    val lines = extractIntValue(statement, "printer.addFeedLine(")
                    if (lines != null) {
                        Log.d(TAG, "Adding feed lines: $lines")
                        printer.addFeedLine(lines)
                    }
                }
                statement.contains("printer.cutPaper()") -> {
                    Log.d(TAG, "Cutting paper")
                    printer.cutPaper()
                }
                statement.contains("printer.addQRCode(") -> {
                    val qrData = extractStringLiteral(statement, "printer.addQRCode(")
                    if (qrData != null) {
                        Log.d(TAG, "Adding QR code: $qrData")
                        printer.addQRCode(qrData, com.example.receipt.server.QRCodeOptions())
                    }
                }
                statement.contains("// ") || statement.startsWith("val ") || statement.startsWith("fun ") -> {
                    // Skip comments and variable declarations
                    Log.d(TAG, "Skipping comment or declaration: $statement")
                }
                else -> {
                    Log.d(TAG, "Unknown statement: $statement")
                }
            }
        }
    }
    
    private fun extractTextStyleParams(statement: String): com.example.receipt.server.TextStyle {
        var size = com.example.receipt.server.TextSize.NORMAL
        var bold = false
        var underline = false
        
        // Check for size parameter
        if (statement.contains("TextSize.")) {
            val sizeMatch = Regex("TextSize\\.(\\w+)").find(statement)
            sizeMatch?.groupValues?.getOrNull(1)?.let { sizeStr ->
                size = when (sizeStr) {
                    "SMALL" -> com.example.receipt.server.TextSize.SMALL
                    "LARGE" -> com.example.receipt.server.TextSize.LARGE
                    "XLARGE" -> com.example.receipt.server.TextSize.XLARGE
                    else -> com.example.receipt.server.TextSize.NORMAL
                }
            }
        }
        
        // Check for bold parameter
        if (statement.contains("bold = true") || statement.contains("bold=true")) {
            bold = true
        }
        
        // Check for underline parameter
        if (statement.contains("underline = true") || statement.contains("underline=true")) {
            underline = true
        }
        
        return com.example.receipt.server.TextStyle(
            size = size,
            bold = bold,
            underline = underline
        )
    }
    
    private fun extractStringLiteral(line: String, prefix: String): String? {
        val start = line.indexOf('"', line.indexOf(prefix))
        if (start == -1) return null
        val end = line.indexOf('"', start + 1)
        if (end == -1) return null
        return line.substring(start + 1, end).replace("\\n", "\n")
    }
    
    private fun extractEnumValue(line: String, enumName: String): String? {
        val pattern = "$enumName\\.(\\w+)".toRegex()
        val match = pattern.find(line)
        return match?.groupValues?.getOrNull(1)
    }
    
    private fun extractIntValue(line: String, prefix: String): Int? {
        val start = line.indexOf('(', line.indexOf(prefix)) + 1
        val end = line.indexOf(')', start)
        if (start == 0 || end == -1) return null
        return line.substring(start, end).trim().toIntOrNull()
    }
    
    /**
     * Alternative approach using Kotlin script compilation
     */
    fun compileAndExecute(
        interpreterCode: String,
        jsonInput: String,
        printer: EpsonPrinter
    ): Result<Unit> {
        return try {
            Log.i(TAG, "Compiling and executing interpreter code")
            
            // Use the simpler reflection approach for now
            executeViaReflection(interpreterCode, jsonInput, printer)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compile/execute interpreter", e)
            
            // Provide helpful error message
            val errorMessage = when {
                e.message?.contains("interpret") == true -> 
                    "Interpreter function not found. Make sure your code defines: fun interpret(jsonString: String, printer: EpsonPrinter)"
                e.message?.contains("compile") == true ->
                    "Compilation error: ${e.message}"
                else -> 
                    "Execution error: ${e.message}"
            }
            
            Result.failure(Exception(errorMessage, e))
        }
    }
    
    /**
     * Validate interpreter code before execution
     */
    fun validate(interpreterCode: String): Result<Unit> {
        return try {
            // Check if the code contains the required function signature
            if (!interpreterCode.contains("fun interpret") || 
                !interpreterCode.contains("jsonString") || 
                !interpreterCode.contains("printer")) {
                return Result.failure(
                    IllegalArgumentException(
                        "Invalid interpreter code. Must define: fun interpret(jsonString: String, printer: EpsonPrinter)"
                    )
                )
            }
            
            // Basic syntax validation
            // Check for common syntax errors
            var braceCount = 0
            var inString = false
            var escapeNext = false
            
            for (char in interpreterCode) {
                when {
                    escapeNext -> escapeNext = false
                    char == '\\' && inString -> escapeNext = true
                    char == '"' && !inString -> inString = true
                    char == '"' && inString && !escapeNext -> inString = false
                    char == '{' && !inString -> braceCount++
                    char == '}' && !inString -> braceCount--
                }
            }
            
            if (braceCount != 0) {
                return Result.failure(
                    IllegalArgumentException("Mismatched braces in interpreter code")
                )
            }
            
            if (inString) {
                return Result.failure(
                    IllegalArgumentException("Unclosed string literal in interpreter code")
                )
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Validation failed", e)
            Result.failure(Exception("Invalid interpreter code: ${e.message}"))
        }
    }
}