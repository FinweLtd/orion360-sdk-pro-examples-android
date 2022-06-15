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

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.SimpleOrionActivity;

/**
 * An example of a minimal Orion360 video player, for playing a video file from local file system.
 * <p/>
 * Showcases all supported file system locations and access methods (select one from code).
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
@SuppressWarnings("unused")
public class MinimalVideoFilePlayer extends SimpleOrionActivity {

    /** Tag for logging. */
    public static final String TAG = MinimalVideoFilePlayer.class.getSimpleName();

    /** Request code for file read permission. */
    private static final int REQUEST_READ_STORAGE = 111;

    /** Test video path from private expansion files. */
    private static final String PRIVATE_EXPANSION_VIDEO_PATH =
            MainMenu.PRIVATE_EXPANSION_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ;

    /** Test video path from private asset files. */
    private static final String PRIVATE_ASSET_VIDEO_PATH =
            MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ;

    /** Test video path from private R.raw files. */
    private static final String PRIVATE_R_RAW_VIDEO_PATH =
            MainMenu.PRIVATE_R_RAW_FILES_PATH + MainMenu.TEST_VIDEO_FILE_LQ;

    /** Test video path from private internal files. */
    private static final String PRIVATE_INTERNAL_VIDEO_PATH =
            MainMenu.PRIVATE_INTERNAL_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ;

    /** Test video path from private external files. */
    private static final String PRIVATE_EXTERNAL_VIDEO_PATH =
            MainMenu.PRIVATE_EXTERNAL_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ;

    /** Test video path from public external files. */
    private static final String PUBLIC_EXTERNAL_VIDEO_PATH =
            MainMenu.PUBLIC_EXTERNAL_MOVIES_ORION_PATH + MainMenu.TEST_VIDEO_FILE_MQ;

    /** Full path to a video file to be played. */
    private String mVideoPath;


	@Override
	public void onCreate(Bundle savedInstanceState) {

        // Call super class implementation FIRST to set up a simple Orion360 player configuration.
        super.onCreate(savedInstanceState);

        // Above call will fail if a valid Orion360 license file for the package name defined in
        // the application's manifest/build.gradle files cannot be found!

        // Set layout.
		setContentView(R.layout.activity_video_player);

        // Set Orion360 view (defined in the layout) that will be used for rendering 360 content.
        setOrionView(R.id.orion_view_container);

        // Try different locations by commenting out all but one from below:
        String video;

        // Private asset folder allows playing content embedded to the app's own
        // installation package (.apk) (notice 100/150MB apk size limit in Google Play store).
        // This location should always work, independent of granted permissions.
        video = PRIVATE_ASSET_VIDEO_PATH;

        // Private raw resource folder allows playing content embedded to the app's own
        // installation package (.apk) (notice 100/150MB apk size limit in Google Play).
        // Use lowercase characters in filenames and access them without extension!
        // We recommend using assets instead of R.raw.
//        video = PRIVATE_R_RAW_VIDEO_PATH;

        // Private internal folder is useful mainly when the app downloads a protected video file,
        // as only the app itself can access that location (exception: rooted devices).
        // This location should always work, independent of granted permissions.
        // NOTE: Many devices have quite limited memory space for this path.
//        video = PRIVATE_INTERNAL_VIDEO_PATH;

        // Private external folder allows copying videos via file manager app or a
        // USB cable, which can be useful for users who know their way in the file
        // system and the package name of the app (e.g. developers).
        // This location works independent of granted permissions, but on some devices
        // this memory can be unmounted and thus might not be present at all.
//        video = PRIVATE_EXTERNAL_VIDEO_PATH;

        // Public external folder allows easy content sharing between apps and copying
        // content from PC to a familiar location such as the /Movies folder, but video
        // playback requires READ_EXTERNAL_STORAGE permission on Android 6.0 or above.
//        video = PUBLIC_EXTERNAL_VIDEO_PATH;

        // Private expansion package allows playing content embedded to the app's
        // extra installation package (.obb) (up to 2 GB per package, max 2 packages).
        // NOTE: Starting from Android 11, WRITE_EXTERNAL_STORAGE has no function. As a
        // consequence, this test app cannot create an example .obb file to the correct
        // file path. It will create it to /Android/data/fi.finwe.orion360.sdk.pro.examples
        //  /files/Android/obb/fi.finwe.orion360.sdk.pro.examples, and you should manually
        // copy it to /Android/obb/fi.finwe.orion360.sdk.pro.examples to make it work:
        // 1. Install e.g. X-plore File Manager from the Play store.
        // 2. Allow allow access to all files & permission to change system settings.
        // 3. Copy .obb file as explained above.
//        video = PRIVATE_EXPANSION_VIDEO_PATH;

        // Play the selected video file.
        playVideo(video);
    }

    /**
     * Play the given video file.
     *
     * @param path The full path to a video file to be played.
     */
    public void playVideo(String path) {

        // Keep a reference to the current video path.
        mVideoPath = path;

        // When accessing paths on the external media, we should first check if it is currently
        // mounted or not (though, it is often built-in non-removable memory nowadays).
        if (path.equalsIgnoreCase(PRIVATE_EXTERNAL_VIDEO_PATH)
                || path.equalsIgnoreCase(PUBLIC_EXTERNAL_VIDEO_PATH)
                || path.equalsIgnoreCase(PRIVATE_EXPANSION_VIDEO_PATH)) {

            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Toast.makeText(this, R.string.player_media_not_mounted,
                        Toast.LENGTH_LONG).show();
                return;
            }

        }

        // In case we want to access videos in public external folder on Android 6.0 or above,
        // we must ensure that READ_EXTERNAL_STORAGE permission is granted *before* attempting
        // to play the files in that location.
        if (path.equalsIgnoreCase(PUBLIC_EXTERNAL_VIDEO_PATH)) {

            if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest
                    .permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                // Read permission has not been granted. As user can give the permission when
                // requested, the operation now becomes asynchronous: we must wait for
                // user's decision, and act when we receive a callback.
                ActivityCompat.requestPermissions(this, new String [] {
                        Manifest.permission.READ_EXTERNAL_STORAGE }, REQUEST_READ_STORAGE);
                return;

            }

        }

        // We can now play the video file.
        setContentUri(mVideoPath);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String [] permissions,
                                           @NonNull int [] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_READ_STORAGE) {
            // User has now answered to our read permission request. Let's see how:
            if (grantResults.length == 0 || grantResults[0] !=
                    PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Read permission was denied by user");

                // Bail out with a notification for user.
                Toast.makeText(this, R.string.player_read_permission_denied,
                        Toast.LENGTH_LONG).show();
            } else {
                Log.i(TAG, "Read permission was granted by user");

                // Public external folder works, play the video file.
                setContentUri(mVideoPath);
            }
        }
    }
}
