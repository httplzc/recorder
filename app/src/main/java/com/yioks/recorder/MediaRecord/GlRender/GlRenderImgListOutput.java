package com.yioks.recorder.MediaRecord.GlRender;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;

import java.nio.FloatBuffer;

/**
 * Created by Lzc on 2018/3/13 0013.
 */

public class GlRenderImgListOutput extends GlRenderImgList {
    private int rotate;
    public GlRenderImgListOutput(Context context) {
        super(context);
    }

    @Override
    public int drawFrame(int textureId) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                Matrix.setIdentityM(mMVPMatrix, 0);
                Matrix.rotateM(mMVPMatrix, 0, rotate, 0, 0, 1f);
            }
        });
        return super.drawFrame(textureId);
    }

    @Override
    public int drawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        GLES30.glViewport(0, 0, mDisplayWidth, mDisplayHeight);
        return super.drawNormalFrame(textureId, vertexBuffer, textureBuffer);
    }


    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

}
