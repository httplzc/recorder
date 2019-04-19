//
// Created by lzc on 2018/3/23 0023.
//


#include <RtmpPusher.h>


#define STREAM_CHANNEL_VIDEO     0x04
#define STREAM_CHANNEL_AUDIO     0x05

extern "C" {
/**
 * 初始化RTMP
 * @param url
 * @return
 */
void RtmpPusher::initRtmp(char *url) {
    this->pushUrl = url;
    RTMP_LogSetCallback(logCallback);
    // 3、建立连接
    if (!connectRTMP()) {
        backFail();
        return;
    }
    // 创建队列
    create_queue();
    pushing = 1;
    mutex = PTHREAD_MUTEX_INITIALIZER;
    cond = PTHREAD_COND_INITIALIZER;
    pthread_create(&publisher_tid, NULL, RtmpPusher::rtmpPushThread, this);

    return;
}


void RtmpPusher::logCallback(int logLevel, const char *msg, va_list args) {
    char log[1024];
    vsprintf(log, msg, args);
    ALOGI("%s", log);
}

/**
 * 推流线程
 * @param args
 * @return
 */
//int64_t time1 = 0;
//int count = 0;
void *RtmpPusher::rtmpPushThread(void *args) {
    RtmpPusher *pusher = (RtmpPusher *) args;
    while (!pusher->isStop() && pusher->isPushing()) {
        pthread_mutex_lock(&pusher->mutex);
        if (queue_size() == 0)
            pthread_cond_wait(&pusher->cond, &pusher->mutex);
        // 请求停止，则立马跳出循环
        if (pusher->isStop()) {
            break;
        }
        // 如果不处于pushing状态，则继续等待
        if (!pusher->isPushing() || pusher->isConnect) {
            continue;
        }
        int size = queue_size() > 10 ? 10 : queue_size();
        if (size == 0)
            continue;
        RTMPPacket **packets = new RTMPPacket *[size];
        for (int i = 0; i < size; i++) {
            packets[i] = (RTMPPacket *) queue_get_head();
            queue_remove();
        }

        pthread_mutex_unlock(&pusher->mutex);
        // 发送RTMP包，推流操作
        for (int i = 0; i < size; i++) {
            //int64_t time1 = getCurrentTime();
            RTMPPacket *packet = packets[i];
            if (packet && !pusher->isPause) {
                bool retry = FALSE;
                int retryCount = 0;
                do {
                    retryCount++;
                    //如果不是恢复信号，流已经断掉则退出
                    if (!RTMP_IsConnected(pusher->rtmpPusher))
                        break;
                    int result = 0;
                    if (packet->is_pause) {
                        ALOGI("正在请求暂停");
                        result = RTMP_SendPause(pusher->rtmpPusher, TRUE,
                                                RTMP_GetTime() - pusher->startTime);
                    } else {
                        result = RTMP_SendPacket(pusher->rtmpPusher, packet, TRUE);
                    }
                    if (result) {
                        ALOGI("RTMP_SendPacket success!  %d", queue_size());
                        if (packet->is_pause) {
                            pusher->isPause = TRUE;
                        } else {
                            RTMPPacket_Free(packet);
                        }

                    } else {
                        if (packet->is_format || packet->is_pause) {
                            retry = TRUE;
                            ALOGI("RTMP_SendPacket 关键包 failed!");
                        } else {
                            ALOGI("RTMP_SendPacket failed!");
                            RTMPPacket_Free(packet);
                        }
                    }
                } while (retry && retryCount < 5);
            }
        }
    }
    // 如果请求停止操作，则释放RTMP等资源，以防内存泄漏
    if (pusher->requestStop) {
        destroy_queue();
    }
    delete pusher;
    ALOGI("RTMP_SendPacket stop!");
    return NULL;
}
/**
 * 停止推流
 */
void RtmpPusher::stopPush() {
    pushing = 0;
    requestStop = 1;
    releasePush();
    //避免线程处于wait之中
    pthread_mutex_lock(&mutex);
    pthread_cond_signal(&cond);
    pthread_mutex_unlock(&mutex);
}

/**
 * 停止推流
 */
void RtmpPusher::releasePush() {
    if (rtmpPusher) {
        try {
            RTMP_Close(rtmpPusher);
            RTMP_Free(rtmpPusher);
        } catch (...) {

        }
        rtmpPusher = NULL;
    }

}

/**
 * 添加RTMPPacket数据包到队列
 * @param packet
 */
void RtmpPusher::rtmpPacketPush(RTMPPacket *packet) {
    pthread_mutex_lock(&mutex);
    if (queue_size() > 700) {
        ALOGI("推流超时 %d", queue_size());
        backFail();
        return;
    }
        // 丢包操作
    else if (queue_size() > 50) {
        for (int i = 0; i < 25; i++) {
            RTMPPacket *temp = (RTMPPacket *) queue_get(i);
            if ((temp->is_video && temp->is_key) || temp->is_format || temp->is_pause)
                continue;
            queue_delete(i);
        }
        ALOGI("丢包操作！");
        if (RTMP_GetTime() - startTime > 5000)
            backLost();
    }
    queue_put_tail(packet);
    pthread_cond_signal(&cond);
    pthread_mutex_unlock(&mutex);
}


/**
 * 添加视频帧
 * @param buf
 * @param len
 */
void RtmpPusher::pushVideoFrame(char *buf, int len, long time) {
    if (!rtmpPusher || !RTMP_IsConnected(rtmpPusher)) {
        ALOGI("pushVideoFrame RTMP_IsConnected ? false");
        if (isPushing())
            backFail();
        return;
    }

    if (requestStop) {
        return;
    }
    //sps 与 pps 的帧界定符都是 00 00 00 01，而普通帧可能是 00 00 00 01 也有可能 00 00 01
    /*去掉帧界定符*/
    if (buf[2] == 0x00) {   // 00 00 00 01
        buf += 4;
        len -= 4;
    } else if (buf[2] == 0x01) { // 00 00 01
        buf += 3;
        len -= 3;
    }
    int body_size = len + 9;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    if (!packet) {
        return;
    }
    RTMPPacket_Reset(packet);
    if (!RTMPPacket_Alloc(packet, (uint32_t) body_size)) {
        return;
    }
    char *body = packet->m_body;
    int k = 0;
    int type = buf[0] & 0x1f;
    if (type == 5) {
        body[k++] = 0x17;
    } else {
        body[k++] = 0x27;
    }
    body[k++] = 0x01;
    body[k++] = 0x00;
    body[k++] = 0x00;
    body[k++] = 0x00;

    body[k++] = (char) ((len >> 24) & 0xff);
    body[k++] = (char) ((len >> 16) & 0xff);
    body[k++] = (char) ((len >> 8) & 0xff);
    body[k++] = (char) (len & 0xff);

    memcpy(&body[k++], buf, (size_t) len);
    packet->is_video = 1;
    packet->is_key = videoFrameIsKey(buf);
    packet->is_format = 0;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = (uint32_t) body_size;
    packet->m_nChannel = STREAM_CHANNEL_VIDEO;
    packet->m_nTimeStamp = RTMP_GetTime() - startTime;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    //添加到队列
    rtmpPacketPush(packet);
}


//推视频格式
void RtmpPusher::pushVideoFormat(char *pps, char *sps, int pps_len, int sps_len) {
    if (!rtmpPusher || !RTMP_IsConnected(rtmpPusher)) {
        ALOGI("writeSpsPpsFrame RTMP_IsConnected ? false");
        if (isPushing())
            backFail();
        return;
    }
    if (requestStop) {
        return;
    }
    int body_size = 13 + sps_len + 3 + pps_len;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    if (!packet) {
        return;
    }
    RTMPPacket_Reset(packet);
    if (!RTMPPacket_Alloc(packet, (uint32_t) body_size)) {
        RTMPPacket_Free(packet);
        return;
    }
    char *body = packet->m_body;
    int k = 0;
    body[k++] = 0x17;
    body[k++] = 0x00;
    body[k++] = 0x00;
    body[k++] = 0x00;
    body[k++] = 0x00;

    body[k++] = 0x01;
    body[k++] = sps[1];
    body[k++] = sps[2];
    body[k++] = sps[3];
    body[k++] = (char) 0xff;

    // sps信息
    body[k++] = (char) 0xe1;
    body[k++] = (char) ((sps_len >> 8) & 0xff);
    body[k++] = (char) (sps_len & 0xff);
    memcpy(&body[k], sps, (size_t) sps_len);
    k += sps_len;

    //pps
    body[k++] = 0x01;
    body[k++] = (char) ((pps_len >> 8) & 0xff);
    body[k++] = (char) (pps_len & 0xff);
    memcpy(&body[k], pps, (size_t) pps_len);
    k += pps_len;

    // 设置参数
    packet->is_format = 1;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = (uint32_t) body_size;
    packet->m_nChannel = STREAM_CHANNEL_VIDEO;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;

    //添加到队列中
    rtmpPacketPush(packet);
}

//添加音频
void RtmpPusher::pushAudioFrame(char *buffer, int length, long time) {
    if (!rtmpPusher || !RTMP_IsConnected(rtmpPusher)) {
        ALOGI("pushAudioFrame RTMP_IsConnected ? false");
        if (isPushing())
            backFail();
        return;
    }
    if (requestStop) {
        return;
    }
    int body_size = length + 2;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    if (!packet) {
        return;
    }
    RTMPPacket_Reset(packet);
    if (!RTMPPacket_Alloc(packet, (uint32_t) body_size)) {
        RTMPPacket_Free(packet);
        return;
    }
    // 设置RTMP参数
    char *body = packet->m_body;
    body[0] = (char) 0xaf;
    body[1] = 0x01;
    memcpy(&body[2], buffer, (size_t) length);
    packet->is_video = 0;
    packet->is_key = 0;
    packet->is_format = 0;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = (uint32_t) body_size;
    packet->m_nChannel = STREAM_CHANNEL_AUDIO;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = RTMP_GetTime() - startTime;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    // 添加入队
    rtmpPacketPush(packet);
}


//推音频格式
void RtmpPusher::pushAudioFormat(char *data, int length) {
    if (!rtmpPusher || !RTMP_IsConnected(rtmpPusher)) {
        ALOGI("pushAudioFormat RTMP_IsConnected ? false");
        if (isPushing())
            backFail();
        return;
    }
    if (requestStop) {
        return;
    }
    int body_size = length + 2;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    if (!packet) {
        return;
    }
    RTMPPacket_Reset(packet);
    if (!RTMPPacket_Alloc(packet, body_size)) {
        RTMPPacket_Free(packet);
        return;
    }
    char *body = packet->m_body;
    body[0] = (char) 0xaf;
    body[1] = 0x00;
    memcpy(&body[2], data, (size_t) length);
    packet->is_format = 1;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = (uint32_t) body_size;
    packet->m_nChannel = STREAM_CHANNEL_AUDIO;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    rtmpPacketPush(packet);
}

/**
 * 请求暂停
 */
void RtmpPusher::callPause() {
    if (isPause)
        return;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Reset(packet);
    packet->is_pause = TRUE;
    rtmpPacketPush(packet);
}

/**
 * 请求继续
 */
void RtmpPusher::callResume() {
    if (!isPause)
        return;
    connectRTMP();
    isPause = FALSE;
}


/**
 * 设置RTMP对象
 * @param pusher
 */
void RtmpPusher::setRtmpPusher(RTMP *pusher) {
    rtmpPusher = pusher;
}
/**
 * 是否处于停止状态
 * @return
 */
int RtmpPusher::isStop() {
    return requestStop;
}

/**
 * 是否处于推流状态
 * @return
 */
int RtmpPusher::isPushing() {
    return pushing;
}

/**
 * 获取推流器
 * @return
 */
RTMP *RtmpPusher::getRtmpPusher() {
    return rtmpPusher;
}

void RtmpPusher::backFail() {
    if (callback) {
        callback(Fail);
    }
    stopPush();
}
void RtmpPusher::backLost() {
    if (callback)
        callback(Lost);
}

void RtmpPusher::backConnect() {
    if (callback)
        callback(Connect);
}





/*uint8_t   m_headerType;
// ChunkMsgHeader类型(4种)
uint8_t   m_packetType;
// Message type ID（1-7协议控制；8，9音视频；10以后为AMF编码消息）
uint8_t   m_hasAbsTimestamp;
// Timestamp 是绝对值还是相对值？
int         m_nChannel;
// 块流ID (3 <= ID <= 65599)
uint32_t m_nTimeStamp;
// Timestamp，时间戳
int32_t   m_nInfoField2;
// last 4 bytes in a long header，消息流ID
uint32_t  m_nBodySize;
// 消息长度
uint32_t  m_nBytesRead;
// 该RTMP包数据已经读取到m_body中的字节数
RTMPChunk *m_chunk;
char      *m_body;
// 存放实际消息数据的缓冲区
 */

//adb logcat | D:\android_sdk\ndk-bundle\ndk-stack.cmd -sym D:\Work\lzc_library\yioks_record\app\build\intermediates\transforms\mergeJniLibs\debug\0\lib\armeabi-v7a

}

int RtmpPusher::connectRTMP() {
    requestStop = 0;
    if (rtmpPusher)
        releasePush();
    isConnect = TRUE;
    // 1、初始化RTMP
    rtmpPusher = RTMP_Alloc();
    RTMP_Init(rtmpPusher);
    rtmpPusher->Link.timeout = 50;
    rtmpPusher->Link.flashVer = RTMP_DefaultFlashVer;
    ALOGI("推流地址   %s", pushUrl);
    // 2、设置url地址
    if (!RTMP_SetupURL(rtmpPusher, pushUrl)) {
        ALOGI("RTMP_SetupURL fail   %s", pushUrl);
        return 0;
    }
    RTMP_EnableWrite(rtmpPusher);
    // 3、建立连接
    if (!RTMP_Connect(rtmpPusher, NULL)) {
        ALOGI("RTMP_Connect fail");
        return 0;
    }
    // 4、连接流
    if (!RTMP_ConnectStream(rtmpPusher, 0)) {
        ALOGI("RTMP_ConnectStream fail");
        return 0;
    }
    ALOGI("RTMP_Connect 成功");
    backConnect();
    isConnect = FALSE;
    startTime = (long) RTMP_GetTime();
    return 1;
}



