package com.virtualwife.app.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtualwife.app.ui.theme.*

@Composable
fun ChatInputBar(
    onSendText: (String) -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceEnd: () -> Unit,
    onCameraClick: () -> Unit,
    isRecording: Boolean = false,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .navigationBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 相机按钮
            IconButton(
                onClick = onCameraClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF5F0F7))
            ) {
                Icon(
                    Icons.Outlined.CameraAlt, "拍照",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFB8A9E8)
                )
            }

            // 输入框
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "说点什么吧～",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCAC4D0)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD1DC),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            // 发送/语音按钮
            AnimatedContent(
                targetState = inputText.isNotBlank(),
                transitionSpec = {
                    scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                },
                label = "send_button"
            ) { hasText ->
                if (hasText) {
                    IconButton(
                        onClick = {
                            onSendText(inputText)
                            inputText = ""
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF8FAB),
                                        Color(0xFFFFA0C0)
                                    )
                                )
                            )
                    ) {
                        Icon(
                            Icons.Filled.Send, "发送",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isRecording) Color(0xFFFF6B8A)
                                else Color(0xFFE8DEFF)
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onVoiceStart()
                                        tryAwaitRelease()
                                        onVoiceEnd()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isRecording) Icons.Filled.Mic else Icons.Outlined.Mic,
                            "语音",
                            tint = if (isRecording) Color.White else Color(0xFFB8A9E8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
