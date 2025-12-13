package com.skillswap.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.viewmodel.ReferralViewModel

import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    viewModel: ReferralViewModel = viewModel(),
    onBack: (() -> Unit)? = null,
    onNavigateToCodeGeneration: () -> Unit = {}
) {
    LaunchedEffect(Unit) { viewModel.loadReferrals() }
    val state by viewModel.state.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()
    val loading by viewModel.loading.collectAsState()
    var code by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parrainage") },
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Filled.Close, contentDescription = "Retour")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Parrainage", fontWeight = FontWeight.Bold)
            Text("Invitez un ami ou entrez un code pour débloquer vos récompenses.")
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Code") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.redeem(code) },
                enabled = code.isNotBlank() && !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Appliquer le code")
            }

            Button(
                onClick = onNavigateToCodeGeneration,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
            ) {
                Icon(Icons.Default.CardGiftcard, null)
                Spacer(Modifier.width(8.dp))
                Text("Mon Code & Partage")
            }

            if (loading) {
                CircularProgressIndicator()
            }
            if (message != null) {
                Text(
                    message!!,
                    color = Color(0xFF0F9D58),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE6F4EA), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
            }
            if (error != null) {
                Text(
                    error!!,
                    color = Color(0xFFB3261E),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEDEC), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            state?.let { data ->
                if (data.inviteeReferral == null && data.inviterReferrals.isEmpty()) {
                    EmptyReferralCard()
                } else {
                    ReferralStatusCard(data.inviteeReferral, data.inviterReferrals.size)
                }
                if (data.rewards.isNotEmpty()) {
                    Text("Récompenses", fontWeight = FontWeight.Bold)
                    data.rewards.forEach { reward ->
                        RewardRow(
                            title = reward.rewardType,
                            subtitle = "Statut: ${reward.status}",
                            amount = reward.amount ?: 0
                        )
                    }
                }
                if (data.inviterReferrals.isNotEmpty()) {
                    Text("Parrainages envoyés", fontWeight = FontWeight.Bold)
                    data.inviterReferrals.forEach { item ->
                        ReferralRow(
                            title = item.inviteeEmail ?: "Invité",
                            subtitle = item.status ?: "en attente",
                            date = item.createdAt ?: ""
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyReferralCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F7))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Aucun parrainage pour l'instant.", fontWeight = FontWeight.Medium)
            Text("Partagez votre code ou entrez celui d'un ami pour débloquer des récompenses.", color = Color.Gray)
        }
    }
}

@Composable
private fun ReferralStatusCard(invitee: com.skillswap.model.ReferralItem?, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F4FF))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Statut", fontWeight = FontWeight.Medium)
            invitee?.let {
                Text("Vous avez été parrainé · ${it.status ?: "en attente"}", color = Color(0xFF6A1B9A))
            }
            Text("$count invitations envoyées", color = Color(0xFF6A1B9A))
        }
    }
}

@Composable
private fun RewardRow(title: String, subtitle: String, amount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.Gray)
        }
        Text("+$amount", fontWeight = FontWeight.Bold, color = Color(0xFF0F9D58))
    }
}

@Composable
private fun ReferralRow(title: String, subtitle: String, date: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Color.Gray)
        if (date.isNotBlank()) Text(date, color = Color.Gray, fontSize = 12.sp)
    }
}
