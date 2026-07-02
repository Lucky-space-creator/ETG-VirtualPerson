package com.virtualwife.app.ui.route

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.virtualwife.app.data.remote.dto.RouteDto
import com.virtualwife.app.data.remote.dto.SpotDto
import com.virtualwife.app.ui.components.ErrorDialog
import com.virtualwife.app.ui.components.LoadingIndicator
import com.virtualwife.app.ui.theme.*
import com.virtualwife.app.viewmodel.RouteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(
    onNavigateBack: () -> Unit,
    onRouteSelected: ((RouteDto) -> Unit)? = null,
    routeViewModel: RouteViewModel = viewModel()
) {
    val uiState by routeViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        routeViewModel.loadRoutes()
    }

    if (uiState.error != null) {
        ErrorDialog(
            message = uiState.error!!,
            onDismiss = { routeViewModel.clearError() }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "路线推荐",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = PrimaryLight
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { routeViewModel.loadRoutes() }) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "刷新",
                            tint = PrimaryLight
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 兴趣标签
            InterestTagRow(
                selectedTags = uiState.selectedTags,
                onTagToggle = { routeViewModel.toggleTag(it) }
            )

            when {
                uiState.isLoading -> {
                    LoadingIndicator(message = "加载路线中...")
                }
                uiState.selectedRoute != null -> {
                    RouteDetailCard(
                        route = uiState.selectedRoute!!,
                        spots = uiState.spots,
                        onBack = { routeViewModel.clearSelection() },
                        onSelect = { onRouteSelected?.invoke(uiState.selectedRoute!!) }
                    )
                }
                else -> {
                    if (uiState.routes.isEmpty()) {
                        EmptyRouteView()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.routes) { route ->
                                RouteCard(
                                    route = route,
                                    onClick = { routeViewModel.selectRoute(route) },
                                    onSelect = { onRouteSelected?.invoke(route) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InterestTagRow(
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit
) {
    val tags = listOf(
        "历史文化" to Icons.Outlined.HistoryEdu,
        "自然风光" to Icons.Outlined.Landscape,
        "美食" to Icons.Outlined.Restaurant,
        "拍照打卡" to Icons.Outlined.PhotoCamera
    )

    LazyRow(
        modifier = Modifier.padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tags) { (tag, icon) ->
            val isSelected = selectedTags.contains(tag)
            FilterChip(
                selected = isSelected,
                onClick = { onTagToggle(tag) },
                label = {
                    Text(
                        tag,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryLight,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteCard(
    route: RouteDto,
    onClick: () -> Unit,
    onSelect: (() -> Unit)? = null
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 路线名称
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryLight.copy(alpha = 0.1f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Route,
                            contentDescription = null,
                            tint = PrimaryLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.routeName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = PrimaryLight
                    )
                    if (!route.description.isNullOrEmpty()) {
                        Text(
                            text = route.description!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 路线信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (route.interestTags != null) {
                    InfoChip(
                        icon = Icons.Outlined.Tag,
                        text = route.interestTags!!
                    )
                }
                if (route.timeBudget != null) {
                    InfoChip(
                        icon = Icons.Outlined.Schedule,
                        text = "${route.timeBudget}分钟"
                    )
                }
            }

            // 选择路线按钮
            if (onSelect != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSelect,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8FAB)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Filled.Navigation, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("选择此路线", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SecondaryLight.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = SecondaryLight
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryLight
            )
        }
    }
}

@Composable
private fun EmptyRouteView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Explore,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = PrimaryLight.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "暂无路线数据",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "请先在管理后台添加路线",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun RouteDetailCard(
    route: RouteDto,
    spots: List<SpotDto>,
    onBack: () -> Unit,
    onSelect: (() -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("返回列表")
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryLight
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = route.routeName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    if (!route.description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = route.description!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // 选择路线按钮
        if (onSelect != null) {
            item {
                Button(
                    onClick = onSelect,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8FAB)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Filled.Navigation, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("选择此路线开始游览", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }

        item {
            Text(
                text = "景点列表",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(spots.sortedBy { it.spotOrder }) { spot ->
            SpotCard(spot = spot)
        }

        if (spots.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "该路线暂无景点数据",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotCard(spot: SpotDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 序号
            Card(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SecondaryLight.copy(alpha = 0.2f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${spot.spotOrder}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = SecondaryLight
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spot.spotName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (!spot.narrateText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = spot.narrateText!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
            }
        }
    }
}
