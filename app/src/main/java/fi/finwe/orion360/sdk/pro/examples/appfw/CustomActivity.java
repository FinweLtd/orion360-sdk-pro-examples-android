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

package fi.finwe.orion360.sdk.pro.examples.appfw;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

import fi.finwe.math.QuatF;
import fi.finwe.orion360.sdk.pro.OrionContext;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.controller.RotationAligner;
import fi.finwe.orion360.sdk.pro.controller.TouchPincher;
import fi.finwe.orion360.sdk.pro.controller.TouchRotater;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.licensing.LicenseManager;
import fi.finwe.orion360.sdk.pro.licensing.LicenseSource;
import fi.finwe.orion360.sdk.pro.licensing.LicenseStatus;
import fi.finwe.orion360.sdk.pro.licensing.LicenseVerifier;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.item.OrionSceneItem;
import fi.finwe.orion360.sdk.pro.item.OrionSceneItem.RotationBaseControllerListenerBase;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.widget.OrionWidget;
import fi.finwe.util.ContextUtil;

/**
 * An example of a minimal Orion360 video player, implemented as a custom activity.
 * <p/>
 * If you can't inherit from SimpleOrionActivity (or want to setup everything from scratch),
 * this example shows how to configure a simple Orion360 video player in your own activity.
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
public class CustomActivity extends Activity {

    /** Tag for logging. */
    public static final String TAG = CustomActivity.class.getSimpleName();

    /**
     * OrionView is an Android SurfaceView that Orion360 uses for presenting rendered content
     * on the device display. Different types of OrionViews exist for the purpose of rendering
     * content using a particular external library, such as Oculus GearVR (OrionOvrView) or
     * Google DayDream (OrionGvrView).
     *
     * Further, OrionViewport defines a part of an OrionView that is used for rendering an
     * OrionScene using an OrionCamera (on that particular slice of the OrionView).
     *
     * In most cases, the OrionView contains just a single OrionViewport that comprises the
     * full OrionView, or two OrionViewports side by side to be used for rendering a separate
     * image for both eyes when using a VR headset.
     */
    protected OrionView mView;

    /**
     * OrionScene is a collection of content that is present in a rendered OrionView. The content
     * consists of a set of visible artefacts (OrionSceneItem) and programs that manipulate them
     * (OrionController).
     */
    protected OrionScene mScene;

    /**
     * OrionPanorama is an OrionSceneItem that specializes in rendering equirectangular textures.
     *
     * In most cases, the texture is wrapped around the inner surface of a polygon (sphere,
     * diamond etc.) and the camera placed at the center of the polygon, which presents a
     * beautiful panoramic view of the given texture.
     *
     * In other cases, planar surfaces are used to render the texture in original (source)
     * format or with a special effect such as the little planet projection.
     *
     * OrionPanorama is also responsible for handling the cases where different parts of the
     * source texture needs to be mapped into different parts of the polygon, and cases where
     * the source texture contains less than full 360x180 degree panorama image.
     */
    protected OrionPanorama mPanorama;

    /**
     * OrionTexture represents a graphical image such as a still image (OrionImageTexture),
     * video (OrionVideoTexture) or camera preview image (OrionCameraTexture).
     *
     * In order to render the texture into the scene, the OrionTexture needs to be bound to an
     * OrionSceneItem that defines the object's shape, location, size and orientation in the 3D
     * rendering world. This OrionSceneItem may be an OrionSprite (for planar shapes) or
     * OrionPanorama (for spherical shapes).
     */
    protected OrionTexture mPanoramaTexture;

    /**
     * OrionCamera is an OrionSceneItem that defines the viewing parameters in the 3D rendering
     * world, such as the location, orientation and the field of view (FOV) of the viewer.
     *
     * The OrionCamera can be bound to an OrionViewport or an OrionView. When bound to an
     * OrionView, the camera is used as a default for each OrionViewport that does not have
     * another explicitly bound camera.
     */
    protected OrionCamera mCamera;

    /**
     * Convenience class for configuring typical touch control logic, where dragging gestures
     * are mapped to panning the view, and pinch gestures to zooming/rotating the view.
     *
     * In addition, a special mathematical algorithm called RotationAligner keeps the horizon
     * straight when user is not looking towards nadir (down) or zenith (up). This allows
     * free navigation within the sphere, without any artificial limits - a feature that is
     * unique to Orion360.
     */
    protected TouchControllerWidget mTouchController;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Propagate activity lifecycle callbacks to the OrionContext object (singleton).
        OrionContext.onCreate(this);

        // Perform Orion360 license check. A valid license file should be put to /assets folder!
        verifyOrionLicense();

        // Set layout.
		setContentView(R.layout.activity_main);

        // Configure Orion360 for playing full spherical monoscopic videos/images.
        configureOrionView();
	}

    @Override
    protected void onStart() {
        super.onStart();

        // Propagate activity lifecycle callbacks to the OrionContext object (singleton).
        OrionContext.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Propagate activity lifecycle callbacks to the OrionContext object (singleton).
        OrionContext.onResume();
    }

    @Override
    protected void onPause() {
        // Propagate activity lifecycle callbacks to the OrionContext object (singleton).
        OrionContext.onPause();

        super.onPause();
    }

    @Override
    protected void onStop() {
        // Propagate activity lifecycle callbacks to the OrionContext object (singleton).
        OrionContext.onStop();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // Propagate activity lifecycle callbacks to the OrionContext object (singleton).
        OrionContext.onDestroy();

        super.onDestroy();
    }

    /**
     * Verify Orion360 license. This is the first thing to do after creating OrionContext.
     */
    protected void verifyOrionLicense() {
        LicenseManager licenseManager = OrionContext.getLicenseManager();
        List<LicenseSource> licenses = licenseManager.findLicensesFromAssets(this);
        for (LicenseSource license : licenses) {
            LicenseVerifier verifier = licenseManager.verifyLicense(this, license);
            Log.i(TAG, "Orion360 license " + verifier.getLicenseSource().uri + " verified: "
                    + verifier.getLicenseStatus());
            if (verifier.getLicenseStatus() == LicenseStatus.OK) break;
        }
    }

    /**
     * Configure Orion360 for playing full spherical monoscopic videos/images.
     */
    private void configureOrionView() {

        // For compatibility with Google Cardboard 1.0 with magnetic switch, disable magnetometer
        // from sensor fusion. Also recommended for devices with poorly calibrated magnetometer.
        OrionContext.getSensorFusion().setMagnetometerEnabled(false);

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene();

        // To prevent rendering garbage, hide the scene until we have initialized everything.
        mScene.setVisible(false);

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindController(OrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanorama = new OrionPanorama();

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

        // Define maximum limit for zooming. As an example, at value 1.0 (100%) zooming in is
        // disabled. At 3.0 (300%) camera will never reduce the FOV below 1/3 of the base FOV.
        mCamera.setZoomMax(3.0f);

        // React to the camera getting bound to the SensorFusion for the first time.
        mCamera.setRotationBaseControllerListener(new RotationBaseControllerListenerBase() {
            @Override
            public void onRotationBaseControllerBound(OrionSceneItem item, QuatF rotationBase) {
                super.onRotationBaseControllerBound(item, rotationBase);

                // Set yaw angle to 0. Now the camera will always point to the same yaw angle
                // (to the horizontal center point of the equirectangular video/image source)
                // when starting the app, regardless of the orientation of the device.
                item.setRotationYaw(0);

                // The camera is now properly initialized, and we can start rendering the scene.
                mScene.setVisible(true);
            }
        });

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
}
