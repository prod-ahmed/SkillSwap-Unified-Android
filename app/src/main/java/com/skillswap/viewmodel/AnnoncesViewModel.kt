package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.Annonce
import com.skillswap.security.SecureStorage
import com.skillswap.model.CreateAnnonceRequest
import com.skillswap.model.MediaPayload
import com.skillswap.model.UpdateAnnonceRequest
import com.skillswap.network.NetworkService
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnnoncesViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = SecureStorage.getInstance(application)

    private val _annonces = MutableStateFlow<List<Annonce>>(emptyList())
    val annonces: StateFlow<List<Annonce>> = _annonces.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()
    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()
    private val _uploadProgress = MutableStateFlow<Int>(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress.asStateFlow()

    fun clearMessages() {
        _error.value = null
        _success.value = null
    }

    fun loadAnnonces() {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
                _isLoading.value = true
                try {
                val result = NetworkService.api.getMyAnnonces("Bearer $token")
                _annonces.value = result.map { withAbsoluteImage(it) }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createAnnonce(
        title: String,
        description: String,
        city: String?,
        category: String? = null,
        media: MediaPayload? = null
    ) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                android.util.Log.d("AnnoncesViewModel", "Creating annonce: title=$title, media=${media != null}")
                if (media != null && !isImageSafe(token, media)) {
                    android.util.Log.w("AnnoncesViewModel", "Image moderation failed, aborting")
                    _isLoading.value = false
                    return@launch
                }
                val created = NetworkService.api.createAnnonce(
                    "Bearer $token",
                    CreateAnnonceRequest(
                        title = title,
                        description = description,
                        city = city,
                        category = category,
                        imageUrl = null
                    )
                )
                android.util.Log.d("AnnoncesViewModel", "Annonce created with id=${created.id}")
                val finalAnnonce = if (media != null) {
                    android.util.Log.d("AnnoncesViewModel", "Uploading image...")
                    uploadAnnonceImage(token, created.id, media) ?: created
                } else created
                android.util.Log.d("AnnoncesViewModel", "Final annonce imageUrl: ${finalAnnonce.imageUrl}")
                _annonces.value = _annonces.value + withAbsoluteImage(finalAnnonce)
                _success.value = if (media != null) "Annonce publiée avec image" else "Annonce créée"
            } catch (e: Exception) {
                android.util.Log.e("AnnoncesViewModel", "Error creating annonce: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateAnnonce(
        id: String,
        title: String,
        description: String,
        city: String?,
        category: String? = null,
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
                val updated = NetworkService.api.updateAnnonce(
                    "Bearer $token",
                    id,
                    UpdateAnnonceRequest(
                        title = title,
                        description = description,
                        city = city,
                        category = category,
                        imageUrl = null
                    )
                )
                val finalAnnonce = if (media != null) {
                    uploadAnnonceImage(token, id, media) ?: updated
                } else updated
                _annonces.value = _annonces.value.map { if (it.id == id) withAbsoluteImage(finalAnnonce) else withAbsoluteImage(it) }
                _success.value = "Annonce mise à jour"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun isImageSafe(token: String, media: MediaPayload): Boolean {
        return try {
            val base64 = Base64.encodeToString(media.bytes, Base64.NO_WRAP)
            val result = NetworkService.api.checkImage(
                "Bearer $token",
                mapOf("imageBase64" to base64)
            )
            if (!result.safe) {
                val reason = result.categories?.joinToString(", ")
                    ?: result.reasons?.joinToString(", ")
                    ?: "Image non conforme aux règles de la communauté"
                _error.value = "⚠️ $reason"
            }
            result.safe
        } catch (e: Exception) {
            // On error, allow the image (fail open) but log
            android.util.Log.e("AnnoncesViewModel", "Moderation check failed: ${e.message}")
            true
        }
    }

    private suspend fun uploadAnnonceImage(token: String, id: String, media: MediaPayload): Annonce? {
        android.util.Log.d("AnnoncesViewModel", "Uploading image for annonce $id (${media.bytes.size} bytes)")
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
            val result = NetworkService.api.uploadAnnonceImage("Bearer $token", id, part)
            android.util.Log.d("AnnoncesViewModel", "Image uploaded successfully. New imageUrl: ${result.imageUrl}")
            result
        } catch (e: Exception) {
            android.util.Log.e("AnnoncesViewModel", "Image upload failed: ${e.message}", e)
            _error.value = "Image non envoyée: ${e.message}"
            null
        } finally {
            _uploading.value = false
            _uploadProgress.value = 0
        }
    }

    fun deleteAnnonce(id: String) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            try {
                NetworkService.api.deleteAnnonce("Bearer $token", id)
                _annonces.value = _annonces.value.filterNot { it.id == id }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    suspend fun getAnnonceById(id: String): Annonce? {
        val token = sharedPreferences.getString("auth_token", null) ?: return null
        return try {
            val annonce = NetworkService.api.getAnnonce("Bearer $token", id)
            withAbsoluteImage(annonce)
        } catch (e: Exception) {
            null
        }
    }

    private fun withAbsoluteImage(annonce: Annonce): Annonce {
        val url = annonce.imageUrl
        val absolute = if (!url.isNullOrBlank() && !(url.startsWith("http://") || url.startsWith("https://"))) {
            com.skillswap.network.NetworkService.baseUrl + "/uploads/annonces/" + url
        } else url
        android.util.Log.d("AnnoncesViewModel", "withAbsoluteImage: original=$url, absolute=$absolute")
        return annonce.copy(imageUrl = absolute)
    }
}
