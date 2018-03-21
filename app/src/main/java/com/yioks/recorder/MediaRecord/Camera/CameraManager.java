package com.yioks.recorder.MediaRecord.Camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodecInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ${UserWrapper} on 2017/5/3 0003.
 */

public class CameraManager {
    private Camera mCamera;
    private Activity context;
    private boolean askedPermission = false;
    private boolean isOpenCamera = false;
    private int fps;
    private int cameraPosition = 0;
    private static final int targetWidth = 1080;
    private static final int targetHeight = 1920;
    private static final int previewViewFps = 30;
    private CallBackEvent event;
    private SurfaceHolder surfaceHolder;

    private SurfaceTexture surfaceTexture;
    private int colorFormat;


    //照片分辨率
    private int photoWidth;
    private int photoHeight;
    //录制分辨率
    private int videoWidth;
    private int videoHeight;
    private int mOrientation;

    private boolean exposureEnable = false;

    private int currentZoom = 0;


    public Camera getmCamera() {
        return mCamera;
    }


    public CameraManager(Activity context, SurfaceHolder surfaceHolder) {
        this.context = context;
        this.surfaceHolder = surfaceHolder;
    }

    public CameraManager(Activity context) {
        this.context = context;
    }


    private void openCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            realInitCamera(surfaceTexture);
        } else if (!askedPermission) {
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    3668);
            askedPermission = true;
        } else {
            // Wait for permission result
        }
    }

    public void onRequestPermissionsDo(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 3668) {
            if (grantResults[0] == 0 && grantResults[1] == 0) {
                realInitCamera(surfaceTexture);
            } else {
                if (event != null)
                    event.openCameraFailure(0);
            }
        }
    }


    private void realInitCamera(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            freeCameraResource();
        }
        try {
            mCamera = Camera.open(cameraPosition);
        } catch (Exception e) {
            e.printStackTrace();
            freeCameraResource();
            mCamera = null;
        }
        if (mCamera == null) {
            if (event != null)
                event.openCameraFailure(cameraPosition);
            return;
        }
//        changeCameraParams();
        try {
            if (surfaceTexture != null)
                mCamera.setPreviewTexture(surfaceTexture);
            else
                mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
            if (event != null)
                event.openCameraFailure(cameraPosition);
            return;
        }

        changeCameraParams(targetWidth, targetHeight);
        Camera.Size size = mCamera.getParameters().getPictureSize();
        if (event != null)
            event.onPhotoSizeChange(size.height, size.width);
        photoWidth = size.height;
        photoHeight = size.width;
        size = mCamera.getParameters().getPreviewSize();
        if (event != null)
            event.onVideoSizeChange(size.height, size.width);
        videoWidth = size.height;
        videoHeight = size.width;
        mCamera.startPreview();
        //    mCamera.lock();
        isOpenCamera = true;
        if (event != null)
            event.openCameraSuccess(cameraPosition);
    }

    /**
     * 初始化摄像头同时开启预览
     */
    public void initCamera(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
        openCameraWithPermission();

    }

    /**
     * 释放摄像头资源
     */
    public void freeCameraResource() {
        if (mCamera != null && isOpenCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
//            mCamera.lock();
            mCamera.release();
            mCamera = null;
            isOpenCamera = false;
        }
    }

    private Camera.Size calcMaxSize(List<Camera.Size> pictureSizeList) {
        if (pictureSizeList.size() == 0) {
            return null;
        }
        return (pictureSizeList.get(0).width * pictureSizeList.get(0).height
                > pictureSizeList.get(pictureSizeList.size() - 1).width * pictureSizeList.get(pictureSizeList.size() - 1).height) ? pictureSizeList.get(0)
                : pictureSizeList.get(pictureSizeList.size() - 1);
    }

    private Camera.Size calcBestSize(List<Camera.Size> pictureSizeList, int targetWidth, int targetHeight) {
        Camera.Size result = null;
        for (Camera.Size size : pictureSizeList) {
            if (targetWidth == size.height && size.width == targetHeight) {
                return size;
            }
        }
        int lastPxDx = Integer.MAX_VALUE;
        Size other;
        for (int i = pictureSizeList.size() - 1; i >= 0; i--) {
            Camera.Size size = pictureSizeList.get(i);
            if ((targetWidth - targetHeight) * (size.height - size.width) >= 0) {
                int current = Math.abs(size.width * size.height - targetHeight * targetWidth);
                if (current < lastPxDx) {
                    result = size;
                    lastPxDx = current;
                }
            }

        }

        return result;
    }

    public void changeCamera() {
        if (!isOpenCamera || Camera.getNumberOfCameras() <= 1) {
            return;
        }
        cameraPosition = cameraPosition == 1 ? 0 : 1;
        initCamera(surfaceTexture);
    }

    public void setExposure(boolean enable) {
        if (mCamera == null)
            return;
        this.exposureEnable = enable;
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setExposureCompensation(enable ? 1 : 0);
        // parameters.set("brightness-step",parameters.get("max-brightness"));
        mCamera.setParameters(parameters);
    }


    //设置相机参数
    private void changeCameraParams(int targetWidth, int targetHeight) {
        Camera.Parameters parameters = mCamera.getParameters();
        // 获取摄像头支持的PictureSize列表
        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        /**从列表中选取合适的分辨率*/
        Camera.Size picSize = calcBestSize(pictureSizeList, targetWidth, targetHeight);

        if (null == picSize) {
            picSize = parameters.getPictureSize();
        }
        // 根据选出的PictureSize重新设置SurfaceView大小
        parameters.setPictureSize(picSize.width, picSize.height);

        // 获取摄像头支持的PreviewSize列表
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();
        Camera.Size preSize = calcBestSize(previewSizeList, targetWidth, targetHeight);
        if (null == preSize) {
            preSize = parameters.getPreferredPreviewSizeForVideo();
        }
        parameters.setPreviewSize(preSize.width, preSize.height);
        fps = chooseFixedPreviewFps(parameters, previewViewFps * 1000);
        parameters.setJpegQuality(100); // 设置照片质量

        //获取对焦支持列表
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        setExposure(exposureEnable);
//        //获取体颜色类型列表
//        List<Integer> previewFormatsSizes = parameters.getSupportedPreviewFormats();
//        if (-1 != previewFormatsSizes.indexOf(ImageFormat.NV21)) {
//            parameters.setPreviewFormat(ImageFormat.NV21);
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
//                colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
//            else
//                colorFormat = COLOR_FormatYUV420Flexible;
//        } else if (-1 != previewFormatsSizes.indexOf(ImageFormat.YV12)) {
//            parameters.setPreviewFormat(ImageFormat.YV12);
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
//                colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
//            else
//                colorFormat = COLOR_FormatYUV420Flexible;
//        } else
        colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

        parameters.set("orientation", "portrait");
        mCamera.setDisplayOrientation(0);
        // parameters.setRecordingHint(true);

        mCamera.setParameters(parameters);
    }

    /**
     * @param zoomRatio 手势放大倍率
     */
    public void zoom(float zoomRatio) {
        final float maxScaleRatio = 10f;
        Camera.Parameters parameters = mCamera.getParameters();
        if (!parameters.isZoomSupported())
            return;
        int newZoom = 0;
        newZoom = (int) (currentZoom + (zoomRatio > 1 ? 1 : -1) * (zoomRatio / maxScaleRatio * parameters.getMaxZoom()));
        if (newZoom > parameters.getMaxZoom()) {
            newZoom = parameters.getMaxZoom();
        } else if (newZoom < 0) {
            newZoom = 0;
        }
        if (newZoom == currentZoom)
            return;
        if (parameters.isSmoothZoomSupported()) {
            mCamera.startSmoothZoom(newZoom);
        } else
            parameters.setZoom(newZoom);
        currentZoom = newZoom;
        mCamera.setParameters(parameters);
    }


    //获取最佳高分辨率
    private int chooseFixedPreviewFps(Camera.Parameters parms, int desiredThousandFps) {
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            //Log.d(TAG, "entry: " + entry[0] + " - " + entry[1]);
            if ((entry[0] == entry[1]) && (entry[0] == desiredThousandFps)) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }

        int[] tmp = new int[2];
        parms.getPreviewFpsRange(tmp);
        int guess;
        if (tmp[0] == tmp[1]) {
            guess = tmp[0];
        } else {
            guess = tmp[1] / 2;     // shrug
        }

        return guess;
    }

    public boolean isAutoFocus() {
        return mCamera.getParameters().getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO);
    }


    /**
     * 对焦到该区域
     *
     * @param rect   对焦区域（页面坐标）
     * @param width  页面宽度
     * @param height 页面高度
     */
    public void focusToRect(Rect rect, int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Area> areaList = new ArrayList<>();
        rect = calcFocus(rect, videoHeight, videoWidth, width, height);
        areaList.clear();
        areaList.add(new Camera.Area(new Rect(rect.top, rect.left, rect.bottom, rect.right), 600));
        int maxMeteringAreas = mCamera.getParameters().getMaxNumMeteringAreas();
        int maxFocusAreas = mCamera.getParameters().getMaxNumFocusAreas();
        parameters.setFocusAreas(areaList);
        parameters.setMeteringAreas(areaList);

        try {
            mCamera.setParameters(parameters);
            mCamera.autoFocus(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //计算对焦区域  坐标映射
    private Rect calcFocus(Rect rectOr, float cameraVideoWidth, float cameraVideoHeight, int screenWidth, int screenHeight) {

        if (rectOr.left < 0)
            rectOr.left = 0;
        if (rectOr.right > screenWidth)
            rectOr.right = screenWidth;
        if (rectOr.top < 0)
            rectOr.top = 0;
        if (rectOr.bottom > screenHeight)
            rectOr.bottom = screenHeight;


        rectOr.left *= cameraVideoHeight / screenWidth;
        rectOr.right *= cameraVideoHeight / screenWidth;
        rectOr.top *= cameraVideoWidth / screenHeight;
        rectOr.bottom *= cameraVideoWidth / screenHeight;


        rectOr.left = (int) (2000f / cameraVideoHeight * rectOr.left - 1000f);
        rectOr.right = (int) (2000f / cameraVideoHeight * rectOr.right - 1000f);
        rectOr.bottom = (int) (2000f / cameraVideoWidth * rectOr.bottom - 1000f);
        rectOr.top = (int) (2000f / cameraVideoWidth * rectOr.top - 1000f);
        if (rectOr.left <= -1000)
            rectOr.left = -1000;
        if (rectOr.right >= 1000)
            rectOr.right = 1000;
        if (rectOr.top <= -1000)
            rectOr.top = -1000;
        if (rectOr.bottom >= 1000)
            rectOr.bottom = 1000;
        return new Rect(rectOr);
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    public int getCameraPosition() {
        return cameraPosition;
    }

    public void setCameraPosition(int cameraPosition) {
        this.cameraPosition = cameraPosition;
    }

    public boolean isOpenCamera() {
        return isOpenCamera;
    }

    public void setOpenCamera(boolean openCamera) {
        isOpenCamera = openCamera;
    }

    public int getFps() {
        return fps;
    }

    public CallBackEvent getEvent() {
        return event;
    }

    public void setEvent(CallBackEvent event) {
        this.event = event;
    }

    public int getColorFormat() {
        return colorFormat;
    }

    public int getPhotoWidth() {
        return photoWidth;
    }

    public void setPhotoWidth(int photoWidth) {
        this.photoWidth = photoWidth;
    }

    public int getPhotoHeight() {
        return photoHeight;
    }

    public void setPhotoHeight(int photoHeight) {
        this.photoHeight = photoHeight;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public void setmOrientation(int mOrientation) {
        this.mOrientation = mOrientation;
    }

    /**
     * 拍摄的所有事件
     */
    public interface CallBackEvent {
        void openCameraSuccess(int cameraPosition);

        void openCameraFailure(int cameraPosition);

        void onVideoSizeChange(int width, int height);

        void onPhotoSizeChange(int width, int height);

    }

}
