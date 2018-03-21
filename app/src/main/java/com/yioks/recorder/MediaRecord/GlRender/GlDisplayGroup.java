package com.yioks.recorder.MediaRecord.GlRender;

import android.content.Context;

import com.yioks.recorder.MediaRecord.GlRenderBase.GlRenderCamera;
import com.yioks.recorder.MediaRecord.GlRenderBase.GlRenderGroup;

/**
 * Created by Lzc on 2018/3/15 0015.
 */

public class GlDisplayGroup extends GlRenderGroup {
    private GlRenderCamera glRenderCamera;
    private boolean enableBeauty = false;

    public GlDisplayGroup(Context context) {
        super(context);
        glRenderCamera = new GlRenderCamera(context);
        mFilters.add(glRenderCamera);
        mFilters.add(null);
       // mFilters.add(new GlRenderBrightness(context));
        mFilters.add(new GlRenderOutput(context));
    }

    public void setMirroring(boolean mirroring) {
        glRenderCamera.setMirroring(mirroring);
    }

    public void enableBeauty(boolean enable) {
        if (this.enableBeauty != enable) {
            enableBeauty = enable;
            replace(1, enableBeauty ? new GlRenderRealTimeBeauty(context) : null);
        }
    }

}
