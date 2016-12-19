/**
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
import fi.finwe.orion360.sdk.pro.OrionContext;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.controller.RotationAligner;
import fi.finwe.orion360.sdk.pro.controller.TouchPincher;
import fi.finwe.orion360.sdk.pro.controller.TouchRotater;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.item.OrionPolygon;
import fi.finwe.orion360.sdk.pro.item.OrionSceneItem;
import fi.finwe.orion360.sdk.pro.item.sprite.OrionSprite;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.widget.OrionWidget;
import fi.finwe.util.ContextUtil;

/**
 * An example of a 3D model within a 360 panorama background and planar video background.
 * <p/>
 * Features:
 * <ul>
 * <li>Loads one hard-coded untextured model in Wavefront .obj format from file system
 * <li>Loads one hard-coded 360 panorama image in .jpg format from file system
 * <li>Player one hard-coded planar rectilinear video</li>
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Renders the panorama and the model using standard rectilinear projection
 * <li>Allows navigation with touch & movement sensors (if supported by HW) as follows:
 * <ul>
 * <li>Panning (gyro or swipe)
 * <li>Zooming (pinch)
 * <li>Tilting (pinch rotate)
 * </ul>
 * <li>Auto Horizon Aligner (AHL) feature straightens the horizon</li>
 * </ul>
 */
public class OrionFigure extends OrionActivity {

    /** The Android view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our 3D polygon model will be added to. */
    protected OrionScene mScene;

    /** The panorama sphere where our image texture will be mapped to. */
    protected OrionPanorama mPanorama;

    /** The image texture where our decoded image will be updated to. */
    protected OrionTexture mPanoramaTexture;

    /** The sprite where our video texture will be mapped to. */
    protected OrionSprite mSprite;

    /** The video texture where our decoded video frames will be updated to. */
    protected OrionTexture mSpriteTexture;

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
        mScene = new OrionScene();

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindController(OrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanorama = new OrionPanorama();

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = OrionTexture.createTextureFromURI(this,
                MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_IMAGE_FILE_LIVINGROOM_HQ);

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mPanorama.bindTextureFull(0, mPanoramaTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanorama);

        // Create a new sprite. This is a 2D plane in the 3D world for our planar video.
        mSprite = new OrionSprite();

        // Create a new video (or image) texture from a video (or image) source URI.
        mSpriteTexture = OrionTexture.createTextureFromURI(this,
                "http://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4");

        // Set sprite location in the 3D world. Here we place the video on the white screen.
        mSprite.setWorldTranslation(new Vec3f(0.03f, 0.19f, -0.77f));

        // Set sprite size in the 3D world. Here we make it fit on the white screen.
        mSprite.setScale(0.42f);

        // Bind the sprite texture to the sprite object. Here we assume planar rectilinear source.
        mSprite.bindTexture(mSpriteTexture);

        // Bind the sprite to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mSprite);

        // Create a new polygon. This is where our 3D model will be loaded to.
        mPolygon = new OrionPolygon();

        // Set wavefront .obj file where from the polygon model will be loaded, and also
        // set the filter pattern for selecting what to load from the file.
        mPolygon.setSourceURI(getString(R.string.asset_polygon_orion_plain), "*");

        // Set the location where the Orion360 figure polygon will be rendered to in the 3D world.
        // Here we put it balancing on top of the footstool to give some tension to the scene.
        mPolygon.setWorldTranslation(new Vec3f(-0.25f, -0.4f, -0.6f));

        // Set the size of the polygon as a scale factor relative to its size in the .obj model. */
        mPolygon.setScale(0.1f);

        // Bind the polygon to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPolygon);

        // Create a timer for animation (controlled from onResume(), onPause()).
        mTimer = new Timer();

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera();

        // Set yaw angle to 0. Now the camera will always point to the same yaw angle
        // when starting the app, regardless of the orientation of the device.
        mCamera.setRotationYaw(0);

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        OrionContext.getSensorFusion().bindControllable(mCamera);

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
            mTouchPincher.setMinimumDistanceDp(OrionContext.getActivity(), 20);
            mTouchPincher.bindControllable(mCamera, OrionCamera.VAR_FLOAT1_ZOOM);

            // Create drag-to-pan handler.
            mTouchRotater = new TouchRotater();
            mTouchRotater.bindControllable(mCamera);

            // Create the rotation aligner, responsible for rotating the view so that the horizon
            // aligns with the user's real-life horizon when the user is not looking up or down.
            mRotationAligner = new RotationAligner();
            mRotationAligner.setDeviceAlignZ(-ContextUtil.getDisplayRotationDegreesFromNatural(
                    OrionContext.getActivity()));
            mRotationAligner.bindControllable(mCamera);

            // Rotation aligner needs sensor fusion data in order to do its job.
            OrionContext.getSensorFusion().bindControllable(mRotationAligner);
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

        super.onDestroy();
    }

}
