package com.yioks.recorder.LiveRecord;

/**
 * Created by Lzc on 2018/3/22 0022.
 */

public class Pusher {
    private Callback callback;

    static {
        System.loadLibrary("pusher");
    }


    public native void pushAudioFormat(byte[] data);

    public native void pushVideoFormat(byte[] pps, byte[] sps);

    public native void pushVideoFrame(byte[] data,long time);

    public native void pushAudioFrame(byte[] data,long time);

    public native void stop();

    public native int start(String url);

    public  void fail()
    {
        if(callback!=null)
            callback.fail();
    }

    public void lostPack()
    {
        if(callback!=null)
            callback.lostPack();;
    }

    public void setCallBack(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void fail();

        void lostPack();
    }

}
