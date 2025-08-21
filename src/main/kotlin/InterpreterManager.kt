package com.example.receipt.server

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import com.example.receipt.server.printer.EpsonPrinter
import java.util.concurrent.ConcurrentHashMap

data class TeamData(
    val teamName: String,
    val interpreterCode: String
)

data class TeamStats(
    val lastActivity: Long? = null,
    val printJobCount: Int = 0
)

class InterpreterManager {
    private val interpreters = ConcurrentHashMap<String, String>()
    private val teamNames = ConcurrentHashMap<String, String>()
    private val teamStats = ConcurrentHashMap<String, TeamStats>()
    private val scriptEngineManager = ScriptEngineManager()
    
    /**
     * Store interpreter code for a team
     * @return teamId generated from team name
     */
    fun store(teamName: String, code: String): String {
        val teamId = generateTeamId(teamName)
        val wrappedCode = wrapInterpreterCode(code)
        interpreters[teamId] = code  // Store original code for display
        teamNames[teamId] = teamName
        teamStats[teamId] = TeamStats()
        return teamId
    }
    
    /**
     * Load interpreter for a team
     * @throws NoSuchElementException if no interpreter found
     */
    fun load(teamId: String): InterpreterScript {
        val code = interpreters[teamId] 
            ?: throw NoSuchElementException("No interpreter found for team: $teamId")
        
        // Update last activity
        val stats = teamStats[teamId] ?: TeamStats()
        teamStats[teamId] = stats.copy(
            lastActivity = System.currentTimeMillis(),
            printJobCount = stats.printJobCount + 1
        )
        
        val wrappedCode = wrapInterpreterCode(code)
        return InterpreterScript(wrappedCode)
    }
    
    /**
     * Update existing interpreter
     * @throws NoSuchElementException if no interpreter found
     */
    fun update(teamId: String, code: String) {
        if (!interpreters.containsKey(teamId)) {
            throw NoSuchElementException("No interpreter found for team: $teamId")
        }
        interpreters[teamId] = code  // Store original code
    }
    
    /**
     * Check if interpreter exists for team
     */
    fun exists(teamId: String): Boolean = interpreters.containsKey(teamId)
    
    /**
     * List all registered teams
     */
    fun listTeams(): List<Map<String, String>> {
        return interpreters.keys.map { teamId ->
            mapOf(
                "teamId" to teamId,
                "teamName" to (teamNames[teamId] ?: teamId)
            )
        }
    }
    
    /**
     * Get team data for admin display
     */
    fun getTeamData(teamId: String): TeamData {
        val code = interpreters[teamId]
            ?: throw NoSuchElementException("No interpreter found for team: $teamId")
        val name = teamNames[teamId] ?: teamId
        return TeamData(teamName = name, interpreterCode = code)
    }
    
    /**
     * Get team statistics
     */
    fun getTeamStats(teamId: String): TeamStats {
        return teamStats[teamId] ?: TeamStats()
    }
    
    /**
     * Generate team ID from team name
     */
    private fun generateTeamId(teamName: String): String {
        return teamName
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(50) // Limit length
    }
    
    /**
     * Wrap user code with necessary imports and error handling
     */
    private fun wrapInterpreterCode(code: String): String {
        return """
            import com.example.receipt.server.printer.*
            import kotlinx.serialization.json.*
            
            // User code starts here
            $code
            // User code ends here
            
            // Entry point for execution - calls the user's interpret function
            fun executeInterpreter(jsonString: String, printer: EpsonPrinter) {
                try {
                    interpret(jsonString, printer)
                } catch (e: Exception) {
                    // If interpreter fails, print error on receipt
                    printer.addText("INTERPRETER ERROR", TextStyle(bold = true, size = TextSize.LARGE))
                    printer.addFeedLine(1)
                    printer.addText("Error: ${'$'}{e.message ?: "Unknown error"}")
                    printer.addFeedLine(2)
                    printer.addText("Please check your code and try again")
                    printer.addFeedLine(3)
                    printer.cutPaper()
                    throw e
                }
            }
        """.trimIndent()
    }
}

/**
 * Wrapper for executing Kotlin script interpreter
 */
class InterpreterScript(private val code: String) {
    
    /**
     * Execute the interpreter with given JSON and printer
     */
    fun execute(json: String, printer: EpsonPrinter) {
        val engine = ScriptEngineManager().getEngineByExtension("kts")
            ?: throw IllegalStateException("Kotlin script engine not available")
        
        try {
            // Set up bindings
            val bindings = engine.createBindings()
            bindings["jsonInput"] = json
            bindings["printerInstance"] = printer
            
            // Compile and execute the interpreter code
            engine.eval(code, bindings)
            
            // Call the entry point function
            engine.eval("""
                executeInterpreter(jsonInput as String, printerInstance as EpsonPrinter)
            """.trimIndent(), bindings)
            
        } catch (e: ScriptException) {
            throw RuntimeException("Script execution failed: ${e.message}", e)
        } catch (e: Exception) {
            throw RuntimeException("Interpreter execution failed: ${e.message}", e)
        }
    }
}