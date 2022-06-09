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
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.TouchControllerWidget;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.item.OrionSceneItem;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;

/**
 * An example of bindings for creating a player with an overview image on top.
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
public class Overview extends OrionActivity {

    /** The Android view where our 3D scenes will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our rectilinear panorama sphere will be added to. */
    protected OrionScene mSceneRectilinear;

    /** The 3D scene where our equirectangular panorama plane will be added to. */
    protected OrionScene mSceneEquirectangular;

    /** The rectilinear panorama sphere where our video texture will be mapped to. */
    protected OrionPanorama mPanoramaRectilinear;

    /** The equirectangular panorama plane where our video texture will be mapped to. */
    protected OrionPanorama mPanoramaEquirectangular;

    /** The video texture where our decoded video frames will be updated to. */
    protected OrionTexture mPanoramaTexture;

    /** The camera which will project our 3D scene to a 2D main viewport surface. */
    protected OrionCamera mMainViewCamera;

    /** The camera which will project our 3D scene to a 2D overview viewport surface. */
    protected OrionCamera mOverviewCamera;

    /** The widget that will handle our touch gestures. */
    protected TouchControllerWidget mTouchController;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // Create two scenes. Notice that we need multiple scenes because we will have
        // multiple panoramas and want only one panorama to be visible per scene.
        mSceneRectilinear = new OrionScene();
        mSceneEquirectangular = new OrionScene();

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mSceneRectilinear.bindController(mOrionContext.getSensorFusion());

        // Create new panoramas for rectilinear sphere and equirectangular plane projections.
        mPanoramaRectilinear = new OrionPanorama();
        mPanoramaRectilinear.setPanoramaType(OrionPanorama.PanoramaType.SPHERE);
        mPanoramaEquirectangular = new OrionPanorama();
        mPanoramaEquirectangular.setPanoramaType(OrionPanorama.PanoramaType.PANEL_SOURCE);

        // We don't need perspective camera for viewing original equirectangular projection.
        mPanoramaEquirectangular.setRenderingMode(OrionSceneItem.RenderingMode.CAMERA_DISABLED);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = OrionTexture.createTextureFromURI(this,
                MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);

        // Bind the panorama texture to the panorama objects. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere /
        // onto the plane.
        mPanoramaRectilinear.bindTextureFull(0, mPanoramaTexture);
        mPanoramaEquirectangular.bindTextureFull(0, mPanoramaTexture);

        // Bind the panoramas to the scenes. This will make them part of our two separate 3D worlds.
        mSceneRectilinear.bindSceneItem(mPanoramaRectilinear);
        mSceneEquirectangular.bindSceneItem(mPanoramaEquirectangular);

        // Next we will create a separate camera for both viewports, because we want the
        // main viewport to respond to sensors and touch but overview camera stay still.

        // Create a new camera for the main viewport.
        mMainViewCamera = new OrionCamera();

        // Set yaw angle to 0. Now the camera will always point to the same angle
        // (to the center point of the equirectangular video/image source)
        // when starting the app, regardless of the orientation of the device.
        mMainViewCamera.setDefaultRotationYaw(0.0f);

        // Create a new camera for the overview viewport.
        mOverviewCamera = new OrionCamera();

        // Set yaw angle to 0. Now the camera will always point to the same angle
        // (to the center point of the equirectangular video/image source)
        // when starting the app, regardless of the orientation of the device.
        mOverviewCamera.setDefaultRotationYaw(0.0f);

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        mOrionContext.getSensorFusion().bindControllable(mMainViewCamera);

        // Create a new touch controller widget (convenience class) and let it control our camera.
        mTouchController = new TouchControllerWidget(mOrionContext, mMainViewCamera);

        // Bind the touch controller widget to the scene. This will make it functional in the scene.
        mSceneRectilinear.bindWidget(mTouchController);

        // Find Orion360 view from the XML layout. This is an Android view where we render content.
        mView = (OrionView)findViewById(R.id.orion_view);

        // The view can be divided into one or more viewports. For example, in VR mode we have one
        // viewport per eye. Here we fill the complete view with one (landscape) viewport, and
        // add another much smaller one on top of it to function as an overview image.
        mView.bindViewports(new RectF[] {
                new RectF(0.0F, 1.0F, 1.0F, 0.0F),
                // above: Main covers the whole view
                new RectF(0.355F, 0.25F, 0.645F, 0.0F) },
                // above: Overview covers small bottom area
                OrionViewport.CoordinateType.FIXED_LANDSCAPE);

        // Notice the viewport rect coordinate system. The viewport coordinates are relative
        // to its parent view, whose left edge is 0.0 and right edge 1.0, bottom edge is 0.0
        // and top edge 1.0. Values less than 0.0 (negative) and greater than 1.0 are allowed.
        // The coordinates are given as follows:
        // - 1st param: viewport left coordinate (calculated from view left edge at 0.0)
        // - 2nd param: viewport top coordinate (calculated from view bottom edge at 0.0)
        // - 3rd param: viewport right coordinate (calculated from view left edge at 0.0)
        // - 4th param: viewport bottom coordinate (calculated from view bottom edge at 0.0)

        // Bind the rectilinear scene to the main viewport.
        mView.getViewports()[0].bindScene(mSceneRectilinear);

        // Bind the equirectangular scene to the overview viewport.
        mView.getViewports()[1].bindScene(mSceneEquirectangular);

        // Bind the main viewport to the main viewport camera.
        mView.getViewports()[0].bindCamera(mMainViewCamera);

        // Bind the overview viewport to the overview viewport camera.
        mView.getViewports()[1].bindCamera(mOverviewCamera);
	}
}
