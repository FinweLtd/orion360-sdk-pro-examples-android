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

package fi.finwe.orion360.sdk.pro.examples.fx;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import fi.finwe.device.DisplayUtil;
import fi.finwe.log.Logger;
import fi.finwe.math.Vec2f;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;
import fi.finwe.orion360.sdk.pro.viewport.OrionFrameBufferViewport;
import fi.finwe.orion360.sdk.pro.viewport.OrionViewport;
import fi.finwe.orion360.sdk.pro.viewport.fx.OrionBarrelDistortion;

/**
 * An example of applying lens barrel correction for a Google Cardboard style VR headset.
 * <p/>
 * See VR examples for a more complete implementation with barrel distortion compensation, etc.
 * <p/>
 * Features:
 * <ul>
 * <li>Plays one hard-coded full spherical (360x180) equirectangular video
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Auto-starts playback on load and stops when playback is completed
 * <li>Renders the video using standard rectilinear projection
 * <li>Allows navigation with movement sensors (if supported by HW) as follows:
 * <ul>
 * <li>Panning (gyro)
 * </ul>
 * </ul>
 */
public class BarrelCorrection extends OrionActivity {

    /** Undistorted VR mode viewport width for FullHD and below (in pixels). */
    protected static final int UNDISTORTED_VIEWPORT_WIDTH_FULL_HD_PX = 1024;

    /** Undistorted VR mode viewport height for FullHD and below (in pixels). */
    protected static final int UNDISTORTED_VIEWPORT_HEIGHT_FULL_HD_PX = 1024;

    /** Undistorted VR mode viewport width for WQHD (in pixels). */
    protected static final int UNDISTORTED_VIEWPORT_WIDTH_WQHD_PX = 1440;

    /** Undistorted VR mode viewport height for WQHD (in pixels). */
    protected static final int UNDISTORTED_VIEWPORT_HEIGHT_WQHD_PX = 1440;

    /** Undistorted VR mode viewport width for 4K (in pixels). */
    protected static final int UNDISTORTED_VIEWPORT_WIDTH_4K_PX = 2048;

    /** Undistorted VR mode viewport height for 4K (in pixels). */
    protected static final int UNDISTORTED_VIEWPORT_HEIGHT_4K_PX = 2048;

    /** VR mode IPD ie. inter-pupillary distance in mm. */
    protected static final float TARGET_VR_MODE_IPD_MM = 63.8f;

    /** VR mode lens correction distortion coefficients for Cardboard v2. */
    protected static final float[] DISTORTION_COEFFS_CB_V2 = { 1.0f, 0.20f, 0.10f, 0.15f };

    /** VR mode lens correction fill scale for Cardboard v2. */
    protected static final float DISTORTION_FILL_SCALE_CB_V2 = 1.1f;

    /** The Android view where our 3D scene (OrionView) will be added to. */
    protected OrionViewContainer mViewContainer;

    /** The Orion360 SDK view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our panorama sphere will be added to. */
    protected OrionScene mScene;

    /** The panorama sphere where our video texture will be mapped to. */
    protected OrionPanorama mPanorama;

    /** The video texture where our decoded video frames will be updated to. */
    protected OrionTexture mPanoramaTexture;

    /** The camera which will project our 3D scene to a 2D (view) surface. */
    protected OrionCamera mCamera;

    /** Barrel distortion configuration for the VR mode. */
    protected OrionBarrelDistortion mBarrelDistortion;

    /** Display viewport to be used in normal (non-VR) mode. */
    protected OrionDisplayViewport mNormalModeViewport;

    /** Framebuffer viewport to be used in VR mode for the left eye (undistorted). */
    protected OrionFrameBufferViewport mVrModeUndistortedViewportLeft;

    /** Display viewport to be used in VR mode for the left eye (distorted). */
    protected OrionDisplayViewport mVrModeDistortedViewportLeft;

    /** Framebuffer viewport to be used in VR mode for the right eye (undistorted). */
    protected OrionFrameBufferViewport mVrModeUndistortedViewportRight;

    /** Display viewport to be used in VR mode for the right eye (distorted). */
    protected OrionDisplayViewport mVrModeDistortedViewportRight;

    /** Gesture detector for tapping events (used for toggling between normal and VR modes). */
    private GestureDetector mGestureDetector;

    /** Flag for indicating if VR mode is currently enabled, or not. */
    private boolean mIsVRModeEnabled = true;


	@SuppressLint("ClickableViewAccessibility")
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene(mOrionContext);

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindRoutine(mOrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanorama = new OrionPanorama(mOrionContext);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = OrionTexture.createTextureFromURI(mOrionContext, this,
                MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        mPanorama.bindTextureFull(0, mPanoramaTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanorama);

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera(mOrionContext);

        // Reset view to the 'front' direction (horizontal center of the panorama).
        mCamera.setDefaultRotationYaw(0);

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        mOrionContext.getSensorFusion().bindControllable(mCamera);

        // Find Orion360 view container from the XML layout. This is an Android view for content.
        mViewContainer = (OrionViewContainer)findViewById(R.id.orion_view_container);

        // Create a new OrionView and bind it into the container.
        mView = new OrionView(mOrionContext);
        mViewContainer.bindView(mView);

        // Bind the scene to the view. This is the 3D world that we will be rendering to this view.
        mView.bindDefaultScene(mScene);

        // Bind the camera to the view. We will look into the 3D world through this camera.
        mView.bindDefaultCamera(mCamera);

        // In VR mode we have one viewport per eye, hence we use horizontal split viewport layout.
//        mView.bindViewports(OrionDisplayViewport.VIEWPORT_CONFIG_SPLIT_HORIZONTAL,
//                OrionDisplayViewport.CoordinateType.FIXED_LANDSCAPE);


        // On Android, VR mode is selectable. Here we create viewports for both normal and
        // VR mode. Initially, the mode is selected with a default value. The mode can be
        // later changed at any time, even by the user if the application allows that.

        // Viewport for normal mode (non-VR).
        mNormalModeViewport = new OrionDisplayViewport(mOrionContext,
                OrionDisplayViewport.VIEWPORT_CONFIG_FULL[0],
                OrionDisplayViewport.CoordinateType.UNFIXED_LANDSCAPE);

        // Determine eye viewport size. Depending on the device's display resolution, we will
        // select a suitable texture size for calculating high quality lens distortion correction.
        DisplayUtil util = new DisplayUtil(this);
        int displayHeight = util.getDisplayHeightInPixels();
        int viewportWidth, viewportHeight;
        Logger.logD(TAG, "Viewport: display height in pixels: " + displayHeight);
        if (displayHeight <= 1080) {

            // Display resolution (height) is 1080p (FullHD) or lower.
            viewportWidth = UNDISTORTED_VIEWPORT_WIDTH_FULL_HD_PX;
            viewportHeight = UNDISTORTED_VIEWPORT_HEIGHT_FULL_HD_PX;

        } else if (displayHeight < 1920) {

            // Display resolution (height) is 1440p (WQHD) or similar.
            viewportWidth = UNDISTORTED_VIEWPORT_WIDTH_WQHD_PX;
            viewportHeight = UNDISTORTED_VIEWPORT_HEIGHT_WQHD_PX;

        } else {

            // Display resolution (height) is 1920p/2160p (4k) or higher
            viewportWidth = UNDISTORTED_VIEWPORT_WIDTH_4K_PX;
            viewportHeight = UNDISTORTED_VIEWPORT_HEIGHT_4K_PX;

        }
        Logger.logD(TAG, "Viewport: size set to " + viewportWidth + "x" + viewportHeight);

        // Undistorted viewport for VR mode, left eye.
        mVrModeUndistortedViewportLeft = new OrionFrameBufferViewport(mOrionContext,
                viewportWidth, viewportHeight);
        mVrModeUndistortedViewportLeft.setVRMode(OrionViewport.VRMode.VR_LEFT);
        mVrModeUndistortedViewportLeft.setRenderingPriority(2.0f); // Higher than distorted vp!

        // Undistorted viewport for VR mode, right eye.
        mVrModeUndistortedViewportRight = new OrionFrameBufferViewport(mOrionContext,
                viewportWidth, viewportHeight);
        mVrModeUndistortedViewportRight.setVRMode(OrionViewport.VRMode.VR_RIGHT);
        mVrModeUndistortedViewportRight.setRenderingPriority(2.0f); // Higher than distorted vp!

        // Notice that VR mode has two framebuffer viewports for undistorted left and right eye
        // images. That is not enough, we need another two display viewports where lens
        // compensation FX is applied ie. distorted left and right eye images.

        // First determine inter-pupillary distance (IPD) to offset left and right eye images.

        Logger.logD(TAG, "Target IPD (mm): " + TARGET_VR_MODE_IPD_MM);
        float ipdOffset = util.getVrIpdOffset(TARGET_VR_MODE_IPD_MM);
        Logger.logD(TAG, "Using IPD offset: " + ipdOffset);

        // Only move view barrels closer to each other on large screens,
        // never farther apart on small screens.
        if (ipdOffset < 0) ipdOffset = 0.0f;

        // Create the distortion effect.
        mBarrelDistortion = new OrionBarrelDistortion(mOrionContext);
        mBarrelDistortion.setDistortionFillScale(DISTORTION_FILL_SCALE_CB_V2);
        mBarrelDistortion.setDistortionCenterOffset(new Vec2f(0,0f));
        mBarrelDistortion.setTranslation(new Vec2f(ipdOffset,0.01f));
        mBarrelDistortion.setDistortionCoeffs(DISTORTION_COEFFS_CB_V2);

        // Distorted viewport for VR mode, left eye.
        mVrModeDistortedViewportLeft = new OrionDisplayViewport(mOrionContext,
                OrionDisplayViewport.VIEWPORT_CONFIG_SPLIT_HORIZONTAL[0],
                OrionDisplayViewport.CoordinateType.UNFIXED_LANDSCAPE);
        mVrModeDistortedViewportLeft.setVRMode(OrionViewport.VRMode.VR_LEFT);
        mVrModeDistortedViewportLeft.bindFX(mBarrelDistortion);
        mVrModeDistortedViewportLeft.bindInputTexture(
                mVrModeUndistortedViewportLeft.getOutputTexture());

        // Distorted viewport for VR mode, right eye.
        mVrModeDistortedViewportRight = new OrionDisplayViewport(mOrionContext,
                OrionDisplayViewport.VIEWPORT_CONFIG_SPLIT_HORIZONTAL[1],
                OrionDisplayViewport.CoordinateType.UNFIXED_LANDSCAPE);
        mVrModeDistortedViewportRight.setVRMode(OrionViewport.VRMode.VR_RIGHT);
        mVrModeDistortedViewportRight.bindFX(mBarrelDistortion);
        mVrModeDistortedViewportRight.bindInputTexture(
                mVrModeUndistortedViewportRight.getOutputTexture());

        // Configure viewports according to initial config change object.
        configureViewports(mIsVRModeEnabled);

        // The user should always have an easy-to-find method for returning from VR mode to
        // normal mode. Here we use touch events, as it is natural to try tapping the screen
        // if you don't know what else to do. Start by propagating touch events from the
        // Orion360 view to a gesture detector.
        mViewContainer.setOnTouchListener((v, event) -> mGestureDetector.onTouchEvent(event));

        // Then, handle tap and long press events based on VR mode state. Typically you
        // want to associate long tap for entering/exiting VR mode and inform the user
        // that this hidden feature exists (at least when the user is stuck in VR mode).
        mGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {

                        // Notify user how to enter/exit VR mode with long press.
                        String message;
                        if (!mIsVRModeEnabled) {
                            message = getString(R.string.player_long_tap_hint_enter_vr_mode);
                        } else {
                            message = getString(R.string.player_long_tap_hint_exit_vr_mode);
                        }
                        Toast.makeText(BarrelCorrection.this, message,
                                Toast.LENGTH_SHORT).show();

                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {

                        // Enter or exit VR mode.
                        configureViewports(!mIsVRModeEnabled);

                    }

                });
    }

    private void configureViewports(boolean vrMode) {

        // Release all existing viewports from the Orion view.
        mView.clearViewports();

        // Bind new viewport(s) according to given configuration.
        if (vrMode) {
            mView.bindViewport(mVrModeUndistortedViewportLeft);
            mView.bindViewport(mVrModeDistortedViewportLeft);
            mView.bindViewport(mVrModeUndistortedViewportRight);
            mView.bindViewport(mVrModeDistortedViewportRight);
        } else {
            mView.bindViewport(mNormalModeViewport);
        }

        mIsVRModeEnabled = vrMode;

    }
}
