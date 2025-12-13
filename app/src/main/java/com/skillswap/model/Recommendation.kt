package com.skillswap.model

import com.google.gson.annotations.SerializedName

data class RecommendationResponse(
    val message: String?,
    val data: List<Recommendation>
)

data class Recommendation(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("mentorName")
    val mentorName: String,
    
    @SerializedName("mentorImage")
    val mentorImage: String? = null,
    
    @SerializedName("age")
    val age: Int = 0,
    
    @SerializedName("skills")
    val skills: List<String> = emptyList(),
    
    @SerializedName("description")
    val description: String = "",
    
    @SerializedName("availability")
    val availability: String = "Disponible",
    
    @SerializedName("distance")
    val distance: String = "",
    
    @SerializedName("rating")
    val rating: Double = 0.0,
    
    @SerializedName("lastActive")
    val lastActive: String = "Récemment",
    
    @SerializedName("sessionsCount")
    val sessionsCount: Int = 0
) {
    val initials: String
        get() {
            val components = mentorName.split(" ")
            return components
                .take(2)
                .mapNotNull { it.firstOrNull()?.toString() }
                .joinToString("")
                .uppercase()
                .ifEmpty { mentorName.take(1).uppercase() }
        }
}
