#include <jni.h>
#include <android/log.h>
#include "carver.h"

#define LOG_TAG "FileCarver"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static FileCarver* g_carver = nullptr;

extern "C" JNIEXPORT void JNICALL
Java_com_recovery_filecarver_CarverEngine_initCarver(
        JNIEnv* env,
        jobject thiz) {
    if (g_carver == nullptr) {
        g_carver = new FileCarver();
        LOGI("Carver initialized");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_recovery_filecarver_CarverEngine_startCarving(
        JNIEnv* env,
        jobject thiz,
        jstring input_path,
        jstring output_dir,
        jint max_size,
        jobjectArray file_types) {

    if (g_carver == nullptr) {
        LOGE("Carver not initialized");
        return;
    }

    const char* input_c = env->GetStringUTFChars(input_path, nullptr);
    const char* output_c = env->GetStringUTFChars(output_dir, nullptr);

    // Convert file types array
    jsize array_len = env->GetArrayLength(file_types);
    std::vector<std::string> types;
    for (jsize i = 0; i < array_len; i++) {
        jstring type_str = (jstring) env->GetObjectArrayElement(file_types, i);
        const char* type_c = env->GetStringUTFChars(type_str, nullptr);
        types.push_back(std::string(type_c));
        env->ReleaseStringUTFChars(type_str, type_c);
    }

    LOGI("Starting carving: %s -> %s (max: %d MB)", input_c, output_c, max_size);
    g_carver->carveFiles(std::string(input_c), std::string(output_c), max_size * 1024 * 1024, types);

    env->ReleaseStringUTFChars(input_path, input_c);
    env->ReleaseStringUTFChars(output_dir, output_c);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_recovery_filecarver_CarverEngine_getRecoveredCount(
        JNIEnv* env,
        jobject thiz) {
    if (g_carver == nullptr) return 0;
    return g_carver->getRecoveredCount();
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_recovery_filecarver_CarverEngine_getRecoveredFiles(
        JNIEnv* env,
        jobject thiz) {

    if (g_carver == nullptr) {
        jclass string_class = env->FindClass("java/lang/String");
        return env->NewObjectArray(0, string_class, nullptr);
    }

    std::vector<std::string> files = g_carver->getRecoveredFiles();
    jclass string_class = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(files.size(), string_class, nullptr);

    for (size_t i = 0; i < files.size(); i++) {
        env->SetObjectArrayElement(result, i, env->NewStringUTF(files[i].c_str()));
    }

    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_recovery_filecarver_CarverEngine_stopCarving(
        JNIEnv* env,
        jobject thiz) {
    if (g_carver != nullptr) {
        g_carver->stop();
        LOGI("Carving stopped");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_recovery_filecarver_CarverEngine_destroyCarver(
        JNIEnv* env,
        jobject thiz) {
    if (g_carver != nullptr) {
        delete g_carver;
        g_carver = nullptr;
        LOGI("Carver destroyed");
    }
}
