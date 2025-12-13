package com.skillswap.util

import android.content.Context
import android.content.Intent

object ShareHelper {
    
    fun shareText(context: Context, text: String, title: String = "Partager") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
    
    fun shareReferralCode(context: Context, code: String) {
        val message = """
            Rejoignez SkillSwap avec mon code de parrainage: $code
            
            Téléchargez l'app et gagnez des récompenses!
            https://skillswap.tn/referral/$code
        """.trimIndent()
        
        shareText(context, message, "Partager le code")
    }
    
    fun shareSession(context: Context, sessionId: String, title: String) {
        val message = """
            Découvrez cette session sur SkillSwap: $title
            
            ${DeepLinkHandler.generateWebLink("session", sessionId)}
        """.trimIndent()
        
        shareText(context, message, "Partager la session")
    }
    
    fun shareLessonPlan(context: Context, sessionId: String) {
        val message = """
            Consultez ce plan de cours sur SkillSwap!
            
            ${DeepLinkHandler.generateWebLink("lesson-plan", sessionId)}
        """.trimIndent()
        
        shareText(context, message, "Partager le plan")
    }
}
