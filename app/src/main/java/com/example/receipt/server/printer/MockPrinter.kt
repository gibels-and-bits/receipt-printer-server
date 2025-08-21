package com.example.receipt.server

import android.util.Log

/**
 * Mock printer implementation for when Epson SDK is not available
 */
class MockPrinter : EpsonPrinter {
    
    companion object {
        private const val TAG = "MockPrinter"
    }
    
    private val receiptContent = StringBuilder()
    private var currentAlignment = Alignment.LEFT
    private var currentStyle = TextStyle()
    
    override fun addText(text: String, style: TextStyle?) {
        val effectiveStyle = style ?: currentStyle
        val styledText = if (effectiveStyle.bold) {
            "**$text**"
        } else {
            text
        }
        
        val alignedText = when (currentAlignment) {
            Alignment.CENTER -> "    [CENTER] $styledText"
            Alignment.RIGHT -> "         [RIGHT] $styledText"
            Alignment.LEFT -> styledText
        }
        
        receiptContent.append(alignedText)
        Log.d(TAG, "Added text: $alignedText")
    }
    
    override fun addBarcode(data: String, type: BarcodeType, options: BarcodeOptions?) {
        val barcodeRep = "[BARCODE: $type - $data]"
        receiptContent.append("\n$barcodeRep\n")
        Log.d(TAG, "Added barcode: $barcodeRep")
    }
    
    override fun addQRCode(data: String, options: QRCodeOptions?) {
        val qrRep = "[QR CODE: $data]"
        receiptContent.append("\n$qrRep\n")
        Log.d(TAG, "Added QR code: $qrRep")
    }
    
    override fun addImage(imageData: String, options: ImageOptions?) {
        val imageRep = "[IMAGE: ${imageData.take(20)}...]"
        receiptContent.append("\n$imageRep\n")
        Log.d(TAG, "Added image")
    }
    
    override fun addFeedLine(lines: Int) {
        repeat(lines) {
            receiptContent.append("\n")
        }
        Log.d(TAG, "Added $lines feed lines")
    }
    
    override fun cutPaper() {
        receiptContent.append("\n========== CUT HERE ==========\n")
        
        // Print the entire receipt to logcat
        Log.i(TAG, "\n╔══════════════════════════════════╗")
        Log.i(TAG, "║       MOCK RECEIPT OUTPUT       ║")
        Log.i(TAG, "╚══════════════════════════════════╝")
        
        receiptContent.toString().split("\n").forEach { line ->
            Log.i(TAG, "║ $line")
        }
        
        Log.i(TAG, "╔══════════════════════════════════╗")
        Log.i(TAG, "║         END OF RECEIPT           ║")
        Log.i(TAG, "╚══════════════════════════════════╝")
        
        // Clear the buffer after printing
        receiptContent.clear()
    }
    
    override fun addTextStyle(style: TextStyle) {
        currentStyle = style
        Log.d(TAG, "Set text style: $style")
    }
    
    override fun addTextAlign(alignment: Alignment) {
        currentAlignment = alignment
        Log.d(TAG, "Set alignment: $alignment")
    }
    
    override fun addTextFont(font: Font) {
        Log.d(TAG, "Set font: $font")
    }
}