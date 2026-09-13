package com.example.ui.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaActionSound
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class FeedbackManager(private val context: Context) {

    private val mediaActionSound: MediaActionSound by lazy {
        MediaActionSound().apply {
            try {
                load(MediaActionSound.SHUTTER_CLICK)
            } catch (_: Exception) {}
        }
    }

    private fun getVibrator(): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 撮影直後のフィードバック（シャッター音＋振動）
     */
    fun playShutterFeedback(hapticsEnabled: Boolean = true, soundEnabled: Boolean = true) {
        if (soundEnabled) {
            try {
                mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
            } catch (_: Exception) {
                try {
                    ToneGenerator(AudioManager.STREAM_SYSTEM, 70).startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                } catch (_: Exception) {}
            }
        }

        if (hapticsEnabled) {
            try {
                val vibrator = getVibrator()
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(50)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * 結果通知判定後のフィードバック（判定完了通知音＋2回振動）
     */
    fun playResultFeedback(hapticsEnabled: Boolean = true, soundEnabled: Boolean = true) {
        if (soundEnabled) {
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                if (ringtone != null) {
                    ringtone.play()
                } else {
                    ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).startTone(ToneGenerator.TONE_PROP_ACK, 200)
                }
            } catch (_: Exception) {
                try {
                    ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).startTone(ToneGenerator.TONE_PROP_ACK, 200)
                } catch (_: Exception) {}
            }
        }

        if (hapticsEnabled) {
            try {
                val vibrator = getVibrator()
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // 2回振動 (0ms待機, 70ms振動, 60ms休止, 90ms振動)
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 60, 90), -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 70, 60, 90), -1)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun release() {
        try {
            mediaActionSound.release()
        } catch (_: Exception) {}
    }
}
