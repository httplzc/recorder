package com.yioks.recorder.MediaRecord.RecorderHelper;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.yioks.recorder.MediaRecord.Bean.CameraSetting;
import com.yioks.recorder.MediaRecord.Bean.MediaFrameData;
import com.yioks.recorder.MediaRecord.Bean.RecordSetting;
import com.yioks.recorder.MediaRecord.Camera.CameraManager;
import com.yioks.recorder.MediaRecord.Utils.GlUtil;

import java.io.File;


/**
 * Created by lzc on 2017/8/24 0024.
 * 录制短视频控制器
 */

public abstract class RecordManageBase {
    //相机控制器
    protected CameraManager cameraManager;
    //录音控制器
    protected RecordAudioManager recordAudioManager;
    //录视频控制器
    protected RecordVideoManager recordVideoManager;

    //openGl es 绘图
    protected GlRenderManager glRenderManager;

    protected SurfaceTexture surfaceTexture;


    //录制视频配置
    protected RecordSetting recordSetting;

    protected int cameraPosition = 0;


    protected CallBackEvent callBackEvent;

    private int startSucceedCount = 0;
    private int stopSucceedCount = 0;

    //录制方向
    private int recordOrientation = 0;

    //是否暂停
    protected boolean pause;

    //是否是录制状态
    protected boolean isRecord;


    protected boolean needVideo = true;
    protected boolean needAudio = true;

    private FpsCallBack fpsCallBack;

    protected Activity context;

    protected Handler handler = new Handler();

    protected SurfaceView surfaceView;

    public enum DataType {Type_Video, Type_Audio}

    protected int MaxCount() {
        int i = 0;
        if (needAudio)
            i++;
        if (needVideo)
            i++;
        return i;
    }

    public RecordManageBase(Activity context, @Nullable RecordSetting recordSetting, @Nullable CameraSetting cameraSetting, SurfaceView surfaceView) {
        this.context = context;
        if (recordSetting == null)
            recordSetting = new RecordSetting();
        if (cameraSetting == null)
            cameraSetting = new CameraSetting();
        this.recordSetting = recordSetting;
        this.surfaceView = surfaceView;
        cameraManager = new CameraManager(cameraSetting);
        recordVideoManager = new RecordVideoManager(context);
        recordAudioManager = new RecordAudioManager(context);
        initEvent();
    }

    protected abstract void onRecordStop();

    protected abstract void onRecordStart();

    protected abstract void onFrameAvailable(DataType type, MediaFrameData frameData);

    protected abstract int onFormatConfirm(DataType type, MediaFormat mediaFormat);

    protected void initEvent() {
        cameraManager.setEvent(new CameraManager.CallBackEvent() {
            @Override
            public void openCameraSuccess(int cameraPosition) {
                callBackEvent.openCameraSuccess(cameraPosition);
                RecordManageBase.this.onDisplayChanged(surfaceView.getWidth(), surfaceView.getHeight());
                RecordManageBase.this.onInputSizeChanged(surfaceView.getWidth(), surfaceView.getHeight());
            }

            @Override
            public void openCameraFailure(int cameraPosition) {
                callBackEvent.openCameraFailure(cameraPosition);
            }

            @Override
            public void onVideoSizeChange(int width, int height) {
                callBackEvent.onVideoSizeChange(width, height);
                changeSurfaceSize(width, height, true);
            }

            @Override
            public void onPhotoSizeChange(int width, int height) {
                callBackEvent.onPhotoSizeChange(width, height);
            }
        });
        recordAudioManager.setEvent(new RecordAudioManager.CallBackEvent() {
            @Override
            public void startRecordAudio() {
                startSucceedCount++;
                if (startSucceedCount == MaxCount()) {
                    callBackEvent.startRecordSuccess();
                }
            }

            @Override
            public void recordAudioError(String errorMsg) {
                cancelRecord();
                callBackEvent.recordError(errorMsg);
            }

            @Override
            public void recordAudioFinish() {
                stopSucceedCount++;
                if (stopSucceedCount == MaxCount()) {
                    onRecordStop();
                }
            }

            @Override
            public int formatConfirm(MediaFormat mediaFormat) {
                return onFormatConfirm(DataType.Type_Audio, mediaFormat);
            }

            @Override
            public void frameAvailable(MediaFrameData frameData) {
                if (needAudio)
                    onFrameAvailable(DataType.Type_Audio, frameData);
            }


        });
        recordVideoManager.setEvent(new RecordVideoManager.CallBackEvent() {
            @Override
            public void startRecordSucceed() {
                startSucceedCount++;
                if (startSucceedCount == MaxCount()) {
                    callBackEvent.startRecordSuccess();
                }
            }


            @Override
            public void onDuringUpdate(float time) {
                callBackEvent.onDuringUpdate(time);
                if (recordSetting.desiredSpanSec != -1 && time - RecordManageBase.this.recordSetting.desiredSpanSec >= 0.0001) {
                    RecordManageBase.this.stopRecord();
                }
            }

            @Override
            public void recordVideoFinish() {
                stopSucceedCount++;
                if (stopSucceedCount == MaxCount()) {
                    onRecordStop();
                }
            }

            @Override
            public void recordVideoError(String errorMsg) {
                cancelRecord();
            }

            @Override
            public int formatConfirm(MediaFormat mediaFormat) {
                return onFormatConfirm(DataType.Type_Video, mediaFormat);
            }

            @Override
            public void frameAvailable(MediaFrameData frameData) {
                if (needVideo)
                    onFrameAvailable(DataType.Type_Video, frameData);
            }
        });

    }


    //改变surfaceView页面大小
    //type 0 满屏不留黑  1不超出边界
    private void changeSurfaceSize(int width, int height, boolean canOutSide) {
        int newWidth = 0;
        int newHeight = 0;
        DisplayMetrics metric = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getRealMetrics(metric);
        int screenWidth = metric.widthPixels;
        int screenHeight = metric.heightPixels;
        if ((width == -1 && height == -1) || (width == screenWidth && height == screenHeight)) {
            newHeight = FrameLayout.LayoutParams.MATCH_PARENT;
            newWidth = FrameLayout.LayoutParams.MATCH_PARENT;
        } else {
            float widthRatio = (float) screenWidth / width;
            float heightRadio = (float) screenHeight / height;
            if (canOutSide) {
                if (widthRatio > heightRadio) {
                    newWidth = screenWidth;
                    newHeight = (int) (height * widthRatio);
                } else {
                    newHeight = screenHeight;
                    newWidth = (int) (width * heightRadio);
                }
            } else {
                if (widthRatio > heightRadio) {
                    newHeight = screenHeight;
                    newWidth = (int) (width * heightRadio);
                } else {
                    newWidth = screenWidth;
                    newHeight = (int) (height * widthRatio);
                }
            }
        }
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) surfaceView.getLayoutParams();
        lp.width = newWidth;
        lp.height = newHeight;
        surfaceView.setLayoutParams(lp);
    }


    public void startRecord() throws Exception {
        startSucceedCount = 0;
        stopSucceedCount = 0;
        glRenderManager.setmRecordWidth(getRecordSetting().width);
        glRenderManager.setmRecordHeight(getRecordSetting().height);
        boolean succeed = recordVideoManager.startRecord(recordSetting.width, recordSetting.height,
                recordSetting.frameRate, recordSetting.videoBitRate, recordSetting.colorFormat);
        if (succeed)
            recordAudioManager.startRecord(recordSetting.audioSampleRate, recordSetting.audioBitRate, recordSetting.desiredSpanSec);
        Surface surface = getRecordVideoManager().getInputSurface();
        glRenderManager.setEncoderSurface(surface);
        onRecordStart();
        isRecord = true;
    }

    /**
     * 停止
     */
    public void stopRecord() {
        isRecord = false;
        recordVideoManager.stopRecord();
        recordAudioManager.stopRecord();
    }

    /**
     * 取消
     */
    public void cancelRecord() {
        isRecord = false;
        recordVideoManager.cancelRecord();
        recordAudioManager.cancelRecord();

    }

    /**
     * 销毁
     */
    public void destroyRecord() {
        isRecord = false;
        cameraManager.freeCameraResource();
        recordVideoManager.cancelRecord();
        recordAudioManager.cancelRecord();
        if (glRenderManager != null)
            glRenderManager.release();
        if (surfaceTexture != null)
            surfaceTexture.release();
    }


    //初始化，配置帧 回调
    public void init() {
        int textureId = GlUtil.createRecordCameraTextureID();
        surfaceTexture = new SurfaceTexture(textureId);
        try {
            glRenderManager = new GlRenderManager(context, textureId, surfaceView.getHolder().getSurface(), surfaceTexture);
        } catch (GlUtil.OpenGlException e) {
            e.printStackTrace();
            return;
        }
        surfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);
        cameraManager.getCameraSetting().surfaceTexture = surfaceTexture;
        cameraManager.initCamera();
    }


    //一帧准备完毕
    private void callFrameAvailable() {
        if (pause)
            return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                //转换方向
                int recordRotate = recordOrientation;
                if (recordRotate == 90)
                    recordRotate = 270;
                else if (recordRotate == 270)
                    recordRotate = 90;
                try {
                    glRenderManager.drawFrame(isRecord, recordRotate, cameraPosition == 1);
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


    public interface CallBackEvent extends CameraManager.CallBackEvent {

        void startRecordSuccess();

        void onDuringUpdate(float time);

        void stopRecordFinish(File file);

        void recordError(String errorMsg);

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

    public void setNeedVideo(boolean needVideo) {
        this.needVideo = needVideo;
    }

    public void setNeedAudio(boolean needAudio) {
        this.needAudio = needAudio;
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

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    /**
     * 静音
     */
    public void soundOff(boolean off) {
        if (recordAudioManager != null && recordAudioManager.getAudioEncoder() != null)
            recordAudioManager.getAudioEncoder().setSoundOff(off);
    }

    public void setCameraPosition(int cameraPosition) {
        this.cameraPosition = cameraPosition;
    }
}
