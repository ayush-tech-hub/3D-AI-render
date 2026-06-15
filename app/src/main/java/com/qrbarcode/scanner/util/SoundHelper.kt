package com.qrbarcode.scanner.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

object SoundHelper {
    private var toneGenerator: ToneGenerator? = null

    fun playBeep(context: Context) {
        try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                if (toneGenerator == null) {
                    toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                }
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
