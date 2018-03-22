package com.yioks.recorder.LiveRecord;

import android.app.Activity;
import android.media.MediaFormat;
import android.support.annotation.Nullable;
import android.view.SurfaceView;

import com.yioks.recorder.MediaRecord.Bean.CameraSetting;
import com.yioks.recorder.MediaRecord.Bean.MediaFrameData;
import com.yioks.recorder.MediaRecord.Bean.RecordSetting;
import com.yioks.recorder.MediaRecord.RecorderHelper.RecordManageBase;

/**
 * Created by Lzc on 2018/3/22 0022.
 */

public class LivePushManager extends RecordManageBase{
    private Pusher pusher;
    public LivePushManager(Activity context, @Nullable RecordSetting recordSetting, @Nullable CameraSetting cameraSetting, SurfaceView surfaceView) {
        super(context, recordSetting, cameraSetting, surfaceView);
        pusher=new Pusher();
    }

    @Override
    protected void onRecordStop() {

    }

    @Override
    protected void onRecordStart() {
        pusher=new Pusher();
    }

    @Override
    protected void onFrameAvailable(DataType type, MediaFrameData frameData) {

    }

    @Override
    protected int onFormatConfirm(DataType type, MediaFormat mediaFormat) {
        return 0;
    }
}
