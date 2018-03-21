package com.yioks.recorder.MediaRecord.PlayVideo;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.SurfaceView;

import com.yioks.recorder.MediaRecord.Utils.StringManagerUtil;

import java.io.File;

/**
 * Created by ${UserWrapper} on 2017/5/3 0003.
 * 视频播放控制器
 */

public class PlayVideoManager {
    private Context context;
    private MediaPlayer mediaPlayer;
    private CallBackEvent event;

    public PlayVideoManager(Context context) {
        this.context = context;
    }

    public void stopPlayVideo() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        if (event != null)
            event.stopPlayVideo();
    }

    public void releasePlayVideo() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    //播放动画
    public void playVideo(File file, final SurfaceView surfaceView) {

        int width = 0;
        int height = 0;
        try {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(file.getPath());
            String widthStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String heightStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (StringManagerUtil.VerifyNumber(widthStr))
                width = Integer.valueOf(widthStr);
            if (StringManagerUtil.VerifyNumber(heightStr))
                height = Integer.valueOf(heightStr);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(context, Uri.fromFile(file));
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {

                }
            });
            mediaPlayer.setLooping(true);
            //  mediaPlayer.setDisplay(holder);
            final int finalWidth = width;
            final int finalHeight = height;
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaPlayer.start();
                    mediaPlayer.setDisplay(surfaceView.getHolder());
                    if (event != null)
                        event.startPlayVideo(finalWidth, finalHeight);
                }
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            if (mediaPlayer != null)
                mediaPlayer.release();
            if (event != null)
                event.playVideoFailure();
        }

    }

    /**
     * 拍摄的所有事件
     */
    public interface CallBackEvent {
        void startPlayVideo(int width, int height);

        void stopPlayVideo();

        void playVideoFailure();
    }

    public CallBackEvent getEvent() {
        return event;
    }

    public void setEvent(CallBackEvent event) {
        this.event = event;
    }
}
