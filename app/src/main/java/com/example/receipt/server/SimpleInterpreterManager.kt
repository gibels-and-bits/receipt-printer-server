package com.example.receipt.server

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
    
    fun store(teamName: String, code: String): String {
        val teamId = generateTeamId(teamName)
        interpreters[teamId] = code
        teamNames[teamId] = teamName
        teamStats[teamId] = TeamStats()
        return teamId
    }
    
    fun load(teamId: String): InterpreterScript {
        val code = interpreters[teamId] 
            ?: throw NoSuchElementException("No interpreter found for team: $teamId")
        
        val stats = teamStats[teamId] ?: TeamStats()
        teamStats[teamId] = stats.copy(
            lastActivity = System.currentTimeMillis(),
            printJobCount = stats.printJobCount + 1
        )
        
        return InterpreterScript(code)
    }
    
    fun update(teamId: String, code: String) {
        if (!interpreters.containsKey(teamId)) {
            throw NoSuchElementException("No interpreter found for team: $teamId")
        }
        interpreters[teamId] = code
    }
    
    fun exists(teamId: String): Boolean = interpreters.containsKey(teamId)
    
    fun listTeams(): List<Map<String, String>> {
        return interpreters.keys.map { teamId ->
            mapOf(
                "teamId" to teamId,
                "teamName" to (teamNames[teamId] ?: teamId)
            )
        }
    }
    
    fun getTeamData(teamId: String): TeamData {
        val code = interpreters[teamId]
            ?: throw NoSuchElementException("No interpreter found for team: $teamId")
        val name = teamNames[teamId] ?: teamId
        return TeamData(teamName = name, interpreterCode = code)
    }
    
    fun getTeamStats(teamId: String): TeamStats {
        return teamStats[teamId] ?: TeamStats()
    }
    
    private fun generateTeamId(teamName: String): String {
        return teamName
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(50)
    }
}

class InterpreterScript(private val code: String) {
    fun execute(json: String, printer: EpsonPrinter) {
        // Simplified execution - just log for now
        println("Executing interpreter with JSON: $json")
        printer.addText("Receipt printed for team")
        printer.cutPaper()
    }
}