package com.grimnej.lmcomment.crop

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private data class CropVisuals(
    val backdrop: Color,
    val outsideSelection: Color,
    val selectionOutline: Color,
    val handleAccent: Color,
)

private val LightCropVisuals = CropVisuals(
    backdrop = Color(0xFF101411),
    outsideSelection = Color(0xB8101411),
    selectionOutline = Color(0xFFB9E84A),
    handleAccent = Color(0xFFE96D4C),
)

private val DarkCropVisuals = CropVisuals(
    backdrop = Color(0xFF05070B),
    outsideSelection = Color(0xB805070B),
    selectionOutline = Color(0xFF9B8CFF),
    handleAccent = Color(0xFF55E1D0),
)

/**
 * Touch-owning crop viewport for a frozen, in-memory frame. Reset, full-frame,
 * cancel, manual-entry, and extract actions deliberately remain host controls.
 */
@Composable
fun CropEditor(
    bitmap: Bitmap,
    selection: NormalizedCropRect,
    onSelectionChange: (NormalizedCropRect) -> Unit,
    modifier: Modifier = Modifier,
    minimumCropSizePixels: Int = 48,
    darkTheme: Boolean = false,
) {
    require(minimumCropSizePixels >= 1) { "Minimum crop size must be at least one pixel." }
    val visuals = if (darkTheme) DarkCropVisuals else LightCropVisuals
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val currentSelection by rememberUpdatedState(selection)
    val selectionChanged by rememberUpdatedState(onSelectionChange)
    val transform = remember(bitmap.width, bitmap.height, viewportSize) {
        viewportSize.takeIf { it.width > 0 && it.height > 0 }?.let {
            PreviewTransform.fit(
                frameWidth = bitmap.width,
                frameHeight = bitmap.height,
                previewWidth = it.width.toFloat(),
                previewHeight = it.height.toFloat(),
            )
        }
    }

    val hitRadius = 36.dp
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(visuals.backdrop)
            .onSizeChanged { viewportSize = it }
            .semantics {
                contentDescription =
                    "Crop preview. Drag the corners, or use the crop actions below."
                stateDescription =
                    "${(selection.width * 100).roundToInt()} percent wide by " +
                    "${(selection.height * 100).roundToInt()} percent tall"
                customActions = listOf(
                    CustomAccessibilityAction("Reset crop") {
                        selectionChanged(NormalizedCropRect.Suggested)
                        true
                    },
                    CustomAccessibilityAction("Use full screen") {
                        selectionChanged(NormalizedCropRect.FullFrame)
                        true
                    },
                )
            }
            .pointerInput(transform, minimumCropSizePixels) {
                val previewTransform = transform ?: return@pointerInput
                var activeHandle: CropHandle? = null
                detectDragGestures(
                    onDragStart = { position ->
                        activeHandle = CropGeometry.closestHandle(
                            selection = currentSelection,
                            previewPoint = CropPoint(position.x, position.y),
                            transform = previewTransform,
                            hitRadius = hitRadius.toPx(),
                        )
                    },
                    onDragCancel = { activeHandle = null },
                    onDragEnd = { activeHandle = null },
                    onDrag = { change, _ ->
                        val handle = activeHandle ?: return@detectDragGestures
                        change.consume()
                        selectionChanged(
                            CropGeometry.dragHandleInPreview(
                                selection = currentSelection,
                                handle = handle,
                                previewPoint = CropPoint(change.position.x, change.position.y),
                                transform = previewTransform,
                                minimumPixels = minimumCropSizePixels,
                            ),
                        )
                    },
                )
            },
    ) {
        val previewTransform = transform ?: return@Canvas
        val content = previewTransform.contentBounds
        val destinationOffset = IntOffset(content.left.roundToInt(), content.top.roundToInt())
        val destinationSize = IntSize(
            width = (content.right.roundToInt() - destinationOffset.x).coerceAtLeast(1),
            height = (content.bottom.roundToInt() - destinationOffset.y).coerceAtLeast(1),
        )
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = destinationOffset,
            dstSize = destinationSize,
            filterQuality = FilterQuality.Medium,
        )

        val crop = previewTransform.selectionBounds(selection)
        val outsidePath = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, size.width, size.height))
            addRect(Rect(crop.left, crop.top, crop.right, crop.bottom))
        }
        drawPath(outsidePath, visuals.outsideSelection)
        drawRect(
            color = visuals.selectionOutline,
            topLeft = Offset(crop.left, crop.top),
            size = androidx.compose.ui.geometry.Size(crop.width, crop.height),
            style = Stroke(width = 2.dp.toPx()),
        )

        val corners = CropHandle.entries.associateWith { handle ->
            previewTransform.imageToPreview(selection.corner(handle)).let { Offset(it.x, it.y) }
        }
        val cornerLength = 18.dp.toPx()
        val cornerStroke = 4.dp.toPx()
        corners.forEach { (handle, point) ->
            val horizontalDirection = when (handle) {
                CropHandle.TopLeft, CropHandle.BottomLeft -> 1f
                CropHandle.TopRight, CropHandle.BottomRight -> -1f
            }
            val verticalDirection = when (handle) {
                CropHandle.TopLeft, CropHandle.TopRight -> 1f
                CropHandle.BottomLeft, CropHandle.BottomRight -> -1f
            }
            drawLine(
                color = visuals.selectionOutline,
                start = point,
                end = point.copy(x = point.x + cornerLength * horizontalDirection),
                strokeWidth = cornerStroke,
            )
            drawLine(
                color = visuals.selectionOutline,
                start = point,
                end = point.copy(y = point.y + cornerLength * verticalDirection),
                strokeWidth = cornerStroke,
            )
            drawCircle(color = visuals.handleAccent, radius = 4.dp.toPx(), center = point)
        }
    }
}
