package com.virtualwife.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.app.Activity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color
import android.graphics.Typeface

class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1B4332"))
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.splash_logo)
            layoutParams = LinearLayout.LayoutParams(240, 240).apply { gravity = Gravity.CENTER }
        }
        root.addView(logo)

        root.addView(TextView(this).apply {
            text = "AI 数字人导游"
            setTextColor(Color.WHITE)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 8)
        })

        root.addView(TextView(this).apply {
            text = "智能景区导览 · 沉浸式体验"
            setTextColor(Color.argb(180, 255, 255, 255))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        })

        root.addView(ProgressBar(this))

        setContentView(root)

        // 1.5秒后跳转（缩短等待时间）
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                startActivity(Intent(this, MainActivity::class.java))
            } catch (e: Exception) {
                // 如果跳转失败，直接启动
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }, 1500)
    }
}
