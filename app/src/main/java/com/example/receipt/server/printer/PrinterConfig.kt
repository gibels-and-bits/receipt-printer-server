package com.example.receipt.server.printer

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration manager for printer settings
 */
class PrinterConfig(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("printer_config", Context.MODE_PRIVATE)
    
    companion object {
        const val KEY_PRINTER_ADDRESS = "printer_address"
        const val KEY_CONNECTION_TYPE = "connection_type"
        const val DEFAULT_TCP_ADDRESS = "TCP:192.168.1.100"
        const val DEFAULT_USB_ADDRESS = "USB:"
        const val DEFAULT_BLUETOOTH_ADDRESS = "BT:"
    }
    
    enum class ConnectionType {
        TCP,
        USB,
        BLUETOOTH
    }
    
    var printerAddress: String
        get() = prefs.getString(KEY_PRINTER_ADDRESS, DEFAULT_TCP_ADDRESS) ?: DEFAULT_TCP_ADDRESS
        set(value) = prefs.edit().putString(KEY_PRINTER_ADDRESS, value).apply()
    
    var connectionType: ConnectionType
        get() = ConnectionType.valueOf(
            prefs.getString(KEY_CONNECTION_TYPE, ConnectionType.TCP.name) ?: ConnectionType.TCP.name
        )
        set(value) = prefs.edit().putString(KEY_CONNECTION_TYPE, value.name).apply()
    
    fun setTcpPrinter(ipAddress: String, port: Int = 9100) {
        connectionType = ConnectionType.TCP
        printerAddress = "TCP:$ipAddress:$port"
    }
    
    fun setUsbPrinter() {
        connectionType = ConnectionType.USB
        printerAddress = DEFAULT_USB_ADDRESS
    }
    
    fun setBluetoothPrinter(macAddress: String) {
        connectionType = ConnectionType.BLUETOOTH
        printerAddress = "BT:$macAddress"
    }
    
    fun getFormattedAddress(): String {
        return when (connectionType) {
            ConnectionType.TCP -> {
                // Ensure proper TCP format
                if (!printerAddress.startsWith("TCP:")) {
                    "TCP:$printerAddress"
                } else {
                    printerAddress
                }
            }
            ConnectionType.USB -> DEFAULT_USB_ADDRESS
            ConnectionType.BLUETOOTH -> {
                if (!printerAddress.startsWith("BT:")) {
                    "BT:$printerAddress"
                } else {
                    printerAddress
                }
            }
        }
    }
}