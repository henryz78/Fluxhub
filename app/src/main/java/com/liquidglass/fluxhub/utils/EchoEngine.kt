package com.liquidglass.fluxhub.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class EchoEngine(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    // Callback for playback status
    var onStart: ((String) -> Unit)? = null
    var onDone: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale.getDefault()
                
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {
                        onStart?.invoke(utteranceId)
                    }

                    override fun onDone(utteranceId: String) {
                        onDone?.invoke(utteranceId)
                    }

                    override fun onError(utteranceId: String) {
                        onError?.invoke(utteranceId)
                    }
                })
            } else {
                Log.e("EchoEngine", "TTS Initialiazation Failed!")
            }
        }
    }

    fun speak(text: String, id: String) {
        if (!isInitialized) return
        
        // 自动检测语言 (简单策略：如果不全是ASCII，则尝试中文，否则英文，或跟随系统)
        // 目前暂用系统默认
        
        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id)
        
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, id)
    }

    fun stop() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
