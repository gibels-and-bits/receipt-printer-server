package com.example.receipt.server

import java.util.concurrent.Semaphore

class QueueManager(private val maxConcurrent: Int = 3) {
    private val semaphore = Semaphore(maxConcurrent)
    
    fun tryAcquire(): Boolean {
        return semaphore.tryAcquire()
    }
    
    fun release() {
        semaphore.release()
    }
    
    fun availableSlots(): Int {
        return semaphore.availablePermits()
    }
}