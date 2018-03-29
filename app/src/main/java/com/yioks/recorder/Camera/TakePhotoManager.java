package com.yioks.recorder.Camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;


import com.yioks.recorder.Utils.FileUntil;

import java.lang.ref.WeakReference;

/**
 * Created by ${UserWrapper} on 2017/5/3 0003.
 */

public class TakePhotoManager {
    private Context context;
    private CallBackEvent event;
    private Bitmap bitmap;
    private long lastTakePhotoTime = 0;
    private int lastPicOrientation = 0;
    private TakePhotoHandler takePhotoHandler;

    public TakePhotoManager(Context context) {
        this.context = context;
        takePhotoHandler = new TakePhotoHandler(this);
    }


    private static class TakePhotoHandler extends Handler {
        private WeakReference<TakePhotoManager> takePhotoManagerWeakReference;

        public TakePhotoHandler(TakePhotoManager takePhotoManager) {
            this.takePhotoManagerWeakReference = new WeakReference<TakePhotoManager>(takePhotoManager);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                if (takePhotoManagerWeakReference.get() != null) {
                    takePhotoManagerWeakReference.get().bitmap = (Bitmap) msg.obj;
                }
                if (takePhotoManagerWeakReference.get().event != null) {
                    takePhotoManagerWeakReference.get().event.takePhotoFinish();
                }

            } else if (msg.what == 1) {

            }
            super.handleMessage(msg);
        }
    }

    private class TakePhotoTask extends Thread {

        private Camera camera;
        private int cameraPosition;
        private int orientation;

        public TakePhotoTask(Camera camera, int cameraPosition, int orientation) {
            this.camera = camera;
            this.cameraPosition = cameraPosition;
            this.orientation = orientation;
        }

        @Override
        public void run() {
            try {
                camera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                       // options.inSampleSize = 2;
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                        Bitmap bitmapNew = FileUntil.rotateBitmap(bitmap, cameraPosition == 0 ? 90 : (orientation == 90 || orientation == 270) ? 90 : -90);
                        bitmap.recycle();
                        takePhotoHandler.sendMessage(Message.obtain(takePhotoHandler, 0, bitmapNew));

                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                takePhotoHandler.sendMessage(Message.obtain(takePhotoHandler, 1));
            }
        }

    }




    public void takePhoto(Camera mCamera, final int cameraPosition, final int orientation) {
        lastPicOrientation = orientation;
        if (System.currentTimeMillis() - lastTakePhotoTime < 500)
            return;
        lastTakePhotoTime = System.currentTimeMillis();
        TakePhotoTask takePhotoTask = new TakePhotoTask(mCamera, cameraPosition, orientation);
        takePhotoTask.start();

    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public CallBackEvent getEvent() {
        return event;
    }

    public void setEvent(CallBackEvent event) {
        this.event = event;
    }

    public interface CallBackEvent {
        void takePhotoFinish();
    }

    public long getLastTakePhotoTime() {
        return lastTakePhotoTime;
    }

    public void setLastTakePhotoTime(long lastTakePhotoTime) {
        this.lastTakePhotoTime = lastTakePhotoTime;
    }

    public int getLastPicOrientation() {
        return lastPicOrientation;
    }

    public void setLastPicOrientation(int lastPicOrientation) {
        this.lastPicOrientation = lastPicOrientation;
    }
}
