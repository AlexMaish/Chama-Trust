package com.example.chamabuddy.util

import android.util.Log

object SyncLogger {
    private const val TAG = "ChamaSync"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String, exception: Throwable? = null) {
        if (exception != null) {
            Log.e(TAG, message, exception)
        } else {
            Log.e(TAG, message)
        }
    }
}