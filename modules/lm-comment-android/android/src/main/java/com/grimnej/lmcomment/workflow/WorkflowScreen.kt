package com.grimnej.lmcomment.workflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grimnej.lmcomment.crop.CropEditor
import com.grimnej.lmcomment.crop.NormalizedCropRect
import kotlin.math.roundToInt

private val Graphite = Color(0xFF090B10)
private val GraphiteRaised = Color(0xFF0E121A)
private val Surface = Color(0xFF151A25)
private val TextPrimary = Color(0xFFF7F8FC)
private val TextSecondary = Color(0xFFADB6C8)
private val TextMuted = Color(0xFF7E899D)
private val Violet = Color(0xFF9B8CFF)
private val Cyan = Color(0xFF55E1D0)
private val Outline = Color(0xFF313A4E)
private val Warning = Color(0xFFFFC978)
private val CardShape = RoundedCornerShape(22.dp)
private val ControlShape = RoundedCornerShape(15.dp)

data class WorkflowActions(
    val onSelectionChange: (NormalizedCropRect) -> Unit,
    val onResetSelection: () -> Unit,
    val onUseFullFrame: () -> Unit,
    val onExtractText: () -> Unit,
    val onExtractFullFrame: () -> Unit,
    val onTypeText: () -> Unit,
    val onReviewedTextChange: (String) -> Unit,
    val onBackToCrop: () -> Unit,
    val onRetryOcr: () -> Unit,
    val onClose: () -> Unit,
)

@Composable
fun WorkflowScreen(state: WorkflowState, actions: WorkflowActions) {
    if (state is WorkflowState.CaptureCloak) return

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet,
            secondary = Cyan,
            background = Graphite,
            surface = Surface,
            onPrimary = Graphite,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
        ),
    ) {
        WorkflowBackdrop {
            when (state) {
                is WorkflowState.CaptureCloak -> Unit
                is WorkflowState.Cropping -> CropScreen(state, actions)
                is WorkflowState.RecognizingText -> RecognitionScreen(actions)
                is WorkflowState.ReviewingText -> {
                    if (state.emptyRecognition) {
                        EmptyRecognitionScreen(actions)
                    } else {
                        ReviewScreen(state, actions)
                    }
                }
                is WorkflowState.OcrError -> OcrErrorScreen(state, actions)
                is WorkflowState.Closing -> Unit
            }
        }
    }
}

@Composable
private fun WorkflowBackdrop(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Graphite, Color(0xFF0B1018), GraphiteRaised),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x1F9B8CFF), Color.Transparent),
                    ),
                ),
        )
        content()
    }
}

@Composable
private fun CropScreen(state: WorkflowState.Cropping, actions: WorkflowActions) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        val isLandscape = maxWidth > maxHeight
        val landscapeControlsWidth = minOf(300.dp, maxWidth * 0.38f)
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                eyebrow = "ON-DEVICE SELECTION",
                title = "Frame the context",
                onClose = actions.onClose,
                compact = isLandscape,
            )
            Spacer(Modifier.height(if (isLandscape) 10.dp else 16.dp))

            if (isLandscape) {
                Row(Modifier.fillMaxSize()) {
                    CropViewport(
                        state = state,
                        actions = actions,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    Spacer(Modifier.width(16.dp))
                    CropControls(
                        selection = state.selection,
                        actions = actions,
                        modifier = Modifier
                            .width(landscapeControlsWidth)
                            .fillMaxHeight(),
                        landscape = true,
                    )
                }
            } else {
                CropViewport(
                    state = state,
                    actions = actions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                Spacer(Modifier.height(14.dp))
                CropControls(
                    selection = state.selection,
                    actions = actions,
                    modifier = Modifier.fillMaxWidth(),
                    landscape = false,
                )
            }
        }
    }
}

@Composable
private fun CropViewport(
    state: WorkflowState.Cropping,
    actions: WorkflowActions,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .clip(CardShape)
            .background(Color.Black)
            .border(1.dp, Outline, CardShape),
    ) {
        CropEditor(
            bitmap = state.bitmap,
            selection = state.selection,
            onSelectionChange = actions.onSelectionChange,
        )
        PrivacyBadge(
            text = "MEMORY ONLY",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp),
        )
    }
}

@Composable
private fun CropControls(
    selection: NormalizedCropRect,
    actions: WorkflowActions,
    modifier: Modifier,
    landscape: Boolean,
) {
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(Surface.copy(alpha = 0.94f))
            .border(1.dp, Outline, CardShape)
            .then(
                if (landscape) Modifier.verticalScroll(rememberScrollState()) else Modifier,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Column {
            Text(
                "DRAG THE FOUR CORNERS",
                color = TextMuted,
                fontSize = 10.sp,
                letterSpacing = 1.3.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                selectionLabel(selection),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    text = "Reset",
                    onClick = actions.onResetSelection,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Full screen",
                    onClick = actions.onUseFullFrame,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Column {
            TextButton(
                onClick = actions.onTypeText,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Type text instead", color = TextSecondary, fontWeight = FontWeight.SemiBold)
            }
            PrimaryButton(
                text = "Extract text",
                onClick = actions.onExtractText,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RecognitionScreen(actions: WorkflowActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            CloseButton(actions.onClose)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x149B8CFF))
                    .border(1.dp, Color(0x559B8CFF), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(42.dp),
                    color = Cyan,
                    trackColor = Outline,
                    strokeWidth = 3.dp,
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "READING ON THIS DEVICE",
                color = Cyan,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                "Finding the words",
                color = TextPrimary,
                fontSize = 29.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                "The selected pixels stay on your phone.",
                color = TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 360.dp),
            )
        }
        SecondaryButton(
            text = "Back to crop",
            onClick = actions.onBackToCrop,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ReviewScreen(state: WorkflowState.ReviewingText, actions: WorkflowActions) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        val isLandscape = maxWidth > maxHeight
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        LaunchedEffect(state.manualEntry) {
            if (state.manualEntry) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .then(
                    if (isLandscape) {
                        Modifier.verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    },
                ),
        ) {
            ScreenHeader(
                eyebrow = if (state.manualEntry) "MANUAL CONTEXT" else "TEXT FOUND",
                title = if (state.manualEntry) "Write the context" else "Make it yours",
                onClose = actions.onClose,
                compact = isLandscape,
            )
            Spacer(Modifier.height(if (isLandscape) 10.dp else 16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(Surface.copy(alpha = 0.94f))
                    .border(1.dp, Outline, CardShape)
                    .padding(horizontal = 15.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Cyan),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    when {
                        state.manualEntry -> "Ready for your own text"
                        state.blocks.size == 1 -> "1 text block detected"
                        else -> "${state.blocks.size} text blocks detected"
                    },
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${state.text.length} chars",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.text,
                onValueChange = actions.onReviewedTextChange,
                label = { Text("Context text") },
                placeholder = { Text("Type or paste the words you want help responding to") },
                supportingText = { Text("Editable now, and again before you copy a result.") },
                shape = CardShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isLandscape) {
                            Modifier.heightIn(min = 160.dp)
                        } else {
                            Modifier
                                .weight(1f)
                                .heightIn(min = 220.dp)
                        },
                    )
                    .focusRequester(focusRequester),
            )
            Spacer(Modifier.height(12.dp))
            if (state.canReturnToCrop) {
                SecondaryButton(
                    text = "Back to crop",
                    onClick = actions.onBackToCrop,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PrimaryButton(
                    text = "Done for now",
                    onClick = actions.onClose,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EmptyRecognitionScreen(actions: WorkflowActions) {
    RecoveryScreen(
        eyebrow = "NOTHING READABLE YET",
        title = "Let's try another angle",
        message = "No readable text was found. Adjust the crop or enter the text manually.",
        accent = Warning,
        onClose = actions.onClose,
    ) {
        SecondaryButton(
            text = "Back to crop",
            onClick = actions.onBackToCrop,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        SecondaryButton(
            text = "Use full screen",
            onClick = actions.onExtractFullFrame,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        PrimaryButton(
            text = "Type text",
            onClick = actions.onTypeText,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OcrErrorScreen(state: WorkflowState.OcrError, actions: WorkflowActions) {
    RecoveryScreen(
        eyebrow = "ON-DEVICE OCR PAUSED",
        title = "That read didn't finish",
        message = state.message,
        accent = Warning,
        onClose = actions.onClose,
    ) {
        PrimaryButton(
            text = "Try again",
            onClick = actions.onRetryOcr,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                text = "Back to crop",
                onClick = actions.onBackToCrop,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "Type text",
                onClick = actions.onTypeText,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RecoveryScreen(
    eyebrow: String,
    title: String,
    message: String,
    accent: Color,
    onClose: () -> Unit,
    actions: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            CloseButton(onClose)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(19.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("!", color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                eyebrow,
                color = accent,
                fontSize = 11.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                color = TextPrimary,
                fontSize = 30.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                color = TextSecondary,
                fontSize = 16.sp,
                lineHeight = 23.sp,
            )
            Spacer(Modifier.height(24.dp))
            actions()
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ScreenHeader(
    eyebrow: String,
    title: String,
    onClose: () -> Unit,
    compact: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark()
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                eyebrow,
                color = Cyan,
                fontSize = 11.sp,
                letterSpacing = 1.35.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                title,
                color = TextPrimary,
                fontSize = if (compact) 22.sp else 26.sp,
                lineHeight = if (compact) 25.sp else 30.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        Spacer(Modifier.width(10.dp))
        CloseButton(onClose)
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0x129B8CFF))
            .border(1.5.dp, Violet, RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Cyan),
        )
    }
}

@Composable
private fun PrivacyBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xD9121720))
            .border(1.dp, Color(0x6655E1D0), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            color = Cyan,
            fontSize = 10.sp,
            letterSpacing = 1.1.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Violet,
            contentColor = Graphite,
        ),
        shape = ControlShape,
        modifier = modifier.heightIn(min = 54.dp),
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        border = BorderStroke(1.dp, Outline),
        shape = ControlShape,
        modifier = modifier.heightIn(min = 50.dp),
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
        border = BorderStroke(1.dp, Outline),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Text("Close", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun selectionLabel(selection: NormalizedCropRect): String {
    if (selection == NormalizedCropRect.FullFrame) return "Full frame selected"
    val width = (selection.width * 100).roundToInt()
    val height = (selection.height * 100).roundToInt()
    return "$width% wide · $height% tall"
}
