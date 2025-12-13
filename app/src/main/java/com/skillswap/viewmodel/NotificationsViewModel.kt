package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.NotificationItem
import com.skillswap.network.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class NotificationPrefs(
    val chat: Boolean = true,
    val calls: Boolean = true,
    val marketing: Boolean = false
)

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _prefs = MutableStateFlow(loadPrefsFromStorage())
    val prefs: StateFlow<NotificationPrefs> = _prefs.asStateFlow()

    fun loadNotifications() {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = NetworkService.api.getNotifications("Bearer $token", limit = 50)
                _notifications.value = response.items
                _unreadCount.value = response.items.count { !it.isRead }
                runCatching {
                    NetworkService.api.getUnreadCount("Bearer $token")["unread"] ?: _unreadCount.value
                }.onSuccess { serverCount -> _unreadCount.value = serverCount }
                _prefs.value = loadPrefsFromStorage()
            } catch (_: Exception) {
                _error.value = "Impossible de charger les notifications"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAllRead() {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            try {
                NetworkService.api.markAllNotificationsRead("Bearer $token")
                _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                _unreadCount.value = 0
            } catch (_: Exception) {
                _error.value = "Échec du marquage"
            }
        }
    }

    fun markRead(notificationId: String) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        val current = _notifications.value
        if (current.none { it.id == notificationId }) return

        val locallyRead = current.map { if (it.id == notificationId) it.copy(isRead = true) else it }
        _notifications.value = locallyRead
        _unreadCount.value = locallyRead.count { !it.isRead }

        viewModelScope.launch {
            runCatching {
                NetworkService.api.markNotificationsRead(
                    "Bearer $token",
                    mapOf("ids" to listOf(notificationId))
                )
            }.onFailure {
                _error.value = "Impossible de marquer comme lu"
            }
        }
    }

    fun respond(notificationId: String, accept: Boolean) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            try {
                val updated = NetworkService.api.respondNotification(
                    "Bearer $token",
                    notificationId,
                    mapOf("accepted" to accept, "message" to if (accept) "Accepté" else "Refusé")
                )
                _notifications.value = _notifications.value.map { if (it.id == notificationId) updated else it }
                _unreadCount.value = _notifications.value.count { !it.isRead }
                _message.value = if (accept) "Demande acceptée" else "Demande refusée"
            } catch (e: Exception) {
                _error.value = "Réponse impossible: ${e.message}"
            }
        }
    }

    fun clearMessage() { _message.value = null }
    fun clearError() { _error.value = null }

    fun updatePrefs(chat: Boolean? = null, calls: Boolean? = null, marketing: Boolean? = null) {
        val current = _prefs.value
        val updated = current.copy(
            chat = chat ?: current.chat,
            calls = calls ?: current.calls,
            marketing = marketing ?: current.marketing
        )
        _prefs.value = updated
        persistPrefs(updated)
    }

    private fun loadPrefsFromStorage(): NotificationPrefs {
        return NotificationPrefs(
            chat = sharedPreferences.getBoolean("notif_chat", true),
            calls = sharedPreferences.getBoolean("notif_calls", true),
            marketing = sharedPreferences.getBoolean("notif_marketing", false)
        )
    }

    private fun persistPrefs(prefs: NotificationPrefs) {
        sharedPreferences.edit()
            .putBoolean("notif_chat", prefs.chat)
            .putBoolean("notif_calls", prefs.calls)
            .putBoolean("notif_marketing", prefs.marketing)
            .apply()
    }
}
