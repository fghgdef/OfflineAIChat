package com.example.offlineaichat

// صف Kotlin بيوصل مباشرة بكود C++ (JNI) يلي بيشغل llama.cpp محليًا على الجهاز
class LlamaBridge {

    companion object {
        init {
            System.loadLibrary("offlineaichat")
        }
    }

    private var handle: Long = 0

    fun loadModel(modelPath: String, nThreads: Int = 4, nCtx: Int = 2048): Boolean {
        handle = nativeLoadModel(modelPath, nThreads, nCtx)
        return handle != 0L
    }

    fun generate(prompt: String, maxTokens: Int = 256): String {
        if (handle == 0L) return "الموديل غير محمّل بعد"
        return nativeGenerate(handle, prompt, maxTokens)
    }

    fun free() {
        if (handle != 0L) {
            nativeFreeModel(handle)
            handle = 0
        }
    }

    private external fun nativeLoadModel(modelPath: String, nThreads: Int, nCtx: Int): Long
    private external fun nativeGenerate(handle: Long, prompt: String, maxTokens: Int): String
    private external fun nativeFreeModel(handle: Long)
}
