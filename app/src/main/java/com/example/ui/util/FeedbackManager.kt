package com.example.ui.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaActionSound
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

class FeedbackManager(private val context: Context) {

    private val mediaActionSound: MediaActionSound by lazy {
        MediaActionSound().apply {
            try {
                load(MediaActionSound.SHUTTER_CLICK)
            } catch (_: Exception) {}
        }
    }

    /**
     * 漫画風の「ボヨ〜ン！」というバネ・コミック風シャッターサウンドのAudioTrack
     */
    private var boyonAudioTrack: AudioTrack? = null

    init {
        initBoyonTrack()
    }

    private fun initBoyonTrack() {
        try {
            val sampleRate = 22050
            val durationMs = 450
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)

            var phase = 0.0
            val wobbleRate = 13.5 // バネの振動周波数 (13.5Hz)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate

                // 周波数変調: 基本周波数は260Hzから140Hzへ急速に降下し、13.5Hzのバネ揺れが重なる
                val fBase = 140.0 + 125.0 * exp(-4.2 * t)
                val fWobble = 70.0 * exp(-3.8 * t) * sin(2.0 * PI * wobbleRate * t)
                val freq = fBase + fWobble

                phase += 2.0 * PI * freq / sampleRate

                // 高調波合成（倍音成分を加えてバネの弾き音・コミックのボヨーン感を強調）
                val s1 = sin(phase)
                val s2 = 0.42 * sin(2.0 * phase)
                val s3 = 0.20 * sin(3.0 * phase)
                val s4 = 0.08 * sin(4.0 * phase)
                var wave = s1 + s2 + s3 + s4

                // ソフトサチュレーション（温かみのある弾力音）
                wave = tanh(1.25 * wave)

                // エンベロープ（アタック10ms、指数減衰＋バネ伸縮トレモロ）
                val attack = if (t < 0.010) (t / 0.010) else 1.0
                val decay = exp(-3.8 * t)
                val tremolo = 0.80 + 0.20 * cos(2.0 * PI * wobbleRate * t)
                val envelope = attack * decay * tremolo

                val amp = (wave * envelope * 0.88 * Short.MAX_VALUE).toInt()
                samples[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val bufferSize = samples.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            boyonAudioTrack = track
        } catch (_: Exception) {
            boyonAudioTrack = null
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
     * 撮影直後のフィードバック（ボヨ〜ン音＋バウンス振動）
     */
    fun playShutterFeedback(hapticsEnabled: Boolean = true, soundEnabled: Boolean = true) {
        if (soundEnabled) {
            var played = false
            try {
                val track = boyonAudioTrack
                if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                    track.stop()
                    track.reloadStaticData()
                    track.play()
                    played = true
                }
            } catch (_: Exception) {
                played = false
            }

            if (!played) {
                try {
                    mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
                } catch (_: Exception) {
                    try {
                        ToneGenerator(AudioManager.STREAM_SYSTEM, 70).startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    } catch (_: Exception) {}
                }
            }
        }

        if (hapticsEnabled) {
            try {
                val vibrator = getVibrator()
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // バネの弾み（ボヨ〜ン）に合わせた減衰バウンス振動
                        val timings = longArrayOf(0, 45, 35, 25, 30, 15)
                        val amplitudes = intArrayOf(0, 225, 0, 130, 0, 60)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 45, 35, 25, 30, 15), -1)
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
            boyonAudioTrack?.let {
                if (it.state == AudioTrack.STATE_INITIALIZED) {
                    it.stop()
                }
                it.release()
            }
            boyonAudioTrack = null
        } catch (_: Exception) {}

        try {
            mediaActionSound.release()
        } catch (_: Exception) {}
    }
}
