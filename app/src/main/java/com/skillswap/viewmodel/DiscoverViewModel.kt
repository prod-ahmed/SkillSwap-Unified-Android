package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.Annonce
import com.skillswap.security.SecureStorage
import com.skillswap.model.CreateAnnonceRequest
import com.skillswap.model.CreatePromoRequest
import com.skillswap.model.Promo
import com.skillswap.model.User
import com.skillswap.network.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DiscoverSegment {
    PROFILS, ANNONCES, PROMOS
}

class DiscoverViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = SecureStorage.getInstance(application)
    
    private val _segment = MutableStateFlow(DiscoverSegment.PROFILS)
    val segment: StateFlow<DiscoverSegment> = _segment.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()
    
    // Using simple index for swipeable cards
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _annonces = MutableStateFlow<List<Annonce>>(emptyList())
    val annonces: StateFlow<List<Annonce>> = _annonces.asStateFlow()

    private val _promos = MutableStateFlow<List<Promo>>(emptyList())
    val promos: StateFlow<List<Promo>> = _promos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val _cities = MutableStateFlow<List<String>>(emptyList())
    val cities: StateFlow<List<String>> = _cities.asStateFlow()
    private val _skills = MutableStateFlow<List<String>>(emptyList())
    val skills: StateFlow<List<String>> = _skills.asStateFlow()
    private val _cityFilter = MutableStateFlow<String?>(null)
    val cityFilter: StateFlow<String?> = _cityFilter.asStateFlow()
    private val _skillFilter = MutableStateFlow<String?>(null)
    val skillFilter: StateFlow<String?> = _skillFilter.asStateFlow()
    
    private val _categories = MutableStateFlow(listOf("Design", "Développement", "Marketing", "Langues", "Musique", "Autre"))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()
    
    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()
    
    // ============ PHASE 13: Advanced Filters ============
    
    private val _annonceFilterState = MutableStateFlow(com.skillswap.ui.discover.AnnonceFilterState())
    val annonceFilterState: StateFlow<com.skillswap.ui.discover.AnnonceFilterState> = _annonceFilterState.asStateFlow()
    
    private val _promoFilterState = MutableStateFlow(com.skillswap.ui.discover.PromoFilterState())
    val promoFilterState: StateFlow<com.skillswap.ui.discover.PromoFilterState> = _promoFilterState.asStateFlow()
    
    fun applyAnnonceFilters(filterState: com.skillswap.ui.discover.AnnonceFilterState) {
        _annonceFilterState.value = filterState
    }
    
    fun applyPromoFilters(filterState: com.skillswap.ui.discover.PromoFilterState) {
        _promoFilterState.value = filterState
    }

    fun setSegment(newSegment: DiscoverSegment) {
        _segment.value = newSegment
        loadForCurrentSegment()
    }

    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }

    fun loadForCurrentSegment() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Get real token
                val token = sharedPreferences.getString("auth_token", null)
                val authHeader = token?.let { "Bearer $it" }

                if (authHeader == null) {
                    _errorMessage.value = "Session expirée - veuillez vous reconnecter"
                    return@launch
                }
                runCatching {
                    val filters = NetworkService.api.getLocationFilters(authHeader)
                    _cities.value = filters["cities"] ?: emptyList()
                    _skills.value = filters["skills"] ?: emptyList()
                }

                when (_segment.value) {
                    DiscoverSegment.PROFILS -> {
                        val fetchedUsers = NetworkService.api.getRecommendations(
                            authHeader,
                            city = _cityFilter.value,
                            skill = _skillFilter.value,
                            limit = 50
                        ).map { withAbsoluteAvatar(it) }
                        _users.value = fetchedUsers
                        _currentIndex.value = 0
                        if (fetchedUsers.isEmpty()) _errorMessage.value = "Aucun profil trouvé"
                    }
                    DiscoverSegment.ANNONCES -> {
                         val fetched = NetworkService.api.getAllAnnonces(authHeader).map { withAbsoluteAnnonce(it) }
                         _annonces.value = fetched
                         if (fetched.isEmpty()) _errorMessage.value = "Aucune annonce disponible"
                    }
                    DiscoverSegment.PROMOS -> {
                         val fetched = NetworkService.api.getAllPromos(authHeader).map { withAbsolutePromo(it) }
                         _promos.value = fetched
                         if (fetched.isEmpty()) _errorMessage.value = "Aucune promotion active"
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Erreur de chargement: ${e.message}"
                _errorMessage.value = errorMsg
                android.util.Log.e("DiscoverViewModel", errorMsg, e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createAnnonce(title: String, description: String, city: String?) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val created = NetworkService.api.createAnnonce(
                    "Bearer $token",
                    CreateAnnonceRequest(title = title, description = description, city = city, price = null, imageUrl = null)
                )
                _annonces.value = _annonces.value + created
                _successMessage.value = "Annonce créée"
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPromo(title: String, description: String, discount: Int, validTo: String, validFrom: String? = null, code: String? = null) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val created = NetworkService.api.createPromo(
                    "Bearer $token",
                    CreatePromoRequest(
                        title = title,
                        description = description,
                        discount = discount,
                        validFrom = validFrom,
                        validUntil = validTo,
                        promoCode = code
                    )
                )
                _promos.value = _promos.value + created
                _successMessage.value = "Promo créée"
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun nextProfile() {
        if (_currentIndex.value < _users.value.size - 1) {
            _currentIndex.value += 1
        } else {
             // Reset or No more profiles
        }
    }
    
    fun swipeRight(user: User, onMatch: (User) -> Unit) {
        // Move to next profile (interest endpoint not available in backend)
        // In production, this would save the interest to the server
        nextProfile()
    }
    
    fun swipeLeft() {
        nextProfile()
    }

    fun startChatWithUser(userId: String, onThreadReady: (String) -> Unit) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            try {
                val thread = NetworkService.api.createThread(
                    "Bearer $token",
                    mapOf("participantId" to userId)
                )
                onThreadReady(thread.id)
            } catch (_: Exception) {
                onThreadReady(userId)
            }
        }
    }

    fun setCityFilter(city: String?) {
        _cityFilter.value = city
        loadForCurrentSegment()
    }

    fun setSkillFilter(skill: String?) {
        _skillFilter.value = skill
        loadForCurrentSegment()
    }
    
    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
        // For annonces, we filter locally or reload if API supports it. 
        // Currently API getAllAnnonces doesn't take filters, so we rely on local filtering in UI or here.
        // But to be consistent, let's just expose the filter and let UI handle it or filter the list here.
        // The UI currently does local filtering for text. Let's keep it consistent.
    }

    private fun withAbsoluteAnnonce(item: Annonce): Annonce {
        val url = item.imageUrl
        val absolute = if (!url.isNullOrBlank() && !(url.startsWith("http://") || url.startsWith("https://"))) {
            if (url.startsWith("/uploads/")) {
                com.skillswap.network.NetworkService.baseUrl + url
            } else {
                com.skillswap.network.NetworkService.baseUrl + "/uploads/annonces/" + url
            }
        } else url
        return item.copy(imageUrl = absolute)
    }

    private fun withAbsolutePromo(item: Promo): Promo {
        val url = item.imageUrl
        val absolute = if (!url.isNullOrBlank() && !(url.startsWith("http://") || url.startsWith("https://"))) {
            if (url.startsWith("/uploads/")) {
                com.skillswap.network.NetworkService.baseUrl + url
            } else {
                com.skillswap.network.NetworkService.baseUrl + "/uploads/promos/" + url
            }
        } else url
        return item.copy(imageUrl = absolute)
    }

    private fun withAbsoluteAvatar(user: User): User {
        val url = user.avatarUrl ?: user.image
        val absolute = if (!url.isNullOrBlank() && !(url.startsWith("http://") || url.startsWith("https://"))) {
            if (url.startsWith("/uploads/")) {
                com.skillswap.network.NetworkService.baseUrl + url
            } else {
                com.skillswap.network.NetworkService.baseUrl + "/uploads/users/" + url
            }
        } else url
        return user.copy(avatarUrl = absolute ?: user.avatarUrl, image = absolute ?: user.image)
    }
}
