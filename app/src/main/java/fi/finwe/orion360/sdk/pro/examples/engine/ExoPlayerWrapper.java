/*
 * Copyright (c) 2017-2019, Finwe Ltd. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package fi.finwe.orion360.sdk.pro.examples.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.io.IOException;
import java.util.Objects;

import fi.finwe.log.Logger;
import fi.finwe.orion360.sdk.pro.source.VideoPlayerWrapper;

/**
 * Wrapper for Google ExoPlayer.
 *
 * This class used to be part of Orion360 engine. It was moved outside of Orion360 when
 * ExoPlayer dependency was changed from internal .jar package to optional gradle dependency.
 *
 * This solves dependency issue in case Orion360 SDK user wishes to use a different ExoPlayer
 * version or another 3rd party library that comes with built-in ExoPlayer.
 *
 * This wrapper works with ExoPlayer version 2.4.1. If you choose to use different ExoPlayer
 * version, you probably need to modify this class according ExoPlayer's API changes.
 *
 * See GoogleExoPlayer example to find out how to use this wrapper.
 *
 * There is also a simplified example CustomExoPlayer if you want to write your own wrapper
 * from scratch.
 */
public class ExoPlayerWrapper extends VideoPlayerWrapper implements
        ExoPlayer.EventListener
{

    /** Tag for debug logging. */
    public static String TAG = ExoPlayerWrapper.class.getSimpleName();

    /** User agent string. */
    private final static String USER_AGENT = "Finwe Ltd. Orion360 VR Video Player v3.0 (Android)";

    /** Handle for ExoPlayer instance. */
    private SimpleExoPlayer mExoPlayer;
    //private FinweExoPlayer mExoPlayer;

    private final DefaultBandwidthMeter mDefaultBandwidthMeter;
    DataSource.Factory mMediaDataSourceFactory;

    private MediaSource mMediaSource;
    private int	mAudioSessionID = 0;

    /** Handler for video position updates. */
    private Handler	mMainHandler;   // Handler in main ui thread

    /** Task that handles video position update notifications. */
    private final VideoPositionUpdateTask	mPositionUpdater = new VideoPositionUpdateTask();

    private int	mPositionUpdateTimeoutMs = 200;

    /** Task that handles video buffer update notifications. */
    private final VideoBufferUpdateTask mBufferUpdater = new VideoBufferUpdateTask();


    /**
     * Constructor.
     *
     * @param context the context.
     */
    public ExoPlayerWrapper(Context context) {
        mContext = context;

        // Measures bandwidth during playback. Can be null if not required.
        mDefaultBandwidthMeter = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        mMediaDataSourceFactory = createDataSourceFactory(true);

        updateState();
    }

    DataSource.Factory createDataSourceFactory(boolean useBandwidthMeter) {
        DefaultBandwidthMeter meter = useBandwidthMeter ? mDefaultBandwidthMeter : null;
        String userAgent = Util.getUserAgent(mContext, USER_AGENT);
        return new DefaultDataSourceFactory(mContext, meter,
                new DefaultHttpDataSourceFactory(userAgent, meter));
    }

    private boolean isInPlaybackState() {
        synchronized (mCurrentStatus) {
            return (mExoPlayer != null
                    && mCurrentStatus.playerState != PlayerState.END
                    && mCurrentStatus.playerState != PlayerState.ERROR
                    && mCurrentStatus.playerState != PlayerState.IDLE
                    && mCurrentStatus.playerState != PlayerState.PREPARING);
        }
    }

    public boolean isPlaying() {
        synchronized (mCurrentStatus) {
            return isInPlaybackState() && mExoPlayer.getPlayWhenReady();
        }
    }

    @Override
    public boolean isReleased() {
        synchronized (mCurrentStatus) {
            return mCurrentStatus.playerState == PlayerState.END;
        }
    }

    @Override
    public long getCurrentPosition() {
        synchronized (mCurrentStatus) {
            if (isInPlaybackState() && mExoPlayer.getDuration() != C.TIME_UNSET) {
                return mExoPlayer.getCurrentPosition();
            } else {
                return -1;
            }
        }
    }

    @Override
    public long getDuration() {
        synchronized (mCurrentStatus) {
            if (isInPlaybackState() && mExoPlayer.getDuration() != C.TIME_UNSET) {
                return mExoPlayer.getDuration();
            } else {
                return -1;
            }
        }
    }

    @Override
    public int getBufferPercentage() {
        return mExoPlayer.getBufferedPercentage();
    }

    @Override
    public int getAudioSessionId() {
        return mAudioSessionID;
    }

    @Override
    public void setPositionUpdateTimeout(int timeoutMs) {
        if (timeoutMs < 0) {
            timeoutMs = 1;
        }
        mPositionUpdateTimeoutMs = timeoutMs;
    }

    // Proceed toward releasing everything
    @Override
    protected boolean processRelease() {
        // At this moment we can be in any state.
        // Decide what to do depending on the current state.
        boolean stop = false;
        boolean reset = false;
        boolean release = false;
        boolean finished = true;
        synchronized (mCurrentStatus) {
            switch (mCurrentStatus.playerState) {
                case STARTED:
                case PAUSED:
                case PLAYBACK_COMPLETED:
                case PREPARED:
                    stop = true;
                    break;
                case ERROR:
                case INITIALIZED:
                case STOPPED:
                    reset = true;
                    break;
                case IDLE:
                case PREPARING:
                    release = true;
                case END:
                    break;
            }
        }
        if (stop) {
            doStop();
            finished = false; // Return and come here once again.
        } else if (reset) {
            doReset();
        } else if (release) {
            doRelease();
        }

        return finished;
    }

    @Override
    protected boolean processCreatePlayer() {
        // At this moment the player should be in the END state.
        synchronized (mCurrentStatus) {
            if (mCurrentStatus.playerState == PlayerState.END) {
                mMainHandler = new Handler(Looper.getMainLooper());
                mMainHandler.postDelayed(mPositionUpdater, mPositionUpdateTimeoutMs);

                BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
                TrackSelection.Factory videoTrackSelectionFactory =	new AdaptiveTrackSelection.Factory(bandwidthMeter);
                TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
                mExoPlayer = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(mContext), trackSelector, new DefaultLoadControl());
                //mExoPlayer = new FinweExoPlayer(new DefaultRenderersFactory(mContext), trackSelector, new DefaultLoadControl());
                mExoPlayer.addListener(this);

                // *Debug*Listener??
                mExoPlayer.setAudioDebugListener(mAudioRendererEventListener);
                mExoPlayer.setVideoDebugListener(mVideoRendererEventListener);

                mCurrentStatus.playerState = PlayerState.IDLE;
            } else {
                return false;
            }
        }
        mUpdateListener.onVideoPlayerCreated(null);
        return true;
    }

    // Proceed toward resetting the player from any other state it might be in,
    // except END, which shouldn't happen.
    @Override
    protected boolean processResetPlayer() {
        // At this moment we can be in any state.
        // Decide what to do depending on the current state.
        boolean stop = false;
        boolean reset = false;
        synchronized (mCurrentStatus) {
            switch (mCurrentStatus.playerState) {
                case STARTED:
                case PAUSED:
                case PLAYBACK_COMPLETED:
                    stop = true;
                    break;
                case ERROR:
                case INITIALIZED:
                case PREPARED:
                case STOPPED:
                case PREPARING:
                    reset = true;
                    break;
                case IDLE:
                case END:
                    mCurrentStatus.uri = null;
                    break;
            }
        }
        if (stop) {
            doStop();
        } else if (reset) {
            doReset();
        }
        return false;
    }

    @Override
    protected boolean processSetDataSource() {
        // At this point we know we are in the IDLE state.
        synchronized (mCurrentStatus) {
            synchronized (mTargetStatus) {
                if (mTargetStatus.uri == null) {
                    mCurrentStatus.uri = null;
                    return false;
                }
                if (Objects.equals(mCurrentStatus.uri, mTargetStatus.uri)) {
                    return true;
                }
            }

            // Deduce the type of data source we need to set, branch accordingly
            if (mMediaSource != null) {
                mMediaSource.releaseSource();
                mMediaSource = null;
            }

            int type;
            type = Util.inferContentType(mTargetStatus.uri);
            switch (type) {
                case C.TYPE_SS:
                    mMediaSource = new SsMediaSource(mTargetStatus.uri,
                            createDataSourceFactory(false),
                            new DefaultSsChunkSource.Factory(mMediaDataSourceFactory),
                            mMainHandler, mAdaptiveMediaSourceEventListener);
                    break;
                case C.TYPE_DASH:
                    mMediaSource = new DashMediaSource(mTargetStatus.uri,
                            createDataSourceFactory(false),
                            new DefaultDashChunkSource.Factory(mMediaDataSourceFactory),
                            mMainHandler, mAdaptiveMediaSourceEventListener);
                    break;
                case C.TYPE_HLS:
                    mMediaSource = new HlsMediaSource(mTargetStatus.uri,
                            mMediaDataSourceFactory,
                            mMainHandler, mAdaptiveMediaSourceEventListener);
                    break;
                case C.TYPE_OTHER:
                    mMediaSource = new ExtractorMediaSource(mTargetStatus.uri,
                            mMediaDataSourceFactory,
                            new DefaultExtractorsFactory(),
                            mMainHandler,
                            e -> {
                                //Logger.logE(TAG, "ExtractorMediaSource.EventListener.onLoadError(): " + e.toString(), e.getCause());
                                Logger.logE(TAG, "ExtractorMediaSource.EventListener.onLoadError(): " + e.toString());
                                mUpdateListener.onException(null, e);
                            }
                    );
                    break;
                default: {
                    throw new IllegalStateException("Unsupported type: " + type);
                }
            }

            // No exceptions? Everything went better than expected
            mCurrentStatus.uri = mTargetStatus.uri;
            mCurrentStatus.playerState = PlayerState.INITIALIZED;
        }
        mUpdateListener.onVideoSourceURISet(null);

        return false;
    }

    @Override
    protected boolean processResetSurface() {
//		boolean stop = false;
        synchronized (mCurrentStatus) {
            switch (mCurrentStatus.playerState) {
                case STARTED:
                case PAUSED:
                case PLAYBACK_COMPLETED:
                case STOPPED:
                case INITIALIZED:
                case PREPARED:
                case PREPARING:
                case ERROR:
                case IDLE:
                    if (mCurrentStatus.surface != null) {
                        mExoPlayer.setVideoSurface(null);
                        // note: above operation is synchronous, so we can release surface.
                        synchronized (mTargetStatus) {
                            if (mTargetStatus.surface != mCurrentStatus.surface) {
                                mCurrentStatus.surface.release();
                            }
                        }
                        mCurrentStatus.surface = null;
                    }
                    break;
                case END:
                    break;
            }
        }
        return false;
    }

    @Override
    protected boolean processSetSurface() {
        // At this point we can be in any state.
        synchronized (mCurrentStatus) {
            synchronized (mTargetStatus) {
                if (mCurrentStatus.surface == mTargetStatus.surface || mExoPlayer == null) {
                    return true;
                }
                mCurrentStatus.surface = mTargetStatus.surface;
            }
            if (mCurrentStatus.surface != null) {
                mExoPlayer.setVideoSurface(mCurrentStatus.surface);
                return false;
            } else {
                return true;
            }
        }
    }

    @Override
    protected boolean processPrepare() {
        // This is easy: just prepare!
        synchronized (mCurrentStatus) {
            mCurrentStatus.playerState = PlayerState.PREPARING;
            if (mMediaSource == null) {
                Logger.logW(TAG, "Cannot prepare ExoPlayer, MediaSource == null!");
                return false;
            }
            // Prepare the ExoPlayer. true, true -> reset the player position and state in the process
            mCurrentStatus.preparing = true;
            mExoPlayer.prepare(mMediaSource, true, true);
            return true;
        }
    }

    @Override
    protected boolean processSeek() {
        synchronized (mCurrentStatus) {
            switch (mCurrentStatus.playerState) {
                case STARTED:
                case PREPARED:
                case PAUSED:
                case PLAYBACK_COMPLETED:
                    synchronized (mTargetStatus) {
                        // Cannot seek if duration is C.TIME_UNSET (is this correct?)
                        if (mExoPlayer.getDuration() == C.TIME_UNSET) {
                            mCurrentStatus.seekPosition = mTargetStatus.seekPosition = -1;
                            return false;
                        }

                        mCurrentStatus.seekPosition = mTargetStatus.seekPosition = Math.max(0, Math.min(mTargetStatus.seekPosition, getDuration()));
                    }
                    mCurrentStatus.seekActive = true;
                    mExoPlayer.seekTo(mCurrentStatus.seekPosition);
                    break;
                // Illegal states. Re-evaluate the logic.
                case STOPPED:
                case INITIALIZED:
                case PREPARING:
                case ERROR:
                case IDLE:
                case END:
                    return false;
            }
        }
        return true;
    }

    @Override
    protected boolean processSetVolume() {
        synchronized (mCurrentStatus) {
            switch (mCurrentStatus.playerState) {
                case STARTED:
                case PREPARED:
                case PAUSED:
                case PLAYBACK_COMPLETED:
                    synchronized (mTargetStatus) {
                        mCurrentStatus.volumeLevel = mTargetStatus.volumeLevel;
                    }
                    mExoPlayer.setVolume(mCurrentStatus.volumeLevel);
                    break;
                // Illegal states. Re-evaluate the logic.
                case STOPPED:
                case INITIALIZED:
                case PREPARING:
                case ERROR:
                case IDLE:
                case END:
                    return true;
            }
        }
        return false;  // false -> run updateStateOnce() again
    }

    @Override
    protected boolean processStartPlayer() {
        boolean notifyStarted = false;
        synchronized (mCurrentStatus) {
            switch (mCurrentStatus.playerState) {
                case STARTED:
                    break;
                case PLAYBACK_COMPLETED:
                    // Don't start again, unless looping is set true, or the player has been sought after playback completion
                    if (!mCurrentStatus.looping && mCurrentStatus.completionSeekRequired && !mCurrentStatus.completionSeekDone) {
                        break;
                    }
                    mCurrentStatus.playerState = PlayerState.PREPARING;
                    mCurrentStatus.preparing = true;
                    mExoPlayer.prepare(mMediaSource, true, false);  // Reset the position, but not the state (keep the same content)
                    return true;
                case PREPARED:
                case PAUSED:
                    mCurrentStatus.playerState = PlayerState.STARTED;
                    // Start the player outside the synchronization block. See below.
                    notifyStarted = true;
                    break;
                // Illegal states. Re-evaluate the logic.
                case STOPPED:
                case INITIALIZED:
                case PREPARING:
                case ERROR:
                case IDLE:
                case END:
                    return false;
            }
        }
        if (notifyStarted) {
            // This call may implicitly call onPlayerStateChanged, so we must do it outside the synchronized block
            mExoPlayer.setPlayWhenReady(true);
            mUpdateListener.onVideoStarted(null);
        }
        return true;
    }

    @Override
    protected boolean processStopPlayer() {
        // At this moment we can be in a couple of states
        boolean stop = false;
        synchronized (mCurrentStatus) {
            switch (mCurrentStatus.playerState) {
                case STOPPED:
                    break;
                case PREPARED:
                case STARTED:
                case PAUSED:
                case PLAYBACK_COMPLETED:
                    stop = true;
                    break;
                // Illegal states
                case ERROR:
                case INITIALIZED:
                case PREPARING:
                case IDLE:
                case END:
                    break;
            }
        }
        if (stop) {
            doStop();
        }
        return true;
    }

    @Override
    protected boolean processPausePlayer() {
        boolean notifyPaused = false;
        synchronized (mCurrentStatus) {
            switch (mCurrentStatus.playerState) {
                case PAUSED:
                    break;
                case STARTED:
                case PLAYBACK_COMPLETED:
                    // Pause the player outside the synchronization block. See below.
                    mCurrentStatus.playerState = PlayerState.PAUSED;
                    notifyPaused = true;
                    break;
                // Illegal states. Re-evaluate the logic.
                case PREPARED:
                case STOPPED:
                case INITIALIZED:
                case PREPARING:
                case ERROR:
                case IDLE:
                case END:
                    return false;
            }
        }
        if (notifyPaused) {
            // This call may implicitly call onPlayerStateChanged, so we must do it outside the synchronized block
            mExoPlayer.setPlayWhenReady(false);
            mUpdateListener.onVideoPaused(null);
        }
        return true;
    }

    private void doStop() {
        synchronized (mCurrentStatus) {
            mCurrentStatus.playerState = PlayerState.STOPPED;
            if (mExoPlayer != null) {
                mExoPlayer.stop();
            } else {
                Logger.logW(TAG, "Could not stop ExoPlayer in state "
                        + mCurrentStatus.playerState.name()
                        + ": ExoPlayer object was null. This could be due to failure to track state changes properly.");
            }
        }
        mUpdateListener.onVideoStopped(null);
    }

    private void doReset() {
        synchronized (mCurrentStatus) {
            if (mExoPlayer != null) {
                mCurrentStatus.playerState = PlayerState.IDLE;
                mExoPlayer.stop();
                mExoPlayer.seekTo(0);
            } else {
                mCurrentStatus.playerState = PlayerState.END;
                Logger.logW(TAG, "Could not reset ExoPlayer in state "
                        + mCurrentStatus.playerState.name()
                        + ": ExoPlayer object was null. This could be due to failure to track state changes properly.");
            }
        }
    }

    private void doRelease() {
        synchronized (mCurrentStatus) {
            mCurrentStatus.playerState = PlayerState.END;
            if (mExoPlayer != null) {
                mExoPlayer.release();
                mExoPlayer = null;
            } else {
                Logger.logW(TAG, "Could not release ExoPlayer in state "
                        + mCurrentStatus.playerState.name()
                        + ": ExoPlayer object was null. This could be due to failure to track state changes properly.");
            }

            if (mMediaSource != null) {
                mMediaSource.releaseSource();
                mMediaSource = null;
            }

            if (mMainHandler != null) {
                mMainHandler.removeCallbacks(mPositionUpdater);
                mMainHandler.removeCallbacks(mBufferUpdater);
                mMainHandler = null;
            }
        }
    }

    /**
     * Handler task executed in the main thread. The sole function is to periodically
     * notify any listeners about the updates to the current position of the video.
     */
    private class VideoPositionUpdateTask implements Runnable {
        @Override
        public void run() {
            long fromPosition;
            long toPosition;

            synchronized (mCurrentStatus) {
                if (mMainHandler == null) {
                    return;
                }
                fromPosition = mCurrentStatus.position;
                toPosition = getCurrentPosition();

                // ExoPlayer may report bigger current position than the duration when playback has
                // been completed. Clamp the position to the duration.
                long duration = getDuration();
                if (duration != C.TIME_UNSET && toPosition > duration) {
                    toPosition = duration;
                }
                mCurrentStatus.position = toPosition;
            }

            if (toPosition != -1 && toPosition != fromPosition) {
                mUpdateListener.onVideoPositionChanged(null, toPosition);
            }

            // Repost the position update task.
            synchronized (mCurrentStatus) {
                if (mMainHandler != null) {
                    mMainHandler.postDelayed(mPositionUpdater, mPositionUpdateTimeoutMs);
                }
            }
        }
    }

    /**
     * Handler task executed in the main thread. The sole function is to periodically
     * notify any listeners about the updates to the current buffer status of the video.
     */
    private class VideoBufferUpdateTask implements Runnable {
        @Override
        public void run() {
            int fromPercent;
            int toPercent;

            synchronized (mCurrentStatus) {
                if (mMainHandler == null || mExoPlayer == null) {
                    return;
                }
                fromPercent = mCurrentStatus.bufferingPercentage;
                toPercent = mExoPlayer.getBufferedPercentage();
                mCurrentStatus.bufferingPercentage = toPercent;

                // Note from API docs:
                // "An estimate of the percentage into the media up to which data is buffered.
                //  0 if the duration of the media is not known or if no estimate is available."
                // For example in case of HLS stream, seems to return 0.
            }

            if (toPercent != 0 && toPercent != fromPercent) {
                Logger.logD(TAG, "Video buffer changed: " + fromPercent + " -> " + toPercent);
                mUpdateListener.onVideoBufferingUpdate(null, fromPercent, toPercent);
            }

            synchronized (mCurrentStatus) {
                if (mMainHandler != null) {
                    mMainHandler.postDelayed(mBufferUpdater, 200);
                }
            }
        }
    }

    private static String getExoPlayerStateString(int state) {
        switch (state) {
            case ExoPlayer.STATE_BUFFERING: return "Buffering";
            case ExoPlayer.STATE_ENDED: return "Ended";
            case ExoPlayer.STATE_IDLE: return "Idle";
            case ExoPlayer.STATE_READY: return "Ready";
            default: return "Unknown(" + state + ")";
        }
    }

    // From ExoPlayer.EventListener:
    // Called when the value returned from either ExoPlayer.getPlayWhenReady() or ExoPlayer.getPlaybackState() changes.
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Logger.logD(TAG, "onPlayerStateChanged(): state = " + getExoPlayerStateString(playbackState) + ", playWhenReady = " + playWhenReady);

        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                // The player is prepared but not able to immediately play from the current position.
                synchronized (mCurrentStatus) {
                    mCurrentStatus.buffering = true;
                }
                mUpdateListener.onVideoBufferingStart(null);
                if (mMainHandler != null) {
                    mMainHandler.postDelayed(mBufferUpdater, 500);
                }
                break;
            case ExoPlayer.STATE_ENDED:
                // The player has finished playing the media.
                synchronized (mCurrentStatus) {
                    mCurrentStatus.playerState = PlayerState.PLAYBACK_COMPLETED;
                    if (!mCurrentStatus.looping) {
                        // The player was not set to loop upon completion -> seek is needed to resume playback
                        mCurrentStatus.completionSeekRequired = true;
                        mCurrentStatus.completionSeekDone = false;
                    }
                }
                mUpdateListener.onVideoCompleted(null);
                postUpdateState();
                break;
            case ExoPlayer.STATE_IDLE:
                // The player is neither prepared or being prepared.
                break;
            case ExoPlayer.STATE_READY:
                // The player is prepared and able to immediately play from the current position.
                synchronized (mCurrentStatus) {
                    if (mCurrentStatus.preparing) {
                        mCurrentStatus.preparing = false;
                        mCurrentStatus.playerState = PlayerState.PREPARED;
                        long durationMs = mExoPlayer.getDuration();
                        if (durationMs != C.TIME_UNSET) {
                            mUpdateListener.onVideoDurationUpdate(null, durationMs);
                        } else {
                            mUpdateListener.onVideoDurationUpdate(null, -1);
                        }
                        mUpdateListener.onVideoPrepared(null);
                        postUpdateState();
                    }
                    if (mCurrentStatus.buffering) {
                        mCurrentStatus.buffering = false;
                        mUpdateListener.onVideoBufferingEnd(null);
                        if (mMainHandler != null) {
                            mMainHandler.removeCallbacks(mBufferUpdater);
                        }
                    }
                    if (mCurrentStatus.seekActive) {
                        // Mark the seek position in the target status back to -1,
                        // if it hasn't changed during the time we were seeking.
                        synchronized (mTargetStatus) {
                            if (mTargetStatus.seekPosition == mCurrentStatus.seekPosition) {
                                mTargetStatus.seekPosition = -1;
                            }
                        }
                        // Mark the current seek as having finished.
                        mCurrentStatus.seekActive = false;
                        mCurrentStatus.completionSeekDone = true;
                        mCurrentStatus.seekPosition = -1;
                        mUpdateListener.onVideoSeekCompleted(null, mExoPlayer.getCurrentPosition());
                        postUpdateState();
                    }
                }
                break;
            default:
                Logger.logE(TAG, "Unhandled ExoPlayer state change:" + playbackState);
                break;
        }
    }

    // From ExoPlayer.EventListener:
    // Called when the timeline and/or manifest has been refreshed.
    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        Logger.logD(TAG, "onTimelineChanged()");
    }

    // From ExoPlayer.EventListener:
    // Called when the available or selected tracks change.
    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        Logger.logD(TAG, "onTracksChanged()");
    }

    // From ExoPlayer.EventListener:
    // Called when the player starts or stops loading the source.
    @Override
    public void onLoadingChanged(boolean isLoading) {
        Logger.logD(TAG, "onLoadingChanged(): " + isLoading);
    }

    // From ExoPlayer.EventListener:
    // Called when a position discontinuity occurs without a change to the timeline.
    @Override
    public void onPositionDiscontinuity() {
        Logger.logD(TAG, "onPositionDiscontinuity()");
    }

    // From ExoPlayer.EventListener:
    // Called when the current playback parameters change.
    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        Logger.logD(TAG, "onPlaybackParametersChanged()");
    }

    // From ExoPlayer.EventListener:
    @Override
    public void onPlayerError(ExoPlaybackException e) {
        Logger.logE(TAG, "onPlayerError(): " + e.toString(), e.getCause());

        // NOTE: ExoPlayer can actually be used even after getting this call.
        // Internally the player is in ExoPlayer.STATE_IDLE immediately after this method is called
        // Also, the player must be released with ExoPlayer.release() afterwards.

        synchronized (mCurrentStatus) {
            mCurrentStatus.playerState = PlayerState.ERROR;
            synchronized (mTargetStatus) {
                mTargetStatus.playerState = PlayerState.END;
            }
        }
        // This API does not work for ExoPlayer errors. We have an Exception, not error codes...
        mUpdateListener.onVideoError(null, 0, 0);
        postUpdateState();
    }

    private final VideoRendererEventListener mVideoRendererEventListener = new VideoRendererEventListener() {
        @Override
        public void onVideoEnabled(DecoderCounters decoderCounters) {
//			Logger.logD(TAG, "ExoPlayer onVideoEnabled()");
        }

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            Logger.logD(TAG, "ExoPlayer onVideoDecoderInitialized(): name = " + decoderName + ", init timestamp = " + initializedTimestampMs + " ms, duration = " + initializationDurationMs + " ms");
        }

        @Override
        public void onVideoInputFormatChanged(Format format) {
            Logger.logD(TAG, "ExoPlayer onVideoInputFormatChanged()");
        }

        @Override
        public void onDroppedFrames(int count, long elapsedMs) {
            Logger.logW(TAG, "ExoPlayer onDroppedFrames(): count = " + count + ", elapsedMs = " + elapsedMs);
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            Logger.logI(TAG, "ExoPlayer onVideoSizeChanged(): size = " +
                    width + " x " + height + ", unapplied rotation = " + unappliedRotationDegrees + ", pixelWidthHeightRatio= " + pixelWidthHeightRatio);

            mUpdateListener.onVideoSizeChanged(null, width, height);
        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {
            Logger.logD(TAG, "ExoPlayer onRenderedFirstFrame()");
            mUpdateListener.onVideoRenderingStart(null);
        }

        @Override
        public void onVideoDisabled(DecoderCounters decoderCounters) {
//			Logger.logD(TAG, "ExoPlayer onVideoDisabled()");
        }
    };

    private final AudioRendererEventListener mAudioRendererEventListener = new AudioRendererEventListener() {
        @Override
        public void onAudioEnabled(DecoderCounters decoderCounters) {
//			Logger.logD(TAG, "ExoPlayer onAudioEnabled()");
        }

        @Override
        public void onAudioSessionId(int audioSessionId) {
            mAudioSessionID = audioSessionId;
        }

        @Override
        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            Logger.logD(TAG, "ExoPlayer onAudioDecoderInitialized(): name = " + decoderName + ", init timestamp = " + initializedTimestampMs + " ms, duration = " + initializationDurationMs + " ms");
        }

        @Override
        public void onAudioInputFormatChanged(Format format) {
            Logger.logD(TAG, "ExoPlayer onAudioInputFormatChanged()");
        }

        @Override
        public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            Logger.logW(TAG, "ExoPlayer onDroppedFrames(): bufferSize = " + bufferSize + ", bufferSizeMs = " + bufferSizeMs + ", elapsedSinceLastFeedMs = " + elapsedSinceLastFeedMs);
        }

        @Override
        public void onAudioDisabled(DecoderCounters decoderCounters) {
//			Logger.logD(TAG, "ExoPlayer onAudioDisabled()");
        }
    };

    private final AdaptiveMediaSourceEventListener mAdaptiveMediaSourceEventListener = new AdaptiveMediaSourceEventListener() {
        @Override
        public void onLoadStarted(DataSpec dataSpec, int i, int i1, Format format, int i2, Object o, long l, long l1, long l2) {

        }

        @Override
        public void onLoadCompleted(DataSpec dataSpec, int i, int i1, Format format, int i2, Object o, long l, long l1, long l2, long l3, long l4) {

        }

        @Override
        public void onLoadCanceled(DataSpec dataSpec, int i, int i1, Format format, int i2, Object o, long l, long l1, long l2, long l3, long l4) {

        }

        @Override
        public void onLoadError(DataSpec dataSpec, int i, int i1, Format format, int i2, Object o, long l, long l1, long l2, long l3, long l4, IOException e, boolean b) {

        }

        @Override
        public void onUpstreamDiscarded(int i, long l, long l1) {

        }

        @Override
        public void onDownstreamFormatChanged(int i, Format format, int i1, Object o, long l) {

        }
    };
}
