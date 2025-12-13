package com.skillswap.util

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController

object DeepLinkHandler {
    
    fun handleDeepLink(intent: Intent?, navController: NavController): Boolean {
        val uri = intent?.data ?: return false
        
        when (uri.scheme) {
            "skillswap" -> return handleSkillSwapDeepLink(uri, navController)
            "https", "http" -> return handleWebDeepLink(uri, navController)
        }
        return false
    }
    
    private fun handleSkillSwapDeepLink(uri: Uri, navController: NavController): Boolean {
        val path = uri.pathSegments
        if (path.isEmpty()) return false
        
        return when (path[0]) {
            "session" -> if (path.size >= 2) {
                navController.navigate("session_detail/${path[1]}")
                true
            } else false
            "chat" -> if (path.size >= 2) {
                navController.navigate("chat/${path[1]}")
                true
            } else false
            "lesson-plan" -> if (path.size >= 2) {
                navController.navigate("lesson_plan/${path[1]}")
                true
            } else false
            else -> false
        }
    }
    
    private fun handleWebDeepLink(uri: Uri, navController: NavController): Boolean {
        val path = uri.path ?: return false
        val segments = path.split("/").filter { it.isNotEmpty() }
        if (segments.isEmpty()) return false
        
        return when (segments[0]) {
            "session" -> if (segments.size >= 2) {
                navController.navigate("session_detail/${segments[1]}")
                true
            } else false
            else -> false
        }
    }
    
    fun generateDeepLink(type: String, id: String) = "skillswap://$type/$id"
    fun generateWebLink(type: String, id: String) = "https://skillswap.tn/$type/$id"
}
