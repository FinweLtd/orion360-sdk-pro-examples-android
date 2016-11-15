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

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.v3.SimpleOrionActivity;

import static fi.finwe.orion360.sdk.pro.examples.MainMenu.PRIVATE_EXTERNAL_FILES_PATH;


/**
 * An example of a minimal Orion360 video player, for downloading a video file before playback.
 * <p>
 * This example uses Android's DownloadManager service for downloading a file (recommended).
 * See MinimalImageDownloadPlayer for an example of using custom code instead.
 * <p>
 * Notice that saving a copy of a video file while streaming it is not possible with Android
 * MediaPlayer as a video backend. To obtain a local copy of a video file that resides in the
 * network you need to download it separately, as shown in this example.
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
public class MinimalVideoDownloadPlayer extends SimpleOrionActivity {

    /** Tag for logging. */
    public static final String TAG = MinimalVideoDownloadPlayer.class.getSimpleName();

    /** Download manager service. */
    private DownloadManager mDownloadManager;

    /** Download reference ID. */
    private long mDownloadId;

    /** Timer for updating download progress periodically on screen. */
    private Timer mProgressTimer = new Timer();


	@Override
	public void onCreate(Bundle savedInstanceState) {

        // Call super class implementation FIRST to set up a simple Orion360 player configuration.
        super.onCreate(savedInstanceState);

        // Above call will fail if a valid Orion360 license file for the package name defined in
        // the application's manifest/build.gradle files cannot be found!

        // Set layout.
		setContentView(R.layout.activity_video_player);

        // Get download manager service.
        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        // Set Orion360 view (defined in the layout) that will be used for rendering 360 content.
        setOrionView(R.id.orion_view);

        // Download the video file, then play it. Notice that this link points to a
        // 4k video file, older/mid-range devices may not be able to play it!
        // In case of problems, try MainMenu.TEST_VIDEO_URI_1920x960 instead.
        downloadAndPlay(
                //MainMenu.TEST_VIDEO_URI_1920x960
                MainMenu.TEST_VIDEO_URI_3840x1920
        );

        // Notice that downloading video files over a network connection requires INTERNET
        // permission to be specified in the manifest file.

        // When you run the app, you may get a warning from MediaPlayer component to the LogCat:
        // W/MediaPlayer: Couldn't open []: java.io.FileNotFoundException: No content provider: []
        // Here Android MediaPlayer is using an exception for control flow; you can disregard it.

    }

    /**
     * Downloads a video file over the network to the local file system, then plays it.
     *
     * @param videoUrl The URL to the video to be downloaded and played.
     */
    public void downloadAndPlay(String videoUrl) {

        // Create a name for the video file.
        String name = videoUrl.substring(videoUrl.lastIndexOf('/') + 1);

        // Skip download, if file already exists (remove file:// scheme before testing).
        String localUri = PRIVATE_EXTERNAL_FILES_PATH + Environment.DIRECTORY_DOWNLOADS
                + File.separator + name;
        if (new File(Uri.parse(localUri).getPath()).exists()) {
            setContentUri(localUri);
            return;
        }

        // Create a progress bar to be shown while downloading the file.
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle(getString(R.string.player_file_download_title));
        progress.setMessage(String.format(getString(R.string.player_file_download_message), name));
        progress.setMax(100);
        progress.setIndeterminate(false);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.show();

        // Create and register a receiver for handling a completed download.
        final BroadcastReceiver receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {

                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(mDownloadId);
                    Cursor c = mDownloadManager.query(query);
                    if (c.moveToFirst()) {

                        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {

                            mProgressTimer.cancel();
                            progress.cancel();
                            unregisterReceiver(this);

                            Toast.makeText(MinimalVideoDownloadPlayer.this,
                                    String.format(getString(
                                            R.string.player_file_download_completed), 1),
                                    Toast.LENGTH_LONG).show();

                            String uriString = c.getString(c.getColumnIndex(
                                    DownloadManager.COLUMN_LOCAL_URI));
                            setContentUri(uriString); // Play downloaded video file.

                        }
                    }
                }
            }
        };
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        // Use download manager to download the file.
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(videoUrl));
        request.setTitle(getResources().getString(R.string.app_name));
        request.setDescription(videoUrl);
        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, name);
        mDownloadId = mDownloadManager.enqueue(request);

        // Create a background task for updating download progress.
        mProgressTimer.schedule(new TimerTask() {

            @Override
            public void run() {

                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(mDownloadId);
                Cursor cursor = mDownloadManager.query(q);
                cursor.moveToFirst();
                int downloaded = cursor.getInt(cursor.getColumnIndex(
                        DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                int total = cursor.getInt(cursor.getColumnIndex(
                        DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                cursor.close();
                final int percent = (int) (100.0 * downloaded / total);
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        progress.setProgress(percent);
                    }

                });

            }

        }, 500, 500);

    }

}
