package com.skillswap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillswap.ui.theme.OrangePrimary

data class TimeSlot(
    val day: String,
    val time: String,
    val available: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityBottomSheet(
    userName: String,
    onDismiss: () -> Unit
) {
    val timeSlots = remember {
        listOf(
            TimeSlot("Lundi", "10:00 - 12:00", true),
            TimeSlot("Lundi", "14:00 - 16:00", false),
            TimeSlot("Mardi", "09:00 - 11:00", true),
            TimeSlot("Mardi", "15:00 - 17:00", true),
            TimeSlot("Mercredi", "10:00 - 12:00", false),
            TimeSlot("Jeudi", "14:00 - 16:00", true),
            TimeSlot("Vendredi", "09:00 - 11:00", true),
        )
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Disponibilités",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        userName,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Fermer")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(
                    color = Color(0xFF4CAF50),
                    label = "Disponible"
                )
                LegendItem(
                    color = Color(0xFFEEEEEE),
                    label = "Non disponible"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Time slots list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                val groupedSlots = timeSlots.groupBy { it.day }
                groupedSlots.forEach { (day, slots) ->
                    item {
                        Text(
                            day,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(slots) { slot ->
                        TimeSlotCard(slot)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text("Fermer", modifier = Modifier.padding(vertical = 8.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun TimeSlotCard(slot: TimeSlot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (slot.available) 
                Color(0xFF4CAF50).copy(alpha = 0.1f) 
            else 
                Color(0xFFEEEEEE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (slot.available) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = if (slot.available) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                slot.time,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (slot.available) Color.Black else Color.Gray
            )
        }
    }
}
