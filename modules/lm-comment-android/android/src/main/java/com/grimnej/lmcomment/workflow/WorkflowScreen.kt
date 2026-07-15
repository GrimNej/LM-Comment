package com.grimnej.lmcomment.workflow

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Graphite = Color(0xFF090B10)
private val Surface = Color(0xFF141822)
private val TextPrimary = Color(0xFFF5F7FB)
private val TextSecondary = Color(0xFFB7BFCE)
private val Violet = Color(0xFF8F83FF)
private val Cyan = Color(0xFF50D7C5)
private val Outline = Color(0xFF30384A)

@Composable
fun WorkflowScreen(state: WorkflowState, onClose: () -> Unit) {
    if (state is WorkflowState.CaptureCloak) return
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet,
            secondary = Cyan,
            background = Graphite,
            surface = Surface,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
        ),
    ) {
        when (state) {
            is WorkflowState.CaptureCloak -> Unit
            is WorkflowState.FrameReady -> FramePreview(state, onClose)
        }
    }
}

@Composable
private fun FramePreview(state: WorkflowState.FrameReady, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Graphite)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("FRAME ACQUIRED", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Choose the words", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x1F50D7C5))
                    .border(1.dp, Color(0x6650D7C5), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text("ON DEVICE", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(18.dp))
        Image(
            bitmap = state.bitmap.asImageBitmap(),
            contentDescription = "One approved captured frame",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .border(1.dp, Outline, RoundedCornerShape(20.dp)),
        )
        Spacer(Modifier.size(14.dp))
        Text(
            "The frame is in app memory only. Crop and on-device OCR are next.",
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.size(14.dp))
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Graphite),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { contentDescription = "Close capture workflow" },
        ) {
            Text("Close and return", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
