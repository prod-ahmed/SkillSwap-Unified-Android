package com.skillswap.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillswap.ui.theme.OrangeGradientEnd
import com.skillswap.ui.theme.OrangeGradientStart

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    val titles = listOf(
        "Apprends",
        "Partage",
        "Connecte"
    )
    val subtitles = listOf(
        "Découvre de nouvelles compétences avec des experts près de chez toi.",
        "Enseigne tes talents et gagne des crédits.",
        "Rejoins une communauté dynamique."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(OrangeGradientStart.copy(alpha = 0.2f), Color.White))
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                titles[page],
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitles[page],
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (page < titles.lastIndex) page += 1 else onFinish()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (page < titles.lastIndex) "Suivant" else "Commencer")
            }
            if (page < titles.lastIndex) {
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text("Passer")
                }
            }
        }
    }
}
