package com.skillswap.services

import android.content.Context
import android.util.Log
import com.skillswap.BuildConfig
import com.skillswap.auth.AuthenticationManager
import com.skillswap.util.TokenUtils
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class SocketService private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: SocketService? = null

        fun getInstance(context: Context): SocketService {
            return instance ?: synchronized(this) {
                instance ?: SocketService(context.applicationContext).also { instance = it }
            }
        }
    }

    private val tag = "SocketService"
    private val authManager = AuthenticationManager.getInstance(context)

    private var socket: Socket? = null
    private var hasListeners = false

    private val incomingCallListeners = mutableListOf<(Any) -> Unit>()
    private val callRingingListeners = mutableListOf<(Any) -> Unit>()
    private val callAnsweredListeners = mutableListOf<(Any) -> Unit>()
    private val iceCandidateListeners = mutableListOf<(Any) -> Unit>()
    private val callEndedListeners = mutableListOf<() -> Unit>()
    private val callRejectedListeners = mutableListOf<() -> Unit>()
    private val callBusyListeners = mutableListOf<() -> Unit>()
    private val callErrorListeners = mutableListOf<(Any) -> Unit>()

    var isConnected: Boolean = false
        private set

    private fun normalizedBaseUrl(): String? {
        val raw = BuildConfig.API_BASE_URL.trim()
        if (raw.isEmpty()) return null
        return if (raw.startsWith("http")) raw else "https://$raw"
    }

    private fun buildSocket(): Socket? {
        val userId = authManager.getUserId() ?: return null
        val token = authManager.getToken()?.takeUnless { TokenUtils.isTokenExpired(it) } ?: return null
        val baseUrl = normalizedBaseUrl() ?: return null

        val opts = IO.Options.builder()
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .setReconnectionDelay(1000)
            .setReconnectionDelayMax(8000)
            .setForceNew(true)
            .setQuery("userId=$userId")
            .apply {
                token?.let {
                    setExtraHeaders(mapOf("Authorization" to listOf("Bearer $it")))
                }
            }
            .build()

        val callUrl = if (baseUrl.endsWith("/")) "${baseUrl}calling" else "$baseUrl/calling"
        return runCatching { IO.socket(callUrl, opts) }
            .onFailure { Log.e(tag, "Failed to create socket: ${it.message}") }
            .getOrNull()
    }

    fun onCallError(callback: (Any) -> Unit) {
        callErrorListeners += callback
    }

    fun onIceCandidate(callback: (Any) -> Unit) {
        iceCandidateListeners += callback
    }

    fun onCallEnded(callback: () -> Unit) {
        callEndedListeners += callback
    }

    fun onCallRejected(callback: () -> Unit) {
        callRejectedListeners += callback
    }

    fun onCallBusy(callback: () -> Unit) {
        callBusyListeners += callback
    }

    fun onIncomingCall(callback: (Any) -> Unit) {
        incomingCallListeners += callback
    }

    fun onCallRinging(callback: (Any) -> Unit) {
        callRingingListeners += callback
    }

    fun onCallAnswered(callback: (Any) -> Unit) {
        callAnsweredListeners += callback
    }

    fun emit(event: String, vararg args: Any?) {
        if (socket?.connected() != true) {
            Log.w(tag, "Cannot emit $event - socket not connected")
            return
        }
        socket?.emit(event, *args)
        Log.d(tag, "Socket emit: $event")
    }

    fun emitIceCandidate(callId: String, candidateData: Any) {
        val data = JSONObject().apply {
            put("callId", callId)
            put("candidate", candidateData)
        }
        emit("call:ice-candidate", data)
    }

    fun emitCallAnswer(callId: String, answer: String) {
        val data = JSONObject().apply {
            put("callId", callId)
            put("sdp", answer)
        }
        emit("call:answer", data)
    }

    fun emitCallEnd(callId: String) {
        val data = JSONObject().apply {
            put("callId", callId)
        }
        emit("call:end", data)
    }

    fun emitCallReject(callId: String) {
        val data = JSONObject().apply {
            put("callId", callId)
        }
        emit("call:reject", data)
    }

    fun emitCallBusy(callId: String) {
        val data = JSONObject().apply {
            put("callId", callId)
        }
        emit("call:busy", data)
    }

    fun emitCallOffer(recipientId: String, sdp: String, isVideo: Boolean) {
        val data = JSONObject().apply {
            put("recipientId", recipientId)
            put("sdp", sdp)
            put("callType", if (isVideo) "video" else "audio")
        }
        emit("call:offer", data)
    }

    fun connect() {
        if (socket?.connected() == true) {
            Log.d(tag, "Socket already connected")
            return
        }

        socket = buildSocket()
        if (socket == null) {
            Log.e(tag, "Socket not created (missing base URL or userId)")
            return
        }

        registerListeners()
        socket?.connect()
        Log.d(tag, "Connecting socket...")
    }

    fun disconnect() {
        socket?.disconnect()
        isConnected = false
        hasListeners = false
        socket = null
        Log.d(tag, "Socket disconnected")
    }

    private fun registerListeners() {
        if (hasListeners) return
        hasListeners = true

        socket?.on(Socket.EVENT_CONNECT) {
            isConnected = true
            Log.d(tag, "Socket connected to /calling namespace")
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            isConnected = false
            Log.d(tag, "Socket disconnected from /calling namespace")
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            isConnected = false
            Log.e(tag, "Socket connection error: ${args.joinToString()}")
        }

        socket?.on("call:incoming") { args ->
            args.firstOrNull()?.let { payload ->
                incomingCallListeners.forEach { it(payload) }
            }
        }

        socket?.on("call:ringing") { args ->
            args.firstOrNull()?.let { payload ->
                callRingingListeners.forEach { it(payload) }
            }
        }

        socket?.on("call:answered") { args ->
            args.firstOrNull()?.let { payload ->
                callAnsweredListeners.forEach { it(payload) }
            }
        }

        socket?.on("call:ice-candidate") { args ->
            args.firstOrNull()?.let { payload ->
                iceCandidateListeners.forEach { it(payload) }
            }
        }

        socket?.on("call:ended") {
            callEndedListeners.forEach { it() }
        }

        socket?.on("call:rejected") {
            callRejectedListeners.forEach { it() }
        }

        socket?.on("call:busy") {
            callBusyListeners.forEach { it() }
        }

        socket?.on("call:error") { args ->
            args.firstOrNull()?.let { payload ->
                callErrorListeners.forEach { it(payload) }
            }
        }
    }
}
