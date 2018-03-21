package com.yioks.recorder.MediaRecord.GlRenderBase;

/**
 * Created by Lzc on 2018/3/10 0010.
 */

public interface GlRender {


    public void onInputSizeChanged(int width, int height);

    public void onDisplayChanged(int width, int height);

    public int drawFrame(int textureId);

    public void release();
}
