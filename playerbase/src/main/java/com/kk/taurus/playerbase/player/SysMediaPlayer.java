/*
 * Copyright 2017 jiajunhui<junhui_jia@163.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.kk.taurus.playerbase.player;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.kk.taurus.playerbase.log.PLog;
import com.kk.taurus.playerbase.entity.DataSource;
import com.kk.taurus.playerbase.event.BundlePool;
import com.kk.taurus.playerbase.event.EventKey;
import com.kk.taurus.playerbase.event.OnErrorEventListener;
import com.kk.taurus.playerbase.event.OnPlayerEventListener;

/**
 * Created by Taurus on 2018/3/17.
 */

public class SysMediaPlayer extends BaseInternalPlayer {

    final String TAG = "SysMediaPlayer";
    private MediaPlayer mMediaPlayer;

    private int mTargetState;

    public SysMediaPlayer(Context context) {
        super(context);
        init();
    }

    private void init() {
        mMediaPlayer = new MediaPlayer();
    }

    @Override
    public void setDataSource(DataSource dataSource) {
        try {
            if(mMediaPlayer==null){
                mMediaPlayer = new MediaPlayer();
            }else{
                reset();
                resetListener();
            }
            Uri mUri = Uri.parse(dataSource.getData());
            // REMOVED: mAudioSession
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            updateStatus(STATE_INITIALIZED);
            mMediaPlayer.setDataSource(mUri.toString());

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();

            Bundle bundle = BundlePool.obtain();
            bundle.putSerializable(EventKey.SERIALIZABLE_DATA,dataSource);
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET,bundle);
        }catch (Exception e){
            e.printStackTrace();
            updateStatus(STATE_ERROR);
            mTargetState = STATE_ERROR;
        }
    }

    private boolean available(){
        return mMediaPlayer!=null;
    }

    @Override
    public void setDisplay(SurfaceHolder surfaceHolder) {
        try {
            if(available()){
                mMediaPlayer.setDisplay(surfaceHolder);
                submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_HOLDER_UPDATE, null);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void setSurface(Surface surface) {
        try {
            if(available()){
                mMediaPlayer.setSurface(surface);
                submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_UPDATE, null);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void setVolume(float left, float right) {
        if(available()){
            mMediaPlayer.setVolume(left, right);
        }
    }

    @Override
    public boolean isPlaying() {
        if(available() && getState()!= STATE_ERROR){
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public int getCurrentPosition() {
        if(available()&& (getState()== STATE_PREPARED
                || getState()== STATE_STARTED
                || getState()== STATE_PAUSED
                || getState()== STATE_PLAYBACK_COMPLETE)){
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public int getDuration() {
        if(available()
                && getState()!= STATE_ERROR
                && getState()!= STATE_INITIALIZED
                && getState()!= STATE_IDLE){
            return mMediaPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public int getAudioSessionId() {
        if(available()){
            return mMediaPlayer.getAudioSessionId();
        }
        return 0;
    }

    @Override
    public int getVideoWidth() {
        if(available()){
            return mMediaPlayer.getVideoWidth();
        }
        return 0;
    }

    @Override
    public int getVideoHeight() {
        if(available()){
            return mMediaPlayer.getVideoHeight();
        }
        return 0;
    }

    @Override
    public void start() {
        try {
            if(available() &&
                    (getState()== STATE_PREPARED
                            || getState()== STATE_PAUSED
                            || getState()== STATE_PLAYBACK_COMPLETE)){
                mMediaPlayer.start();
                updateStatus(STATE_STARTED);
                submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_START, null);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        mTargetState = STATE_STARTED;
    }

    @Override
    public void start(int msc) {
        if(available()){
            if(msc > 0){
                startSeekPos = msc;
            }
            start();
        }
    }

    @Override
    public void pause() {
        try{
            if(available()){
                mMediaPlayer.pause();
                updateStatus(STATE_PAUSED);
                submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_PAUSE, null);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        mTargetState = STATE_PAUSED;
    }

    @Override
    public void resume() {
        try {
            if(available() && getState() == STATE_PAUSED){
                mMediaPlayer.start();
                updateStatus(STATE_STARTED);
                submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_RESUME, null);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        mTargetState = STATE_STARTED;
    }

    @Override
    public void seekTo(int msc) {
        if(available() &&
                (getState()== STATE_PREPARED
                        || getState()== STATE_STARTED
                        || getState()== STATE_PAUSED
                        || getState()== STATE_PLAYBACK_COMPLETE)){
            mMediaPlayer.seekTo(msc);
            Bundle bundle = BundlePool.obtain();
            bundle.putInt(EventKey.INT_DATA, msc);
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_TO, bundle);
        }
    }

    @Override
    public void stop() {
        if(available() &&
                (getState()== STATE_PREPARED
                        || getState()== STATE_STARTED
                        || getState()== STATE_PAUSED
                        || getState()== STATE_PLAYBACK_COMPLETE)){
            mMediaPlayer.stop();
            updateStatus(STATE_STOPPED);
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_STOP, null);
        }
        mTargetState = STATE_STOPPED;
    }

    @Override
    public void reset() {
        if(available()){
            mMediaPlayer.reset();
            updateStatus(STATE_IDLE);
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_RESET, null);
        }
        mTargetState = STATE_IDLE;
    }

    @Override
    public void destroy() {
        if(available()){
            updateStatus(STATE_END);
            resetListener();
            mMediaPlayer.release();
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_DESTROY, null);
        }
    }

    private void resetListener(){
        if(mMediaPlayer==null)
            return;
        mMediaPlayer.setOnPreparedListener(null);
        mMediaPlayer.setOnVideoSizeChangedListener(null);
        mMediaPlayer.setOnCompletionListener(null);
        mMediaPlayer.setOnErrorListener(null);
        mMediaPlayer.setOnInfoListener(null);
        mMediaPlayer.setOnBufferingUpdateListener(null);
    }

    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            PLog.d(TAG,"onPrepared...");
            updateStatus(STATE_PREPARED);

            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_PREPARED,null);

            // Get the capabilities of the player for this stream
            // REMOVED: Metadata

            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();

            int seekToPosition = startSeekPos;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
                startSeekPos = 0;
            }

            // We don't know the video size yet, but should start anyway.
            // The video size might be reported to us later.
            PLog.d(TAG,"mTargetState = " + mTargetState);
            if (mTargetState == STATE_STARTED) {
                start();
            }else if(mTargetState == STATE_PAUSED){
                pause();
            }else if(mTargetState == STATE_STOPPED
                    || mTargetState == STATE_IDLE){
                reset();
            }
        }
    };

    private int mVideoWidth;
    private int mVideoHeight;
    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
            new MediaPlayer.OnVideoSizeChangedListener() {
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();
                    Bundle bundle = BundlePool.obtain();
                    bundle.putInt(EventKey.INT_ARG1, mVideoWidth);
                    bundle.putInt(EventKey.INT_ARG2, mVideoHeight);
                    submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE,bundle);
                }
            };

    private MediaPlayer.OnCompletionListener mCompletionListener =
            new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    updateStatus(STATE_PLAYBACK_COMPLETE);
                    mTargetState = STATE_PLAYBACK_COMPLETE;
                    submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_PLAY_COMPLETE,null);
                }
            };

    private int startSeekPos;
    private MediaPlayer.OnInfoListener mInfoListener =
            new MediaPlayer.OnInfoListener() {
                public boolean onInfo(MediaPlayer mp, int arg1, int arg2) {
                    switch (arg1) {
                        case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                            PLog.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:");
                            break;
                        case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                            PLog.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START");
                            startSeekPos = 0;
                            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_RENDER_START,null);
                            break;
                        case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                            PLog.d(TAG, "MEDIA_INFO_BUFFERING_START:");
                            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_START,null);
                            break;
                        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                            PLog.d(TAG, "MEDIA_INFO_BUFFERING_END:");
                            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_END,null);
                            break;
                        case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                            PLog.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING:");
                            break;
                        case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                            PLog.d(TAG, "MEDIA_INFO_NOT_SEEKABLE:");
                            break;
                        case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                            PLog.d(TAG, "MEDIA_INFO_METADATA_UPDATE:");
                            break;
                        case MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                            PLog.d(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE:");
                            break;
                        case MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                            PLog.d(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT:");
                            break;
                    }
                    return true;
                }
            };

    private MediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = new MediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(MediaPlayer mp) {
            PLog.d(TAG,"EVENT_CODE_SEEK_COMPLETE");
            submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_COMPLETE,null);
        }
    };

    private MediaPlayer.OnErrorListener mErrorListener =
            new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
                    PLog.d(TAG, "Error: " + framework_err + "," + impl_err);
                    updateStatus(STATE_ERROR);
                    mTargetState = STATE_ERROR;

                    switch (framework_err){
                        case 100:
//                            release(true);
                            break;
                    }

                    /* If an error handler has been supplied, use it and finish. */
                    Bundle bundle = BundlePool.obtain();
                    submitErrorEvent(OnErrorEventListener.ERROR_EVENT_COMMON,bundle);
                    return true;
                }
            };

    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
            new MediaPlayer.OnBufferingUpdateListener() {
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    Bundle bundle = BundlePool.obtain();
                    bundle.putInt(EventKey.INT_DATA,percent);
                    submitPlayerEvent(OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_UPDATE, bundle);
                }
            };
}