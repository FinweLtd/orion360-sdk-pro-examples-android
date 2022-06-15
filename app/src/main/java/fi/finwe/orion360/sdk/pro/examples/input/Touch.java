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

package fi.finwe.orion360.sdk.pro.examples.input;

import android.annotation.SuppressLint;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import fi.finwe.math.Vec2f;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.controllable.Raycast;
import fi.finwe.orion360.sdk.pro.controllable.RaycastHit;
import fi.finwe.orion360.sdk.pro.controllable.Raycastable;
import fi.finwe.orion360.sdk.pro.item.sprite.OrionSprite;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;
import fi.finwe.orion360.sdk.pro.viewport.OrionViewport;
import fi.finwe.orion360.sdk.pro.controller.RotationAligner;
import fi.finwe.orion360.sdk.pro.controller.TouchPincher;
import fi.finwe.orion360.sdk.pro.controller.TouchRotater;
import fi.finwe.orion360.sdk.pro.controller.TouchWorldClickListener;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.texture.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.widget.OrionWidget;
import fi.finwe.util.ContextUtil;

/**
 * An example that focuses on touch input.
 * <p/>
 * For panning, zooming and rotating the view with swipe and pinch gestures, see separate
 * Sensors example for detailed explanation.
 * <p/>
 * This example uses single tapping for toggling between normal and full screen view,
 * double tapping for toggling between video playback and pause states, and long tapping
 * for toggling between normal and VR mode rendering. These are tried-and-true mappings
 * that are recommended for all 360/VR video apps.
 * <p/>
 * To showcase tapping inside the 3D scene, a set of hotspots are added to the video view.
 * Tapping a hotspot will "collect" it i.e. remove it.
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
public class Touch extends OrionActivity {

    /** Tag for logging. */
    public static final String TAG = Touch.class.getSimpleName();

    /** The distance (from the scene origin) where hotspots will be drawn (sphere = 1.0f). */
    protected final static float HOTSPOT_LAYER_RADIUS = 0.9f;

    /** The size of the hotspots, as a scaling factor relative to image asset size (100% = 1.0f). */
    protected final static float HOTSPOT_SCALE_FACTOR = 0.21f;

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

    /** The hotspot items that can be collected by tapping. */
    protected List<OrionSprite> mHotspots;

    /** The widget that will handle our panning, zooming & rotating touch gestures. */
    protected TouchControllerWidget mTouchController;

    /** Clicks within the 3D world are handled with world click listener. */
    protected TouchWorldClickListener mWorldClickListener;

    /** Media controller. */
    private MediaController mMediaController;

    /** Gesture detector for view tapping events. */
    private GestureDetector mGestureDetector;

    /** Play indicator animation. */
    private Animation mPlayAnimation;

    /** Pause indicator animation. */
    private Animation mPauseAnimation;

    /** Play indicator overlay image. */
    private ImageView mPlayOverlay;

    /** Pause indicator overlay image. */
    private ImageView mPauseOverlay;

    /** Flag for indicating if the title bar is currently showing, or not. */
    private boolean mIsTitleBarShowing = false;

    /** Flag for indicating if the navigation bar is currently showing, or not. */
    private boolean mIsNavigationBarShowing = false;

    /** Flag for indicating if VR mode is currently enabled, or not. */
    private boolean mIsVRModeEnabled = false;


    @SuppressLint("ClickableViewAccessibility")
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Here we use an XML layout that contains 'play' and 'pause' image views
        // centered on top of the Orion360 view, for play and pause indicators.
		setContentView(R.layout.activity_video_player);

        // Initialize Orion360.
        initOrion();

        // Create a couple of hotspots.
        mHotspots = new ArrayList<>();
        mHotspots.add(createHotSpot(0.0f, 0.0f, 0.0f));
        mHotspots.add(createHotSpot(90.0f, 0.0f, 0.0f));
        mHotspots.add(createHotSpot(-90.0f, 0.0f, 0.0f));
        mHotspots.add(createHotSpot(180.0f, 0.0f, 0.0f));

        // Create a media controller.
        mMediaController = new MediaController(this);

        // Set our video texture as a media player (media controller interacts directly with it).
        mMediaController.setMediaPlayer(
                ((OrionVideoTexture)mPanoramaTexture).getMediaPlayerControl());

        // Set our OrionView as anchor view (media controller positions itself on top of anchor).
        mMediaController.setAnchorView(mViewContainer);

        // To handle tapping events from the whole Orion360 view area with a gesture detector
        // (without caring about the position that user touched), propagate all touch events
        // from the view to a gesture detector.
        mViewContainer.setOnTouchListener((v, event) -> mGestureDetector.onTouchEvent(event));

        // In the gesture detector we handle tapping, double tapping and long press events.
        // The recommended mapping goes as follows:
        // - Single tap in normal mode shows/hides other UI components (normal/fullscreen mode),
        //   and in VR mode shows a Toast hinting how to return to normal mode with a long press.
        // - Long tap in normal and VR mode toggles between VR mode and normal mode, respectively.
        // - Double tap in normal mode toggles play/pause state (use animation to indicate event!)
        mGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        Log.d(TAG, "onSingleTapConfirmed() from thread " +
                                Thread.currentThread().getId());

                        // Toggle title bar, navigation bar and media controls in normal mode.
                        // Show hint in VR mode.
                        if (!mIsVRModeEnabled) {
                            toggleTitleBar();
                            toggleNavigationBar();
                            if (mIsTitleBarShowing) {
                                showMediaControls();
                            } else {
                                hideMediaControls();
                            }
                        } else {
                            String message = getString(R.string.player_long_tap_hint_exit_vr_mode);
                            Toast.makeText(Touch.this, message, Toast.LENGTH_SHORT).show();
                        }

                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        Log.d(TAG, "onLongPress() from thread " + Thread.currentThread().getId());

                        // Enter or exit VR mode.
                        setVRMode(!mIsVRModeEnabled);

                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        Log.d(TAG, "onDoubleTap() from thread " + Thread.currentThread().getId());

                        // Toggle between play and pause states in normal mode.
                        if (!mIsVRModeEnabled) {

                            OrionVideoTexture t = (OrionVideoTexture) mPanoramaTexture;
                            if (t.getActualPlaybackState() == OrionTexture.PlaybackState.PLAYING) {
                                t.pause();
                                runPauseAnimation();
                            } else {
                                t.play();
                                runPlayAnimation();
                            }
                        }

                        return true;
                    }

                });

        // An alternative for View.OnTouchListener and GestureDetector is to use Orion360's own
        // TouchDisplayClickListener. Usually these two options should NOT be mixed together.
        //
        // The major difference is that Android's own solution will make callbacks using
        // the UI thread, while Orion360's callbacks come from the GL thread. Hence, if you
        // intend to use the click events mainly for manipulating Android UI components
        // it is recommended to use View.OnTouchListener, and if you manipulate Orion360
        // components instead then it is better to use TouchDisplayClickListener (which
        // also provides more detailed control).
        //
        // Notice that you should use runOnUiThread method for accessing Android UI components
        // from TouchDisplayClickListener to avoid crash with CalledFromWrongThreadException.
//        TouchDisplayClickListener listener = new TouchDisplayClickListener();
//        listener.bindClickable(null, new TouchDisplayClickListener.Listener() {
//
//            @Override
//            public void onDisplayClick(DisplayClickable clickable, Vec2f displayCoords) {
//                Log.d(TAG, "onDisplayClick() from thread " + Thread.currentThread().getId());
//
//                runOnUiThread (new Thread(new Runnable() {
//                    public void run() {
//                        toggleTitleBar(); // This will crash if not run on UI thread!
//                    }
//                }));
//            }
//
//            @Override
//            public void onDisplayDoubleClick(DisplayClickable clickable, Vec2f displayCoords) {
//                Log.d(TAG, "onDisplayDoubleClick() from thread " + Thread.currentThread().getId());
//            }
//
//            @Override
//            public void onDisplayLongClick(DisplayClickable clickable, Vec2f displayCoords) {
//                Log.d(TAG, "onDisplayLongClick() from thread " + Thread.currentThread().getId());
//            }
//
//        });
//
//        // It is possible to enable/disable each click event type individually.
//        // Notice that disabling double clicks improves response time.
//        mView.setTouchInputClickEnabled(Clicker.ClickType.SINGLE, true);
//        mView.setTouchInputClickEnabled(Clicker.ClickType.DOUBLE, true);
//        mView.setTouchInputClickEnabled(Clicker.ClickType.LONG, true);
//
//        // Bind click listener to the scene to make it functional.
//        mScene.bindController(listener);

        // To enable clicking items within the 3D world, we can use Orion360's own
        // TouchWorldClickListener object. Here we bind each hotspot's icon as a
        // clickable item to our world click listener. Notice that you can bind other
        // kinds of objects as well, such as sprites and polygons.
        mWorldClickListener = new TouchWorldClickListener(mOrionContext);
        for (final OrionSprite hotspot : mHotspots) {

            mWorldClickListener.bindClickable(hotspot,
                    new TouchWorldClickListener.ListenerBase() {

                        @Override
                        public void onWorldClick(Raycastable receiver, Vec2f displayCoords,
                                                 Raycast raycast, RaycastHit raycastHit) {
                            Log.d(TAG, "onWorldClick(): " + receiver.toString());
                            hotspot.setVisible(false);
                        }

                        @Override
                        public void onWorldDoubleClick(Raycastable receiver, Vec2f displayCoords,
                                                       Raycast raycast, RaycastHit raycastHit) {
                            Log.d(TAG, "onWorldDoubleClick(): " + receiver.toString());
                        }

                        @Override
                        public void onWorldLongClick(Raycastable receiver, Vec2f displayCoords,
                                                     Raycast raycast, RaycastHit raycastHit) {
                            Log.d(TAG, "onWorldLongClick(): " + receiver.toString());
                        }
                    });

        }

        // Bind the world click listener to the scene to make it functional.
        mScene.bindRoutine(mWorldClickListener);

        // Play overlay image and animation. Since the animation is not needed in VR mode and is
        // not related to viewing direction, simply use an Android image view and XML animation.
        mPlayOverlay = (ImageView) findViewById(R.id.play_overlay);
        mPlayAnimation = AnimationUtils.loadAnimation(this,
                R.anim.fast_fadeinout_animation);
        mPlayAnimation.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mPlayOverlay.setVisibility(View.GONE);
            }

        });

        // Pause overlay image and animation. Since the animation is not needed in VR mode and is
        // not related to viewing direction, simply use an Android image view and XML animation.
        mPauseOverlay = (ImageView) findViewById(R.id.pause_overlay);
        mPauseAnimation = AnimationUtils.loadAnimation(this,
                R.anim.fast_fadeinout_animation);
        mPauseAnimation.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mPauseOverlay.setVisibility(View.GONE);
            }

        });
	}

    /**
     * Configure Orion360 as a typical mono panorama video player.
     */
    protected void initOrion() {

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
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mPanorama.bindTextureFull(0, mPanoramaTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanorama);

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera(mOrionContext);

        // Reset view to the 'front' direction (horizontal center of the panorama).
        mCamera.setDefaultRotationYaw(0);

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        mOrionContext.getSensorFusion().bindControllable(mCamera);

        // Create a new touch controller widget (convenience class), and let it control our camera.
        mTouchController = new TouchControllerWidget(mCamera);

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

    /**
     * Creates a hotspot at the direction given as Euler angles.
     *
     * @param yawDeg The yaw angle in degrees.
     * @param pitchDeg The pitch angle in degrees.
     * @param rollDeg The roll angle in degrees.
     */
    @SuppressWarnings("SameParameterValue")
    protected OrionSprite createHotSpot(float yawDeg, float pitchDeg, float rollDeg) {

        // Create a new hotspot as an OrionSprite.
        OrionSprite hotspot = new OrionSprite(mOrionContext);

        // Set the location of the hotspot using Euler angles.
        hotspot.setWorldTransformFromPolarZXYDeg(yawDeg, pitchDeg, rollDeg,
                HOTSPOT_LAYER_RADIUS);

        // Set the size of the hotspot as a scale factor relative to image asset size.
        hotspot.setScale(HOTSPOT_SCALE_FACTOR);

        // Set the icon for the hotspot as a PNG image. */
        OrionTexture texture = OrionTexture.createTextureFromURI(mOrionContext,
                this, getString(R.string.asset_hotspot_start));
        hotspot.bindTexture(texture);

        // Adjust hotspot icon's alpha value to make it a little bit transparent.
        hotspot.getColorFx().setAmpAlpha(0.90f);

        // Bind to scene to make it part of the 3D world.
        mScene.bindSceneItem(hotspot);

        return hotspot;
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
         * @param camera The camera to be controlled by this widget.
         */
        TouchControllerWidget(OrionCamera camera) {

            // Create pinch-to-zoom/pinch-to-rotate handler.
            mTouchPincher = new TouchPincher(mOrionContext);
            mTouchPincher.setMinimumDistanceDp(mOrionContext.getActivity(), 20);
            mTouchPincher.bindControllable(camera, OrionCamera.VAR_FLOAT1_ZOOM);

            // Create drag-to-pan handler.
            mTouchRotater = new TouchRotater(mOrionContext);
            mTouchRotater.bindControllable(camera);

            // Create the rotation aligner, responsible for rotating the view so that the horizon
            // aligns with the user's real-life horizon when the user is not looking up or down.
            mRotationAligner = new RotationAligner(mOrionContext);
            mRotationAligner.setDeviceAlignZ(-ContextUtil.getDisplayRotationDegreesFromNatural(
                    mOrionContext.getActivity()));
            mRotationAligner.bindControllable(camera);

            // Rotation aligner needs sensor fusion data in order to do its job.
            mOrionContext.getSensorFusion().bindControllable(mRotationAligner);
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

    /**
     * Set VR mode enabled or disabled.
     *
     * @param enabled Set true to enable VR mode, or false to return to normal mode.
     */
    protected void setVRMode(boolean enabled) {

        // Do nothing if the desired mode is already active.
        if (enabled == mIsVRModeEnabled) return;

        // We need to bind the texture to the panorama again, so release the current binding.
        mPanorama.releaseTexture(0);

        // We need to reconfigure viewports, so release the current ones.
        OrionViewport [] viewports = mView.getViewports();
        for (OrionViewport vp : viewports) {
            mView.releaseViewport(vp);
        }

        if (enabled) {

            // Bind the complete texture to both left and right eyes. Assume full spherical mono.
            mPanorama.bindTextureVR(0, mPanoramaTexture,
                    new RectF(-180, 90, 180, -90),
                    OrionPanorama.TEXTURE_RECT_FULL, OrionPanorama.TEXTURE_RECT_FULL);

            // Set up two new viewports side by side (when looked from landscape orientation).
            mView.bindViewports(OrionDisplayViewport.VIEWPORT_CONFIG_SPLIT_HORIZONTAL,
                    OrionDisplayViewport.CoordinateType.FIXED_LANDSCAPE);

            // Designate each viewport to render content for either left or right eye.
            mView.getViewports()[0].setVRMode(OrionViewport.VRMode.VR_LEFT);
            mView.getViewports()[1].setVRMode(OrionViewport.VRMode.VR_RIGHT);

            // Re-configure camera for VR mode.
            mCamera.setVRCameraDistance(0.035f);
            mCamera.setVRCameraFocalDistance(1.5f);
            mCamera.setZoom(1.0f);
            mCamera.setZoomMax(1.0f);

            // Hide the navigation bar, else it will be visible for the right eye!
            hideNavigationBar();

            // VR is still new for many users, hence they should be educated what this feature is
            // and how to use it, e.g. an animation about putting the device inside a VR frame.
            // Here we simply show a notification.
            Toast.makeText(this, "Please put the device inside a VR frame",
                    Toast.LENGTH_LONG).show();
        } else {

            // Bind the complete texture to the panorama sphere. Assume full spherical mono.
            mPanorama.bindTextureFull(0, mPanoramaTexture);

            // Bind one new viewport to landscape orientation.
            mView.bindViewports(OrionDisplayViewport.VIEWPORT_CONFIG_FULL,
                    OrionDisplayViewport.CoordinateType.FIXED_LANDSCAPE);

            // Re-configure camera.
            mCamera.setZoom(1.0f);
            mCamera.setZoomMax(3.0f);

            // Show the navigation bar again.
            showNavigationBar();
        }

        // Store new mode.
        mIsVRModeEnabled = enabled;
    }

    /**
     * Toggle title bar.
     */
    public void toggleTitleBar() {
        if (mIsTitleBarShowing) {
            hideTitleBar();
        } else {
            showTitleBar();
        }
    }

    /**
     * Show title bar.
     */
    public void showTitleBar() {
        try {
            ((View) findViewById(android.R.id.title).getParent())
                    .setVisibility(View.VISIBLE);
        } catch (Exception e) { e.printStackTrace(); }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mIsTitleBarShowing = true;
    }

    /**
     * Hide title bar.
     */
    public void hideTitleBar() {
        try {
            ((View) findViewById(android.R.id.title).getParent())
                    .setVisibility(View.GONE);
        } catch (Exception e) { e.printStackTrace(); }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        mIsTitleBarShowing = false;
    }

    /**
     * Toggle navigation bar.
     */
    public void toggleNavigationBar() {
        if (mIsNavigationBarShowing) {
            hideNavigationBar();
        } else {
            showNavigationBar();
        }
    }

    /**
     * Show navigation bar.
     */
    public void showNavigationBar() {
        View v = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        v.setSystemUiVisibility(uiOptions);
        mIsNavigationBarShowing = true;
    }

    /**
     * Hide navigation bar.
     */
    public void hideNavigationBar() {
        View v = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        v.setSystemUiVisibility(uiOptions);
        mIsNavigationBarShowing = false;
    }

    /**
     * Show media controls.
     */
    public void showMediaControls() {
        mMediaController.show();
    }

    /**
     * Hide media controls.
     */
    public void hideMediaControls() {
        mMediaController.hide();
    }

    /**
     * Run Play animation.
     */
    public void runPlayAnimation() {
        mPlayOverlay.setVisibility(View.VISIBLE);
        mPlayOverlay.startAnimation(mPlayAnimation);
    }

    /**
     * Run Pause animation.
     */
    public void runPauseAnimation() {
        mPauseOverlay.setVisibility(View.VISIBLE);
        mPauseOverlay.startAnimation(mPauseAnimation);
    }
}
