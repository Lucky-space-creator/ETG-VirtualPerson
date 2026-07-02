package com.virtualwife.app.ui.map

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.virtualwife.app.viewmodel.ChatViewModel

/**
 * 地图游览页面
 *
 * 功能：
 * - 高德地图全屏展示
 * - 实时GPS定位蓝点
 * - 景点标记（已访问=绿色，未访问=灰色，当前=红色）
 * - 路线连线
 * - 底部进度卡片
 */
@Composable
fun MapScreen(
    chatViewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 位置相关状态
    var userLat by remember { mutableStateOf(0.0) }
    var userLng by remember { mutableStateOf(0.0) }
    var aMapRef by remember { mutableStateOf<AMap?>(null) }
    var locationClient by remember { mutableStateOf<AMapLocationClient?>(null) }

    val spots = uiState.tourSpots
    val visitedSpots = uiState.visitedSpots
    val currentIndex = uiState.currentSpotIndex

    // 定位监听
    LaunchedEffect(Unit) {
        try {
            Log.d("MapScreen", "Initializing AMap location client...")
            val client = AMapLocationClient(context)
            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                interval = 3000L
                isNeedAddress = false
            }
            client.setLocationOption(option)
            client.setLocationListener { location ->
                if (location != null && location.errorCode == 0) {
                    userLat = location.latitude
                    userLng = location.longitude
                    Log.d("MapScreen", "Location: $userLat, $userLng")
                    chatViewModel.onLocationUpdate(userLat, userLng)
                } else {
                    Log.e("MapScreen", "Location error: ${location?.errorCode}, ${location?.errorInfo}")
                }
            }
            client.startLocation()
            locationClient = client
            Log.d("MapScreen", "Location client started")
        } catch (e: Exception) {
            Log.e("MapScreen", "Location init failed: ${e.message}", e)
        }
    }

    // 清理
    DisposableEffect(Unit) {
        onDispose {
            locationClient?.stopLocation()
            locationClient?.onDestroy()
        }
    }

    var mapError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 地图
        AndroidView(
            factory = { ctx ->
                try {
                    Log.d("MapScreen", "Creating MapView...")
                    MapView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        onCreate(null)
                        val map = map
                        aMapRef = map

                        map.uiSettings.isZoomControlsEnabled = false
                        map.uiSettings.isMyLocationButtonEnabled = false
                        map.moveCamera(CameraUpdateFactory.zoomTo(15f))
                        Log.d("MapScreen", "MapView created successfully")
                    }
                } catch (e: Exception) {
                    Log.e("MapScreen", "MapView creation failed: ${e.message}", e)
                    mapError = e.message
                    // 返回空FrameLayout作为占位
                    android.widget.FrameLayout(ctx)
                }
            },
            update = { mapView ->
                val map = aMapRef ?: return@AndroidView

                // 更新用户位置蓝点
                if (userLat != 0.0 && userLng != 0.0) {
                    try {
                        val locationOverlay = map.myLocationStyle
                        if (locationOverlay == null) {
                            val style = com.amap.api.maps.model.MyLocationStyle()
                            style.myLocationType(com.amap.api.maps.model.MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE)
                            style.interval(3000L)
                            map.myLocationStyle = style
                            map.isMyLocationEnabled = true
                        }
                    } catch (_: Exception) {}
                }

                // 更新景点标记和路线
                if (spots.isNotEmpty()) {
                    map.clear()

                    // 绘制路线连线
                    if (spots.size >= 2) {
                        val polylineOptions = PolylineOptions()
                        spots.sortedBy { it.spotOrder }.forEach { spot ->
                            polylineOptions.add(LatLng(spot.latitude, spot.longitude))
                        }
                        polylineOptions.width(8f)
                        polylineOptions.color(android.graphics.Color.parseColor("#4FC3F7"))
                        polylineOptions.setDottedLine(true)
                        map.addPolyline(polylineOptions)
                    }

                    // 绘制景点标记
                    spots.forEachIndexed { index, spot ->
                        val isVisited = visitedSpots.contains(index)
                        val isCurrent = index == currentIndex

                        val markerColor = when {
                            isCurrent -> BitmapDescriptorFactory.HUE_RED
                            isVisited -> BitmapDescriptorFactory.HUE_GREEN
                            else -> BitmapDescriptorFactory.HUE_AZURE
                        }

                        val markerOptions = MarkerOptions()
                            .position(LatLng(spot.latitude, spot.longitude))
                            .title(spot.name)
                            .snippet("景点 ${spot.spotOrder}")
                            .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                            .anchor(0.5f, 1.0f)

                        map.addMarker(markerOptions)
                    }

                    // 首次加载时调整视野包含所有景点
                    val boundsBuilder = LatLngBounds.Builder()
                    spots.forEach { spot ->
                        boundsBuilder.include(LatLng(spot.latitude, spot.longitude))
                    }
                    if (userLat != 0.0) {
                        boundsBuilder.include(LatLng(userLat, userLng))
                    }
                    try {
                        val bounds = boundsBuilder.build()
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(bounds, 100)
                        )
                    } catch (_: Exception) {}
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 地图加载失败时显示景点列表
        if (mapError != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Map, null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("地图加载失败",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray)
                    Text(mapError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    // 显示景点列表作为降级
                    spots.forEach { spot ->
                        val isVisited = visitedSpots.contains(spot.spotOrder - 1)
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isVisited) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                null,
                                tint = if (isVisited) Color(0xFF4CAF50) else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${spot.spotOrder}. ${spot.name}")
                        }
                    }
                }
            }
        }

        // 顶部返回栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f))
            ) {
                Icon(Icons.Filled.ArrowBack, "返回")
            }
            Spacer(modifier = Modifier.weight(1f))
            // 定位按钮
            IconButton(
                onClick = {
                    if (userLat != 0.0) {
                        aMapRef?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(LatLng(userLat, userLng), 16f)
                        )
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f))
            ) {
                Icon(Icons.Filled.MyLocation, "定位")
            }
        }

        // 底部进度卡片
        if (spots.isNotEmpty()) {
            TourProgressCard(
                spots = spots,
                visitedSpots = visitedSpots,
                currentIndex = currentIndex,
                userLat = userLat,
                userLng = userLng,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * 底部游览进度卡片
 */
@Composable
private fun TourProgressCard(
    spots: List<com.virtualwife.app.viewmodel.TourSpot>,
    visitedSpots: Set<Int>,
    currentIndex: Int,
    userLat: Double,
    userLng: Double,
    modifier: Modifier = Modifier
) {
    val currentSpot = if (currentIndex in spots.indices) spots[currentIndex] else null
    val nextSpotIndex = spots.indices.firstOrNull { !visitedSpots.contains(it) && it != currentIndex }
    val nextSpot = if (nextSpotIndex != null) spots[nextSpotIndex] else null

    // 计算到下一景点距离
    val distanceToNext = if (nextSpot != null && userLat != 0.0) {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            userLat, userLng,
            nextSpot.latitude, nextSpot.longitude,
            results
        )
        results[0]
    } else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 进度条
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "游览进度",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${visitedSpots.size}/${spots.size} 景点",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = visitedSpots.size.toFloat() / spots.size.coerceAtLeast(1).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 当前景点
            if (currentSpot != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn, null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "当前位置：${currentSpot.name}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // 下一景点 + 距离
            if (nextSpot != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Flag, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "下一景点：${nextSpot.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                    if (distanceToNext != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        val distText = if (distanceToNext >= 1000) {
                            String.format("%.1fkm", distanceToNext / 1000)
                        } else {
                            String.format("%.0fm", distanceToNext)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                distText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 景点列表进度点
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                spots.forEachIndexed { index, spot ->
                    val isVisited = visitedSpots.contains(index)
                    val isCurrent = index == currentIndex
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCurrent -> MaterialTheme.colorScheme.error
                                        isVisited -> MaterialTheme.colorScheme.primary
                                        else -> androidx.compose.ui.graphics.Color.LightGray
                                    }
                                )
                        )
                        Text(
                            "${spot.spotOrder}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isVisited) MaterialTheme.colorScheme.primary
                            else androidx.compose.ui.graphics.Color.Gray
                        )
                    }
                }
            }
        }
    }
}
