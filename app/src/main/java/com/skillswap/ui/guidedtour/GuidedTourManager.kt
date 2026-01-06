package com.skillswap.ui.guidedtour

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Manages the guided tour state and persistence
 */
class GuidedTourManager private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("guided_tour", Context.MODE_PRIVATE)
    
    var hasCompletedTour: Boolean
        get() = prefs.getBoolean("hasCompletedGuidedTour", false)
        private set(value) {
            prefs.edit().putBoolean("hasCompletedGuidedTour", value).apply()
        }
    
    var isShowingTour by mutableStateOf(false)
        private set
    
    var currentStepIndex by mutableStateOf(0)
        private set
    
    val currentStep: TourStep?
        get() = if (currentStepIndex < TourStep.allSteps.size) {
            TourStep.allSteps[currentStepIndex]
        } else null
    
    val isLastStep: Boolean
        get() = currentStepIndex == TourStep.allSteps.size - 1
    
    val progress: Float
        get() = (currentStepIndex + 1).toFloat() / TourStep.allSteps.size.toFloat()
    
    val shouldShowTour: Boolean
        get() = !hasCompletedTour
    
    fun startTour() {
        if (!shouldShowTour) return
        currentStepIndex = 0
        isShowingTour = true
    }
    
    fun nextStep() {
        if (currentStepIndex < TourStep.allSteps.size - 1) {
            currentStepIndex++
        } else {
            completeTour()
        }
    }
    
    fun previousStep() {
        if (currentStepIndex > 0) {
            currentStepIndex--
        }
    }
    
    fun skipTour() {
        completeTour()
    }
    
    fun completeTour() {
        isShowingTour = false
        hasCompletedTour = true
    }
    
    /**
     * Reset tour for testing purposes
     */
    fun resetTour() {
        hasCompletedTour = false
        currentStepIndex = 0
        isShowingTour = false
    }
    
    companion object {
        @Volatile
        private var instance: GuidedTourManager? = null
        
        fun getInstance(context: Context): GuidedTourManager {
            return instance ?: synchronized(this) {
                instance ?: GuidedTourManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
