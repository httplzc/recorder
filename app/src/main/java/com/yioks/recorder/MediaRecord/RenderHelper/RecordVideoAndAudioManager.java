package com.yioks.recorder.MediaRecord.RenderHelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.view.Surface;
import android.view.SurfaceView;


import com.yioks.recorder.MediaRecord.Utils.FileUntil;
import com.yioks.recorder.MediaRecord.Bean.MediaFrameData;
import com.yioks.recorder.MediaRecord.Camera.CameraManager;
import com.yioks.recorder.MediaRecord.Encode.VideoAudioMerger;
import com.yioks.recorder.MediaRecord.Utils.GlUtil;

import java.io.File;
import java.io.IOException;


/**
 * Created by ${UserWrapper} on 2017/8/24 0024.
 * 录制短视频控制器
 */

public class RecordVideoAndAudioManager {
    //录音控制器
    private RecordAudioManager recordAudioManager;
    //录视频控制器
    private RecordVideoManager recordVideoManager;
    //合并音频和视频
    private VideoAudioMerger videoAudioMerger;
    //相机控制器
    private CameraManager cameraManager;
    //容器
    private SurfaceView surfaceView;
    //最终版视频文件
    private File file;
    //最终版视频第一帧图片
    private File videoImg;

    //openGl es 绘图
    private GlRenderManager glRenderManager;

    private SurfaceTexture surfaceTexture;


    //录制视频配置
    private RecordSetting recordSetting;


    //音频采样率
    private final static int AUDIO_SAMPLE_RATE = 44100;
    //编码码率
    private final static int AUDIO_RATE = 64000;
    //视频清晰度Level
    private static final float bitRatio = 2.2f;
    //视频默认帧率
    private static final int FPS = 30;


    private CallBackEvent callBackEvent;

    private int startSucceedCount = 0;
    private int stopSucceedCount = 0;
    private int formatConfirmSucceedCount = 0;

    //录制方向
    private int recordOrientation = 0;

    //是否暂停
    private boolean pause;

    //是否是录制状态
    private boolean isRecord;


    private boolean needVideo = true;
    private boolean needAudio = true;

    private FpsCallBack fpsCallBack;

    private Context context;


    public RecordVideoAndAudioManager(Context context, File file, File videoImg, final RecordSetting recordSetting, CameraManager cameraManager) {
        this.file = file;
        this.videoImg = videoImg;
        this.context = context;
        this.recordSetting = recordSetting;
        this.cameraManager = cameraManager;
        recordVideoManager = new RecordVideoManager(context);
        recordAudioManager = new RecordAudioManager(context);
        recordAudioManager.setEvent(new RecordAudioManager.CallBackEvent() {
            @Override
            public void startRecordAudio() {
                startSucceedCount++;
                if (startSucceedCount == 2) {
                    callBackEvent.startRecordSuccess();
                }
            }

            @Override
            public void recordAudioError(String errorMsg) {
                cancelRecord();
                release(true);
                callBackEvent.recordError(errorMsg);
            }

            @Override
            public void recordAudioFinish() {
                stopSucceedCount++;
                if (stopSucceedCount == 2) {
                    videoAudioMerger.shutdownCompoundVideo();
                }
            }

            @Override
            public int formatConfirm(MediaFormat mediaFormat) {

                int trace = -1;
                try {
                    if (needAudio) {
                        trace = videoAudioMerger.addTrack(mediaFormat);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    cancelRecord();
                    release(true);
                    callBackEvent.recordError("添加音频轨失败！");
                }
                formatConfirmSucceedCount++;
                if (formatConfirmSucceedCount == 2)
                    videoAudioMerger.start();
                return trace;
            }

            @Override
            public void frameAvailable(MediaFrameData frameData) {
                if (needAudio)
                    videoAudioMerger.frameAvailable(frameData, recordAudioManager.getTrack());
            }


        });
        recordVideoManager.setEvent(new RecordVideoManager.CallBackEvent() {
            @Override
            public void startRecordSucceed() {
                startSucceedCount++;
                if (startSucceedCount == 2) {
                    callBackEvent.startRecordSuccess();
                }
            }


            @Override
            public void onDuringUpdate(float time) {
                callBackEvent.onDuringUpdate(time);
                if (time - recordSetting.desiredSpanSec >= 0.0001) {
                    RecordVideoAndAudioManager.this.stopRecord();
                }
            }

            @Override
            public void recordVideoFinish() {
                stopSucceedCount++;
                if (stopSucceedCount == 2) {
                    videoAudioMerger.shutdownCompoundVideo();
                }
            }

            @Override
            public void recordVideoError(String errorMsg) {
                cancelRecord();
                release(true);
            }

            @Override
            public int formatConfirm(MediaFormat mediaFormat) {
                int trace = -1;
                try {
                    if (needVideo)
                        trace = videoAudioMerger.addTrack(mediaFormat);
                } catch (Exception e) {
                    e.printStackTrace();
                    cancelRecord();
                    release(true);
                    callBackEvent.recordError("添加音频轨失败！");
                }
                formatConfirmSucceedCount++;
                if (formatConfirmSucceedCount == 2)
                    videoAudioMerger.start();
                return trace;
            }

            @Override
            public void frameAvailable(MediaFrameData frameData) {
                if (needVideo)
                    videoAudioMerger.frameAvailable(frameData, recordVideoManager.getTrack());
            }


        });

        release(false);
        resetFile();
        videoAudioMerger = new VideoAudioMerger(file, new VideoAudioMerger.CallBack() {

            @Override
            public void compoundFail(String msg) {
                release(true);
                callBackEvent.recordError(msg);

            }

            @Override
            public void compoundSuccess(File file) {
                release(true);
                saveVideoImg(file);

                callBackEvent.stopRecordFinish(file);
            }
        });
    }


    public void startRecord() throws Exception {
        startSucceedCount = 0;
        stopSucceedCount = 0;
        formatConfirmSucceedCount = 0;
        glRenderManager.setmRecordWidth(getRecordSetting().width);
        glRenderManager.setmRecordHeight(getRecordSetting().height);
        if (!videoAudioMerger.initCompoundVideo()) {
            callBackEvent.recordError("开始合成器失败!");
            return;
        }
        boolean succeed = recordVideoManager.startRecord(recordSetting.width, recordSetting.height,
                recordSetting.frameRate, recordSetting.videoBitRate, recordSetting.colorFormat);
        if (succeed)
            recordAudioManager.startRecord(recordSetting.audioSampleRate, recordSetting.audioBitRate, recordSetting.desiredSpanSec);
        Surface surface = getRecordVideoManager().getInputSurface();
        glRenderManager.setEncoderSurface(surface);

        isRecord = true;
    }

    public void stopRecord() {
        isRecord = false;
        recordVideoManager.stopRecord();
        recordAudioManager.stopRecord();
    }

    public void cancelRecord() {
        isRecord = false;
        videoAudioMerger.cancelCompoundVideo();
        recordVideoManager.cancelRecord();
        recordAudioManager.cancelRecord();

    }

    public void destroyRecord() {
        isRecord = false;
        videoAudioMerger.cancelCompoundVideo();
        recordVideoManager.cancelRecord();
        recordAudioManager.cancelRecord();

        if (glRenderManager != null)
            glRenderManager.release();
        if (surfaceTexture != null)
            surfaceTexture.release();
    }

    private void release(boolean releaseData) {
        recordVideoManager.releaseRecord(releaseData);
        recordAudioManager.releaseRecord(releaseData);
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

    //初始化，配置帧 回调
    public SurfaceTexture init(SurfaceView surfaceViewP) {
        int textureId = GlUtil.createRecordCameraTextureID();
        surfaceTexture = new SurfaceTexture(textureId);
        this.surfaceView = surfaceViewP;
        try {
            glRenderManager = new GlRenderManager(context, textureId, surfaceView.getHolder().getSurface(), surfaceTexture);
        } catch (GlUtil.OpenGlException e) {
            e.printStackTrace();
            return null;
        }
        surfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);
        // asdasd();
        return surfaceTexture;


    }


    //一帧准备完毕
    private void callFrameAvailable() {
        if (pause)
            return;
        surfaceView.post(new Runnable() {
            @Override
            public void run() {
                //转换方向
                int recordRotate = recordOrientation;
                if (recordRotate == 90)
                    recordRotate = 270;
                else if (recordRotate == 270)
                    recordRotate = 90;
                try {
                    glRenderManager.drawFrame(isRecord, recordRotate, cameraManager.getCameraPosition() == 1);
                    if (isRecord) {
                        getRecordVideoManager().callRecordFrameAvailable();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }


    //回调帧准备完毕
    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
            callFrameAvailable();
            if (fpsCallBack != null)
                fpsCallBack.onFrameDraw(FrameRateMeter.getInstance().getFPS());
        }
    };

    public void pause() {
        pause = true;
    }

    public void resume() {
        pause = false;
    }

    public void onDisplayChanged(int width, int height) {
        if (glRenderManager != null)
            glRenderManager.onDisplaySizeChanged(width, height);
    }

    public void onInputSizeChanged(int width, int height) {
        if (glRenderManager != null)
            glRenderManager.onInputSizeChanged(width, height);
    }


    public interface CallBackEvent {

        void startRecordSuccess();

        void onDuringUpdate(float time);

        void stopRecordFinish(File file);

        void recordError(String errorMsg);

    }

    public File getVideoImg() {
        return videoImg;
    }

    public void setVideoImg(File videoImg) {
        this.videoImg = videoImg;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public RecordSetting getRecordSetting() {
        return recordSetting;
    }

    public void setRecordSetting(RecordSetting recordSetting) {
        this.recordSetting = recordSetting;
    }

    public CallBackEvent getCallBackEvent() {
        return callBackEvent;
    }

    public void setCallBackEvent(CallBackEvent callBackEvent) {
        this.callBackEvent = callBackEvent;
    }

    public static class RecordSetting {

        public RecordSetting(int desiredSpanSec) {
            this.desiredSpanSec = desiredSpanSec;
        }


        public void setVideoSetting(int width, int height, int frameRate, int colorFormat) {
            this.width = width;
            this.height = height;
            this.frameRate = frameRate;
            this.videoBitRate = (int) (width * height * bitRatio);
            this.colorFormat = colorFormat;
        }

        public void setAudioSetting(int audioSampleRate, int audioBitRate) {
            this.audioSampleRate = audioSampleRate;
            this.audioBitRate = audioBitRate;
        }

        //视频宽
        public int width = 0;
        //视频高
        public int height = 0;
        //视频比特率
        public int videoBitRate;
        //视频帧率
        public int frameRate = FPS;
        //预计时常
        public int desiredSpanSec;
        //音频采样率
        public int audioSampleRate = AUDIO_SAMPLE_RATE;
        ;
        // 音频比特率
        public int audioBitRate = AUDIO_RATE;
        //视频颜色类型
        public int colorFormat;
    }

    public RecordAudioManager getRecordAudioManager() {
        return recordAudioManager;
    }

    public RecordVideoManager getRecordVideoManager() {
        return recordVideoManager;
    }

    public int getRecordOrientation() {
        return recordOrientation;
    }

    public void setRecordOrientation(int recordOrientation) {
        this.recordOrientation = recordOrientation;
    }

    public GlRenderManager getGlRenderManager() {
        return glRenderManager;
    }

    public void switchOnBeauty(boolean enable) {
        glRenderManager.setBeautyEnable(enable);
    }

    public boolean isBeautyEnable() {
        return glRenderManager != null && glRenderManager.isBeautyEnable();
    }

    public interface FpsCallBack {
        void onFrameDraw(float fps);
    }

    public FpsCallBack getFpsCallBack() {
        return fpsCallBack;
    }

    public void setFpsCallBack(FpsCallBack fpsCallBack) {
        this.fpsCallBack = fpsCallBack;
    }

    public void setGlBackBitmap(GlRenderManager.GlBackBitmap glBackBitmap) {
        if (glRenderManager != null)
            glRenderManager.setGlBackBitmap(glBackBitmap);
    }

    public void startGetGlImg() {
        if (glRenderManager != null)
            glRenderManager.setTakePhoto(true);
    }

    /**
     * 静音
     */
    public void soundOff(boolean off) {
        if (recordAudioManager != null&&recordAudioManager.getAudioEncoder()!=null)
            recordAudioManager.getAudioEncoder().setSoundOff(off);
    }
}
