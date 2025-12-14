package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.CreateSessionRequest
import com.skillswap.security.SecureStorage
import com.skillswap.model.LessonPlan
import com.skillswap.model.LessonPlanGenerateRequest
import com.skillswap.model.RateSessionRequest
import com.skillswap.model.RescheduleStatus
import com.skillswap.model.Session
import com.skillswap.model.SessionUserSummary
import com.skillswap.model.RescheduleProposalPayload
import com.skillswap.network.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = SecureStorage.getInstance(application)

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _lessonPlan = MutableStateFlow<LessonPlan?>(null)
    val lessonPlan: StateFlow<LessonPlan?> = _lessonPlan.asStateFlow()

    private val _planError = MutableStateFlow<String?>(null)
    val planError: StateFlow<String?> = _planError.asStateFlow()

    private val _planLoading = MutableStateFlow(false)
    val planLoading: StateFlow<Boolean> = _planLoading.asStateFlow()

    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }

    fun clearPlanError() {
        _planError.value = null
    }

    fun loadSessions() {
        val token = sharedPreferences.getString("auth_token", null)
        if (token == null) {
            _errorMessage.value = "User not logged in"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val sessions = NetworkService.api.getSessions("Bearer $token")
                _sessions.value = sessions
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load sessions: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createSession(
        title: String,
        skill: String,
        studentEmail: String? = null,
        studentEmails: List<String>? = null,
        date: String,
        duration: Int,
        meetingLink: String?,
        location: String? = null,
        notes: String?,
        addToCalendar: Boolean = true
    ) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            try {
                val created = NetworkService.api.createSession(
                    "Bearer $token",
                    CreateSessionRequest(
                        title = title,
                        skill = skill,
                        studentEmail = studentEmail,
                        studentEmails = studentEmails,
                        date = date,
                        duration = duration,
                        meetingLink = meetingLink,
                        location = location,
                        notes = notes
                    )
                )
                _sessions.value = _sessions.value + created
                
                // Optionally create calendar event for this session
                if (addToCalendar) {
                    try {
                        val endTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).let { fmt ->
                            val startDate = fmt.parse(date)
                            if (startDate != null) {
                                val endDate = java.util.Date(startDate.time + duration * 60 * 1000L)
                                fmt.format(endDate)
                            } else date
                        }
                        
                        val participants = when {
                            studentEmails != null -> studentEmails
                            studentEmail != null -> listOf(studentEmail)
                            else -> emptyList()
                        }
                        
                        NetworkService.api.createCalendarEvent(
                            "Bearer $token",
                            com.skillswap.model.CreateEventRequest(
                                title = "Session: $title",
                                description = "Session de $skill. ${notes ?: ""}",
                                startTime = date,
                                endTime = endTime,
                                location = location,
                                participants = participants.ifEmpty { null },
                                sessionId = created.id,
                                syncToGoogle = true
                            )
                        )
                    } catch (e: Exception) {
                        // Calendar event creation failed, but session was created
                    }
                }
                
                _successMessage.value = "Session créée"
            } catch (e: Exception) {
                _errorMessage.value = "Création échouée: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun postponeSession(sessionId: String) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updated = NetworkService.api.updateSessionStatus(
                    "Bearer $token",
                    sessionId,
                    mapOf("status" to "reportee")
                )
                upsertSession(updated)
                _successMessage.value = "Session reportée"
            } catch (e: Exception) {
                _errorMessage.value = "Report impossible: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun proposeReschedule(sessionId: String, proposedDate: String, proposedTime: String, note: String?) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val payload = RescheduleProposalPayload(
                    proposedDate = proposedDate,
                    proposedTime = proposedTime,
                    note = note?.ifBlank { null }
                )
                val updated = NetworkService.api.proposeReschedule("Bearer $token", sessionId, payload)
                upsertSession(updated)
                _successMessage.value = "Replanification proposée"
            } catch (e: Exception) {
                _errorMessage.value = "Impossible de proposer: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun respondToReschedule(sessionId: String, accept: Boolean) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updated = NetworkService.api.respondToReschedule(
                    "Bearer $token",
                    sessionId,
                    mapOf("accepted" to accept)
                )
                upsertSession(updated)
                _successMessage.value = if (accept) "Nouveau créneau accepté" else "Proposition refusée"
            } catch (e: Exception) {
                _errorMessage.value = "Réponse impossible: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rateSession(session: Session, rating: Int, comment: String?) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        val me = sharedPreferences.getString("user_id", null)
        val ratedUserId = if (me == session.teacher.id) {
            session.student?.id
        } else {
            session.teacher.id
        }
        if (ratedUserId == null) {
            _errorMessage.value = "Impossible d'identifier l'utilisateur à noter"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                NetworkService.api.rateSession(
                    "Bearer $token",
                    session.id,
                    RateSessionRequest(
                        ratedUserId = ratedUserId,
                        rating = rating,
                        comment = comment?.ifBlank { null }
                    )
                )
                _successMessage.value = "Avis envoyé"
            } catch (e: Exception) {
                _errorMessage.value = "Notation échouée: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadLessonPlan(sessionId: String) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _planLoading.value = true
            _planError.value = null
            try {
                val response = NetworkService.api.getLessonPlan("Bearer $token", sessionId)
                _lessonPlan.value = response.data
            } catch (e: Exception) {
                _planError.value = "Plan introuvable: ${e.message}"
                _lessonPlan.value = null
            } finally {
                _planLoading.value = false
            }
        }
    }

    fun generateLessonPlan(sessionId: String, level: String?, goal: String?) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _planLoading.value = true
            _planError.value = null
            try {
                val response = NetworkService.api.generateLessonPlan(
                    "Bearer $token",
                    sessionId,
                    LessonPlanGenerateRequest(level = level, goal = goal)
                )
                _lessonPlan.value = response.data
            } catch (e: Exception) {
                _planError.value = "Génération échouée: ${e.message}"
                _lessonPlan.value = null
            } finally {
                _planLoading.value = false
            }
        }
    }

    private fun upsertSession(updated: Session) {
        _sessions.value = _sessions.value
            .filter { it.id != updated.id } + updated
    }
    
    // Add missing methods for SessionDetailScreen
    private val _selectedSession = MutableStateFlow<Session?>(null)
    val selectedSession: StateFlow<Session?> = _selectedSession.asStateFlow()
    
    fun loadSessionDetail(sessionId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        
        // Find session in existing list or fetch from API
        val existingSession = _sessions.value.find { it.id == sessionId }
        if (existingSession != null) {
            _selectedSession.value = existingSession
            _isLoading.value = false
        } else {
            // In production, fetch from API
            viewModelScope.launch {
                try {
                    // For now, just set loading to false
                    _isLoading.value = false
                } catch (e: Exception) {
                    _errorMessage.value = e.message
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun confirmSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val token = sharedPreferences.getString("auth_token", null) ?: return@launch
                NetworkService.api.updateSessionStatus(
                    "Bearer $token",
                    sessionId,
                    mapOf("status" to "confirmed")
                )
                _successMessage.value = "Session confirmée"
                loadSessions()
            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
            }
        }
    }
    
    fun cancelSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val token = sharedPreferences.getString("auth_token", null) ?: return@launch
                NetworkService.api.updateSessionStatus(
                    "Bearer $token",
                    sessionId,
                    mapOf("status" to "cancelled")
                )
                _successMessage.value = "Session annulée"
                loadSessions()
            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
            }
        }
    }
    
    // --- User Search & Availability ---
    private val _searchResults = MutableStateFlow<List<com.skillswap.model.User>>(emptyList())
    val searchResults: StateFlow<List<com.skillswap.model.User>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _availabilityStatus = MutableStateFlow<com.skillswap.model.AvailabilityResponse?>(null)
    val availabilityStatus: StateFlow<com.skillswap.model.AvailabilityResponse?> = _availabilityStatus.asStateFlow()
    
    fun searchUsers(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = NetworkService.api.searchUsers("Bearer $token", query, 10)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
    
    fun checkAvailability(userId: String, date: String) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            try {
                val result = NetworkService.api.checkAvailability("Bearer $token", userId, date)
                _availabilityStatus.value = result
            } catch (e: Exception) {
                _availabilityStatus.value = com.skillswap.model.AvailabilityResponse(
                    available = false,
                    message = "Impossible de vérifier: ${e.message}"
                )
            }
        }
    }
    
    fun clearAvailability() {
        _availabilityStatus.value = null
    }
}
