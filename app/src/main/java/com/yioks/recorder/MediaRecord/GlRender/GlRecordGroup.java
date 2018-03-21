package com.yioks.recorder.MediaRecord.GlRender;

import android.content.Context;

import com.yioks.recorder.MediaRecord.GlRenderBase.GlRenderGroup;

/**
 * Created by Lzc on 2018/3/15 0015.
 */

public class GlRecordGroup extends GlRenderGroup {
    private GlRenderFBODefault glOutput;

    public GlRecordGroup(Context context) {
        super(context);
        glOutput = new GlRenderFBODefault(context);
        mFilters.add(glOutput);
      //  mFilters.add(new GlRenderImgList(context));
        mFilters.add(new GlRenderOutput(context));
    }

    public void setRotate(int rotate) {
        if (glOutput != null)
            glOutput.setRotate(rotate);
    }
}
