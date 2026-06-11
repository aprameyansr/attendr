package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.SuccessColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeableCard(
    onSwipeLeft: () -> Unit, // Swiping LEFT slides card to left, confirming Present
    onSwipeRight: () -> Unit, // Swiping RIGHT slides card to right, confirming Absent
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    // Smoothly animated offset
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    
    // Convert threshold in DP to pixels
    val swipeThresholdPx = with(density) { 120.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(24.dp))
    ) {
        // --- ACTION BACKGROUNDS ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        offsetX.value < 0 -> SuccessColor // Swiping left (slid to left) reveals green Present action on the right
                        offsetX.value > 0 -> ErrorColor   // Swiping right (slid to right) reveals red Absent action on the left
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = when {
                offsetX.value < 0 -> Alignment.CenterEnd   // Present text + icon revealed on right
                offsetX.value > 0 -> Alignment.CenterStart // Absent text + icon revealed on left
                else -> Alignment.Center
            }
        ) {
            if (offsetX.value < 0) {
                Row(
                    modifier = Modifier.padding(end = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("PRESENT", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                    Icon(Icons.Filled.Check, contentDescription = "Mark Present", tint = Color.White)
                }
            } else if (offsetX.value > 0) {
                Row(
                    modifier = Modifier.padding(start = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Mark Absent", tint = Color.White)
                    Text("ABSENT", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        // --- INTERACTIVE FOREGROUND CARD ---
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                when {
                                    offsetX.value < -swipeThresholdPx -> {
                                        onSwipeLeft()
                                    }
                                    offsetX.value > swipeThresholdPx -> {
                                        onSwipeRight()
                                    }
                                }
                                // Spring back smoothly automatically
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = 300f)
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                // Add a subtle scaling dampener so you feel resistance as you pull farther
                                val currentOffset = offsetX.value + dragAmount
                                offsetX.snapTo(currentOffset)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
