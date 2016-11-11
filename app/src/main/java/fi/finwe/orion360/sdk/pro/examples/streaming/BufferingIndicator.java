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

package fi.finwe.orion360.sdk.pro.examples.streaming;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.v3.SimpleOrionActivity;
import fi.finwe.orion360.v3.source.OrionTexture;
import fi.finwe.orion360.v3.source.OrionVideoTexture;

/**
 * An example of a minimal Orion360 video player, with a buffering indicator.
 * <p/>
 * A buffering indicator tells end user that the video player is currently loading
 * content and should start/continue soon. This example shows some tips on how to
 * implement it properly.
 * <p/>
 * To show and hide a buffering indicator when buffering occurs during video playback,
 * set OrionVideoTexture.Listener and show/hide an indeterminate progress bar accordingly.
 * <p/>
 * It can take a long time before media player backend reports that buffering has started.
 * Hence, it is a good idea to show the buffering indicator immediately after changing
 * the content URI for the video texture.
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
 *
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
public class BufferingIndicator extends SimpleOrionActivity {

    /** Tag for logging. */
    public static final String TAG = BufferingIndicator.class.getSimpleName();

    /** Buffering indicator, for normal mode. */
    private ProgressBar mBufferingIndicator;

    /** Buffering indicator, for VR mode. */
    private LinearLayout mBufferingIndicatorVR;

    /** Handler for buffering indicators. */
    private Handler mBufferingIndicatorHandler;

    /** Polling interval for buffering indicator handler, in ms. */
    int mBufferingIndicatorInterval = 500;

    /** Gesture detector for tapping events. */
    private GestureDetector mGestureDetector;


    @Override
	public void onCreate(Bundle savedInstanceState) {

        // Call super class implementation FIRST to set up a simple Orion360 player configuration.
        super.onCreate(savedInstanceState);

        // Above call will fail if a valid Orion360 license file for the package name defined in
        // the application's manifest/build.gradle files cannot be found!

        // Set layout.
		setContentView(R.layout.activity_video_player);

        // Set Orion360 view (defined in the layout) that will be used for rendering 360 content.
        setOrionView(R.id.orion_view);

        // Get buffering indicator and setup a handler for it.
        mBufferingIndicator = (ProgressBar) findViewById(R.id.buffering_indicator);
        mBufferingIndicatorVR = (LinearLayout) findViewById(R.id.buffering_indicator_vr);
        mBufferingIndicatorHandler = new Handler();

        // Initialize Orion360 video view with a URI to an .mp4 video-on-demand stream.
        setContentUri(MainMenu.TEST_VIDEO_URI_1280x640);

        // Don't wait for 'buffering started' event; show buffering indicator right away.
        showBufferingIndicator();

        // Propagate all touch events from the video view to a gesture detector.
        getOrionView().setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }

        });

        // Toggle VR mode with long tapping.
        mGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {

                        // Notify user how to enter/exit VR mode with long press.
                        String message;
                        if (!isVRModeEnabled()) {
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
                        if (!isVRModeEnabled()) {
                            setVRMode(true);
                        } else {
                            setVRMode(false);
                        }

                        // Update buffering indicator type (normal or VR mode).
                        if (mBufferingIndicator.getVisibility() == View.VISIBLE ||
                                mBufferingIndicatorVR.getVisibility() == View.VISIBLE) {
                            showBufferingIndicator();
                        }

                    }

                });
    }

	@Override
	public void onResume() {
		super.onResume();

        // We will automatically start/continue video playback when resumed,
        // hence we make also the buffering indicator visible now.
        showBufferingIndicator();

        // Hide buffering indicator when playback starts, even if device doesn't
        // properly notify that buffering has ended.
        mBufferingIndicatorRunnable.run();
    }

	@Override
	public void onPause() {
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
            int newPosition = (int)((OrionVideoTexture)getOrionTexture()).getCurrentPosition();
            if (newPosition > 0) {
                Log.d(TAG, "Now playing video.");
                hideBufferingIndicator();
            } else if (((OrionVideoTexture)getOrionTexture()).getActualPlaybackState()
                    != OrionTexture.PlaybackState.PLAYING){
                Log.d(TAG, "Still buffering.");
                mBufferingIndicatorHandler.postDelayed(mBufferingIndicatorRunnable,
                        mBufferingIndicatorInterval);
            }
        }
    };

    /**
     * Show buffering indicator, or toggle between normal and VR mode indicator.
     */
    private void showBufferingIndicator() {
        if (isVRModeEnabled()) {
            mBufferingIndicatorVR.setVisibility(View.VISIBLE);
            mBufferingIndicator.setVisibility(View.GONE);
        } else {
            mBufferingIndicatorVR.setVisibility(View.GONE);
            mBufferingIndicator.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hide buffering indicator.
     */
    private void hideBufferingIndicator() {
        mBufferingIndicatorVR.setVisibility(View.GONE);
        mBufferingIndicator.setVisibility(View.GONE);
    }

    @Override
    public void onVideoBufferingStart(OrionVideoTexture orionVideoTexture) {
        showBufferingIndicator();
    }

    @Override
    public void onVideoBufferingEnd(OrionVideoTexture orionVideoTexture) {
        hideBufferingIndicator();
    }

    @Override
    public void onVideoBufferingUpdate(OrionVideoTexture orionVideoTexture, int percentage, int i1) {
        Log.v(TAG, "Buffer: " + percentage + "%");
    }

}
