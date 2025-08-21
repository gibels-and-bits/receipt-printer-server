package com.example.receipt.server.printer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.DiscoveryListener
import com.epson.epos2.discovery.FilterOption

/**
 * Helper class for USB printer connection and permissions
 */
class UsbPrinterHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "UsbPrinterHelper"
        private const val ACTION_USB_PERMISSION = "com.example.receipt.server.USB_PERMISSION"
        
        // Epson printer vendor IDs
        private const val EPSON_VENDOR_ID = 0x04B8 // 1208 in decimal
    }
    
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var permissionCallback: ((Boolean) -> Unit)? = null
    
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        
                        if (device != null) {
                            if (granted) {
                                Log.i(TAG, "USB permission granted for device: ${device.deviceName}")
                                permissionCallback?.invoke(true)
                            } else {
                                Log.e(TAG, "USB permission denied for device: ${device.deviceName}")
                                permissionCallback?.invoke(false)
                            }
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Log.i(TAG, "USB device attached")
                    checkForEpsonPrinter()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.i(TAG, "USB device detached")
                }
            }
        }
    }
    
    init {
        // Register USB receiver
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
    }
    
    /**
     * Find connected Epson printers
     */
    fun findEpsonPrinters(): List<UsbDevice> {
        val devices = mutableListOf<UsbDevice>()
        
        usbManager.deviceList.values.forEach { device ->
            Log.d(TAG, "Found USB device: ${device.deviceName}, Vendor: ${device.vendorId}, Product: ${device.productId}")
            
            // Check if it's an Epson device
            if (device.vendorId == EPSON_VENDOR_ID) {
                Log.i(TAG, "Found Epson printer: ${device.deviceName}")
                devices.add(device)
            }
        }
        
        if (devices.isEmpty()) {
            Log.w(TAG, "No Epson USB printers found. Available devices: ${usbManager.deviceList.size}")
        }
        
        return devices
    }
    
    /**
     * Request USB permission for a device
     */
    fun requestPermission(device: UsbDevice, callback: (Boolean) -> Unit) {
        permissionCallback = callback
        
        if (usbManager.hasPermission(device)) {
            Log.i(TAG, "Already have permission for device: ${device.deviceName}")
            callback(true)
            return
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            flags
        )
        
        Log.i(TAG, "Requesting USB permission for device: ${device.deviceName}")
        usbManager.requestPermission(device, permissionIntent)
    }
    
    /**
     * Check if we have USB permission for any Epson printer
     */
    fun checkForEpsonPrinter(): UsbDevice? {
        val printers = findEpsonPrinters()
        
        for (printer in printers) {
            if (usbManager.hasPermission(printer)) {
                Log.i(TAG, "Have permission for Epson printer: ${printer.deviceName}")
                return printer
            }
        }
        
        // If we found printers but don't have permission, request it for the first one
        if (printers.isNotEmpty()) {
            val firstPrinter = printers.first()
            Log.i(TAG, "Found Epson printer but no permission, requesting for: ${firstPrinter.deviceName}")
            requestPermission(firstPrinter) { granted ->
                Log.i(TAG, "Permission request result: $granted")
            }
        }
        
        return null
    }
    
    /**
     * Use Epson Discovery to find USB printers
     */
    fun discoverUsbPrinters(callback: (List<String>) -> Unit) {
        val devices = mutableListOf<String>()
        
        try {
            val filter = FilterOption()
            filter.deviceType = Discovery.TYPE_PRINTER
            filter.portType = Discovery.PORTTYPE_USB
            
            Discovery.start(context, filter, object : DiscoveryListener {
                override fun onDiscovery(deviceInfo: com.epson.epos2.discovery.DeviceInfo) {
                    Log.i(TAG, "Discovered USB printer: ${deviceInfo.deviceName} at ${deviceInfo.target}")
                    devices.add(deviceInfo.target)
                }
            })
            
            // Stop discovery after 3 seconds
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                Discovery.stop()
                callback(devices)
            }, 3000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
            callback(devices)
        }
    }
    
    fun cleanup() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister receiver", e)
        }
    }
}