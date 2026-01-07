package com.skillswap.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class EmojiCategory(
    val name: String,
    val icon: String,
    val emojis: List<String>
)

val emojiCategories = listOf(
    EmojiCategory(
        name = "Récents",
        icon = "🕐",
        emojis = listOf("😀", "❤️", "👍", "😂", "🎉", "🔥", "💯", "✨")
    ),
    EmojiCategory(
        name = "Smileys",
        icon = "😀",
        emojis = listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
            "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
            "😘", "😗", "😚", "😙", "🥲", "😋", "😛", "😜",
            "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐",
            "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬",
            "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒",
            "🤕", "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵",
            "🤯", "🤠", "🥳", "🥸", "😎", "🤓", "🧐", "😕"
        )
    ),
    EmojiCategory(
        name = "Gestes",
        icon = "👋",
        emojis = listOf(
            "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏",
            "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆",
            "🖕", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛",
            "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️",
            "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻", "👃"
        )
    ),
    EmojiCategory(
        name = "Coeurs",
        icon = "❤️",
        emojis = listOf(
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
            "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖",
            "💘", "💝", "💟", "♥️", "💋", "💌", "💍", "💎"
        )
    ),
    EmojiCategory(
        name = "Objets",
        icon = "🎉",
        emojis = listOf(
            "🎉", "🎊", "🎈", "🎁", "🏆", "🥇", "🥈", "🥉",
            "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🎮", "🎲",
            "🎵", "🎶", "🎤", "🎧", "📱", "💻", "⌚", "📷",
            "🔥", "💯", "✨", "⭐", "🌟", "💫", "🌈", "☀️"
        )
    ),
    EmojiCategory(
        name = "Nature",
        icon = "🌸",
        emojis = listOf(
            "🌸", "🌺", "🌻", "🌹", "🌷", "🌱", "🌲", "🌳",
            "🍀", "🍁", "🍂", "🍃", "🐶", "🐱", "🐭", "🐹",
            "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮",
            "🐷", "🐸", "🐵", "🙈", "🙉", "🙊", "🐔", "🐧"
        )
    ),
    EmojiCategory(
        name = "Nourriture",
        icon = "🍕",
        emojis = listOf(
            "🍕", "🍔", "🍟", "🌭", "🍿", "🧂", "🥚", "🍳",
            "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🦴", "🌮",
            "🌯", "🥙", "🧆", "🥗", "🍝", "🍜", "🍲", "🍛",
            "🍣", "🍱", "🥟", "🍤", "🍙", "🍚", "🍘", "🍥",
            "🍦", "🍧", "🍨", "🍩", "🍪", "🎂", "🍰", "🧁",
            "☕", "🍵", "🧃", "🥤", "🍶", "🍷", "🍸", "🍹"
        )
    )
)

@Composable
fun EmojiPicker(
    visible: Boolean,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableIntStateOf(0) }
    
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column {
                // Category tabs
                TabRow(
                    selectedTabIndex = selectedCategory,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(48.dp)
                ) {
                    emojiCategories.forEachIndexed { index, category ->
                        Tab(
                            selected = selectedCategory == index,
                            onClick = { selectedCategory = index },
                            text = { Text(category.icon, fontSize = 18.sp) }
                        )
                    }
                }
                
                // Emoji grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(emojiCategories[selectedCategory].emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onEmojiSelected(emoji) }
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
