package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.CreateSessionRequest
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
    private val sharedPreferences = application.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)

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
        studentEmail: String,
        date: String,
        duration: Int,
        meetingLink: String?,
        notes: String?
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
                        date = date,
                        duration = duration,
                        meetingLink = meetingLink,
                        notes = notes
                    )
                )
                _sessions.value = _sessions.value + created
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
}
