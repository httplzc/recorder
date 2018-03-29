package com.yioks.recorder.RecorderHelper;

import android.content.Context;

import com.yioks.recorder.GlRender.GlRenderFBODefault;
import com.yioks.recorder.GlRender.GlRenderImgList;
import com.yioks.recorder.GlRender.GlRenderOutput;
import com.yioks.recorder.GlRender.GlRenderRealTimeBeauty;
import com.yioks.recorder.GlRenderBase.GlRenderNormal;

/**
 * Created by Lzc on 2018/3/10 0010.
 */

public class RenderTypeManager {
    public static GlRenderNormal getFilter(Context context, GLFilterType type) {
        switch (type) {
            // 实时磨皮
            case REALTIMEBEAUTY:
                return new GlRenderRealTimeBeauty(context);
            case STICKER:
                return new GlRenderImgList(context);
            case SOURCE:
                return new GlRenderFBODefault(context);
            case Display:      // 没有滤镜
            default:
                return new GlRenderOutput(context);
        }
    }


    public enum GLFilterType {
        Display, // 没有滤镜


        // 图片编辑滤镜
        BRIGHTNESS, // 亮度
        CONTRAST, // 对比度
        EXPOSURE, // 曝光
        GUASS, // 高斯模糊
        HUE, // 色调
        MIRROR, // 镜像
        SATURATION, // 饱和度
        SHARPNESS, // 锐度


        // 人脸美颜美妆贴纸
        REALTIMEBEAUTY, // 实时美颜
        FACESTRETCH, // 人脸变形(瘦脸大眼等)

        STICKER,    // 水印


        // 颜色滤镜
        SOURCE,         // 原图
        AMARO,          // 阿马罗
        ANTIQUE,        // 古董
        BLACKCAT,       // 黑猫
        BLACKWHITE,     // 黑白
        BROOKLYN,       // 布鲁克林
        CALM,           // 冷静
        COOL,           // 冷色调
        EARLYBIRD,      // 晨鸟
        EMERALD,        // 翡翠
        EVERGREEN,      // 常绿
        FAIRYTALE,      // 童话
        FREUD,          // 佛洛伊特
        HEALTHY,        // 健康
        HEFE,           // 酵母
        HUDSON,         // 哈德森
        KEVIN,          // 凯文
        LATTE,          // 拿铁
        LOMO,           // LOMO
        NOSTALGIA,      // 怀旧之情
        ROMANCE,        // 浪漫
        SAKURA,         // 樱花
        SKETCH,         // 素描
        SUNSET,         // 日落
        WHITECAT,       // 白猫
        WHITENORREDDEN, // 白皙还是红润


    }

    public enum GLFilterGroupType {
        DEFAULT, // 默认滤镜组
        BEAUTY, //美颜
        Water,
    }

}
