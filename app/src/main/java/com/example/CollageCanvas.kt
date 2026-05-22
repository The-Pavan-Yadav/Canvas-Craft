package com.example

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val AccentColor = Color(0xFFA78BFA)

data class FrameState(
    val index: Int,
    val imageUri: Uri? = null,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    val offset: Offset get() = Offset(offsetX, offsetY)
}

data class CollageAspectRatio(
    val name: String,
    val value: Float,
    val label: String
)

sealed class BackgroundOption {
    abstract val name: String
    
    data class Solid(override val name: String, val color: Color) : BackgroundOption()
    data class Gradient(override val name: String, val colors: List<Color>) : BackgroundOption()
}

data class CollageState(
    val frameStates: List<FrameState>,
    val selectedRatioIndex: Int,
    val backgroundIndex: Int,
    val borderSpacing: Float,
    val innerSpacing: Float,
    val cornerRadius: Float,
    val shadowIntensity: Float,
    val borderWidth: Float,
    val borderColorIndex: Int
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CollageCanvas(
    layout: CollageLayout,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    var activePreviewWidthDp by remember { mutableStateOf(300.dp) }
    val context = LocalContext.current

    val count = remember(layout) {
        when (layout) {
            CollageLayout.Grid2 -> 2
            CollageLayout.Grid3 -> 3
            CollageLayout.Grid4 -> 4
            CollageLayout.Grid5 -> 5
        }
    }

    // Standard frames state (loaded and restored properly)
    val frameStates = rememberSaveable(layout, saver = listSaver(
        save = { list ->
            val flat = mutableListOf<Any>()
            list.forEach { state ->
                flat.add(state.index)
                flat.add(state.imageUri?.toString() ?: "")
                flat.add(state.scale)
                flat.add(state.rotation)
                flat.add(state.offsetX)
                flat.add(state.offsetY)
            }
            flat
        },
        restore = { flat ->
            val restored = mutableListOf<FrameState>()
            for (i in flat.indices step 6) {
                if (i + 5 < flat.size) {
                    val index = (flat[i] as? Number)?.toInt() ?: 0
                    val uriStr = flat[i + 1] as? String ?: ""
                    val uri = if (uriStr.isNotEmpty()) Uri.parse(uriStr) else null
                    val scale = (flat[i + 2] as? Number)?.toFloat() ?: 1f
                    val rotation = (flat[i + 3] as? Number)?.toFloat() ?: 0f
                    val offsetX = (flat[i + 4] as? Number)?.toFloat() ?: 0f
                    val offsetY = (flat[i + 5] as? Number)?.toFloat() ?: 0f
                    restored.add(FrameState(index, uri, scale, rotation, offsetX, offsetY))
                }
            }
            mutableStateListOf<FrameState>().apply { addAll(restored) }
        }
    )) {
        mutableStateListOf<FrameState>().apply {
            repeat(count) { add(FrameState(index = it)) }
        }
    }

    var selectedFrameIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    // --- State values with history sync capability ---
    val aspectRatios = remember {
        listOf(
            CollageAspectRatio("Square", 1.0f, "1:1"),
            CollageAspectRatio("Portrait", 4f / 5f, "4:5"),
            CollageAspectRatio("Landscape", 16f / 9f, "16:9"),
            CollageAspectRatio("Story", 9f / 16f, "9:16")
        )
    }
    
    val backgroundOptions = remember {
        listOf(
            BackgroundOption.Solid("Charcoal", Color(0xFF1E1E1E)),
            BackgroundOption.Solid("Deep Blue", Color(0xFF0F172A)),
            BackgroundOption.Solid("Warm Cream", Color(0xFFF1EFE7)),
            BackgroundOption.Solid("Macaron Pink", Color(0xFFFFECEF)),
            BackgroundOption.Gradient("Aurora", listOf(Color(0xFF02AAB0), Color(0xFF00CDAC))),
            BackgroundOption.Gradient("Sunset Glow", listOf(Color(0xFFFE8C00), Color(0xFFF12711))),
            BackgroundOption.Gradient("Elegance Violet", listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFF56040))),
            BackgroundOption.Gradient("Ocean Breeze", listOf(Color(0xFF00C6FF), Color(0xFF0072FF))),
            BackgroundOption.Gradient("Cyber Punk", listOf(Color(0xFFF107A3), Color(0xFF07F1E7))),
            BackgroundOption.Gradient("Golden Dust", listOf(Color(0xFF1E1E1E), Color(0xFFE6C47C)))
        )
    }

    val borderColors = remember {
        listOf(
            Color.Transparent,
            Color.White,
            Color.LightGray,
            Color(0xFFE6C47C), // Gold
            Color(0xFFA78BFA), // Violet Accent
            Color(0xFFFFB4AB), // Coral
            Color(0xFF00C6FF), // Cyan
            Color(0xFF1E1E1E)  // Charcoal
        )
    }

    var currentRatioIndex by remember { mutableStateOf(0) }
    var currentBackgroundIndex by remember { mutableStateOf(0) }
    var borderSpacing by remember { mutableStateOf(8f) }
    var innerSpacing by remember { mutableStateOf(8f) }
    var cornerRadius by remember { mutableStateOf(12f) }
    var shadowIntensity by remember { mutableStateOf(0f) }
    var borderWidth by remember { mutableStateOf(0f) }
    var selectedBorderColorIndex by remember { mutableStateOf(1) } // White default

    // --- High-Fidelity History Engine (Undo / Redo) ---
    val undoStack = remember { mutableStateListOf<CollageState>() }
    val redoStack = remember { mutableStateListOf<CollageState>() }

    fun captureCurrentState(): CollageState {
        return CollageState(
            frameStates = frameStates.map { it.copy() },
            selectedRatioIndex = currentRatioIndex,
            backgroundIndex = currentBackgroundIndex,
            borderSpacing = borderSpacing,
            innerSpacing = innerSpacing,
            cornerRadius = cornerRadius,
            shadowIntensity = shadowIntensity,
            borderWidth = borderWidth,
            borderColorIndex = selectedBorderColorIndex
        )
    }

    fun pushHistory() {
        val snapshot = captureCurrentState()
        if (undoStack.isEmpty() || undoStack.last() != snapshot) {
            undoStack.add(snapshot)
            redoStack.clear()
        }
    }

    fun restoreState(state: CollageState) {
        frameStates.clear()
        frameStates.addAll(state.frameStates)
        currentRatioIndex = state.selectedRatioIndex
        currentBackgroundIndex = state.backgroundIndex
        borderSpacing = state.borderSpacing
        innerSpacing = state.innerSpacing
        cornerRadius = state.cornerRadius
        shadowIntensity = state.shadowIntensity
        borderWidth = state.borderWidth
        selectedBorderColorIndex = state.borderColorIndex
    }

    fun undo() {
        if (undoStack.size > 1) {
            val last = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(last)
            val previous = undoStack.last()
            restoreState(previous)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(next)
            restoreState(next)
        }
    }

    // Launch initial state capture once frameStates is populated
    LaunchedEffect(layout) {
        if (undoStack.isEmpty()) {
            pushHistory()
        }
    }

    // Export Loading Transition State
    var isSaving by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val idx = selectedFrameIndex
            if (idx != null && idx in frameStates.indices) {
                frameStates[idx] = frameStates[idx].copy(imageUri = uri)
                pushHistory()
            }
        }
    }

    val onClickAdd: (Int) -> Unit = { index ->
        selectedFrameIndex = index
        galleryLauncher.launch("image/*")
    }

    // Detection of device orientation to pick beautiful Canonical Layout representation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val onResetClick: () -> Unit = {
        for (i in frameStates.indices) {
            frameStates[i] = frameStates[i].copy(
                scale = 1f,
                rotation = 0f,
                offsetX = 0f,
                offsetY = 0f
            )
        }
        pushHistory()
        Toast.makeText(context, "Layout transforms reset", Toast.LENGTH_SHORT).show()
    }

    // Backdrops
    val activeBackground = backgroundOptions.getOrElse(currentBackgroundIndex) { backgroundOptions[0] }
    val backgroundModifier = when (activeBackground) {
        is BackgroundOption.Solid -> Modifier.background(activeBackground.color)
        is BackgroundOption.Gradient -> Modifier.background(
            Brush.linearGradient(activeBackground.colors)
        )
    }

    val activeRatio = aspectRatios.getOrElse(currentRatioIndex) { aspectRatios[0] }
    val activeBorderColor = borderColors.getOrElse(selectedBorderColorIndex) { Color.White }

    val onSaveClick: () -> Unit = {
        if (!isSaving) {
            isSaving = true
            coroutineScope.launch {
                try {
                    // Slight artificial delay to allow smooth transition animation and draw cycle
                    withContext(Dispatchers.IO) { Thread.sleep(300) }
                    
                    val targetWidth = 1200f
                    val ratioVal = if (activeRatio.value > 0f) activeRatio.value else 1f
                    val targetHeight = targetWidth / ratioVal
                    
                    val bitmap = Bitmap.createBitmap(targetWidth.toInt(), targetHeight.toInt(), Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    
                    // 1. Draw Backdrop background (solid color or gradient)
                    val activeBackground = backgroundOptions.getOrElse(currentBackgroundIndex) { backgroundOptions[0] }
                    when (activeBackground) {
                        is BackgroundOption.Solid -> {
                            val paint = android.graphics.Paint().apply {
                                color = activeBackground.color.toArgb()
                                style = android.graphics.Paint.Style.FILL
                            }
                            canvas.drawRect(0f, 0f, targetWidth, targetHeight, paint)
                        }
                        is BackgroundOption.Gradient -> {
                            val colors = activeBackground.colors.map { it.toArgb() }.toIntArray()
                            val shader = android.graphics.LinearGradient(
                                0f, 0f, targetWidth, targetHeight,
                                colors, null, android.graphics.Shader.TileMode.CLAMP
                            )
                            val paint = android.graphics.Paint().apply {
                                setShader(shader)
                                style = android.graphics.Paint.Style.FILL
                            }
                            canvas.drawRect(0f, 0f, targetWidth, targetHeight, paint)
                        }
                    }
                    
                    // 2. Calculate coordinates and scale factor based on live preview container width
                    val previewWidthVal = if (activePreviewWidthDp.value > 10f) activePreviewWidthDp.value else 300f
                    val scaleFactor = targetWidth / previewWidthVal
                    
                    val borderSpacingPx = borderSpacing * scaleFactor
                    val innerSpacingPx = innerSpacing * scaleFactor
                    val cornerRadiusPx = cornerRadius * scaleFactor
                    val borderWidthPx = borderWidth * scaleFactor
                    
                    val framesRects = mutableListOf<android.graphics.RectF>()
                    val availableWidth = targetWidth - 2 * borderSpacingPx
                    val availableHeight = targetHeight - 2 * borderSpacingPx
                    
                    when (layout) {
                        CollageLayout.Grid2 -> {
                            val colWidth = (availableWidth - innerSpacingPx) / 2f
                            val bottomVal = borderSpacingPx + availableHeight
                            framesRects.add(android.graphics.RectF(borderSpacingPx, borderSpacingPx, borderSpacingPx + colWidth, bottomVal))
                            framesRects.add(android.graphics.RectF(borderSpacingPx + colWidth + innerSpacingPx, borderSpacingPx, targetWidth - borderSpacingPx, bottomVal))
                        }
                        CollageLayout.Grid3 -> {
                            val row0Height = (availableHeight - innerSpacingPx) * (1.0f / 2.2f)
                            val row1Height = (availableHeight - innerSpacingPx) * (1.2f / 2.2f)
                            framesRects.add(android.graphics.RectF(borderSpacingPx, borderSpacingPx, targetWidth - borderSpacingPx, borderSpacingPx + row0Height))
                            
                            val top1 = borderSpacingPx + row0Height + innerSpacingPx
                            val bottom1 = top1 + row1Height
                            val colWidth = (availableWidth - innerSpacingPx) / 2f
                            framesRects.add(android.graphics.RectF(borderSpacingPx, top1, borderSpacingPx + colWidth, bottom1))
                            framesRects.add(android.graphics.RectF(borderSpacingPx + colWidth + innerSpacingPx, top1, targetWidth - borderSpacingPx, bottom1))
                        }
                        CollageLayout.Grid4 -> {
                            val colWidth = (availableWidth - innerSpacingPx) / 2f
                            val rowHeight = (availableHeight - innerSpacingPx) / 2f
                            val left1 = borderSpacingPx
                            val right1 = borderSpacingPx + colWidth
                            val left2 = borderSpacingPx + colWidth + innerSpacingPx
                            val right2 = targetWidth - borderSpacingPx
                            val top1 = borderSpacingPx
                            val bottom1 = borderSpacingPx + rowHeight
                            val top2 = borderSpacingPx + rowHeight + innerSpacingPx
                            val bottom2 = targetHeight - borderSpacingPx
                            
                            framesRects.add(android.graphics.RectF(left1, top1, right1, bottom1))
                            framesRects.add(android.graphics.RectF(left2, top1, right2, bottom1))
                            framesRects.add(android.graphics.RectF(left1, top2, right1, bottom2))
                            framesRects.add(android.graphics.RectF(left2, top2, right2, bottom2))
                        }
                        CollageLayout.Grid5 -> {
                            val row0Height = (availableHeight - innerSpacingPx) * (0.9f / 2.0f)
                            val row1Height = (availableHeight - innerSpacingPx) * (1.1f / 2.0f)
                            
                            val top0 = borderSpacingPx
                            val bottom0 = borderSpacingPx + row0Height
                            val colWidth0 = (availableWidth - innerSpacingPx) / 2f
                            framesRects.add(android.graphics.RectF(borderSpacingPx, top0, borderSpacingPx + colWidth0, bottom0))
                            framesRects.add(android.graphics.RectF(borderSpacingPx + colWidth0 + innerSpacingPx, top0, targetWidth - borderSpacingPx, bottom0))
                            
                            val top1 = borderSpacingPx + row0Height + innerSpacingPx
                            val bottom1 = targetHeight - borderSpacingPx
                            val colWidth1 = (availableWidth - 2 * innerSpacingPx) / 3f
                            framesRects.add(android.graphics.RectF(borderSpacingPx, top1, borderSpacingPx + colWidth1, bottom1))
                            framesRects.add(android.graphics.RectF(borderSpacingPx + colWidth1 + innerSpacingPx, top1, borderSpacingPx + 2f * colWidth1 + innerSpacingPx, bottom1))
                            framesRects.add(android.graphics.RectF(borderSpacingPx + 2f * colWidth1 + 2f * innerSpacingPx, top1, targetWidth - borderSpacingPx, bottom1))
                        }
                    }
                    
                    // 3. Draw each frame (shadow, clipped image, border)
                    for (i in 0 until count) {
                        if (i >= frameStates.size) break
                        val state = frameStates[i]
                        val rect = framesRects[i]
                        
                        val left = rect.left
                        val top = rect.top
                        val right = rect.right
                        val bottom = rect.bottom
                        val frameWidth = rect.width()
                        val frameHeight = rect.height()
                        
                        // Draw Drop Shadow
                        if (shadowIntensity > 0f) {
                            val shadowPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb((shadowIntensity * 255).toInt(), 0, 0, 0)
                                style = android.graphics.Paint.Style.FILL
                                isAntiAlias = true
                            }
                            val shadowOffset = shadowIntensity * 12f * scaleFactor
                            canvas.drawRoundRect(
                                android.graphics.RectF(left + shadowOffset, top + shadowOffset, right + shadowOffset, bottom + shadowOffset),
                                cornerRadiusPx, cornerRadiusPx,
                                shadowPaint
                            )
                        }
                        
                        // Clip and Draw frame content
                        val path = android.graphics.Path().apply {
                            addRoundRect(
                                rect,
                                cornerRadiusPx, cornerRadiusPx,
                                android.graphics.Path.Direction.CW
                            )
                        }
                        
                        canvas.save()
                        canvas.clipPath(path)
                        
                        if (state.imageUri != null) {
                            val srcBitmap = withContext(Dispatchers.IO) {
                                loadUriToBitmap(context, state.imageUri)
                            }
                            if (srcBitmap != null) {
                                val baseScaleX = frameWidth / srcBitmap.width.toFloat()
                                val baseScaleY = frameHeight / srcBitmap.height.toFloat()
                                val baseScale = maxOf(baseScaleX, baseScaleY)
                                
                                val dx = (frameWidth - srcBitmap.width.toFloat() * baseScale) / 2f
                                val dy = (frameHeight - srcBitmap.height.toFloat() * baseScale) / 2f
                                
                                val matrix = android.graphics.Matrix()
                                matrix.postScale(baseScale, baseScale)
                                matrix.postTranslate(left + dx, top + dy)
                                
                                val frameCenterX = left + frameWidth / 2f
                                val frameCenterY = top + frameHeight / 2f
                                matrix.postScale(state.scale, state.scale, frameCenterX, frameCenterY)
                                matrix.postRotate(state.rotation, frameCenterX, frameCenterY)
                                
                                val scaledOffsetX = state.offsetX * scaleFactor
                                val scaledOffsetY = state.offsetY * scaleFactor
                                matrix.postTranslate(scaledOffsetX, scaledOffsetY)
                                
                                val imagePaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    isFilterBitmap = true
                                }
                                canvas.drawBitmap(srcBitmap, matrix, imagePaint)
                                srcBitmap.recycle()
                            } else {
                                // Fallback placeholder
                                val framePaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.argb((0.15f * 255).toInt(), 211, 211, 211)
                                    style = android.graphics.Paint.Style.FILL
                                }
                                canvas.drawRect(rect, framePaint)
                            }
                        } else {
                            // Empty frame background color
                            val framePaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb((0.15f * 255).toInt(), 211, 211, 211)
                                style = android.graphics.Paint.Style.FILL
                            }
                            canvas.drawRect(rect, framePaint)
                        }
                        
                        canvas.restore()
                        
                        // Draw Border Outline
                        if (borderWidth > 0f) {
                            val borderPaint = android.graphics.Paint().apply {
                                color = activeBorderColor.toArgb()
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = borderWidthPx
                                isAntiAlias = true
                            }
                            canvas.drawRoundRect(
                                android.graphics.RectF(
                                    left + borderWidthPx / 2f,
                                    top + borderWidthPx / 2f,
                                    right - borderWidthPx / 2f,
                                    bottom - borderWidthPx / 2f
                                ),
                                cornerRadiusPx, cornerRadiusPx,
                                borderPaint
                            )
                        }
                    }
                    
                    val success = saveBitmapToDownloads(context, bitmap)
                    bitmap.recycle()
                    
                    if (success) {
                        Toast.makeText(context, "Collage saved to Downloads folder!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to save collage", Toast.LENGTH_SHORT).show()
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    Toast.makeText(context, "Error saving collage: ${t.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isSaving = false
                }
            }
        }
    }



    // --- Custom Visual Tab Switcher Panel ---
    var activeTab by rememberSaveable { mutableStateOf("Ratio") }
    val tabs = listOf("Ratio", "Background", "Framing", "Effects")

    @Composable
    fun CustomTabsWidget() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.04f), shape = RoundedCornerShape(24.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = activeTab == tab
                val animBgColor by animateColorAsState(
                    targetValue = if (isSelected) AccentColor.copy(alpha = 0.2f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 250),
                    label = "tabBg"
                )
                val animTextColor by animateColorAsState(
                    targetValue = if (isSelected) AccentColor else Color.White.copy(alpha = 0.6f),
                    animationSpec = tween(durationMillis = 250),
                    label = "tabText"
                )
                val interactionSource = remember { MutableInteractionSource() }
                val isTabPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isTabPressed) 0.94f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "tabScale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(animBgColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.material3.ripple(color = AccentColor.copy(alpha = 0.3f)),
                            onClick = { activeTab = tab }
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = animTextColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    @Composable
    fun TabContentWidget(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (activeTab) {
                "Ratio" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Aspect Ratio",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            aspectRatios.forEachIndexed { index, ratio ->
                                AspectRatioCell(
                                    aspectRatio = ratio,
                                    isSelected = currentRatioIndex == index,
                                    onClick = {
                                        currentRatioIndex = index
                                        pushHistory()
                                    }
                                )
                            }
                        }
                    }
                }
                "Background" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Backdrop Tone & Gradient",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(backgroundOptions) { index, option ->
                                BackgroundPresetCell(
                                    option = option,
                                    isSelected = currentBackgroundIndex == index,
                                    onClick = {
                                        currentBackgroundIndex = index
                                        pushHistory()
                                    }
                                )
                            }
                        }
                    }
                }
                "Framing" -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        // Slider: Outer spacing Padding
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Outer Rim", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${borderSpacing.toInt()} dp", color = AccentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = borderSpacing,
                                onValueChange = { borderSpacing = it },
                                onValueChangeFinished = { pushHistory() },
                                valueRange = 0f..32f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentColor,
                                    activeTrackColor = AccentColor,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }

                        // Slider: Inner Gap
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Grid Gap", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${innerSpacing.toInt()} dp", color = AccentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = innerSpacing,
                                onValueChange = { innerSpacing = it },
                                onValueChangeFinished = { pushHistory() },
                                valueRange = 0f..24f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentColor,
                                    activeTrackColor = AccentColor,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }

                        // Slider: Corner Radius
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Frame Corner Radius", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${cornerRadius.toInt()} dp", color = AccentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = cornerRadius,
                                onValueChange = { cornerRadius = it },
                                onValueChangeFinished = { pushHistory() },
                                valueRange = 0f..32f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentColor,
                                    activeTrackColor = AccentColor,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }

                        // Slider: Border Width
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Border Stroke Outline", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${borderWidth.toInt()} dp", color = AccentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = borderWidth,
                                onValueChange = { borderWidth = it },
                                onValueChangeFinished = { pushHistory() },
                                valueRange = 0f..6f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentColor,
                                    activeTrackColor = AccentColor,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }

                        // Border Color Palette
                        if (borderWidth > 0f) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Stroke Color", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(borderColors) { index, color ->
                                        BorderColorCell(
                                            color = color,
                                            isSelected = selectedBorderColorIndex == index,
                                            onClick = {
                                                selectedBorderColorIndex = index
                                                pushHistory()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "Effects" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Frame Shadow intensity", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${(shadowIntensity * 100).toInt()}%", color = AccentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = shadowIntensity,
                                onValueChange = { shadowIntensity = it },
                                onValueChangeFinished = { pushHistory() },
                                valueRange = 0f..0.8f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentColor,
                                    activeTrackColor = AccentColor,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    val BluePurpleGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF090616), // Extra deep obsidian space
                Color(0xFF140D36), // Moody midnight violet
                Color(0xFF08040F)  // Bottom space dark
            )
        )
    }

    Box(modifier = modifier.fillMaxSize().background(BluePurpleGradient)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Custom Header Bar with Title, Back, Undo & Redo ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0A21).copy(alpha = 0.65f))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        ),
                        shape = RectangleShape
                    )
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BouncyIconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "CANVAS",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.5.sp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFC084FC), Color(0xFFEC4899))
                            )
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CRAFT",
                        style = TextStyle(
                            fontWeight = FontWeight.Light,
                            fontSize = 18.sp,
                            letterSpacing = 1.5.sp,
                            color = Color.White
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BouncyIconButton(
                        onClick = { undo() },
                        enabled = undoStack.size > 1
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.size > 1) Color(0xFFC084FC) else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    BouncyIconButton(
                        onClick = { redo() },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStack.isNotEmpty()) Color(0xFFEC4899) else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // --- Main Content Splitter (Orientation-Sensitive Canonical Layout) ---
            if (isLandscape) {
                // Side-by-side tablet / landscape layout
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Active Canvas Preview
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        CollagePreviewContainer(
                            layout = layout,
                            activeRatio = activeRatio,
                            backgroundModifier = backgroundModifier,
                            borderSpacing = borderSpacing,
                            innerSpacing = innerSpacing,
                            cornerRadius = cornerRadius,
                            shadowIntensity = shadowIntensity,
                            borderWidth = borderWidth,
                            activeBorderColor = activeBorderColor,
                            frameStates = frameStates,
                            selectedFrameIndex = selectedFrameIndex,
                            onClickAdd = onClickAdd,
                            onFrameSelected = { selectedFrameIndex = it },
                            onDeselectAll = {
                                if (selectedFrameIndex != null) {
                                    pushHistory()
                                    selectedFrameIndex = null
                                }
                            },
                            onPreviewWidthChanged = { activePreviewWidthDp = it }
                        )

                        // Floating Action Button (FAB) to shuffle and randomize backgrounds!
                        BackgroundShuffleFAB(
                            onClick = {
                                val nextIndex = (currentBackgroundIndex + 1) % backgroundOptions.size
                                pushHistory()
                                currentBackgroundIndex = nextIndex
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        )
                    }

                    // Right Side: Sidebar Navigation / Side Controls Panel (Scrollable Card)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF13102B).copy(alpha = 0.82f),
                                        Color(0xFF090616).copy(alpha = 0.94f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.16f),
                                        Color.White.copy(alpha = 0.03f)
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            CustomTabsWidget()
                            Box(modifier = Modifier.weight(1f)) {
                                TabContentWidget()
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controls Utility Footer Row
                        BottomEditorToolbar(
                            onGalleryClick = {
                                val idx = selectedFrameIndex
                                if (idx != null) {
                                    galleryLauncher.launch("image/*")
                                } else {
                                    Toast.makeText(context, "Select a collage frame first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onResetClick = onResetClick,
                            onSaveClick = onSaveClick
                        )
                    }
                }
            } else {
                // Portrait Layout
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top: Preview container taking up flexible weight to allow dynamic resizing
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CollagePreviewContainer(
                            layout = layout,
                            activeRatio = activeRatio,
                            backgroundModifier = backgroundModifier,
                            borderSpacing = borderSpacing,
                            innerSpacing = innerSpacing,
                            cornerRadius = cornerRadius,
                            shadowIntensity = shadowIntensity,
                            borderWidth = borderWidth,
                            activeBorderColor = activeBorderColor,
                            frameStates = frameStates,
                            selectedFrameIndex = selectedFrameIndex,
                            onClickAdd = onClickAdd,
                            onFrameSelected = { selectedFrameIndex = it },
                            onDeselectAll = {
                                if (selectedFrameIndex != null) {
                                    pushHistory()
                                    selectedFrameIndex = null
                                }
                            },
                            onPreviewWidthChanged = { activePreviewWidthDp = it }
                        )

                        // Floating Action Button (FAB) to shuffle and randomize backgrounds!
                        BackgroundShuffleFAB(
                            onClick = {
                                val nextIndex = (currentBackgroundIndex + 1) % backgroundOptions.size
                                pushHistory()
                                currentBackgroundIndex = nextIndex
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        )
                    }

                    // Bottom: Tab and Controls sheet with dark gradient theme
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF13102B).copy(alpha = 0.90f),
                                        Color(0xFF080518).copy(alpha = 0.98f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.16f),
                                        Color.White.copy(alpha = 0.03f)
                                    )
                                ),
                                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CustomTabsWidget()
                        Box(modifier = Modifier.heightIn(max = 200.dp, min = 120.dp).fillMaxWidth()) {
                            TabContentWidget()
                        }
                        
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                        BottomEditorToolbar(
                            onGalleryClick = {
                                val idx = selectedFrameIndex
                                if (idx != null) {
                                    galleryLauncher.launch("image/*")
                                } else {
                                    Toast.makeText(context, "Select a collage frame first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onResetClick = onResetClick,
                            onSaveClick = onSaveClick
                        )
                    }
                }
            }
        }

        // --- Custom Interactive Loading Layer while Exporting ---
        AnimatedVisibility(
            visible = isSaving,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* Absorb taps entirely */ },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = AccentColor,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(52.dp)
                    )
                    Text(
                        text = "Rendering High-Res Collage...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Saving perfectly to your Downloads folder",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CollagePreviewContainer(
    layout: CollageLayout,
    activeRatio: CollageAspectRatio,
    backgroundModifier: Modifier,
    borderSpacing: Float,
    innerSpacing: Float,
    cornerRadius: Float,
    shadowIntensity: Float,
    borderWidth: Float,
    activeBorderColor: Color,
    frameStates: SnapshotStateList<FrameState>,
    selectedFrameIndex: Int?,
    onClickAdd: (Int) -> Unit,
    onFrameSelected: (Int) -> Unit,
    onDeselectAll: () -> Unit,
    onPreviewWidthChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        val ratioValue = activeRatio.value

        // Calculate maximum dimensions maintaining exact targets
        val previewWidth: Dp
        val previewHeight: Dp
        if (containerWidth / ratioValue > containerHeight) {
            previewHeight = containerHeight * 0.95f
            previewWidth = previewHeight * ratioValue
        } else {
            previewWidth = containerWidth * 0.92f
            previewHeight = previewWidth / ratioValue
        }

        // Propagate size dynamically to preserve custom high-res export coordinates
        LaunchedEffect(previewWidth) {
            onPreviewWidthChanged(previewWidth)
        }

        // Parent background tap-catcher to gracefully deselect & record history
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onDeselectAll()
                }
        )

        // The exact bounding container
        Box(
            modifier = Modifier
                .size(previewWidth, previewHeight)
                .clip(RoundedCornerShape(cornerRadius.dp))
                .then(backgroundModifier)
                .padding(borderSpacing.dp)
        ) {
            when (layout) {
                CollageLayout.Grid2 -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(innerSpacing.dp)
                    ) {
                        FrameContainer(0, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxSize())
                        FrameContainer(1, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxSize())
                    }
                }
                CollageLayout.Grid3 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(innerSpacing.dp)
                    ) {
                        FrameContainer(0, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxWidth())
                        Row(
                            modifier = Modifier.weight(1.2f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(innerSpacing.dp)
                        ) {
                            FrameContainer(1, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxSize())
                            FrameContainer(2, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxSize())
                        }
                    }
                }
                CollageLayout.Grid4 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(innerSpacing.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(innerSpacing.dp)
                        ) {
                            FrameContainer(0, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxSize())
                            FrameContainer(1, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxSize())
                        }
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(innerSpacing.dp)
                        ) {
                            FrameContainer(2, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxSize())
                            FrameContainer(3, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxSize())
                        }
                    }
                }
                CollageLayout.Grid5 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(innerSpacing.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(0.9f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(innerSpacing.dp)
                        ) {
                            FrameContainer(0, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxSize())
                            FrameContainer(1, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxSize())
                        }
                        Row(
                            modifier = Modifier.weight(1.1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(innerSpacing.dp)
                        ) {
                            FrameContainer(2, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxSize())
                            FrameContainer(3, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxSize())
                            FrameContainer(4, frameStates, selectedFrameIndex, onFrameSelected, onClickAdd, cornerRadius, shadowIntensity, borderWidth, activeBorderColor, Modifier.weight(1f).fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FrameContainer(
    index: Int,
    frameStates: SnapshotStateList<FrameState>,
    selectedFrameIndex: Int?,
    onSelected: (Int) -> Unit,
    onClickAdd: (Int) -> Unit,
    cornerRadius: Float,
    shadowIntensity: Float,
    borderWidth: Float,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val state = frameStates.getOrNull(index) ?: FrameState(index)
    CollageFrame(
        state = state,
        isSelected = selectedFrameIndex == index,
        onSelected = { onSelected(index) },
        onTransform = { pan, zoom, rotate ->
            if (index in frameStates.indices) {
                val current = frameStates[index]
                
                val sPanX = if (pan.x.isNaN() || pan.x.isInfinite()) 0f else pan.x
                val sPanY = if (pan.y.isNaN() || pan.y.isInfinite()) 0f else pan.y
                val sZoom = if (zoom.isNaN() || zoom.isInfinite() || zoom <= 0f) 1f else zoom
                val sRotate = if (rotate.isNaN() || rotate.isInfinite()) 0f else rotate

                val newScale = (current.scale * sZoom).coerceIn(0.5f, 5f)
                val newRotation = current.rotation + sRotate
                val newOffsetX = current.offsetX + sPanX
                val newOffsetY = current.offsetY + sPanY

                val finalScale = if (newScale.isNaN()) 1f else newScale
                val finalRotation = if (newRotation.isNaN()) 0f else newRotation
                val finalOffsetX = if (newOffsetX.isNaN()) 0f else newOffsetX
                val finalOffsetY = if (newOffsetY.isNaN()) 0f else newOffsetY

                frameStates[index] = current.copy(
                    scale = finalScale,
                    rotation = finalRotation,
                    offsetX = finalOffsetX,
                    offsetY = finalOffsetY
                )
            }
        },
        onReset = {
            if (index in frameStates.indices) {
                frameStates[index] = frameStates[index].copy(
                    scale = 1f,
                    rotation = 0f,
                    offsetX = 0f,
                    offsetY = 0f
                )
            }
        },
        onClickAdd = { onClickAdd(index) },
        cornerRadius = cornerRadius,
        shadowIntensity = shadowIntensity,
        borderWidth = borderWidth,
        borderColor = borderColor,
        modifier = modifier
    )
}

@Composable
fun CollageFrame(
    state: FrameState,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onTransform: (Offset, Float, Float) -> Unit,
    onReset: () -> Unit,
    onClickAdd: () -> Unit,
    cornerRadius: Float,
    shadowIntensity: Float,
    borderWidth: Float,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val currentOnSelected by rememberUpdatedState(onSelected)
    val currentOnTransform by rememberUpdatedState(onTransform)
    val currentOnReset by rememberUpdatedState(onReset)
    val currentOnClickAdd by rememberUpdatedState(onClickAdd)

    val frameShape = RoundedCornerShape(cornerRadius.dp)

    Box(
        modifier = modifier
            .pointerInput(state.index) {
                detectTapGestures(
                    onTap = { currentOnSelected() },
                    onDoubleTap = { currentOnReset() }
                )
            }
            .then(
                if (isSelected) {
                    Modifier.pointerInput(state.index) {
                        detectTransformGestures { _, pan, zoom, rotate ->
                            currentOnTransform(pan, zoom, rotate)
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Physical real-time drop shadows drawn precisely behind item clip boundaries
        if (shadowIntensity > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = (shadowIntensity * 12).dp, y = (shadowIntensity * 12).dp)
                    .clip(frameShape)
                    .background(Color.Black.copy(alpha = shadowIntensity))
            )
        }

        // Active Collage Frame Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(frameShape)
                .background(Color.LightGray.copy(alpha = 0.15f))
                .border(
                    width = if (isSelected) 3.dp else if (borderWidth > 0f) borderWidth.dp else 0.dp,
                    color = if (isSelected) AccentColor else if (borderWidth > 0f) borderColor else Color.Transparent,
                    shape = frameShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state.imageUri != null) {
                AsyncImage(
                    model = state.imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = state.scale
                            scaleY = state.scale
                            rotationZ = state.rotation
                            translationX = state.offsetX
                            translationY = state.offsetY
                        }
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clickable { currentOnClickAdd() }
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Image",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to Add",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun BackgroundPresetCell(
    option: BackgroundOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cellShape = CircleShape
    Box(
        modifier = Modifier
            .size(52.dp)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) AccentColor else Color.White.copy(alpha = 0.15f),
                shape = cellShape
            )
            .padding(4.dp)
            .clip(cellShape)
            .then(
                when (option) {
                    is BackgroundOption.Solid -> Modifier.background(option.color)
                    is BackgroundOption.Gradient -> Modifier.background(Brush.linearGradient(option.colors))
                }
            )
            .clickable { onClick() }
    )
}

@Composable
fun AspectRatioCell(
    aspectRatio: CollageAspectRatio,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(82.dp)
            .clip(shape)
            .background(if (isSelected) AccentColor.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.2f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AccentColor else Color.White.copy(alpha = 0.1f),
                shape = shape
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .aspectRatio(aspectRatio.value)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isSelected) AccentColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.3f))
                .border(1.dp, if (isSelected) AccentColor else Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = aspectRatio.name,
            color = if (isSelected) AccentColor else Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun BorderColorCell(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cellShape = CircleShape
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) AccentColor else Color.White.copy(alpha = 0.2f),
                shape = cellShape
            )
            .padding(3.dp)
            .clip(cellShape)
            .background(if (color == Color.Transparent) Color.Black else color)
            .clickable { onClick() }
    ) {
        if (color == Color.Transparent) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.Red,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun BottomEditorToolbar(
    onGalleryClick: () -> Unit,
    onResetClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                spotColor = Color(0xFF8B5CF6).copy(alpha = 0.25f)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF13102B).copy(alpha = 0.75f),
                        Color(0xFF0A0718).copy(alpha = 0.9f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButton(
            onClick = onGalleryClick,
            icon = Icons.Default.Image,
            label = "Gallery",
            isPrimary = false,
            modifier = Modifier.weight(1f)
        )
        ToolbarButton(
            onClick = onResetClick,
            icon = Icons.Default.Refresh,
            label = "Reset",
            isPrimary = false,
            modifier = Modifier.weight(1f)
        )
        ToolbarButton(
            onClick = onSaveClick,
            icon = Icons.Default.Save,
            label = "Save",
            isPrimary = true,
            modifier = Modifier.weight(1.1f)
        )
    }
}

@Composable
fun ToolbarButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )

    val backgroundBrush = if (isPrimary) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF8B5CF6), // Bright Violet
                Color(0xFFD946EF)  // Fuchsia Pink
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.03f)
            )
        )
    }

    val borderBrush = if (isPrimary) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.4f),
                Color.White.copy(alpha = 0.1f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isPrimary) {
                    Modifier.shadow(
                        elevation = if (isPressed) 6.dp else 12.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color(0xFF9333EA).copy(alpha = 0.4f),
                        spotColor = Color(0xFFEC4899).copy(alpha = 0.5f)
                    )
                } else {
                    Modifier
                }
            )
            .background(backgroundBrush, shape = RoundedCornerShape(18.dp))
            .border(1.dp, borderBrush, shape = RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(
                    color = if (isPrimary) Color.White else Color(0xFFC084FC)
                ),
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BouncyIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.82f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncyIconScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(
                    bounded = true,
                    color = Color(0xFFC084FC)
                ),
                onClick = onClick
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun BackgroundShuffleFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fabScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPressed) 6.dp else 12.dp,
                shape = CircleShape,
                ambientColor = Color(0xFFC084FC).copy(alpha = 0.4f),
                spotColor = Color(0xFFEC4899).copy(alpha = 0.6f)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6),
                        Color(0xFFD946EF),
                        Color(0xFF3B82F6)
                    )
                ),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.35f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = Color.White),
                onClick = onClick
            )
            .size(52.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ColorLens,
            contentDescription = "Shuffle Background",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

fun loadUriToBitmap(context: Context, uri: android.net.Uri, maxDimension: Int = 2048): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
            
            var scale = 1
            while (options.outWidth / scale / 2 >= maxDimension || options.outHeight / scale / 2 >= maxDimension) {
                scale *= 2
            }
            
            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            
            context.contentResolver.openInputStream(uri)?.use { reOpenStream ->
                android.graphics.BitmapFactory.decodeStream(reOpenStream, null, decodeOptions)
            }
        }
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}

suspend fun saveBitmapToDownloads(context: Context, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
    try {
        val fileName = "collage_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "image/png")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                true
            } else {
                false
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = java.io.File(downloadsDir, fileName)
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            true
        }
    } catch (t: Throwable) {
        t.printStackTrace()
        false
    }
}

@Preview(showBackground = true)
@Composable
fun CollageCanvasPreview() {
    MyApplicationTheme {
        CollageCanvas(layout = CollageLayout.Grid3)
    }
}
