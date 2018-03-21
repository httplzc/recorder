package com.yioks.recorder.MediaRecord.RenderHelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.view.Surface;

import com.yioks.recorder.MediaRecord.GlRender.GlDisplayGroup;
import com.yioks.recorder.MediaRecord.GlRender.GlRecordGroup;
import com.yioks.recorder.MediaRecord.GlRender.GlRenderImg;
import com.yioks.recorder.MediaRecord.OpenGl.EglCore;
import com.yioks.recorder.MediaRecord.OpenGl.WindowSurface;
import com.yioks.recorder.MediaRecord.Utils.GlUtil;

import java.lang.ref.WeakReference;
import java.nio.Buffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Lzc on 2018/3/12 0012.
 * 绘图控制
 */

public class GlRenderManager {
    //gl 核心
    private EglCore mEglCore;
    //展示的surface
    private WindowSurface mDisplaySurface;
    //编码的surface
    private WindowSurface mEncoderSurface;
    private final Object mSyncObject = new Object();

    private boolean beautyEnable;

    private GlDisplayGroup displayRenderGroup;
    private GlRecordGroup recordRenderGroup;

    // 输入流大小
    private int mTextureWidth;
    private int mTextureHeight;
    // 显示大小
    private int mDisplayWidth;
    private int mDisplayHeight;

    // 显示大小
    private int mRecordWidth;
    private int mRecordHeight;

    private Context context;

    private int texture;
    private SurfaceTexture surfaceTexture;

    private GlBackBitmap glBackBitmap;

    //是否拍照状态
    private boolean takePhoto = false;


    private static class TakePhotoTask extends AsyncTask<Object, Object, Bitmap> {
        private WeakReference<GlRenderManager> glRenderManagerRef;

        TakePhotoTask(GlRenderManager glRenderManager) {
            glRenderManagerRef = new WeakReference<GlRenderManager>(glRenderManager);
        }

        @Override
        protected Bitmap doInBackground(Object[] objects) {
            Buffer buffer = (Buffer) objects[0];
            int width = (int) objects[1];
            int height = (int) objects[2];
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buffer);
            Matrix matrix = new Matrix();
            matrix.postRotate(180); /*翻转90度*/
            matrix.postScale(-1,1);
            bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap o) {
            Bitmap bitmap = (Bitmap) o;
            if (glRenderManagerRef != null && glRenderManagerRef.get() != null && glRenderManagerRef.get().glBackBitmap != null) {
                glRenderManagerRef.get().glBackBitmap.onFinish(bitmap);
            } else
                bitmap.recycle();
        }
    }

    public interface GlBackBitmap {
        void onFinish(Bitmap bitmap);
    }


    public GlRenderManager(Context context, int texture, Surface disPlaySurface, SurfaceTexture surfaceTexture) throws GlUtil.OpenGlException {
        this.context = context;
        this.texture = texture;
        this.surfaceTexture = surfaceTexture;
        mEglCore = new EglCore(null, EglCore.FLAG_TRY_GLES3);
        setDisPlaySurface(disPlaySurface);
        mDisplaySurface.makeCurrent();
        //关闭深度测试和绘制背面
        GLES20.glDisable(GL10.GL_DEPTH_TEST);
        GLES20.glDisable(GL10.GL_CULL_FACE);
        init();
    }


    public void init() {
        //显示渲染组
        displayRenderGroup = new GlDisplayGroup(context);
        //录制渲染组
        recordRenderGroup = new GlRecordGroup(context);
    }


    /**
     * 销毁
     */
    public void release() {
        if (displayRenderGroup != null) {
            displayRenderGroup.release();
            displayRenderGroup = null;
        }
        if (recordRenderGroup != null) {
            recordRenderGroup.release();
            recordRenderGroup = null;
        }
        if (mEncoderSurface != null) {
            mEncoderSurface.release();
            mEncoderSurface = null;
        }
        if (mDisplaySurface != null) {
            mDisplaySurface.release();
            mDisplaySurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }


    public void setDisPlaySurface(Surface displaySurface) throws GlUtil.OpenGlException {
        mDisplaySurface = new WindowSurface(mEglCore, displaySurface, false);
    }

    public void setEncoderSurface(Surface encodeSurface) throws GlUtil.OpenGlException {
        mEncoderSurface = new WindowSurface(mEglCore, encodeSurface, true);
    }

    //绘制
    public void drawFrame(boolean is_record, int recordRotate, boolean mirroring) throws Exception {
        int currentTexture = texture;
        if (mEglCore == null || mDisplaySurface == null) {
            return;
        }
        mDisplaySurface.makeCurrent();
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        try {
            surfaceTexture.updateTexImage();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (displayRenderGroup != null) {
            displayRenderGroup.setMirroring(mirroring);
            currentTexture = displayRenderGroup.drawFrame(currentTexture);
        }
        //拍照状态
        if (takePhoto) {
            takePhoto = false;
            new TakePhotoTask(this).execute(mDisplaySurface.getCurrentFrame(), mDisplayWidth, mDisplayHeight);
        }
        mDisplaySurface.swapBuffers();
        if (is_record && mEncoderSurface != null && recordRenderGroup != null) {
            mEncoderSurface.makeCurrent();
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            recordRenderGroup.onInputSizeChanged(mRecordWidth, mRecordHeight);
            recordRenderGroup.onDisplayChanged(mRecordWidth, mRecordHeight);
            recordRenderGroup.setRotate(recordRotate);
            recordRenderGroup.drawFrame(currentTexture);
            mEncoderSurface.setPresentationTime(surfaceTexture.getTimestamp());
            mEncoderSurface.swapBuffers();
        }
        FrameRateMeter.getInstance().drawFrameCount();
    }


    /**
     * 渲染Texture的大小
     *
     * @param width
     * @param height
     */
    public void onInputSizeChanged(int width, int height) {
        mTextureWidth = width;
        mTextureHeight = height;
        if (displayRenderGroup != null)
            displayRenderGroup.onInputSizeChanged(width, height);
        if (recordRenderGroup != null)
            recordRenderGroup.onInputSizeChanged(width, height);
    }

    /**
     * Surface显示的大小
     *
     * @param width
     * @param height
     */
    public void onDisplaySizeChanged(int width, int height) {
        mDisplayWidth = width;
        mDisplayHeight = height;
        if (displayRenderGroup != null)
            displayRenderGroup.onDisplayChanged(width, height);
        if (recordRenderGroup != null)
            recordRenderGroup.onDisplayChanged(width, height);
    }




    public boolean isBeautyEnable() {
        return beautyEnable;
    }

    public void setBeautyEnable(boolean beautyEnable) {
        this.beautyEnable = beautyEnable;
        displayRenderGroup.enableBeauty(beautyEnable);
    }


    public void setmRecordWidth(int mRecordWidth) {
        this.mRecordWidth = mRecordWidth;
    }

    public void setmRecordHeight(int mRecordHeight) {
        this.mRecordHeight = mRecordHeight;
    }

    public void addWaterImg(GlRenderImg glRenderImg) {
//        if (recordRenderGroup != null) {
//            ((GlRenderImgList) recordRenderGroup.getmFilters().get(1)).add(glRenderImg);
//        }
    }

    public GlBackBitmap getGlBackBitmap() {
        return glBackBitmap;
    }

    public void setGlBackBitmap(GlBackBitmap glBackBitmap) {
        this.glBackBitmap = glBackBitmap;
    }

    public void setTakePhoto(boolean takePhoto) {
        this.takePhoto = takePhoto;
    }
}
