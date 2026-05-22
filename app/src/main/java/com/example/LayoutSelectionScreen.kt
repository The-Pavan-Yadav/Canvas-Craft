package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CollageLayout {
    Grid2, Grid3, Grid4, Grid5
}

@Composable
fun LayoutSelectionScreen(
    onLayoutSelected: (CollageLayout) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val BluePurpleGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF090616), // Extra deep obsidian space
            Color(0xFF140D36), // Moody midnight violet
            Color(0xFF08040F)  // Bottom space dark
        )
    )

    Box(modifier = modifier.fillMaxSize().background(BluePurpleGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = "CANVAS",
                    style = TextStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = 1.8.sp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFC084FC), Color(0xFFEC4899))
                        )
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                     text = "CRAFT",
                     style = TextStyle(
                         fontWeight = FontWeight.Light,
                         fontSize = 32.sp,
                         letterSpacing = 1.8.sp,
                         color = Color.White
                     )
                )
            }

            Text(
                text = "Choose a canvas grid or let Canvas Craft decide",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(CollageLayout.values()) { layout ->
                    LayoutCard(layout = layout, onClick = { onLayoutSelected(layout) })
                }
            }
        }

        // Floating Action Button (FAB) for instant random layout selection
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.88f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "fabSelectionScale"
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = if (isPressed) 6.dp else 16.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color(0xFFC084FC).copy(alpha = 0.4f),
                    spotColor = Color(0xFFEC4899).copy(alpha = 0.6f)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.material3.ripple(color = Color.White),
                    onClick = {
                        val randomLayout = CollageLayout.values().random()
                        onLayoutSelected(randomLayout)
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Quick Start",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Quick Start",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LayoutCard(layout: CollageLayout, onClick: () -> Unit) {
    val animScale = remember { Animatable(0.85f) }
    val animAlpha = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(50f) }

    LaunchedEffect(layout) {
        val delayMs = when (layout) {
            CollageLayout.Grid2 -> 0L
            CollageLayout.Grid3 -> 100L
            CollageLayout.Grid4 -> 200L
            CollageLayout.Grid5 -> 300L
        }
        delay(delayMs)
        launch {
            animScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            animAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400)
            )
        }
        launch {
            animOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                val combinedScale = animScale.value * pressScale
                scaleX = combinedScale
                scaleY = combinedScale
                translationY = animOffsetY.value
                alpha = animAlpha.value
            }
            .aspectRatio(1f)
            .shadow(
                elevation = if (isPressed) 18.dp else 10.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0xFF8B5CF6).copy(alpha = 0.35f),
                spotColor = Color(0xFFC084FC).copy(alpha = 0.45f)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1F1A3A).copy(alpha = 0.65f),
                        Color(0xFF131024).copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = Color(0xFFC084FC)),
                onClick = onClick
            )
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LayoutPreview(layout = layout, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "${layout.name.replace("Grid", "")} Grid",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LayoutPreview(layout: CollageLayout, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(6.dp)
    ) {
        when (layout) {
            CollageLayout.Grid2 -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                    Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                }
            }
            CollageLayout.Grid3 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                    }
                }
            }
            CollageLayout.Grid4 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                    }
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                    }
                }
            }
            CollageLayout.Grid5 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                    }
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LayoutSelectionScreenPreview() {
    MyApplicationTheme {
        LayoutSelectionScreen()
    }
}
