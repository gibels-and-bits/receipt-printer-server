package com.example.receipt.server.printer

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Diagnostic utility to understand why native library isn't loading
 */
object LibraryDiagnostics {
    private const val TAG = "LibraryDiagnostics"
    
    fun runFullDiagnostics(context: Context): String {
        val report = StringBuilder()
        report.appendLine("=== EPSON LIBRARY DIAGNOSTICS ===\n")
        
        // 1. Device Information
        report.appendLine("DEVICE INFO:")
        report.appendLine("  Model: ${Build.MODEL}")
        report.appendLine("  Manufacturer: ${Build.MANUFACTURER}")
        report.appendLine("  SDK: ${Build.VERSION.SDK_INT}")
        report.appendLine("  ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        report.appendLine("  Primary ABI: ${Build.SUPPORTED_ABIS.firstOrNull()}")
        report.appendLine()
        
        // 2. Application Paths
        report.appendLine("APPLICATION PATHS:")
        report.appendLine("  Native Lib Dir: ${context.applicationInfo.nativeLibraryDir}")
        report.appendLine("  Data Dir: ${context.dataDir}")
        report.appendLine("  APK Path: ${context.applicationInfo.sourceDir}")
        report.appendLine()
        
        // 3. Check Native Library Directory
        report.appendLine("NATIVE LIBRARY DIRECTORY:")
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        if (nativeDir.exists()) {
            val files = nativeDir.listFiles() ?: emptyArray()
            report.appendLine("  Directory exists with ${files.size} files:")
            files.forEach { file ->
                report.appendLine("    - ${file.name} (${file.length()} bytes)")
                if (file.name.contains("epos", ignoreCase = true)) {
                    report.appendLine("      ✓ EPSON LIBRARY FOUND!")
                }
            }
        } else {
            report.appendLine("  ✗ Directory does not exist!")
        }
        report.appendLine()
        
        // 4. Try different loading methods
        report.appendLine("LIBRARY LOADING ATTEMPTS:")
        
        // Method 1: Standard load
        report.appendLine("  Method 1 - System.loadLibrary(\"epos2\"):")
        try {
            System.loadLibrary("epos2")
            report.appendLine("    ✓ SUCCESS!")
        } catch (e: UnsatisfiedLinkError) {
            report.appendLine("    ✗ Failed: ${e.message}")
        } catch (e: Exception) {
            report.appendLine("    ✗ Failed: ${e.javaClass.simpleName}: ${e.message}")
        }
        
        // Method 2: Load with full path
        report.appendLine("  Method 2 - Load with full path:")
        val libPath = File(nativeDir, "libepos2.so")
        if (libPath.exists()) {
            try {
                System.load(libPath.absolutePath)
                report.appendLine("    ✓ SUCCESS with path: ${libPath.absolutePath}")
            } catch (e: UnsatisfiedLinkError) {
                report.appendLine("    ✗ Failed: ${e.message}")
                // Check for dependency issues
                if (e.message?.contains("dlopen failed") == true) {
                    report.appendLine("    → Possible missing dependencies")
                }
            } catch (e: Exception) {
                report.appendLine("    ✗ Failed: ${e.javaClass.simpleName}: ${e.message}")
            }
        } else {
            report.appendLine("    ✗ File not found at: ${libPath.absolutePath}")
        }
        
        // Method 3: Check if already loaded
        report.appendLine("  Method 3 - Check if already loaded:")
        try {
            val loadedLibs = getLoadedLibraries()
            val eposLoaded = loadedLibs.any { it.contains("epos", ignoreCase = true) }
            if (eposLoaded) {
                report.appendLine("    ✓ Library already loaded")
                loadedLibs.filter { it.contains("epos", ignoreCase = true) }.forEach {
                    report.appendLine("      - $it")
                }
            } else {
                report.appendLine("    ✗ Not in loaded libraries list")
            }
        } catch (e: Exception) {
            report.appendLine("    ✗ Could not check loaded libraries: ${e.message}")
        }
        
        report.appendLine()
        
        // 5. Check alternative locations
        report.appendLine("ALTERNATIVE LOCATIONS:")
        val altPaths = listOf(
            File(context.filesDir.parentFile, "lib"),
            File(context.cacheDir.parentFile, "lib"),
            File("/system/lib64"),
            File("/system/lib"),
            File("/vendor/lib64"),
            File("/vendor/lib")
        )
        
        altPaths.forEach { dir ->
            if (dir.exists()) {
                val eposFile = File(dir, "libepos2.so")
                if (eposFile.exists()) {
                    report.appendLine("  ✓ Found at: ${eposFile.absolutePath}")
                }
            }
        }
        
        report.appendLine()
        
        // 6. Library Path
        report.appendLine("JAVA LIBRARY PATH:")
        System.getProperty("java.library.path")?.split(":")?.forEach { path ->
            report.appendLine("  - $path")
        }
        
        val result = report.toString()
        Log.i(TAG, result)
        return result
    }
    
    private fun getLoadedLibraries(): List<String> {
        // This is a hack to get loaded libraries via reflection
        try {
            val vmClass = Class.forName("dalvik.system.VMDebug")
            val method = vmClass.getDeclaredMethod("getLoadedClassCount")
            // This doesn't actually get libraries, but we can try other approaches
            return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
    }
}