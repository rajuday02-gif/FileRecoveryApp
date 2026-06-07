#include <jni.h>
#include <android/log.h>
#include "recovery_core.h"

#define LOG_TAG "RecoveryJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static RecoveryCore* g_recovery_core = nullptr;

extern "C" JNIEXPORT void JNICALL
Java_com_recovery_filecarver_RecoveryEngine_initEngine(
        JNIEnv* env,
        jobject thiz) {
    if (g_recovery_core == nullptr) {
        g_recovery_core = new RecoveryCore();
        g_recovery_core->initSignatures();
        LOGI("Recovery engine initialized");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_recovery_filecarver_RecoveryEngine_startScan(
        JNIEnv* env,
        jobject thiz,
        jstring input_path,
        jstring output_dir,
        jlong max_size) {

    if (g_recovery_core == nullptr) {
        LOGE("Engine not initialized");
        return;
    }

    const char* input = env->GetStringUTFChars(input_path, nullptr);
    const char* output = env->GetStringUTFChars(output_dir, nullptr);

    LOGI("Starting scan: %s -> %s (max: %lld bytes)", input, output, max_size);
    g_recovery_core->startScan(std::string(input), std::string(output), (size_t)max_size);

    env->ReleaseStringUTFChars(input_path, input);
    env->ReleaseStringUTFChars(output_dir, output);
}

extern "C" JNIEXPORT void JNICALL
Java_com_recovery_filecarver_RecoveryEngine_stopScan(
        JNIEnv* env,
        jobject thiz) {

    if (g_recovery_core != nullptr) {
        g_recovery_core->stopScan();
        LOGI("Scan stopped");
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_recovery_filecarver_RecoveryEngine_getProgress(
        JNIEnv* env,
        jobject thiz) {

    if (g_recovery_core == nullptr) {
        return nullptr;
    }

    auto progress = g_recovery_core->getProgress();

    // Create ScanProgress object
    jclass progress_class = env->FindClass("com/recovery/filecarver/ScanProgress");
    jmethodID constructor = env->GetMethodID(progress_class, "<init>", "(JJII)V");

    jobject progress_obj = env->NewObject(
        progress_class,
        constructor,
        (jlong)progress.processed_bytes,
        (jlong)progress.total_bytes,
        (jint)progress.files_found,
        (jint)progress.confidence_avg
    );

    return progress_obj;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_recovery_filecarver_RecoveryEngine_getRecoveredFiles(
        JNIEnv* env,
        jobject thiz) {

    if (g_recovery_core == nullptr) {
        jclass string_class = env->FindClass("java/lang/String");
        return env->NewObjectArray(0, string_class, nullptr);
    }

    auto files = g_recovery_core->getRecoveredFiles();

    // Create array of RecoveredFileInfo objects
    jclass file_info_class = env->FindClass("com/recovery/filecarver/RecoveredFileInfo");
    jobjectArray result = env->NewObjectArray(files.size(), file_info_class, nullptr);

    jmethodID constructor = env->GetMethodID(
        file_info_class,
        "<init>",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JJI)V"
    );

    for (size_t i = 0; i < files.size(); i++) {
        const auto& file = files[i];
        jstring path = env->NewStringUTF(file.path.c_str());
        jstring name = env->NewStringUTF(file.name.c_str());
        jstring type = env->NewStringUTF(file.type.c_str());

        jobject file_obj = env->NewObject(
            file_info_class,
            constructor,
            path,
            name,
            type,
            (jlong)file.size,
            (jlong)file.offset,
            (jint)file.confidence
        );

        env->SetObjectArrayElement(result, i, file_obj);
    }

    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_recovery_filecarver_RecoveryEngine_enableRootMode(
        JNIEnv* env,
        jobject thiz,
        jboolean enable) {

    if (g_recovery_core != nullptr) {
        g_recovery_core->enableRootMode((bool)enable);
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_recovery_filecarver_RecoveryEngine_hasRootAccess(
        JNIEnv* env,
        jobject thiz) {

    if (g_recovery_core == nullptr) return false;
    return g_recovery_core->hasRootAccess();
}

extern "C" JNIEXPORT void JNICALL
Java_com_recovery_filecarver_RecoveryEngine_destroyEngine(
        JNIEnv* env,
        jobject thiz) {

    if (g_recovery_core != nullptr) {
        delete g_recovery_core;
        g_recovery_core = nullptr;
        LOGI("Engine destroyed");
    }
}
