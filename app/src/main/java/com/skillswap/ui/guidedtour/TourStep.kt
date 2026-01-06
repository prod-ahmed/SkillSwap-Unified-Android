package com.skillswap.ui.guidedtour

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defines a single step in the guided tour
 */
data class TourStep(
    val id: Int,
    val targetId: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    companion object {
        val allSteps: List<TourStep> = listOf(
            TourStep(
                id = 0,
                targetId = "tab_discover",
                title = "Découvrir",
                description = "Découvrez des profils, annonces et promotions de la communauté",
                icon = Icons.Default.Home,
                accentColor = Color(0xFFFF9800) // Orange
            ),
            TourStep(
                id = 1,
                targetId = "tab_messages",
                title = "Messages",
                description = "Échangez avec les autres membres de la communauté",
                icon = Icons.Default.Email,
                accentColor = Color(0xFFFA5940) // Red-Orange
            ),
            TourStep(
                id = 2,
                targetId = "tab_sessions",
                title = "Sessions",
                description = "Planifiez et gérez vos sessions d'apprentissage",
                icon = Icons.Default.DateRange,
                accentColor = Color(0xFF12947D) // Teal
            ),
            TourStep(
                id = 3,
                targetId = "tab_progress",
                title = "Progrès",
                description = "Suivez votre progression et vos objectifs hebdomadaires",
                icon = Icons.Default.Info,
                accentColor = Color(0xFFF28F24) // Orange
            ),
            TourStep(
                id = 4,
                targetId = "tab_map",
                title = "Carte",
                description = "Explorez les membres près de chez vous sur la carte",
                icon = Icons.Default.LocationOn,
                accentColor = Color(0xFF5C52BF) // Purple
            ),
            TourStep(
                id = 5,
                targetId = "tab_profile",
                title = "Profil",
                description = "Gérez votre profil, paramètres et préférences",
                icon = Icons.Default.Person,
                accentColor = Color(0xFF4ECDC4) // Turquoise
            )
        )
    }
}
