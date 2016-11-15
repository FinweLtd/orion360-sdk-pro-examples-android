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

package fi.finwe.orion360.sdk.pro.examples;

import android.Manifest;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Provides application's main menu: a list of selectable examples, each implemented as an activity.
 * <p/>
 * The activities are automatically searched from the package (manifest) and added to the menu.
 */
public class MainMenu extends ListActivity {

	/** Tag for logging. */
	public static final String TAG = MainMenu.class.getSimpleName();

	/** Request code for file write permission. */
	private static final int REQUEST_WRITE_STORAGE = 112;

	/** Test video URI for low quality video that can be found from the network. */
	public static final String TEST_VIDEO_URI_1280x640 =
			"https://s3.amazonaws.com/orion360-us/Orion360_test_video_2d_equi_360x180deg_1280x640pix_30fps_30sec_x264.mp4";

	/** Test video URI for medium quality video that can be found from the network. */
	public static final String TEST_VIDEO_URI_1920x960 =
			"https://s3.amazonaws.com/orion360-us/Orion360_test_video_2d_equi_360x180deg_1920x960pix_30fps_30sec_x264.mp4";

	/** Test video URI for high quality video that can be found from the network. */
	public static final String TEST_VIDEO_URI_3840x1920 =
			"https://s3.amazonaws.com/orion360-us/Orion360_test_video_2d_equi_360x180deg_3840x1920pix_30fps_30sec_x264.mp4";

	/** Test video URI for medium quality cropped video that can be found from the network. */
	public static final String TEST_VIDEO_URI_1920x720 =
			"https://s3.amazonaws.com/orion360-us/Orion360_test_video_2d_equi_360x135deg_1920x720pix_30fps_30sec_x264.mp4";

	/** Test image URI for low quality image that can be found from the network. */
	public static final String TEST_IMAGE_URI_1280x640 =
			"https://s3.amazonaws.com/orion360-us/Orion360_test_image_1280x640.jpg";

	/** Test image URI for medium quality image that can be found from the network. */
	public static final String TEST_IMAGE_URI_1920x960 =
			"https://s3.amazonaws.com/orion360-us/Orion360_test_image_1920x960.jpg";

	/** Test image URI for high quality image that can be found from the network. */
	public static final String TEST_IMAGE_URI_3840x1920 =
			"https://s3.amazonaws.com/orion360-us/Orion360_test_image_3840x1920.jpg";

	/** Example image 1 URI for high quality image that can be found from the network. */
	public static final String EXAMPLE_IMAGE_1_URI_4096x2048 =
			"https://s3.amazonaws.com/orion360-us/Orion360_example_image_1_4096x2048.jpg";

	/** Example image 1 URI for 8k image that can be found from the network. */
	public static final String EXAMPLE_IMAGE_1_URI_8129x4096 =
			"https://s3.amazonaws.com/orion360-us/Orion360_example_image_1_8192x4096.jpg";


	/** Test video name for low quality video that is bundled with the app in /res/raw. */
	// Notice: As this file is located in the R.raw folder, we must access it without the
	// filename extension, and ensure that the filename uses lowercase characters.
	public static final String TEST_VIDEO_FILE_LQ = "orion360_test_video_1024x512";

	/** Test image name for low quality image that is bundled with the app in /res/raw. */
	// Notice: As this file is located in the R.raw folder, we must access it without the
	// filename extension, and ensure that the filename uses lowercase characters.
	public static final String TEST_IMAGE_FILE_LQ = "orion360_test_image_1024x512";

	/** Test video name for medium quality video that is bundled with the app in /assets. */
	public static final String TEST_VIDEO_FILE_MQ = "Orion360_test_video_1920x960.mp4";

	/** Test image name for medium quality image that is bundled with the app in /assets. */
	public static final String TEST_IMAGE_FILE_MQ = "Orion360_test_image_1920x960.jpg";

	/** Test image name for medium quality preview image that is bundled with the app in /assets. */
	public static final String TEST_PREVIEW_IMAGE_FILE_MQ = "Orion360_preview_image_1920x960.jpg";

	/** Test image name for high quality tag image that is bundled with the app in /assets. */
	public static final String TEST_TAG_IMAGE_FILE_HQ = "Orion360_nadir_patch_1024x1024.png";

	/** Orion360 directory name (to be created under device's public external files). */
	public static final String ORION_DIRECTORY_NAME = "Orion360/SDK";

	/** Device's public /Movies path with Orion360 subdirectory appended to it. */
	public static final String PUBLIC_EXTERNAL_MOVIES_ORION_PATH =
			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
					.getAbsolutePath() + File.separator + ORION_DIRECTORY_NAME + File.separator;

	/** Device's public /Pictures path with Orion360 subdirectory appended to it. */
	public static final String PUBLIC_EXTERNAL_PICTURES_ORION_PATH =
			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
					.getAbsolutePath() + File.separator + ORION_DIRECTORY_NAME + File.separator;

	/** Expansion package (.obb) path prefix. */
	private static final String EXPANSION_PACKAGES_PATH = "/Android/obb/";

	/** Expansion package files path via content provider. */
	public static final String PRIVATE_EXPANSION_FILES_PATH = "content://" +
			ExpansionContentProvider.AUTHORITY + File.separator;

	/** Asset path prefix. */
	public static final String PRIVATE_ASSET_FILES_PATH = "file:///android_asset/";

	/** R.raw path prefix. */
	public static String PRIVATE_R_RAW_FILES_PATH;

	/** Application's private internal files path. */
	public static String PRIVATE_INTERNAL_FILES_PATH;

	/** Application's private external files path. */
	public static String PRIVATE_EXTERNAL_FILES_PATH;

	/** A class for creating a tuple from two file paths. */
	private class FilePathPair extends Pair<String, String> {
		FilePathPair(String first, String second) {
			super(first, second);
		}
	}

	/** Key for activity's name parameter. */
	private static final String KEY_ACTIVITY_NAME = "ACTIVITY_NAME";

	/** Key for activity's package parameter. */
	private static final String KEY_ACTIVITY_PACKAGE = "ACTIVITY_PACKAGE";

	/** Key for activity's full name parameter. */
	private static final String KEY_ACTIVITY_FULL_NAME = "ACTIVITY_FULL_NAME";

	/** A class for storing key-value data about an activity, such as its name and package. */
	private static class ActivityData extends HashMap<String, String> {
		private static final long serialVersionUID = 1L;
	}

	/** Store activity data structures grouped by their last package names. */
	private HashMap<String, List<ActivityData>> mGroupedActivities;

    /** Adapter for listing activity groups. */
    private ArrayAdapter<String> mGroupAdapter;

    /** Adapter for listing activities themselves. */
    private SimpleAdapter mItemAdapter;

    /** Text view for title text. */
    private TextView mMainMenuTitleText;

    /** Time limit for counting two back presses (in ms). */
    protected static final int DOUBLE_BACK_TO_EXIT_TIME_WINDOW = 2000;

    /** Flag for enabling double back to exit -feature. */
    protected boolean mDoubleBackToExitEnabled = true;

    /** Flag for counting two back presses for exit. */
    protected boolean mDoubleBackToExitPressedOnce = false;

    /** Toast for asking another back press. */
    protected Toast mDoubleBackToExitNotification = null;

    /** Handler for clearing back press counter. */
    protected Handler mDoubleBackToExitHandler = null;

    /** Runnable that actually resets back press counter (flag). */
    private Runnable mDoubleBackToExitCounterReset = new Runnable() {
        @Override
        public void run() {
            mDoubleBackToExitPressedOnce = false;
        }
    };


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout.
		setContentView(R.layout.activity_main_menu);

        // Get title text view.
        mMainMenuTitleText = (TextView) findViewById(R.id.main_menu_title);

		// Initialize application's private paths (we need a Context to do these).
		PRIVATE_R_RAW_FILES_PATH = ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
				+ getPackageName() + "/raw/";
		File filesDir = getFilesDir();
		if (null != filesDir) {
			PRIVATE_INTERNAL_FILES_PATH = ContentResolver.SCHEME_FILE + "://"
					+ filesDir.getAbsolutePath() + File.separator;
		}
		File externalFilesDir = getExternalFilesDir(null);
		if (null != externalFilesDir) {
			PRIVATE_EXTERNAL_FILES_PATH = ContentResolver.SCHEME_FILE + "://"
					+ externalFilesDir.getAbsolutePath() + File.separator;
		}

		// Copy test content in place to private and public dirs. Note: We need to check write
		// permission before attempting to write to public area on Android 6.0 or above.
		// When the result is known, copy only the relevant files in the background.
		checkWritePermissionAndCopyContent();

		// Find other activities in our package.
		List<ActivityData> activityDataList = findOtherActivities();

        // Group the activities based on last package name.
        mGroupedActivities = createActivityGroups(activityDataList);

        // Setup an adapter for listing the activity groups in the UI.
        ArrayList<String> groupNames = new ArrayList<String>(mGroupedActivities.keySet());
        Collections.sort(groupNames);
        mGroupAdapter = new ArrayAdapter<String>(this,
                R.layout.list_main_menu_row, R.id.textview_activity_name, groupNames);

        // Show groups.
        setListAdapter(mGroupAdapter);

        // Double back press to exit.
        mDoubleBackToExitPressedOnce = false;
        mDoubleBackToExitNotification = Toast.makeText(this, getResources()
                        .getString(R.string.double_back_exit_notification),
                Toast.LENGTH_SHORT); // Disregard warning, not supposed to show the toast yet!
        mDoubleBackToExitHandler = new Handler();
    }

    @Override
    protected void onPause() {
        if (null != mDoubleBackToExitNotification) {
            mDoubleBackToExitNotification.cancel();
        }
        if (null != mDoubleBackToExitHandler) {
            mDoubleBackToExitHandler.removeCallbacks(
                    mDoubleBackToExitCounterReset);
        }
        mDoubleBackToExitPressedOnce = false;
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (mDoubleBackToExitEnabled) {

            // Handle double back to exit -feature.
            if (!mDoubleBackToExitPressedOnce) {

                if (this.getListAdapter() != mGroupAdapter) {

                    // Return to groups view.
                    setListAdapter(mGroupAdapter);
                    mMainMenuTitleText.setText(R.string.app_name);

                } else {

                    // First press observed.
                    mDoubleBackToExitPressedOnce = true;

                    // Notify user that another press is needed.
                    mDoubleBackToExitNotification.show();

                    // Cancel first press if second press is not observed in time.
                    mDoubleBackToExitHandler.postDelayed(
                            mDoubleBackToExitCounterReset,
                            DOUBLE_BACK_TO_EXIT_TIME_WINDOW);

                }

            } else {

                // Second press came in time. Cancel notification and handler.
                mDoubleBackToExitNotification.cancel();
                mDoubleBackToExitHandler
                        .removeCallbacks(mDoubleBackToExitCounterReset);

                // Let the app exit now.
                super.onBackPressed();
            }
        } else {

            // Let the app exit now.
            super.onBackPressed();
        }
    }

    @Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		view.setSelected(true);

        if (listView.getAdapter() == mGroupAdapter) {

            // An activity group was selected from the UI, try to show grouped activities.
            String groupName = (String) listView.getItemAtPosition(position);
            List<ActivityData> activities = mGroupedActivities.get(groupName);
            String [] rowNames = new String [] { KEY_ACTIVITY_NAME };
            int [] cellResIds = new int [] { R.id.textview_activity_name };
            mItemAdapter = new SimpleAdapter(this, activities,
                    R.layout.list_main_menu_row, rowNames, cellResIds);
            setListAdapter(mItemAdapter);
            mMainMenuTitleText.setText(groupName);

        } else if (listView.getAdapter() == mItemAdapter) {

            // An activity was selected from the UI, try to start it now.
            ActivityData activityData = (ActivityData) listView.getItemAtPosition(position);
            try {
                Intent intent = new Intent(this, Class.forName(
                        activityData.get(KEY_ACTIVITY_FULL_NAME)));
                startActivity(intent);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Failed to start selected activity", e);
            }

        }
	}

    /**
     * Check if write permission is granted, and if not, request it, and then copy the content.
     */
	private void checkWritePermissionAndCopyContent() {

		// Check permission status.
		int permission = ContextCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE);

		// If missing, check if we should first explain why it is needed, then request it.
		if (permission != PackageManager.PERMISSION_GRANTED) {

			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.main_menu_permission_request_write)
						.setTitle(R.string.main_menu_permission_request_title);
				builder.setPositiveButton(R.string.main_menu_permission_grant_button_label,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								requestWritePermission();
							}
						});
				AlertDialog dialog = builder.create();
				dialog.show();
			} else {
				requestWritePermission();
			}
		} else {

			// Permission is already granted, proceed to copy test content in place.
			copyTestContent(true);

		}
	}

	/**
	 * Request write permission from user.
	 */
	private void requestWritePermission() {
		ActivityCompat.requestPermissions(this,
				new String [] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
				REQUEST_WRITE_STORAGE);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String [] permissions,
										   @NonNull int [] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		switch (requestCode) {
			case REQUEST_WRITE_STORAGE: {
				if (grantResults.length == 0 || grantResults[0] !=
						PackageManager.PERMISSION_GRANTED) {
					Log.i(TAG, "Write permission was denied by user");

					Toast.makeText(this, R.string.main_menu_permission_warning,
							Toast.LENGTH_LONG).show();

					// Permission was denied, proceed to copy files that do not need it.
					copyTestContent(false);

				} else {
					Log.i(TAG, "Write permission was granted by user");

					// Permission was granted, proceed to copy all test content in place.
					copyTestContent(true);
				}
				return;
			}
			default:
				break;
		}
	}

	/**
	 * Copy test content to public directories. If write permission is missing, skip some files.
	 *
	 * @param hasWritePermission Tells whether write permission has been granted, or not.
	 */
	private void copyTestContent(boolean hasWritePermission) {
		List<FilePathPair> copyFiles = new ArrayList<>();

		// Add files to be copied to private area (does not need write permission).
		copyFiles.add(new FilePathPair(TEST_VIDEO_FILE_MQ,
				PRIVATE_INTERNAL_FILES_PATH + TEST_VIDEO_FILE_MQ));
		copyFiles.add(new FilePathPair(TEST_IMAGE_FILE_MQ,
				PRIVATE_INTERNAL_FILES_PATH + TEST_IMAGE_FILE_MQ));
		copyFiles.add(new FilePathPair(TEST_VIDEO_FILE_MQ,
				PRIVATE_EXTERNAL_FILES_PATH + TEST_VIDEO_FILE_MQ));
		copyFiles.add(new FilePathPair(TEST_IMAGE_FILE_MQ,
				PRIVATE_EXTERNAL_FILES_PATH + TEST_IMAGE_FILE_MQ));
		copyFiles.add(new FilePathPair(TEST_PREVIEW_IMAGE_FILE_MQ,
				PRIVATE_EXTERNAL_FILES_PATH + TEST_PREVIEW_IMAGE_FILE_MQ));

		// Add files to be copied to public area (needs write permission).
		if (hasWritePermission) {
			copyFiles.add(new FilePathPair(TEST_VIDEO_FILE_MQ,
					PUBLIC_EXTERNAL_MOVIES_ORION_PATH + TEST_VIDEO_FILE_MQ));
			copyFiles.add(new FilePathPair(TEST_IMAGE_FILE_MQ,
					PUBLIC_EXTERNAL_PICTURES_ORION_PATH + TEST_IMAGE_FILE_MQ));
			copyFiles.add(new FilePathPair(TEST_TAG_IMAGE_FILE_HQ,
					PUBLIC_EXTERNAL_PICTURES_ORION_PATH + TEST_TAG_IMAGE_FILE_HQ));
		}

		// Create a progress bar to be shown while copying files.
		ProgressDialog progress = new ProgressDialog(this);
		progress.setTitle(getString(R.string.main_menu_init_title));
		progress.setMessage(getString(R.string.main_menu_init_message));
		progress.setMax(copyFiles.size());
		progress.setIndeterminate(false);
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

		// Create a background task for copying the files (will take a moment).
		new CopyFileTask(progress).execute(copyFiles.toArray(
				new FilePathPair[ copyFiles.size() ]));
	}

	/**
	 * Background task for copying files.
	 */
	private class CopyFileTask extends AsyncTask<FilePathPair, Integer, Integer> {

		/** Progress dialog to be shown while working. */
		ProgressDialog mProgress;

		/**
		 * Constructor.
		 *
		 * @param progress The progress dialog to be used.
		 */
		CopyFileTask(ProgressDialog progress) {
			mProgress = progress;
		}

		@Override
		public void onPreExecute() {
			mProgress.show();
		}

		@Override
		protected Integer doInBackground(FilePathPair... filePaths) {

			// Copy media files from assets to ordinary files in the file system,
			// if not already there.
			int pathCount = filePaths.length;
			int copyCount = 0;
			for (int i = 0; i < pathCount; i++) {
				String fromPath = filePaths[i].first;
				String toPath = filePaths[i].second;
				if (copyAssetToFileIfNotExist(fromPath, toPath)) {
					copyCount++;
				}
				publishProgress((int) ((i / (float) pathCount) * 100));

				// Escape early if cancel() is called.
				if (isCancelled()) break;
			}

			// Expansion package (.obb) is an optional extra installation file that is used for
			// bundling large asset files with the app (for example videos that rarely change).
			// With an expansion package, it is possible to publish larger than 100 MB apps
			// in the Google Play store. Usually the file comes from Google Play automatically
			// when the app is downloaded, but here we create one for simplicity, by zipping
			// media files (without compressing them) to a specifically named file.
			if (createExpansionPackageIfNotFound()) {
				copyCount++;
			}

			return copyCount;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			mProgress.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Integer result) {
			mProgress.dismiss();
			if (result > 0) {
				Toast.makeText(MainMenu.this,
						String.format(getString(R.string.main_menu_init_files_copied), result),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * Copy given asset file to a given file path, unless target file already exists.
	 *
	 * Missing directories will be automatically created.
	 *
	 * Note: This only works for files that have not been compressed when building the apk.
	 * Typically OK for media files, which have their own compression.
	 *
	 * @param assetPath The asset path where from to copy.
	 * @param filePath The file path where to to copy.
	 * @return true if file was copied, else false.
	 */
	private boolean copyAssetToFileIfNotExist(String assetPath, String filePath) {
		boolean success = false;

		File file = new File(filePath);
		if (!file.exists()) {
			Log.i(TAG, "Copying " + assetPath + " to " + filePath);

			// Create directories.
			if (file.getParentFile().mkdirs()) {
				Log.i(TAG, "Created directory " + file.getParentFile().getAbsolutePath());
			}

			InputStream in = null;
			OutputStream out = null;
			try {
				if (file.createNewFile()) {
					Log.i(TAG, "Created file " + file.getName());
				}
				in = getAssets().open(assetPath);
				out = new FileOutputStream(file);

				// Copy the file.
				copyFile(in, out);
				Log.i(TAG, "Copied file contents from " + assetPath + " to " + filePath);
				success = true;

			} catch (IOException e) {
				Log.e(TAG, "Failed to copy " + assetPath + " to " + filePath, e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						Log.e(TAG, "Failed to close input file stream", e);
					}
				}
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {
						Log.e(TAG, "Failed to close output file stream", e);
					}
				}
			}
		}

		return success;
	}

	/**
	 * Copy file contents from one stream to another.
	 *
	 * @param in The input stream where from to copy.
	 * @param out The output stream where to to copy.
	 * @throws IOException
	 */
	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte [] buffer = new byte[1024];
		int read;
		while (( read = in.read(buffer) ) != -1 ) {
			out.write(buffer, 0, read);
		}
	}

	/**
	 * Check if an expansion package can be found, and if not, create one.
	 *
	 * @return true if the expansion package was created, else false.
	 */
	boolean createExpansionPackageIfNotFound() {

		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

			// Build the full path to the app's main expansion file.
			File root = Environment.getExternalStorageDirectory();
			File expPath = new File(root.toString() + EXPANSION_PACKAGES_PATH + getPackageName());

			// Create missing directories, if any.
			if (expPath.mkdirs()) {
				Log.i(TAG, "Created directory for expansion packages: " + expPath);
			}

			// Create main expansion filename using current package version name.
			int mainVersion;
			try {
				mainVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			} catch (PackageManager.NameNotFoundException e) {
				Log.e(TAG, "Failed to find own package version number");
				return false;
			}
			String mainExpFilename = expPath + File.separator + "main." + mainVersion + "." +
					getPackageName() + ".obb";

			// Check if the main expansion file exists, and create it, if not.
			File mainExpFile = new File(mainExpFilename);
			if (mainExpFile.exists()) {
				Log.i(TAG, "Main expansion package found from: " + mainExpFilename);
			} else {
				if (zip(mainExpFilename, false, // do not compress media files again
						PRIVATE_EXTERNAL_FILES_PATH + TEST_VIDEO_FILE_MQ,
						PRIVATE_EXTERNAL_FILES_PATH + TEST_IMAGE_FILE_MQ)) {
					Log.i(TAG, "Created main expansion file: " + mainExpFilename);
					return true;
				} else {
					Log.i(TAG, "Failed to create main expansion file: " + mainExpFilename);
				}
			}

		} else {
			Log.e(TAG, "Media not mounted, cannot find/create expansion package");
		}

		return false;
	}

	/**
	 * Create a zip package from given file(s).
	 *
	 * @param zipFile The zip file to be created, with full path.
	 * @param compress Set to true to compress files, false to just wrap them into a zip container.
	 * @param filenames The full path to the file(s) to be included to the zip file.
	 * @return true if package was successfully created, else false.
	 */
	private boolean zip(String zipFile, boolean compress, String... filenames) {
		Log.i(TAG, "Creating a zip file: " + zipFile);

		try {
			BufferedInputStream origin;
			FileOutputStream out = new FileOutputStream(zipFile);
			ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(out));

			int bufferSize = 1024;
			byte [] buffer = new byte[bufferSize];
			for (String filename : filenames) {
				Log.i(TAG, " adding to zip: " + filename);

				FileInputStream in = new FileInputStream(filename);
				origin = new BufferedInputStream(in, bufferSize);
				ZipEntry entry = new ZipEntry(filename.substring(filename.lastIndexOf(
						File.separator) + 1));
				if (!compress) {
					long bytes = new File(filename).length();
					long crc = crc32(filename);
					Log.i(TAG, " size: " + bytes + " crc32: " + Long.toHexString(crc));

					entry.setMethod(ZipEntry.STORED);
					entry.setSize(bytes);
					entry.setCompressedSize(bytes);
					entry.setCrc(crc);
				}
				zip.putNextEntry(entry);
				int read;
				while (( read = origin.read(buffer, 0, bufferSize)) != -1) {
					zip.write(buffer, 0, read);
				}
				origin.close();
			}
			zip.close();

			return true;
		} catch(Exception e) {
			Log.e(TAG, "Failed to create a zip package: " + zipFile, e);
		}

		return false;
	}

	/**
	 * Calculates the CRC checksum value from the given file.
	 *
	 * @param filepath The full path to the file to whose checksum to calculate.
	 * @return the calculated CRC value.
	 */
	private long crc32(String filepath) {
		CRC32 checksum = new CRC32();
		checksum.reset();
		byte [] buffer = new byte[1024];
		InputStream in = null;
		try {
			in = new FileInputStream(filepath);
			int read;
			while (( read = in.read(buffer) ) != -1 ) {
				checksum.update(buffer, 0, read);
			}
		} catch (IOException e) {
			Log.e(TAG, "Failed to calculate CRC32 checksum from file " + filepath);
		} finally {
			if (null != in) {
				try { in.close(); } catch (IOException e) { Log.e(TAG, "Failed to close stream."); }
			}
		}
		return checksum.getValue();
	}

	/**
	 * Find all other activities in the package, and return a list of data structures.
	 *
	 * @return A data structure for each found activity, or an empty list if none was found.
	 */
	private List<ActivityData> findOtherActivities() {

		// Create a list where to store activity data.
		List<ActivityData> activityDataList = new ArrayList<>();

		// Get all activities in the package.
		PackageInfo packageInfo = getPackageManager().getPackageArchiveInfo(
				getPackageCodePath(), PackageManager.GET_ACTIVITIES);

		// Parse each activity's name, package and full name, and store them into the list.
		for (ActivityInfo activityInfo : packageInfo.activities) {
			if (activityInfo.name.equals(this.getClass().getName()))
				continue; // Skip self.

			ActivityData activityData = new ActivityData();
			String [] nameParts = activityInfo.name.split("\\.");
			activityData.put(KEY_ACTIVITY_NAME, nameParts[nameParts.length - 1]
					.replaceAll("(\\p{Ll})(\\p{Lu})","$1 $2")); // HelloWorld -> Hello World
			activityData.put(KEY_ACTIVITY_PACKAGE, activityInfo.packageName);
			activityData.put(KEY_ACTIVITY_FULL_NAME, activityInfo.name);

			activityDataList.add(activityData);
		}

		return activityDataList;
	}

	/**
	 * Group activity data structures by their last package names.
	 *
	 * @param data The key-value data structure of each activity to be grouped.
	 * @return A map containing grouped activity data structures.
     */
	private HashMap<String, List<ActivityData>> createActivityGroups(List<ActivityData> data) {
		HashMap<String, List<ActivityData>> grouped = new HashMap<>();

		for (ActivityData activityData : data) {
            String fullName = activityData.get(KEY_ACTIVITY_FULL_NAME);
			String groupName = fullName.substring(
                    (fullName.substring(0, fullName.lastIndexOf('.'))).lastIndexOf('.') + 1,
                    fullName.lastIndexOf('.'));
            String GroupName = groupName.substring(0, 1).toUpperCase() + groupName.substring(1);
            if (grouped.containsKey(GroupName)) {
                List<ActivityData> oldGroup = grouped.get(GroupName);
                oldGroup.add(activityData);
            } else {
                List<ActivityData> newGroup = new ArrayList<>();
                newGroup.add(activityData);
                grouped.put(GroupName, newGroup);
            }
		}

		return grouped;
	}

}
