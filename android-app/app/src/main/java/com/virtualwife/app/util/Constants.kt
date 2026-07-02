package com.virtualwife.app.util

object Constants {
    // 服务器基础地址（开发环境使用本地，生产环境替换为域名）
    const val BASE_URL = "http://10.0.2.2:8080/api/admin/"
    const val ADMIN_BASE_URL = "http://10.0.2.2:8080/api/admin/"
    const val WS_URL = "ws://10.0.2.2:8080/api/admin/ws/"

    // WebSocket 重连参数
    const val WS_RECONNECT_BASE_DELAY = 1000L
    const val WS_RECONNECT_MAX_DELAY = 30000L
    const val WS_PING_INTERVAL = 30000L

    // 图片压缩参数
    const val IMAGE_MAX_WIDTH = 1024
    const val IMAGE_MAX_HEIGHT = 1024
    const val IMAGE_MAX_SIZE_KB = 500L
    const val IMAGE_QUALITY = 80

    // VRM WebView 进程名
    const val VRM_PROCESS_NAME = ":vrm"

    // Room 数据库
    const val DATABASE_NAME = "virtualwife_chat.db"
}
