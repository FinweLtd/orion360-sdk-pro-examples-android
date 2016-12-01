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

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.SimpleOrionActivity;

/**
 * An example of a minimal Orion360 image player, for playing an image file from local file system.
 * <p/>
 * Showcases all supported file system locations and access methods.
 * <p/>
 * Features:
 * <ul>
 * <li>Plays one hard-coded full spherical (360x180) equirectangular image
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Renders the image using standard rectilinear projection
 * <li>Allows navigation with touch & movement sensors (if supported by HW) as follows:
 * <ul>
 * <li>Panning (gyro or swipe)
 * <li>Zooming (pinch)
 * <li>Tilting (pinch rotate)
 * </ul>
 * <li>Auto Horizon Aligner (AHL) feature straightens the horizon</li>
 * </ul>
 */
public class MinimalImageFilePlayer extends SimpleOrionActivity {

    /** Tag for logging. */
    public static final String TAG = MinimalImageFilePlayer.class.getSimpleName();

    /** Request code for file read permission. */
    private static final int REQUEST_READ_STORAGE = 111;

    /** Test image path from private expansion files. */
    private static final String PRIVATE_EXPANSION_IMAGE_PATH =
            MainMenu.PRIVATE_EXPANSION_FILES_PATH + MainMenu.TEST_IMAGE_FILE_MQ;

    /** Test image path from private asset files. */
    private static final String PRIVATE_ASSET_IMAGE_PATH =
            MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_IMAGE_FILE_MQ;

    /** Test image path from private R.raw files. */
    private static final String PRIVATE_R_RAW_IMAGE_PATH =
            MainMenu.PRIVATE_R_RAW_FILES_PATH + MainMenu.TEST_IMAGE_FILE_LQ;

    /** Test image path from private internal files. */
    private static final String PRIVATE_INTERNAL_IMAGE_PATH =
            MainMenu.PRIVATE_INTERNAL_FILES_PATH + MainMenu.TEST_IMAGE_FILE_MQ;

    /** Test image path from private external files. */
    private static final String PRIVATE_EXTERNAL_IMAGE_PATH =
            MainMenu.PRIVATE_EXTERNAL_FILES_PATH + MainMenu.TEST_IMAGE_FILE_MQ;

    /** Test image path from public external files. */
    private static final String PUBLIC_EXTERNAL_IMAGE_PATH =
            MainMenu.PUBLIC_EXTERNAL_PICTURES_ORION_PATH + MainMenu.TEST_IMAGE_FILE_MQ;

    /** Full path to an image file to be played. */
    private String mImagePath;


	@Override
	public void onCreate(Bundle savedInstanceState) {

        // Call super class implementation FIRST to set up a simple Orion360 player configuration.
        super.onCreate(savedInstanceState);

        // Above call will fail if a valid Orion360 license file for the package name defined in
        // the application's manifest/build.gradle files cannot be found!

        // Set layout.
		setContentView(R.layout.activity_image_player);

        // Set Orion360 view (defined in the layout) that will be used for rendering 360 content.
        setOrionView(R.id.orion_view);

        // Try different locations by commenting out all but one from below:
        String image;

        // Private asset folder allows playing content embedded to the apps's own
        // installation package (.apk) (notice 100MB apk size limit in Google Play store).
        image = PRIVATE_ASSET_IMAGE_PATH;

        // Private raw resource folder allows playing content embedded to the app's own
        // installation package (.apk) (notice 100MB apk size limit in Google Play).
        // Use lowercase characters in filename, and access it without extension!
        // NOTE: Playing images from R.raw is currently NOT supported - without filename
        // extension we assume URI points to a video file, and MediaPlayer does not decode images.
        //image = PRIVATE_R_RAW_IMAGE_PATH;

        // Private internal folder is useful mainly when the app downloads an image file,
        // as only the app itself can access that location (exception: rooted devices).
        //image = PRIVATE_INTERNAL_IMAGE_PATH;

        // Private external folder allows copying images via file manager app or a
        // USB cable, which can be useful for users who know their way in the file
        // system and the package name of the app (e.g. developers).
        //image = PRIVATE_EXTERNAL_IMAGE_PATH;

        // Public external folder allows easy content sharing between apps and copying
        // content from PC to a familiar location such as the /Movies folder, but image
        // playback requires READ_EXTERNAL_STORAGE permission.
        //image = PUBLIC_EXTERNAL_IMAGE_PATH;

        // Private expansion package allows playing content embedded to the app's
        // extra installation package (.obb) (up to 2 GB per package, max 2 packages).
        // NOTE: Playing images from expansion package is currently NOT supported! This feature
        // will be added later.
        //image = PRIVATE_EXPANSION_IMAGE_PATH;

        // Show the selected image file.
        showImage(image);
    }

    /**
     * Show the given image file.
     *
     * @param path The full path to an image file to be showed.
     */
    public void showImage(String path) {

        // Keep a reference to the current image path.
        mImagePath = path;

        // When accessing paths on the external media, we should first check if it is currently
        // mounted or not (though, it is often built-in non-removable memory nowadays).
        if (path.equalsIgnoreCase(PRIVATE_EXTERNAL_IMAGE_PATH)
                || path.equalsIgnoreCase(PUBLIC_EXTERNAL_IMAGE_PATH)
                || path.equalsIgnoreCase(PRIVATE_EXPANSION_IMAGE_PATH)) {

            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Toast.makeText(this, R.string.player_media_not_mounted,
                        Toast.LENGTH_LONG).show();
                return;
            }

        }

        // In case we want to access images in public external folder on Android 6.0 or above,
        // we must ensure that READ_EXTERNAL_STORAGE permission is granted *before* attempting
        // to play the files in that location.
        if (path.equalsIgnoreCase(PUBLIC_EXTERNAL_IMAGE_PATH)) {

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

        // We can now show the image file.
        setContentUri(path);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String [] permissions,
                                           @NonNull int [] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_READ_STORAGE: {

                // User has now answered to our read permission request. Let's see how:
                if (grantResults.length == 0 || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Read permission was denied by user");

                    // Bail out with a notification for user.
                    Toast.makeText(this, R.string.player_read_permission_denied,
                            Toast.LENGTH_LONG).show();
                } else {
                    Log.i(TAG, "Read permission was granted by user");

                    // Public external folder works, start preparing the image file.
                    setContentUri(mImagePath);
                }
                return;
            }
            default:
                break;
        }
    }
}
