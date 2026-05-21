package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          var currentLayout by remember { mutableStateOf<CollageLayout?>(null) }
          val layout = currentLayout
          
          androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
              if (layout == null) {
                  LayoutSelectionScreen(
                      onLayoutSelected = { currentLayout = it },
                      modifier = Modifier.padding(innerPadding)
                  )
              } else {
                  BackHandler {
                      currentLayout = null
                  }
                  CollageCanvas(layout = layout, modifier = Modifier.padding(innerPadding))
              }
              
              Text(
                  text = "Made by Pavan",
                  color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                  fontSize = 12.sp,
                  fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                  modifier = Modifier
                      .align(androidx.compose.ui.Alignment.BottomCenter)
                      .padding(bottom = innerPadding.calculateBottomPadding() + 16.dp)
              )
          }
        }
      }
    }
  }
}


