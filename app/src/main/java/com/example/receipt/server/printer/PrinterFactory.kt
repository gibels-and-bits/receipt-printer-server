package com.example.receipt.server.printer

import android.content.Context
import android.util.Log
import com.example.receipt.server.EpsonPrinter
import com.example.receipt.server.MockPrinter
import java.io.File

/**
 * Factory for creating printer instances with detailed diagnostics
 */
object PrinterFactory {
    private const val TAG = "PrinterFactory"
    
    data class PrinterInitResult(
        val printer: EpsonPrinter?,
        val isReal: Boolean,
        val error: PrinterInitError?
    )
    
    data class PrinterInitError(
        val type: ErrorType,
        val message: String,
        val details: List<String>
    )
    
    enum class ErrorType {
        NATIVE_LIBRARY_MISSING,
        INITIALIZATION_FAILED,
        UNKNOWN_ERROR
    }
    
    fun createPrinter(context: Context): PrinterInitResult {
        // Step 1: Check if native libraries exist
        val libraryCheck = checkNativeLibraries(context)
        if (!libraryCheck.success) {
            Log.e(TAG, "Native library check failed: ${libraryCheck.details.joinToString("; ")}")
            return PrinterInitResult(
                printer = MockPrinter(),
                isReal = false,
                error = PrinterInitError(
                    type = ErrorType.NATIVE_LIBRARY_MISSING,
                    message = "Epson SDK native libraries not found",
                    details = libraryCheck.details
                )
            )
        }
        
        // Step 2: Try to verify native library availability
        // The Epson SDK will load it automatically when Printer class is instantiated
        Log.i(TAG, "Attempting to initialize Epson printer (library will be loaded by SDK)")
        
        // Step 3: Try to initialize the printer
        return try {
            val printer = RealEpsonPrinter(context)
            Log.i(TAG, "Real Epson printer created successfully")
            PrinterInitResult(
                printer = printer,
                isReal = true,
                error = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize printer", e)
            PrinterInitResult(
                printer = MockPrinter(),
                isReal = false,
                error = PrinterInitError(
                    type = ErrorType.INITIALIZATION_FAILED,
                    message = "Printer initialization failed",
                    details = listOf(
                        "Error: ${e.message}",
                        "Check printer connection and configuration",
                        "Verify printer IP address in settings"
                    )
                )
            )
        }
    }
    
    private data class LibraryCheckResult(
        val success: Boolean,
        val details: List<String>
    )
    
    private fun checkNativeLibraries(context: Context): LibraryCheckResult {
        val details = mutableListOf<String>()
        var hasLibraries = false
        
        // Check common ABIs
        val abis = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        
        details.add("Native library directory: ${nativeLibDir.absolutePath}")
        details.add("Directory exists: ${nativeLibDir.exists()}")
        
        if (nativeLibDir.exists()) {
            val files = nativeLibDir.listFiles()
            details.add("Files in native directory: ${files?.size ?: 0}")
            files?.forEach { file ->
                if (file.name.contains("epos2", ignoreCase = true)) {
                    hasLibraries = true
                    details.add("Found: ${file.name} (${file.length()} bytes)")
                }
            }
        }
        
        // Check jniLibs in APK
        val jniLibsPath = File(context.applicationInfo.sourceDir).parentFile
        details.add("APK directory: ${jniLibsPath?.absolutePath}")
        
        // Check device ABIs
        val supportedAbis = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.os.Build.SUPPORTED_ABIS.joinToString(", ")
        } else {
            @Suppress("DEPRECATION")
            android.os.Build.CPU_ABI
        }
        details.add("Device ABIs: $supportedAbis")
        
        if (!hasLibraries) {
            details.add("❌ No libepos2.so found in any location")
            details.add("📥 Download required: Epson ePOS2 SDK for Android")
            details.add("📁 Place .so files in: app/src/main/jniLibs/[your-device-abi]/")
            details.add("🔧 Your device needs libraries for: $supportedAbis")
        }
        
        return LibraryCheckResult(hasLibraries, details)
    }
}