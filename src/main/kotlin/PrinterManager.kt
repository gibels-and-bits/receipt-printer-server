package com.example.receipt.server

import com.example.receipt.server.printer.*
import java.util.concurrent.ConcurrentHashMap

class PrinterManager {
    private val realPrinterEnabled = ConcurrentHashMap<String, Boolean>()
    private var epsonPrinter: Any? = null // Real Epson SDK printer instance
    private val mockPrinter = LoggingPrinter()
    
    init {
        // Initialize printer connection if available
        initializePrinter()
    }
    
    /**
     * Get the real hardware printer
     */
    fun getRealPrinter(): EpsonPrinter {
        return if (epsonPrinter != null) {
            EpsonSDKPrinter(epsonPrinter!!)
        } else {
            println("WARNING: Real printer not available, using mock printer instead")
            mockPrinter
        }
    }
    
    /**
     * Get the mock printer for testing
     */
    fun getMockPrinter(): EpsonPrinter = mockPrinter
    
    /**
     * Check if real printing is enabled for a team
     */
    fun isRealPrintEnabled(teamId: String): Boolean = realPrinterEnabled[teamId] == true
    
    /**
     * Enable real printing for a team (admin only)
     */
    fun enableRealPrint(teamId: String) {
        realPrinterEnabled[teamId] = true
        println("Real printing ENABLED for team: $teamId")
    }
    
    /**
     * Disable real printing for a team
     */
    fun disableRealPrint(teamId: String) {
        realPrinterEnabled[teamId] = false
        println("Real printing DISABLED for team: $teamId")
    }
    
    /**
     * Get list of teams with printer enabled
     */
    fun getEnabledTeams(): List<String> {
        return realPrinterEnabled.filter { it.value }.keys.toList()
    }
    
    /**
     * Initialize connection to real printer
     */
    private fun initializePrinter() {
        try {
            // This would initialize the real Epson SDK
            // For now, we'll just log that we're trying
            println("Attempting to initialize Epson printer...")
            
            // In a real implementation:
            // epsonPrinter = com.epson.epos2.printer.Printer(
            //     com.epson.epos2.printer.Printer.TM_T88,
            //     com.epson.epos2.printer.Printer.MODEL_ANK
            // )
            // epsonPrinter.connect("TCP:192.168.1.100", Printer.PARAM_DEFAULT)
            
            println("Printer initialization skipped (mock mode)")
        } catch (e: Exception) {
            println("Failed to initialize printer: ${e.message}")
            epsonPrinter = null
        }
    }
}

/**
 * Mock printer that logs all operations
 */
class LoggingPrinter : EpsonPrinter {
    private val log = mutableListOf<String>()
    
    override fun addText(text: String, style: TextStyle?) {
        val styleStr = style?.let { 
            " [bold=${it.bold}, size=${it.size}, underline=${it.underline}]" 
        } ?: ""
        val logEntry = "TEXT: $text$styleStr"
        log.add(logEntry)
        println(logEntry)
    }
    
    override fun addBarcode(data: String, type: BarcodeType, options: BarcodeOptions?) {
        val optStr = options?.let { 
            " [width=${it.width}, height=${it.height}, hri=${it.hri}]" 
        } ?: ""
        val logEntry = "BARCODE: $data (type=$type)$optStr"
        log.add(logEntry)
        println(logEntry)
    }
    
    override fun addQRCode(data: String, options: QRCodeOptions?) {
        val optStr = options?.let { 
            " [size=${it.size}, errorCorrection=${it.errorCorrection}]" 
        } ?: ""
        val logEntry = "QRCODE: $data$optStr"
        log.add(logEntry)
        println(logEntry)
    }
    
    override fun addImage(imageData: String, options: ImageOptions?) {
        val optStr = options?.let { 
            " [width=${it.width}, alignment=${it.alignment}]" 
        } ?: ""
        val logEntry = "IMAGE: [${imageData.take(20)}...]$optStr"
        log.add(logEntry)
        println(logEntry)
    }
    
    override fun addFeedLine(lines: Int) {
        val logEntry = "FEED: $lines lines"
        log.add(logEntry)
        println(logEntry)
    }
    
    override fun cutPaper() {
        log.add("CUT: Paper cut")
        println("CUT: Paper cut")
        println("\n========== RECEIPT OUTPUT ==========")
        log.forEach { println(it) }
        println("====================================\n")
        log.clear()
    }
    
    override fun addTextStyle(style: TextStyle) {
        val logEntry = "STYLE: bold=${style.bold}, size=${style.size}"
        log.add(logEntry)
        println(logEntry)
    }
    
    override fun addTextAlign(alignment: Alignment) {
        val logEntry = "ALIGN: $alignment"
        log.add(logEntry)
        println(logEntry)
    }
    
    override fun addTextFont(font: Font) {
        val logEntry = "FONT: $font"
        log.add(logEntry)
        println(logEntry)
    }
    
    fun getLog(): List<String> = log.toList()
}

/**
 * Wrapper around real Epson SDK (placeholder implementation)
 */
class EpsonSDKPrinter(private val printer: Any) : EpsonPrinter {
    
    override fun addText(text: String, style: TextStyle?) {
        // In real implementation, would call:
        // printer.addTextStyle(...)
        // printer.addText(text)
        println("[REAL PRINTER] TEXT: $text")
    }
    
    override fun addBarcode(data: String, type: BarcodeType, options: BarcodeOptions?) {
        // In real implementation, would call:
        // printer.addBarcode(data, convertBarcodeType(type), ...)
        println("[REAL PRINTER] BARCODE: $data ($type)")
    }
    
    override fun addQRCode(data: String, options: QRCodeOptions?) {
        // In real implementation, would call:
        // printer.addSymbol(data, SYMBOL_QRCODE, ...)
        println("[REAL PRINTER] QRCODE: $data")
    }
    
    override fun addImage(imageData: String, options: ImageOptions?) {
        // In real implementation, would decode base64 and call:
        // printer.addImage(bitmap, ...)
        println("[REAL PRINTER] IMAGE: [data]")
    }
    
    override fun addFeedLine(lines: Int) {
        // In real implementation, would call:
        // printer.addFeedLine(lines)
        println("[REAL PRINTER] FEED: $lines lines")
    }
    
    override fun cutPaper() {
        // In real implementation, would call:
        // printer.addCut(CUT_FEED)
        // printer.sendData(PARAM_DEFAULT)
        println("[REAL PRINTER] CUT: Paper cut and sent to printer")
    }
    
    override fun addTextStyle(style: TextStyle) {
        println("[REAL PRINTER] STYLE: $style")
    }
    
    override fun addTextAlign(alignment: Alignment) {
        println("[REAL PRINTER] ALIGN: $alignment")
    }
    
    override fun addTextFont(font: Font) {
        println("[REAL PRINTER] FONT: $font")
    }
}