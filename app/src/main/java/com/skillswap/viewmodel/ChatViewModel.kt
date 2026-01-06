package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.Conversation
import com.skillswap.security.SecureStorage
import com.skillswap.model.Message
import com.skillswap.model.ChatThread
import com.skillswap.network.NetworkService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID
import com.skillswap.network.ChatSocketClient
import kotlinx.coroutines.flow.collectLatest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = SecureStorage.getInstance(application)
    private val socketClient = ChatSocketClient.getInstance(application)

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
                    val threadMatches = it["threadId"] == activeThreadId
                    val ids = it["messageIds"] as? List<*> ?: emptyList<Any>()
                    if (threadMatches && ids.isNotEmpty()) {
                        val updated = _messages.value.map { m ->
                            if (m.isMe && ids.contains(m.id)) m.copy(read = true) else m
                        }
                        _messages.value = updated
                    }
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
    
    fun loadMessagesForThread(threadId: String) {
        loadMessages(threadId)
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

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Load threads first if needed (await completion)
                if (_threads.value.isEmpty()) {
                    val threadsResponse = NetworkService.api.getThreads(header)
                    _threads.value = threadsResponse.items
                    _conversations.value = threadsResponse.items.mapNotNull { thread ->
                        toConversation(thread, me)
                    }
                    hydrateActivePartner(conversationId, me)
                }
                
                val response = NetworkService.api.getMessages(header, conversationId)
                _messages.value = response.items.map { it.toUiMessage(me) }
                startSocket(conversationId)
                markThreadRead(conversationId, response.items.map { it.id })
            } catch (e: Exception) {
                val recovered = if (e is HttpException && (e.code() == 404 || e.code() == 403)) {
                    // If the passed id was actually a userId, create (or fetch) the thread then retry
                    tryRecoverThreadAndReload(conversationId, header, me)
                } else false

                if (!recovered) {
                    _messages.value = emptyList()
                    _error.value = "Impossible de charger les messages"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun tryRecoverThreadAndReload(
        participantId: String,
        header: String,
        me: String
    ): Boolean {
        val thread = runCatching {
            NetworkService.api.createThread(
                header,
                mapOf("participantId" to participantId)
            )
        }.getOrNull() ?: return false

        activeThreadId = thread.id
        // Keep thread metadata for header rendering
        _threads.value = (_threads.value + thread).distinctBy { it.id }
        hydrateActivePartner(thread.id, me)

        return runCatching {
            val response = NetworkService.api.getMessages(header, thread.id)
            _messages.value = response.items.map { it.toUiMessage(me) }
            startSocket(thread.id)
            markThreadRead(thread.id, response.items.map { it.id })
            true
        }.getOrElse { false }
    }
    
    fun sendMessage(text: String, replyTo: com.skillswap.model.ThreadMessage? = null) {
        val threadId = activeThreadId
        val header = authHeader()
        val me = currentUserId()
        if (threadId == null || header == null || me == null) {
            _error.value = "Impossible d'envoyer le message (session expirée)"
            return
        }

        viewModelScope.launch {
            try {
                val payload = mutableMapOf<String, String>(
                    "content" to text,
                    "type" to "text"
                )
                
                // Add replyToId if present (backend expects replyToId, not replyTo)
                replyTo?.let {
                    payload["replyToId"] = it.id
                }
                
                val remote = NetworkService.api.sendMessage(
                    header,
                    threadId,
                    payload
                )
                _messages.value = _messages.value + remote.toUiMessage(me)
            } catch (e: Exception) {
                _error.value = "Message non envoyé: ${e.message}"
            }
        }
    }

    fun uploadAttachment(uri: android.net.Uri, context: android.content.Context) {
        val threadId = activeThreadId
        val header = authHeader()
        val me = currentUserId()
        if (threadId == null || header == null || me == null) {
            _error.value = "Impossible d'envoyer le fichier (session expirée)"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Get file from URI
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
                val bytes = inputStream.readBytes()
                inputStream.close()
                
                // Get file name and mime type
                val fileName = uri.lastPathSegment ?: "file"
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                
                // Create multipart body
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val filePart = okhttp3.MultipartBody.Part.createFormData("file", fileName, requestBody)
                
                val remote = NetworkService.api.uploadChatAttachment(
                    header,
                    threadId,
                    filePart
                )
                _messages.value = _messages.value + remote.toUiMessage(me)
            } catch (e: Exception) {
                _error.value = "Fichier non envoyé: ${e.message}"
            } finally {
                _isLoading.value = false
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
                            id = msg.id.ifBlank { UUID.randomUUID().toString() },
                            text = msg.content,
                            isMe = msg.senderId == me,
                            time = msg.createdAt,
                            read = msg.senderId == me,
                            replyTo = msg.replyTo
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
            launch {
                socketClient.messageReactions.collectLatest { reaction ->
                    val msgId = reaction["messageId"] as? String ?: return@collectLatest
                    val reactionsJson = reaction["reactions"] as? String ?: return@collectLatest
                    val reactions = parseReactions(reactionsJson)
                    val updated = _messages.value.map { m ->
                        if (m.id == msgId) {
                            m.copy(reactions = reactions)
                        } else m
                    }
                    _messages.value = updated
                }
            }
            launch {
                socketClient.messageDeletions.collectLatest { deletedId ->
                    val updated = _messages.value.map { m ->
                        if (m.id == deletedId) {
                            m.copy(text = "🚫 Message supprimé", isDeleted = true)
                        } else m
                    }
                    _messages.value = updated
                }
            }
        }
    }

    fun sendTyping(isTyping: Boolean) {
        activeThreadId?.let { socketClient.sendTyping(it, isTyping) }
    }

    private fun markThreadRead(threadId: String, ids: List<String>?) {
        val header = authHeader() ?: return
        
        // Update local conversation unread count immediately (optimistic update)
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == threadId) conv.copy(unreadCount = 0) else conv
        }
        
        // Also update the threads list
        _threads.value = _threads.value.map { thread ->
            if (thread.id == threadId) thread.copy(unreadCount = 0) else thread
        }
        
        // Then sync with server
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
            time = createdAt,
            read = read || senderId == meId,
            reactions = reactions,
            isDeleted = isDeleted == true,
            replyTo = replyTo,
            attachmentUrl = attachmentUrl,
            type = type
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

    private fun parseReactions(json: String): Map<String, List<String>> {
        return runCatching {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { key ->
                val arr = obj.optJSONArray(key)
                List(arr?.length() ?: 0) { idx -> arr?.optString(idx).orEmpty() }.filter { it.isNotBlank() }
            }.filterValues { it.isNotEmpty() }
        }.getOrDefault(emptyMap())
    }
    
    // ============ PHASE 12: Chat Enhancements ============
    
    private val chatService = com.skillswap.network.ChatService.instance
    
    private val _replyToMessage = MutableStateFlow<com.skillswap.model.ThreadMessage?>(null)
    val replyToMessage: StateFlow<com.skillswap.model.ThreadMessage?> = _replyToMessage.asStateFlow()
    
    /**
     * Add a reaction to a message
     */
    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            try {
                val accessToken = sharedPreferences.getString("auth_token", null) ?: return@launch
                chatService.reactToMessage(messageId, emoji, accessToken)
                // Reload messages to reflect the reaction
                activeThreadId?.let { loadMessages(it) }
            } catch (e: Exception) {
                _error.value = "Failed to add reaction: ${e.message}"
            }
        }
    }
    
    /**
     * Delete a message
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                val accessToken = sharedPreferences.getString("auth_token", null) ?: return@launch
                chatService.deleteMessage(messageId, accessToken)
                // Reload messages to reflect the deletion
                activeThreadId?.let { loadMessages(it) }
            } catch (e: Exception) {
                _error.value = "Failed to delete message: ${e.message}"
            }
        }
    }
    
    /**
     * Set a message to reply to
     */
    fun setReplyToMessage(message: com.skillswap.model.ThreadMessage?) {
        _replyToMessage.value = message
    }
    
    /**
     * Send a message with optional reply
     */
    fun sendMessageWithReply(content: String) {
        val threadId = activeThreadId ?: return
        viewModelScope.launch {
            try {
                val accessToken = sharedPreferences.getString("auth_token", null) ?: return@launch
                val replyToId = _replyToMessage.value?.id
                
                chatService.sendMessage(
                    threadId = threadId,
                    content = content,
                    replyToId = replyToId,
                    accessToken = accessToken
                )
                
                // Clear reply-to state
                _replyToMessage.value = null
                
                // Refresh messages
                loadMessages(threadId)
            } catch (e: Exception) {
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }
}
