package com.example

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

enum class CollageLayout {
    Grid2, Grid3, Grid4, Grid5
}

@Composable
fun LayoutSelectionScreen(
    onLayoutSelected: (CollageLayout) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.DarkGray)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Layout",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp, top = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(CollageLayout.values()) { layout ->
                LayoutCard(layout = layout, onClick = { onLayoutSelected(layout) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutCard(layout: CollageLayout, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
        modifier = Modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LayoutPreview(layout = layout, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${layout.name.replace("Grid", "")} Grid",
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LayoutPreview(layout: CollageLayout, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray.copy(alpha = 0.2f))
            .padding(4.dp)
    ) {
        when (layout) {
            CollageLayout.Grid2 -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color.White))
                    Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color.White))
                }
            }
            CollageLayout.Grid3 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color.White))
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color.White))
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color.White))
                    }
                }
            }
            CollageLayout.Grid4 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color.White))
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color.White))
                    }
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color.White))
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color.White))
                    }
                }
            }
            CollageLayout.Grid5 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color.White))
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color.White))
                    }
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color.White))
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color.White))
                        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color.White))
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
