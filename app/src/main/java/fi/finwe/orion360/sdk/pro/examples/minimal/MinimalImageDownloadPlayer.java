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

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.util.Pair;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.SimpleOrionActivity;

import static fi.finwe.orion360.sdk.pro.examples.MainMenu.PRIVATE_EXTERNAL_FILES_PATH;
import static fi.finwe.orion360.sdk.pro.examples.MainMenu.PRIVATE_INTERNAL_FILES_PATH;

/**
 * An example of a minimal Orion360 image player, for downloading an image file before playback.
 * <p>
 * This example uses custom code for downloading a file. See MinimalVideoDownloadPlayer for an
 * example of using Android's DownloadManager service instead (recommended).
 * <p>
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
public class MinimalImageDownloadPlayer extends SimpleOrionActivity {

    /** Tag for logging. */
    public static final String TAG = MinimalImageDownloadPlayer.class.getSimpleName();

    /** Full path to an image file to be played. */
    private String mImagePath;

    /** A class for creating a tuple from a URL and a file path. */
    private class UrlFilePair extends Pair<String, String> {
        UrlFilePair(String url, String filePath) {
            super(url, filePath);
        }
    }


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

        // Download the image file, then play it.
        downloadAndPlay(
                MainMenu.EXAMPLE_IMAGE_1_URI_4096x2048
                //MainMenu.EXAMPLE_IMAGE_1_URI_8129x4096
        );

        // Notice that downloading image files over a network connection requires INTERNET
        // permission to be specified in the manifest file.

    }

    /**
     * Downloads an image file over the network to the local file system, then plays it.
     *
     * @param imageUrl The URL to the image to be downloaded and played.
     */
    public void downloadAndPlay(String imageUrl) {

        // Save the file to external media, if it is currently mounted.
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            mImagePath = PRIVATE_EXTERNAL_FILES_PATH;
        } else {
            mImagePath = PRIVATE_INTERNAL_FILES_PATH;
        }

        // Create a name for the image file.
        String name = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        mImagePath += name;

        // Create a progress bar to be shown while downloading the file.
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle(getString(R.string.player_file_download_title));
        progress.setMessage(String.format(getString(R.string.player_file_download_message), name));
        progress.setMax(100);
        progress.setIndeterminate(false);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        // Create a background task for downloading the file (will take a moment).
        new DownloadFileTask(progress).execute(new UrlFilePair(imageUrl, mImagePath));
    }

    /**
     * Background task for downloading files.
     */
    private class DownloadFileTask extends AsyncTask<UrlFilePair, Integer, Integer> {

        /** Progress dialog to be shown while working. */
        ProgressDialog mProgress;

        /**
         * Constructor.
         *
         * @param progress The progress dialog to be used.
         */
        DownloadFileTask(ProgressDialog progress) {
            mProgress = progress;
        }

        @Override
        public void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected Integer doInBackground(UrlFilePair... files) {

            // Download files over the network to the local file system,
            // if not already there.
            int downloadFileCount = 0;
            for (UrlFilePair filePair : files) {
                String downloadUrl = filePair.first;
                String outputFile = filePair.second;

                if (!new File(outputFile).exists()) {
                    DataInputStream in = null;
                    DataOutputStream out = null;

                    try {
                        URL url = new URL(downloadUrl);
                        URLConnection connection = url.openConnection();
                        int contentLength = connection.getContentLength();
                        in = new DataInputStream(url.openStream());
                        out = new DataOutputStream(new FileOutputStream(outputFile));

                        byte [] buffer = new byte[1024];
                        int read;
                        int total = 0;
                        while (( read = in.read(buffer) ) != -1 ) {
                            out.write(buffer, 0, read);
                            out.flush();
                            total += read;
                            publishProgress((int) ((total / (float) contentLength) * 100));

                            // Escape early if cancel() is called.
                            if (isCancelled()) break;
                        }
                        downloadFileCount++;

                    } catch (Exception e) {
                        Log.e(TAG, "Failed to download " + downloadUrl + " to " + outputFile, e);
                    } finally {
                        if (null != in) {
                            try { in.close(); } catch (IOException e) { Log.e(TAG,
                                    "Failed to close input stream."); }
                        }
                        if (null != out) {
                            try { out.close(); } catch (IOException e) { Log.e(TAG,
                                    "Failed to close output stream."); }
                        }
                    }
                }
            }

            return downloadFileCount;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mProgress.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Integer result) {
            mProgress.dismiss();

            // Notify downloaded files.
            if (result > 0) {
                Toast.makeText(MinimalImageDownloadPlayer.this,
                        String.format(getString(R.string.player_file_download_completed), result),
                        Toast.LENGTH_LONG).show();
            }

            // Show downloaded image, if the file exists.
            if (new File(mImagePath).exists()) {

                setContentUri(mImagePath);

            }
        }
    }

}
