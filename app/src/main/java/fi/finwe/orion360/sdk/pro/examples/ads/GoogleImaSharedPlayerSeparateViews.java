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

package fi.finwe.orion360.sdk.pro.examples.ads;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.analytics.DefaultAnalyticsCollector;
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.Util;

import fi.finwe.log.Logger;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.TouchControllerWidget;
import fi.finwe.orion360.sdk.pro.examples.engine.ExoPlayerWrapper;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.item.OrionSceneItem;
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.texture.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;

/**
 * An example of using Google ExoPlayer and Google IMA SDK with Orion360 (shared player, two views).
 * <p/>
 * Read SERVING_ADS.md for more information and detailed explanation.
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
public class GoogleImaSharedPlayerSeparateViews extends OrionActivity
        implements AdEvent.AdEventListener {

    /** Tag for logging. */
    public static final String TAG = GoogleImaSharedPlayerSeparateViews.class.getSimpleName();

    /** Google ExoPlayer. */
    protected ExoPlayer mExoPlayer;

    /** ExoPlayer's own styled player view. */
    protected StyledPlayerView mStyledPlayerView;

    /** Google IMA ad loader. */
    protected ImaAdsLoader mImaAdsLoader;

    /** The Android view where our 3D scene (OrionView) will be added to. */
    protected OrionViewContainer mViewContainer;

    /** The Orion360 SDK view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our panorama sphere will be added to. */
    protected OrionScene mScene;

    /** The panorama sphere where our video texture will be mapped to. */
    protected OrionPanorama mPanorama;

    /** The video player. */
    protected ExoPlayerWrapper mVideoPlayer;

    /** The video texture where our decoded video frames will be updated to. */
    protected OrionTexture mPanoramaTexture;

    /** The camera which will project our 3D scene to a 2D (view) surface. */
    protected OrionCamera mCamera;

    /** The widget that will handle our touch gestures. */
    protected TouchControllerWidget mTouchController;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Logger.logF();

        // Layout for IMA example.
        setContentView(R.layout.activity_ad_on_top);

        // Find Orion view container view. This is where media content will appear.
        mViewContainer = findViewById(R.id.orion_view_container);

        // Find styled player view. This is where ads will appear.
        mStyledPlayerView = findViewById(R.id.exoplayer_styled_player_view);

        // Create an AdsLoader and set us as a listener to its events.
        mImaAdsLoader = new ImaAdsLoader.Builder(this)
                .setAdEventListener(this)
                .build();
	}

    /**
     * Initialize player.
     */
    private void initializePlayer() {
        Logger.logF();

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene(mOrionContext);

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindRoutine(mOrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanorama = new OrionPanorama(mOrionContext);

        // Create a new video player that uses Google ExoPlayer as an audio/video engine.
        // Notice that in this example, we could use also Android MediaPlayer as video backend
        // for Orion360, since content player is separate from the ad player.
        mVideoPlayer = new ExoPlayerWrapper(this);

        // Configurations to enable playing ads.
        mVideoPlayer.setAdTag(getString(R.string.ad_tag_url));      // IMPORTANT
        mVideoPlayer.setAdsLoader(mImaAdsLoader);                   // IMPORTANT
        mVideoPlayer.setAdViewProvider(mStyledPlayerView);          // IMPORTANT

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = new OrionVideoTexture(mOrionContext,
                mVideoPlayer, MainMenu.TEST_VIDEO_URI_HLS);

        // Get handle to Orion's ExoPlayer as soon as it has been created.
        // We will use this same ExoPlayer instance for playing ads.
        ((OrionVideoTexture)mPanoramaTexture).addTextureListener(
                new OrionVideoTexture.ListenerBase() {

            @Override
            public void onVideoPlayerCreated(OrionVideoTexture texture) {
                Logger.logF();

                if (null != mVideoPlayer) {
                    mExoPlayer = mVideoPlayer.getExoPlayer();
                }
            }

            @Override
            public void onException(OrionTexture orionTexture, Exception e) {
                Logger.logF();
            }

            @Override
            public void onVideoSourceURISet(OrionVideoTexture orionVideoTexture) {
                Logger.logF();
            }

            @Override
            public void onVideoBufferingStart(OrionVideoTexture orionVideoTexture) {
                Logger.logF();

            }

            @Override
            public void onVideoBufferingEnd(OrionVideoTexture orionVideoTexture) {
                Logger.logF();
            }

            @Override
            public void onVideoBufferingUpdate(OrionVideoTexture orionVideoTexture,
                                               int fromPercent, int toPercent) {
                Logger.logF();
            }

            @Override
            public void onVideoPrepared(OrionVideoTexture orionVideoTexture) {
                Logger.logF();
            }

            @Override
            public void onVideoRenderingStart(OrionVideoTexture orionVideoTexture) {
                Logger.logF();
            }

            @Override
            public void onVideoStarted(OrionVideoTexture orionVideoTexture) {
                Logger.logF();
            }

            @Override
            public void onVideoPaused(OrionVideoTexture orionVideoTexture) {
                Logger.logF();
            }

            @Override
            public void onVideoStopped(OrionVideoTexture orionVideoTexture) {
                Logger.logF();
            }

            @Override
            public void onVideoCompleted(OrionVideoTexture orionVideoTexture) {
                Logger.logF();
            }

            @Override
            public void onVideoPlayerDestroyed(OrionVideoTexture texture) {
                Logger.logF();
            }

            @Override
            public void onVideoSeekStarted(OrionVideoTexture orionVideoTexture, long positionMs) {
                Logger.logF();
            }

            @Override
            public void onVideoSeekCompleted(OrionVideoTexture orionVideoTexture, long positionMs) {
                Logger.logF();
            }

            @Override
            public void onVideoPositionChanged(OrionVideoTexture orionVideoTexture, long positionMs) {
                Logger.logF();
            }

            @Override
            public void onVideoDurationUpdate(OrionVideoTexture orionVideoTexture, long durationMs) {
                Logger.logF();
            }

            @Override
            public void onVideoSizeChanged(OrionVideoTexture orionVideoTexture, int width, int height) {
                Logger.logF();
            }

            @Override
            public void onVideoError(OrionVideoTexture orionVideoTexture, int what, int extra) {
                Logger.logF();
            }

            @Override
            public void onVideoInfo(OrionVideoTexture orionVideoTexture, int what, String message) {
                Logger.logF();
            }

        });

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mPanorama.bindTextureFull(0, mPanoramaTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanorama);

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera(mOrionContext);

        // Reset view to the 'front' direction (horizontal center of the panorama).
        mCamera.setDefaultRotationYaw(0);

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        mOrionContext.getSensorFusion().bindControllable(mCamera);

        // Create a new touch controller widget (convenience class), and let it control our camera.
        mTouchController = new TouchControllerWidget(mOrionContext, mCamera);

        // Bind the touch controller widget to the scene. This will make it functional in the scene.
        mScene.bindWidget(mTouchController);

        // Create a new OrionView and bind it into the container.
        mView = new OrionView(mOrionContext);
        mViewContainer.bindView(mView);

        // Bind the scene to the view. This is the 3D world that we will be rendering to this view.
        mView.bindDefaultScene(mScene);

        // Bind the camera to the view. We will look into the 3D world through this camera.
        mView.bindDefaultCamera(mCamera);

        // The view can be divided into one or more viewports. For example, in VR mode we have one
        // viewport per eye. Here we fill the complete view with one (landscape) viewport.
        mView.bindViewports(OrionDisplayViewport.VIEWPORT_CONFIG_FULL,
                OrionDisplayViewport.CoordinateType.FIXED_LANDSCAPE);
    }

    /**
     * Release player.
     */
    private void releasePlayer() {
        Logger.logF();

        if (null != mImaAdsLoader) {
            mImaAdsLoader.setPlayer(null);
        }

        // ExoPlayer:

        if (null != mStyledPlayerView) {
            mStyledPlayerView.setPlayer(null);
        }
        if (null != mExoPlayer) {
            mExoPlayer.release();
            mExoPlayer = null;
        }

        // Orion360:

        if (null != mPanoramaTexture) {
            mPanoramaTexture.release();
            mPanoramaTexture.destroy();
            mPanoramaTexture = null;
        }

        if (null != mVideoPlayer) {
            mVideoPlayer.release();
            mVideoPlayer = null;
        }

        if (null != mPanorama) {
            mPanorama.releaseTextures();
            mPanorama.destroy();
            mPanorama = null;
        }

        if (null != mCamera) {
            mCamera.destroy();
            mCamera = null;
        }

        if (null != mScene) {
            mScene.releaseWidget(mTouchController);
            mScene.releaseSceneItem(mPanorama);
            mScene.releaseRoutine(mOrionContext.getSensorFusion());
            mScene.destroy();
            mScene = null;
        }

        if (null != mTouchController) {
            mTouchController = null;
        }

        if (null != mViewContainer) {
            mViewContainer.releaseView();
        }

        if (null != mView) {
            mView.releaseDefaultCamera();
            mView.releaseDefaultScene();
            mView.destroy();
            mView = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Logger.logF();

        if (Util.SDK_INT > 23) {
            initializePlayer();

            if (mStyledPlayerView != null) {
                mStyledPlayerView.onResume();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.logF();

        if (Util.SDK_INT <= 23 || mStyledPlayerView == null) {
            initializePlayer();

            if (mStyledPlayerView != null) {
                mStyledPlayerView.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Logger.logF();

        if (Util.SDK_INT <= 23) {
            if (mStyledPlayerView != null) {
                mStyledPlayerView.onPause();
            }

            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Logger.logF();

        if (Util.SDK_INT > 23) {
            if (mStyledPlayerView != null) {
                mStyledPlayerView.onPause();
            }

            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.logF();

        mImaAdsLoader.release();
    }

    @Override
    public void onAdEvent(AdEvent adEvent) {
        //Logger.logF();
        //Logger.logD(TAG, "onAdEvent(): " + adEvent);
        if (adEvent.getType() != AdEvent.AdEventType.AD_PROGRESS) {
            Logger.logD(TAG, "onAdEvent(): " + adEvent);
        }

        // ImaAdsLoader provides a number of events that we can listen to. Here we use
        // two of them, which are called when the media content should play/pause.
        // We will use the events for controlling to which view the shared ExoPlayer's
        // video content will be directed to.

        // When the app is resumed after pause, Orion360 will become ExoPlayer's output
        // surface again. Here we use RESUMED event for swapping ExoPlayer's output
        // surface to the ad view again. RESUMED is called when ad playback is resumed,
        // but not when media content playback is resumed.

        switch (adEvent.getType()) {
            case CONTENT_PAUSE_REQUESTED:
                // Swap surface from Orion360 view to ad view.
                // StyledPlayerView will set its own surface to ExoPlayer when ExoPlayer
                // is given to it. Video content (ad) will now appear in StyledPlayerView.
                mStyledPlayerView.setPlayer(mExoPlayer);

                // Make Orion invisible. We don't want it to appear and pause ad playback
                // when it is ready and texture gets initialized and paused.
                mViewContainer.setVisibility(View.INVISIBLE);
                break;
            case STARTED:
                // Make the ad player visible now.
                mStyledPlayerView.setVisibility(View.VISIBLE);
                break;
            case CONTENT_RESUME_REQUESTED:
                // Swap surface from ad view to Orion360 view.
                // Request Orion360 to restore its surface to ExoPlayer via our video texture.
                // Video content (360 media) will now appear in Orion360 view.
                ((OrionVideoTexture)mPanoramaTexture).restoreSurface();

                // Make the ad player invisible.
                mStyledPlayerView.setVisibility(View.INVISIBLE);

                // Make Orion visible.
                mViewContainer.setVisibility(View.VISIBLE);
                break;
        }
     }
}
