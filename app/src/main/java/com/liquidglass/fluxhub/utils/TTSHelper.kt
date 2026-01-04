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
            // 优先尝试中文，失败则使用设备默认语言
            var result = tts?.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Chinese not supported, falling back to default locale")
                result = tts?.setLanguage(Locale.getDefault())
            }
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Default language also not supported")
            } else {
                isInitialized = true
                Log.d(TAG, "TTS initialized successfully")
                pendingText?.let {
                    speak(it)
                    pendingText = null
                }
            }
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
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
