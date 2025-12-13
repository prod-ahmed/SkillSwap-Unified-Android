package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.Conversation
import com.skillswap.model.Message
import com.skillswap.model.ChatThread
import com.skillswap.network.NetworkService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID
import com.skillswap.network.ChatSocketClient
import kotlinx.coroutines.flow.collectLatest

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
    private val socketClient = ChatSocketClient(userIdProvider = { sharedPreferences.getString("user_id", null) })

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _threads = MutableStateFlow<List<ChatThread>>(emptyList())
    val threads: StateFlow<List<ChatThread>> = _threads.asStateFlow()

    // Current active conversation messages
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var activeThreadId: String? = null
    private val _activePartnerName = MutableStateFlow<String?>(null)
    val activePartnerName: StateFlow<String?> = _activePartnerName.asStateFlow()
    private val _activePartnerId = MutableStateFlow<String?>(null)
    val activePartnerId: StateFlow<String?> = _activePartnerId.asStateFlow()
    private val _activePartnerInitials = MutableStateFlow<String>("")
    val activePartnerInitials: StateFlow<String> = _activePartnerInitials.asStateFlow()

    private var pollingJob: Job? = null
    private var socketJob: Job? = null
    private var presenceJob: Job? = null
    private val _partnerTyping = MutableStateFlow(false)
    val partnerTyping: StateFlow<Boolean> = _partnerTyping.asStateFlow()
    private val _socketConnected = MutableStateFlow(false)
    val socketConnected: StateFlow<Boolean> = _socketConnected.asStateFlow()
    private val _presence = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val presence: StateFlow<Map<String, Boolean>> = _presence.asStateFlow()

    private fun authHeader(): String? =
        sharedPreferences.getString("auth_token", null)?.let { "Bearer $it" }

    private fun currentUserId(): String? = sharedPreferences.getString("user_id", null)

    private fun ensurePresenceListener() {
        if (presenceJob != null) return
        socketClient.connect()
        presenceJob = viewModelScope.launch {
            launch {
                socketClient.connection.collectLatest { connected ->
                    _socketConnected.value = connected
                    if (!connected && activeThreadId != null) {
                        delay(1500)
                        socketClient.reconnect()
                        activeThreadId?.let { socketClient.joinThread(it) }
                    } else if (connected && activeThreadId != null) {
                        socketClient.joinThread(activeThreadId!!)
                    }
                }
            }
            launch {
                socketClient.presence.collectLatest { presenceEvent ->
                    val userId = presenceEvent["userId"]
                    val status = presenceEvent["status"]
                    if (userId != null && status != null) {
                        _presence.value = _presence.value + (userId to (status == "online"))
                    }
                }
            }
            launch {
                socketClient.readReceipts.collectLatest {
                    // placeholder: messages UI not yet showing read state
                }
            }
        }
    }

    fun loadConversations() {
        val header = authHeader()
        val me = currentUserId()
        if (header == null || me == null) {
            _error.value = "Session expirée"
            return
        }

        ensurePresenceListener()
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = NetworkService.api.getThreads(header)
                _threads.value = response.items
                _conversations.value = response.items.mapNotNull { thread ->
                    toConversation(thread, me)
                }
            } catch (e: Exception) {
                _error.value = "Impossible de charger les conversations: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadMessages(conversationId: String) {
        activeThreadId = conversationId
        val header = authHeader()
        val me = currentUserId()
        if (header == null || me == null) {
            _messages.value = emptyList()
            _socketConnected.value = false
            _error.value = "Session expirée"
            return
        }

        hydrateActivePartner(conversationId, me)

        if (_threads.value.isEmpty()) {
            // ensure we have metadata for header if navigation came directly
            loadConversations()
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = NetworkService.api.getMessages(header, conversationId)
                _messages.value = response.items.map { it.toUiMessage(me) }
                startSocket(conversationId)
                markThreadRead(conversationId, response.items.map { it.id })
            } catch (e: Exception) {
                _messages.value = emptyList()
                _error.value = "Impossible de charger les messages"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun sendMessage(text: String) {
        val threadId = activeThreadId
        val header = authHeader()
        val me = currentUserId()
        if (threadId == null || header == null || me == null) {
            _error.value = "Impossible d'envoyer le message (session expirée)"
            return
        }

        viewModelScope.launch {
            try {
                val remote = NetworkService.api.sendMessage(
                    header,
                    threadId,
                    mapOf("content" to text, "type" to "text")
                )
                _messages.value = _messages.value + remote.toUiMessage(me)
            } catch (e: Exception) {
                _error.value = "Message non envoyé: ${e.message}"
            }
        }
    }

    private fun startSocket(threadId: String) {
        socketJob?.cancel()
        socketClient.reconnect()
        ensurePresenceListener()
        socketJob = viewModelScope.launch {
            socketClient.joinThread(threadId)
            launch {
                socketClient.messages.collectLatest { msg ->
                    val me = currentUserId() ?: return@collectLatest
                    if (msg.threadId == threadId) {
                        _messages.value = _messages.value + Message(
                            id = UUID.randomUUID().toString(),
                            text = msg.content,
                            isMe = msg.senderId == me,
                            time = msg.createdAt
                        )
                        if (msg.senderId != me) {
                            markThreadRead(threadId, null)
                        }
                    }
                }
            }
            launch {
                socketClient.typing.collectLatest { typing ->
                    val me = currentUserId() ?: return@collectLatest
                    if (typing.threadId == threadId && typing.userId != me) {
                        _partnerTyping.value = typing.isTyping
                    }
                }
            }
        }
    }

    fun sendTyping(isTyping: Boolean) {
        activeThreadId?.let { socketClient.sendTyping(it, isTyping) }
    }

    private fun markThreadRead(threadId: String, ids: List<String>?) {
        val header = authHeader() ?: return
        viewModelScope.launch {
            runCatching {
                NetworkService.api.markThreadRead(
                    header,
                    threadId,
                    mapOf("ids" to ids)
                )
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        socketClient.disconnect()
        pollingJob?.cancel()
        socketJob?.cancel()
        presenceJob?.cancel()
    }

    private fun loadMockConversations() {
        _conversations.value = listOf(
            Conversation("1", "u1", "Mohamed Ali", "Salut! Comment ça va?", "10:30", 2),
            Conversation("2", "u2", "Sarah Ben", "Merci pour la session!", "Hier", 0),
            Conversation("3", "u3", "Ahmed Khelifi", "À demain", "Lun", 1)
        )
    }

    private fun hydrateActivePartner(threadId: String, me: String) {
        val thread = _threads.value.firstOrNull { it.id == threadId }
        val partner = thread?.participants?.firstOrNull { it.id != me } ?: thread?.participants?.firstOrNull()
        val name = partner?.username ?: "Conversation"
        _activePartnerName.value = name
        _activePartnerId.value = partner?.id
        _activePartnerInitials.value = name.split(" ").take(2).joinToString("") { it.take(1) }.uppercase()
    }

    private fun com.skillswap.model.ThreadMessage.toUiMessage(meId: String): Message {
        return Message(
            id = id,
            text = content,
            isMe = senderId == meId,
            time = createdAt
        )
    }

    private fun toConversation(thread: ChatThread, me: String): Conversation {
        val partner = thread.participants.firstOrNull { it.id != me } ?: thread.participants.firstOrNull()
        val preview = thread.lastMessage?.content ?: thread.metadata?.get("lastPreview")?.toString().orEmpty()
        return Conversation(
            id = thread.id,
            partnerId = partner?.id ?: thread.id,
            partnerName = partner?.username ?: "Conversation",
            lastMessage = preview,
            timestamp = thread.lastMessageAt ?: "",
            unreadCount = thread.unreadCount
        )
    }
}
