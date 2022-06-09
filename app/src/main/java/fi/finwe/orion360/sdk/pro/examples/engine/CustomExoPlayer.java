/*
 * Copyright (c) 2017, Finwe Ltd. All rights reserved.
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.Objects;

import fi.finwe.log.Logger;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.TouchControllerWidget;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.source.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.source.VideoPlayerWrapper;
import fi.finwe.orion360.sdk.pro.view.OrionView;

/**
 * An example of using a custom configured ExoPlayer as an audio/video engine.
 * <p/>
 * Features:
 * <ul>
 * <li>Plays one hard-coded full spherical (360x180) equirectangular video
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Auto-starts playback on load and stops when playback is completed
 * <li>Renders the video using standard rectilinear projection
 * <li>Allows navigation with touch & movement sensors (if supported by HW) as follows:
 * <ul>
 * <li>Panning (gyro or swipe)
 * <li>Zooming (pinch)
 * <li>Tilting (pinch rotate)
 * </ul>
 * <li>Auto Horizon Aligner (AHL) feature straightens the horizon</li>
 * </ul>
 */
public class CustomExoPlayer extends OrionActivity {

    /** The Android view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our panorama sphere will be added to. */
    protected OrionScene mScene;

    /** The panorama sphere where our video texture will be mapped to. */
    protected OrionPanorama mPanorama;

    /** The video player. */
    protected VideoPlayerWrapper mVideoPlayer;

    /** The video texture where our decoded video frames will be updated to. */
    protected OrionTexture mPanoramaTexture;

    /** The camera which will project our 3D scene to a 2D (view) surface. */
    protected OrionCamera mCamera;

    /** The widget that will handle our touch gestures. */
    protected TouchControllerWidget mTouchController;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene();

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindController(mOrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanorama = new OrionPanorama();

        // Create a new video player that uses our ExoPlayer wrapper as an audio/video engine.
        mVideoPlayer = new CustomExoPlayerWrapper(this);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = new OrionVideoTexture(mVideoPlayer,
                MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mPanorama.bindTextureFull(0, mPanoramaTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanorama);

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera();

        // Reset view to the 'front' direction (horizontal center of the panorama).
        mCamera.setDefaultRotationYaw(0);

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        mOrionContext.getSensorFusion().bindControllable(mCamera);

        // Create a new touch controller widget (convenience class), and let it control our camera.
        mTouchController = new TouchControllerWidget(mOrionContext, mCamera);

        // Bind the touch controller widget to the scene. This will make it functional in the scene.
        mScene.bindWidget(mTouchController);

        // Find Orion360 view from the XML layout. This is an Android view where we render content.
        mView = (OrionView)findViewById(R.id.orion_view);

        // Bind the scene to the view. This is the 3D world that we will be rendering to this view.
        mView.bindDefaultScene(mScene);

        // Bind the camera to the view. We will look into the 3D world through this camera.
        mView.bindDefaultCamera(mCamera);

        // The view can be divided into one or more viewports. For example, in VR mode we have one
        // viewport per eye. Here we fill the complete view with one (landscape) viewport.
        mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL,
                OrionViewport.CoordinateType.FIXED_LANDSCAPE);
	}

    /**
     * A wrapper for a custom video player engine.
     *
     * Sometimes there is a need to use a custom video player engine, for example to add support
     * for a video format that the built-in video engine does not support, to replace the video
     * engine with a 3rd party or the developer's own implementation, to configure ExoPlayer
     * engine for a particular use case, or to simply use a different version of ExoPlayer.
     * All of this can be achieved by implementing Orion360 SDK's VideoPlayer interface.
     *
     * However, video player engines usually have an internal state that will be accessed and
     * modified very carefully and in an asynchronous manner. The VideoPlayerWrapper class hides
     * this complexity and thus the recommended way of using own video player engine is to
     * inherit the VideoPlayerWrapper class, override all of its abstract methods, and also
     * implemented methods when necessary.
     *
     * The VideoPlayerWrapper uses the concept of a control status for describing the video player
     * engine's status, and holds two instances of related ControlStatus class: one represents the
     * current status of the engine and the other the target status that has been requested via
     * various API calls.
     *
     * The adaptation of a video player engine to be used with Orion360 involves the following:
     * - Creating a wrapper class (such as this class) and providing it to OrionVideoTexture
     * - Implementing the process- methods that command the player engine based on control status
     * - Implementing the is- and get- methods that allow querying the player status from outside
     * - Implementing a listener for the player engine events, and responding accordingly
     *
     * Notice that the decoded video frames are expected to be provided to Orion360 texture
     * via Android's Surface class. See processSetSurface() and processResetSurface(), as well as
     * the Surface member in the ControlStatus class.
     *
     * Depending on video player engine's complexity, implementing a production quality adaptation
     * can be a fairly laborious task. This example simply provides an idea of the process.
     * Examining Orion360's AndroidMediaPlayerWrapper and ExoPlayerWrapper is very informative.
     *
     * Notice the boolean flag that will be returned from all process- methods. Returning 'true'
     * means that the task is completed, while returning 'false' means that the execution
     * will continue with another callback. This allows performing operations in multiple steps
     * and in correct order based on the current control status.
     */
    private static class CustomExoPlayerWrapper extends VideoPlayerWrapper
            implements ExoPlayer.EventListener {

        /** User agent string. */
        String USER_AGENT = "Orion360";

        /** Context. */
        Context mContext;

        /** Here the ExoPlayer will be used as a custom video player engine. */
        SimpleExoPlayer mExoPlayer;

        /** ExoPlayer abstracts different video formats as media sources. */
        MediaSource mMediaSource;

        /** ExoPlayer callbacks to the UI thread are made using a handler. */
        Handler mMainHandler;


        /**
         * Constructor.
         *
         * @param context the context.
         */
        CustomExoPlayerWrapper(Context context) {
            mContext = context;
        }

        @Override
        protected boolean processCreatePlayer() {
            Log.d(TAG, "processCreatePlayer");

            // Here we should instantiate the video player engine.
            // At this moment the player should be in the END state.
            synchronized (mCurrentStatus) {
                if (mCurrentStatus.playerState == PlayerState.END) {

                    // Create a handler for callbacks via UI thread.
                    mMainHandler = new Handler(Looper.getMainLooper());

                    // Create a default RenderersFactory.
                    RenderersFactory renderersFactory =
                            new DefaultRenderersFactory(mContext);

                    // Create a default TrackSelector.
                    BandwidthMeter bandwidthMeter =
                            new DefaultBandwidthMeter();
                    TrackSelection.Factory trackSelectionFactory =
                            new AdaptiveTrackSelection.Factory(bandwidthMeter);
                    TrackSelector trackSelector =
                            new DefaultTrackSelector(trackSelectionFactory);

                    // Create a default LoadControl.
                    LoadControl loadControl =
                            new DefaultLoadControl();

                    // Create the player instance.
                    mExoPlayer = ExoPlayerFactory.newSimpleInstance(
                            renderersFactory,
                            trackSelector,
                            loadControl);

                    // Begin listening to player events.
                    mExoPlayer.addListener(this);

                    // Change current state from END to IDLE.
                    mCurrentStatus.playerState = PlayerState.IDLE;
                } else {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected boolean processSetDataSource() {
            Log.d(TAG, "processSetDataSource");

            // Here we should set the video URI to the player engine, and configure it accordingly.
            // At this moment the player should be in IDLE state.

            synchronized (mCurrentStatus) {

                // Handle special cases, such as NULL and setting same Uri again.
                synchronized (mTargetStatus) {
                    if (mTargetStatus.uri == null) {
                        mCurrentStatus.uri = null;
                        return false;
                    }
                    if (Objects.equals(mCurrentStatus.uri, mTargetStatus.uri)) {
                        return true;
                    }
                }

                // Clean up previous media source, if any.
                if (mMediaSource != null) {
                    mMediaSource.releaseSource();
                    mMediaSource = null;
                }

                // Create a new media source. Here we simply create the basic type, but
                // you could create a different source for HLS, DASH, SmoothStreaming etc.
                String userAgent = Util.getUserAgent(mContext, USER_AGENT);
                DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
                DefaultDataSourceFactory dataSourceFactory =
                        new DefaultDataSourceFactory(mContext, userAgent, bandwidthMeter);
                DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
                mMediaSource = new ExtractorMediaSource(
                        mTargetStatus.uri, // Here we set the video URI that will be played!
                        dataSourceFactory,
                        extractorsFactory,
                        mMainHandler,
                        e -> Log.e(TAG, "onLoadError(): " + e.toString()));

                // Update video Uri and player state.
                mCurrentStatus.uri = mTargetStatus.uri;
                mCurrentStatus.playerState = PlayerState.INITIALIZED;
            }

            return false;
        }

        @Override
        protected boolean processSetSurface() {
            Log.d(TAG, "processSetSurface");

            // Here we should set the Android's Surface to the video player; this is how the
            // decoded video frames are provided to Orion360 for rendering them on screen.

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
            Log.d(TAG, "processPrepare");

            // Here we should prepare the video player engine for playback.

            synchronized (mCurrentStatus) {
                mCurrentStatus.playerState = PlayerState.PREPARING;
                if (mMediaSource == null) {
                    Log.w(TAG, "Cannot prepare ExoPlayer, MediaSource == null!");
                    return false;
                }

                // Update player state to PREPARING.
                mCurrentStatus.preparing = true;

                // Reset the position and the state.
                mExoPlayer.prepare(mMediaSource, true, true);
                return true;
            }
        }

        @Override
        protected boolean processStartPlayer() {
            Log.d(TAG, "processStartPlayer");

            // Here we should start the video playback.

            boolean notifyStarted = false;
            synchronized (mCurrentStatus) {
                switch (mCurrentStatus.playerState) {
                    case STARTED:
                        break;
                    case PLAYBACK_COMPLETED:
                        // Don't start again unless looping is set true or the player has been
                        // sought after playback completion.
                        if (!mCurrentStatus.looping && mCurrentStatus.completionSeekRequired
                                && !mCurrentStatus.completionSeekDone) {
                            break;
                        }
                        // Update player state to PREPARING.
                        mCurrentStatus.playerState = PlayerState.PREPARING;
                        mCurrentStatus.preparing = true;

                        // Reset the position but not the state (keep the same content).
                        mExoPlayer.prepare(mMediaSource, true, false);
                        return true;
                    case PREPARED:
                    case PAUSED:
                        // Update player state to STARTED.
                        mCurrentStatus.playerState = PlayerState.STARTED;

                        // Start the player outside the synchronization block. See below.
                        notifyStarted = true;
                        break;
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
                // This call may implicitly call onPlayerStateChanged, so we must do it
                // outside the synchronized block.
                mExoPlayer.setPlayWhenReady(true);
                mUpdateListener.onVideoStarted(null);
            }
            return true;
        }

        @Override
        protected boolean processPausePlayer() {
            Log.d(TAG, "processPausePlayer");

            // Here we should pause the video playback.

            boolean notifyPaused = false;
            synchronized (mCurrentStatus) {
                switch (mCurrentStatus.playerState) {
                    case PAUSED:
                        break;
                    case STARTED:
                    case PLAYBACK_COMPLETED:

                        // Update player state to PAUSED.
                        mCurrentStatus.playerState = PlayerState.PAUSED;

                        // Pause the player outside the synchronization block. See below.
                        notifyPaused = true;
                        break;
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
                // This call may implicitly call onPlayerStateChanged, so we must do it
                // outside the synchronized block.
                mExoPlayer.setPlayWhenReady(false);
                mUpdateListener.onVideoPaused(null);
            }
            return true;
        }

        @Override
        protected boolean processStopPlayer() {
            Log.d(TAG, "processStopPlayer");

            // Here we should stop the video playback.

            boolean stop = false;
            synchronized (mCurrentStatus) {
                switch (mCurrentStatus.playerState) {
                    case PREPARED:
                    case STARTED:
                    case PAUSED:
                    case PLAYBACK_COMPLETED:
                        stop = true;
                        break;
                    case STOPPED:
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
        protected boolean processSeek() {
            Log.d(TAG, "processSeek");

            // Here we should seek within the current video.

            synchronized (mCurrentStatus) {
                switch (mCurrentStatus.playerState) {
                    case STARTED:
                    case PREPARED:
                    case PAUSED:
                    case PLAYBACK_COMPLETED:
                        synchronized (mTargetStatus) {
                            if (mExoPlayer.getDuration() == C.TIME_UNSET) {
                                mCurrentStatus.seekPosition = mTargetStatus.seekPosition = -1;
                                return false;
                            }
                            mCurrentStatus.seekPosition = mTargetStatus.seekPosition =
                                    Math.max(0,Math.min(mTargetStatus.seekPosition, getDuration()));
                        }
                        mCurrentStatus.seekActive = true;
                        mExoPlayer.seekTo(mCurrentStatus.seekPosition);
                        break;
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
            Log.d(TAG, "processSetVolume");

            // Here we should adjust the player's audio volume.

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
                    case STOPPED:
                    case INITIALIZED:
                    case PREPARING:
                    case ERROR:
                    case IDLE:
                    case END:
                        return true;
                }
            }
            return false;
        }

        @Override
        protected boolean processResetPlayer() {
            Log.d(TAG, "processResetPlayer");

            // Here we should reset the video player engine.
            // At this moment the player could be in any state, so we need to act based on state.

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
        protected boolean processResetSurface() {
            Log.d(TAG, "processResetSurface");

            // Here we should reset the video player's surface.
            // At this moment the player could be in any other state than END.

            synchronized (mCurrentStatus) {
                if (mCurrentStatus.playerState != PlayerState.END) {
                    if (mCurrentStatus.surface != null) {
                        mExoPlayer.setVideoSurface(null);
                        synchronized (mTargetStatus) {
                            if (mTargetStatus.surface != mCurrentStatus.surface) {
                                mCurrentStatus.surface.release();
                            }
                        }
                        mCurrentStatus.surface = null;
                    }
                }
            }
            return false;
        }

        @Override
        protected boolean processRelease() {
            Log.d(TAG, "processRelease");

            // Here we should release the video player's resources.
            // At this moment we can be in any state.

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

        private void doStop() {
            synchronized (mCurrentStatus) {
                mCurrentStatus.playerState = PlayerState.STOPPED;
                if (mExoPlayer != null) {
                    mExoPlayer.stop();
                } else {
                    Logger.logW(TAG, "Could not stop ExoPlayer in state "
                            + mCurrentStatus.playerState.name()
                            + ": ExoPlayer object was null.");
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
                            + ": ExoPlayer object was null.");
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
                            + ": ExoPlayer object was null.");
                }

                if (mMediaSource != null) {
                    mMediaSource.releaseSource();
                    mMediaSource = null;
                }

                if (mMainHandler != null) {
                    mMainHandler = null;
                }
            }
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
            return 0;
        }

        @Override
        public void setPositionUpdateTimeout(int i) {

        }

        // -- ExoPlayer event listener callbacks --

        @Override
        public void onTimelineChanged(Timeline timeline, Object o) {
            Log.d(TAG, "ExoPlayer: onTimelineChanged()");
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {
            Log.d(TAG, "ExoPlayer: onTracksChanged()");
        }

        @Override
        public void onLoadingChanged(boolean b) {
            Log.d(TAG, "ExoPlayer: onLoadingChanged()");
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.d(TAG, "ExoPlayer: onPlayerStateChanged()");

            switch (playbackState) {
                case ExoPlayer.STATE_BUFFERING:
                    // Prepared but not able to immediately play from the current position.
                    synchronized (mCurrentStatus) {
                        mCurrentStatus.buffering = true;
                    }
                    mUpdateListener.onVideoBufferingStart(null);
                    break;
                case ExoPlayer.STATE_ENDED:
                    // The player has finished playing the media.
                    synchronized (mCurrentStatus) {
                        mCurrentStatus.playerState = PlayerState.PLAYBACK_COMPLETED;
                        if (!mCurrentStatus.looping) {
                            // The player was not set to loop upon completion, seek needed.
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
                            mUpdateListener.onVideoSeekCompleted(null,
                                    mExoPlayer.getCurrentPosition());
                            postUpdateState();
                        }
                    }
                    break;
                default:
                    Logger.logE(TAG, "Unhandled ExoPlayer state change:" + playbackState);
                    break;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            Log.d(TAG, "ExoPlayer: onPlayerError()");

            synchronized (mCurrentStatus) {
                mCurrentStatus.playerState = PlayerState.ERROR;
                synchronized (mTargetStatus) {
                    mTargetStatus.playerState = PlayerState.END;
                }
            }
            mUpdateListener.onVideoError(null, 0, 0);
            postUpdateState();
        }

        @Override
        public void onPositionDiscontinuity() {
            Log.d(TAG, "ExoPlayer: onPositionDiscontinuity()");
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            Log.d(TAG, "ExoPlayer: onPlaybackParametersChanged()");
        }
    }
}
