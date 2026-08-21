package io.github.task320.earthstep.core.common.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import io.github.task320.earthstep.R

/**
 * 短い効果音の再生(P6-2)。
 *
 * SoundPool は同時再生数が少なく生存期間もアプリ全体なので、
 * ViewModel を介さずアプリ起動時に一度だけ初期化するだけで足りる。
 * [init] は [io.github.task320.earthstep.EarthStepApplication.onCreate] から呼ぶ。
 */
object SoundEffects {

    enum class Sound(val rawRes: Int) {
        /** 通常のマイルストーン到達。 */
        MILESTONE(R.raw.se_milestone),

        /** 大台(地球一周・周内マーカー)。 */
        MAJOR(R.raw.se_major),

        /** ボタン操作。 */
        TAP(R.raw.se_tap),
    }

    private const val MAX_STREAMS = 4

    private var soundPool: SoundPool? = null
    private var soundIds: Map<Sound, Int> = emptyMap()

    fun init(context: Context) {
        if (soundPool != null) return

        val pool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        soundIds = Sound.entries.associateWith { sound -> pool.load(context, sound.rawRes, 1) }
        soundPool = pool
    }

    fun play(sound: Sound) {
        val id = soundIds[sound] ?: return
        soundPool?.play(id, VOLUME, VOLUME, 1, 0, 1f)
    }

    private const val VOLUME = 1f
}
