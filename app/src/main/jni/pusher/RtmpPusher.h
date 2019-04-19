//
// Created by lzc on 2018/3/23 0023.
//



#ifndef YIOKS_RECORD_RTMPPUSHER_H
#define YIOKS_RECORD_RTMPPUSHER_H

#include <rtmp.h>
#include "jni.h"

#include "pthread.h"
#include "strings.h"
#include "android/log.h"
#include <pthread.h>
#include <malloc.h>
#include <pushUtil.h>


#endif //YIOKS_RECORD_RTMPPUSHER_H
extern "C" {
#include "queue.h"
#include "log.h"

#ifdef VERBOSE

#define JNI_TAG "RTMP_PUSH"
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, JNI_TAG, __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, JNI_TAG, __VA_ARGS__)

#else
#define  LOG_TAG    "lzc_push"
#define ALOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG ,__VA_ARGS__)
#define ALOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG ,__VA_ARGS__)

#endif

#define Fail 1
#define Lost 2
#define Connect 3


class RtmpPusher {
private:
    char *pushUrl;
    int mediaSize;
    //rtmp开始时间
    long startTime;
    // 线程
    pthread_t publisher_tid;
    pthread_mutex_t mutex;
    pthread_cond_t cond;
// rtmp
    RTMP *rtmpPusher;
    //是否暂停
    int isPause=FALSE;
    // 推流标志
    int pushing;
    // 请求停止
    int requestStop;
    int isConnect=TRUE;

    // 入队
    void rtmpPacketPush(RTMPPacket *packet);

    // 释放资源
    static void nativeStop(RtmpPusher *pusher);

    //推流线程
    static void *rtmpPushThread(void *args);

    static void logCallback(int logLevel, const char* msg,va_list args);

public:
    RtmpPusher()
            : mediaSize(0),
              rtmpPusher(NULL),
              startTime(0),
              pushing(0),
              requestStop(0),
              publisher_tid(NULL) {};

    //回调函数
    void (*callback)(int status)=nullptr;

    //初始化推流器
    void initRtmp(char *url);

    //推视频帧
    void pushVideoFrame(char *buf, int len,long time);

    //推音频帧
    void pushAudioFrame(char *buf, int len,long time);

    //推视频格式
    void pushVideoFormat(char *pps, char *sps, int pps_len, int sps_len);

    //推音频格式
    void pushAudioFormat(char *data, int length);

    //停止推流
    void stopPush();

    // 停止状态
    int isStop();

    // 推流状态
    int isPushing();

    // 请求暂停
    void callPause();

    // 请求恢复
    void callResume();

    //设置推流器
    void setRtmpPusher(RTMP *pusher);


    //获取推流器
    RTMP *getRtmpPusher();

    void backFail();

    void backLost();
    void backConnect();


    int connectRTMP();

    void releasePush();
};
}