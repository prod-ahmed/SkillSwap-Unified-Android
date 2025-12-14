package com.skillswap.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.CallIceCandidate
import com.skillswap.model.CallSdp
import com.skillswap.model.CallOfferPayload
import com.skillswap.network.ChatSocketClient
import com.skillswap.network.WebRtcClient
import com.skillswap.utils.LocalNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.webrtc.VideoTrack

private enum class CallRole { CALLER, CALLEE }

data class CallState(
    val isInCall: Boolean = false,
    val callId: String? = null,
    val isVideo: Boolean = false,
    val videoEnabled: Boolean = true,
    val partnerName: String = "",
    val muted: Boolean = false,
    val speakerOn: Boolean = false,
    val isRinging: Boolean = false,
    val connectionStatus: String = "idle",
    val ended: Boolean = false,
    val localSdp: String? = null,
    val remoteSdp: String? = null,
    val iceCandidates: List<CallIceCandidate> = emptyList(),
    val callDurationSec: Int = 0,
    val error: String? = null
)

class CallViewModel(application: Application) : AndroidViewModel(application), WebRtcClient.Listener {
    private val _state = MutableStateFlow(CallState())
    val state: StateFlow<CallState> = _state.asStateFlow()
    private val rtcClient = WebRtcClient(application.applicationContext, this)
    private val socketClient = ChatSocketClient(
        context = application.applicationContext,
        userIdProvider = { application.getSharedPreferences("SkillSwapPrefs", android.content.Context.MODE_PRIVATE).getString("user_id", null) }
    )
    private val notificationManager by lazy { LocalNotificationManager.getInstance(application) }
    private var threadId: String? = null
    private var partnerId: String? = null
    private var callId: String? = null
    private var role: CallRole = CallRole.CALLER
    private var pendingOffer: CallOfferPayload? = null
    private var observingSocket = false
    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()
    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()
    val eglBaseContext get() = rtcClient.eglBaseContext
    private var callTimerJob: Job? = null
    private var endedClearJob: Job? = null

    init {
        socketClient.connect()
        observeSocket()
    }

    fun startCall(partnerName: String, video: Boolean, partnerId: String? = null, threadId: String? = null) {
        this.threadId = threadId
        this.partnerId = partnerId
        callId = java.util.UUID.randomUUID().toString()
        role = CallRole.CALLER
        _state.value = CallState(isInCall = true, isVideo = video, videoEnabled = video, partnerName = partnerName, callId = callId)
        socketClient.connect()
        threadId?.let { socketClient.joinThread(it) }
        observeSocket()
        startTimer()
        rtcClient.start(video)
    }

    fun hangUp() {
        callId?.let { cid ->
            socketClient.sendCallEnd(cid)
        }
        callId = null
        _state.value = _state.value.copy(isInCall = false, isRinging = false, ended = true)
        rtcClient.dispose()
        _localVideoTrack.value = null
        _remoteVideoTrack.value = null
        stopTimer()
        scheduleEndedClear()
    }

    fun toggleMute() {
        _state.value = _state.value.copy(muted = !_state.value.muted)
        rtcClient.toggleMute(_state.value.muted)
    }

    fun toggleVideo() {
        if (!_state.value.isVideo) return
        val newValue = !_state.value.videoEnabled
        _state.value = _state.value.copy(videoEnabled = newValue)
        rtcClient.setVideoEnabled(newValue)
    }

    fun switchCamera() {
        rtcClient.switchCamera()
    }

    fun toggleSpeaker() {
        val newValue = !_state.value.speakerOn
        _state.value = _state.value.copy(speakerOn = newValue)
        rtcClient.toggleSpeaker(newValue)
    }

    fun applyRemoteSdp(sdp: String, type: String = "answer") {
        rtcClient.setRemoteSdp(sdp, type)
        _state.value = _state.value.copy(remoteSdp = sdp)
    }

    fun addRemoteCandidate(candidate: CallIceCandidate) {
        rtcClient.addRemoteCandidate(candidate)
    }

    override fun onLocalSdp(sdp: CallSdp) {
        _state.value = _state.value.copy(localSdp = sdp.sdp)
        when (role) {
            CallRole.CALLER -> {
                val recipient = partnerId ?: threadId
                if (recipient != null) {
                    socketClient.sendCallOffer(callId.orEmpty(), recipient, sdp.sdp, _state.value.isVideo)
                }
            }
            CallRole.CALLEE -> socketClient.sendCallAnswer(callId.orEmpty(), sdp.sdp)
        }
    }

    override fun onIceCandidate(candidate: CallIceCandidate) {
        _state.value = _state.value.copy(iceCandidates = _state.value.iceCandidates + candidate)
        socketClient.sendCallIce(callId.orEmpty(), candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex)
    }

    override fun onConnectionState(state: String) {
        _state.value = _state.value.copy(connectionStatus = state)
    }

    override fun onLocalVideoTrack(track: VideoTrack?) {
        _localVideoTrack.value = track
    }

    override fun onRemoteVideoTrack(track: VideoTrack?) {
        _remoteVideoTrack.value = track
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun acceptIncomingCall() {
        val offer = pendingOffer ?: return
        threadId = offer.threadId
        partnerId = offer.callerId
        callId = offer.callId
        role = CallRole.CALLEE
        _state.value = _state.value.copy(isRinging = false, isInCall = true, isVideo = offer.isVideo, videoEnabled = offer.isVideo, callId = callId, ended = false)
        offer.threadId?.let { socketClient.joinThread(it) }
        applyRemoteSdp(offer.sdp, "offer")
        rtcClient.start(offer.isVideo, asAnswer = true)
        startTimer()
    }

    fun declineIncomingCall() {
        pendingOffer?.let { offer ->
            socketClient.sendCallReject(offer.callId)
        }
        callId = null
        pendingOffer = null
        _state.value = CallState()
        rtcClient.dispose()
        stopTimer()
    }

    private fun observeSocket() {
        if (observingSocket) return
        observingSocket = true
        
        // Chat Notifications
        viewModelScope.launch {
            socketClient.messages.collectLatest { msg ->
                val prefs = getApplication<Application>().getSharedPreferences("SkillSwapPrefs", android.content.Context.MODE_PRIVATE)
                val myId = prefs.getString("user_id", "")
                
                if (msg.senderId != myId) {
                    notificationManager.showMessageNotification(
                        threadId = msg.threadId,
                        senderName = "Nouveau message", // Placeholder as payload lacks name
                        messageText = msg.content
                    )
                }
            }
        }

        viewModelScope.launch {
            socketClient.callOffers.collectLatest { offer ->
                val busy = _state.value.isInCall && _state.value.callId != offer.callId
                if (busy) {
                    socketClient.sendCallBusy(offer.callId)
                    return@collectLatest
                }
                pendingOffer = offer
                threadId = offer.threadId ?: threadId
                partnerId = offer.callerId
                callId = offer.callId
                role = CallRole.CALLEE
                _state.value = _state.value.copy(
                    isRinging = true,
                    isInCall = true,
                    partnerName = _state.value.partnerName.ifBlank { "Contact" },
                    isVideo = offer.isVideo,
                    callId = callId,
                    error = null
                )
                offer.threadId?.let { socketClient.joinThread(it) }
            }
        }
        viewModelScope.launch {
            socketClient.callAnswers.collectLatest { answer ->
                val callMatches = answer.callId == callId || callId.isNullOrBlank() || answer.callId.isBlank()
                if (callMatches) {
                    if (callId.isNullOrBlank()) callId = answer.callId
                    applyRemoteSdp(answer.sdp, "answer")
                }
            }
        }
        viewModelScope.launch {
            socketClient.callIce.collectLatest { ice ->
                val callMatches = ice.callId == callId || callId.isNullOrBlank() || ice.callId.isBlank()
                if (callMatches) {
                    if (callId.isNullOrBlank()) callId = ice.callId
                    addRemoteCandidate(CallIceCandidate(ice.candidate, ice.sdpMid, ice.sdpMLineIndex))
                }
            }
        }
        viewModelScope.launch {
            socketClient.callEnded.collectLatest { ended ->
                val callMatches = ended.callId == callId || callId.isNullOrBlank() || ended.callId.isBlank()
                if (callMatches) {
                    _state.value = _state.value.copy(isInCall = false, isRinging = false, ended = true)
                    rtcClient.dispose()
                    stopTimer()
                    scheduleEndedClear()
                }
            }
        }
        viewModelScope.launch {
            socketClient.callRejected.collectLatest { rejected ->
                val callMatches = rejected.callId == callId || callId.isNullOrBlank() || rejected.callId.isBlank()
                if (callMatches) {
                    _state.value = CallState(error = "Call rejected")
                    rtcClient.dispose()
                    callId = null
                    stopTimer()
                    scheduleEndedClear()
                }
            }
        }
        viewModelScope.launch {
            socketClient.callBusy.collectLatest { busy ->
                val callMatches = busy.callId == callId || callId.isNullOrBlank() || busy.callId.isBlank()
                if (callMatches) {
                    _state.value = CallState(error = "User busy")
                    rtcClient.dispose()
                    callId = null
                    stopTimer()
                    scheduleEndedClear()
                }
            }
        }
    }

    fun clearEnded() {
        _state.value = CallState()
    }

    private fun startTimer() {
        callTimerJob?.cancel()
        _state.value = _state.value.copy(callDurationSec = 0)
        callTimerJob = viewModelScope.launch {
            var elapsed = 0
            while (true) {
                delay(1000)
                elapsed += 1
                _state.value = _state.value.copy(callDurationSec = elapsed)
            }
        }
    }

    private fun stopTimer() {
        callTimerJob?.cancel()
        callTimerJob = null
    }

    private fun scheduleEndedClear() {
        endedClearJob?.cancel()
        endedClearJob = viewModelScope.launch {
            delay(1500)
            _state.value = CallState()
            _localVideoTrack.value = null
            _remoteVideoTrack.value = null
        }
    }
    
    // Add missing methods for VideoCallScreen
    private val _callState = MutableStateFlow("idle")
    val callState: StateFlow<String> = _callState.asStateFlow()
    
    fun setupRemoteRenderer(renderer: org.webrtc.SurfaceViewRenderer) {
        rtcClient.setupRemoteRenderer(renderer)
    }
    
    fun setupLocalRenderer(renderer: org.webrtc.SurfaceViewRenderer) {
        rtcClient.setupLocalRenderer(renderer)
    }
    
    fun initializeCall(sessionId: String) {
        _callState.value = "connecting"
        viewModelScope.launch {
            try {
                rtcClient.initializePeerConnection()
                _callState.value = "connected"
            } catch (e: Exception) {
                _callState.value = "error"
            }
        }
    }
    
    fun toggleMute(mute: Boolean) {
        _state.value = _state.value.copy(muted = mute)
        rtcClient.toggleAudio(!mute)
    }
    
    fun toggleCamera(enable: Boolean) {
        _state.value = _state.value.copy(videoEnabled = enable)
        rtcClient.toggleVideo(enable)
    }
    
    fun toggleSpeaker(enable: Boolean) {
        _state.value = _state.value.copy(speakerOn = enable)
    }
}
