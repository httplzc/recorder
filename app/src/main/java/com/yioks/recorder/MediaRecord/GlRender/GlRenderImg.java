package com.yioks.recorder.MediaRecord.GlRender;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.yioks.recorder.MediaRecord.RenderHelper.TexturePositionUtil;
import com.yioks.recorder.MediaRecord.Utils.GlUtil;

import java.nio.FloatBuffer;

/**
 * Created by Lzc on 2018/3/12 0012.
 */

public class GlRenderImg {
    private int texture = -1;
    private FloatBuffer vertexPosition;
    private FloatBuffer vertexPositionHorizontal;
    private FloatBuffer fragmentPosition;
    private Bitmap bitmap;


    /**
     * @param bitmap 图片
     */
    public GlRenderImg(Bitmap bitmap) {
        texture = GlUtil.create2DTexture(bitmap);
        fragmentPosition = TexturePositionUtil.DefaultTextureFloatBuffer;
        this.bitmap = bitmap;
    }

    /**
     * @param verticalWidth  1-0  1为屏幕大小
     * @param verticalHeight 1-0  1为屏幕大小
     * @param positionX      1-0  贴图左上角X 0为屏幕左上角
     * @param positionY      1-0  贴图左上角Y 1为屏幕左上角
     */
    public void initVerticalPosition(float verticalWidth, float verticalHeight, float positionX, float positionY) {
        vertexPosition = GlUtil.createFloatBuffer(new float[]{
                -1 + positionX * 2, 1 - positionY * 2,
                -1 + positionX * 2, 1 - positionY * 2 - verticalHeight * 2,
                -1 + positionX * 2 + verticalWidth * 2, 1 - positionY * 2 - verticalHeight * 2,
                -1 + positionX * 2 + verticalWidth * 2, 1 - positionY * 2

        });

    }

    /**
     * @param verticalWidth  1-0  1为屏幕大小
     * @param verticalHeight 1-0  1为屏幕大小
     * @param positionX      1-0  贴图左上角X 0为屏幕左上角
     * @param positionY      1-0  贴图左上角Y 1为屏幕左上角
     */
    public void initHorizontalPosition(float verticalWidth, float verticalHeight, float positionX, float positionY) {
        vertexPositionHorizontal = GlUtil.createFloatBuffer(new float[]{
                -1 + positionX * 2, 1 - positionY * 2,
                -1 + positionX * 2, 1 - positionY * 2 - verticalHeight * 2,
                -1 + positionX * 2 + verticalWidth * 2, 1 - positionY * 2 - verticalHeight * 2,
                -1 + positionX * 2 + verticalWidth * 2, 1 - positionY * 2

        });
    }


    public void release() {
        if (texture != 0 && texture != -1) {
            GLES20.glDeleteTextures(1, new int[]{texture}, 0);
            texture = -1;
        }
        if (bitmap != null)
            bitmap.recycle();
        bitmap = null;
    }


    public int getTexture() {
        return texture;
    }

    public FloatBuffer getVertexPosition() {
        return vertexPosition;
    }

    public FloatBuffer getVertexPositionHorizontal() {
        return vertexPositionHorizontal;
    }

    public FloatBuffer getFragmentPosition() {
        return fragmentPosition;
    }
}
