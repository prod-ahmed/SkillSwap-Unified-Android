package com.skillswap.ui.profile

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillswap.ui.theme.OrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(true) }
    
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Déconnexion") },
            text = { Text("Êtes-vous sûr de vouloir vous déconnecter ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Déconnexion", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
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
        ) {
            // Account Section
            SettingsSection(title = "Compte") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Modifier le profil",
                    onClick = onEditProfile
                )
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Changer le mot de passe",
                    onClick = { /* Navigate to change password */ }
                )
                SettingsItem(
                    icon = Icons.Default.Email,
                    title = "Email et notifications",
                    onClick = { /* Navigate to email settings */ }
                )
            }
            
            // Preferences Section
            SettingsSection(title = "Préférences") {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications push",
                    checked = notifications,
                    onCheckedChange = { notifications = it }
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Star,
                    title = "Mode sombre",
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Langue",
                    onClick = { /* Show language picker */ }
                )
            }
            
            // Privacy Section
            SettingsSection(title = "Confidentialité") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Conditions d'utilisation",
                    onClick = { /* Open terms */ }
                )
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Politique de confidentialité",
                    onClick = { /* Open privacy policy */ }
                )
            }
            
            // Danger Zone
            SettingsSection(title = "Compte", textColor = Color.Red) {
                SettingsItem(
                    icon = Icons.Default.ExitToApp,
                    title = "Déconnexion",
                    textColor = Color.Red,
                    onClick = { showLogoutDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Supprimer le compte",
                    textColor = Color.Red,
                    onClick = { /* Show delete confirmation */ }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Version 1.0.0",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    textColor: Color = Color.Gray,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    textColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            color = textColor
        )
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
    Divider()
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OrangePrimary,
                checkedTrackColor = OrangePrimary.copy(alpha = 0.5f)
            )
        )
    }
    Divider()
}
