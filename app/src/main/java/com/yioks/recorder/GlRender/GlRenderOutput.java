package com.yioks.recorder.GlRender;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;

import com.yioks.recorder.Utils.StringManagerUtil;
import com.yioks.recorder.GlRenderBase.GlRenderNormal;
import com.yioks.recorder.R;

import java.nio.FloatBuffer;

/**
 * Created by Lzc on 2018/3/12 0012.
 */

public class GlRenderOutput extends GlRenderNormal {

    private int rotate;

    public GlRenderOutput(Context context) {
        super(context);
    }

    @Override
    public String getFragmentShaderCode() {

        return StringManagerUtil.getStringFromRaw(context,
                R.raw.normal_fragment_shader);
    }


    @Override
    public String getVertexShaderCode() {
        return StringManagerUtil.getStringFromRaw(context, R.raw.camera_vertex_shader);
    }


    @Override
    public void onDrawArraysBegin() {

    }

    @Override
    public void onDrawArraysAfter() {

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
        return super.drawFrame(textureId, vertexBuffer, textureBuffer);
    }

    @Override
    public int getTextureType() {
        return GLES30.GL_TEXTURE_2D;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }
}
