package com.skillswap.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.skillswap.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

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
    private var socket: Socket? = null
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
    
    init {
        try {
            val opts = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 10000
            }
            
            val baseUrl = BuildConfig.API_BASE_URL.replace("/api", "")
            socket = IO.socket("$baseUrl/calling", opts)
            
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(tag, "Socket connected to /calling namespace")
                isConnected = true
                authenticateSocket()
            }
            
            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(tag, "Socket disconnected")
                isConnected = false
            }
            
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(tag, "Socket connection error: ${args.joinToString()}")
                isConnected = false
            }
            
        } catch (e: URISyntaxException) {
            Log.e(tag, "Socket URI error", e)
        }
    }
    
    private fun authenticateSocket() {
        val userId = sharedPreferences.getString("user_id", null)
        val authToken = sharedPreferences.getString("auth_token", null)
        
        if (userId != null && authToken != null) {
            val authData = JSONObject().apply {
                put("userId", userId)
                put("token", authToken)
            }
            socket?.emit("authenticate", authData)
            Log.d(tag, "Sent authentication with userId: $userId")
        } else {
            Log.w(tag, "Missing userId or authToken for socket authentication")
        }
    }
    
    fun on(event: String, callback: (Array<Any>) -> Unit) {
        socket?.on(event) { args ->
            Log.d(tag, "Socket event received: $event")
            callback(args)
        }
    }
    
    fun onCallError(callback: (Any) -> Unit) {
        on("call:error") { args ->
            if (args.isNotEmpty()) callback(args[0])
        }
    }
    
    fun onIceCandidate(callback: (Any) -> Unit) {
        on("ice-candidate") { args ->
            if (args.isNotEmpty()) callback(args[0])
        }
    }
    
    fun onCallEnded(callback: () -> Unit) {
        on("call:ended") { callback() }
    }
    
    fun onCallRejected(callback: () -> Unit) {
        on("call:rejected") { callback() }
    }
    
    fun onCallBusy(callback: () -> Unit) {
        on("call:busy") { callback() }
    }
    
    fun onIncomingCall(callback: (Any) -> Unit) {
        on("call:incoming") { args ->
            if (args.isNotEmpty()) callback(args[0])
        }
    }
    
    fun onCallRinging(callback: (Any) -> Unit) {
        on("call:ringing") { args ->
            if (args.isNotEmpty()) callback(args[0])
        }
    }
    
    fun onCallAnswered(callback: (Any) -> Unit) {
        on("call:answered") { args ->
            if (args.isNotEmpty()) callback(args[0])
        }
    }
    
    fun emit(event: String, vararg args: Any?) {
        if (socket?.connected() == true) {
            socket?.emit(event, *args)
            Log.d(tag, "Socket emit: $event")
        } else {
            Log.w(tag, "Cannot emit $event - socket not connected")
        }
    }
    
    fun emitIceCandidate(callId: String, candidateData: Any) {
        val data = JSONObject().apply {
            put("callId", callId)
            put("candidate", candidateData)
        }
        emit("ice-candidate", data)
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
    
    var isConnected: Boolean = false
        private set
    
    fun connect() {
        if (socket?.connected() == false) {
            socket?.connect()
            Log.d(tag, "Connecting socket...")
        }
    }
    
    fun disconnect() {
        socket?.disconnect()
        isConnected = false
        Log.d(tag, "Socket disconnected")
    }
}
