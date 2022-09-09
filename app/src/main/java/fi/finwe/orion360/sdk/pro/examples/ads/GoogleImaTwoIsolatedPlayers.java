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
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.texture.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;

/**
 * An example of using Google ExoPlayer and Google IMA SDK with Orion360 (two isolated players).
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
public class GoogleImaTwoIsolatedPlayers extends OrionActivity implements AdEvent.AdEventListener {

    /** Tag for logging. */
    public static final String TAG = GoogleImaTwoIsolatedPlayers.class.getSimpleName();

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

        // In this example, we use two isolated ExoPlayer instances for ads and media content.
        // One approach is to first initialize an ad player and once an ad has been shown, tear
        // down the ad player and initialize Orion360 normally for media content playback.
        // However, here we initialize both without waiting for the ad to complete. This way
        // Orion360's ExoPlayer can pre-buffer media content while the ad is being played.
        initializeAdPlayer();
        initializeOrion();
    }

    /**
     * Initialize the player for ads.
     */
    private void initializeAdPlayer() {
        Logger.logF();

        // Create an ExoPlayer instance and play ads with it. Parts related to ads
        // are marked with IMPORTANT comment.
        try {
            DefaultRenderersFactory defaultRenderersFactory =
                    new DefaultRenderersFactory(this);

            // Set up the factory for media sources, passing the ads loader and ad view providers.
            String userAgent = Util.getUserAgent(this, getString(R.string.app_name));
            DefaultHttpDataSource.Factory defaultHttpDataSourceFactory =
                    new DefaultHttpDataSource.Factory().setUserAgent(userAgent);
            DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
                    this, defaultHttpDataSourceFactory);
            DefaultMediaSourceFactory defaultMediaSourceFactory =
                    new DefaultMediaSourceFactory(dataSourceFactory)
                            .setLocalAdInsertionComponents(         // IMPORTANT
                                    unusedAdTagUri -> mImaAdsLoader,
                                    mStyledPlayerView);

            AdaptiveTrackSelection.Factory videoTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory();
            TrackSelector trackSelector = new DefaultTrackSelector(this,
                    videoTrackSelectionFactory);

            DefaultLoadControl defaultLoadControl = new DefaultLoadControl();

            BandwidthMeter bandwidthMeter =
                    new DefaultBandwidthMeter.Builder(this).build();

            DefaultAnalyticsCollector defaultAnalyticsCollector =
                    new DefaultAnalyticsCollector(Clock.DEFAULT);

            // Create an ExoPlayer and set it as the player for content and ads.
            ExoPlayer.Builder exoplayerBuilder = new ExoPlayer.Builder(this,
                    defaultRenderersFactory, defaultMediaSourceFactory, trackSelector,
                    defaultLoadControl, bandwidthMeter, defaultAnalyticsCollector);
            mExoPlayer = exoplayerBuilder.build();
            mStyledPlayerView.setPlayer(mExoPlayer);
            mImaAdsLoader.setPlayer(mExoPlayer);                    // IMPORTANT

            // Create the MediaItem to play, specifying the content URI and ad tag URI.
            Uri contentUri = Uri.parse(MainMenu.TEST_VIDEO_URI_HLS);
            Uri adTagUri = Uri.parse(getString(R.string.ad_tag_url));
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(contentUri)
                    .setAdsConfiguration(                           // IMPORTANT
                            new MediaItem.AdsConfiguration.Builder(adTagUri).build())
                    .build();

            // Prepare the content (and ad) to be played with the ExoPlayer instance.
            mExoPlayer.setMediaItem(mediaItem);
            mExoPlayer.prepare();

            // Set PlayWhenReady. If true, content and ads will autoplay.
            mExoPlayer.setPlayWhenReady(false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize the player for media content (Orion360).
     */
    private void initializeOrion() {
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

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = new OrionVideoTexture(mOrionContext,
                mVideoPlayer, MainMenu.TEST_VIDEO_URI_HLS);

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

        if (Util.SDK_INT <= 23 || mStyledPlayerView == null) {
            initializePlayer();

            if (mStyledPlayerView != null) {
                mStyledPlayerView.onResume();
            }
        }

        // If we resume the app and ad player is visible, we must not let
        // Orion360 play the media content in the background -> pause it.
        if (null != mStyledPlayerView && mStyledPlayerView.getVisibility() == View.VISIBLE) {
            mPanoramaTexture.pause();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

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

        mImaAdsLoader.release();
    }

    @Override
    public void onAdEvent(AdEvent adEvent) {
        //Logger.logF();
        Logger.logD(TAG, "onAdEvent(): " + adEvent);

        // ImaAdsLoader provides a number of events that we can listen to. Here we use
        // only two of them, which are called when the media content should play/pause.
        // We will use the events for controlling the ad player's visibility and both
        // players' play/pause states. This is very simplified, but shows that using
        // isolated ad and media content players is possible, if needed.

        switch (adEvent.getType()) {
            case CONTENT_PAUSE_REQUESTED:

                // Pause media content.
                mPanoramaTexture.pause();

                // Make Orion invisible.
                mViewContainer.setVisibility(View.INVISIBLE);

                // Resume ad.
                mExoPlayer.play();

                break;
            case STARTED:
                // Make the ad player visible.
                mStyledPlayerView.setVisibility(View.VISIBLE);
                break;
            case CONTENT_RESUME_REQUESTED:

                // Resume media content.
                mPanoramaTexture.play();

                // Pause ad.
                mExoPlayer.pause();

                // Make the ad player invisible.
                mStyledPlayerView.setVisibility(View.INVISIBLE);

                // Make Orion visible.
                mViewContainer.setVisibility(View.VISIBLE);

                break;
        }

        // Notice that we have given the same media content URL for Orion360 and the ad player.
        // Hence, both players will pre-buffer the same URL, i.e. at least part of the video
        // content will be loaded twice... Since we use the ad player only for playing the
        // pre-roll ad, it doesn't really matter what content URL we give to it. We could,
        // for example, use a short dummy video stream or even a tiny clip bundled with the app,
        // and thus minimize network load.

        // However, if you plan to play mid-roll or post-roll ads and let ImaAdsLoader control
        // this, everything becomes more complex especially if the ad player is paused and/or
        // uses different length content: ImaAdsLoader/ad playback will not stay in sync with
        // media content playback. This is an obvious drawback of using two completely isolated
        // players for ads and media content, and it is laborious to solve it reliably.

        // Therefore, while the approach shown in this example works OK for pre-roll ads,
        // other examples that use *the same ExoPlayer instance* for ads and media content are
        // probably more suitable for mid-roll and post-roll ads, and other more complex cases.

        // Moreover, some old devices cannot decode and play two videos simultaneously.
        // This approach may not work on these devices at all. For example, EGL_BAD_DISPLAY
        // error might appear in LogCat.
    }
}
