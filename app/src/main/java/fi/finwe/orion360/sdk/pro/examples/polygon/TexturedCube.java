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

package fi.finwe.orion360.sdk.pro.examples.polygon;

import android.os.Bundle;

import java.util.Timer;
import java.util.TimerTask;

import fi.finwe.math.Quatf;
import fi.finwe.math.Vec3f;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.TouchControllerWidget;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPolygon;
import fi.finwe.orion360.sdk.pro.view.OrionView;

/**
 * An example of loading a textured 3D model and rendering it with Orion360.
 * <p/>
 * Features:
 * <ul>
 * <li>Loads one hard-coded textured cube in Wavefront .obj format from file system
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Renders the cube using standard rectilinear projection
 * <li>Allows navigation with touch & movement sensors (if supported by HW) as follows:
 * <ul>
 * <li>Panning (gyro or swipe)
 * <li>Zooming (pinch)
 * <li>Tilting (pinch rotate)
 * </ul>
 */
public class TexturedCube extends OrionActivity {

    /** The Android view where our 3D scene (OrionView) will be added to. */
    protected OrionViewContainer mViewContainer;

    /** The Orion360 SDK view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our 3D polygon model will be added to. */
    protected OrionScene mScene;

    /** The 3D polygon model loaded from Wavefront .obj file. */
    protected OrionPolygon mPolygon;

    /** The camera which will project our 3D scene to a 2D (view) surface. */
    protected OrionCamera mCamera;

    /** The widget that will handle our touch gestures. */
    protected TouchControllerWidget mTouchController;

    /** A timer for animating the polygon. */
    private Timer mTimer;


    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene(mOrionContext);

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindRoutine(mOrionContext.getSensorFusion());

        // Create a new polygon. This is where our 3D model will be loaded to.
        mPolygon = new OrionPolygon(mOrionContext);

        // Set wavefront .obj file where from the polygon model will be loaded, and also
        // set the filter pattern for selecting what to load from the file.
        mPolygon.setSourceURI(getString(R.string.asset_polygon_cube), "*");

        // Set the location where the polygon will be rendered to in the 3D world.
        mPolygon.setWorldTranslation(new Vec3f(0, 0, -1.0f));

        // Set the size of the polygon as a scale factor relative to its size in the .obj model. */
        mPolygon.setScale(0.3f);

        // Bind the polygon to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPolygon);

        // Create a timer for animation (controlled from onResume(), onPause()).
        mTimer = new Timer();

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera(mOrionContext);

        // Set yaw angle to 0. Now the camera will always point to the same yaw angle
        // when starting the app, regardless of the orientation of the device.
        mCamera.setDefaultRotationYaw(0);

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        mOrionContext.getSensorFusion().bindControllable(mCamera);

        // Create a new touch controller widget (convenience class), and let it control our camera.
        mTouchController = new TouchControllerWidget(mOrionContext, mCamera);

        // Bind the touch controller widget to the scene. This will make it functional in the scene.
        mScene.bindWidget(mTouchController);

        // Find Orion360 view container from the XML layout. This is an Android view for content.
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
	}

    @Override
    public void onResume() {
        super.onResume();

        // Start a timer task for animating the 3D model (rotate it around its own axis).
        mTimer.schedule(new TimerTask() {
            private int mRotation = 0;

            @Override
            public void run() {
                mRotation++;
                Quatf rotY = Quatf.fromRotationAxisY(
                        (float)((Math.PI) * Math.sin(mRotation / 60.f)));
                Quatf rotX = Quatf.fromRotationAxisX(
                        (float)((0.80f * Math.PI/2.0f) * Math.sin(0.33f * mRotation / 60.f)));
                mPolygon.setRotation(rotX.multiply(rotY));
            }
        }, 0, 16);
    }

    @Override
    public void onPause() {
        // Stop the timer task.
        mTimer.cancel();

        super.onPause();
    }
}
