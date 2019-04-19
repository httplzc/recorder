package com.yioks.recorder.LiveRecord;

import android.app.Activity;
import android.media.MediaFormat;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.SurfaceView;

import com.yioks.recorder.Bean.CameraSetting;
import com.yioks.recorder.Bean.MediaFrameData;
import com.yioks.recorder.Bean.RecordSetting;
import com.yioks.recorder.Bean.RenderSetting;
import com.yioks.recorder.RecorderHelper.RecordManageBase;

import java.util.Arrays;

/**
 * Created by Lzc on 2018/3/22 0022.
 */

public class LiveManager extends RecordManageBase {
    private PushManager pusherManager;
    private PushManager.CallBack callBack;


    public LiveManager(Activity context, @Nullable RecordSetting recordSetting, @Nullable CameraSetting cameraSetting,
                       @Nullable RenderSetting renderSetting, SurfaceView surfaceView) {
        super(context, recordSetting, cameraSetting, renderSetting, surfaceView);
    }


    @Override
    public void startRecord() {
        super.startRecord();
        onRecordStart();
    }

    private void onRecordStart() {
        if (TextUtils.isEmpty(recordSetting.pushUrl)) {
            callLiveError();
            return;
        }
        if (pusherManager == null) {
            pusherManager = new PushManager(recordSetting.pushUrl);
            pusherManager.setCallBack(new PushManager.CallBack() {
                @Override
                public void connect() {
                    if (callBack != null)
                        callBack.connect();
                }

                @Override
                public void fail() {
                    cancelRecord();
                    if (callBack != null)
                        callBack.fail();
                }

                @Override
                public void lostPacket() {
                    if (callBack != null)
                        callBack.lostPacket();
                }
            });
        }
        pusherManager.start();
    }

    private void callLiveError() {
        cancelRecord();
        if (callBackEvent != null)
            callBackEvent.recordError("推流地址错误！");
    }

    @Override
    protected void onFrameAvailable(DataType type, MediaFrameData frameData) {
        if (!isRecord)
            return;
        if (type == DataType.Type_Audio) {
            pusherManager.pushAudioFrame(frameData.getmDataBuffer(), frameData.getInfo().presentationTimeUs);
        } else {
            pusherManager.pushVideoFrame(frameData.getmDataBuffer(), frameData.getInfo().presentationTimeUs);
        }
    }

    @Override
    protected int onFormatConfirm(DataType type, MediaFormat mediaFormat) {
        if (!isRecord)
            return type.ordinal();
        if (type == DataType.Type_Audio) {
            pusherManager.pushAudioFormat(mediaFormat.getByteBuffer("csd-0").array());
        } else {
            byte[] sps = mediaFormat.getByteBuffer("csd-0").array();
            byte[] pps = mediaFormat.getByteBuffer("csd-1").array();
            pusherManager.pushVideoFormat(Arrays.copyOfRange(pps, 4, pps.length), Arrays.copyOfRange(sps, 4, sps.length));
        }
        return type.ordinal();
    }

    public PushManager.CallBack getPushCallBack() {
        return callBack;
    }

    public void setPushCallBack(PushManager.CallBack callBack) {
        this.callBack = callBack;
    }

    private void destroyPusher()
    {
        if (pusherManager != null) {
            pusherManager.stop();
            pusherManager = null;
        }
    }

    @Override
    public void cancelRecord() {
        super.cancelRecord();
        destroyPusher();
    }


    @Override
    protected void onRecordStop() {
        destroyPusher();
    }

    @Override
    public void destroy() {
        super.destroy();
        destroyPusher();
    }

    @Override
    public void init() {
        super.init();
    }

    public void pause() {
        if (pusherManager != null)
            pusherManager.pause();
        super.cancelRecord();
        if (cameraManager != null)
            cameraManager.freeCameraResource();

    }

    public PushManager.PushStatus getPushStatus() {
        return pusherManager.getPushStatus();
    }


}
