package com.yioks.recorder;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.yioks.recorder.Bean.CameraSetting;
import com.yioks.recorder.Bean.RecordSetting;
import com.yioks.recorder.Bean.RenderSetting;
import com.yioks.recorder.GlRender.GlRenderImg;
import com.yioks.recorder.GlRender.GlRenderImgList;
import com.yioks.recorder.LiveRecord.LiveManager;
import com.yioks.recorder.LiveRecord.PushManager;
import com.yioks.recorder.RecorderHelper.RecordManageBase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LiveTextActivity extends Activity {

    private Button btn1;
    private Button btn2;
    private Button btn3;
    private Button btn4;
    private Button btn5;
    private Button btn6;

    private EditText push_url;


    private View iv;
    private RelativeLayout recordarea;
    private static final int RequestCameraPermission = 12304;

    //view
    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private LiveManager recorder;

    private final static int TargetLongWidth = 1280;
    private int TargetShortWidth = 720;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_live_text);
        this.recordarea = (RelativeLayout) findViewById(R.id.record_area);
        this.iv = (View) findViewById(R.id.iv);
        this.btn3 = (Button) findViewById(R.id.btn3);
        this.btn2 = (Button) findViewById(R.id.btn2);
        this.btn1 = (Button) findViewById(R.id.btn1);
        this.btn4 = (Button) findViewById(R.id.btn4);
        this.btn5 = (Button) findViewById(R.id.btn5);
        this.btn6 = (Button) findViewById(R.id.btn6);
        push_url = findViewById(R.id.push_url);
        this.surfaceView = findViewById(R.id.surface_view);
        RecordSetting recordSetting = new RecordSetting();
        CameraSetting cameraSetting = new CameraSetting();
        cameraSetting.fps = 30;
        cameraSetting.cameraPosition = 0;
        cameraSetting.width = 720;
        cameraSetting.height = 1280;
//        push_url.setText("rtmp://video-center-bj.alivecdn.com/jiaozuo/3096?vhost=yioks-match-1.yioks.com&auth_key=1532193838-0-0-2d9a61d30ff00a7def95129e99e63502");
        push_url.setText("rtmp://video-center-bj.alivecdn.com/devVideo/2791?vhost=yioks-match-dev.yioks.com&auth_key=1532606434-0-0-a1c42b71dd113fb4aeb67b8b0a5f71d2");
        RenderSetting renderSetting = new RenderSetting();
        renderSetting.setDisplaySize(720, 1280);
        renderSetting.setRenderSize(720, 1280);
        recorder = new LiveManager(this, recordSetting, cameraSetting, renderSetting, surfaceView);
        recorder.setPushCallBack(new PushManager.CallBack() {

            @Override
            public void connect() {
                Log.i("lzc", "开始成功");
                btn1.setText("开始成功");
            }

            @Override
            public void fail() {
                recorder.cancelRecord();
                btn1.setText("失败");

            }

            @Override
            public void lostPacket() {
                Log.i("lzc", "lostPacket");

            }
        });


        recorder.setCallBackEvent(new RecordManageBase.CallBackEvent() {
            @Override
            public void startRecordSuccess() {

            }

            @Override
            public void onDuringUpdate(float v) {

            }

            @Override
            public void stopRecordFinish(File file) {

            }

            @Override
            public void recordError(String s) {

            }

            @Override
            public void openCameraSuccess(int i) {
                recorder.getRecordSetting().setVideoSetting(TargetShortWidth, TargetLongWidth,
                  recorder.getCameraManager().getRealFps() / 1000, RecordSetting.ColorFormatDefault);
                recorder.getRecordSetting().setVideoBitRate(1500 * 1024);
                recorder.switchOnBeauty(i == 1);
            }

            @Override
            public void openCameraFailure(int i) {

            }

            @Override
            public void onVideoSizeChange(int i, int i1) {

            }

            @Override
            public void onPhotoSizeChange(int i, int i1) {

            }
        });
        holder = surfaceView.getHolder();
        holder.addCallback(new CustomCallBack());

        btn1.setOnClickListener(v -> {
            recorder.getRecordSetting().setPushUrl(push_url.getText().toString());
            try {
                recorder.startRecord();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        btn2.setOnClickListener(v -> recorder.stopRecord());
        btn3.setOnClickListener(v -> {
            if (btn3.getText().equals("继续")) {
                btn3.setText("暂停");
                recorder.init();
                recorder.startRecord();
            } else {
                btn3.setText("继续");
                recorder.pause();
            }

        });


        btn4.setOnClickListener(v -> {
            if (recorder.getCameraManager().isOpenCamera())
                recorder.getCameraManager().switchCamera();
        });

        btn5.setOnClickListener(v -> {
            boolean off = recorder.isSoundOff();
            recorder.soundOff(!off);
            if (recorder.isSoundOff())
                btn5.setText("开启声音");
            else
                btn5.setText("静音");
        });

        btn6.setOnClickListener(v -> {
            GlRenderImgList renderImgList = recorder.getGlRenderManager().getRenderList();
            if (renderImgList.getSize() == 2) {
                renderImgList.clear();
                return;
            }
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.match_water_img);
            GlRenderImg glRenderImg = new GlRenderImg(bitmap);
            glRenderImg.initVerticalPosition(bitmap.getWidth() / (float) surfaceView.getWidth(),
              bitmap.getHeight() / (float) surfaceView.getHeight(), 0.05f, renderImgList.getSize() == 0 ? 0.05f : 0.5f);
            glRenderImg.initHorizontalPosition(bitmap.getWidth() / (float) TargetLongWidth,
              bitmap.getHeight() / (float) TargetShortWidth, 0.05f, renderImgList.getSize() == 0 ? 0.05f : 0.5f);

            renderImgList.add(glRenderImg);
        });
    }

    protected void onPermissionBack(int requestCode, boolean succeed) {
        if (requestCode == RequestCameraPermission) {
            if (succeed)
                recorder.init();
            else {
                recorder.getCameraManager().getEvent().openCameraFailure(0);
            }

        }
    }


    private void initWaterTexture() {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.match_water_img);
        GlRenderImg glRenderImg = new GlRenderImg(bitmap);
        glRenderImg.initVerticalPosition(bitmap.getWidth() / (float) surfaceView.getWidth(),
          bitmap.getHeight() / (float) surfaceView.getHeight(), 0.05f, 0.05f);
//        glRenderImg.initVerticalPosition(0.5f,
//                0.5f, 0, 0);
        glRenderImg.initHorizontalPosition(bitmap.getWidth() / (float) TargetLongWidth,
          bitmap.getHeight() / (float) TargetShortWidth, 0.05f, 0.05f);
        GlRenderImgList renderImgList = recorder.getGlRenderManager().getRenderList();
        renderImgList.add(glRenderImg);
    }

    private class CustomCallBack implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            requestPermission(RequestCameraPermission, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            recorder.getRenderSetting().setRenderSize(width < height ? TargetShortWidth : TargetLongWidth, width < height ? TargetLongWidth : TargetShortWidth);
            recorder.getRenderSetting().setDisplaySize(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            recorder.pause();
        }

    }

    @Override
    protected void onDestroy() {
        if (recorder != null)
            recorder.destroy();

        super.onDestroy();
    }

    protected void requestPermission(int requestCode, String permission[]) {
        List<String> requestStr = new ArrayList<>();
        for (String s : permission) {
            if (ContextCompat.checkSelfPermission(this, s)
              != PackageManager.PERMISSION_GRANTED) {
                requestStr.add(s);
            }
        }
        if (requestStr.size() == 0) {
            onPermissionBack(requestCode, true);
            return;
        }
        String realPermission[] = new String[requestStr.size()];
        requestStr.toArray(realPermission);
        ActivityCompat.requestPermissions(this, realPermission, requestCode);
    }
}
