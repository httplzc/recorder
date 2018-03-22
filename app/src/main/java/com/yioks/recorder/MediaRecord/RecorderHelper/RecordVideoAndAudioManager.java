package com.yioks.recorder.MediaRecord.RecorderHelper;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.support.annotation.Nullable;
import android.view.SurfaceView;

import com.yioks.recorder.MediaRecord.Bean.CameraSetting;
import com.yioks.recorder.MediaRecord.Bean.MediaFrameData;
import com.yioks.recorder.MediaRecord.Bean.RecordSetting;
import com.yioks.recorder.MediaRecord.Encode.VideoAudioMerger;
import com.yioks.recorder.MediaRecord.Utils.FileUntil;

import java.io.File;
import java.io.IOException;

/**
 * Created by Lzc on 2018/3/22 0022.
 */

public class RecordVideoAndAudioManager extends RecordManageBase {
    //合并音频和视频
    private VideoAudioMerger videoAudioMerger;
    //最终版视频文件
    private File file;
    //最终版视频第一帧图片
    private File videoImg;

    private int formatConfirmSucceedCount = 0;

    public RecordVideoAndAudioManager(Activity context, File file, File videoImg,
                                      @Nullable RecordSetting recordSetting, @Nullable CameraSetting cameraSetting, SurfaceView surfaceView) {
        super(context, recordSetting, cameraSetting, surfaceView);
        this.file = file;
        this.videoImg = videoImg;
        videoAudioMerger = new VideoAudioMerger(file);
        initCallBack();
        resetFile();
    }

    private void initCallBack() {
        videoAudioMerger.setCallBack(new VideoAudioMerger.CallBack() {

            @Override
            public void compoundFail(String msg) {
                cancelRecord();
                callBackEvent.recordError(msg);

            }

            @Override
            public void compoundSuccess(File file) {
                cancelRecord();
                saveVideoImg(file);
                callBackEvent.stopRecordFinish(file);
            }
        });
    }

    @Override
    protected void onRecordStop() {
        videoAudioMerger.shutdownCompoundVideo();
    }

    @Override
    protected void onRecordStart() {
        formatConfirmSucceedCount = 0;
        if (!videoAudioMerger.initCompoundVideo()) {
            callBackEvent.recordError("开始合成器失败!");
            destroyRecord();
        }
    }

    @Override
    protected void onFrameAvailable(DataType type, MediaFrameData frameData) {
        if (type == DataType.Type_Video) {
            videoAudioMerger.frameAvailable(frameData, recordVideoManager.getTrack());
        } else {
            videoAudioMerger.frameAvailable(frameData, recordAudioManager.getTrack());
        }
    }

    @Override
    protected int onFormatConfirm(DataType type, MediaFormat mediaFormat) {
        int trace = -1;
        try {
            if (type == DataType.Type_Video) {
                if (needVideo)
                    trace = videoAudioMerger.addTrack(mediaFormat);
            } else if (type == DataType.Type_Audio) {
                if (needAudio) {
                    trace = videoAudioMerger.addTrack(mediaFormat);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            cancelRecord();
            callBackEvent.recordError("添加合并轨道失败！");
        }
        formatConfirmSucceedCount++;
        if (formatConfirmSucceedCount == MaxCount())
            videoAudioMerger.start();
        return trace;
    }


    //重置文件
    private void resetFile() {
        if (file != null) {
            try {
                FileUntil.clearFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (videoImg != null)
            try {
                FileUntil.clearFile(videoImg);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    //保存视频缩略图
    private void saveVideoImg(File file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(file.getPath());
        Bitmap bmp = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        FileUntil.saveImageAndGetFile(bmp, videoImg, -1, Bitmap.CompressFormat.JPEG);
    }

    @Override
    public void cancelRecord() {
        super.cancelRecord();
        videoAudioMerger.cancelCompoundVideo();
    }

    @Override
    public void destroyRecord() {
        super.destroyRecord();
        videoAudioMerger.cancelCompoundVideo();
    }

    public File getFile() {
        return file;
    }

    public File getVideoImg() {
        return videoImg;
    }
}
