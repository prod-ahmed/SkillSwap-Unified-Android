package com.skillswap.services

import android.content.Context
import android.util.Log
import org.webrtc.*

interface WebRTCClientDelegate {
    fun onLocalIceCandidate(candidate: IceCandidate)
    fun onConnectionStateChanged(state: PeerConnection.IceConnectionState)
    fun onRemoteVideoTrack(track: VideoTrack?)
}

class WebRTCClient(
    private val context: Context,
    private val iceServers: List<String>,
    private val isVideo: Boolean = false,
    private val delegate: WebRTCClientDelegate
) {
    private val tag = "WebRTCClient"
    
    private val peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    
    private var videoCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    
    private var localVideoRenderer: SurfaceViewRenderer? = null
    private var remoteVideoRenderer: SurfaceViewRenderer? = null
    
    private val audioConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
    }
    
    private val sdpConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideo) "true" else "false"))
    }
    
    init {
        Log.d(tag, "Initializing WebRTC (isVideo: $isVideo)")
        
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
        
        val videoEncoderFactory = DefaultVideoEncoderFactory(
            EglBase.create().eglBaseContext,
            true,
            true
        )
        val videoDecoderFactory = DefaultVideoDecoderFactory(EglBase.create().eglBaseContext)
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .createPeerConnectionFactory()
        
        createPeerConnection()
        createMediaTracks()
    }
    
    private fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(
            iceServers.map { PeerConnection.IceServer.builder(it).createIceServer() }
        ).apply {
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        
        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                Log.d(tag, "Local ICE candidate: ${candidate.sdp}")
                delegate.onLocalIceCandidate(candidate)
            }
            
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                Log.d(tag, "ICE connection state: $state")
                delegate.onConnectionStateChanged(state)
            }
            
            override fun onAddStream(stream: MediaStream) {
                Log.d(tag, "Remote stream added: ${stream.videoTracks.size} video tracks")
                if (stream.videoTracks.isNotEmpty()) {
                    remoteVideoTrack = stream.videoTracks[0]
                    delegate.onRemoteVideoTrack(remoteVideoTrack)
                    remoteVideoRenderer?.let { renderer ->
                        remoteVideoTrack?.addSink(renderer)
                    }
                }
            }
            
            override fun onRemoveStream(stream: MediaStream) {
                Log.d(tag, "Remote stream removed")
                remoteVideoTrack = null
                delegate.onRemoteVideoTrack(null)
            }
            
            override fun onSignalingChange(state: PeerConnection.SignalingState) {
                Log.d(tag, "Signaling state: $state")
            }
            
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                Log.d(tag, "ICE gathering state: $state")
            }
            
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onDataChannel(dataChannel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(tag, "ICE connection receiving change: $receiving")
            }
        }
        
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, observer)
        Log.d(tag, "PeerConnection created")
    }
    
    private fun createMediaTracks() {
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource)
        peerConnection?.addStream(peerConnectionFactory.createLocalMediaStream("local_stream").apply {
            addTrack(localAudioTrack)
        })
        
        if (isVideo) {
            startCaptureLocalVideo()
        }
        
        Log.d(tag, "Media tracks created (video: $isVideo)")
    }
    
    fun startCaptureLocalVideo() {
        if (!isVideo || localVideoTrack != null) return
        
        val cameraEnumerator = Camera2Enumerator(context)
        val frontCameraName = cameraEnumerator.deviceNames.firstOrNull { 
            cameraEnumerator.isFrontFacing(it) 
        } ?: cameraEnumerator.deviceNames.firstOrNull()
        
        frontCameraName?.let { cameraName ->
            videoCapturer = cameraEnumerator.createCapturer(cameraName, null)
            
            val surfaceTextureHelper = SurfaceTextureHelper.create(
                "CaptureThread",
                EglBase.create().eglBaseContext
            )
            
            videoSource = peerConnectionFactory.createVideoSource(videoCapturer!!.isScreencast)
            videoCapturer?.initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)
            videoCapturer?.startCapture(1280, 720, 30)
            
            localVideoTrack = peerConnectionFactory.createVideoTrack("video_track", videoSource)
            peerConnection?.addStream(peerConnectionFactory.createLocalMediaStream("video_stream").apply {
                addTrack(localVideoTrack)
            })
            
            localVideoRenderer?.let { renderer ->
                localVideoTrack?.addSink(renderer)
            }
            
            Log.d(tag, "Local video capture started")
        }
    }
    
    fun createOffer(onSuccess: (SessionDescription) -> Unit) {
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.d(tag, "Offer created successfully")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.d(tag, "Local description set")
                        onSuccess(sdp)
                    }
                    override fun onSetFailure(error: String) {
                        Log.e(tag, "Failed to set local description: $error")
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sdp)
            }
            
            override fun onCreateFailure(error: String) {
                Log.e(tag, "Failed to create offer: $error")
            }
            
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, sdpConstraints)
    }
    
    fun createAnswer(onSuccess: (SessionDescription) -> Unit) {
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.d(tag, "Answer created successfully")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.d(tag, "Local description set")
                        onSuccess(sdp)
                    }
                    override fun onSetFailure(error: String) {
                        Log.e(tag, "Failed to set local description: $error")
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sdp)
            }
            
            override fun onCreateFailure(error: String) {
                Log.e(tag, "Failed to create answer: $error")
            }
            
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, sdpConstraints)
    }
    
    fun setRemoteDescription(sdp: SessionDescription, onSuccess: () -> Unit = {}) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(tag, "Remote description set successfully")
                onSuccess()
            }
            
            override fun onSetFailure(error: String) {
                Log.e(tag, "Failed to set remote description: $error")
            }
            
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sdp)
    }
    
    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
        Log.d(tag, "ICE candidate added")
    }
    
    fun renderLocalVideo(renderer: SurfaceViewRenderer) {
        localVideoRenderer = renderer
        localVideoTrack?.addSink(renderer)
    }
    
    fun renderRemoteVideo(renderer: SurfaceViewRenderer) {
        remoteVideoRenderer = renderer
        remoteVideoTrack?.addSink(renderer)
    }
    
    fun muteAudio() {
        localAudioTrack?.setEnabled(false)
    }
    
    fun unmuteAudio() {
        localAudioTrack?.setEnabled(true)
    }
    
    fun enableVideo() {
        localVideoTrack?.setEnabled(true)
        videoCapturer?.startCapture(1280, 720, 30)
    }
    
    fun disableVideo() {
        localVideoTrack?.setEnabled(false)
        videoCapturer?.stopCapture()
    }
    
    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }
    
    fun close() {
        Log.d(tag, "Closing WebRTC client")
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        videoSource?.dispose()
        audioSource?.dispose()
        peerConnection?.close()
        peerConnection?.dispose()
    }
}
