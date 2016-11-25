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

package fi.finwe.orion360.sdk.pro.examples.minimal;

import android.app.Activity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;

import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.SimpleOrionActivity;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.source.OrionVideoTexture;

/**
 * An example of a minimal Orion360 video player, with minimal video controls.
 * <p/>
 * This example uses Android MediaController as a simple way to implement media controls.
 * For more advanced solution, see CustomControls example.
 * <p/>
 * When seeking, notice that it is only possible to seek to video keyframes. The number of
 * keyframes and their positions depends on video content, video encoder, and encoder settings.
 * The Orion360 example video has very few keyframes, since it is mostly stationary.
 *
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
public class MinimalVideoControls extends SimpleOrionActivity {

    /** Media controller. */
    private MediaController mMediaController;

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

        // Initialize Orion360 video view with a URI to an .mp4 video file.
        setContentUri(MainMenu.PRIVATE_EXTERNAL_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);

        // Create a media controller.
        mMediaController = new MediaController(this);

        // Set Orion360 video texture as media player; media controller interacts with it.
        mMediaController.setMediaPlayer(
                ((OrionVideoTexture)getOrionTexture()).getMediaPlayerControl());

        // Set Orion360 view as anchor view; media controller positions itself on top of it.
        mMediaController.setAnchorView(getOrionView());

        // Propagate all touch events from the Orion view to a gesture detector.
        getOrionView().setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }

        });

        // Toggle media controls by tapping the screen.
        mGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {

                        if (mMediaController.isShowing()) {
                            mMediaController.hide();
                        } else {
                            mMediaController.show();
                        }

                        return true;
                    }

                });

	}

    @Override
    public void onVideoPrepared(OrionVideoTexture orionVideoTexture) {

        // Make the controls visible when we have a video prepared.
        mMediaController.show();

    }

}
