package com.yioks.recorder.LiveRecord;

import android.graphics.SurfaceTexture;

import com.yioks.recorder.MediaRecord.Bean.RecordSetting;
import com.yioks.recorder.MediaRecord.RecorderHelper.GlRenderManager;
import com.yioks.recorder.MediaRecord.RecorderHelper.RecordAudioManager;
import com.yioks.recorder.MediaRecord.RecorderHelper.RecordVideoManager;

/**
 * Created by Lzc on 2018/3/22 0022.
 * 直播控制器
 */

public class LiveManager {
    //录音控制器
    private RecordAudioManager recordAudioManager;
    //录视频控制器
    private RecordVideoManager recordVideoManager;
    //推流控制器
    private LivePushManager pushManager;

    //openGl es 绘图
    private GlRenderManager glRenderManager;

    private SurfaceTexture surfaceTexture;

    //录制视频配置
    private RecordSetting recordSetting;

    private int cameraPosition = 0;


    public interface CallBackEvent {

        void startLiveSuccess();

        void stopLive();

        void liveError(String errorMsg);

    }

}
