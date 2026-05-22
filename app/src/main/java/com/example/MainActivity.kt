package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          var currentLayoutName by rememberSaveable { mutableStateOf<String?>(null) }
          val currentLayout = currentLayoutName?.let { name ->
              try {
                  CollageLayout.valueOf(name)
              } catch (e: Exception) {
                  null
              }
          }
          
          Box(modifier = Modifier.fillMaxSize()) {
              Crossfade(
                  targetState = currentLayout,
                  animationSpec = tween(400),
                  label = "screenTransition"
              ) { layout ->
                  if (layout == null) {
                      LayoutSelectionScreen(
                          onLayoutSelected = { currentLayoutName = it.name },
                          modifier = Modifier.padding(innerPadding)
                      )
                  } else {
                      BackHandler {
                          currentLayoutName = null
                      }
                      CollageCanvas(
                          layout = layout,
                          onBackClick = { currentLayoutName = null },
                          modifier = Modifier.padding(innerPadding)
                      )
                  }
              }
              
              Text(
                  text = "Made by Niharika",
                  color = Color.White.copy(alpha = 0.5f),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier
                      .align(Alignment.BottomCenter)
                      .padding(bottom = innerPadding.calculateBottomPadding() + 16.dp)
              )
          }
        }
      }
    }
  }
}


