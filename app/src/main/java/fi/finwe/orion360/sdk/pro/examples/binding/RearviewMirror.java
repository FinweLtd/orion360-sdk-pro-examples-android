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

package fi.finwe.orion360.sdk.pro.examples.binding;

import android.graphics.RectF;
import android.os.Bundle;

import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;
import fi.finwe.orion360.sdk.pro.controller.RotationAligner;
import fi.finwe.orion360.sdk.pro.controller.TouchPincher;
import fi.finwe.orion360.sdk.pro.controller.TouchRotater;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.widget.OrionWidget;
import fi.finwe.util.ContextUtil;

/**
 * An example of bindings for creating a player with a rear-view mirror.
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
public class RearviewMirror extends OrionActivity {

    /** The Android view where our 3D scene (OrionView) will be added to. */
    protected OrionViewContainer mViewContainer;

    /** The Orion360 SDK view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scenes where our panorama spheres will be added to. */
    protected OrionScene mScene;
    protected OrionScene mSceneRearview;

    /** The panorama spheres where our video textures will be mapped to. */
    protected OrionPanorama mPanorama;
    protected OrionPanorama mPanoramaRearview;

    /** The video texture where our decoded video frames will be updated to. */
    protected OrionTexture mPanoramaTexture;

    /** The camera which will project our 3D scene to a 2D main viewport surface. */
    protected OrionCamera mMainViewCamera;

    /** The camera which will project our 3D scene to a 2D rear-view viewport surface. */
    protected OrionCamera mRearViewCamera;

    /** The widget that will handle our touch gestures. */
    protected TouchControllerWidget mTouchController;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // Create new scenes. These represent 3D worlds where various objects can be placed.
        mScene = new OrionScene(mOrionContext);
        mSceneRearview = new OrionScene(mOrionContext);

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindRoutine(mOrionContext.getSensorFusion());

        // Create new panoramas. These are 3D objects that will represent a spherical video/image.
        mPanorama = new OrionPanorama(mOrionContext);
        mPanoramaRearview = new OrionPanorama(mOrionContext);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = OrionTexture.createTextureFromURI(mOrionContext, this,
                MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);

        // Bind the panorama textures to the panorama objects. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mPanorama.bindTextureFull(0, mPanoramaTexture);
        mPanoramaRearview.bindTextureFull(0, mPanoramaTexture);

        // Bind the panoramas to the scenes. This will make them part of our 3D worlds.
        mScene.bindSceneItem(mPanorama);
        mSceneRearview.bindSceneItem(mPanoramaRearview);

        // Create a new camera (main view). This will become the end-user's eyes into the 3D world.
        mMainViewCamera = new OrionCamera(mOrionContext);

        // Reset view to the 'front' direction (horizontal center of the panorama).
        mMainViewCamera.setDefaultRotationYaw(0);

        // Create a new camera (rear-view). This will become the end-user's eyes into the 3D world.
        mRearViewCamera = new OrionCamera(mOrionContext);

        // Reset view to the 'back' direction.
        mRearViewCamera.setDefaultRotationYaw(180.0f);

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        mOrionContext.getSensorFusion().bindControllable(mMainViewCamera);

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        mOrionContext.getSensorFusion().bindControllable(mRearViewCamera);

        // Create a new touch controller widget (convenience class) and let it control our cameras.
        // This is a slightly modified version compared to the usual one, with two cameras.
        mTouchController = new TouchControllerWidget(mMainViewCamera, mRearViewCamera);

        // Bind the touch controller widget to the scene. This will make it functional in the scene.
        mScene.bindWidget(mTouchController);

        // Find Orion360 view container from the XML layout. This is an Android view for content.
        mViewContainer = (OrionViewContainer)findViewById(R.id.orion_view_container);

        // Create a new OrionView and bind it into the container.
        mView = new OrionView(mOrionContext);
        mViewContainer.bindView(mView);

        // The view can be divided into one or more viewports. For example, in VR mode we have one
        // viewport per eye. Here we fill the complete view with one (landscape) viewport, and
        // add another much smaller one on top of it to function as a rear-view mirror.
        mView.bindViewports(new RectF[] {
                new RectF(0.0F, 1.0F, 1.0F, 0.0F),
                // above: Main view covers the whole view
                new RectF(0.25F, 0.95F, 0.75F, 0.75F) },
                // above: Rear-view covers small area on top
                OrionDisplayViewport.CoordinateType.FIXED_LANDSCAPE);

        // Notice the viewport rect coordinate system. The viewport coordinates are relative
        // to its parent view, whose left edge is 0.0 and right edge 1.0, bottom edge is 0.0
        // and top edge 1.0. Values less than 0.0 (negative) and greater than 1.0 are allowed.
        // The coordinates are given as follows:
        // - 1st param: viewport left coordinate (calculated from view left edge at 0.0)
        // - 2nd param: viewport top coordinate (calculated from view bottom edge at 0.0)
        // - 3rd param: viewport right coordinate (calculated from view left edge at 0.0)
        // - 4th param: viewport bottom coordinate (calculated from view bottom edge at 0.0)

        // Bind the main viewport to the main view camera.
        mView.getViewports()[0].bindCamera(mMainViewCamera);
        mView.getViewports()[0].bindScene(mScene);

        // Bind the rear-view viewport to the rear-view camera.
        mView.getViewports()[1].bindCamera(mRearViewCamera);
        mView.getViewports()[1].bindScene(mSceneRearview);
	}

    /**
     * Convenience class for configuring typical touch control logic.
     */
    public class TouchControllerWidget implements OrionWidget {

        /** Touch pinch-to-zoom/pinch-to-rotate gesture handler. */
        private final TouchPincher mTouchPincher;

        /** Touch drag-to-pan gesture handler. */
        private final TouchRotater mTouchRotater;

        /** Rotation aligner keeps the horizon straight at all times. */
        private final RotationAligner mRotationAligner;


        /**
         * Constructs the widget.
         *
         * @param mainCamera The main camera to be controlled by this widget.
         * @param rearViewCamera The rear-view camera to be controlled by this widget.
         */
        TouchControllerWidget(OrionCamera mainCamera, OrionCamera rearViewCamera) {

            // Create pinch-to-zoom/pinch-to-rotate handler for the main camera only.
            // Notice that we do not want zooming to rear-view mirror, hence no binding.
            mTouchPincher = new TouchPincher(mOrionContext);
            mTouchPincher.setMinimumDistanceDp(mOrionContext.getActivity(), 20);
            mTouchPincher.bindControllable(mainCamera, OrionCamera.VAR_FLOAT1_ZOOM);

            // Create drag-to-pan handler.
            // Notice that we want panning to affect to both main and rear-view cameras.
            mTouchRotater = new TouchRotater(mOrionContext);
            mTouchRotater.bindControllable(mainCamera);
            mTouchRotater.bindControllable(rearViewCamera);

            // Create the rotation aligner, responsible for rotating the view so that the horizon
            // aligns with the user's real-life horizon when the user is not looking up or down.
            mRotationAligner = new RotationAligner(mOrionContext);
            mRotationAligner.setDeviceAlignZ(-ContextUtil.getDisplayRotationDegreesFromNatural(
                    mOrionContext.getActivity()));
            mRotationAligner.bindControllable(mainCamera);
            mRotationAligner.bindControllable(rearViewCamera);

            // Rotation aligner needs sensor fusion data in order to do its job.
            mOrionContext.getSensorFusion().bindControllable(mRotationAligner);

            // Notice that touch gestures will work when performed either on main viewport or
            // rear-view viewport. If user has zoomed in the amount of panning applied is
            // relative to the zoom level.
        }

        @Override
        public void onBindWidget(OrionScene scene) {
            // When widget is bound to scene, bind the controllers to it to make them functional.
            scene.bindRoutine(mTouchPincher);
            scene.bindRoutine(mTouchRotater);
            scene.bindRoutine(mRotationAligner);
        }

        @Override
        public void onReleaseWidget(OrionScene scene) {
            // When widget is released from scene, release the controllers as well.
            scene.releaseRoutine(mTouchPincher);
            scene.releaseRoutine(mTouchRotater);
            scene.releaseRoutine(mRotationAligner);
        }
    }
}
