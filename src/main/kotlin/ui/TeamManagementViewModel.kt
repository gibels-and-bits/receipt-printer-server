package com.example.receipt.server.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.receipt.server.InterpreterManager
import com.example.receipt.server.PrinterManager
import com.example.receipt.server.QueueManager
import com.example.receipt.server.ui.screens.TeamDetail
import com.example.receipt.server.ui.screens.TeamInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class DashboardUiState(
    val teams: List<TeamInfo> = emptyList(),
    val printerEnabledCount: Int = 0,
    val queueStatus: Pair<Int, Int> = 0 to 3,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TeamDetailUiState(
    val teamDetail: TeamDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastTestResult: String? = null
)

class TeamManagementViewModel(
    private val interpreterManager: InterpreterManager,
    private val printerManager: PrinterManager,
    private val queueManager: QueueManager
) : ViewModel() {
    
    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()
    
    private val _teamDetailState = MutableStateFlow(TeamDetailUiState())
    val teamDetailState: StateFlow<TeamDetailUiState> = _teamDetailState.asStateFlow()
    
    init {
        loadTeams()
        // Auto-refresh dashboard
        viewModelScope.launch {
            while (true) {
                delay(5000)
                loadTeams()
            }
        }
    }
    
    fun loadTeams() {
        viewModelScope.launch {
            _dashboardState.value = _dashboardState.value.copy(isLoading = true)
            
            try {
                val teams = interpreterManager.listTeams().map { teamMap ->
                    val teamId = teamMap["teamId"] ?: ""
                    val teamName = teamMap["teamName"] ?: ""
                    val stats = interpreterManager.getTeamStats(teamId)
                    
                    TeamInfo(
                        teamId = teamId,
                        teamName = teamName,
                        printerEnabled = printerManager.isRealPrintEnabled(teamId),
                        lastActivity = stats.lastActivity,
                        printJobCount = stats.printJobCount
                    )
                }
                
                val enabledCount = printerManager.getEnabledTeams().size
                val availableSlots = queueManager.availableSlots()
                val maxSlots = 3
                val usedSlots = maxSlots - availableSlots
                
                _dashboardState.value = DashboardUiState(
                    teams = teams,
                    printerEnabledCount = enabledCount,
                    queueStatus = usedSlots to maxSlots,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    error = "Failed to load teams: ${e.message}"
                )
            }
        }
    }
    
    fun loadTeamDetail(teamId: String) {
        viewModelScope.launch {
            _teamDetailState.value = _teamDetailState.value.copy(isLoading = true)
            
            try {
                val teamData = interpreterManager.getTeamData(teamId)
                val stats = interpreterManager.getTeamStats(teamId)
                
                _teamDetailState.value = TeamDetailUiState(
                    teamDetail = TeamDetail(
                        teamId = teamId,
                        teamName = teamData.teamName,
                        interpreterCode = teamData.interpreterCode,
                        printerEnabled = printerManager.isRealPrintEnabled(teamId),
                        lastActivity = stats.lastActivity,
                        printJobCount = stats.printJobCount
                    ),
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _teamDetailState.value = _teamDetailState.value.copy(
                    isLoading = false,
                    error = "Failed to load team details: ${e.message}"
                )
            }
        }
    }
    
    fun togglePrinterAccess(teamId: String) {
        viewModelScope.launch {
            try {
                if (printerManager.isRealPrintEnabled(teamId)) {
                    printerManager.disableRealPrint(teamId)
                } else {
                    printerManager.enableRealPrint(teamId)
                }
                
                // Reload both dashboard and detail
                loadTeams()
                loadTeamDetail(teamId)
                
            } catch (e: Exception) {
                _teamDetailState.value = _teamDetailState.value.copy(
                    error = "Failed to toggle printer access: ${e.message}"
                )
            }
        }
    }
    
    fun testPrint(teamId: String, jsonString: String) {
        viewModelScope.launch {
            try {
                val interpreter = interpreterManager.load(teamId)
                val printer = if (printerManager.isRealPrintEnabled(teamId)) {
                    printerManager.getRealPrinter()
                } else {
                    printerManager.getMockPrinter()
                }
                
                interpreter.execute(jsonString, printer)
                
                _teamDetailState.value = _teamDetailState.value.copy(
                    lastTestResult = "Test print successful! Mode: ${
                        if (printerManager.isRealPrintEnabled(teamId)) "REAL" else "MOCK"
                    }"
                )
                
                // Reload to update print count
                loadTeamDetail(teamId)
                
            } catch (e: Exception) {
                _teamDetailState.value = _teamDetailState.value.copy(
                    lastTestResult = "Test print failed: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _dashboardState.value = _dashboardState.value.copy(error = null)
        _teamDetailState.value = _teamDetailState.value.copy(error = null)
    }
    
    fun clearTestResult() {
        _teamDetailState.value = _teamDetailState.value.copy(lastTestResult = null)
    }
}