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

package fi.finwe.orion360.sdk.pro.examples.minimal;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.SimpleOrionActivity;

/**
 * An example of a minimal Orion360 video player, with VR mode enabled.
 * <p>
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
public class MinimalVRVideoFilePlayer extends SimpleOrionActivity {

    /** Gesture detector for touch events. */
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

        // Initialize Orion360 view with a URI to a local .mp4 video file.
        setContentUri(MainMenu.PRIVATE_EXTERNAL_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);

        // Configure video view for VR mode. This will split the screen horizontally,
        // render the image separately for left and right eye, and apply lens distortion
        // compensation and field-of-view (FOV) locking to configured values.
        setVRMode(true);

        // The user should always have an easy-to-find method for returning from VR mode to
        // normal mode. Here we use touch events, as it is natural to try tapping the screen
        // if you don't know what else to do. Start by propagating touch events from the
        // Orion360 view to a gesture detector.
        getOrionView().setOnTouchListener(new View.OnTouchListener() {

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
                        if (!isVRModeEnabled()) {
                            message = getString(R.string.player_long_tap_hint_enter_vr_mode);
                        } else {
                            message = getString(R.string.player_long_tap_hint_exit_vr_mode);
                        }
                        Toast.makeText(MinimalVRVideoFilePlayer.this, message,
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

                    }

                });
	}

}
