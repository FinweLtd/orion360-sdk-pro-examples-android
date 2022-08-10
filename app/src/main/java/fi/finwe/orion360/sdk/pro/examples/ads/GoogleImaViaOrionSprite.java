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

import android.os.Bundle;

import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader;
import com.google.android.exoplayer2.util.Util;

import fi.finwe.log.Logger;
import fi.finwe.math.Vec3f;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.TouchControllerWidget;
import fi.finwe.orion360.sdk.pro.examples.engine.ExoPlayerWrapper;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.item.sprite.OrionSprite;
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.texture.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;

/**
 * An example of using Google ExoPlayer and Google IMA SDK with Orion360 (ad placed in Orion world).
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
public class GoogleImaViaOrionSprite extends OrionActivity implements AdEvent.AdEventListener {

    /** Tag for logging. */
    public static final String TAG = GoogleImaSharedPlayerSeparateViews.class.getSimpleName();

    /** Google ExoPlayer. */
    protected ExoPlayer mExoPlayer;

    /** Google IMA ad loader. */
    protected ImaAdsLoader mImaAdsLoader;

    /** The view group where Orion and ad related views will be added to as layers. */
    protected OrionViewContainerIma mViewContainerIma;

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

    /** The panorama sphere where our ad background texture will be mapped to. */
    protected OrionPanorama mPanoramaAdBackground;

    /** The image texture for our ad background. */
    protected OrionTexture mPanoramaAdBackgroundTexture;

    /** The sprite where our video texture for the ads will be mapped to. */
    protected OrionSprite mSprite;

    /** The camera which will project our 3D scene to a 2D (view) surface. */
    protected OrionCamera mCamera;

    /** The widget that will handle our touch gestures. */
    protected TouchControllerWidget mTouchController;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Use special layout that supports ad overlay.
        setContentView(R.layout.activity_ad_via_orion);

        // Find view group for Orion and ad player views.
        mViewContainerIma = findViewById(R.id.orion_view_container_ima);

        // Get Orion360 view container from the inflated XML layout.
        // This is where both ads and media content will appear.
        mViewContainer = mViewContainerIma.getOrionViewContainer();

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
        mVideoPlayer = new ExoPlayerWrapper(this);

        // Configurations to enable playing ads.
        mVideoPlayer.setAdTag(getString(R.string.ad_tag_url));      // IMPORTANT
        mVideoPlayer.setAdsLoader(mImaAdsLoader);                   // IMPORTANT
        mVideoPlayer.setAdViewProvider(mViewContainerIma);          // IMPORTANT

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

                mExoPlayer = mVideoPlayer.getExoPlayer();
            }

        });

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mPanorama.bindTextureFull(0, mPanoramaTexture);

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanoramaAdBackground = new OrionPanorama(mOrionContext);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaAdBackgroundTexture = OrionTexture.createTextureFromURI(mOrionContext,
                this,MainMenu.PRIVATE_ASSET_FILES_PATH
                        + MainMenu.TEST_IMAGE_FILE_LIVINGROOM_HQ);

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mPanoramaAdBackground.bindTextureFull(0, mPanoramaAdBackgroundTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanoramaAdBackground);

        // Create a new sprite. This is a 2D plane in the 3D world for our planar video.
        mSprite = new OrionSprite(mOrionContext);

        // Set sprite location in the 3D world. Here we place the video on the white screen.
        mSprite.setWorldTranslation(new Vec3f(0.03f, 0.19f, -0.77f));

        // Set sprite size in the 3D world. Here we make it fit on the white screen.
        mSprite.setScale(0.42f);

        // Bind the video texture to the sprite object. Here we use the same texture
        // where both ad and media content will be decoded to from ExoPlayer.
        mSprite.bindTexture(mPanoramaTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanorama);

        // Bind the sprite to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mSprite);

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

        if (null != mPanoramaAdBackgroundTexture) {
            mPanoramaAdBackgroundTexture.release();
            mPanoramaAdBackgroundTexture.destroy();
            mPanoramaAdBackground = null;
        }

        if (null != mPanoramaAdBackground) {
            mPanoramaAdBackground.releaseTextures();
            mPanoramaAdBackground.destroy();
            mPanoramaAdBackground = null;
        }

        if (null != mSprite) {
            mSprite.releaseTexture();
            mSprite.destroy();
            mSprite = null;
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
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Util.SDK_INT <= 23) {
            initializePlayer();
        }

        // Ensure add sprite is visible when app is resumed while playing an ad.
        if (null != mExoPlayer && mExoPlayer.isPlayingAd()) {
            mSprite.setVisible(true);
            mPanoramaAdBackground.setVisible(true);
            mPanorama.setVisible(false);
        } else {
            mSprite.setVisible(false);
            mPanoramaAdBackground.setVisible(false);
            mPanorama.setVisible(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (Util.SDK_INT > 23) {
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
        // We will use the events for controlling component visibilities.

        switch (adEvent.getType()) {
            case CONTENT_PAUSE_REQUESTED:
                mSprite.setVisible(true);
                mPanoramaAdBackground.setVisible(true);
                mPanorama.setVisible(false);
                break;
            case CONTENT_RESUME_REQUESTED:
                mSprite.setVisible(false);
                mPanoramaAdBackground.setVisible(false);
                mPanorama.setVisible(true);
                break;
        }
    }
}
