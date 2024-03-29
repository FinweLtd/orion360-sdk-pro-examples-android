/*
 * Copyright (c) 2016, Finwe Ltd. All rights reserved.
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

package fi.finwe.orion360.sdk.pro.examples.animation;

import android.os.Bundle;
import android.util.Log;

import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.variable.BasicFunction;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.TouchControllerWidget;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.texture.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.variable.TimedFloat1ToFloat1Function;
import fi.finwe.orion360.sdk.pro.view.OrionView;

/**
 * An example of cross-fading between a panorama image and a panorama video.
 * <p/>
 * Features:
 * <ul>
 * <li>Plays one hard-coded full spherical (360x180) equirectangular video
 * <li>Cross-fades from image to video upon startup, and from video to image when completed
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Starts playback automatically and stops when playback is completed
 * <li>Renders the video and image using standard rectilinear projection
 * <li>Allows navigation with touch & movement sensors (if supported by HW) as follows:
 * <ul>
 * <li>Panning (gyro or swipe)
 * <li>Zooming (pinch)
 * <li>Tilting (pinch rotate)
 * </ul>
 * <li>Auto Horizon Aligner (AHL) feature straightens the horizon</li>
 * </ul>
 */
public class CrossFade extends OrionActivity {

    /** The Android view where our 3D scene (OrionView) will be added to. */
    protected OrionViewContainer mViewContainer;

    /** The Orion360 SDK view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our panorama spheres will be added to. */
    protected OrionScene mScene;

    /** The panorama sphere where our video texture will be mapped to. */
    protected OrionPanorama mPanoramaVideo;

    /** The video texture where our decoded video frames will be updated to. */
    protected OrionTexture mPanoramaVideoTexture;

    /** The panorama sphere where our image texture will be mapped to. */
    protected OrionPanorama mPanoramaImage;

    /** The image texture where our decoded image will be added to. */
    protected OrionTexture mPanoramaImageTexture;

    /** The animator that creates cross-fade FX by adjusting image panorama alpha value. */
    protected TimedFloat1ToFloat1Function mAlphaAnimator;

    /** The camera which will project our 3D scene to a 2D (view) surface. */
    protected OrionCamera mCamera;

    /** The widget that will handle our touch gestures. */
    protected TouchControllerWidget mTouchController;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene(mOrionContext);

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindRoutine(mOrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanoramaVideo = new OrionPanorama(mOrionContext);

        // Set video panorama size to the usual radius.
        mPanoramaVideo.setScale(1.0f);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaVideoTexture = OrionTexture.createTextureFromURI(mOrionContext, this,
                MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);

        // Listen for video texture events.
        ((OrionVideoTexture)mPanoramaVideoTexture).addTextureListener(
                new OrionVideoTexture.ListenerBase() {
                    @Override
                    public void onVideoStarted(OrionVideoTexture orionVideoTexture) {
                        // Animate image from opaque to transparent.
                        mAlphaAnimator.animateToInputValue(0.0f);
                    }
                    @Override
                    public void onVideoCompleted(OrionVideoTexture orionVideoTexture) {
                        // Animate image from transparent to opaque.
                        mAlphaAnimator.animateToInputValue(1.0f);
                    }
                });

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mPanoramaVideo.bindTextureFull(0, mPanoramaVideoTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanoramaVideo);

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanoramaImage = new OrionPanorama(mOrionContext);

        // Set image panorama size slightly smaller than video panorama, so that when
        // it becomes partially transparent the other panorama can be seen through it.
        mPanoramaImage.setScale(0.9f);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaImageTexture = OrionTexture.createTextureFromURI(mOrionContext, this,
                MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_PREVIEW_IMAGE_FILE_MQ);

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mPanoramaImage.bindTextureFull(0, mPanoramaImageTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanoramaImage);

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

        // Find Orion360 view container from the XML layout. This is an Android view for our content.
        mViewContainer = (OrionViewContainer)findViewById(R.id.orion_view_container);

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

        // Create a new timed variable for alpha animation. Here we want to adjust alpha
        // in the range [0.0-1.0] to cross-fade between two panoramas using linear interpolation.
        mAlphaAnimator = TimedFloat1ToFloat1Function.fromRange(mOrionContext,
                0.0f, 1.0f, BasicFunction.LINEAR);

        // We start from opaque (1.0).
        mAlphaAnimator.setInputValue(1.0f);

        // The duration for cross-fade, milliseconds.
        mAlphaAnimator.setDurationMs(3000);

        // Bind animator as a shared variable to the inner (image) panorama.
        mPanoramaImage.getColorFx().bindAmpAlpha(mAlphaAnimator);

        // Set up a listener to act when the animation is finished.
        mAlphaAnimator.setListener((phase, wrappedPhase, value) ->
                Log.v(TAG, "Cross-fade finished. Phase = " + phase + ", value = " + value));
	}
}
