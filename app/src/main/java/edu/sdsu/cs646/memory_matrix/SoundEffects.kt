package edu.sdsu.cs646.memory_matrix

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

// Singleton class for playing different sound effects
class SoundEffects private constructor(context: Context) {
    private var soundPool: SoundPool? = null
    private var advanceSoundId: Int? = null
    private var winSoundId: Int? = null
    private var loseSoundId: Int? = null

    companion object {
        private var instance: SoundEffects? = null

        fun getInstance(context: Context): SoundEffects {
            if (instance == null) {
                instance = SoundEffects(context)
            }
            return instance!!
        }
    }

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(attributes)
            .build()

        advanceSoundId = soundPool?.load(context, R.raw.advance, 1)
        winSoundId = soundPool?.load(context, R.raw.win, 1)
        loseSoundId = soundPool?.load(context, R.raw.lose, 1)
    }

    private fun playAudio(soundID: Int?) {
        soundID?.let { soundPool?.play(it, 1f, 1f, 1, 0, 1f) }
    }

    fun advance() {
        playAudio(advanceSoundId)
    }

    fun win() {
        playAudio(winSoundId)
    }

    fun lose() {
        playAudio(loseSoundId)
    }

}