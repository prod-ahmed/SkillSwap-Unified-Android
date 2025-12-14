package com.skillswap.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.skillswap.ui.theme.OrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("SkillSwapPrefs", android.content.Context.MODE_PRIVATE)
    val themeManager = remember { com.skillswap.util.ThemeManager.getInstance(context) }
    val localizationManager = remember { com.skillswap.util.LocalizationManager.getInstance(context) }
    
    val currentTheme by themeManager.themePreference
    val currentLanguage by localizationManager.currentLanguage
    
    var notificationsEnabled by remember { 
        mutableStateOf(sharedPrefs.getBoolean("notifications_enabled", true)) 
    }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Déconnexion") },
            text = { Text("Êtes-vous sûr de vouloir vous déconnecter ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        // Clear session
                        sharedPrefs.edit().clear().apply()
                        // Navigate to Auth
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
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
    
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("À propos de SkillSwap") },
            text = { 
                Column {
                    Text("Version 1.0.0")
                    Spacer(Modifier.height(8.dp))
                    Text("SkillSwap est une plateforme d'échange de compétences qui connecte les mentors et les apprenants.")
                    Spacer(Modifier.height(8.dp))
                    Text("© 2025 SkillSwap Inc.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangePrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF2F2F7))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSection("Général") {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Langue",
                    subtitle = currentLanguage.displayName,
                    onClick = { showLanguagePicker = true }
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            SettingsSection("Apparence") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Thème",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Choisissez un thème clair, sombre ou suivez le système",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOption(
                            title = "Système",
                            isSelected = currentTheme == com.skillswap.util.ThemePreference.SYSTEM,
                            onClick = { themeManager.setTheme(com.skillswap.util.ThemePreference.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOption(
                            title = "Clair",
                            isSelected = currentTheme == com.skillswap.util.ThemePreference.LIGHT,
                            onClick = { themeManager.setTheme(com.skillswap.util.ThemePreference.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOption(
                            title = "Sombre",
                            isSelected = currentTheme == com.skillswap.util.ThemePreference.DARK,
                            onClick = { themeManager.setTheme(com.skillswap.util.ThemePreference.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            SettingsSection("Notifications") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Activer les notifications",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Recevoir des alertes",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            notificationsEnabled = enabled
                            sharedPrefs.edit()
                                .putBoolean("notifications_enabled", enabled)
                                .apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OrangePrimary
                        )
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            SettingsSection("Confidentialité & Sécurité") {
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Confidentialité",
                    subtitle = "Gérer vos données",
                    onClick = { navController.navigate("privacy") }
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            SettingsSection("Compte") {
                SettingsItem(
                    icon = Icons.Default.ExitToApp,
                    title = "Déconnexion",
                    subtitle = "Se déconnecter du compte",
                    onClick = { showLogoutDialog = true }
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            SettingsSection("À propos") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version de l'application",
                    subtitle = "1.0.0",
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }
    
    if (showLanguagePicker) {
        LanguagePickerDialog(
            currentLanguage = currentLanguage,
            onDismiss = { showLanguagePicker = false },
            onLanguageSelected = { lang ->
                localizationManager.setLanguage(lang)
                showLanguagePicker = false
            }
        )
    }
}

@Composable
fun ThemeOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) OrangePrimary.copy(alpha = 0.15f) else Color(0xFFF2F2F7),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, OrangePrimary) else null,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) OrangePrimary else Color.Gray
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White
        ) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
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
            tint = OrangePrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun LanguagePickerDialog(
    currentLanguage: com.skillswap.util.AppLanguage,
    onDismiss: () -> Unit,
    onLanguageSelected: (com.skillswap.util.AppLanguage) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir la langue") },
        text = {
            Column {
                com.skillswap.util.AppLanguage.values().forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                onLanguageSelected(lang)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(lang.flag, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.width(12.dp))
                            Text(lang.displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (lang == currentLanguage) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = OrangePrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = OrangePrimary)
            }
        }
    )
}
