package com.arcore.MixedAIAR

import android.app.Activity
import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Emits static bitmap to be collected
 * */
class BitmapSource(override var activity: Activity, override var imgFileName: String): StreamApi {
    lateinit var bitmapStream : Flow<Bitmap>
    var run = false
    fun toggleFlow() {
        run = !run
        startFlow()
    }

    fun startFlow() {
        bitmapStream = flow {
            while(run) {
                val bitmap = fetchStream(activity)
                emit(bitmap)
            }
        }
    }
}



