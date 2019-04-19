#include <jni.h>
#include <RtmpPusher.h>


extern "C" {
RtmpPusher *rtmpPusher;
JavaVM *jvm;
jobject callbackObj;
jclass callbackClass;


JNIEnv *getEnvByJvm() {
    JNIEnv *env;
    int status = jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (status < 0) {
        status = jvm->AttachCurrentThread(&env, NULL);
        if (status < 0) {
            return nullptr;
        }
    }
    return env;
}

void callBackFail() {
    JNIEnv *env = getEnvByJvm();
    if (env == nullptr || callbackClass == NULL)
        return;
    jmethodID failMethodId = env->GetMethodID(callbackClass, "fail", "()V");
    env->CallVoidMethod(callbackObj, failMethodId);
}

void callBackConnect() {
    JNIEnv *env = getEnvByJvm();
    if (env == nullptr || callbackClass == NULL)
        return;
    jmethodID failMethodId = env->GetMethodID(callbackClass, "connect", "()V");
    env->CallVoidMethod(callbackObj, failMethodId);
}

void callBackLost() {
    JNIEnv *env = getEnvByJvm();
    if (env == nullptr || callbackClass == NULL)
        return;
    jmethodID lostPackMethodId = env->GetMethodID(callbackClass, "lostPack", "()V");;
    env->CallVoidMethod(callbackObj, lostPackMethodId);
}
void initCallback(JNIEnv *env, jobject instance) {
    env->GetJavaVM(&jvm);
    callbackObj = env->NewGlobalRef(instance);
    const char *classStr = "com/yioks/recorder/LiveRecord/Pusher";
    callbackClass = (jclass) env->NewGlobalRef(env->FindClass(classStr));
}


void callBack(int status) {
    //失败回调
    if (status == Fail) {
        callBackFail();
    }
        //丢包回调
    else if (status == Lost) {
        callBackLost();
    }
    else if(status==Connect)
    {
        callBackConnect();
    }
}


JNIEXPORT void JNICALL
Java_com_yioks_recorder_LiveRecord_Pusher_pushAudioFormat(JNIEnv *env, jobject instance,
                                                          jbyteArray data_) {
    char *data = (char *) env->GetByteArrayElements(data_, NULL);
    if (rtmpPusher) {
        rtmpPusher->pushAudioFormat(data, env->GetArrayLength(data_));
    }
    env->ReleaseByteArrayElements(data_, (jbyte *) data, 0);
}

JNIEXPORT void JNICALL
Java_com_yioks_recorder_LiveRecord_Pusher_pushVideoFormat(JNIEnv *env, jobject instance,
                                                          jbyteArray pps_, jbyteArray sps_) {
    char *pps = (char *) env->GetByteArrayElements(pps_, NULL);
    char *sps = (char *) env->GetByteArrayElements(sps_, NULL);

    if (rtmpPusher) {
        rtmpPusher->pushVideoFormat(pps, sps, env->GetArrayLength(pps_), env->GetArrayLength(sps_));
    }

    env->ReleaseByteArrayElements(pps_, (jbyte *) pps, 0);
    env->ReleaseByteArrayElements(sps_, (jbyte *) sps, 0);
}

JNIEXPORT void JNICALL
Java_com_yioks_recorder_LiveRecord_Pusher_pushVideoFrame(JNIEnv *env, jobject instance,
                                                         jbyteArray data_, jlong time_) {
    char *data = (char *) env->GetByteArrayElements(data_, NULL);

    if (rtmpPusher) {
        rtmpPusher->pushVideoFrame(data, env->GetArrayLength(data_), (long) time_);
    }

    env->ReleaseByteArrayElements(data_, (jbyte *) data, 0);
}

JNIEXPORT void JNICALL
Java_com_yioks_recorder_LiveRecord_Pusher_pushAudioFrame(JNIEnv *env, jobject instance,
                                                         jbyteArray data_, jlong time_) {
    char *data = (char *) env->GetByteArrayElements(data_, NULL);
    if (rtmpPusher) {
        rtmpPusher->pushAudioFrame(data, env->GetArrayLength(data_), (long) time_);
    }
    env->ReleaseByteArrayElements(data_, (jbyte *) data, 0);
}

JNIEXPORT void JNICALL
Java_com_yioks_recorder_LiveRecord_Pusher_stop(JNIEnv *env, jobject instance) {
    if (rtmpPusher) {
        rtmpPusher->stopPush();
        rtmpPusher = NULL;
        env->DeleteGlobalRef(callbackClass);
        callbackClass = NULL;
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_com_yioks_recorder_LiveRecord_Pusher_pause(JNIEnv *env, jobject instance) {

    if (rtmpPusher) {
        rtmpPusher->callPause();
    }

}extern "C"
JNIEXPORT void JNICALL
Java_com_yioks_recorder_LiveRecord_Pusher_resume(JNIEnv *env, jobject instance) {
    if (rtmpPusher) {
        rtmpPusher->callResume();
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_yioks_recorder_LiveRecord_Pusher_start(JNIEnv *env, jobject instance, jstring url_) {
    jstring gloStr= (jstring)(env->NewGlobalRef(url_));
    const char *url = env->GetStringUTFChars(gloStr, NULL);
    if (!rtmpPusher) {
        rtmpPusher = new RtmpPusher();
        rtmpPusher->callback = callBack;
        initCallback(env, instance);
    }
    rtmpPusher->initRtmp((char *) url);
    return;
}
}