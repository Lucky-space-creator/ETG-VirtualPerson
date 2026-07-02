package com.virtualwife.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.format(pattern: String = "HH:mm"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(this)
}

fun Long.toDateString(pattern: String = "HH:mm"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}
