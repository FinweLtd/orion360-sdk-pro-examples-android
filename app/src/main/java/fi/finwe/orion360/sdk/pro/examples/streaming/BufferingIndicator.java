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

package fi.finwe.orion360.sdk.pro.examples.streaming;

import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.source.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.viewport.fx.BarrelDistortion;

/**
 * An example of a buffering indicator for a streaming Orion360 video player.
 * <p/>
 * A buffering indicator tells end-user that the video player is currently loading
 * content and should start/continue soon. This example shows some tips on how to
 * implement it properly.
 * <p/>
 * To show and hide a buffering indicator when buffering occurs during video playback,
 * listen to buffering events by implementing OrionVideoTexture.Listener interface,
 * and show/hide an indeterminate progress bar accordingly.
 * <p/>
 * It can take a long time before Android MediaPlayer reports that buffering has started.
 * Hence, it is a good idea to show the buffering indicator immediately after setting
 * (or changing) a content URI to an OrionTexture object.
 * <p/>
 * Since the activity can get paused and resumed at any time, and the video playback is
 * usually auto-started when the player activity is resumed, it is often simplest
 * to show the buffering indicator in onResume().
 * <p/>
 * Some Android devices have a buggy implementation of buffering events and the 'buffering
 * stopped' event might never come in the case we are buffering the very beginning
 * of the video. To prevent buffering indicator for staying on screen forever, you can use a
 * handler that polls when the video playback has progressed and ensure that buffering
 * indicator gets removed.
 * <p/>
 * In VR mode, both eyes need a separate buffering indicator. Simple implementation is to
 * have both normal and VR mode indicators configured in the layout, and select which one
 * to use by toggling their visibilities. Remember to update the indicators when user
 * toggles between normal and VR mode.
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
public class BufferingIndicator extends OrionActivity implements OrionVideoTexture.Listener {

    /** Tag for logging. */
    public static final String TAG = BufferingIndicator.class.getSimpleName();

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

    /** Buffering indicator for normal mode. */
    private ProgressBar mBufferingIndicatorNormal;

    /** Buffering indicator for VR mode. */
    private LinearLayout mBufferingIndicatorVR;

    /** Handler for buffering indicators. */
    private Handler mBufferingIndicatorHandler;

    /** Polling interval for buffering indicator handler, in ms. */
    int mBufferingIndicatorIntervalMs = 500;

    /** Gesture detector for tapping events (used for toggling between normal and VR modes). */
    private GestureDetector mGestureDetector;

    /** Flag for indicating if VR mode is currently enabled, or not. */
    private boolean mIsVRModeEnabled = false;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Use an XML layout where you can stack components on top of each other, such as
        // a FrameLayout. Add OrionView, and then put a centered ProgressBar on top of it
        // to act as a buffering indicator for normal mode. For VR mode you can simply
        // use two ProgressBars; first divide the layout horizontally to left and right
        // eye parts, then center the ProgressBars within their own halves.
		setContentView(R.layout.activity_video_player);

        // Get buffering indicators and setup a handler for them.
        mBufferingIndicatorNormal = (ProgressBar) findViewById(R.id.buffering_indicator);
        mBufferingIndicatorVR = (LinearLayout) findViewById(R.id.buffering_indicator_vr);
        mBufferingIndicatorHandler = new Handler();

        // Configure Orion360.
        initOrion();

        // The user should always have an easy-to-find method for returning from VR mode to
        // normal mode. Here we use touch events, as it is natural to try tapping the screen
        // if you don't know what else to do. Start by propagating touch events from the
        // Orion360 view to a gesture detector.
        mView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }

        });

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
                        Toast.makeText(BufferingIndicator.this, message,
                                Toast.LENGTH_SHORT).show();

                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {

                        // Enter or exit VR mode.
                        if (!mIsVRModeEnabled) {
                            setVRMode(true);
                        } else {
                            setVRMode(false);
                        }

                    }

                });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't wait for 'buffering started' event; show buffering indicator right away.
        showBufferingIndicator();

        // Start listening to video texture events.
        if (mPanoramaTexture instanceof OrionVideoTexture) {
            ((OrionVideoTexture) mPanoramaTexture).addTextureListener(this);
        }

        // Hide buffering indicator when playback starts, even if device doesn't
        // properly notify that buffering has ended...
        mBufferingIndicatorRunnable.run();
    }

    @Override
    public void onPause() {
        // Stop listening to video texture events.
        if (mPanoramaTexture instanceof OrionVideoTexture) {
            ((OrionVideoTexture) mPanoramaTexture).removeTextureListener(this);
        }

        // Cancel buffering indicator handler (polling).
        mBufferingIndicatorHandler.removeCallbacks(mBufferingIndicatorRunnable);

        super.onPause();
    }

    /**
     * Runnable for polling if video playback has already begun, and to hide buffering indicator.
     */
    Runnable mBufferingIndicatorRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Checking if playback has started...");
            long newPosition = mPanoramaTexture.getCurrentPosition();
            if (newPosition > 0) {
                Log.d(TAG, "Now playing video.");
                hideBufferingIndicator();
            } else {
                Log.d(TAG, "Still buffering.");
                mBufferingIndicatorHandler.postDelayed(mBufferingIndicatorRunnable,
                        mBufferingIndicatorIntervalMs);
            }
        }
    };

    /**
     * Initialize Orion360 as a typical mono panorama player. Touch controls omitted.
     */
    protected void initOrion() {

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene();

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindController(mOrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanorama = new OrionPanorama();

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = OrionTexture.createTextureFromURI(this,
                MainMenu.TEST_VIDEO_URI_1280x640);

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
            mPanorama.bindTextureVR(0, mPanoramaTexture, new RectF(-180, 90, 180, -90),
                    OrionPanorama.TEXTURE_RECT_FULL, OrionPanorama.TEXTURE_RECT_FULL);

            // Set up two new viewports side by side (when looked from landscape orientation).
            mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_SPLIT_HORIZONTAL,
                    OrionViewport.CoordinateType.FIXED_LANDSCAPE);

            // Designate each viewport to render content for either left or right eye.
            mView.getViewports()[0].setVRMode(OrionViewport.VRMode.VR_LEFT);
            mView.getViewports()[1].setVRMode(OrionViewport.VRMode.VR_RIGHT);

            // Compensate for VR frame lens distortion using barrel distortion FX.
            BarrelDistortion barrelFx = new BarrelDistortion();
            barrelFx.setDistortionFillScale(1.0f);
            barrelFx.setDistortionCenterOffset(0, 0);
            barrelFx.setDistortionCoeffs(new float[] { 1.0f, 0.39f, -0.35f, 0.19f} );
            mView.getViewports()[0].bindFX(barrelFx);
            mView.getViewports()[1].bindFX(barrelFx);

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
            mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL,
                    OrionViewport.CoordinateType.FIXED_LANDSCAPE);

            // Re-configure camera.
            mCamera.setZoom(1.0f);
            mCamera.setZoomMax(3.0f);

            // Show the navigation bar again.
            showNavigationBar();
        }

        // Store new mode.
        mIsVRModeEnabled = enabled;

        // Update buffering indicator type (normal or VR mode), if it is currently visible.
        if (mBufferingIndicatorNormal.getVisibility() == View.VISIBLE ||
                mBufferingIndicatorVR.getVisibility() == View.VISIBLE) {
            showBufferingIndicator();
        }
    }

    /**
     * Show buffering indicator.
     *
     * Also useful for toggling between normal and VR mode indicators.
     */
    protected void showBufferingIndicator() {
        if (mIsVRModeEnabled) {
            mBufferingIndicatorNormal.setVisibility(View.GONE);
            mBufferingIndicatorVR.setVisibility(View.VISIBLE);
        } else {
            mBufferingIndicatorNormal.setVisibility(View.VISIBLE);
            mBufferingIndicatorVR.setVisibility(View.GONE);
        }
    }

    /**
     * Hide buffering indicator.
     */
    protected void hideBufferingIndicator() {
        mBufferingIndicatorNormal.setVisibility(View.GONE);
        mBufferingIndicatorVR.setVisibility(View.GONE);
    }

    /**
     * Show navigation bar.
     */
    protected void showNavigationBar() {
        View v = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT < 19) {
            v.setSystemUiVisibility(View.VISIBLE);
        } else {
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            v.setSystemUiVisibility(uiOptions);
        }
    }

    /**
     * Hide navigation bar.
     */
    protected void hideNavigationBar() {
        View v = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT < 19) {
            v.setSystemUiVisibility(View.GONE);
        } else {
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            v.setSystemUiVisibility(uiOptions);
        }
    }

    // From OrionVideoTexture.Listener:

    @Override
    public void onSourceURIChanged(OrionTexture orionTexture) {

        // Assume buffering is needed when a new video stream URI is set. Show indicator.
        showBufferingIndicator();

    }

    @Override
    public void onInvalidURI(OrionTexture orionTexture) {

        // If the set video stream URI was invalid, we can't play it. Hide indicator.
        hideBufferingIndicator();

    }

    @Override
    public void onVideoBufferingStart(OrionVideoTexture orionVideoTexture) {

        // Video player tells it has started buffering. Show indicator.
        showBufferingIndicator();

    }

    @Override
    public void onVideoBufferingEnd(OrionVideoTexture orionVideoTexture) {

        // Video player tells it has stopped buffering. Hide indicator.
        hideBufferingIndicator();

    }

    @Override
    public void onVideoBufferingUpdate(OrionVideoTexture orionVideoTexture,
                                       int fromPercent, int toPercent) {

        // Video player tells its buffer fill status has changed. Print to log.
        Log.v(TAG, "Video buffer updated: " + fromPercent + "% -> " + toPercent + "%");

    }

    @Override
    public void onVideoPrepared(OrionVideoTexture orionVideoTexture) {}

    @Override
    public void onVideoRenderingStart(OrionVideoTexture orionVideoTexture) {}

    @Override
    public void onVideoStarted(OrionVideoTexture orionVideoTexture) {}

    @Override
    public void onVideoPaused(OrionVideoTexture orionVideoTexture) {}

    @Override
    public void onVideoStopped(OrionVideoTexture orionVideoTexture) {}

    @Override
    public void onVideoCompleted(OrionVideoTexture orionVideoTexture) {}

    @Override
    public void onVideoSeekStarted(OrionVideoTexture orionVideoTexture, long l) {}

    @Override
    public void onVideoSeekCompleted(OrionVideoTexture orionVideoTexture, long l) {}

    @Override
    public void onVideoPositionChanged(OrionVideoTexture orionVideoTexture, long l) {}

    @Override
    public void onVideoDurationUpdate(OrionVideoTexture orionVideoTexture, long l) {}

    @Override
    public void onVideoSizeChanged(OrionVideoTexture orionVideoTexture, int i, int i1) {}

    @Override
    public void onVideoError(OrionVideoTexture orionVideoTexture, int i, int i1) {}

    @Override
    public void onVideoInfo(OrionVideoTexture orionVideoTexture, int i, String s) {}

}
