package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.CalendarEvent
import com.skillswap.security.SecureStorage
import com.skillswap.model.CreateEventRequest
import com.skillswap.model.UpdateEventRequest
import com.skillswap.network.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = SecureStorage.getInstance(application)
    
    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()
    
    private val _selectedEvent = MutableStateFlow<CalendarEvent?>(null)
    val selectedEvent: StateFlow<CalendarEvent?> = _selectedEvent.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // Google Calendar state
    private val _isGoogleConnected = MutableStateFlow(false)
    val isGoogleConnected: StateFlow<Boolean> = _isGoogleConnected.asStateFlow()
    
    private val _syncInProgress = MutableStateFlow(false)
    val syncInProgress: StateFlow<Boolean> = _syncInProgress.asStateFlow()
    
    private fun authHeader(): String? =
        sharedPreferences.getString("auth_token", null)?.let { "Bearer $it" }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
    
    // ============ Event CRUD ============
    
    fun loadEvents(startDate: String? = null, endDate: String? = null) {
        val token = authHeader() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = NetworkService.api.getCalendarEvents(token, startDate, endDate)
                _events.value = response.events
            } catch (e: Exception) {
                _errorMessage.value = "Erreur de chargement: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadEventsForMonth(year: Int, month: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
        
        loadEvents(startDate, endDate)
    }
    
    fun loadEventDetail(eventId: String) {
        val token = authHeader() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val event = NetworkService.api.getCalendarEvent(token, eventId)
                _selectedEvent.value = event
            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createEvent(
        title: String,
        description: String?,
        startTime: String,
        endTime: String,
        location: String?,
        participants: List<String>?,
        sessionId: String? = null,
        reminder: Int = 15,
        isAllDay: Boolean = false,
        syncToGoogle: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        val token = authHeader() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val request = CreateEventRequest(
                    title = title,
                    description = description,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    participants = participants,
                    sessionId = sessionId,
                    reminder = reminder,
                    isAllDay = isAllDay,
                    syncToGoogle = syncToGoogle
                )
                val created = NetworkService.api.createCalendarEvent(token, request)
                _events.value = _events.value + created
                _successMessage.value = "Événement créé"
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Création échouée: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateEvent(
        eventId: String,
        title: String? = null,
        description: String? = null,
        startTime: String? = null,
        endTime: String? = null,
        location: String? = null,
        status: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val token = authHeader() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = UpdateEventRequest(
                    title = title,
                    description = description,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    status = status
                )
                val updated = NetworkService.api.updateCalendarEvent(token, eventId, request)
                _events.value = _events.value.map { if (it.id == eventId) updated else it }
                _selectedEvent.value = updated
                _successMessage.value = "Événement mis à jour"
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Mise à jour échouée: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteEvent(eventId: String, onSuccess: () -> Unit = {}) {
        val token = authHeader() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                NetworkService.api.deleteCalendarEvent(token, eventId)
                _events.value = _events.value.filter { it.id != eventId }
                _successMessage.value = "Événement supprimé"
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Suppression échouée: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // ============ Participants ============
    
    fun addParticipant(eventId: String, userIdOrEmail: String) {
        val token = authHeader() ?: return
        viewModelScope.launch {
            try {
                val updated = NetworkService.api.addEventParticipant(
                    token, eventId, mapOf("userId" to userIdOrEmail)
                )
                _events.value = _events.value.map { if (it.id == eventId) updated else it }
                _selectedEvent.value = updated
                _successMessage.value = "Participant ajouté"
            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
            }
        }
    }
    
    fun removeParticipant(eventId: String, userId: String) {
        val token = authHeader() ?: return
        viewModelScope.launch {
            try {
                val updated = NetworkService.api.removeEventParticipant(token, eventId, userId)
                _events.value = _events.value.map { if (it.id == eventId) updated else it }
                _selectedEvent.value = updated
                _successMessage.value = "Participant retiré"
            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
            }
        }
    }
    
    fun respondToInvite(eventId: String, response: String) {
        val token = authHeader() ?: return
        viewModelScope.launch {
            try {
                val updated = NetworkService.api.respondToEventInvite(
                    token, eventId, mapOf("response" to response)
                )
                _events.value = _events.value.map { if (it.id == eventId) updated else it }
                _successMessage.value = when (response) {
                    "accepted" -> "Invitation acceptée"
                    "declined" -> "Invitation refusée"
                    else -> "Réponse enregistrée"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
            }
        }
    }
    
    // ============ Google Calendar ============
    
    fun checkGoogleCalendarStatus() {
        val token = authHeader() ?: return
        viewModelScope.launch {
            try {
                val status = NetworkService.api.getGoogleCalendarStatus(token)
                _isGoogleConnected.value = status["connected"] as? Boolean ?: false
            } catch (e: Exception) {
                _isGoogleConnected.value = false
            }
        }
    }
    
    fun getGoogleAuthUrl(onUrl: (String) -> Unit) {
        val token = authHeader() ?: return
        viewModelScope.launch {
            try {
                val response = NetworkService.api.getGoogleCalendarAuthUrl(token)
                onUrl(response.authUrl)
            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
            }
        }
    }
    
    fun handleGoogleCallback(code: String, redirectUri: String? = null, onSuccess: () -> Unit = {}) {
        val token = authHeader() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = com.skillswap.model.GoogleCalendarTokenRequest(
                    code = code,
                    redirectUri = redirectUri
                )
                NetworkService.api.handleGoogleCalendarCallback(token, request)
                _isGoogleConnected.value = true
                _successMessage.value = "Google Calendar connecté"
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Connexion échouée: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun syncWithGoogle(bidirectional: Boolean = true) {
        val token = authHeader() ?: return
        viewModelScope.launch {
            _syncInProgress.value = true
            try {
                val result = NetworkService.api.syncWithGoogleCalendar(
                    token, mapOf("bidirectional" to bidirectional)
                )
                _successMessage.value = "Synchronisé: ${result.synced} événements"
                loadEvents() // Reload to get synced events
            } catch (e: Exception) {
                _errorMessage.value = "Sync échouée: ${e.message}"
            } finally {
                _syncInProgress.value = false
            }
        }
    }
    
    fun disconnectGoogleCalendar() {
        val token = authHeader() ?: return
        viewModelScope.launch {
            try {
                NetworkService.api.disconnectGoogleCalendar(token)
                _isGoogleConnected.value = false
                _successMessage.value = "Google Calendar déconnecté"
            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
            }
        }
    }
    
    // ============ Helpers ============
    
    fun getEventsForDate(date: Date): List<CalendarEvent> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateStr = dateFormat.format(date)
        return _events.value.filter { it.startTime.startsWith(dateStr) }
    }
    
    fun clearSelectedEvent() {
        _selectedEvent.value = null
    }
}
