package com.grimnej.lmcomment.workflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grimnej.lmcomment.config.Tone
import com.grimnej.lmcomment.crop.CropEditor
import com.grimnej.lmcomment.crop.NormalizedCropRect
import com.grimnej.lmcomment.relay.GenerationContractCodec
import com.grimnej.lmcomment.relay.GenerationOption
import com.grimnej.lmcomment.relay.RelayFailureCode
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
private val Success = Color(0xFF72E6A6)
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
    val onToneChange: (Tone) -> Unit,
    val onInstructionChange: (String) -> Unit,
    val onOptionCountChange: (Int) -> Unit,
    val onGenerate: () -> Unit,
    val onCancelGeneration: () -> Unit,
    val onSelectResult: (String) -> Unit,
    val onEditResult: (String) -> Unit,
    val onEditDraftChange: (String) -> Unit,
    val onSaveEdit: () -> Unit,
    val onCancelEdit: () -> Unit,
    val onCopyResult: (String) -> Unit,
    val onRegenerate: () -> Unit,
    val onBackToReview: () -> Unit,
    val onNewCapture: () -> Unit,
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
                is WorkflowState.Generating -> GeneratingScreen(state, actions)
                is WorkflowState.ShowingResults -> ResultsScreen(state, actions)
                is WorkflowState.EditingResult -> EditResultScreen(state, actions)
                is WorkflowState.GenerationError -> GenerationErrorScreen(state, actions)
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
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(
                eyebrow = if (state.manualEntry) "MANUAL CONTEXT" else "TEXT FOUND",
                title = if (state.manualEntry) "Start with the context" else "Shape the reply",
                onClose = actions.onClose,
                compact = isLandscape,
            )
            Spacer(Modifier.height(if (isLandscape) 10.dp else 16.dp))

            ContextStatusRow(
                manualEntry = state.manualEntry,
                blockCount = state.blocks.size,
                characterCount = state.text.length,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.text,
                onValueChange = {
                    actions.onReviewedTextChange(
                        it.take(GenerationContractCodec.MAX_SOURCE_CHARACTERS),
                    )
                },
                label = { Text("Context text") },
                placeholder = { Text("Type or paste the words you want help responding to") },
                supportingText = {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            if (state.text.isBlank()) {
                                "Add context before generating."
                            } else {
                                "Only this text is sent when you tap Generate."
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Text("${state.text.length}/${GenerationContractCodec.MAX_SOURCE_CHARACTERS}")
                    }
                },
                shape = CardShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (isLandscape) 145.dp else 200.dp)
                    .focusRequester(focusRequester),
            )
            Spacer(Modifier.height(22.dp))

            SectionHeading(
                step = "01",
                title = "Choose a voice",
                detail = "Set the feel of every option.",
            )
            Spacer(Modifier.height(11.dp))
            TonePicker(
                selected = state.tone,
                onSelected = actions.onToneChange,
            )

            Spacer(Modifier.height(22.dp))
            SectionHeading(
                step = "02",
                title = "Add direction",
                detail = "Optional — a detail, constraint, or point to include.",
            )
            Spacer(Modifier.height(11.dp))
            OutlinedTextField(
                value = state.instruction,
                onValueChange = {
                    actions.onInstructionChange(
                        it.take(GenerationContractCodec.MAX_INSTRUCTION_CHARACTERS),
                    )
                },
                label = { Text("Extra instruction (optional)") },
                placeholder = { Text("e.g. Mention that Friday afternoon works best") },
                supportingText = {
                    Text(
                        "${state.instruction.length}/${GenerationContractCodec.MAX_INSTRUCTION_CHARACTERS}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
                minLines = 2,
                maxLines = 4,
                shape = CardShape,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(22.dp))
            SectionHeading(
                step = "03",
                title = "How many options?",
                detail = "Keep it focused or compare a few approaches.",
            )
            Spacer(Modifier.height(11.dp))
            OptionCountSelector(
                selected = state.optionCount,
                onSelected = actions.onOptionCountChange,
            )

            if (!state.demoConfigured) {
                Spacer(Modifier.height(14.dp))
                ConfigurationNotice()
            }

            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                text = when (state.optionCount) {
                    1 -> "Generate 1 option"
                    else -> "Generate ${state.optionCount} options"
                },
                onClick = actions.onGenerate,
                enabled = state.text.isNotBlank() && state.demoConfigured,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            if (state.canReturnToCrop) {
                SecondaryButton(
                    text = "Back to crop",
                    onClick = actions.onBackToCrop,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ContextStatusRow(
    manualEntry: Boolean,
    blockCount: Int,
    characterCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Surface.copy(alpha = 0.94f))
            .border(1.dp, Outline, CardShape)
            .padding(horizontal = 15.dp, vertical = 12.dp),
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
                manualEntry -> "Your text is ready to shape"
                blockCount == 1 -> "1 text block found on-device"
                else -> "$blockCount text blocks found on-device"
            },
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$characterCount chars",
            color = TextMuted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SectionHeading(step: String, title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0x169B8CFF))
                .border(1.dp, Color(0x449B8CFF), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                step,
                color = Violet,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                detail,
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun TonePicker(selected: Tone, onSelected: (Tone) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Tone.entries.forEach { tone ->
            ToneChip(
                tone = tone,
                selected = tone == selected,
                onClick = { onSelected(tone) },
            )
        }
    }
}

@Composable
private fun ToneChip(tone: Tone, selected: Boolean, onClick: () -> Unit) {
    val label = when (tone) {
        Tone.NATURAL -> "Natural"
        Tone.PROFESSIONAL -> "Professional"
        Tone.FRIENDLY -> "Friendly"
        Tone.WITTY -> "Witty"
        Tone.CONCISE -> "Concise"
    }
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Violet.copy(alpha = 0.18f) else Surface,
            contentColor = if (selected) TextPrimary else TextSecondary,
        ),
        border = BorderStroke(
            1.dp,
            if (selected) Violet else Outline,
        ),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics { this.selected = selected },
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Cyan),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OptionCountSelector(selected: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .background(Surface)
            .border(1.dp, Outline, ControlShape)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        (1..3).forEach { count ->
            val isSelected = selected == count
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) Violet.copy(alpha = 0.2f) else Color.Transparent,
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Violet.copy(alpha = 0.72f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelected(count) },
                        role = Role.RadioButton,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$count",
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (count == 1) "option" else "options",
                        color = if (isSelected) Violet else TextMuted,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigurationNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .background(Warning.copy(alpha = 0.09f))
            .border(1.dp, Warning.copy(alpha = 0.36f), ControlShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "!",
            color = Warning,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Demo access is not configured",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Return to setup in LM-Comment, then reopen this workflow.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun GeneratingScreen(state: WorkflowState.Generating, actions: WorkflowActions) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        val isLandscape = maxWidth > maxHeight
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 720.dp)
                .fillMaxSize(),
        ) {
            ScreenHeader(
                eyebrow = "SECURE GENERATION",
                title = "Writing options…",
                onClose = actions.onClose,
                compact = isLandscape,
            )
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
                        .size(if (isLandscape) 88.dp else 106.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0x149B8CFF))
                        .border(1.dp, Color(0x559B8CFF), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(if (isLandscape) 42.dp else 48.dp),
                        color = Cyan,
                        trackColor = Outline,
                        strokeWidth = 3.dp,
                    )
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Violet),
                    )
                }
                Spacer(Modifier.height(if (isLandscape) 18.dp else 26.dp))
                Text(
                    "TURNING CONTEXT INTO LANGUAGE",
                    color = Cyan,
                    fontSize = 11.sp,
                    letterSpacing = 1.45.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Finding the right words",
                    color = TextPrimary,
                    fontSize = if (isLandscape) 25.sp else 30.sp,
                    lineHeight = if (isLandscape) 29.sp else 35.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    "This usually takes a few seconds. You can cancel safely at any time.",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 430.dp),
                )
                Spacer(Modifier.height(22.dp))
                DraftSummaryCard(
                    tone = state.draft.tone,
                    optionCount = state.draft.optionCount,
                    sourceLength = state.draft.sourceText.length,
                )
            }
            SecondaryButton(
                text = "Cancel generation",
                onClick = actions.onCancelGeneration,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DraftSummaryCard(tone: Tone, optionCount: Int, sourceLength: Int) {
    Row(
        modifier = Modifier
            .widthIn(max = 430.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(Surface.copy(alpha = 0.94f))
            .border(1.dp, Outline, CardShape)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Cyan.copy(alpha = 0.1f))
                .border(1.dp, Cyan.copy(alpha = 0.35f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("✦", color = Cyan, fontSize = 17.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${toneDisplayName(tone)} · $optionCount ${if (optionCount == 1) "option" else "options"}",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "$sourceLength characters of context",
                color = TextMuted,
                fontSize = 12.sp,
            )
        }
        PrivacyBadge(text = "IN FLIGHT")
    }
}

@Composable
private fun ResultsScreen(state: WorkflowState.ShowingResults, actions: WorkflowActions) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        val isLandscape = maxWidth > maxHeight
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 780.dp)
                .fillMaxSize(),
        ) {
            ScreenHeader(
                eyebrow = "${state.options.size} ${if (state.options.size == 1) "OPTION" else "OPTIONS"} READY",
                title = "Pick what feels like you",
                onClose = actions.onClose,
                compact = isLandscape,
            )
            Spacer(Modifier.height(if (isLandscape) 10.dp else 14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ControlShape)
                    .background(Cyan.copy(alpha = 0.07f))
                    .border(1.dp, Cyan.copy(alpha = 0.24f), ControlShape)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Success),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    "Tap a card to select it. Edit anything before copying.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.options.forEachIndexed { index, option ->
                    ResultCard(
                        option = option,
                        number = index + 1,
                        selected = state.selectedOptionId == option.id,
                        copied = state.copiedOptionId == option.id,
                        onSelect = { actions.onSelectResult(option.id) },
                        onEdit = { actions.onEditResult(option.id) },
                        onCopy = { actions.onCopyResult(option.id) },
                    )
                }
                Spacer(Modifier.height(2.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SecondaryButton(
                    text = "New capture",
                    onClick = actions.onNewCapture,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Regenerate",
                    onClick = actions.onRegenerate,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = actions.onBackToReview,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    "Back to text and settings",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ResultCard(
    option: GenerationOption,
    number: Int,
    selected: Boolean,
    copied: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
) {
    val cardOutline = when {
        copied -> Success.copy(alpha = 0.66f)
        selected -> Violet
        else -> Outline
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(
                when {
                    copied -> Success.copy(alpha = 0.055f)
                    selected -> Violet.copy(alpha = 0.075f)
                    else -> Surface.copy(alpha = 0.95f)
                },
            )
            .border(if (selected || copied) 1.5.dp else 1.dp, cardOutline, CardShape)
            .clickable(onClick = onSelect)
            .semantics { this.selected = selected }
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) Violet.copy(alpha = 0.2f) else GraphiteRaised)
                    .border(
                        1.dp,
                        if (selected) Violet.copy(alpha = 0.6f) else Outline,
                        RoundedCornerShape(11.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$number",
                    color = if (selected) Violet else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "OPTION ${number.toString().padStart(2, '0')}",
                color = TextMuted,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (copied) {
                StatusLabel(text = "COPIED", color = Success)
            } else if (selected) {
                StatusLabel(text = "SELECTED", color = Violet)
            }
        }
        Spacer(Modifier.height(13.dp))
        Text(
            option.text,
            color = TextPrimary,
            fontSize = 16.sp,
            lineHeight = 23.sp,
        )
        Spacer(Modifier.height(15.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ResultActionButton(
                text = "Edit",
                onClick = onEdit,
                modifier = Modifier.weight(1f),
            )
            ResultActionButton(
                text = if (copied) "Copied" else "Copy",
                onClick = onCopy,
                highlighted = copied,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatusLabel(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.38f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .semantics {
                if (text == "COPIED") liveRegion = LiveRegionMode.Polite
            },
    ) {
        Text(
            text,
            color = color,
            fontSize = 9.sp,
            letterSpacing = 0.9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ResultActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (highlighted) Success.copy(alpha = 0.1f) else Color.Transparent,
            contentColor = if (highlighted) Success else TextPrimary,
        ),
        border = BorderStroke(1.dp, if (highlighted) Success.copy(alpha = 0.5f) else Outline),
        shape = RoundedCornerShape(13.dp),
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EditResultScreen(state: WorkflowState.EditingResult, actions: WorkflowActions) {
    val optionNumber = state.results.options.indexOfFirst { it.id == state.optionId } + 1
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
        LaunchedEffect(state.optionId) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(
                eyebrow = if (optionNumber > 0) "EDITING OPTION ${optionNumber.toString().padStart(2, '0')}" else "EDITING RESULT",
                title = "Make every word yours",
                onClose = actions.onClose,
                compact = isLandscape,
            )
            Spacer(Modifier.height(if (isLandscape) 10.dp else 16.dp))
            Text(
                "The text below is exactly what Copy will use after you save.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.draftText,
                onValueChange = {
                    actions.onEditDraftChange(
                        it.take(GenerationContractCodec.MAX_OPTION_CHARACTERS),
                    )
                },
                label = { Text("Reply text") },
                placeholder = { Text("Write the reply you want to copy") },
                supportingText = {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            if (state.draftText.isBlank()) "Reply text cannot be empty." else "Changes stay in this workflow.",
                            modifier = Modifier.weight(1f),
                        )
                        Text("${state.draftText.length}/${GenerationContractCodec.MAX_OPTION_CHARACTERS}")
                    }
                },
                shape = CardShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (isLandscape) 165.dp else 310.dp)
                    .focusRequester(focusRequester),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = actions.onCancelEdit,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Save changes",
                    onClick = actions.onSaveEdit,
                    enabled = state.draftText.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GenerationErrorScreen(
    state: WorkflowState.GenerationError,
    actions: WorkflowActions,
) {
    RecoveryScreen(
        eyebrow = relayErrorEyebrow(state.code),
        title = "The words didn't arrive",
        message = state.message,
        accent = Warning,
        onClose = actions.onClose,
    ) {
        PrimaryButton(
            text = "Try generation again",
            onClick = actions.onRegenerate,
            enabled = state.draft.demoConfigured && state.draft.sourceText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        SecondaryButton(
            text = "Back to text and settings",
            onClick = actions.onBackToReview,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        TextButton(
            onClick = actions.onNewCapture,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text("Start a new capture", color = TextSecondary, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun toneDisplayName(tone: Tone): String = when (tone) {
    Tone.NATURAL -> "Natural"
    Tone.PROFESSIONAL -> "Professional"
    Tone.FRIENDLY -> "Friendly"
    Tone.WITTY -> "Witty"
    Tone.CONCISE -> "Concise"
}

private fun relayErrorEyebrow(code: RelayFailureCode): String = when (code) {
    RelayFailureCode.NETWORK_UNAVAILABLE -> "CHECK YOUR CONNECTION"
    RelayFailureCode.NETWORK_TIMEOUT,
    RelayFailureCode.PROVIDER_TIMEOUT -> "GENERATION TIMED OUT"
    RelayFailureCode.UNAUTHORIZED,
    RelayFailureCode.INVALID_CONFIGURATION -> "DEMO ACCESS NEEDED"
    RelayFailureCode.RATE_LIMITED,
    RelayFailureCode.DAILY_LIMIT_REACHED,
    RelayFailureCode.PROVIDER_RATE_LIMIT -> "DEMO IS BUSY"
    else -> "GENERATION PAUSED"
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
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Violet,
            contentColor = Graphite,
            disabledContainerColor = Outline.copy(alpha = 0.62f),
            disabledContentColor = TextMuted,
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
