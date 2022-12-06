package fr.xgouchet.radiovoice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

class AnchorVoice(context: Context) : UtteranceProgressListener(),
    TextToSpeech.OnInitListener,
    AudioManager.OnAudioFocusChangeListener {

    // TODO customize preferred provider
    private val tts = TextToSpeech(context, this, "com.cereproc.William").apply {
        language = Locale.US
    }

    private var audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    private var audioFocusRequest: AudioFocusRequest? = null

    private val sessionId: String = UUID.randomUUID().toString()
    private var messageCount = 0

    private var isTtsready = false
    private var lastUtteranceId = ""

    suspend fun queueMessage(message: String) {
        Thread.sleep(100)
        val utteranceId = synchronized(this) {
            messageCount++
            "$sessionId/$messageCount"
        }

        if (isTtsready) {
            val params = Bundle()
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            params.putInt(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1) // TODO settings ? 1 = max
            params.putInt(TextToSpeech.Engine.KEY_PARAM_PAN, 0) // TODO settings ? 1 = right

            Log.i("AnchorVoice", "Starting utterance:$utteranceId msg:“$message”")
            tts.speak(message, TextToSpeech.QUEUE_ADD, params, utteranceId)
            lastUtteranceId = utteranceId
        }
    }

    // region UtteranceProgressListener

    override fun onStart(utteranceId: String?) {
        if (audioFocusRequest != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(this).build()
        Log.i("AnchorVoice", "Requesting audio focus for utterance:$utteranceId $request")
        val requestStatus = audioManager?.requestAudioFocus(request)
        when (requestStatus) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                Log.i("AnchorVoice", "Audio focus request was granted")
                audioFocusRequest = request
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                Log.i("AnchorVoice", "Audio focus request was delayed")
                audioFocusRequest = request
            }
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Log.e("AnchorVoice", "Unable to request audio focus")
                audioFocusRequest = null
            }
        }
    }

    override fun onDone(utteranceId: String?) {
        val previousRequest = audioFocusRequest
        Log.i("AnchorVoice", "Done playing utterance:$utteranceId $previousRequest")
        if (utteranceId == lastUtteranceId && previousRequest != null) {
            Log.i("AnchorVoice", "Abandoning audio focus $previousRequest")
            audioManager?.abandonAudioFocusRequest(previousRequest)
            audioFocusRequest = null
        }
    }

    override fun onError(utteranceId: String?, errorCode: Int) {
        Log.e("AnchorVoice", "Error playing utterance:$utteranceId code:$errorCode")
        super.onError(utteranceId, errorCode)
    }

    override fun onError(utteranceId: String?) {
        Log.e("AnchorVoice", "Error playing utterance:$utteranceId")
    }

    // endregion

    // region TextToSpeech.OnInitListener

    override fun onInit(status: Int) {
        when (status) {
            TextToSpeech.SUCCESS -> {
                isTtsready = true
                tts.setOnUtteranceProgressListener(this)
            }
            TextToSpeech.ERROR -> Log.e("AnchorVoice", "Failed to initialize TTS")
            else -> Log.wtf("AnchorVoice", "Unknown TTS status")
        }
    }

    // endregion

    // region OnAudioFocusChangeListener

    override fun onAudioFocusChange(focusChange: Int) {
        val focusName = when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> "AUDIOFOCUS_GAIN"
            AudioManager.AUDIOFOCUS_LOSS -> "AUDIOFOCUS_LOSS"
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> "AUDIOFOCUS_LOSS_TRANSIENT"
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK"
            else -> "Unknown"
        }
        Log.d("AnchorVoice", "Audio focus changed to $focusChange/$focusName")
    }

    // endregion
}