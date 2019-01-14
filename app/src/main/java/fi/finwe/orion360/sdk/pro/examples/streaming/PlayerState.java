/*
 * Copyright (c) 2019, Finwe Ltd. All rights reserved.
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

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;

import fi.finwe.math.Vec2f;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.controllable.DisplayClickable;
import fi.finwe.orion360.sdk.pro.controller.TouchDisplayClickListener;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.source.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;

/**
 * An example of observing Orion360 video player's state while streaming video from network.
 * <p>
 * Features:
 * <ul>
 * <li>Plays one hard-coded full spherical (360x180) equirectangular video
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Auto-starts playback on load and stops when playback is completed
 * <li>Renders the video using standard rectilinear projection
 * <li>Allows navigation with touch & movement sensors (if supported by HW) as follows:
 * <ul>
 * <li>Panning (gyro)
 * </ul>
 * </ul>
 */
public class PlayerState extends OrionActivity implements OrionVideoTexture.Listener {

    /** Tag for logging. */
    public static final String TAG = PlayerState.class.getSimpleName();

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

    /** Buffering indicator. */
    protected ProgressBar mBufferingIndicator;

    /** Media controller. */
    private MediaController mMediaController;

    /** Message log. */
    protected TextView mMessageLog;

    /** Start time (timestamp). */
    protected long mStartTime;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Use an XML layout with Orion view and a text view as a console for output.
		setContentView(R.layout.activity_video_player_with_console);

        // Get buffering indicator.
        mBufferingIndicator = (ProgressBar) findViewById(R.id.buffering_indicator);

        // Get message log.
        mMessageLog = (TextView) findViewById(R.id.message_log);
        mMessageLog.setMovementMethod(new ScrollingMovementMethod());

        // Configure Orion360.
        initOrion();

        // Set up controls etc. if playing video.
        if (mPanoramaTexture instanceof OrionVideoTexture) {

            // Create a media controller.
            mMediaController = new MediaController(this);

            // Set Orion360 video texture as media player.
            mMediaController.setMediaPlayer(
                    ((OrionVideoTexture)mPanoramaTexture).getMediaPlayerControl());

            // Set Orion360 view as anchor view (media controller positions itself on top of anchor).
            mMediaController.setAnchorView(mView);

            // By default, controls disappear after 3 seconds. Show again when view is clicked.
            TouchDisplayClickListener listener = new TouchDisplayClickListener();
            listener.bindClickable(null, new TouchDisplayClickListener.Listener() {

                @Override
                public void onDisplayClick(DisplayClickable clickable, Vec2f displayCoords) {
                    runOnUiThread (new Thread(new Runnable() {
                        public void run() {

                            // Show controls.
                            mMediaController.show();

                        }
                    }));
                }

                @Override
                public void onDisplayDoubleClick(DisplayClickable clickable, Vec2f displayCoords) {}

                @Override
                public void onDisplayLongClick(DisplayClickable clickable, Vec2f displayCoords) {}

            });

            // Bind click listener to the scene to make it functional.
            mScene.bindController(listener);
        }

        // Configure how often video position changes are notified to us.
        // If the value is small, updates will quickly fill the message log!
        if (mPanoramaTexture instanceof OrionVideoTexture) {
            mPanoramaTexture.setPositionUpdateTimeout(10000);
        }

        // Remember time when the player was created.
        mStartTime = System.currentTimeMillis();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Start listening to video texture events.
        if (mPanoramaTexture instanceof OrionVideoTexture) {
            ((OrionVideoTexture) mPanoramaTexture).addTextureListener(this);
        }
    }

    @Override
    public void onPause() {
        // Stop listening to video texture events.
        if (mPanoramaTexture instanceof OrionVideoTexture) {
            ((OrionVideoTexture) mPanoramaTexture).removeTextureListener(this);
        }

        super.onPause();
    }

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
                //MainMenu.TEST_VIDEO_URI_HLS);
                MainMenu.TEST_VIDEO_URI_1280x640, OrionTexture.PlaybackState.PAUSED);

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
     * Show buffering indicator.
     *
     * Also useful for toggling between normal and VR mode indicators.
     */
    protected void showBufferingIndicator() {
        mBufferingIndicator.setVisibility(View.VISIBLE);
    }

    /**
     * Hide buffering indicator.
     */
    protected void hideBufferingIndicator() {
        mBufferingIndicator.setVisibility(View.GONE);
    }

    /**
     * Log a message to message log text view, with a time stamp.
     *
     * @param message the message to be logged.
     */
    protected void logMessage(String message) {
        mMessageLog.append("\n" + (System.currentTimeMillis() - mStartTime) + ": " + message);
    }

    // From OrionVideoTexture.Listener:

    @Override
    public void onInvalidURI(OrionTexture orionTexture) {
        logMessage("onInvalidURI");

        // If the set video stream URI was invalid, we can't play it. Hide indicator.
        hideBufferingIndicator();

    }

    @Override
    public void onException(OrionTexture orionTexture, Exception e) {
        logMessage("onException: " + e.toString());
    }

    @Override
    public void onVideoPlayerCreated(OrionVideoTexture orionVideoTexture) {
        logMessage("onVideoPlayerCreated");
    }

    @Override
    public void onVideoSourceURISet(OrionVideoTexture orionVideoTexture) {
        logMessage("onVideoSourceURISet");

        // Assume buffering is needed when a new video stream URI is set. Show indicator.
        showBufferingIndicator();

    }

    @Override
    public void onVideoBufferingStart(OrionVideoTexture orionVideoTexture) {
        logMessage("onVideoBufferingStart");

        // Video player tells it has started buffering. Show indicator.
        showBufferingIndicator();

    }

    @Override
    public void onVideoBufferingEnd(OrionVideoTexture orionVideoTexture) {
        logMessage("onVideoBufferingEnd");

        // Video player tells it has stopped buffering. Hide indicator.
        hideBufferingIndicator();

    }

    @Override
    public void onVideoBufferingUpdate(OrionVideoTexture orionVideoTexture,
                                       int fromPercent, int toPercent) {
        logMessage("onVideoBufferingUpdate: " + fromPercent + " -> " + toPercent);
    }

    @Override
    public void onVideoPrepared(OrionVideoTexture orionVideoTexture) {
        logMessage("onVideoPrepared");

        // Show controls.
        if (null != mMediaController) {
            mMediaController.show();
        }
    }

    @Override
    public void onVideoRenderingStart(OrionVideoTexture orionVideoTexture) {
        logMessage("onVideoRenderingStart");
    }

    @Override
    public void onVideoStarted(OrionVideoTexture orionVideoTexture) {
        logMessage("onVideoStarted");
    }

    @Override
    public void onVideoPaused(OrionVideoTexture orionVideoTexture) {
        logMessage("onVideoPaused");
    }

    @Override
    public void onVideoStopped(OrionVideoTexture orionVideoTexture) {
        logMessage("onVideoStopped");
    }

    @Override
    public void onVideoCompleted(OrionVideoTexture orionVideoTexture) {
        logMessage("onVideoCompleted");
    }

    @Override
    public void onVideoSeekStarted(OrionVideoTexture orionVideoTexture, long positionMs) {
        logMessage("onVideoSeekStarted: " + positionMs);
    }

    @Override
    public void onVideoSeekCompleted(OrionVideoTexture orionVideoTexture, long positionMs) {
        logMessage("onVideoSeekCompleted: " + positionMs);
    }

    @Override
    public void onVideoPositionChanged(OrionVideoTexture orionVideoTexture, long positionMs) {
        logMessage("onVideoPositionChanged: " + positionMs);
    }

    @Override
    public void onVideoDurationUpdate(OrionVideoTexture orionVideoTexture, long durationMs) {
        logMessage("onVideoDurationUpdate: " + durationMs);
    }

    @Override
    public void onVideoSizeChanged(OrionVideoTexture orionVideoTexture, int width, int height) {
        logMessage("onVideoSizeChanged: " + width + " " + height);
    }

    @Override
    public void onVideoError(OrionVideoTexture orionVideoTexture, int what, int extra) {
        logMessage("onVideoError: " + what + " " + extra);
    }

    @Override
    public void onVideoInfo(OrionVideoTexture orionVideoTexture, int what, String message) {
        logMessage("onVideoInfo: " + what + " " + message);
    }
}
