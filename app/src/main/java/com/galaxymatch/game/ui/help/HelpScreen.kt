package com.galaxymatch.game.ui.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxymatch.game.ui.components.GalaxyBackground
import com.galaxymatch.game.ui.components.bounceClick
import com.galaxymatch.game.ui.theme.GameBackground

/**
 * Help screen that explains all game mechanics to the player.
 *
 * Covers:
 * - How to play (basic swipe mechanics)
 * - Obstacles (Ice, Reinforced Ice, Locked, Stone, Bombs)
 * - Special gems (Striped, Wrapped, Color Bomb)
 * - Power-ups (Hammer, Color Bomb, Extra Moves)
 * - Game modes (Normal, Daily Challenge, Timed Challenge)
 *
 * @param onBack Called when the player taps the back button
 */
@Composable
fun HelpScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
    ) {
        GalaxyBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // === Header with back button ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .bounceClick(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u2190", // Left arrow
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }

                Text(
                    text = "\u2753 How to Play",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // === Scrollable content ===
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Basics ---
                HelpSection(
                    title = "\uD83D\uDC8E Basics",
                    items = listOf(
                        HelpItem(
                            emoji = "\u2194\uFE0F",
                            title = "Swipe to Match",
                            description = "Swipe gems to swap them with a neighbor. Match 3 or more of the same color in a row or column to clear them and score points."
                        ),
                        HelpItem(
                            emoji = "\u2B50",
                            title = "Stars",
                            description = "Each level has 3 star thresholds. Score higher to earn more stars! Stars can be spent on power-ups."
                        ),
                        HelpItem(
                            emoji = "\uD83D\uDD25",
                            title = "Combos",
                            description = "When clearing gems causes new matches to form, that's a combo! Combos give bonus points and build up multipliers."
                        )
                    )
                )

                // --- Special Gems ---
                HelpSection(
                    title = "\u2728 Special Gems",
                    items = listOf(
                        HelpItem(
                            emoji = "\u2500",
                            title = "Striped Gem",
                            description = "Match 4 in a row to create a Striped gem. When cleared, it destroys an entire row or column depending on the stripe direction."
                        ),
                        HelpItem(
                            emoji = "\uD83D\uDCA0",
                            title = "Wrapped Gem",
                            description = "Match in an L or T shape to create a Wrapped gem. When cleared, it explodes in a 3x3 area around it."
                        ),
                        HelpItem(
                            emoji = "\uD83C\uDF08",
                            title = "Color Bomb",
                            description = "Match 5 in a row to create a Color Bomb. Swap it with any gem to clear ALL gems of that color from the board!"
                        )
                    )
                )

                // --- Obstacles ---
                HelpSection(
                    title = "\uD83E\uDDE9 Obstacles",
                    items = listOf(
                        HelpItem(
                            emoji = "\uD83E\uDDCA",
                            title = "Ice",
                            description = "A thin ice layer covers a gem. The gem can still be matched normally. When matched, the ice breaks and the gem clears. Takes 1 hit."
                        ),
                        HelpItem(
                            emoji = "\uD83E\uDDCA\uD83E\uDDCA",
                            title = "Reinforced Ice",
                            description = "A thicker, double-layered ice. Takes 2 hits to break — the first hit downgrades it to regular Ice, the second hit breaks it fully."
                        ),
                        HelpItem(
                            emoji = "\uD83D\uDD12",
                            title = "Locked Gem",
                            description = "The gem is frozen in place and cannot be swapped. To free it, match gems adjacent to the locked gem. Once unlocked, it becomes a normal gem."
                        ),
                        HelpItem(
                            emoji = "\uD83E\uDEA8",
                            title = "Stone Wall",
                            description = "A permanent, indestructible wall. No gem exists here and nothing can break it. Gems fall around stones, creating split gravity columns."
                        ),
                        HelpItem(
                            emoji = "\uD83D\uDCA3",
                            title = "Bomb",
                            description = "A countdown timer attached to a gem. The timer goes down by 1 each move. If it reaches 0, the bomb explodes and it's game over! Clear the gem to defuse it."
                        )
                    )
                )

                // --- Power-Ups ---
                HelpSection(
                    title = "\uD83D\uDE80 Power-Ups",
                    items = listOf(
                        HelpItem(
                            emoji = "\uD83D\uDD28",
                            title = "Hammer (3 \u2B50)",
                            description = "Tap any single gem to instantly destroy it. Great for breaking ice, defusing bombs, or clearing a tricky spot."
                        ),
                        HelpItem(
                            emoji = "\uD83C\uDF08",
                            title = "Color Bomb (5 \u2B50)",
                            description = "Tap any gem to clear ALL gems of that color from the board. Triggers cascades for massive combos!"
                        ),
                        HelpItem(
                            emoji = "\u2728",
                            title = "Extra Moves (2 \u2B50)",
                            description = "Adds 3 extra moves to your remaining move count. Use when you're close to completing the objective!"
                        )
                    )
                )

                // --- Game Modes ---
                HelpSection(
                    title = "\uD83C\uDFAE Game Modes",
                    items = listOf(
                        HelpItem(
                            emoji = "\uD83D\uDCCD",
                            title = "Story Levels",
                            description = "Progress through increasingly challenging levels with various objectives: reach a target score, break all ice, or clear specific gem colors."
                        ),
                        HelpItem(
                            emoji = "\uD83D\uDCC5",
                            title = "Daily Challenge",
                            description = "A new unique level every day! Difficulty scales from easy on Monday to hard on Sunday. Build streaks by completing consecutive days."
                        ),
                        HelpItem(
                            emoji = "\u23F1",
                            title = "Timed Challenge",
                            description = "Score as high as you can before time runs out! Choose Easy (120s), Medium (90s), or Hard (60s)."
                        )
                    )
                )

                // Bottom padding for scroll
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * A section header with a list of help items below it.
 */
@Composable
private fun HelpSection(
    title: String,
    items: List<HelpItem>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFFFFD700), // Gold
            modifier = Modifier.padding(bottom = 4.dp)
        )

        for (item in items) {
            HelpCard(item)
        }
    }
}

/**
 * A single help card with emoji icon, title, and description.
 */
@Composable
private fun HelpCard(item: HelpItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Emoji icon
        Text(
            text = item.emoji,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Data class for a single help entry.
 */
private data class HelpItem(
    val emoji: String,
    val title: String,
    val description: String
)
