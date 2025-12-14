package com.skillswap.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillswap.ui.theme.OrangePrimary
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    label: String = "Sélectionner une date",
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMMM yyyy") }
    
    OutlinedTextField(
        value = selectedDate?.format(dateFormatter) ?: "",
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        enabled = enabled,
        leadingIcon = {
            Icon(Icons.Default.CalendarToday, contentDescription = null)
        },
        trailingIcon = {
            if (selectedDate != null) {
                IconButton(onClick = { /* Clear date logic if needed */ }) {
                    Icon(Icons.Default.Close, contentDescription = "Effacer")
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showDialog = true },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangePrimary,
            focusedLabelColor = OrangePrimary,
            disabledBorderColor = Color.LightGray,
            disabledTextColor = Color.Black
        )
    )
    
    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            onDateSelected(date)
                        }
                        showDialog = false
                    }
                ) {
                    Text("Valider", color = OrangePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Annuler")
                }
            },
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = OrangePrimary,
                todayDateBorderColor = OrangePrimary
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = OrangePrimary,
                    todayDateBorderColor = OrangePrimary
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    selectedTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    label: String = "Sélectionner l'heure",
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    
    OutlinedTextField(
        value = selectedTime?.format(timeFormatter) ?: "",
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        enabled = enabled,
        leadingIcon = {
            Icon(Icons.Default.AccessTime, contentDescription = null)
        },
        trailingIcon = {
            if (selectedTime != null) {
                IconButton(onClick = { /* Clear time logic if needed */ }) {
                    Icon(Icons.Default.Close, contentDescription = "Effacer")
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showDialog = true },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangePrimary,
            focusedLabelColor = OrangePrimary,
            disabledBorderColor = Color.LightGray,
            disabledTextColor = Color.Black
        )
    )
    
    if (showDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime?.hour ?: LocalTime.now().hour,
            initialMinute = selectedTime?.minute ?: LocalTime.now().minute,
            is24Hour = true
        )
        
        TimePickerDialog(
            onDismiss = { showDialog = false },
            onConfirm = {
                val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                onTimeSelected(time)
                showDialog = false
            }
        ) {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialSelectedContentColor = Color.White,
                    selectorColor = OrangePrimary,
                    periodSelectorSelectedContainerColor = OrangePrimary
                )
            )
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Valider", color = OrangePrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = { content() }
    )
}

@Composable
fun DurationPicker(
    selectedDuration: Int,
    onDurationSelected: (Int) -> Unit,
    label: String = "Durée",
    modifier: Modifier = Modifier,
    durationOptions: List<Int> = listOf(30, 60, 90, 120, 180)
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = "$selectedDuration min",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(Icons.Default.Timer, contentDescription = null)
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                focusedLabelColor = OrangePrimary
            )
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            durationOptions.forEach { duration ->
                DropdownMenuItem(
                    text = { Text("$duration minutes") },
                    onClick = {
                        onDurationSelected(duration)
                        expanded = false
                    },
                    leadingIcon = {
                        if (duration == selectedDuration) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = OrangePrimary
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun <T> DropdownPickerField(
    selectedValue: T?,
    onValueSelected: (T) -> Unit,
    options: List<T>,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    displayText: (T) -> String = { it.toString() },
    leadingIcon: @Composable (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedValue?.let { displayText(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            leadingIcon = leadingIcon,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                focusedLabelColor = OrangePrimary,
                disabledBorderColor = Color.LightGray,
                disabledTextColor = Color.Black
            )
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayText(option)) },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    },
                    leadingIcon = {
                        if (option == selectedValue) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = OrangePrimary
                            )
                        }
                    }
                )
            }
        }
    }
}
