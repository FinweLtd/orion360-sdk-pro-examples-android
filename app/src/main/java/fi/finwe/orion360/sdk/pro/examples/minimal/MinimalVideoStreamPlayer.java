///**
// * Copyright (c) 2016, Finwe Ltd. All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without modification,
// * are permitted provided that the following conditions are met:
// *
// * 1. Redistributions of source code must retain the above copyright notice, this
// *    list of conditions and the following disclaimer.
// *
// * 2. Redistributions in binary form must reproduce the above copyright notice,
// *    this list of conditions and the following disclaimer in the documentation and/or
// *    other materials provided with the distribution.
// *
// * 3. Neither the name of the copyright holder nor the names of its contributors
// *    may be used to endorse or promote products derived from this software without
// *    specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
// * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
// * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//
//package fi.finwe.orion360.sdk.pro.examples.minimal;
//
//import android.app.Activity;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.ProgressBar;
//import android.widget.Toast;
//
///**
// * An example of a minimal Orion360 video player, for streaming a video file over the network.
// * <p>
// * Features:
// * <ul>
// * <li>Plays one hard-coded full spherical (360x180) equirectangular video
// * <li>Creates a fullscreen view locked to landscape orientation
// * <li>Auto-starts playback on load and stops when playback is completed
// * <li>Renders the video using standard rectilinear projection
// * <li>Allows navigation with touch & movement sensors (if supported by HW) as follows:
// * <ul>
// * <li>Panning (gyro or swipe)
// * <li>Zooming (pinch)
// * <li>Tilting (pinch rotate)
// * </ul>
// * <li>Auto Horizon Aligner (AHL) feature straightens the horizon</li>
// * </ul>
// */
//public class MinimalVideoStreamPlayer extends Activity {
//
//    /** Tag for logging. */
//    public static final String TAG = MinimalVideoStreamPlayer.class.getSimpleName();
//
//    /** Orion360 video player view. */
//	private OrionVideoView mOrionVideoView;
//
//    /** Buffering indicator, to be shown while buffering video from the network. */
//    private ProgressBar mBufferingIndicator;
//
//
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//
//        // Set layout.
//		setContentView(R.layout.activity_video_player);
//
//        // Get Orion360 video view that is defined in the XML layout.
//        mOrionVideoView = (OrionVideoView) findViewById(R.id.orion_video_view);
//
//        // Get buffering indicator.
//        mBufferingIndicator = (ProgressBar) findViewById(R.id.buffering_indicator);
//
//        // Listen for buffering events, and show/hide the buffering indicator accordingly.
//        // For a better example, see BufferingIndicator example.
//        mOrionVideoView.setOnBufferingStatusListener(new OrionVideoView.OnBufferingStatusListener() {
//            @Override
//            public void onBufferingStarted(OrionVideoView orionVideoView) {
//                mBufferingIndicator.setVisibility(View.VISIBLE);
//            }
//
//            @Override
//            public void onBufferFillRateChanged(OrionVideoView orionVideoView, int percentage) {
//                Log.v(TAG, "Buffer: " + percentage + "%");
//            }
//
//            @Override
//            public void onBufferingStopped(OrionVideoView orionVideoView) {
//                mBufferingIndicator.setVisibility(View.GONE);
//            }
//        });
//
//        // Start playback when the player has initialized itself and buffered enough video frames.
//        mOrionVideoView.setOnPreparedListener(new OrionVideoView.OnPreparedListener() {
//            @Override
//            public void onPrepared(OrionVideoView view) {
//                mOrionVideoView.start();
//            }
//        });
//
//        // Initialize Orion360 video view with a URI to an .mp4 video-on-demand stream
//        // (encode video with web/progressive setting enabled for best performance).
//
//        // Notice that accessing video streams over a network connection requires INTERNET
//        // permission to be specified in the manifest file.
//
//        // Notice that 'Orion360 SDK Basic' does not officially support HLS, MPEG-DASH or
//        // other adaptive video streams, although some devices may be able to play them
//        // (HLS is officially supported in 'Orion360 SDK Pro' product).
//
//        // Notice that this call will fail if a valid Orion360 license file for the package name
//        // (defined in the application's manifest file) cannot be found.
//        try {
//            mOrionVideoView.prepare(MainMenu.TEST_VIDEO_URI_1280x640);
//        } catch (OrionVideoView.LicenseVerificationException e) {
//            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
//        }
//
//        // When you run the app, you may get a warning from MediaPlayer component to the LogCat:
//        // W/MediaPlayer: Couldn't open []: java.io.FileNotFoundException: No content provider: []
//        // Here Android MediaPlayer is using an exception for control flow; you can disregard it.
//	}
//
//    @Override
//    public void onStart() {
//        super.onStart();
//
//        // Propagate activity lifecycle events to Orion360 video view.
//        mOrionVideoView.onStart();
//    }
//
//	@Override
//	public void onResume() {
//		super.onResume();
//
//        // Propagate activity lifecycle events to Orion360 video view.
//		mOrionVideoView.onResume();
//	}
//
//	@Override
//	public void onPause() {
//        // Propagate activity lifecycle events to Orion360 video view.
//		mOrionVideoView.onPause();
//
//		super.onPause();
//	}
//
//	@Override
//	public void onStop() {
//        // Propagate activity lifecycle events to Orion360 video view.
//		mOrionVideoView.onStop();
//
//		super.onStop();
//	}
//
//	@Override
//	public void onDestroy() {
//        // Propagate activity lifecycle events to Orion360 video view.
//		mOrionVideoView.onDestroy();
//
//		super.onDestroy();
//	}
//}
