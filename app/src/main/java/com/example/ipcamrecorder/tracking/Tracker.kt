package com.example.ipcamrecorder.tracking

import android.graphics.Rect

/**
 * Simple frame-diff motion detector placeholder.
 * For production use ML Kit Object Detection and Tracking.
 */

class Tracker {
    fun detectMotion(previousFrame: ByteArray?, currentFrame: ByteArray?): Rect? {
        // implement lightweight frame difference to find bounding box of motion
        return null
    }
}
