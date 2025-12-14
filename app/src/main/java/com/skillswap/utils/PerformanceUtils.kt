package com.skillswap.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Performance optimization utilities for SkillSwap
 * Helps maintain 60fps and reduce jank
 */
object PerformanceUtils {
    
    /**
     * Stable key for LazyColumn items
     * Prevents unnecessary recomposition
     */
    @Stable
    fun stableKey(id: String, prefix: String = ""): String {
        return if (prefix.isNotEmpty()) "$prefix-$id" else id
    }
    
    /**
     * Convert Dp to Px with memoization
     */
    @Composable
    fun Dp.toPxMemoized(): Float {
        val density = LocalDensity.current
        return remember(this, density) {
            with(density) { this@toPxMemoized.toPx() }
        }
    }
    
    /**
     * Debounce function for search/filter inputs
     * Prevents excessive API calls
     */
    class Debouncer(private val delayMillis: Long = 300) {
        private var lastActionTime = 0L
        private var pendingAction: (() -> Unit)? = null
        
        fun debounce(action: () -> Unit) {
            pendingAction = action
            val currentTime = System.currentTimeMillis()
            
            if (currentTime - lastActionTime >= delayMillis) {
                action()
                lastActionTime = currentTime
                pendingAction = null
            }
        }
        
        fun executePending() {
            pendingAction?.invoke()
            pendingAction = null
        }
    }
    
    /**
     * Image loading optimization recommendations
     */
    object ImageOptimization {
        const val THUMBNAIL_SIZE = 100 // dp
        const val PROFILE_SIZE = 200 // dp
        const val FULL_SIZE = 400 // dp
        
        fun getOptimalSize(usage: ImageUsage): Int {
            return when (usage) {
                ImageUsage.THUMBNAIL -> THUMBNAIL_SIZE
                ImageUsage.PROFILE -> PROFILE_SIZE
                ImageUsage.FULL -> FULL_SIZE
            }
        }
    }
    
    enum class ImageUsage {
        THUMBNAIL,  // List items, small avatars
        PROFILE,    // Profile pictures
        FULL        // Full-screen images
    }
    
    /**
     * List pagination helper
     */
    data class PaginationState(
        val page: Int = 0,
        val pageSize: Int = 20,
        val isLoading: Boolean = false,
        val hasMore: Boolean = true
    ) {
        fun nextPage() = copy(page = page + 1, isLoading = true)
        fun loaded(itemCount: Int) = copy(
            isLoading = false,
            hasMore = itemCount == pageSize
        )
    }
    
    /**
     * Memory cache size recommendations
     */
    fun getRecommendedCacheSize(): Int {
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024 // MB
        return (maxMemory / 8).toInt() // Use 1/8 of available memory
    }
}
