package com.virtualwife.app

import android.app.Application
import com.virtualwife.app.data.local.ChatDatabase
import com.virtualwife.app.data.local.PreferencesManager
import com.virtualwife.app.data.remote.RetrofitClient
import com.virtualwife.app.preload.ResourceManager

class MainApplication : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var chatDatabase: ChatDatabase
        private set

    lateinit var resourceManager: ResourceManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferencesManager = PreferencesManager(this)
        chatDatabase = ChatDatabase.getInstance(this)

        // 初始化 RetrofitClient
        RetrofitClient.initAdminApi(preferencesManager)

        // 立即开始预加载资源（数字人配置、路线等）
        resourceManager = ResourceManager(preferencesManager)
        resourceManager.preload()
    }

    companion object {
        lateinit var instance: MainApplication
            private set
    }
}
