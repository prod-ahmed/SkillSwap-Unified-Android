package com.skillswap.ui.guidedtour

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Stores layout coordinates for tour targets
 */
object TourTargetRegistry {
    private val targets = mutableStateMapOf<String, LayoutCoordinates>()
    
    fun register(id: String, coordinates: LayoutCoordinates) {
        targets[id] = coordinates
    }
    
    fun unregister(id: String) {
        targets.remove(id)
    }
    
    fun getRect(id: String): Rect? {
        return targets[id]?.takeIf { it.isAttached }?.boundsInRoot()
    }
    
    fun clear() {
        targets.clear()
    }
}

/**
 * Coach mark overlay with spotlight effect
 */
@Composable
fun CoachMarkOverlay(
    step: TourStep,
    targetRect: Rect?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    isLastStep: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val spotlightPadding = 12.dp
    val tooltipWidth = 280.dp
    
    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    // Tooltip appear animation
    var tooltipVisible by remember { mutableStateOf(false) }
    LaunchedEffect(step) {
        tooltipVisible = false
        kotlinx.coroutines.delay(200)
        tooltipVisible = true
    }
    val tooltipAlpha by animateFloatAsState(
        targetValue = if (tooltipVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "tooltipAlpha"
    )
    
    val density = LocalDensity.current
    val spotlightPaddingPx = with(density) { spotlightPadding.toPx() }
    
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        
        // Dark overlay with spotlight cutout
        if (targetRect != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val expandedRect = Rect(
                    left = targetRect.left - spotlightPaddingPx,
                    top = targetRect.top - spotlightPaddingPx,
                    right = targetRect.right + spotlightPaddingPx,
                    bottom = targetRect.bottom + spotlightPaddingPx
                )
                
                val spotlightPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = expandedRect,
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )
                    )
                }
                
                // Create a path for the entire screen
                val overlayPath = Path().apply {
                    addRect(Rect(0f, 0f, size.width, size.height))
                }
                
                // Subtract the spotlight from the overlay using PathOperation
                val finalPath = Path().apply {
                    op(overlayPath, spotlightPath, PathOperation.Difference)
                }
                
                // Draw the dark overlay with cutout
                drawPath(
                    path = finalPath,
                    color = Color.Black.copy(alpha = 0.75f)
                )
                
                // Draw pulsing ring
                val ringSize = maxOf(expandedRect.width, expandedRect.height) + 8.dp.toPx()
                val ringOffset = (pulseScale - 1f) * ringSize / 2
                
                drawRoundRect(
                    color = step.accentColor.copy(alpha = pulseAlpha * 0.6f),
                    topLeft = Offset(
                        expandedRect.center.x - ringSize / 2 - ringOffset,
                        expandedRect.center.y - ringSize / 2 - ringOffset
                    ),
                    size = Size(ringSize + ringOffset * 2, ringSize + ringOffset * 2),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
                
                // Inner ring
                drawRoundRect(
                    color = step.accentColor,
                    topLeft = Offset(
                        expandedRect.center.x - ringSize / 2,
                        expandedRect.center.y - ringSize / 2
                    ),
                    size = Size(ringSize, ringSize),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
            
            // Tooltip card
            val tooltipHeightEstimate = 220.dp
            val safeAreaTop = 60.dp
            val safeAreaBottom = 120.dp
            
            val tooltipY = with(density) {
                val targetBottom = targetRect.bottom + spotlightPaddingPx
                val targetTop = targetRect.top - spotlightPaddingPx
                val tooltipHeightPx = tooltipHeightEstimate.toPx()
                val safeBottomPx = safeAreaBottom.toPx()
                val safeTopPx = safeAreaTop.toPx()
                
                when {
                    targetBottom + tooltipHeightPx + 20.dp.toPx() < screenHeight - safeBottomPx -> {
                        (targetBottom + 20.dp.toPx()).toDp()
                    }
                    targetTop - tooltipHeightPx - 20.dp.toPx() > safeTopPx -> {
                        (targetTop - tooltipHeightPx - 20.dp.toPx()).toDp()
                    }
                    else -> {
                        ((screenHeight - tooltipHeightPx) / 2).toDp()
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ((maxWidth - tooltipWidth) / 2).coerceAtLeast(16.dp))
                    .offset(y = tooltipY)
                    .graphicsLayer { alpha = tooltipAlpha }
            ) {
                TooltipCard(
                    step = step,
                    progress = progress,
                    isLastStep = isLastStep,
                    onNext = onNext,
                    onSkip = onSkip,
                    modifier = Modifier.width(tooltipWidth)
                )
            }
        } else {
            // No target rect - just show overlay with centered tooltip
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                TooltipCard(
                    step = step,
                    progress = progress,
                    isLastStep = isLastStep,
                    onNext = onNext,
                    onSkip = onSkip,
                    modifier = Modifier
                        .width(tooltipWidth)
                        .graphicsLayer { alpha = tooltipAlpha }
                )
            }
        }
    }
}

@Composable
private fun TooltipCard(
    step: TourStep,
    progress: Float,
    isLastStep: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(20.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFFFF6B35),
                trackColor = Color.Gray.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(step.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = step.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = "Passer",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = step.accentColor
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isLastStep) "Terminer" else "Suivant",
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!isLastStep) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
