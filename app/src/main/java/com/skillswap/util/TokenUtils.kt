package com.skillswap.util

import android.util.Base64
import org.json.JSONObject

object TokenUtils {
    fun isTokenExpired(token: String): Boolean {
        try {
            val parts = token.split(".")
            if (parts.size != 3) return true
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)
            val exp = json.optLong("exp")
            if (exp == 0L) return false
            return System.currentTimeMillis() / 1000 >= exp
        } catch (e: Exception) {
            return true
        }
    }
}
