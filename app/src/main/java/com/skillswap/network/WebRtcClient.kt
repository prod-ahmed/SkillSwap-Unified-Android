package com.skillswap.network

import android.content.Context
import android.util.Log
import android.media.AudioManager
import com.skillswap.model.CallIceCandidate
import com.skillswap.model.CallSdp
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class WebRtcClient(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onLocalSdp(sdp: CallSdp)
        fun onIceCandidate(candidate: CallIceCandidate)
        fun onConnectionState(state: String)
        fun onLocalVideoTrack(track: VideoTrack?)
        fun onRemoteVideoTrack(track: VideoTrack?)
    }

    private val eglBase: EglBase = EglBase.create()
    val eglBaseContext: EglBase.Context = eglBase.eglBaseContext

    private val peerConnectionFactory: PeerConnectionFactory
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var peerConnection: PeerConnection? = null
    private var videoCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoEnabled = true

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )
        val encoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBaseContext)
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun start(isVideo: Boolean, asAnswer: Boolean = false) {
        disposePeer()
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isMicrophoneMute = false
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = isVideo
        videoEnabled = isVideo
        buildPeerConnection()
        createMediaStreams(isVideo)
        if (asAnswer) {
            createAnswer()
        } else {
            createOffer()
        }
    }

    private fun buildPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peerConnection = peerConnectionFactory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                listener.onConnectionState(newState?.name ?: "unknown")
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    listener.onIceCandidate(
                        CallIceCandidate(
                            sdp = it.sdp,
                            sdpMid = it.sdpMid,
                            sdpMLineIndex = it.sdpMLineIndex
                        )
                    )
                }
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: org.webrtc.MediaStream?) {}
            override fun onRemoveStream(p0: org.webrtc.MediaStream?) {}
            override fun onDataChannel(p0: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out org.webrtc.MediaStream>?) {
                receiver?.track()?.let { track ->
                    if (track.kind() == MediaStreamTrack.VIDEO_TRACK_KIND) {
                        val remote = track as? VideoTrack
                        listener.onRemoteVideoTrack(remote)
                    }
                }
            }
            override fun onTrack(transceiver: RtpTransceiver?) {
                transceiver?.receiver?.track()?.let { track ->
                    if (track.kind() == MediaStreamTrack.VIDEO_TRACK_KIND) {
                        listener.onRemoteVideoTrack(track as? VideoTrack)
                    }
                }
            }
        })
    }

    private fun createMediaStreams(isVideo: Boolean) {
        audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        audioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource)
        peerConnection?.addTrack(audioTrack)

        if (isVideo) {
            videoCapturer = createVideoCapturer()
            surfaceTextureHelper = runCatching { SurfaceTextureHelper.create("CameraThread", eglBaseContext) }.getOrNull()
            if (surfaceTextureHelper == null || videoCapturer == null) {
                videoEnabled = false
                listener.onLocalVideoTrack(null)
                return
            }
            videoSource = peerConnectionFactory.createVideoSource(false)
            val initialized = runCatching {
                videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
                videoCapturer?.startCapture(640, 480, 30)
                true
            }.getOrDefault(false)
            if (initialized) {
                videoTrack = peerConnectionFactory.createVideoTrack("ARDAMSv0", videoSource)
                videoTrack?.setEnabled(true)
                peerConnection?.addTrack(videoTrack)
                listener.onLocalVideoTrack(videoTrack)
            } else {
                videoEnabled = false
                listener.onLocalVideoTrack(null)
            }
        } else {
            listener.onLocalVideoTrack(null)
        }
    }

    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    setLocalDescription(it)
                    listener.onLocalSdp(CallSdp(it.type.canonicalForm(), it.description))
                }
            }
        }, constraints)
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    setLocalDescription(it)
                    listener.onLocalSdp(CallSdp(it.type.canonicalForm(), it.description))
                }
            }
        }, constraints)
    }

    private fun setLocalDescription(desc: SessionDescription) {
        peerConnection?.setLocalDescription(object : SdpObserverAdapter() {}, desc)
    }

    fun setRemoteSdp(sdp: String, type: String) {
        val sdpType = SessionDescription.Type.fromCanonicalForm(type)
        val desc = SessionDescription(sdpType, sdp)
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                if (sdpType == SessionDescription.Type.OFFER) {
                    createAnswer()
                }
            }
            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            override fun onSetFailure(error: String?) {
                Log.e("WebRtcClient", "Remote SDP set failed: $error")
            }
        }, desc)
    }

    fun addRemoteCandidate(candidate: CallIceCandidate) {
        val ice = IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)
        peerConnection?.addIceCandidate(ice)
    }

    fun toggleMute(mute: Boolean) {
        audioTrack?.setEnabled(!mute)
    }

    fun setVideoEnabled(enabled: Boolean) {
        videoEnabled = enabled
        videoTrack?.setEnabled(enabled)
        if (!enabled) {
            try {
                (videoCapturer as? CameraVideoCapturer)?.stopCapture()
            } catch (_: Exception) {}
        } else {
            try {
                (videoCapturer as? CameraVideoCapturer)?.startCapture(640, 480, 30)
            } catch (_: Exception) {}
        }
    }

    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    fun toggleSpeaker(on: Boolean) {
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = on
    }

    fun dispose() {
        disposePeer()
        eglBase.release()
        audioManager.mode = AudioManager.MODE_NORMAL
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = false
        listener.onConnectionState("disconnected")
    }

    private fun disposePeer() {
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {}
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        videoCapturer?.dispose()
        videoCapturer = null
        videoSource?.dispose()
        videoSource = null
        videoTrack = null
        try {
            audioTrack?.dispose()
        } catch (_: IllegalStateException) {
            // Track may already be disposed when restarting a call after hangup
        }
        audioTrack = null
        audioSource?.dispose()
        audioSource = null
        peerConnection?.close()
        peerConnection = null
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        val frontCamera = deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        val backCamera = deviceNames.firstOrNull { enumerator.isBackFacing(it) }
        val target = frontCamera ?: backCamera
        return target?.let { enumerator.createCapturer(it, null) }
    }

    private open class SdpObserverAdapter : SdpObserver {
        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun onCreateSuccess(sessionDescription: SessionDescription?) {}
        override fun onSetSuccess() {}
        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun onCreateFailure(p0: String?) {}
        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun onSetFailure(p0: String?) {}
    }
}
