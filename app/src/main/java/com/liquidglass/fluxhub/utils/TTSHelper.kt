package com.liquidglass.fluxhub.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class TTSHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false
    private var pendingText: String? = null

    companion object {
        private const val TAG = "TTSHelper"
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported")
            } else {
                isInitialized = true
                pendingText?.let {
                    speak(it)
                    pendingText = null
                }
            }
        } else {
            Log.e(TAG, "Initialization failed")
        }
    }

    fun speak(text: String, utteranceId: String = UUID.randomUUID().toString()) {
        if (isInitialized) {
            // 简单逻辑：根据文字包含英文字符比例切换语言（可选）
            // 这里暂用系统默认语言切换逻辑或手动指定
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            pendingText = text
            Log.w(TAG, "TTS not initialized yet, pending text")
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
