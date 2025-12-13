package com.skillswap.network

import com.skillswap.BuildConfig
import com.skillswap.model.SocketMessagePayload
import com.skillswap.model.SocketTypingPayload
import com.skillswap.model.CallOfferPayload
import com.skillswap.model.CallAnswerPayload
import com.skillswap.model.CallIcePayload
import com.skillswap.model.CallBusyPayload
import com.skillswap.model.CallEndPayload
import com.skillswap.model.CallRejectPayload
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

class ChatSocketClient(
    private val userIdProvider: () -> String?
) {
    private var chatSocket: Socket? = null
    private var callSocket: Socket? = null

    private val _messages = MutableSharedFlow<SocketMessagePayload>(replay = 0, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages = _messages.asSharedFlow()

    private val _typing = MutableSharedFlow<SocketTypingPayload>(replay = 0, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val typing = _typing.asSharedFlow()
    private val _connection = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val connection = _connection.asSharedFlow()
    private val _callOffers = MutableSharedFlow<CallOfferPayload>(replay = 0, extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val callOffers = _callOffers.asSharedFlow()
    private val _callAnswers = MutableSharedFlow<CallAnswerPayload>(replay = 0, extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val callAnswers = _callAnswers.asSharedFlow()
    private val _callIce = MutableSharedFlow<CallIcePayload>(replay = 0, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val callIce = _callIce.asSharedFlow()
    private val _callEnded = MutableSharedFlow<CallEndPayload>(replay = 0, extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val callEnded = _callEnded.asSharedFlow()
    private val _callRejected = MutableSharedFlow<CallRejectPayload>(replay = 0, extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val callRejected = _callRejected.asSharedFlow()
    private val _callBusy = MutableSharedFlow<CallBusyPayload>(replay = 0, extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val callBusy = _callBusy.asSharedFlow()
    private val _presence = MutableSharedFlow<Map<String, String>>(replay = 0, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val presence = _presence.asSharedFlow()
    private val _readReceipts = MutableSharedFlow<Map<String, Any>>(replay = 0, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val readReceipts = _readReceipts.asSharedFlow()

    fun connect() {
        if (chatSocket?.connected() == true) return
        buildChatSocket()
        chatSocket?.connect()
        buildCallSocket()
        callSocket?.connect()
    }

    fun reconnect() {
        disconnect()
        buildChatSocket()
        chatSocket?.connect()
        buildCallSocket()
        callSocket?.connect()
    }

    fun joinThread(threadId: String) {
        chatSocket?.emit("chat:join", mapOf("threadId" to threadId))
    }

    fun sendTyping(threadId: String, isTyping: Boolean) {
        val payload = mapOf("threadId" to threadId, "isTyping" to isTyping)
        chatSocket?.emit("chat:typing", payload)
    }

    fun sendCallOffer(callId: String, recipientId: String, sdp: String, isVideo: Boolean) {
        callSocket?.emit("call:offer", mapOf("recipientId" to recipientId, "sdp" to sdp, "callType" to if (isVideo) "video" else "audio"))
    }

    fun sendCallAnswer(callId: String, sdp: String) {
        callSocket?.emit("call:answer", mapOf("callId" to callId, "sdp" to sdp))
    }

    fun sendCallIce(callId: String, candidate: String, sdpMid: String?, sdpMLineIndex: Int) {
        callSocket?.emit("call:ice-candidate", mapOf("callId" to callId, "candidate" to candidate, "sdpMid" to sdpMid, "sdpMLineIndex" to sdpMLineIndex))
    }

    fun sendCallEnd(callId: String) {
        callSocket?.emit("call:end", mapOf("callId" to callId))
    }

    fun sendCallReject(callId: String) {
        callSocket?.emit("call:reject", mapOf("callId" to callId))
    }

    fun sendCallBusy(callId: String) {
        callSocket?.emit("call:busy", mapOf("callId" to callId))
    }

    fun disconnect() {
        chatSocket?.disconnect()
        callSocket?.disconnect()
        chatSocket = null
        callSocket = null
        _connection.tryEmit(false)
    }

    private fun buildChatSocket() {
        val userId = userIdProvider() ?: return
        val opts = IO.Options.builder()
            .setQuery("userId=$userId")
            .setReconnection(true)
            .setReconnectionAttempts(8)
            .setReconnectionDelay(1000)
            .setReconnectionDelayMax(8000)
            .build()
        chatSocket = IO.socket(BuildConfig.API_BASE_URL, opts)
        chatSocket?.on(Socket.EVENT_CONNECT) { _connection.tryEmit(true) }
        chatSocket?.on(Socket.EVENT_DISCONNECT) { _connection.tryEmit(false) }
        chatSocket?.on(Socket.EVENT_CONNECT_ERROR) { _connection.tryEmit(false) }
        chatSocket?.on("message:new") { args ->
            args.firstOrNull()?.let {
                if (it is JSONObject) {
                    val id = it.optString("_id", it.optString("id", ""))
                    val threadId = it.optString("threadId")
                    val senderId = it.optString("senderId")
                    val content = it.optString("content")
                    val createdAt = it.optString("createdAt")
                    _messages.tryEmit(SocketMessagePayload(id, threadId, senderId, content, createdAt))
                }
            }
        }
        chatSocket?.on("user:typing") { args ->
            args.firstOrNull()?.let {
                if (it is JSONObject) {
                    val threadId = it.optString("threadId")
                    val userId = it.optString("userId")
                    val isTyping = it.optBoolean("isTyping", false)
                    _typing.tryEmit(SocketTypingPayload(threadId, userId, isTyping))
                }
            }
        }
        chatSocket?.on("chat:presence") { args ->
            args.firstOrNull()?.let {
                if (it is JSONObject) {
                    val userIdPresence = it.optString("userId")
                    val status = it.optString("status")
                    _presence.tryEmit(mapOf("userId" to userIdPresence, "status" to status))
                }
            }
        }
        chatSocket?.on("chat:read") { args ->
            args.firstOrNull()?.let {
                if (it is JSONObject) {
                    val threadId = it.optString("threadId")
                    val readerId = it.optString("readerId")
                    val messageIds = it.optJSONArray("messageIds") ?: org.json.JSONArray()
                    _readReceipts.tryEmit(
                        mapOf(
                            "threadId" to threadId,
                            "readerId" to readerId,
                            "messageIds" to List(messageIds.length()) { idx -> messageIds.optString(idx) }
                        )
                    )
                }
            }
        }
    }

    private fun buildCallSocket() {
        val userId = userIdProvider() ?: return
        val opts = IO.Options.builder()
            .setAuth(mapOf("userId" to userId)) // backend expects userId
            .setReconnection(true)
            .setReconnectionAttempts(8)
            .setReconnectionDelay(1000)
            .setReconnectionDelayMax(8000)
            .build()
        callSocket = IO.socket(BuildConfig.API_BASE_URL + "calling", opts)
        callSocket?.on(Socket.EVENT_CONNECT) { _connection.tryEmit(true) }
        callSocket?.on(Socket.EVENT_DISCONNECT) { _connection.tryEmit(false) }
        callSocket?.on(Socket.EVENT_CONNECT_ERROR) { _connection.tryEmit(false) }
        callSocket?.on("connection:confirmed") { _connection.tryEmit(true) }
        callSocket?.on("call:incoming") { args ->
            args.firstOrNull()?.let {
                if (it is JSONObject) {
                    _callOffers.tryEmit(
                        CallOfferPayload(
                            callId = it.optString("callId", it.optString("_id", "")),
                            callerId = it.optString("callerId"),
                            sdp = it.optString("sdp"),
                            isVideo = it.optString("callType", "audio") == "video",
                            threadId = it.optString("threadId", null)
                        )
                    )
                }
            }
        }
        callSocket?.on("call:answer") { args ->
            args.firstOrNull()?.let {
                if (it is JSONObject) {
                    _callAnswers.tryEmit(
                        CallAnswerPayload(
                            callId = it.optString("callId", it.optString("_id", "")),
                            fromUserId = it.optString("callerId", it.optString("senderId", "")),
                            sdp = it.optString("sdp")
                        )
                    )
                }
            }
        }
        callSocket?.on("call:answered") { args ->
            args.firstOrNull()?.let {
                if (it is JSONObject) {
                    _callAnswers.tryEmit(
                        CallAnswerPayload(
                            callId = it.optString("callId", it.optString("_id", "")),
                            fromUserId = it.optString("recipientId", it.optString("senderId", "")),
                            sdp = it.optString("sdp")
                        )
                    )
                }
            }
        }
        callSocket?.on("call:ice-candidate") { args ->
            args.firstOrNull()?.let {
                if (it is JSONObject) {
                    _callIce.tryEmit(
                        CallIcePayload(
                            callId = it.optString("callId", it.optString("_id", "")),
                            fromUserId = it.optString("senderId", it.optString("callerId")),
                            candidate = it.optString("candidate"),
                            sdpMid = it.optString("sdpMid"),
                            sdpMLineIndex = it.optInt("sdpMLineIndex", 0)
                        )
                    )
                }
            }
        }
        callSocket?.on("call:end") { args ->
            args.firstOrNull()?.let {
                if (it is JSONObject) {
                    _callEnded.tryEmit(
                        CallEndPayload(
                            callId = it.optString("callId", it.optString("_id", "")),
                            fromUserId = it.optString("senderId", it.optString("callerId"))
                        )
                    )
                }
            }
        }
        callSocket?.on("call:reject") { args ->
            args.firstOrNull()?.let {
                if (it is JSONObject) {
                    _callRejected.tryEmit(
                        CallRejectPayload(
                            callId = it.optString("callId", it.optString("_id", "")),
                            fromUserId = it.optString("senderId", it.optString("callerId"))
                        )
                    )
                }
            }
        }
        callSocket?.on("call:busy") { args ->
            args.firstOrNull()?.let {
                if (it is JSONObject) {
                    _callBusy.tryEmit(
                        CallBusyPayload(
                            callId = it.optString("callId", it.optString("_id", "")),
                            fromUserId = it.optString("senderId", it.optString("callerId"))
                        )
                    )
                }
            }
        }
    }
}
