package com.example.receipt.server.printer

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import com.example.receipt.server.EpsonPrinter
import com.example.receipt.server.TextStyle
import com.example.receipt.server.TextSize
import com.example.receipt.server.BarcodeType
import com.example.receipt.server.BarcodeOptions
import com.example.receipt.server.QRCodeOptions
import com.example.receipt.server.QRErrorCorrection
import com.example.receipt.server.ImageOptions
import com.example.receipt.server.Alignment
import com.example.receipt.server.Font

/**
 * Real implementation of EpsonPrinter using the Epson ePOS2 SDK
 */
class RealEpsonPrinter(
    private val context: Context,
    private val printerAddress: String = "USB:" // USB connection instead of TCP
) : EpsonPrinter, ReceiveListener {
    
    companion object {
        private const val TAG = "RealEpsonPrinter"
        private const val DISCONNECT_INTERVAL = 500L
    }
    
    private var printer: Printer? = null
    private val printLock = Object()
    
    init {
        initializePrinter()
    }
    
    private fun initializePrinter() {
        try {
            // The Printer class will load the native library automatically
            // Initialize printer with TM-T88 model
            printer = Printer(Printer.TM_T88, Printer.MODEL_ANK, context).apply {
                setReceiveEventListener(this@RealEpsonPrinter)
            }
            Log.d(TAG, "Printer initialized successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Epson SDK native library not found. Please ensure libepos2.so is included in the project.", e)
            throw RuntimeException("Epson SDK native library not found: ${e.message}")
        } catch (e: NoClassDefFoundError) {
            Log.e(TAG, "Epson SDK classes not found.", e)
            throw RuntimeException("Epson SDK not properly initialized: ${e.message}")
        } catch (e: Epos2Exception) {
            Log.e(TAG, "Failed to initialize printer: ${e.errorStatus}", e)
            throw RuntimeException("Failed to initialize Epson printer: ${e.errorStatus}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error initializing printer", e)
            throw RuntimeException("Printer initialization failed: ${e.message}")
        }
    }
    
    override fun addText(text: String, style: TextStyle?) {
        synchronized(printLock) {
            try {
                Log.d(TAG, "addText called with: $text")
                printer?.let { p ->
                    // Apply text style if provided
                    style?.let { applyTextStyle(p, it) }
                    
                    // Add text
                    p.addText(text)
                    Log.d(TAG, "Text added successfully to printer buffer")
                    
                    // Reset style to default
                    style?.let { resetTextStyle(p) }
                } ?: Log.e(TAG, "Printer object is null!")
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Failed to add text: ${e.errorStatus}", e)
            }
        }
    }
    
    override fun addBarcode(data: String, type: BarcodeType, options: BarcodeOptions?) {
        synchronized(printLock) {
            try {
                printer?.let { p ->
                    val barcodeType = when (type) {
                        BarcodeType.CODE39 -> Printer.BARCODE_CODE39
                        BarcodeType.CODE128 -> Printer.BARCODE_CODE128
                        BarcodeType.EAN13 -> Printer.BARCODE_EAN13
                        BarcodeType.UPC_A -> Printer.BARCODE_UPC_A
                        BarcodeType.UPC_E -> Printer.BARCODE_UPC_E
                        BarcodeType.EAN8 -> Printer.BARCODE_EAN8
                        BarcodeType.ITF -> Printer.BARCODE_ITF
                        BarcodeType.CODABAR -> Printer.BARCODE_CODABAR
                        BarcodeType.CODE93 -> Printer.BARCODE_CODE93
                        BarcodeType.GS1_128 -> Printer.BARCODE_GS1_128
                    }
                    
                    val width = options?.width ?: 2
                    val height = options?.height ?: 100
                    val hri = if (options?.hri == true) Printer.HRI_BELOW else Printer.HRI_NONE
                    
                    if (false) { // Remove QR from barcode, handle in addQRCode instead
                        p.addSymbol(
                            data,
                            Printer.SYMBOL_QRCODE_MODEL_2,
                            Printer.LEVEL_M,
                            width,
                            width,
                            0
                        )
                    } else {
                        p.addBarcode(
                            data,
                            barcodeType,
                            hri,
                            Printer.FONT_A,
                            width,
                            height
                        )
                    }
                }
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Failed to add barcode: ${e.errorStatus}", e)
            }
        }
    }
    
    override fun addQRCode(data: String, options: QRCodeOptions?) {
        synchronized(printLock) {
            try {
                printer?.let { p ->
                    val size = options?.size ?: 3
                    val level = when (options?.errorCorrection) {
                        QRErrorCorrection.L -> Printer.LEVEL_L
                        QRErrorCorrection.M -> Printer.LEVEL_M
                        QRErrorCorrection.Q -> Printer.LEVEL_Q
                        QRErrorCorrection.H -> Printer.LEVEL_H
                        null -> Printer.LEVEL_M
                    }
                    
                    p.addSymbol(
                        data,
                        Printer.SYMBOL_QRCODE_MODEL_2,
                        level,
                        size,
                        size,
                        0
                    )
                }
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Failed to add QR code: ${e.errorStatus}", e)
            }
        }
    }
    
    override fun addImage(imageData: String, options: ImageOptions?) {
        synchronized(printLock) {
            try {
                printer?.let { p ->
                    // Decode base64 image data
                    val imageBytes = Base64.decode(imageData, Base64.DEFAULT)
                    // Note: Image processing would require additional implementation
                    // to convert bytes to Bitmap and then add to printer
                    Log.w(TAG, "Image printing requires bitmap conversion - not fully implemented")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add image: ${e.message}", e)
            }
        }
    }
    
    override fun addFeedLine(lines: Int) {
        synchronized(printLock) {
            try {
                printer?.addFeedLine(lines)
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Failed to add feed line: ${e.errorStatus}", e)
            }
        }
    }
    
    override fun cutPaper() {
        synchronized(printLock) {
            try {
                Log.d(TAG, "cutPaper called")
                printer?.let { p ->
                    // Add cut command
                    p.addCut(Printer.CUT_FEED)
                    Log.d(TAG, "Cut command added, sending data to printer...")
                    
                    // Send data to printer
                    sendDataToPrinter()
                    Log.d(TAG, "Data sent to printer successfully")
                } ?: Log.e(TAG, "Printer object is null when cutting!")
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Failed to cut paper: ${e.errorStatus}", e)
            }
        }
    }
    
    override fun addTextStyle(style: TextStyle) {
        synchronized(printLock) {
            try {
                printer?.let { p ->
                    applyTextStyle(p, style)
                }
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Failed to set text style: ${e.errorStatus}", e)
            }
        }
    }
    
    override fun addTextAlign(alignment: Alignment) {
        synchronized(printLock) {
            try {
                printer?.let { p ->
                    val align = when (alignment) {
                        Alignment.LEFT -> Printer.ALIGN_LEFT
                        Alignment.CENTER -> Printer.ALIGN_CENTER
                        Alignment.RIGHT -> Printer.ALIGN_RIGHT
                    }
                    p.addTextAlign(align)
                }
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Failed to set text alignment: ${e.errorStatus}", e)
            }
        }
    }
    
    override fun addTextFont(font: Font) {
        synchronized(printLock) {
            try {
                printer?.let { p ->
                    val fontType = when (font) {
                        Font.A -> Printer.FONT_A
                        Font.B -> Printer.FONT_B
                    }
                    p.addTextFont(fontType)
                }
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Failed to set text font: ${e.errorStatus}", e)
            }
        }
    }
    
    private fun applyTextStyle(p: Printer, style: TextStyle) {
        try {
            // Set text size
            val size = when (style.size) {
                TextSize.SMALL -> Pair(1, 1)
                TextSize.NORMAL -> Pair(1, 1)
                TextSize.LARGE -> Pair(2, 2)
                TextSize.XLARGE -> Pair(3, 3)
            }
            p.addTextSize(size.first, size.second)
            
            // Set bold
            p.addTextStyle(
                Printer.FALSE,
                if (style.underline) Printer.TRUE else Printer.FALSE,
                if (style.bold) Printer.TRUE else Printer.FALSE,
                Printer.COLOR_1
            )
        } catch (e: Epos2Exception) {
            Log.e(TAG, "Failed to apply text style: ${e.errorStatus}", e)
        }
    }
    
    private fun resetTextStyle(p: Printer) {
        try {
            p.addTextSize(1, 1)
            p.addTextStyle(Printer.FALSE, Printer.FALSE, Printer.FALSE, Printer.COLOR_1)
        } catch (e: Epos2Exception) {
            Log.e(TAG, "Failed to reset text style: ${e.errorStatus}", e)
        }
    }
    
    private fun sendDataToPrinter() {
        try {
            Log.d(TAG, "sendDataToPrinter starting, address: $printerAddress")
            printer?.let { p ->
                try {
                    // Connect to printer
                    Log.d(TAG, "Connecting to printer...")
                    p.connect(printerAddress, Printer.PARAM_DEFAULT)
                    Log.d(TAG, "Connected successfully")
                    
                    // Begin transaction
                    Log.d(TAG, "Beginning transaction...")
                    p.beginTransaction()
                    
                    // Send data
                    Log.d(TAG, "Sending data...")
                    p.sendData(Printer.PARAM_DEFAULT)
                    Log.d(TAG, "Data sent")
                    
                    // End transaction
                    p.endTransaction()
                    Log.d(TAG, "Transaction ended")
                    
                    // Clear command buffer
                    p.clearCommandBuffer()
                    
                    // Disconnect safely
                    Thread.sleep(DISCONNECT_INTERVAL)
                    try {
                        p.disconnect()
                        Log.d(TAG, "Disconnected from printer")
                    } catch (disconnectError: Epos2Exception) {
                        // ERR_ILLEGAL (6) means already disconnected, which is ok
                        if (disconnectError.errorStatus != 6) {
                            Log.e(TAG, "Error disconnecting: ${disconnectError.errorStatus}")
                        }
                    }
                } catch (connectError: Epos2Exception) {
                    // If we fail to connect, it might be because we're already connected
                    // Try to disconnect first then reconnect
                    if (connectError.errorStatus == 5) { // ERR_CONNECT
                        Log.w(TAG, "Connection failed, trying to reset connection...")
                        try {
                            p.disconnect()
                        } catch (e: Exception) {
                            // Ignore disconnect errors
                        }
                        Thread.sleep(100)
                        // Try once more
                        p.connect(printerAddress, Printer.PARAM_DEFAULT)
                        p.beginTransaction()
                        p.sendData(Printer.PARAM_DEFAULT)
                        p.endTransaction()
                        p.clearCommandBuffer()
                        Thread.sleep(DISCONNECT_INTERVAL)
                        try {
                            p.disconnect()
                        } catch (e: Exception) {
                            // Ignore disconnect errors
                        }
                    } else {
                        throw connectError
                    }
                }
                
                Log.d(TAG, "Print job sent successfully")
            }
        } catch (e: Epos2Exception) {
            Log.e(TAG, "Failed to send data to printer: ${e.errorStatus}", e)
            throw RuntimeException("Failed to print: ${e.errorStatus}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while printing: ${e.message}", e)
            throw RuntimeException("Print failed: ${e.message}")
        }
    }
    
    // ReceiveListener implementation
    override fun onPtrReceive(printerObj: Printer?, code: Int, status: PrinterStatusInfo?, printJobId: String?) {
        Log.d(TAG, "Printer response - Code: $code, Status: ${status?.connection}, JobId: $printJobId")
        
        when (code) {
            Epos2CallbackCode.CODE_SUCCESS -> {
                Log.d(TAG, "Print completed successfully")
            }
            Epos2CallbackCode.CODE_ERR_TIMEOUT -> {
                Log.e(TAG, "Print timeout")
            }
            Epos2CallbackCode.CODE_ERR_NOT_FOUND -> {
                Log.e(TAG, "Printer not found")
            }
            else -> {
                Log.e(TAG, "Print error code: $code")
            }
        }
    }
    
    fun disconnect() {
        synchronized(printLock) {
            try {
                printer?.let { p ->
                    p.clearCommandBuffer()
                    p.setReceiveEventListener(null)
                    p.disconnect()
                }
                printer = null
                Log.d(TAG, "Printer disconnected")
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Error during disconnect: ${e.errorStatus}", e)
            }
        }
    }
}

// Callback codes for Epson SDK
object Epos2CallbackCode {
    const val CODE_SUCCESS = 0
    const val CODE_ERR_TIMEOUT = 1
    const val CODE_ERR_NOT_FOUND = 2
    const val CODE_ERR_SYSTEM = 3
    const val CODE_ERR_PORT = 4
    const val CODE_ERR_INVALID_PARAM = 5
}