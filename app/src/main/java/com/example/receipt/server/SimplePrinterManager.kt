package com.example.receipt.server

import java.util.concurrent.ConcurrentHashMap

// Printer interfaces
interface EpsonPrinter {
    fun addText(text: String, style: TextStyle? = null)
    fun addBarcode(data: String, type: BarcodeType, options: BarcodeOptions? = null)
    fun addQRCode(data: String, options: QRCodeOptions? = null)
    fun addImage(imageData: String, options: ImageOptions? = null)
    fun addFeedLine(lines: Int)
    fun cutPaper()
    fun addTextStyle(style: TextStyle)
    fun addTextAlign(alignment: Alignment)
    fun addTextFont(font: Font)
}

// Data classes for printer options
data class TextStyle(
    val bold: Boolean = false,
    val underline: Boolean = false,
    val size: TextSize = TextSize.NORMAL
)

enum class TextSize { SMALL, NORMAL, LARGE, XLARGE }
enum class BarcodeType { CODE39, CODE128, EAN13, UPC_A, UPC_E, EAN8, ITF, CODABAR, CODE93, GS1_128 }
enum class Alignment { LEFT, CENTER, RIGHT }
enum class Font { A, B }

data class BarcodeOptions(
    val width: Int = 2,
    val height: Int = 100,
    val hri: Boolean = true
)

enum class QRErrorCorrection { L, M, Q, H }

data class QRCodeOptions(
    val size: Int = 3,
    val errorCorrection: QRErrorCorrection = QRErrorCorrection.M
)

data class ImageOptions(
    val width: Int = 384,
    val alignment: Alignment = Alignment.CENTER
)

// Printer Manager
class PrinterManager(private val context: android.content.Context? = null) {
    private val realPrinterEnabled = ConcurrentHashMap<String, Boolean>()
    private val mockPrinter = LoggingPrinter()
    private var realPrinter: com.example.receipt.server.printer.RealEpsonPrinter? = null
    
    init {
        // Initialize real printer once at startup if context is available
        if (context != null) {
            try {
                realPrinter = com.example.receipt.server.printer.RealEpsonPrinter(context)
                println("Real Epson printer initialized successfully at startup")
            } catch (e: Exception) {
                println("Failed to initialize real printer at startup: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    fun getRealPrinter(): EpsonPrinter {
        return realPrinter ?: mockPrinter
    }
    
    fun getMockPrinter(): EpsonPrinter = mockPrinter
    
    fun isRealPrintEnabled(teamId: String): Boolean = realPrinterEnabled[teamId] == true
    
    fun enableRealPrint(teamId: String) {
        realPrinterEnabled[teamId] = true
        println("Real printing ENABLED for team: $teamId")
    }
    
    fun disableRealPrint(teamId: String) {
        realPrinterEnabled[teamId] = false
        println("Real printing DISABLED for team: $teamId")
    }
    
    fun getEnabledTeams(): List<String> {
        return realPrinterEnabled.filter { it.value }.keys.toList()
    }
}

// Mock printer implementation
class LoggingPrinter : EpsonPrinter {
    private val log = mutableListOf<String>()
    
    override fun addText(text: String, style: TextStyle?) {
        val logEntry = "TEXT: $text"
        log.add(logEntry)
        println(logEntry)
    }
    
    override fun addBarcode(data: String, type: BarcodeType, options: BarcodeOptions?) {
        val logEntry = "BARCODE: $data (type=$type)"
        log.add(logEntry)
        println(logEntry)
    }
    
    override fun addQRCode(data: String, options: QRCodeOptions?) {
        val logEntry = "QRCODE: $data"
        log.add(logEntry)
        println(logEntry)
    }
    
    override fun addImage(imageData: String, options: ImageOptions?) {
        val logEntry = "IMAGE: [data]"
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
        println("========== RECEIPT OUTPUT ==========")
        log.forEach { println(it) }
        println("====================================")
        log.clear()
    }
    
    override fun addTextStyle(style: TextStyle) {
        println("STYLE: $style")
    }
    
    override fun addTextAlign(alignment: Alignment) {
        println("ALIGN: $alignment")
    }
    
    override fun addTextFont(font: Font) {
        println("FONT: $font")
    }
}