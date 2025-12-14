package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.BuildConfig
import com.skillswap.security.SecureStorage
import com.skillswap.model.CreatePromoRequest
import com.skillswap.model.MediaPayload
import com.skillswap.model.Promo
import com.skillswap.model.UpdatePromoRequest
import com.skillswap.network.NetworkService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PromosViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = SecureStorage.getInstance(application)

    private val _promos = MutableStateFlow<List<Promo>>(emptyList())
    val promos: StateFlow<List<Promo>> = _promos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()
    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()
    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress.asStateFlow()
    
    private val _generatingImage = MutableStateFlow(false)
    val generatingImage: StateFlow<Boolean> = _generatingImage.asStateFlow()
    private val _generatedImageUrl = MutableStateFlow<String?>(null)
    val generatedImageUrl: StateFlow<String?> = _generatedImageUrl.asStateFlow()

    fun clearMessages() {
        _error.value = null
        _success.value = null
    }
    
    fun clearGeneratedImage() {
        _generatedImageUrl.value = null
    }
    
    suspend fun generatePromoImage(prompt: String): String? {
        val token = sharedPreferences.getString("auth_token", null) ?: return null
        _generatingImage.value = true
        _error.value = null
        
        return try {
            val response = NetworkService.api.generateImage(
                "Bearer $token",
                mapOf("prompt" to prompt)
            )
            val imageUrl = response["url"]
            _generatedImageUrl.value = imageUrl
            _success.value = "Image générée avec succès!"
            imageUrl
        } catch (e: Exception) {
            _error.value = "Erreur lors de la génération: ${e.message}"
            null
        } finally {
            _generatingImage.value = false
        }
    }

    fun loadPromos() {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = NetworkService.api.getMyPromos("Bearer $token")
                _promos.value = result.map { withAbsoluteImage(it) }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPromo(
        title: String,
        description: String,
        discount: Int,
        validTo: String,
        validFrom: String? = null,
        promoCode: String? = null,
        imageUrl: String? = null,
        media: MediaPayload? = null
    ) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                if (media != null && !isImageSafe(token, media)) {
                    _isLoading.value = false
                    return@launch
                }
                val created = NetworkService.api.createPromo(
                    "Bearer $token",
                    CreatePromoRequest(
                        title = title,
                        description = description,
                        discount = discount,
                        validFrom = validFrom,
                        validUntil = validTo,
                        promoCode = promoCode,
                        imageUrl = imageUrl
                    )
                )
                val finalPromo = if (media != null) {
                    uploadPromoImage(token, created.id, media) ?: created
                } else created
                _promos.value = _promos.value + withAbsoluteImage(finalPromo)
                _success.value = if (media != null) "Promo publiée avec image" else "Promo créée"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
                _uploading.value = false
            }
        }
    }

    fun updatePromo(
        id: String,
        title: String,
        description: String,
        discount: Int,
        validTo: String,
        validFrom: String? = null,
        promoCode: String? = null,
        imageUrl: String? = null,
        media: MediaPayload? = null
    ) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                if (media != null && !isImageSafe(token, media)) {
                    _isLoading.value = false
                    return@launch
                }
                val updated = NetworkService.api.updatePromo(
                    "Bearer $token",
                    id,
                    UpdatePromoRequest(
                        title = title,
                        description = description,
                        discount = discount,
                        validFrom = validFrom,
                        validUntil = validTo,
                        promoCode = promoCode,
                        imageUrl = imageUrl
                    )
                )
                val finalPromo = if (media != null) {
                    uploadPromoImage(token, id, media) ?: updated
                } else updated
                _promos.value = _promos.value.map { if (it.id == id) withAbsoluteImage(finalPromo) else withAbsoluteImage(it) }
                _success.value = "Promo mise à jour"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
                _uploading.value = false
            }
        }
    }

    fun deletePromo(id: String) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            try {
                NetworkService.api.deletePromo("Bearer $token", id)
                _promos.value = _promos.value.filterNot { it.id == id }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    suspend fun getPromoById(id: String): Promo? {
        val token = sharedPreferences.getString("auth_token", null) ?: return null
        return try {
            val promo = NetworkService.api.getPromo("Bearer $token", id)
            withAbsoluteImage(promo)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun isImageSafe(token: String, media: MediaPayload): Boolean {
        return try {
            val base64 = android.util.Base64.encodeToString(media.bytes, android.util.Base64.NO_WRAP)
            val result = NetworkService.api.checkImage(
                "Bearer $token",
                mapOf("imageBase64" to base64)
            )
            if (!result.safe) {
                val reason = result.categories?.joinToString(", ")
                    ?: result.reasons?.joinToString(", ")
                    ?: "Image refusée"
                _error.value = reason
            }
            result.safe
        } catch (e: Exception) {
            _error.value = "Vérification image impossible: ${e.message}"
            false
        }
    }

    private suspend fun uploadPromoImage(token: String, id: String, media: MediaPayload): Promo? {
        val mediaType = runCatching { media.mimeType.toMediaType() }.getOrElse { "image/jpeg".toMediaType() }
        val countingBody = com.skillswap.network.CountingRequestBody(media.bytes, mediaType) { progress ->
            _uploadProgress.value = progress
        }
        val part = MultipartBody.Part.createFormData(
            "image",
            media.filename,
            countingBody
        )
        return try {
            _uploading.value = true
            NetworkService.api.uploadPromoImage("Bearer $token", id, part)
        } catch (e: Exception) {
            _error.value = "Image non envoyée: ${e.message}"
            null
        } finally {
            _uploading.value = false
            _uploadProgress.value = 0
        }
    }

    private fun withAbsoluteImage(promo: Promo): Promo {
        val url = promo.imageUrl
        val absolute = if (!url.isNullOrBlank() && !(url.startsWith("http://") || url.startsWith("https://"))) {
            BuildConfig.API_BASE_URL.trimEnd('/') + "/uploads/promos/" + url
        } else url
        return promo.copy(imageUrl = absolute)
    }
}
