package com.candycrush.game.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Styled button used throughout the game UI.
 *
 * Provides a consistent look for all buttons (play, retry, next level, etc.)
 *
 * @param text The button text
 * @param onClick Called when the button is clicked
 * @param color The button background color
 * @param modifier Optional modifier
 */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    color: Color = Color(0xFF6650a4),
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}
