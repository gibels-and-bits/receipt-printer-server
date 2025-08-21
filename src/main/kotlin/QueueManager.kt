package com.example.receipt.server

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages concurrent access to printer resources
 * Prevents server overload during hackathon
 */
class QueueManager(private val maxConcurrent: Int = 3) {
    private val semaphore = Semaphore(maxConcurrent)
    private val activeJobs = AtomicInteger(0)
    private val totalProcessed = AtomicInteger(0)
    
    /**
     * Try to acquire a slot for processing
     * @return true if slot acquired, false if queue is full
     */
    fun tryAcquire(): Boolean {
        val acquired = semaphore.tryAcquire()
        if (acquired) {
            activeJobs.incrementAndGet()
            println("Queue slot acquired. Active jobs: ${activeJobs.get()}/$maxConcurrent")
        } else {
            println("Queue is FULL. All $maxConcurrent slots are in use.")
        }
        return acquired
    }
    
    /**
     * Acquire a slot, blocking if necessary
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return true if slot acquired within timeout
     */
    fun acquire(timeoutMs: Long = 5000): Boolean {
        val acquired = try {
            semaphore.tryAcquire(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            false
        }
        
        if (acquired) {
            activeJobs.incrementAndGet()
            println("Queue slot acquired after wait. Active jobs: ${activeJobs.get()}/$maxConcurrent")
        }
        return acquired
    }
    
    /**
     * Release a slot back to the pool
     */
    fun release() {
        semaphore.release()
        activeJobs.decrementAndGet()
        totalProcessed.incrementAndGet()
        println("Queue slot released. Active jobs: ${activeJobs.get()}/$maxConcurrent")
        println("Total jobs processed: ${totalProcessed.get()}")
    }
    
    /**
     * Get number of available slots
     */
    fun availableSlots(): Int = semaphore.availablePermits()
    
    /**
     * Get number of active jobs
     */
    fun activeJobCount(): Int = activeJobs.get()
    
    /**
     * Get total number of processed jobs
     */
    fun totalProcessedCount(): Int = totalProcessed.get()
    
    /**
     * Check if queue is full
     */
    fun isFull(): Boolean = availableSlots() == 0
    
    /**
     * Get queue statistics
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "maxConcurrent" to maxConcurrent,
            "availableSlots" to availableSlots(),
            "activeJobs" to activeJobCount(),
            "totalProcessed" to totalProcessedCount(),
            "isFull" to isFull()
        )
    }
    
    /**
     * Reset statistics (for testing)
     */
    fun resetStats() {
        totalProcessed.set(0)
        println("Queue statistics reset")
    }
}