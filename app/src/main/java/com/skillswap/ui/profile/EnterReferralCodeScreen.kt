package com.skillswap.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.ReferralViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterReferralCodeScreen(
    onBack: () -> Unit,
    onCodeRedeemed: () -> Unit,
    viewModel: ReferralViewModel = viewModel()
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrer un code") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = OrangePrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Utiliser un code parrainage",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Entrez le code de parrainage reçu pour bénéficier de récompenses",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = code,
                onValueChange = { 
                    code = it.uppercase()
                    errorMessage = null
                },
                label = { Text("Code de parrainage") },
                placeholder = { Text("XXXX-XXXX-XXXX") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage != null
            )
            
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (code.isBlank()) {
                        errorMessage = "Veuillez entrer un code"
                        return@Button
                    }
                    
                    isLoading = true
                    // Call viewModel to redeem code
                    // For now, just simulate success
                    onCodeRedeemed()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                enabled = !isLoading && code.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Utiliser le code",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = OrangePrimary.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Astuce",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = OrangePrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Demandez à vos amis de partager leur code de parrainage pour gagner des récompenses mutuelles!",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
