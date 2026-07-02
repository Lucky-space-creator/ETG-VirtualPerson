package com.virtualwife.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.virtualwife.app.ui.components.ErrorDialog
import com.virtualwife.app.ui.theme.*
import com.virtualwife.app.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // 加载保存的凭据
    LaunchedEffect(uiState.savedUsername) {
        if (uiState.savedUsername.isNotEmpty() && username.isEmpty()) {
            username = uiState.savedUsername
            password = uiState.savedPassword
            rememberMe = uiState.rememberMe
        }
    }

    // 浮动动画
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    val floatRotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatRot"
    )

    LaunchedEffect(Unit) {
        authViewModel.loginSuccess.collect { onLoginSuccess() }
    }

    if (uiState.error != null) {
        ErrorDialog(
            message = uiState.error!!,
            onDismiss = { authViewModel.clearError() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFE))
    ) {
        // 柔和渐变背景装饰
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset((-80).dp, (-60).dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD1DC).copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(280.dp, 500.dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE8DEFF).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(100.dp, 100.dp)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD4E5FF).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ═══ 可爱图标区域 ═══
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = floatY
                        rotationZ = floatRotation
                    }
                    .size(120.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(36.dp),
                        ambientColor = PrimaryLight.copy(alpha = 0.15f),
                        spotColor = PrimaryLight.copy(alpha = 0.1f)
                    )
                    .clip(RoundedCornerShape(36.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFD1DC),
                                Color(0xFFE8DEFF),
                                Color(0xFFD4E5FF)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 可爱的山+云+太阳图标
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 太阳 ☀️
                    Text("☀️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    // 山+树 🏔️
                    Text("🏔️", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    // 小云朵
                    Text("☁️", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 标题
            Text(
                "AI 导游小助手",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF1C1B1F)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "陪你探索每一处风景 ✨",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF79747E)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ═══ 登录卡片 ═══
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 用户名
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("用户名") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFD1DC).copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Person, null,
                                    modifier = Modifier.size(18.dp),
                                    tint = PrimaryLight
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryLight,
                            unfocusedBorderColor = Color(0xFFE8E4EA),
                            focusedContainerColor = Color(0xFFFFFBFE),
                            unfocusedContainerColor = Color(0xFFF5F0F7)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 密码
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8DEFF).copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Lock, null,
                                    modifier = Modifier.size(18.dp),
                                    tint = SecondaryLight
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFB8A9E8)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                authViewModel.login(username, password, rememberMe)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecondaryLight,
                            unfocusedBorderColor = Color(0xFFE8E4EA),
                            focusedContainerColor = Color(0xFFFFFBFE),
                            unfocusedContainerColor = Color(0xFFF5F0F7)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 记住我
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PrimaryLight,
                                uncheckedColor = Color(0xFFE8E4EA)
                            ),
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "记住我",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF79747E)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 登录按钮
                    Button(
                        onClick = { authViewModel.login(username, password, rememberMe) },
                        enabled = !uiState.isLoading && username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryLight,
                            disabledContainerColor = PrimaryLight.copy(alpha = 0.4f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                Icons.Filled.Login, null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "登录",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 2.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 底部版本
            Text(
                "v1.0 · AI数字人导游",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCAC4D0)
            )
        }
    }
}
