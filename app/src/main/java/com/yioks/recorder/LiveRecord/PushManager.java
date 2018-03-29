package com.yioks.recorder.LiveRecord;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Created by Lzc on 2018/3/24 0024.
 */

public class PushManager {
    private HandlerThread pushThread;
    private PushHandler pushHandler;
    private CallBackHandler callBackHandler;
    private String pushUrl;
    private Pusher pusher;

    private final static int PushStart = 0;
    private final static int PushStop = 1;
    private final static int PushVideoFrame = 2;
    private final static int PushAudioFrame = 3;
    private final static int PushVideoFormat = 4;
    private final static int PushAudioFormat = 5;


    private final static int CallBackStartSucceed = 0;
    private final static int CallBackFail = 1;
    private final static int CallBackBusy = 2;

    private boolean isStop=true;

    private CallBack callBack;

    public PushManager(String pushUrl) {
        this.pushUrl = pushUrl;
        pusher = new Pusher();
        pusher.setCallBack(new Pusher.Callback() {
            @Override
            public void fail() {
                callBackHandler.sendMessage(Message.obtain(callBackHandler, CallBackFail));
            }

            @Override
            public void lostPack() {
                callBackHandler.sendMessage(Message.obtain(callBackHandler, CallBackBusy));
            }
        });
        this.pushThread = new HandlerThread("pushThread");
        pushThread.start();
        isStop=false;
        pushHandler = new PushHandler(pushThread.getLooper(), this);
        callBackHandler = new CallBackHandler(this);
    }


    public void pushAudioFormat(byte[] data) {
        if(isStop)
            return;
        pushHandler.sendMessage(Message.obtain(pushHandler, PushAudioFormat, data));
    }

    public void pushVideoFormat(byte[] pps, byte[] sps) {
        if(isStop)
            return;
        byte data[][] = new byte[2][];
        data[0] = pps;
        data[1] = sps;
        pushHandler.sendMessage(Message.obtain(pushHandler, PushVideoFormat, data));
    }

    public void pushVideoFrame(byte[] data, long presentationTimeUs) {
        if(isStop)
            return;
        Object[]objects=new Object[2];
        objects[0]=data;
        objects[1]=presentationTimeUs;
        pushHandler.sendMessage(Message.obtain(pushHandler, PushVideoFrame, objects));
    }

    public void pushAudioFrame(byte[] data, long presentationTimeUs) {
        if(isStop)
            return;
        Object[]objects=new Object[2];
        objects[0]=data;
        objects[1]=presentationTimeUs;
        pushHandler.sendMessage(Message.obtain(pushHandler, PushAudioFrame, objects));
    }

    public void stop() {
        if(isStop)
            return;
        pushHandler.sendMessage(Message.obtain(pushHandler, PushStop));
        callBack=null;
        isStop=true;
    }

    public void start() {
        pushHandler.sendMessage(Message.obtain(pushHandler, PushStart, pushUrl));
    }

    private static class PushHandler extends Handler {
        private WeakReference<PushManager> weakReference;


        public PushHandler(Looper looper, PushManager pushManager) {
            super(looper);
            weakReference = new WeakReference<PushManager>(pushManager);
        }

        @Override
        public void handleMessage(Message msg) {
            PushManager pushManager = weakReference.get();
            if (pushManager == null)
                return;
            CallBackHandler callbackHandler = pushManager.callBackHandler;
            switch (msg.what) {
                case PushStart:
                    int result = pushManager.pusher.start((String) msg.obj);
                    if (result == 0)
                        sendSucceed();
                    else
                        sendFail();
                    break;
                case PushStop:
                    pushManager.pusher.stop();
                    pushManager.pushThread.quitSafely();
                    break;
                case PushVideoFrame:
                    Object objectsVideo[]= (Object[]) msg.obj;
                    pushManager.pusher.pushVideoFrame((byte[]) objectsVideo[0],(long)objectsVideo[1]);
                    break;
                case PushAudioFrame:
                    Object objectsAudio[]= (Object[]) msg.obj;
                    pushManager.pusher.pushAudioFrame((byte[])objectsAudio[0],(long) objectsAudio[1]);
                    break;
                case PushVideoFormat:
                    byte array[][] = (byte[][]) msg.obj;
                    pushManager.pusher.pushVideoFormat(array[0], array[1]);
                    break;
                case PushAudioFormat:
                    pushManager.pusher.pushAudioFormat((byte[]) msg.obj);
                    break;
            }
            super.handleMessage(msg);
        }


        private void sendSucceed() {
            PushManager pushManager = weakReference.get();
            if (pushManager == null)
                return;
            CallBackHandler callbackHandler = pushManager.callBackHandler;
            callbackHandler.sendMessage(Message.obtain(callbackHandler, CallBackStartSucceed));
        }

        private void sendFail() {
            PushManager pushManager = weakReference.get();
            if (pushManager == null)
                return;
            CallBackHandler callbackHandler = pushManager.callBackHandler;
            callbackHandler.sendMessage(Message.obtain(callbackHandler, CallBackFail));

        }

        private void sendBusy() {
            PushManager pushManager = weakReference.get();
            if (pushManager == null)
                return;
            CallBackHandler callbackHandler = pushManager.callBackHandler;
            callbackHandler.sendMessage(Message.obtain(callbackHandler, CallBackBusy));
        }
    }

    private static class CallBackHandler extends Handler {
        private WeakReference<PushManager> weakReference;

        public CallBackHandler(PushManager pushManager) {
            weakReference = new WeakReference<PushManager>(pushManager);
        }

        @Override
        public void handleMessage(Message msg) {
            PushManager pushManager = weakReference.get();
            if (pushManager == null)
                return;
            CallBack callBack = pushManager.callBack;
            if (callBack == null)
                return;
            switch (msg.what) {
                case CallBackStartSucceed:
                    callBack.startSucceed();
                    break;
                case CallBackFail:
                    callBack.fail();
                    break;
                case CallBackBusy:
                    callBack.lostPacket();
                    break;
            }
            super.handleMessage(msg);
        }
    }

    public interface CallBack {
        void startSucceed();

        void fail();

        void lostPacket();

    }

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }
}
