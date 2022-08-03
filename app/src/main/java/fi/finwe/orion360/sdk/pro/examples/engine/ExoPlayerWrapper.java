/*
 * Copyright (c) 2022, Finwe Ltd. All rights reserved.
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
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.analytics.DefaultAnalyticsCollector;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.decoder.DecoderReuseEvaluation;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaLoadData;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.ads.AdsLoader;
import com.google.android.exoplayer2.source.ads.AdsMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.AdViewProvider;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoSize;
import com.google.common.collect.ImmutableList;

import java.io.IOException;

import fi.finwe.log.Logger;
import fi.finwe.orion360.sdk.pro.texture.VideoPlayerWrapper;

/**
 * Wrapper for Google ExoPlayer.
 */
public class ExoPlayerWrapper extends VideoPlayerWrapper implements Player.Listener {

    /** Tag for logging. */
    public static final String TAG = ExoPlayerWrapper.class.getSimpleName();

    /** User agent string. */
    private final static String USER_AGENT = "Finwe Ltd. Orion360 VR Video Player v4.0 (Android)";

    private final DefaultBandwidthMeter BANDWIDTH_METER;

    /** HLS filename extension. */
    private final static String HLS_FILENAME_EXT = ".m3u8";

    /** Buffer size for a single segment. */
    private final static int BUFFER_SEGMENT_SIZE = 256 * 1024;
    // above: in total 262144 bytes (256kB), value comes from ExoPlayer demo app.

    /** Number of segments in the buffer. */
    private final static int BUFFER_SEGMENTS = 64;
    // above: in total 16777216 bytes (16MB), value comes from ExoPlayer demo app.

    private static final long	DEFAULT_ALLOWED_JOINING_TIME_MS = 5000;
    private static final int	DEFAULT_MAX_DROPPED_FRAME_COUNT_TO_NOTIFY = 50;

//	private static final int ERROR_EXTRA_SECURITY_EXCEPTION_WHEN_SET_DATA_SOURCE= 0x1FF;
//	private static final int ERROR_EXTRA_ILLEGAL_STATE= 0x2FF;

    /** Handle for ExoPlayer instance. */
    private ExoPlayer mExoPlayer;

    private final DataSource.Factory mDataSourceFactory;
    private final DefaultMediaSourceFactory mMediaSourceFactory;

    private DefaultTrackSelector mTrackSelector;

    private MediaSource mMediaSource;
    private String mOverrideExtension = "";  // Set to override media type inference from the uri
    private int	mAudioSessionID = 0;

    /** Task that handles video position update notifications. */
    private final VideoPositionUpdateTask mPositionUpdater = new VideoPositionUpdateTask();

    private int	mPositionUpdateTimeoutMs = 200;

    /** Task that handles video buffer update notifications. */
    private final VideoBufferUpdateTask mBufferUpdater = new VideoBufferUpdateTask();

    private String mAdTag = null;
    private AdsLoader mAdsLoader = null;
    private AdViewProvider mAdViewProvider = null;


    public ExoPlayerWrapper(Context context) {
        mContext = context;

        BANDWIDTH_METER = new DefaultBandwidthMeter.Builder(mContext).build();

        // Produces DataSource instances through which media data is loaded.
//		mDataSourceFactory = buildDataSourceFactory(true);
        mDataSourceFactory = buildDataSourceFactory();
        mMediaSourceFactory = new DefaultMediaSourceFactory(
                mDataSourceFactory
                //mContext, new DefaultExtractorsFactory()
        );

        updateState();
    }

    // TODO: There seems to be nowhere to input the DefaultBandwidthMeter. Find out what happened to it.
//	private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
////		DefaultBandwidthMeter meter = useBandwidthMeter ? BANDWIDTH_METER : null;
//
//		String userAgent = Util.getUserAgent(mContext, USER_AGENT);
//		DefaultHttpDataSource.Factory defaultHttpDataSourceFactory = new DefaultHttpDataSource.Factory()
//				.setUserAgent(userAgent);
//		return new DefaultDataSource.Factory(mContext, defaultHttpDataSourceFactory);
//	}

    private DataSource.Factory buildDataSourceFactory() {
        String userAgent = Util.getUserAgent(mContext, USER_AGENT);
        DefaultHttpDataSource.Factory defaultHttpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent);
        return new DefaultDataSource.Factory(mContext, defaultHttpDataSourceFactory);
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

    // Gets the ExoPlayer handle, if it happens to be created at call time. Use at own peril.
    // The player is created just before the call to onVideoPlayerCreated callback until just after the call to onVideoPlayerReleased
    public ExoPlayer getExoPlayer() {
        return mExoPlayer;
    }

    public DefaultMediaSourceFactory getDefaultMediaSourceFactory() {
        return mMediaSourceFactory;
    }

    public void setAdTag(@Nullable String adTag) {
        mAdTag = adTag;
    }

    public void setAdsLoader(AdsLoader loader) {
        mAdsLoader = loader;
    }

    public void setAdViewProvider(AdViewProvider provider) {
        mAdViewProvider = provider;
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

    @Override
    protected boolean processCreatePlayer() {
        // At this moment the player should be in the END state.
        synchronized (mCurrentStatus) {
            if (mCurrentStatus.playerState == PlayerState.END) {
                mMainHandler.postDelayed(mPositionUpdater, mPositionUpdateTimeoutMs);

                DefaultRenderersFactory defaultRenderersFactory = new DefaultRenderersFactory(mContext);

                AdaptiveTrackSelection.Factory videoTrackSelectionFactory =	new AdaptiveTrackSelection.Factory();
                mTrackSelector = new DefaultTrackSelector(mContext, videoTrackSelectionFactory);

                DefaultAnalyticsCollector defaultAnalyticsCollector = new DefaultAnalyticsCollector(Clock.DEFAULT);

                ExoPlayer.Builder exoplayerBuilder = new ExoPlayer.Builder(mContext, defaultRenderersFactory, mMediaSourceFactory, mTrackSelector, new DefaultLoadControl(), BANDWIDTH_METER, defaultAnalyticsCollector);
                mExoPlayer = exoplayerBuilder.build();
                if (null != mAdsLoader) {
                    mAdsLoader.setPlayer(mExoPlayer);
                }
                mExoPlayer.addListener(this);
                mExoPlayer.addAnalyticsListener(mAnalyticsListener);

                mCurrentStatus.playerState = PlayerState.IDLE;
                postVideoPlayerCreated();
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean processSetDataSource() {
        // At this point we know we are in the IDLE state.
//		int notifyError = 0, errorextra = 0;
        synchronized (mCurrentStatus) {
            synchronized (mTargetStatus) {
                if (mTargetStatus.uri == null) {
                    mCurrentStatus.uri = null;
                    return false;
                }
                if (mCurrentStatus.uri == mTargetStatus.uri || (mCurrentStatus.uri != null && mCurrentStatus.uri.equals(mTargetStatus.uri))) {
                    return true;
                }
            }

            // Deduce the type of data source we need to set, branch accordingly
            MediaItem mediaItem;
            if (null != mAdTag && !mAdTag.isEmpty()) {
                mediaItem = new MediaItem.Builder()
                        .setUri(mTargetStatus.uri)
                        .setAdsConfiguration(
                                new MediaItem.AdsConfiguration.Builder(Uri.parse(mAdTag)).build())
                        .build();
                Logger.logD(TAG, "Using ad tag: " + mAdTag);
            } else {
                mediaItem = MediaItem.fromUri(mTargetStatus.uri);
            }

            int type;
            if (mOverrideExtension.length() == 0) {
                type = Util.inferContentType(mTargetStatus.uri);
            } else {
                type = Util.inferContentTypeForExtension(mOverrideExtension);
            }
            DataSource.Factory factory;
            switch (type) {
                case C.CONTENT_TYPE_SS:
                    SsMediaSource.Factory ssMediaSourceFactory = new SsMediaSource.Factory(
                        new DefaultSsChunkSource.Factory(mDataSourceFactory),
//							buildDataSourceFactory(false)
                        buildDataSourceFactory());
                    SsMediaSource ssMediaSource =
                            ssMediaSourceFactory.createMediaSource(mediaItem);
                    if (null != mAdTag && !mAdTag.isEmpty()) {
                        DataSpec adTagDataSpec = new DataSpec(Uri.parse(mAdTag));
                        Object adsId = mAdTag;
                        mMediaSource = new AdsMediaSource(
                                ssMediaSource,
                                adTagDataSpec,
                                adsId,
                                ssMediaSourceFactory,
                                mAdsLoader,
                                mAdViewProvider);
                    } else {
                        mMediaSource = ssMediaSource;
                    }
                    mMediaSource.addEventListener(mMainHandler, mAdaptiveMediaSourceEventListener);
                    break;
                case C.CONTENT_TYPE_DASH:
                    DashMediaSource.Factory dashMediaSourceFactory = new DashMediaSource.Factory(
                            new DefaultDashChunkSource.Factory(mDataSourceFactory),
//							buildDataSourceFactory(false)
                            buildDataSourceFactory());
                    DashMediaSource dashMediaSource =
                            dashMediaSourceFactory.createMediaSource(mediaItem);
                    if (null != mAdTag && !mAdTag.isEmpty()) {
                        DataSpec adTagDataSpec = new DataSpec(Uri.parse(mAdTag));
                        Object adsId = mAdTag;
                        mMediaSource = new AdsMediaSource(
                                dashMediaSource,
                                adTagDataSpec,
                                adsId,
                                dashMediaSourceFactory,
                                mAdsLoader,
                                mAdViewProvider);
                    } else {
                        mMediaSource = dashMediaSource;
                    }
                    mMediaSource.addEventListener(mMainHandler, mAdaptiveMediaSourceEventListener);
                    break;
                case C.CONTENT_TYPE_HLS:
                    HlsMediaSource.Factory hlsMediaSourceFactory = new HlsMediaSource.Factory(
                            mDataSourceFactory);
                    HlsMediaSource hlsMediaSource =
                            hlsMediaSourceFactory.createMediaSource(mediaItem);
                    if (null != mAdTag && !mAdTag.isEmpty()) {
                        DataSpec adTagDataSpec = new DataSpec(Uri.parse(mAdTag));
                        Object adsId = mAdTag;
                        mMediaSource = new AdsMediaSource(
                                hlsMediaSource,
                                adTagDataSpec,
                                adsId,
                                hlsMediaSourceFactory,
                                mAdsLoader,
                                mAdViewProvider);
                    } else {
                        mMediaSource = hlsMediaSource;
                    }
                    mMediaSource.addEventListener(mMainHandler, mAdaptiveMediaSourceEventListener);
                    break;
                case C.CONTENT_TYPE_OTHER:
                    ProgressiveMediaSource.Factory progressiveMediaSourceFactory =
                            new ProgressiveMediaSource.Factory(mDataSourceFactory,
                                    new DefaultExtractorsFactory());
                    ProgressiveMediaSource progressiveMediaSource =
                            progressiveMediaSourceFactory.createMediaSource(mediaItem);
                    if (null != mAdTag && !mAdTag.isEmpty()) {
                        DataSpec adTagDataSpec = new DataSpec(Uri.parse(mAdTag));
                        Object adsId = mAdTag;
                        mMediaSource = new AdsMediaSource(
                                progressiveMediaSource,
                                adTagDataSpec,
                                adsId,
                                progressiveMediaSourceFactory,
                                mAdsLoader,
                                mAdViewProvider);
                    } else {
                        mMediaSource = progressiveMediaSource;
                    }
                    mMediaSource.addEventListener(mMainHandler, new MediaSourceEventListener() {
                        @Override
                        public void onLoadError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo,
                                                MediaLoadData mediaLoadData, IOException error, boolean wasCanceled)
                        {
                            Logger.logE(TAG, "MediaSourceEventListener.onLoadError(): " + error.toString());
                            postException(error);
                            // FIXME this API does not work for ExoPlayer errors. We have an Exception, not error codes!
                            postVideoError(0, 0);
                        }
                    });
                    break;
                default: {
                    mMediaSource = null;
                    throw new IllegalStateException("Unsupported type: " + type);
                }
            }

            // No exceptions? Everything went better than expected
            mCurrentStatus.uri = mTargetStatus.uri;
            mCurrentStatus.playerState = PlayerState.INITIALIZED;
            postVideoSourceUriSet();
        }
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
                    // stop = true;
                    // break;
                case STOPPED:
                case INITIALIZED:
                case PREPARED:
                case PREPARING:
                case ERROR:
                case IDLE:
                    if (mCurrentStatus.surface != null) {
                        // TODO: Previously this was done with blockingSendMessage, do we need to block here as well?
                        mExoPlayer.setVideoSurface(null);
                        // note: above operation is synchronous, so we can release surface.
                        // TODO: Really?
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
//		if (stop) {
//			doStop();
//		}
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
        synchronized (mCurrentStatus) {
            mCurrentStatus.playerState = PlayerState.PREPARING;
            if (mMediaSource == null) {
                Logger.logW(TAG, "Cannot prepare ExoPlayer, MediaSource == null!");
                return false;
            }
            mCurrentStatus.preparing = true;
            mExoPlayer.setMediaSource(mMediaSource, true);
            mExoPlayer.prepare();
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
        boolean start = false;
        synchronized (mCurrentStatus) {
            switch (mCurrentStatus.playerState) {
                case STARTED:
                    break;
                case PLAYBACK_COMPLETED:
                    // Don't start again, unless looping is set true, or seek has been done after finishing the video
                    if (mCurrentStatus.looping == false && mCurrentStatus.startAllowedInPlaybackCompleted == false) {
                        break;
                    }
                    // Otherwise, restart the video by calling prepare again
                    mCurrentStatus.playerState = PlayerState.PREPARING;
                    mCurrentStatus.preparing = true;
                    mExoPlayer.setMediaSource(mMediaSource, true);	 // Reset the position
                    mExoPlayer.prepare();
                    return true;
                case PREPARED:
                case PAUSED:
                    mCurrentStatus.playerState = PlayerState.STARTED;
                    mExoPlayer.setPlayWhenReady(true);
                    postVideoStarted();
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
    protected boolean processPausePlayer() {
        synchronized (mCurrentStatus) {
            switch (mCurrentStatus.playerState) {
                case PLAYBACK_COMPLETED:
                case PAUSED:
                    break;
                case STARTED:
                    doPause();
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
        return true;
    }

    protected boolean processSystemPausePlayer() {
        // If the playback has been paused by the system, pause the player only if it's in STARTED state,
        // i.e. NOT in PLAYBACK_COMPLETED state
        Logger.logD(TAG, "In processPausePlayer()");
        synchronized (mCurrentStatus) {
            if (mCurrentStatus.playerState == PlayerState.STARTED) {
                doPause();
            }
        }
        return true;
    }

    void doPause() {
        mCurrentStatus.playerState = PlayerState.PAUSED;
        mCurrentStatus.startAllowedInPlaybackCompleted = false;
        mExoPlayer.setPlayWhenReady(false);
        postVideoPaused();
    }

    @Override
    protected void doStop() {
        mCurrentStatus.playerState = PlayerState.STOPPED;
        mCurrentStatus.startAllowedInPlaybackCompleted = false;
        mExoPlayer.stop();
        postVideoStopped();
    }

    @Override
    protected void doReset() {
        if (mExoPlayer != null) {
            mCurrentStatus.playerState = PlayerState.IDLE;
            mCurrentStatus.startAllowedInPlaybackCompleted = false;
            // TODO ExoPlayer does not have reset() method. Is this correct replacement?
            mExoPlayer.stop();
            mExoPlayer.seekTo(0);
        } else {
            mCurrentStatus.playerState = PlayerState.END;
            Logger.logW(TAG, "Could not reset ExoPlayer in state "
                    + mCurrentStatus.playerState.name()
                    + ": ExoPlayer object was null. This could be due to failure to track state changes properly.");
        }
    }

    @Override
    protected void doRelease() {
        postVideoReleased();

        mCurrentStatus.playerState = PlayerState.END;
        if (mExoPlayer != null) {
            mExoPlayer.release();
            mExoPlayer = null;
        } else {
            // This could be due to failure to track state changes properly.
            Logger.logW(TAG, "Could not release ExoPlayer in state " + mCurrentStatus.playerState.name() + ": ExoPlayer object was null. ");
        }

        mMediaSource = null;

        mMainHandler.removeCallbacks(mPositionUpdater);
        mMainHandler.removeCallbacksAndMessages(mPositionUpdaterToken);
        mMainHandler.removeCallbacks(mBufferUpdater);

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
                postVideoPositionChanged(toPosition);
            }

            // Repost the position update task.
            // TODO: No need to do this while paused
            synchronized (mCurrentStatus) {
                if (mExoPlayer != null) {
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
                if (mExoPlayer == null) {
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
                postVideoBufferingUpdate(fromPercent, toPercent);
            }

            synchronized (mCurrentStatus) {
                if (mExoPlayer != null) {
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
            default: return "Unknown(" + Integer.toString(state) + ")";
        }
    }

    // From Player.Listener:
    // Called when the value returned from getPlaybackState() changes.
    @Override
    public void onPlaybackStateChanged(@Player.State int playbackState) {
        Logger.logV(TAG, "onPlayerStateChanged(): state = " + getExoPlayerStateString(playbackState));

        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                // The player is prepared but not able to immediately play from the current position.
                synchronized (mCurrentStatus) {
                    mCurrentStatus.buffering = true;
                }
                mMainHandler.postDelayed(mBufferUpdater, 500);
                postVideoBufferingStart();
                break;
            case ExoPlayer.STATE_ENDED:
                // The player has finished playing the media.
                synchronized (mCurrentStatus) {
                    mCurrentStatus.playerState = PlayerState.PLAYBACK_COMPLETED;
                    mCurrentStatus.startAllowedInPlaybackCompleted = false;
                }
                postVideoCompleted();
                postUpdateState();
                break;
            case ExoPlayer.STATE_IDLE:
                // The player is neither prepared or being prepared.
                break;
            case ExoPlayer.STATE_READY: {
                // The player is prepared and able to immediately play from the current position.
                // Don't keep the synchronization locks when calling callbacks
                synchronized (mCurrentStatus) {
                    if (mCurrentStatus.preparing) {
                        mCurrentStatus.preparing = false;
                        mCurrentStatus.playerState = PlayerState.PREPARED;
                        long durationMs = mExoPlayer.getDuration();
                        if (durationMs == C.TIME_UNSET) {
                            durationMs = -1;
                        }
                        postVideoDurationUpdate(durationMs);
                        postVideoPrepared();
                        postUpdateState();
                    }
                    if (mCurrentStatus.buffering) {
                        mCurrentStatus.buffering = false;
                        mMainHandler.removeCallbacks(mBufferUpdater);
                        postVideoBufferingEnd();
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
                        mCurrentStatus.seekPosition = -1;
                        // Allow proceeding from the PLAYBACK_COMPLETED state
                        mCurrentStatus.startAllowedInPlaybackCompleted = true;
                        postVideoSeekCompleted(mExoPlayer.getCurrentPosition());
                        postUpdateState();
                    }
                }
                break;
            }
            default:
                Logger.logE(TAG, "Unhandled ExoPlayer state change:" + playbackState);
                break;
        }
    }

    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, @Player.PlayWhenReadyChangeReason int reason) {
        Logger.logV(TAG, "onPlayWhenReadyChanged(): playWhenReady = " + playWhenReady + ", reason = " + reason);
    }

    // From Player.Listener:
    // Called when the timeline and/or manifest has been refreshed.
    @Override
    public void onTimelineChanged(Timeline timeline, @Player.TimelineChangeReason int reason) {
        Logger.logV(TAG, "onTimelineChanged()");
    }

    // From Player.Listener:
    // Called when the available or selected tracks change.
    @Override
    public void onTracksChanged(Tracks tracks) {
        Logger.logV(TAG, "onTracksChanged()");

        // All available tracks in stream.
//		for (int i = 0; i < trackGroups.length; i++) {
//            TrackGroup trackGroup = trackGroups.get(i);
//            if (trackGroup == null) {
//				continue;
//			}
//            Logger.logD(TAG, "TrackGroup " + i + ": " + trackGroup);
//            for (int j = 0; j < trackGroup.length; j++) {
//                Format format = trackGroup.getFormat(j);
//                Logger.logD(TAG, "  Format " + j + ": " + format);
//            }
//        }

        ImmutableList<Tracks.Group> groups = tracks.getGroups();
        for (int m = 0; m < groups.size(); ++m) {
            Tracks.Group group = groups.get(m);
            for (int n = 0; n < group.length; ++n) {
                if (group.isTrackSelected(n) == false) {
                    continue;
                }
                Format format = group.getTrackFormat(n);
                Logger.logD(TAG, "Track group = " + m + ", track = " + n + ", format = " + format);
                // TODO: How to access the ExoTrackSelection interface? It would have the getSelectionReason etc
            }
        }

        // Tracks that can be played on this device / selected for playback.
//        for (int i = 0; i < trackSelections.length; i++) {
//            TrackSelection trackSelection = trackSelections.get(i);
//            Logger.logD(TAG, "TrackSelection " + i + ": " + trackSelection);
//            if (trackSelection == null) continue;
//            for (int j = 0; j < trackSelection.length(); j++) {
//                Logger.logD(TAG, "  Format " + j + ": " + trackSelection.getFormat(j));
//            }
//            Format selectedFormat = trackSelection.getSelectedFormat();
//            int selectionReason = trackSelection.getSelectionReason();
//            String reason;
//            switch (selectionReason) {
//                case 0:
//                    reason = "UNKNOWN";
//                    break;
//                case 1:
//                    reason = "INITIAL";
//                    break;
//                case 2:
//                    reason = "MANUAL";
//                    break;
//                case 3:
//                    reason = "ADAPTIVE";
//                    break;
//                default:
//                    reason = "CUSTOM"; // 10000 and above
//                    break;
//            }
//            Logger.logD(TAG, "  Selected format "  + selectedFormat + " for reason " + reason);
//        }

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = mTrackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo != null) {
            if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO) == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                Logger.logW(TAG, "Unsupported video track!");
            }
            if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO) == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                Logger.logW(TAG, "Unsupported audio track!");
            }
        }
    }

//	@Override
//	public void onTrackSelectionParametersChanged(TrackSelectionParameters trackSelectionParameters) {
//	}

    // From Player.Listener:
    // Called when the player starts or stops loading the source.
    @Override
    public void onIsLoadingChanged(boolean isLoading) {
        Logger.logV(TAG, "onLoadingChanged(): " + isLoading);
    }

    // From Player.Listener:
    // Called when the current playback parameters change.
    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        Logger.logV(TAG, "onPlaybackParametersChanged()");
    }

    // From Player.Listener:
    @Override
    public void onPlayerError(PlaybackException e) {
        Logger.logE(TAG, "onPlayerError(): " + e.toString(), e.getCause());

        // FIXME: ExoPlayer can actually be used even after getting this call.
        // Internally the player is in ExoPlayer.STATE_IDLE immediately after this method is called
        // Also, the player must be released with ExoPlayer.release() afterwards.

        synchronized (mCurrentStatus) {
            mCurrentStatus.playerState = PlayerState.ERROR;
            synchronized (mTargetStatus) {
                mTargetStatus.playerState = PlayerState.END;
            }
        }
        postException(e);
        // FIXME this API does not work for ExoPlayer errors. We have an Exception, not error codes!
        postVideoError(0, 0);
        postUpdateState();
    }

    // From Player.Listener:
    // Called when a position discontinuity occurs without a change to the timeline.
    @Override
    public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
        Logger.logV(TAG, "onPositionDiscontinuity()");
    }

    private AnalyticsListener mAnalyticsListener = new AnalyticsListener() {

        @Override
        public void onVideoEnabled(EventTime eventTime, DecoderCounters decoderCounters) {
            Logger.logV(TAG, "AnalyticsListener.onVideoEnabled()");
        }

        @Override
        public void onVideoDisabled(EventTime eventTime, DecoderCounters decoderCounters) {
            Logger.logV(TAG, "AnalyticsListener.onVideoDisabled()");
        }

        @Override
        public void onVideoDecoderInitialized(EventTime eventTime, String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            Logger.logV(TAG, "AnalyticsListener.onVideoDecoderInitialized(): name = " + decoderName + ", init timestamp = " + initializedTimestampMs + " ms, duration = " + initializationDurationMs + " ms");
        }

        @Override
        public void onVideoInputFormatChanged(EventTime eventTime, Format format, @Nullable DecoderReuseEvaluation decoderReuseEvaluation) {
            Logger.logI(TAG, "AnalyticsListener.onVideoInputFormatChanged(): format = " + format);
        }

        @Override
        public void onDroppedVideoFrames(EventTime eventTime, int count, long elapsedMs) {
            Logger.logW(TAG, "AnalyticsListener.onDroppedFrames(): count = " + count + ", elapsedMs = " + elapsedMs);
        }

        @Override
        public void onVideoSizeChanged(EventTime eventTime, VideoSize videoSize) {
            Logger.logI(TAG, "AnalyticsListener.onVideoSizeChanged(): size = " +
                    + videoSize.width + " x " + videoSize.height + ", unapplied rotation = " + videoSize.unappliedRotationDegrees + ", pixelWidthHeightRatio= " + videoSize.pixelWidthHeightRatio);

            // TODO: We are not using unappliedRotationDegrees or pixelWidthHeightRatio. Is the former ever other than 0 or latter other than 1.0?
            postVideoSizeChanged(videoSize.width, videoSize.height);
        }

        @Override
        public void onRenderedFirstFrame(EventTime eventTime, Object output, long renderTimeMs) {
            Logger.logV(TAG, "AnalyticsListener.onRenderedFirstFrame()");
            postVideoRenderingStarted();
        }

        @Override
        public void onAudioEnabled(EventTime eventTime, DecoderCounters decoderCounters) {
            Logger.logV(TAG, "AnalyticsListener.onAudioEnabled()");
        }

        @Override
        public void onAudioDisabled(EventTime eventTime, DecoderCounters decoderCounters) {
            Logger.logV(TAG, "AnalyticsListener.onAudioDisabled()");
        }

        @Override
        public void onAudioSessionIdChanged(EventTime eventTime, int audioSessionId) {
            mAudioSessionID = audioSessionId;
        }

        @Override
        public void onAudioDecoderInitialized(EventTime eventTime, String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            Logger.logV(TAG, "AnalyticsListener.onAudioDecoderInitialized(): name = " + decoderName + ", init timestamp = " + initializedTimestampMs + " ms, duration = " + initializationDurationMs + " ms");
        }

        @Override
        public void onAudioInputFormatChanged(EventTime eventTime, Format format, @Nullable DecoderReuseEvaluation decoderReuseEvaluation) {
            Logger.logI(TAG, "AnalyticsListener.onAudioInputFormatChanged(): format = " + format);
        }

        @Override
        public void onAudioUnderrun(EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            Logger.logW(TAG, "AnalyticsListener.onAudioUnderrun(): bufferSize = " + bufferSize + ", bufferSizeMs = " + bufferSizeMs + ", elapsedSinceLastFeedMs = " + elapsedSinceLastFeedMs);
        }
    };

    private final MediaSourceEventListener mAdaptiveMediaSourceEventListener = new MediaSourceEventListener () {
        @Override
        public void onLoadStarted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
            Logger.logV(TAG, "MediaSourceEventListener.onLoadStarted()");
        }

        @Override
        public void onLoadCompleted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
            Logger.logV(TAG, "MediaSourceEventListener.onLoadCompleted()");
        }

        @Override
        public void onLoadCanceled(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
            Logger.logV(TAG, "MediaSourceEventListener.onLoadCanceled()");
        }

        @Override
        public void onLoadError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
            Logger.logE(TAG, "MediaSourceEventListener.onLoadError()");
        }

        @Override
        public void onUpstreamDiscarded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
        }

        @Override
        public void onDownstreamFormatChanged(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
            Logger.logV(TAG, "MediaSourceEventListener.onDownstreamFormatChanged()");
        }
    };
}
