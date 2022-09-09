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

package fi.finwe.orion360.sdk.pro.examples.appfw;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.List;

import fi.finwe.orion360.sdk.pro.OrionContext;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.TouchControllerWidget;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.licensing.LicenseManager;
import fi.finwe.orion360.sdk.pro.licensing.LicenseSource;
import fi.finwe.orion360.sdk.pro.licensing.LicenseStatus;
import fi.finwe.orion360.sdk.pro.licensing.LicenseVerifier;
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;

/**
 * An example of a minimal Orion360 video player, implemented as a custom activity with fragments.
 * <p/>
 * If you can't inherit from SimpleOrionFragment (or want to setup everything from scratch),
 * this example shows how to configure a simple Orion360 video player in your own fragment.
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
public class CustomFragmentActivity extends FragmentActivity {

    /** Tag for logging. */
    public static final String TAG = CustomFragmentActivity.class.getSimpleName();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout.
        setContentView(R.layout.activity_with_fragment);
    }

    /**
     * Fragment that uses Orion360 for rendering 360 content.
     */
    public static class MyFragment extends Fragment {

        /** Context. */
        protected Context mContext;

        /** Root view. */
        protected View mRootView;

        /**
         * OrionContext used to be a static class, but starting from Orion360 3.1.x it must
         * be instantiated as a member.
         */
        protected OrionContext mOrionContext;

        /**
         * The Android view (SurfaceView), where our 3D scene (OrionView) will be added to.
         */
        protected OrionViewContainer mViewContainer;

        /**
         * OrionView is an internal Orion360 class that it uses for presenting rendered content
         * on the device's display.
         *
         * Further, OrionViewport defines a part of an OrionView that is used for rendering an
         * OrionScene using an OrionCamera, on that particular area of the OrionView.
         *
         * In most cases, the OrionView contains just a single OrionViewport that comprises the
         * full OrionView, or two OrionViewports side by side to be used for rendering a separate
         * image for both eyes when using a VR headset. Yet, other configurations are possible.
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
         * the source texture contains less than full 360x180 degree panorama image (e.g. doughnut).
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
         * In addition, a special mathematical algorithm Auto Horizon Aligner (AHL), here used via
         * its component RotationAligner, keeps the horizon straight when user is not looking towards
         * nadir (down) or zenith (up). This allows free navigation within the sphere, without any
         * artificial limits - a feature that is unique to Orion360.
         */
        protected TouchControllerWidget mTouchController;


        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            mContext = context;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            // Propagate fragment lifecycle callbacks to the OrionContext object (singleton).
            mOrionContext = new OrionContext();
            mOrionContext.onCreate((Activity)mContext);

            // Perform Orion360 license check. A valid license file should be put to /assets folder!
            verifyOrionLicense();

            // Use a layout that has OrionVideoView.
            mRootView = inflater.inflate(R.layout.fragment_with_orion,
                    container, false);

            // Configure Orion360 for playing full spherical monoscopic videos/images.
            configureOrionView();

            return mRootView;
        }

        @Override
        public void onStart() {
            super.onStart();

            // Propagate fragment lifecycle callbacks to the OrionContext object (singleton).
            mOrionContext.onStart();
        }

        @Override
        public void onResume() {
            super.onResume();

            // Propagate fragment lifecycle callbacks to the OrionContext object (singleton).
            mOrionContext.onResume();
        }

        @Override
        public void onPause() {
            // Propagate fragment lifecycle callbacks to the OrionContext object (singleton).
            mOrionContext.onPause();

            super.onPause();
        }

        @Override
        public void onStop() {
            // Propagate fragment lifecycle callbacks to the OrionContext object (singleton).
            mOrionContext.onStop();

            super.onStop();
        }

        @Override
        public void onDestroyView() {
            // Propagate fragment lifecycle callbacks to the OrionContext object (singleton).
            mOrionContext.onDestroy();

            super.onDestroyView();
        }

        @Override
        public void onDetach() {
            super.onDetach();
        }

        /**
         * Verify Orion360 license. This is the first thing to do after creating OrionContext.
         */
        protected void verifyOrionLicense() {
            LicenseManager licenseManager = mOrionContext.getLicenseManager();
            List<LicenseSource> licenses = LicenseManager.findLicensesFromAssets(mContext);
            for (LicenseSource license : licenses) {
                LicenseVerifier verifier = licenseManager.verifyLicense(mContext, license);
                Log.i(TAG, "Orion360 license " + verifier.getLicenseSource().uri + " verified: "
                        + verifier.getLicenseStatus());
                if (verifier.getLicenseStatus() == LicenseStatus.OK) break;
            }
        }

        /**
         * Configure Orion360 for playing full spherical monoscopic videos/images.
         */
        private void configureOrionView() {

            // For compatibility with Google Cardboard 1.0 with magnetic switch, disable
            // magnetometer from sensor fusion. Also recommended for devices with poorly
            // calibrated magnetometer.
            mOrionContext.getSensorFusion().setMagnetometerEnabled(false);

            // Create a new scene. This represents a 3D world where various objects can be placed.
            mScene = new OrionScene(mOrionContext);

            // Bind sensor fusion as a controller. This will make it available for scene objects.
            mScene.bindRoutine(mOrionContext.getSensorFusion());

            // Create a new panorama. This is a 3D object that will represent a spherical
            // video/image.
            mPanorama = new OrionPanorama(mOrionContext);

            // Create a new video (or image) texture from a video (or image) source URI.
            mPanoramaTexture = OrionTexture.createTextureFromURI(mOrionContext, mContext,
                    MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);

            // Bind the panorama texture to the panorama object. Here we assume full spherical
            // equirectangular monoscopic source, and wrap the complete texture around the sphere.
            // If you have stereoscopic content or doughnut shape video, use other method variants.
            mPanorama.bindTextureFull(0, mPanoramaTexture);

            // Bind the panorama to the scene. This will make it part of our 3D world.
            mScene.bindSceneItem(mPanorama);

            // Create a new camera. This will become the end-user's eyes into the 3D world.
            mCamera = new OrionCamera(mOrionContext);

            // Define maximum limit for zooming. As an example, at value 1.0 (100%) zooming in is
            // disabled. At 3.0 (300%) camera will never reduce the FOV below 1/3 of the base FOV.
            mCamera.setZoomMax(3.0f);

            // Set yaw angle to 0. Now the camera will always point to the same yaw angle
            // (to the horizontal center point of the equirectangular video/image source)
            // when starting the app, regardless of the orientation of the device.
            mCamera.setDefaultRotationYaw(0);

            // Bind camera as a controllable to sensor fusion. This will let sensors rotate
            // the camera.
            mOrionContext.getSensorFusion().bindControllable(mCamera);

            // Create a new touch controller widget (convenience class), and let it control
            // our camera.
            mTouchController = new TouchControllerWidget(mOrionContext, mCamera);

            // Bind the touch controller widget to the scene. This will make it functional
            // in the scene.
            mScene.bindWidget(mTouchController);

            // Find Orion360 view container from the XML layout. This is an Android view for content.
            mViewContainer = (OrionViewContainer)mRootView.findViewById(R.id.orion_view_container);

            // Create a new OrionView and bind it into the container.
            mView = new OrionView(mOrionContext);
            mViewContainer.bindView(mView);

            // Bind the scene to the view. This is the 3D world that we will be rendering
            // to this view.
            mView.bindDefaultScene(mScene);

            // Bind the camera to the view. We will look into the 3D world through this camera.
            mView.bindDefaultCamera(mCamera);

            // The view can be divided into one or more viewports. For example, in VR mode we have one
            // viewport per eye. Here we fill the complete view with one (landscape) viewport.
            mView.bindViewports(OrionDisplayViewport.VIEWPORT_CONFIG_FULL,
                    OrionDisplayViewport.CoordinateType.FIXED_LANDSCAPE);
        }
    }
}
