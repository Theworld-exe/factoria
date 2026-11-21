package com.factoria.core.designsystem

import android.media.AudioManager
import android.media.ToneGenerator
import java.util.concurrent.Executors

object FactoriaSoundManager {
    private val executor = Executors.newSingleThreadExecutor()
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (_: Exception) {
            // Non-fatal if audio system unavailable
        }
    }

    fun playClick() {
        executor.execute {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 20)
            } catch (_: Exception) {}
        }
    }

    fun playTabSwitch() {
        executor.execute {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 15)
            } catch (_: Exception) {}
        }
    }

    fun playSolveSuccess() {
        executor.execute {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 50)
                Thread.sleep(60)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 40)
            } catch (_: Exception) {}
        }
    }

    fun playToggle() {
        executor.execute {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 18)
            } catch (_: Exception) {}
        }
    }
}
