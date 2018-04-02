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
int RtmpPusher::initRtmp(char *url) {
    // 1、初始化RTMP
    rtmpPusher = RTMP_Alloc();
    RTMP_Init(rtmpPusher);
    rtmpPusher->Link.timeout = 500;
    rtmpPusher->Link.flashVer = RTMP_DefaultFlashVer;

    // 2、设置url地址
    if (!RTMP_SetupURL(rtmpPusher, (char *) url)) {
        RTMP_Close(rtmpPusher);
        RTMP_Free(rtmpPusher);
        ALOGI("RTMP_SetupURL fail");
        return -1;
    }
    RTMP_EnableWrite(rtmpPusher);
    // 3、建立连接
    if (!RTMP_Connect(rtmpPusher, NULL)) {
        RTMP_Close(rtmpPusher);
        RTMP_Free(rtmpPusher);
        ALOGI("RTMP_Connect fail");
        return -1;
    }
    // 4、连接流
    if (!RTMP_ConnectStream(rtmpPusher, 0)) {
        RTMP_Close(rtmpPusher);
        RTMP_Free(rtmpPusher);
        ALOGI("RTMP_ConnectStream fail");
        return -1;
    }

    // 创建队列
    create_queue();
    startTime = (long) RTMP_GetTime();

    pushing = 1;
    mutex = PTHREAD_MUTEX_INITIALIZER;
    cond = PTHREAD_COND_INITIALIZER;
    pthread_create(&publisher_tid, NULL, RtmpPusher::rtmpPushThread, this);
    ALOGI("RTMP_Connect 成功");
    return 0;
}

int RtmpPusher::reConnect() {
    int reCount = 0;
    while (!RTMP_ReconnectStream(rtmpPusher, 0) && reCount < 4) {
        reCount++;
    }
    if (reCount >= 4) {
        ALOGI("RTMP ReConnect failed!");
    } else {
        ALOGI("RTMP ReConnect succeed!");
    }
    return reCount >= 4 ? 0 : 1;
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

    while (pusher->isPushing()) {
        pthread_mutex_lock(&pusher->mutex);
        pthread_cond_wait(&pusher->cond, &pusher->mutex);
        // 请求停止，则立马跳出循环
        if (pusher->isStop()) {
            break;
        }
        // 如果不处于pushing状态，则继续等待
        if (!pusher->isPushing()) {
            continue;
        }
        int size = queue_size() > 10 ? 10 : queue_size();
        RTMPPacket **packets = new RTMPPacket *[size];
        for (int i = 0; i < size; i++) {
            packets[i] = (RTMPPacket *) queue_get_head();
            queue_remove();
        }
        pthread_mutex_unlock(&pusher->mutex);
        // 发送RTMP包，推流操作
        for (int i = 0; i < size; i++) {
            int64_t time1 = getCurrentTime();
            RTMPPacket *packet = packets[i];
            if (packet) {
                if (RTMP_SendPacket(pusher->rtmpPusher, packet, TRUE)) {
                    int64_t time2 = getCurrentTime();
//                    if (time2 - time1 > 1000) {
//                        ALOGI("sendCount %d", count);
//                        count = 0;
//                        time1=getCurrentTime();
//                    }
                    ALOGI("RTMP_SendPacket success! %d   %d", queue_size(), (time2 - time1));
                } else {
                    ALOGI("RTMP_SendPacket failed!");
                }
                RTMPPacket_Free(packet);
            }
        }
    }
    // 如果请求停止操作，则释放RTMP等资源，以防内存泄漏
    if (pusher->requestStop) {
        RtmpPusher::nativeStop(pusher);
    }
    delete pusher;
    ALOGI("RTMP_SendPacket stop!");
    return NULL;
}
/**
 * 停止推流
 */
void RtmpPusher::stop() {
    pushing = 0;
    requestStop = 1;
}

/**
 * 释放资源
 */
void RtmpPusher::nativeStop(RtmpPusher *pusher) {
    destroy_queue();
    if (pusher->getRtmpPusher() && RTMP_IsConnected(pusher->getRtmpPusher())) {
        RTMP_Close(pusher->getRtmpPusher());
        RTMP_Free(pusher->getRtmpPusher());
        pusher->setRtmpPusher(NULL);
    }
}

/**
 * 添加RTMPPacket数据包到队列
 * @param packet
 */
void RtmpPusher::rtmpPacketPush(RTMPPacket *packet) {
    pthread_mutex_lock(&mutex);
    if (queue_size() > 700) {
        ALOGI("推流超时");
        backFail();
        return;
    }
        // 丢包操作
    else if (queue_size() > 50) {
        for (int i = 0; i < 25; i++) {
            RTMPPacket *temp = (RTMPPacket *) queue_get(i);
            if (temp->is_video && temp->is_key)
                continue;
            queue_delete(i);
        }
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
        if (reConnect() && isPushing())
            backFail();
        return;
    }

    if (!pushing || requestStop) {
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
    ALOGI("isKeyFrame   %d",packet->is_key);
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
        if (reConnect() && isPushing())
            backFail();
        return;
    }
    if (!pushing || requestStop) {
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
    if (!pushing || requestStop) {
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
    if (!pushing || requestStop) {
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

    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = (uint32_t) body_size;
    packet->m_nChannel = STREAM_CHANNEL_AUDIO;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    rtmpPacketPush(packet);
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
    stop();
    if (callback != nullptr)
        callback(Fail);
}
void RtmpPusher::backLost() {
    if (callback != nullptr)
        callback(Lost);
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

}