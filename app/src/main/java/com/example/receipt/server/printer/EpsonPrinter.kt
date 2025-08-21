package com.example.receipt.server.printer

/**
 * Interface for Epson printer operations
 * Matches the interface used by hackathon participants
 */
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

/**
 * Barcode types supported by Epson printers
 */
enum class BarcodeType {
    UPC_A, UPC_E, EAN13, EAN8, 
    CODE39, ITF, CODABAR, CODE93, CODE128,
    GS1_128, GS1_DATABAR_OMNIDIRECTIONAL,
    GS1_DATABAR_TRUNCATED, GS1_DATABAR_LIMITED,
    GS1_DATABAR_EXPANDED
}

/**
 * Text sizes
 */
enum class TextSize {
    SMALL, NORMAL, LARGE, XLARGE
}

/**
 * Text alignment options
 */
enum class Alignment {
    LEFT, CENTER, RIGHT
}

/**
 * Font options
 */
enum class Font {
    FONT_A, FONT_B, FONT_C
}

/**
 * Barcode width options
 */
enum class BarcodeWidth {
    THIN, MEDIUM, THICK
}

/**
 * QR code error correction levels
 */
enum class QRErrorCorrection {
    LOW, MEDIUM, QUARTILE, HIGH
}

/**
 * Text styling options
 */
data class TextStyle(
    val bold: Boolean = false,
    val size: TextSize = TextSize.NORMAL,
    val underline: Boolean = false,
    val reverse: Boolean = false
)

/**
 * Barcode printing options
 */
data class BarcodeOptions(
    val width: BarcodeWidth = BarcodeWidth.MEDIUM,
    val height: Int = 50,
    val hri: Boolean = true  // Human Readable Interpretation
)

/**
 * QR code printing options
 */
data class QRCodeOptions(
    val size: Int = 3,
    val errorCorrection: QRErrorCorrection = QRErrorCorrection.MEDIUM
)

/**
 * Image printing options
 */
data class ImageOptions(
    val width: Int = 384,
    val alignment: Alignment = Alignment.CENTER,
    val dithering: Boolean = true
)