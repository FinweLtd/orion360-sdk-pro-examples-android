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

package fi.finwe.orion360.sdk.pro.examples.input;

import android.os.Bundle;
import android.util.Log;

import fi.finwe.math.QuatF;
import fi.finwe.math.Vec3F;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionContext;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.controller.RotationAligner;
import fi.finwe.orion360.sdk.pro.controller.SensorFusion;
import fi.finwe.orion360.sdk.pro.controller.TouchPincher;
import fi.finwe.orion360.sdk.pro.controller.TouchRotater;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.item.OrionSceneItem;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.widget.OrionWidget;
import fi.finwe.util.ContextUtil;

/**
 * An example that focuses on sensor fusion input.
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
public class Sensors extends OrionActivity implements SensorFusion.Listener {

    /** Tag for logging. */
    public static final String TAG = Sensors.class.getSimpleName();

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

        /**
         * 360/VR video applications automatically rotate the view based on device orientation.
         * Hence, to look at a desired direction, end-user can turn the device towards that
         * direction, or when viewing through a VR frame, simply turn her head.
         * <p/>
         * This feature requires hardware movement sensors that are built-in to phones and
         * tablets - most importantly, a gyroscope. Unfortunately, not all Android devices
         * have one. The feature is automatically disabled on devices that do not have the
         * necessary sensor hardware, with a fallback to touch-only control.
         * <p/>
         * To make sensor fusion available for items in your 3D scene, get it from OrionContext
         * and bind it as a controller to the scene. Notice that you do not need to create
         * or initialize the sensor fusion algorithm; this is automatically done when
         * OrionContext is first created.
         */
        mScene.bindController(OrionContext.getSensorFusion());

        /**
         * Sensor fusion makes use of a sophisticated mathematical algorithm to combine the
         * data from the hardware sensors so that device's current orientation can be deduced.
         * <p/>
         * Many Android devices have built-in sensor fusion algorithms, some even several.
         * Their quality and availability varies - a typical Android fragmentation issue.
         * To overcome this problem, Orion360 contains a built-in high quality sensor fusion
         * algorithm that is particularly well tuned for 360/VR video application usage.
         * The supported hardware movement sensors include:
         * <li>
         * <ul/>an accelerometer, which tells where is 'Down' direction, and linear acceleration
         * <ul/>a magnetometer, which tells where is 'North' direction, and slow rotation
         * <ul/>a gyroscope, which tells the rotation about the device's own axis very rapidly
         * </li>
         * Using data from all three sensors the algorithm calculates device orientation several
         * hundreds of times per second.
         * <p/>
         * While gyroscope and accelerometer are absolutely required for proper operation,
         * the magnetometer sensor is optional. Frequently, it is not necessary to align
         * the 360 content with the point of the compass - instead, the view is rotated
         * so that viewing begins from the content's 'front' direction, despite of the end
         * user's initial orientation with respect to North.
         * <p/>
         * Some VR frames (especially the 1st generation Google Cardboards) have a magnetic
         * switch that confuses the magnetometer reading just like a magnetic object confuses
         * a compass. Furthermore, some devices have poor magnetometer calibration; the typical
         * symptom is that the view begins to rotate horizontally at a constant slow rate
         * even though the device is held still, or refuses to turn to some directions.
         * While manual calibration often helps (draw an 8-figure in the air a few times using
         * the device as a 'pen', simultaneously rotating it along its axis), end users are not
         * generally aware of this. Moreover, the device may need to be re-calibrated when the
         * magnetic environment changes - for example, the device is placed near metallic objects
         * or strong electrical currents (USB cable connected for charging, for instance).
         * <p/>
         * As a conclusion, it is often better to disable the magnetometer. The recommended
         * place to do that is here when Orion360 environment is being initially configured.
         * <p/>
         * If you wonder why it isn't disabled by default, the reason is that the sensor fusion
         * stabilizes more quickly when it is using data from all three sensors. In addition,
         * consider the case when the device is lying on a table display facing directly up or
         * down. The accelerometer and gyroscope sensors cannot determine the Azimuth angle,
         * i.e. know if the device top side is pointing to North, East or what. However, when
         * magnetometer is active during the initialization phase and even just a few samples
         * are received from it, the view can be much better oriented to make the 'front'
         * direction appear when the end-user lifts the phone up.
         */
        OrionContext.getSensorFusion().setMagnetometerEnabled(false);

        /**
         * When OrionContext is created and sensor fusion algorithm initialized and started,
         * it begins to iterate from its default orientation towards the actual device
         * orientation by calculating new orientation estimates based on received movement
         * sensor samples.
         *
         * This process takes a (short) moment to complete: the estimated orientation quickly
         * reaches and stabilizes to the actual device orientation. However, to prevent rendering
         * a few frames during initialization phase it is recommended to initially hide the
         * scene - and make it visible after sensor fusion has stabilized.
         */
        mScene.setVisible(false);

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

        /**
         * Binding the sensor fusion as a controller to the scene does not yet make the view
         * respond to device orientation changes - we haven't yet told which object it is
         * supposed to control. To reach this goal, we need to let sensor fusion control our
         * camera in the 3D world. This can be accomplished simply by binding the camera to
         * the sensor fusion as a controllable. If you have multiple cameras, bind all of
         * them that you wish to be controlled by sensors.
         */
        OrionContext.getSensorFusion().bindControllable(mCamera);

        /**
         * It is often useful to set a listener for receiving an event when the sensor fusion
         * is actually bound to a camera.
         */
        mCamera.setRotationBaseControllerListener(new OrionSceneItem.RotationBaseControllerListenerBase() {
            @Override
            public void onRotationBaseControllerBound(OrionSceneItem item, QuatF rotationBase) {
                super.onRotationBaseControllerBound(item, rotationBase);

                /**
                 * If you want to ensure that viewing begins from the horizontal center point
                 * of the equirectangular panoramic content ('front' direction appears when
                 * device is lifted up to reveal the horizon level), set item yaw rotation
                 * to 0 degrees using this convenience method.
                 *
                 * You can also set it to any other yaw angle, for example value 90 makes the
                 * panorama start from 'left' and -90 from 'right'.
                 *
                 * Notice that in VR mode it is important to use this method variant for
                 * configuring the initial viewing angle as this only sets the yaw angle
                 * and lets the sensor fusion algorithm determine proper compensation to
                 * keep the 3D scene upright (horizon appears in level with real world).
                 */
                item.setRotationYaw(0);

                /**
                 * If you want to set the initial viewing angle freely (and override initial
                 * upright compensation), you can use this method variant. In this example the
                 * view starts from 'left' orientation: first we create a rotation quaternion
                 * with 90 degree rotation about the Y axis, and then set this as the rotation
                 * for the item. The 'left' direction will initially appear at the center of
                 * the screen, no matter what is the orientation of the device.
                 *
                 * This approach is not suitable for VR mode, but can be useful for example
                 * if user is lying on a sofa (looking up) or sitting in a bus (looking down)
                 * and we'd still like to start from the 'front' direction (instead of top or
                 * bottom direction, respectively).
                 */
                //item.setRotation(QuatF.fromRotationAxisY(90 * Math.PI / 180.0f));

                /**
                 * When this event is fired the sensor fusion should have stabilized already.
                 * Hence, this is a good place to make the scene visible again, if you decided
                 * to hide it initially.
                 */
                mScene.setVisible(true);
            }
        });

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

            /**
             * The sensor fusion algorithm is also responsible for merging end user's touch
             * input drag events with the orientation calculated from movement sensor data
             * (touch tapping events are handled separately, see Touch input example).
             * <p/>
             * Many sensor fusion algorithms on the market have severe shortcomings in this
             * particular task; most notably they prevent panning the 360 view over the nadir
             * and zenith directions, thus creating an artificial limit in navigation. This
             * is annoying to users especially when viewing drone shots where most of the
             * action occurs down below and panning to different directions near the nadir
             * is very common. Watching fireworks is an example of similar situation at the
             * opposite direction; panning occurs near the zenith. Moreover, the basic
             * principle of panning with touch - keeping the image position where user
             * initially touched under the finger all the time - is broken near nadir and
             * zenith directions in most of the algorithms.
             * <p/>
             * The underlying reason is their failure to solve the problem of tilted horizon
             * properly. Consider a case where user is looking straight ahead towards the
             * horizon line, then pans straight down to nadir, and finally straight to left
             * or right, up to the horizon level. The horizon now appears 90 degrees tilted.
             * The easy solution is to forget the mapping between the fingertip and touched
             * image position, apply left-right finger movement directly to panning the yaw
             * angle full 360 degrees, and then apply the up-down finger movement to panning
             * the pitch angle ONLY from -90 (nadir) to +90 (zenith), clamping any further
             * panning attempts to these limits - and thus creating the before said issues.
             * <p/>
             * Orion360 sensor fusion solves this with a special auto-horizon aligner
             * algorithm, which silently steps in and gently rotates the view so that the
             * horizon gets aligned again. This unique approach is mathematically much more
             * complex, but provides a very good user experience. The feature fuses touch
             * and movement sensor controls perfectly together, and allows panning freely
             * within the whole image sphere without any artificial limits - the way 360
             * content should be experienced.
             * <p/>
             * To enable basic panning with touch, create a TouchRotater object and bind the
             * camera as a controllable to it.
             * <p/>
             * To enable auto-horizon aligner, create a RotationAligner object, tell the
             * primary direction for alignment (taking into account device's display rotation!),
             * and bind the camera as a controllable to it. Also bind the RotationAligner
             * as a controllable to sensor fusion, as it needs sensor data for alignment.
             */

            // Keep a reference to the camera that we control.
            mCamera = camera;

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

            /**
             * Most of the touch screens are able to detect at least two fingers on screen
             * simultaneously, allowing two additional drag-type gestures to be recognized:
             * two-finger pinch, and two-finger rotate. Still following the basic principle
             * of touch control, the sensor fusion algorithm aims for keeping the image
             * position where the fingers initially touched under the fingers all the time,
             * by means of panning, zooming and rotating the image.
             * <p/>
             * As usual, the pinch gesture is used for zooming, which in case of 360 content
             * means changing the virtual camera field-of-view (FOV). The developer should set
             * reasonable limits to zooming based on his content: consider your resolution
             * and set a limit for zooming in so that image does not get too blurry.
             * <p/>
             * Configure the FOV by setting the default value (typically 1.0f) and the maximum
             * value (e.g. 3.0f). Notice that end-user is able to use pinch gesture to change
             * the camera FOV in range [1.0-max].
             * <p/>
             * In order to prevent zooming altogether, simply do not bind (or release binding)
             * between touch pincher and camera zoom variable.
             * <p/>
             * The two-finger rotate gesture is mapped to rotating the content along the
             * camera roll axis. This is handy especially when viewing content at the nadir
             * direction. However, it can be confusing if applied elsewhere or left in use
             * - for example, to tilt the horizon permanently.
             * <p/>
             * For consistency, the feature works everywhere within the 360 content, not just
             * near nadir or zenith. The reasoning is that if user has found the gesture and
             * rolled the view to one direction, she has already learned to operate it and
             * can easily roll the view back and forth as she pleases. Furthermore, if
             * auto-horizon aligner is enabled, it will fix tilted horizon automatically.
             */

            // Set default camera zoom level and maximum zoom level.
            mCamera.setZoom(1.0f);
            mCamera.setZoomMax(3.0f);

            // Create pinch-to-zoom/pinch-to-rotate handler.
            mTouchPincher = new TouchPincher();
            mTouchPincher.setMinimumDistanceDp(OrionContext.getActivity(), 20);
            mTouchPincher.bindControllable(mCamera, OrionCamera.VAR_FLOAT1_ZOOM);
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

        // Start listening for sensor fusion events.
        OrionContext.getSensorFusion().registerOrientationChangeListener(this);

    }

    @Override
    public void onPause() {

        // Stop listening for sensor fusion events.
        OrionContext.getSensorFusion().unregisterOrientationChangeListener(this);

        super.onPause();
    }

    @Override
    public void onDeviceOrientationChanged(QuatF orientation) {

        /**
         * It is possible to listen to sensor fusion data (device orientation changes)
         * that are given as a quaternion rotation.
         * <p/>
         * A reasonable place to register a listener is at onResume(). Remember to also
         * unregister your listener at onPause(), and ensure that your implementation
         * of the callbacks return quickly, as sensor fusion runs at a high data rate,
         * typically about 200 Hz (depends on hardware).
         * <p/>
         * In this example, we simply print the latest orientation angles to logcat.
         * <p/>
         * Notice that the quaternion tells device's orientation, not the 3D camera
         * orientation where also touch and various compensations are included.
         */

        // Rotate front vector to the direction where the user is currently looking at.
        Vec3F lookAt = Vec3F.AXIS_FRONT.rotate(orientation);

        // Get the yaw offset with respective to the 360 image center.
        float lookAtYaw = lookAt.getYaw();

        // Get the pitch offset with respective to the 360 image center.
        float lookAtPitch = lookAt.getPitch();

        float toDegree = (float) (180.0f / Math.PI);
        Log.d(TAG, "Looking at yaw=" + lookAtYaw * toDegree + " pitch=" + lookAtPitch * toDegree);

    }

}
