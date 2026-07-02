package com.virtualwife.app.bridge

import android.webkit.JavascriptInterface

class VrmWebBridge {

    private var onModelLoadedListener: ((String) -> Unit)? = null
    private var onClickListener: ((String) -> Unit)? = null
    private var onSnapshotReadyListener: ((String) -> Unit)? = null

    fun setOnModelLoadedListener(listener: (String) -> Unit) {
        onModelLoadedListener = listener
    }

    fun setOnClickListener(listener: (String) -> Unit) {
        onClickListener = listener
    }

    fun setOnSnapshotReadyListener(listener: (String) -> Unit) {
        onSnapshotReadyListener = listener
    }

    @JavascriptInterface
    fun onVrmLoaded(modelName: String) {
        onModelLoadedListener?.invoke(modelName)
    }

    @JavascriptInterface
    fun onVrmClick(expression: String) {
        onClickListener?.invoke(expression)
    }

    @JavascriptInterface
    fun onSnapshotReady(base64: String) {
        onSnapshotReadyListener?.invoke(base64)
    }

    fun buildJsBridgeCode(): String {
        return """
            window.AndroidBridge = {
                onVrmLoaded: function(name) {
                    if (window.Android && window.Android.onVrmLoaded) {
                        window.Android.onVrmLoaded(name);
                    }
                },
                onVrmClick: function(expr) {
                    if (window.Android && window.Android.onVrmClick) {
                        window.Android.onVrmClick(expr);
                    }
                },
                onSnapshotReady: function(b64) {
                    if (window.Android && window.Android.onSnapshotReady) {
                        window.Android.onSnapshotReady(b64);
                    }
                }
            };
        """.trimIndent()
    }

    companion object {
        fun kotlinToJs(expression: String?): String {
            return "setExpression('${expression ?: "neutral"}')"
        }

        fun kotlinToJs(action: String?, isAction: Boolean): String {
            return if (isAction && action != null) {
                "playAction('$action')"
            } else ""
        }

        fun setLipSyncJs(value: Float): String {
            return "setLipSync(${value.coerceIn(0f, 1f)})"
        }

        fun loadModelJs(url: String): String {
            return "loadModel('$url')"
        }

        fun triggerBlinkJs(): String {
            return "triggerBlink()"
        }
    }
}
