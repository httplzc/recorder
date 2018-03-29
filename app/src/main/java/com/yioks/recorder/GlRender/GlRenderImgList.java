package com.yioks.recorder.GlRender;

import android.content.Context;
import android.opengl.GLES30;

import com.yioks.recorder.GlRenderBase.GlRenderNormalFBO;
import com.yioks.recorder.R;
import com.yioks.recorder.Utils.StringManagerUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Lzc on 2018/3/13 0013.
 */

public class GlRenderImgList extends GlRenderNormalFBO {
    private List<GlRenderImg> glRenderImgArrayList = new ArrayList<>();
    private boolean vertical = true;

    public GlRenderImgList(Context context) {
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
    protected void unBindValue() {
        super.unBindValue();
        //绘制原图完成后利用同一个program进行处理
        GLES30.glUseProgram(mProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        for (GlRenderImg glRenderImg : glRenderImgArrayList) {
            drawChildImg(glRenderImg.getTexture(), vertical ? glRenderImg.getVertexPosition()
                    : glRenderImg.getVertexPositionHorizontal(), glRenderImg.getFragmentPosition());
        }
        GLES30.glDisable(GLES30.GL_BLEND);
    }

    @Override
    public int getTextureType() {
        return GLES30.GL_TEXTURE_2D;
    }


    private void drawChildImg(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        // 绑定数据
        bindValue(textureId, vertexBuffer, textureBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        // GLES30.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES30.glDisableVertexAttribArray(maPositionLoc);
        GLES30.glDisableVertexAttribArray(maTextureCoordLoc);
    }


    public void add(GlRenderImg openGlImgTexture) {
        glRenderImgArrayList.add(openGlImgTexture);
    }

    public void remove(int position) {
        GlRenderImg openGlImgTexture = glRenderImgArrayList.remove(position);
        if (openGlImgTexture != null)
            openGlImgTexture.release();
    }

    public void clear() {
        Iterator<GlRenderImg> glRenderImgIterator = glRenderImgArrayList.iterator();
        while (glRenderImgIterator.hasNext()) {
            GlRenderImg openGlImgTexture = glRenderImgIterator.next();
            glRenderImgIterator.remove();
            if (openGlImgTexture != null)
                openGlImgTexture.release();
        }
    }

    public void replace(int position, GlRenderImg openGlImgTexture) {
        GlRenderImg last = glRenderImgArrayList.get(position);
        glRenderImgArrayList.set(position, openGlImgTexture);
        last.release();
    }

    public void remove(GlRenderImg openGlImgTexture) {
        glRenderImgArrayList.remove(openGlImgTexture);
        openGlImgTexture.release();
    }

    public int getSize() {
        return glRenderImgArrayList.size();
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    @Override
    public void release() {
        super.release();
        if (glRenderImgArrayList != null) {
            for (GlRenderImg glRenderImg : glRenderImgArrayList) {
                glRenderImg.release();
            }
            glRenderImgArrayList.clear();
            ;
        }
        glRenderImgArrayList = null;
    }
}
