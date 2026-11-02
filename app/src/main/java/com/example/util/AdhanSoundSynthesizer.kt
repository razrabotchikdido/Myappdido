package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.MuezzinVoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

object AdhanSoundSynthesizer {

    private var activeTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays the Adhan or Takbir tone based on selected Muezzin style and volume.
     */
    fun playAdhan(
        context: Context,
        voice: MuezzinVoice,
        volume: Float,
        onComplete: (() -> Unit)? = null
    ) {
        stop()

        playbackJob = scope.launch {
            try {
                val sampleRate = 22050
                // Melody notes: Frequencies in Hz for Takbir "Allahu Akbar, Allahu Akbar"
                val notes = getNotesForVoice(voice)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(sampleRate * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                activeTrack = audioTrack
                audioTrack.setVolume(volume.coerceIn(0f, 1f))
                audioTrack.play()

                // Generate and stream synthesized harmonic waveform for each phrase note
                for ((freq, durationMs, style) in notes) {
                    if (!isActive) break

                    val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                    val pcmData = ShortArray(totalSamples)

                    val attackSamples = (sampleRate * 0.08).toInt()
                    val releaseSamples = (sampleRate * 0.15).toInt()

                    for (i in 0 until totalSamples) {
                        val t = i.toDouble() / sampleRate
                        // Vocal formant vibrato
                        val vibrato = 1.0 + 0.015 * sin(2 * PI * 5.2 * t)
                        val f = freq * vibrato

                        // Multi-harmonic overtone synthesis (gives human-like warmth)
                        val fundamental = sin(2 * PI * f * t)
                        val harmonic2 = 0.45 * sin(2 * PI * (f * 2.0) * t)
                        val harmonic3 = 0.22 * sin(2 * PI * (f * 3.0) * t)
                        val harmonic4 = 0.12 * sin(2 * PI * (f * 4.0) * t)

                        val rawSample = (fundamental + harmonic2 + harmonic3 + harmonic4) / 1.8

                        // ADSR Envelope
                        val envelope = when {
                            i < attackSamples -> i.toDouble() / attackSamples
                            i > totalSamples - releaseSamples -> (totalSamples - i).toDouble() / releaseSamples
                            else -> 1.0
                        }

                        val sampleValue = (rawSample * envelope * 28000).toInt().coerceIn(-32768, 32767)
                        pcmData[i] = sampleValue.toShort()
                    }

                    audioTrack.write(pcmData, 0, pcmData.size)
                }

                audioTrack.stop()
                audioTrack.release()
                activeTrack = null
                onComplete?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isPlaying(): Boolean = activeTrack != null

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            activeTrack?.stop()
            activeTrack?.release()
        } catch (e: Exception) {
            // ignore
        }
        activeTrack = null
    }

    private data class NoteSpec(val freq: Double, val durationMs: Int, val style: String = "vocal")

    private fun getNotesForVoice(voice: MuezzinVoice): List<NoteSpec> = when (voice) {
        MuezzinVoice.MECCA -> listOf(
            // "Allahu Akbar" (Mecca Bayati solemn cadence)
            NoteSpec(261.63, 600), // C4: Al-
            NoteSpec(293.66, 750), // D4: la-
            NoteSpec(329.63, 1100), // E4: hu
            NoteSpec(293.66, 650), // D4: Ak-
            NoteSpec(261.63, 1300), // C4: bar...
            NoteSpec(0.0, 300),    // pause
            NoteSpec(261.63, 600), // Al-
            NoteSpec(329.63, 800), // la-
            NoteSpec(392.00, 1400), // hu (G4 high lift)
            NoteSpec(349.23, 700), // F4: Ak-
            NoteSpec(329.63, 1200), // E4: bar...
            NoteSpec(293.66, 900), // D4
            NoteSpec(261.63, 1600)  // C4
        )
        MuezzinVoice.MEDINA -> listOf(
            // Medina grand majestic phrasing
            NoteSpec(220.00, 700), // A3
            NoteSpec(261.63, 850), // C4
            NoteSpec(293.66, 1200), // D4
            NoteSpec(261.63, 700), // C4
            NoteSpec(220.00, 1400), // A3
            NoteSpec(0.0, 300),
            NoteSpec(293.66, 800), // D4
            NoteSpec(349.23, 1300), // F4
            NoteSpec(329.63, 800), // E4
            NoteSpec(293.66, 1400) // D4
        )
        MuezzinVoice.AL_AQSA -> listOf(
            // Al-Aqsa traditional Palestine melodic inflection
            NoteSpec(246.94, 600), // B3
            NoteSpec(277.18, 700), // C#4
            NoteSpec(329.63, 1100), // E4
            NoteSpec(370.00, 1300), // F#4
            NoteSpec(329.63, 700), // E4
            NoteSpec(293.66, 1200), // D4
            NoteSpec(0.0, 300),
            NoteSpec(277.18, 650),
            NoteSpec(246.94, 1500)
        )
        MuezzinVoice.ALAFASY -> listOf(
            // Mishary Rashid Alafasy - melodic & lyrical vibrato
            NoteSpec(293.66, 650), // D4
            NoteSpec(329.63, 800), // E4
            NoteSpec(369.99, 1300), // F#4
            NoteSpec(440.00, 1500), // A4
            NoteSpec(392.00, 750), // G4
            NoteSpec(369.99, 900), // F#4
            NoteSpec(329.63, 1600), // E4
            NoteSpec(0.0, 350),
            NoteSpec(293.66, 800),
            NoteSpec(261.63, 1400)
        )
        MuezzinVoice.ISTANBUL -> listOf(
            // Ottoman Sabah maqam
            NoteSpec(261.63, 800), // C4
            NoteSpec(277.18, 850), // Db4
            NoteSpec(329.63, 1200), // E4
            NoteSpec(349.23, 1350), // F4
            NoteSpec(329.63, 750), // E4
            NoteSpec(277.18, 850), // Db4
            NoteSpec(261.63, 1700)  // C4
        )
        MuezzinVoice.SOFT_TONE -> listOf(
            // Calm chime takbir
            NoteSpec(440.00, 450), // A4
            NoteSpec(554.37, 500), // C#5
            NoteSpec(659.25, 900), // E5
            NoteSpec(554.37, 450), // C#5
            NoteSpec(440.00, 1100)  // A4
        )
    }
}
