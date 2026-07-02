package com.virtualwife.app.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.virtualwife.app.ui.auth.LoginScreen
import com.virtualwife.app.ui.chat.ChatScreen
import com.virtualwife.app.ui.image.ImageCaptureScreen
import com.virtualwife.app.ui.map.MapScreen
import com.virtualwife.app.ui.route.RouteScreen
import com.virtualwife.app.ui.settings.CharacterScreen
import com.virtualwife.app.ui.settings.SettingsScreen
import com.virtualwife.app.viewmodel.AuthViewModel
import com.virtualwife.app.viewmodel.ChatViewModel

object Routes {
    const val LOGIN = "login"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val CHARACTER = "character"
    const val ROUTE = "route"
    const val MAP = "map"
    const val IMAGE_CAPTURE = "image_capture"
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    // 在 NavGraph 级别创建 ChatViewModel，所有子页面共享同一实例
    val chatViewModel: ChatViewModel = viewModel()

    val startDestination = if (authViewModel.uiState.value.isLoggedIn) {
        Routes.CHAT
    } else {
        Routes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.CHAT) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CHAT) {
            ChatScreen(
                chatViewModel = chatViewModel,
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToRoute = { navController.navigate(Routes.ROUTE) },
                onNavigateToImageCapture = { navController.navigate(Routes.IMAGE_CAPTURE) },
                onNavigateToMap = { navController.navigate(Routes.MAP) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCharacter = { navController.navigate(Routes.CHARACTER) },
                onLogout = {
                    authViewModel.logout()
                    chatViewModel.disconnectWebSocket()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CHARACTER) {
            CharacterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ROUTE) {
            RouteScreen(
                onNavigateBack = { navController.popBackStack() },
                onRouteSelected = { route ->
                    android.util.Log.d("NavGraph", "Route selected: id=${route.id}, name=${route.routeName}")
                    chatViewModel.selectRoute(route.id, route.routeName)
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.MAP) {
            MapScreen(
                chatViewModel = chatViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.IMAGE_CAPTURE) {
            ImageCaptureScreen(
                chatViewModel = chatViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
