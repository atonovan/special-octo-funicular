/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Based on jni_bridge.cpp
 * https://github.com/google/oboe/tree/0a78e50b64/samples/LiveEffect/src/main/cpp/jni_bridge.cpp
 *
 * Modifications Copyright 2020 Allen Schober
 *
 */

#include <jni.h>
#include <logging_macros.h>
#include "NativeAudioEngine.h"
#include <chromaprint.h>

static const int kOboeApiAAudio = 0;
static const int kOboeApiOpenSLES = 1;

static NativeAudioEngine *engine = nullptr;

extern "C" {

    JNIEXPORT jboolean JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_create(JNIEnv *env, jclass) {
        if (engine == nullptr) {
            engine = new NativeAudioEngine(env);
        }

        return (engine != nullptr) ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT void JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_delete(JNIEnv *env, jclass) {
        delete engine;
        engine = nullptr;
    }

    JNIEXPORT jboolean JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_prepareRecording(
            JNIEnv *env, jclass) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine before calling this "
                    "method");
            return JNI_FALSE;
        }

        return engine->prepareRecording(env) ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT jboolean JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_startRecording(JNIEnv *env, jclass) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine before calling this "
                    "method");
            return -1;
        }

        return engine->startRecording(env) ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT jboolean JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_stopRecording(JNIEnv *env, jclass) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine before calling this "
                    "method");
            return JNI_FALSE;
        }

        return engine->stopRecording(env) ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT void JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_setRecordingDeviceId(JNIEnv *env, jclass, jint deviceId) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine before calling this "
                    "method");
            return;
        }

        engine->setRecordingDeviceId(deviceId);
    }

    JNIEXPORT void JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_setPlaybackDeviceId(
            JNIEnv *env, jclass, jint deviceId) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine before calling this "
                    "method");
            return;
        }

        engine->setPlaybackDeviceId(deviceId);
    }

    JNIEXPORT jboolean JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_setLowLatency(JNIEnv *env, jclass type, jboolean lowLatency) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine "
                    "before calling this method");
            return JNI_FALSE;
        }

        return engine->setLowLatency(lowLatency);
    }

    JNIEXPORT jboolean JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_setAudioApi(JNIEnv *env,jclass type, jint apiType) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine "
                    "before calling this method");
            return JNI_FALSE;
        }

        oboe::AudioApi audioApi;
        switch (apiType) {
            case kOboeApiAAudio:
                audioApi = oboe::AudioApi::AAudio;
                break;
            case kOboeApiOpenSLES:
                audioApi = oboe::AudioApi::OpenSLES;
                break;
            default:
                LOGE("Unknown API selection to setAPI() %d", apiType);
                return JNI_FALSE;
        }

        return engine->setAudioApi(audioApi) ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT jboolean JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_isAAudioSupportedAndRecommended(JNIEnv *env, jclass type) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine "
                    "before calling this method");
            return JNI_FALSE;
        }
        return engine->isAAudioSupportedAndRecommended() ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT void JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_setAudioDataListener(JNIEnv *env, jclass type, jobject listener) {

        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine "
                    "before calling this method");
            return;
        }
        engine->setAudioDataListener(env, type, listener);
    }

    JNIEXPORT jint JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_getSampleRate(JNIEnv *env, jclass type) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine "
                    "before calling this method");
            return JNI_ERR;
        }
        return engine->getSampleRate();
    }

    JNIEXPORT jint JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_getChannelCount(JNIEnv *env, jclass type) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine "
                    "before calling this method");
            return JNI_ERR;
        }
        return engine->getChannelCount();
    }

    JNIEXPORT jint JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_getBitRate(JNIEnv *env, jclass type) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine "
                    "before calling this method");
            return JNI_ERR;
        }
        return engine->getBitRate();
    }

    JNIEXPORT jint JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_getAudioApi(JNIEnv *env, jclass type) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine "
                    "before calling this method");
            return JNI_ERR;
        }
        return engine->getAudioApi();
    }

    JNIEXPORT jstring JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_getOboeVersion(JNIEnv *env, jclass type) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine "
                    "before calling this method");
            return nullptr;
        }
        return env->NewStringUTF(engine->getOboeVersion());
    }

    JNIEXPORT void JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_setGainDecibels(JNIEnv *env, jclass clazz,
                                                                        jdouble decibels) {
        if (engine == nullptr) {
            LOGE(
                    "Engine is null, you must call createEngine "
                    "before calling this method");
            return;
        }
        engine->setGainDecibels(decibels);
    }

    // Chromaprint JNI methods
    JNIEXPORT jlong JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_createChromaprint(
            JNIEnv *env, jclass, jint sampleRate, jint channels) {
        ChromaprintContext *ctx = chromaprint_new(CHROMAPRINT_ALGORITHM_DEFAULT);
        if (ctx == nullptr) {
            LOGE("Failed to create chromaprint context");
            return 0;
        }

        if (!chromaprint_start(ctx, sampleRate, channels)) {
            LOGE("Failed to start chromaprint");
            chromaprint_free(ctx);
            return 0;
        }

        return reinterpret_cast<jlong>(ctx);
    }

    JNIEXPORT jboolean JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_feedChromaprint(
            JNIEnv *env, jclass, jlong ctxPtr, jshortArray samples, jint length) {
        ChromaprintContext *ctx = reinterpret_cast<ChromaprintContext*>(ctxPtr);
        if (ctx == nullptr) {
            LOGE("Chromaprint context is null");
            return JNI_FALSE;
        }

        jshort *sampleData = env->GetShortArrayElements(samples, nullptr);
        if (sampleData == nullptr) {
            LOGE("Failed to get sample data");
            return JNI_FALSE;
        }

        int result = chromaprint_feed(ctx, sampleData, length);
        env->ReleaseShortArrayElements(samples, sampleData, JNI_ABORT);

        return result ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT jstring JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_finishChromaprint(
            JNIEnv *env, jclass, jlong ctxPtr) {
        ChromaprintContext *ctx = reinterpret_cast<ChromaprintContext*>(ctxPtr);
        if (ctx == nullptr) {
            LOGE("Chromaprint context is null");
            return nullptr;
        }

        if (!chromaprint_finish(ctx)) {
            LOGE("Failed to finish chromaprint");
            return nullptr;
        }

        char *fingerprint = nullptr;
        if (!chromaprint_get_fingerprint(ctx, &fingerprint)) {
            LOGE("Failed to get fingerprint");
            return nullptr;
        }

        jstring result = env->NewStringUTF(fingerprint);
        chromaprint_dealloc(fingerprint);

        return result;
    }

    JNIEXPORT void JNICALL
    Java_tech_schober_vinylcast_audio_NativeAudioEngine_freeChromaprint(
            JNIEnv *env, jclass, jlong ctxPtr) {
        ChromaprintContext *ctx = reinterpret_cast<ChromaprintContext*>(ctxPtr);
        if (ctx != nullptr) {
            chromaprint_free(ctx);
        }
    }
}
