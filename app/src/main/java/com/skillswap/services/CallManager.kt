package com.skillswap.services

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.skillswap.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.*

class CallManager private constructor(private val context: Context) : WebRTCClientDelegate {
    
    companion object {
        @Volatile
        private var instance: CallManager? = null
        
        fun getInstance(context: Context): CallManager {
            return instance ?: synchronized(this) {
                instance ?: CallManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val tag = "CallManager"
    private val socketService = SocketService.getInstance(context)
    
    var isCallActive by mutableStateOf(false)
        private set
    var callStatus by mutableStateOf("")
        private set
    var remoteUser by mutableStateOf<User?>(null)
        private set
    var isMuted by mutableStateOf(false)
        private set
    var isSpeakerOn by mutableStateOf(false)
        private set
    var isVideoEnabled by mutableStateOf(false)
        private set
    var isVideoCall by mutableStateOf(false)
        private set
    var remoteVideoTrack by mutableStateOf<VideoTrack?>(null)
        private set
    
    private var webRTCClient: WebRTCClient? = null
    private var currentCallId: String? = null
    private var isInitiator = false
    private var callTimeoutJob: Job? = null
    
    private val iceServers = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302",
        "stun:stun2.l.google.com:19302"
    )
    
    init {
        Log.d(tag, "Initializing CallManager")
        socketService.connect()
        setupSocketListeners()
    }
    
    private fun setupSocketListeners() {
        Log.d(tag, "Setting up socket listeners")
        
        socketService.onIncomingCall { data ->
            Log.d(tag, "Incoming call: $data")
            handleIncomingCall(data as? JSONObject ?: JSONObject())
        }
        
        socketService.onCallRinging { data ->
            Log.d(tag, "Call ringing")
            callStatus = "Ringing..."
        }
        
        socketService.onCallAnswered { data ->
            Log.d(tag, "Call answered: $data")
            handleCallAnswered(data as? JSONObject ?: JSONObject())
        }
        
        socketService.onIceCandidate { data ->
            Log.d(tag, "ICE candidate received")
            handleRemoteCandidate(data as? JSONObject ?: JSONObject())
        }
        
        socketService.onCallEnded {
            Log.d(tag, "Call ended by remote")
            endCall("Call ended")
        }
        
        socketService.onCallRejected {
            Log.d(tag, "Call rejected")
            endCall("Call rejected")
        }
        
        socketService.onCallBusy {
            Log.d(tag, "User busy")
            endCall("User is busy")
        }
        
        socketService.onCallError { data ->
            Log.e(tag, "Call error: $data")
            val message = (data as? JSONObject)?.optString("message") ?: "Unknown error"
            callStatus = "Error: $message"
        }
    }
    
    fun startCall(recipientId: String, recipientName: String, isVideo: Boolean = false) {
        if (isCallActive) return

        if (!socketService.isConnected) {
            Log.w(tag, "Socket not connected, attempting to connect before starting call")
            socketService.connect()
        }
        
        Log.d(tag, "Starting call to $recipientName (video: $isVideo)")
        
        isInitiator = true
        isCallActive = true
        this.isVideoCall = isVideo
        this.isVideoEnabled = isVideo
        callStatus = "Calling..."
        remoteUser = User(
            id = recipientId, 
            username = recipientName, 
            email = "", 
            role = "user",
            credits = null,
            ratingAvg = null,
            isVerified = null
        )
        
        webRTCClient = WebRTCClient(context, iceServers, isVideo, this)
        
        if (isVideo) {
            webRTCClient?.startCaptureLocalVideo()
        }
        
        startCallTimeout()
        
        webRTCClient?.createOffer { sdp ->
            Log.d(tag, "Offer created, emitting to socket")
            socketService.emitCallOffer(
                recipientId = recipientId,
                sdp = sdp.description,
                isVideo = isVideo
            )
        }
    }
    
    fun answerCall() {
        Log.d(tag, "Answering call")
        callStatus = "Connecting..."
        
        webRTCClient?.createAnswer { sdp ->
            val callId = currentCallId ?: return@createAnswer
            Log.d(tag, "Answer created, emitting to socket")
            socketService.emitCallAnswer(callId, sdp.description)
        }
    }
    
    fun endCall(reason: String = "Call ended") {
        Log.d(tag, "Ending call: $reason")
        
        currentCallId?.let { callId ->
            if (isCallActive) {
                socketService.emitCallEnd(callId)
            } else {
                socketService.emitCallReject(callId)
            }
        }
        
        cleanup(reason)
    }
    
    fun toggleMute() {
        isMuted = !isMuted
        if (isMuted) {
            webRTCClient?.muteAudio()
        } else {
            webRTCClient?.unmuteAudio()
        }
    }
    
    fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        // Audio routing would be handled by Android audio manager
    }
    
    fun toggleVideo() {
        isVideoEnabled = !isVideoEnabled
        if (isVideoEnabled) {
            webRTCClient?.enableVideo()
        } else {
            webRTCClient?.disableVideo()
        }
    }
    
    fun switchCamera() {
        webRTCClient?.switchCamera()
    }
    
    private fun handleIncomingCall(data: JSONObject) {
        val callId = data.optString("callId")
        val callerId = data.optString("callerId")
        val sdp = data.optString("sdp")
        val callType = data.optString("callType", "audio")
        val isVideo = callType == "video"
        
        if (callId.isEmpty() || callerId.isEmpty() || sdp.isEmpty()) {
            Log.e(tag, "Invalid incoming call data")
            return
        }
        
        Log.d(tag, "Handling incoming call: $callId (video: $isVideo)")
        
        if (isCallActive) {
            Log.d(tag, "Already in call, sending busy")
            socketService.emitCallBusy(callId)
            return
        }
        
        currentCallId = callId
        isInitiator = false
        this.isVideoCall = isVideo
        this.isVideoEnabled = isVideo
        remoteUser = User(
            id = callerId,
            username = "Incoming Call",
            email = "",
            role = "user",
            credits = null,
            ratingAvg = null,
            isVerified = null
        )
        isCallActive = true
        callStatus = "Incoming call..."
        
        webRTCClient = WebRTCClient(context, iceServers, isVideo, this)
        
        if (isVideo) {
            webRTCClient?.startCaptureLocalVideo()
        }
        
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)
        webRTCClient?.setRemoteDescription(sessionDescription)
    }
    
    private fun handleCallAnswered(data: JSONObject) {
        val sdp = data.optString("sdp")
        val callId = data.optString("callId")
        
        if (sdp.isEmpty() || callId.isEmpty()) return
        
        Log.d(tag, "Call answered, setting remote description")
        currentCallId = callId
        callStatus = "Connected"
        cancelCallTimeout()
        
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        webRTCClient?.setRemoteDescription(sessionDescription)
    }
    
    private fun handleRemoteCandidate(data: JSONObject) {
        val candidateData = data.optJSONObject("candidate") ?: return
        val sdp = candidateData.optString("candidate")
        val sdpMid = candidateData.optString("sdpMid")
        val sdpMLineIndex = candidateData.optInt("sdpMLineIndex", -1)
        
        if (sdp.isEmpty() || sdpMLineIndex == -1) return
        
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        webRTCClient?.addIceCandidate(candidate)
    }
    
    private fun startCallTimeout() {
        callTimeoutJob?.cancel()
        callTimeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(30_000)
            Log.d(tag, "Call timeout after 30 seconds")
            endCall("No answer")
        }
    }
    
    private fun cancelCallTimeout() {
        callTimeoutJob?.cancel()
        callTimeoutJob = null
    }
    
    private fun cleanup(reason: String) {
        Log.d(tag, "Cleaning up: $reason")
        cancelCallTimeout()
        
        isCallActive = false
        callStatus = reason
        currentCallId = null
        isMuted = false
        isSpeakerOn = false
        isVideoEnabled = false
        isVideoCall = false
        remoteVideoTrack = null
        
        webRTCClient?.close()
        webRTCClient = null
    }
    
    // WebRTCClientDelegate implementation
    override fun onLocalIceCandidate(candidate: IceCandidate) {
        val callId = currentCallId ?: return
        
        val candidateData = mapOf(
            "candidate" to candidate.sdp,
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex
        )
        
        socketService.emitIceCandidate(callId, candidateData)
    }
    
    override fun onConnectionStateChanged(state: PeerConnection.IceConnectionState) {
        Log.d(tag, "Connection state: $state")
        when (state) {
            PeerConnection.IceConnectionState.CONNECTED,
            PeerConnection.IceConnectionState.COMPLETED -> {
                callStatus = "Connected"
                cancelCallTimeout()
            }
            PeerConnection.IceConnectionState.DISCONNECTED -> {
                callStatus = "Disconnected"
                endCall("Disconnected")
            }
            PeerConnection.IceConnectionState.FAILED -> {
                callStatus = "Failed"
                endCall("Connection failed")
            }
            PeerConnection.IceConnectionState.CLOSED -> {
                callStatus = "Closed"
            }
            else -> {}
        }
    }
    
    override fun onRemoteVideoTrack(track: VideoTrack?) {
        Log.d(tag, "Remote video track: ${track != null}")
        remoteVideoTrack = track
    }
}
