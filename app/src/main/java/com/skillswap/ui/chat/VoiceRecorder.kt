package com.skillswap.ui.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException

enum class VoiceRecordingState {
    IDLE,
    RECORDING,
    RECORDED,
    PLAYING
}

@Composable
fun VoiceRecorderButton(
    onVoiceRecorded: (File) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFFF6B35)
) {
    val context = LocalContext.current
    var recordingState by remember { mutableStateOf(VoiceRecordingState.IDLE) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    // Recording timer
    LaunchedEffect(recordingState) {
        if (recordingState == VoiceRecordingState.RECORDING) {
            recordingDuration = 0
            while (recordingState == VoiceRecordingState.RECORDING) {
                delay(1000)
                recordingDuration++
                if (recordingDuration >= 120) { // Max 2 minutes
                    stopRecording(mediaRecorder) { file ->
                        recordedFile = file
                        recordingState = VoiceRecordingState.RECORDED
                    }
                }
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.release()
            mediaPlayer?.release()
        }
    }
    
    // Pulsing animation for recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val buttonColor by animateColorAsState(
        targetValue = when (recordingState) {
            VoiceRecordingState.RECORDING -> Color.Red
            VoiceRecordingState.RECORDED -> accentColor
            VoiceRecordingState.PLAYING -> Color.Green
            else -> accentColor.copy(alpha = 0.1f)
        },
        label = "buttonColor"
    )
    
    when (recordingState) {
        VoiceRecordingState.IDLE -> {
            // Mic button to start recording
            IconButton(
                onClick = {
                    if (hasAudioPermission(context)) {
                        startRecording(context) { recorder, file ->
                            mediaRecorder = recorder
                            recordedFile = file
                            recordingState = VoiceRecordingState.RECORDING
                        }
                    }
                },
                modifier = modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(buttonColor)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Enregistrer un message vocal",
                    tint = accentColor
                )
            }
        }
        
        VoiceRecordingState.RECORDING -> {
            // Recording indicator with stop button
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Red.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulsing red dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
                
                // Duration
                Text(
                    text = formatDuration(recordingDuration),
                    color = Color.Red,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                
                // Cancel button
                IconButton(
                    onClick = {
                        cancelRecording(mediaRecorder, recordedFile)
                        mediaRecorder = null
                        recordedFile = null
                        recordingState = VoiceRecordingState.IDLE
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Annuler", tint = Color.Gray)
                }
                
                // Stop button
                IconButton(
                    onClick = {
                        stopRecording(mediaRecorder) { file ->
                            recordedFile = file
                            recordingState = VoiceRecordingState.RECORDED
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Arrêter", tint = Color.White)
                }
            }
        }
        
        VoiceRecordingState.RECORDED -> {
            // Playback controls with send option
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play button
                IconButton(
                    onClick = {
                        recordedFile?.let { file ->
                            playAudio(file) { player ->
                                mediaPlayer = player
                                recordingState = VoiceRecordingState.PLAYING
                            }
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Écouter", tint = Color.White)
                }
                
                // Duration
                Text(
                    text = formatDuration(recordingDuration),
                    color = accentColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                
                // Delete button
                IconButton(
                    onClick = {
                        recordedFile?.delete()
                        recordedFile = null
                        recordingDuration = 0
                        recordingState = VoiceRecordingState.IDLE
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Gray)
                }
                
                // Send button
                IconButton(
                    onClick = {
                        recordedFile?.let { file ->
                            onVoiceRecorded(file)
                            recordedFile = null
                            recordingDuration = 0
                            recordingState = VoiceRecordingState.IDLE
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Envoyer", tint = Color.White)
                }
            }
        }
        
        VoiceRecordingState.PLAYING -> {
            // Playing indicator
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Green.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stop playing button
                IconButton(
                    onClick = {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        recordingState = VoiceRecordingState.RECORDED
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Green)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Arrêter", tint = Color.White)
                }
                
                Text(
                    text = "Lecture...",
                    color = Color.Green,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
            
            // Auto-return to recorded state when playback ends
            LaunchedEffect(mediaPlayer) {
                mediaPlayer?.setOnCompletionListener {
                    recordingState = VoiceRecordingState.RECORDED
                }
            }
        }
    }
}

private fun hasAudioPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}

private fun startRecording(context: Context, onStarted: (MediaRecorder, File) -> Unit) {
    val outputFile = File(context.cacheDir, "voice_message_${System.currentTimeMillis()}.m4a")
    
    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }
    
    try {
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        onStarted(recorder, outputFile)
    } catch (e: IOException) {
        android.util.Log.e("VoiceRecorder", "Failed to start recording: ${e.message}")
        recorder.release()
    }
}

private fun stopRecording(recorder: MediaRecorder?, onStopped: (File?) -> Unit) {
    try {
        recorder?.apply {
            stop()
            release()
        }
    } catch (e: Exception) {
        android.util.Log.e("VoiceRecorder", "Failed to stop recording: ${e.message}")
    }
    // The file should already be saved
    onStopped(null)
}

private fun cancelRecording(recorder: MediaRecorder?, file: File?) {
    try {
        recorder?.apply {
            stop()
            release()
        }
    } catch (e: Exception) {
        // Ignore
    }
    file?.delete()
}

private fun playAudio(file: File, onStarted: (MediaPlayer) -> Unit) {
    val player = MediaPlayer()
    try {
        player.apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
        }
        onStarted(player)
    } catch (e: IOException) {
        android.util.Log.e("VoiceRecorder", "Failed to play audio: ${e.message}")
        player.release()
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
