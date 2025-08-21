package com.example.receipt.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    // Install content negotiation for JSON
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    // Install CORS for hackathon (allow all origins)
    install(CORS) {
        anyHost() // For hackathon only - restrict in production!
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
    }
    
    // Install status pages for error handling
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError, 
                mapOf("error" to (cause.message ?: "Unknown error"))
            )
            cause.printStackTrace()
        }
        
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to (cause.message ?: "Invalid request"))
            )
        }
        
        exception<NoSuchElementException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to (cause.message ?: "Resource not found"))
            )
        }
    }
    
    // Create shared managers
    val interpreterManager = InterpreterManager()
    val printerManager = PrinterManager()
    val queueManager = QueueManager(maxConcurrent = 3)
    
    // Set up routing
    routing {
        setupRoutes(interpreterManager, printerManager, queueManager)
        setupAdminRoutes(interpreterManager, printerManager, queueManager)
        
        // Health check endpoint
        get("/health") {
            call.respond(mapOf("status" to "healthy", "timestamp" to System.currentTimeMillis()))
        }
    }
}