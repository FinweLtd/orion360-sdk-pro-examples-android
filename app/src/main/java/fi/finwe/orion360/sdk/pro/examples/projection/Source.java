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

package fi.finwe.orion360.sdk.pro.examples.projection;

import android.os.Bundle;

import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.controller.RotationAligner;
import fi.finwe.orion360.sdk.pro.controller.TouchPincher;
import fi.finwe.orion360.sdk.pro.controller.TouchRotater;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.widget.OrionWidget;
import fi.finwe.util.ContextUtil;

/**
 * An example that uses content's original (source) projection.
 * <p/>
 * Features:
 * <ul>
 * <li>Plays one hard-coded full spherical (360x180) equirectangular video
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Auto-starts playback on load and stops when playback is completed
 * <li>Renders the video using original (source) projection.
 * <li>Allows navigation with touch & movement sensors (if supported by HW) as follows:
 * <ul>
 * <li>Panning (gyro or swipe)
 * <li>Zooming (pinch)
 * <li>Tilting (pinch rotate)
 * </ul>
 * <li>Auto Horizon Aligner (AHL) feature straightens the horizon</li>
 * </ul>
 */
public class Source extends OrionActivity {

    /** The Android view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our panorama sphere will be added to. */
    protected OrionScene mScene;

    /** The panorama sphere where our video texture will be mapped to. */
    protected OrionPanorama mPanorama;

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

        // Use a rectangular panel for rendering video in its original (source) projection.
        mPanorama.setPanoramaType(OrionPanorama.PanoramaType.PANEL_SOURCE);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = OrionTexture.createTextureFromURI(this,
                MainMenu.PRIVATE_EXPANSION_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mPanorama.bindTextureFull(0, mPanoramaTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanorama);

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera();

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        mOrionContext.getSensorFusion().bindControllable(mCamera);

        // Create a new touch controller widget (convenience class), and let it control our camera.
        mTouchController = new TouchControllerWidget(mCamera);

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
     * Convenience class for configuring typical touch control logic.
     */
    public class TouchControllerWidget implements OrionWidget {

        /** The camera that will be controlled by this widget. */
        private OrionCamera mCamera;

        /** Touch pinch-to-zoom/pinch-to-rotate gesture handler. */
        private TouchPincher mTouchPincher;

        /** Touch drag-to-pan gesture handler. */
        private TouchRotater mTouchRotater;

        /** Rotation aligner keeps the horizon straight at all times. */
        private RotationAligner mRotationAligner;


        /**
         * Constructs the widget.
         *
         * @param camera The camera to be controlled by this widget.
         */
        TouchControllerWidget(OrionCamera camera) {

            // Keep a reference to the camera that we control.
            mCamera = camera;

            // Create pinch-to-zoom/pinch-to-rotate handler.
            mTouchPincher = new TouchPincher();
            mTouchPincher.setMinimumDistanceDp(mOrionContext.getActivity(), 20);
            mTouchPincher.bindControllable(mCamera, OrionCamera.VAR_FLOAT1_ZOOM);

            // Create drag-to-pan handler.
            mTouchRotater = new TouchRotater();
            mTouchRotater.bindControllable(mCamera);

            // Create the rotation aligner, responsible for rotating the view so that the horizon
            // aligns with the user's real-life horizon when the user is not looking up or down.
            mRotationAligner = new RotationAligner();
            mRotationAligner.setDeviceAlignZ(-ContextUtil.getDisplayRotationDegreesFromNatural(
                    mOrionContext.getActivity()));
            mRotationAligner.bindControllable(mCamera);

            // Rotation aligner needs sensor fusion data in order to do its job.
            mOrionContext.getSensorFusion().bindControllable(mRotationAligner);
        }

        @Override
        public void onBindWidget(OrionScene scene) {
            // When widget is bound to scene, bind the controllers to it to make them functional.
            scene.bindController(mTouchPincher);
            scene.bindController(mTouchRotater);
            scene.bindController(mRotationAligner);
        }

        @Override
        public void onReleaseWidget(OrionScene scene) {
            // When widget is released from scene, release the controllers as well.
            scene.releaseController(mTouchPincher);
            scene.releaseController(mTouchRotater);
            scene.releaseController(mRotationAligner);
        }
    }
}
