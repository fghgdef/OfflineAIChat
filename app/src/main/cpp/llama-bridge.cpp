// جسر JNI بسيط بين Kotlin و llama.cpp
// ملاحظة: أسماء دوال llama.cpp ممكن تتغير شوي حسب نسخة الـ submodule المستخدمة.
// إذا صار خطأ بالترجمة (compile error)، افتح llama.cpp/include/llama.h وقارن التواقيع.

#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"

#define LOG_TAG "OfflineAIChat"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct LlamaSession {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    llama_sampler* sampler = nullptr;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_offlineaichat_LlamaBridge_nativeLoadModel(
        JNIEnv* env, jobject, jstring modelPath, jint nThreads, jint nCtx) {

    const char* path = env->GetStringUTFChars(modelPath, nullptr);

    llama_backend_init();

    llama_model_params modelParams = llama_model_default_params();
    modelParams.n_gpu_layers = 0; // تشغيل على CPU بالكامل (أكثر توافق مع كل الأجهزة)

    llama_model* model = llama_model_load_from_file(path, modelParams);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!model) {
        LOGE("فشل تحميل الموديل");
        return 0;
    }

    llama_context_params ctxParams = llama_context_default_params();
    ctxParams.n_ctx = nCtx > 0 ? nCtx : 2048;
    ctxParams.n_threads = nThreads > 0 ? nThreads : 4;
    ctxParams.n_threads_batch = ctxParams.n_threads;

    llama_context* ctx = llama_init_from_model(model, ctxParams);
    if (!ctx) {
        LOGE("فشل إنشاء السياق (context)");
        llama_model_free(model);
        return 0;
    }

    // سلسلة أخذ العينات (sampling): temperature + top_k + اختيار نهائي
    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    llama_sampler* sampler = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    auto* session = new LlamaSession{model, ctx, sampler};
    return reinterpret_cast<jlong>(session);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_offlineaichat_LlamaBridge_nativeGenerate(
        JNIEnv* env, jobject, jlong handle, jstring prompt, jint maxTokens) {

    auto* session = reinterpret_cast<LlamaSession*>(handle);
    if (!session || !session->ctx) {
        return env->NewStringUTF("خطأ: الموديل غير محمّل");
    }

    const char* promptChars = env->GetStringUTFChars(prompt, nullptr);
    std::string promptStr(promptChars);
    env->ReleaseStringUTFChars(prompt, promptChars);

    const llama_vocab* vocab = llama_model_get_vocab(session->model);

    // تحويل النص لـ tokens
    int nPromptTokens = -llama_tokenize(vocab, promptStr.c_str(), (int32_t) promptStr.size(),
                                         nullptr, 0, true, true);
    std::vector<llama_token> tokens(nPromptTokens);
    llama_tokenize(vocab, promptStr.c_str(), (int32_t) promptStr.size(),
                   tokens.data(), (int32_t) tokens.size(), true, true);

    llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t) tokens.size());

    std::string result;

    for (int i = 0; i < maxTokens; i++) {
        if (llama_decode(session->ctx, batch) != 0) {
            LOGE("فشل llama_decode");
            break;
        }

        llama_token newToken = llama_sampler_sample(session->sampler, session->ctx, -1);

        if (llama_vocab_is_eog(vocab, newToken)) {
            break; // النموذج أنهى الجواب
        }

        char piece[256];
        int n = llama_token_to_piece(vocab, newToken, piece, sizeof(piece), 0, true);
        if (n > 0) {
            result.append(piece, n);
        }

        batch = llama_batch_get_one(&newToken, 1);
    }

    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_offlineaichat_LlamaBridge_nativeFreeModel(
        JNIEnv*, jobject, jlong handle) {

    auto* session = reinterpret_cast<LlamaSession*>(handle);
    if (!session) return;

    if (session->sampler) llama_sampler_free(session->sampler);
    if (session->ctx) llama_free(session->ctx);
    if (session->model) llama_model_free(session->model);
    delete session;
}
