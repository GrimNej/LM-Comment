package com.grimnej.lmcomment.workflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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

private data class WorkflowVisuals(
    val isDark: Boolean,
    val paper: Color,
    val paperRaised: Color,
    val paperInset: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkMuted: Color,
    val primary: Color,
    val primaryWash: Color,
    val primaryInk: Color,
    val secondary: Color,
    val secondaryWash: Color,
    val secondaryInk: Color,
    val hairline: Color,
    val success: Color,
    val successWash: Color,
    val cardShape: RoundedCornerShape,
    val controlShape: RoundedCornerShape,
)

private val LightWorkflowVisuals = WorkflowVisuals(
    isDark = false,
    paper = Color(0xFFF4F0E6),
    paperRaised = Color(0xFFFFFDF7),
    paperInset = Color(0xFFFAF7EE),
    ink = Color(0xFF101411),
    inkSoft = Color(0xFF40473F),
    inkMuted = Color(0xFF646C63),
    primary = Color(0xFFB9E84A),
    primaryWash = Color(0xFFE5EBCF),
    primaryInk = Color(0xFF465D0B),
    secondary = Color(0xFFE96D4C),
    secondaryWash = Color(0xFFF3DDD4),
    secondaryInk = Color(0xFFA6422D),
    hairline = Color(0xFFB8B7AB),
    success = Color(0xFF2F6B3A),
    successWash = Color(0xFFDFEBDD),
    cardShape = RoundedCornerShape(12.dp),
    controlShape = RoundedCornerShape(8.dp),
)

private val DarkWorkflowVisuals = WorkflowVisuals(
    isDark = true,
    paper = Color(0xFF090B10),
    paperRaised = Color(0xFF151A25),
    paperInset = Color(0xFF0E121A),
    ink = Color(0xFFF7F8FC),
    inkSoft = Color(0xFFADB6C8),
    inkMuted = Color(0xFF7E899D),
    primary = Color(0xFF9B8CFF),
    primaryWash = Color(0xFF292646),
    primaryInk = Color(0xFFB6ACFF),
    secondary = Color(0xFF55E1D0),
    secondaryWash = Color(0xFF173B3B),
    secondaryInk = Color(0xFF77E9DC),
    hairline = Color(0xFF313A4E),
    success = Color(0xFF72E6A6),
    successWash = Color(0xFF16382D),
    cardShape = RoundedCornerShape(22.dp),
    controlShape = RoundedCornerShape(15.dp),
)

private val LocalWorkflowVisuals = staticCompositionLocalOf { LightWorkflowVisuals }

private val Paper: Color @Composable get() = LocalWorkflowVisuals.current.paper
private val PaperRaised: Color @Composable get() = LocalWorkflowVisuals.current.paperRaised
private val PaperInset: Color @Composable get() = LocalWorkflowVisuals.current.paperInset
private val Ink: Color @Composable get() = LocalWorkflowVisuals.current.ink
private val InkSoft: Color @Composable get() = LocalWorkflowVisuals.current.inkSoft
private val InkMuted: Color @Composable get() = LocalWorkflowVisuals.current.inkMuted
private val Lime: Color @Composable get() = LocalWorkflowVisuals.current.primary
private val LimeWash: Color @Composable get() = LocalWorkflowVisuals.current.primaryWash
private val LimeInk: Color @Composable get() = LocalWorkflowVisuals.current.primaryInk
private val Terracotta: Color @Composable get() = LocalWorkflowVisuals.current.secondary
private val TerracottaWash: Color @Composable get() = LocalWorkflowVisuals.current.secondaryWash
private val TerracottaInk: Color @Composable get() = LocalWorkflowVisuals.current.secondaryInk
private val Hairline: Color @Composable get() = LocalWorkflowVisuals.current.hairline
private val Success: Color @Composable get() = LocalWorkflowVisuals.current.success
private val SuccessWash: Color @Composable get() = LocalWorkflowVisuals.current.successWash
private val CardShape: RoundedCornerShape @Composable get() = LocalWorkflowVisuals.current.cardShape
private val ControlShape: RoundedCornerShape @Composable get() = LocalWorkflowVisuals.current.controlShape

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
fun WorkflowScreen(state: WorkflowState, actions: WorkflowActions, darkTheme: Boolean = false) {
    if (state is WorkflowState.CaptureCloak) return

    val visuals = if (darkTheme) DarkWorkflowVisuals else LightWorkflowVisuals
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = visuals.primary,
            secondary = visuals.secondary,
            background = visuals.paper,
            surface = visuals.paperRaised,
            surfaceVariant = visuals.paperInset,
            outline = visuals.hairline,
            error = Color(0xFFFF7182),
            onPrimary = visuals.paper,
            onSecondary = visuals.paper,
            onBackground = visuals.ink,
            onSurface = visuals.ink,
            onSurfaceVariant = visuals.inkSoft,
        )
    } else {
        lightColorScheme(
            primary = Ink,
            secondary = TerracottaInk,
            background = Paper,
            surface = PaperRaised,
            surfaceVariant = PaperInset,
            outline = Hairline,
            error = TerracottaInk,
            onPrimary = Paper,
            onSecondary = Paper,
            onBackground = Ink,
            onSurface = Ink,
            onSurfaceVariant = InkSoft,
        )
    }
    CompositionLocalProvider(LocalWorkflowVisuals provides visuals) {
        MaterialTheme(colorScheme = colorScheme) {
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
}

@Composable
private fun WorkflowBackdrop(content: @Composable () -> Unit) {
    val visuals = LocalWorkflowVisuals.current
    val background = if (visuals.isDark) {
        Modifier.background(
            Brush.linearGradient(
                colors = listOf(visuals.paper, Color(0xFF0B1018), visuals.paperInset),
            ),
        )
    } else {
        Modifier.background(visuals.paper)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(background),
    ) {
        if (visuals.isDark) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x269B8CFF), Color.Transparent),
                        ),
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Lime),
            )
        }
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
                title = "Select the text",
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
            .border(1.dp, InkSoft, CardShape),
    ) {
        CropEditor(
            bitmap = state.bitmap,
            selection = state.selection,
            onSelectionChange = actions.onSelectionChange,
            darkTheme = LocalWorkflowVisuals.current.isDark,
        )
        PrivacyBadge(
            text = "Kept in memory",
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
            .background(PaperRaised)
            .border(1.dp, Hairline, CardShape)
            .then(
                if (landscape) Modifier.verticalScroll(rememberScrollState()) else Modifier,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Column {
            Text(
                "Drag the corners",
                color = InkMuted,
                fontSize = 12.sp,
                letterSpacing = 0.2.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                selectionLabel(selection),
                color = Ink,
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
                Text("Type text instead", color = InkSoft, fontWeight = FontWeight.SemiBold)
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
                    .size(82.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaperRaised)
                    .border(1.dp, Hairline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(42.dp),
                    color = Terracotta,
                    trackColor = PaperInset,
                    strokeWidth = 3.dp,
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "Reading text on this device",
                color = TerracottaInk,
                fontSize = 11.sp,
                letterSpacing = 0.3.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                "Reading your selection",
                color = Ink,
                fontSize = 29.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                "The selected pixels stay on your phone.",
                color = InkSoft,
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
                title = if (state.manualEntry) "Add the text" else "Review the text",
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
                                "The screenshot is never sent. This text, the selected tone, and any instruction go to the relay when you tap Generate."
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Text("${state.text.length}/${GenerationContractCodec.MAX_SOURCE_CHARACTERS}")
                    }
                },
                shape = ControlShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (isLandscape) 145.dp else 200.dp)
                    .focusRequester(focusRequester),
            )
            Spacer(Modifier.height(22.dp))

            SectionHeading(
                step = "01",
                title = "Choose a tone",
                detail = "Sets the tone for each reply.",
            )
            Spacer(Modifier.height(11.dp))
            TonePicker(
                selected = state.tone,
                onSelected = actions.onToneChange,
            )

            Spacer(Modifier.height(22.dp))
            SectionHeading(
                step = "02",
                title = "Add a note",
                detail = "Optional: include a detail or constraint.",
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
                shape = ControlShape,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(22.dp))
            SectionHeading(
                step = "03",
                title = "Choose the number of replies",
                detail = "Generate one reply or compare up to three.",
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
            .background(PaperRaised)
            .border(1.dp, Hairline, CardShape)
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LimeInk),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            when {
                manualEntry -> "Your text is ready"
                blockCount == 1 -> "1 text block found on this device"
                else -> "$blockCount text blocks found on this device"
            },
            color = InkSoft,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$characterCount characters",
            color = InkMuted,
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
                .size(32.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Lime)
                .border(1.dp, Ink, RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                step,
                color = Ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                detail,
                color = InkMuted,
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
            containerColor = if (selected) Lime else PaperRaised,
            contentColor = if (selected) Ink else InkSoft,
        ),
        border = BorderStroke(
            1.dp,
            if (selected) Ink else Hairline,
        ),
        shape = ControlShape,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics { this.selected = selected },
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TerracottaInk),
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
            .background(PaperRaised)
            .border(1.dp, Hairline, ControlShape)
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
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (isSelected) Lime else Color.Transparent,
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Ink else Color.Transparent,
                        shape = RoundedCornerShape(5.dp),
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
                        color = if (isSelected) Ink else InkSoft,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (count == 1) "option" else "options",
                        color = if (isSelected) InkSoft else InkMuted,
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
            .background(TerracottaWash)
            .border(1.dp, Terracotta, ControlShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "!",
            color = TerracottaInk,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Demo access is not configured",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Return to setup in LM-Comment, then reopen this workflow.",
                color = InkSoft,
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
                title = "Writing replies…",
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
                        .size(if (isLandscape) 80.dp else 96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PaperRaised)
                        .border(1.dp, Hairline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(if (isLandscape) 42.dp else 48.dp),
                        color = Terracotta,
                        trackColor = PaperInset,
                        strokeWidth = 3.dp,
                    )
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(LimeInk),
                    )
                }
                Spacer(Modifier.height(if (isLandscape) 18.dp else 26.dp))
                Text(
                    "Generating replies",
                    color = TerracottaInk,
                    fontSize = 11.sp,
                    letterSpacing = 0.3.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Writing your replies",
                    color = Ink,
                    fontSize = if (isLandscape) 25.sp else 30.sp,
                    lineHeight = if (isLandscape) 29.sp else 35.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    "This usually takes a few seconds. Tap Cancel generation to stop.",
                    color = InkSoft,
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
            .background(PaperRaised)
            .border(1.dp, Hairline, CardShape)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(TerracottaWash)
                .border(1.dp, Terracotta, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("✦", color = TerracottaInk, fontSize = 17.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${toneDisplayName(tone)} · $optionCount ${if (optionCount == 1) "option" else "options"}",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "$sourceLength characters of context",
                color = InkMuted,
                fontSize = 12.sp,
            )
        }
        PrivacyBadge(text = "Text only")
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
                title = "Choose a reply",
                onClose = actions.onClose,
                compact = isLandscape,
            )
            Spacer(Modifier.height(if (isLandscape) 10.dp else 14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ControlShape)
                    .background(LimeWash)
                    .border(1.dp, LimeInk, ControlShape)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(LimeInk),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    "Tap a reply to select it. You can edit it before copying.",
                    color = InkSoft,
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
                    color = InkSoft,
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
        copied -> Success
        selected -> Ink
        else -> Hairline
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(
                when {
                    copied -> SuccessWash
                    selected -> LimeWash
                    else -> PaperRaised
                },
            )
            .border(1.dp, cardOutline, CardShape)
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
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (selected) Lime else PaperInset)
                    .border(
                        1.dp,
                        if (selected) Ink else Hairline,
                        RoundedCornerShape(5.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$number",
                    color = if (selected) Ink else InkSoft,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Reply $number",
                color = InkMuted,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (copied) {
                StatusLabel(text = "Copied", color = Success)
            } else if (selected) {
                StatusLabel(text = "Selected", color = LimeInk)
            }
        }
        Spacer(Modifier.height(13.dp))
        Text(
            option.text,
            color = Ink,
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
            .clip(RoundedCornerShape(5.dp))
            .background(PaperRaised)
            .border(1.dp, color, RoundedCornerShape(5.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .semantics {
                if (text == "Copied") liveRegion = LiveRegionMode.Polite
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
            containerColor = if (highlighted) SuccessWash else PaperRaised,
            contentColor = if (highlighted) Success else Ink,
        ),
        border = BorderStroke(1.dp, if (highlighted) Success else Hairline),
        shape = ControlShape,
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
                title = if (optionNumber > 0) "Edit reply $optionNumber" else "Edit the reply",
                onClose = actions.onClose,
                compact = isLandscape,
            )
            Spacer(Modifier.height(if (isLandscape) 10.dp else 16.dp))
            Text(
                "Copy uses the text shown here after you save.",
                color = InkSoft,
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
                shape = ControlShape,
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
        title = relayErrorTitle(state.code),
        message = state.message,
        accent = TerracottaInk,
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
            Text("Start a new capture", color = InkSoft, fontWeight = FontWeight.SemiBold)
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

private fun relayErrorTitle(code: RelayFailureCode): String = when (code) {
    RelayFailureCode.NETWORK_UNAVAILABLE -> "Check your connection"
    RelayFailureCode.NETWORK_TIMEOUT,
    RelayFailureCode.PROVIDER_TIMEOUT -> "Generation timed out"
    RelayFailureCode.UNAUTHORIZED,
    RelayFailureCode.INVALID_CONFIGURATION -> "Demo access needed"
    RelayFailureCode.RATE_LIMITED,
    RelayFailureCode.DAILY_LIMIT_REACHED,
    RelayFailureCode.PROVIDER_RATE_LIMIT -> "Demo is busy"
    else -> "Generation stopped"
}

@Composable
private fun EmptyRecognitionScreen(actions: WorkflowActions) {
    RecoveryScreen(
        title = "No text found",
        message = "Adjust the crop, use the full screen, or enter the text manually.",
        accent = TerracottaInk,
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
        title = "Text recognition stopped",
        message = state.message,
        accent = TerracottaInk,
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
                    .size(54.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(TerracottaWash)
                    .border(1.dp, Terracotta, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("!", color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                title,
                color = Ink,
                fontSize = 30.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                color = InkSoft,
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
        Text(
            title,
            color = Ink,
            fontSize = if (compact) 22.sp else 26.sp,
            lineHeight = if (compact) 25.sp else 30.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        Spacer(Modifier.width(10.dp))
        CloseButton(onClose)
    }
}

@Composable
private fun BrandMark() {
    val cornerColor = Ink
    val signalColor = Lime
    Canvas(modifier = Modifier.size(44.dp)) {
        val left = size.width * 0.18f
        val right = size.width * 0.82f
        val top = size.height * 0.18f
        val bottom = size.height * 0.82f
        val arm = size.width * 0.24f
        val stroke = 2.5.dp.toPx()

        fun cornerLine(start: Offset, end: Offset) {
            drawLine(cornerColor, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
        }

        cornerLine(Offset(left, top), Offset(left + arm, top))
        cornerLine(Offset(left, top), Offset(left, top + arm))
        cornerLine(Offset(right, top), Offset(right - arm, top))
        cornerLine(Offset(right, top), Offset(right, top + arm))
        cornerLine(Offset(left, bottom), Offset(left + arm, bottom))
        cornerLine(Offset(left, bottom), Offset(left, bottom - arm))
        cornerLine(Offset(right, bottom), Offset(right - arm, bottom))
        cornerLine(Offset(right, bottom), Offset(right, bottom - arm))
        drawCircle(signalColor, radius = size.width * 0.09f, center = center)
    }
}

@Composable
private fun PrivacyBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Ink)
            .border(1.dp, Lime, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            color = Paper,
            fontSize = 10.sp,
            letterSpacing = 0.2.sp,
            fontWeight = FontWeight.SemiBold,
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
            containerColor = Lime,
            contentColor = Ink,
            disabledContainerColor = PaperInset,
            disabledContentColor = InkMuted,
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
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = PaperRaised,
            contentColor = Ink,
        ),
        border = BorderStroke(1.dp, Hairline),
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
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = PaperRaised,
            contentColor = InkSoft,
        ),
        border = BorderStroke(1.dp, Hairline),
        shape = ControlShape,
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
