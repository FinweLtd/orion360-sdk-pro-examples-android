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

package fi.finwe.orion360.sdk.pro.examples.gallery;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fi.finwe.math.Vec3F;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionContext;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.controller.RotationAligner;
import fi.finwe.orion360.sdk.pro.controller.TouchPincher;
import fi.finwe.orion360.sdk.pro.controller.TouchRotater;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.item.sprite.OrionSprite;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.widget.OrionWidget;
import fi.finwe.orion360.sdk.pro.widget.SelectablePointerIcon;
import fi.finwe.util.ContextUtil;

/**
 * An example of a simple video gallery using a thumbnail pager style.
 * <p/>
 * Features:
 * <ul>
 * <li>Automatically finds videos in hard-coded path, creates thumbnails, adds items to gallery
 * <li>Loads one hard-coded 360 panorama image in .jpg format as the gallery room background
 * <li>Allows browsing through the gallery by looking at the next/previous arrows for a moment
 * <li>Plays the video selected by user by looking at the play icon for a moment
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Renders the gallery and the videos using standard rectilinear projection
 * <li>Allows navigation with touch & movement sensors (if supported by HW) as follows:
 * <ul>
 * <li>Panning (gyro or swipe)
 * <li>Zooming (pinch)
 * <li>Tilting (pinch rotate)
 * </ul>
 * <li>Auto Horizon Aligner (AHL) feature straightens the horizon</li>
 * </ul>
 */
public class ThumbnailPager extends OrionActivity {

    /** Tag for logging. */
    public static final String TAG = ThumbnailPager.class.getSimpleName();

    /** The file system path where media items are searched from. */
    protected final static String MEDIA_PATH = MainMenu.PUBLIC_EXTERNAL_MOVIES_ORION_PATH;

    /** Request code for file read permission. */
    protected static final int REQUEST_READ_STORAGE = 111;

    /** The Android view where our 3D scenes will be rendered to. */
    protected OrionView mView;

    /** The 3D scene for our gallery room. */
    protected OrionScene mGalleryScene;

    /** The panorama sphere for the gallery background. */
    protected OrionPanorama mGalleryBackground;

    /** The image texture for the gallery background. */
    protected OrionTexture mGalleryBackgroundTexture;

    /** The sprite for our gallery item thumbnail. */
    protected OrionSprite mGalleryThumbnail;

    /** The image texture for our gallery item thumbnail. */
    protected OrionTexture mGalleryThumbnailTexture;

    /** The widget that will act as the 'play' button. */
    protected SelectablePointerIcon mPlayButton;

    /** The widget that will act as the 'next' button. */
    protected SelectablePointerIcon mNextButton;

    /** The widget that will act as the 'previous' button. */
    protected SelectablePointerIcon mPreviousButton;

    /** The 3D scene for our video player. */
    protected OrionScene mVideoPlayerScene;

    /** The panorama sphere for the 360 video. */
    protected OrionPanorama mVideoCanvas;

    /** The video texture for the panorama video. */
    protected OrionTexture mVideoCanvasTexture;

    /** The widget that will act as the 'home' button. */
    protected SelectablePointerIcon mHomeButton;

    /** The camera which will project our 3D scenes to a 2D (view) surface. */
    protected OrionCamera mCamera;

    /** The widget that will handle our touch gestures. */
    protected TouchControllerWidget mTouchController;

    /** The video gallery where video items found from hard-coded path will be added to. */
    protected Gallery mGallery;

    /** The index of the currently selected item in the gallery, or -1 if none selected. */
    protected int mCurrentItemIndex = -1;


    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // When accessing paths on the external media, we should first check if it is currently
        // mounted or not (though, it is often built-in non-removable memory nowadays).
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, R.string.player_media_not_mounted, Toast.LENGTH_LONG).show();
            return;
        }

        // In case we want to access videos in public external folder on Android 6.0 or above,
        // we must ensure that READ_EXTERNAL_STORAGE permission is granted *before* attempting
        // to play the files in that location.
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest
                .permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Read permission has not been granted. As user can give the permission when
            // requested, the operation now becomes asynchronous: we must wait for
            // user's decision, and act when we receive a callback.
            ActivityCompat.requestPermissions(this, new String [] {
                    Manifest.permission.READ_EXTERNAL_STORAGE }, REQUEST_READ_STORAGE);
            return;
        }

        // We have all the required permissions already and can proceed to init phase.
        initialize();
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

                    // We have all the required permissions and can proceed to init phase.
                    initialize();
                }
                return;
            }
            default:
                break;
        }
    }

    /**
     * Initialize after receiving required permissions.
     */
    protected void initialize() {

        // Create a new gallery from hard-coded video file path.
        mGallery = new Gallery(MEDIA_PATH);

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera();

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        OrionContext.getSensorFusion().bindControllable(mCamera);

        // Create a new touch controller widget (convenience class), and let it control our camera.
        mTouchController = new TouchControllerWidget(mCamera);

        // Initialize video player components.
        initVideoPlayer();

        // Initialize gallery components.
        initGallery();

        // Find Orion360 view from the XML layout. This is an Android view where we render content.
        mView = (OrionView)findViewById(R.id.orion_view);

        // Bind the scene to the view. This is the 3D world that we will be rendering to this view.
        mView.bindDefaultScene(mGalleryScene);

        // Bind the camera to the view. We will look into the 3D world through this camera.
        mView.bindDefaultCamera(mCamera);

        // The view can be divided into one or more viewports. For example, in VR mode we have one
        // viewport per eye. Here we fill the complete view with one (landscape) viewport.
        mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL,
                OrionViewport.CoordinateType.FIXED_LANDSCAPE);
    }

    /**
     * Initializes Orion360 video player components.
     */
    protected void initVideoPlayer() {

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mVideoPlayerScene = new OrionScene();

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mVideoPlayerScene.bindController(OrionContext.getSensorFusion());

        // Bind the touch controller widget to the scene. This will make it functional in the scene.
        mVideoPlayerScene.bindWidget(mTouchController);

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mVideoCanvas = new OrionPanorama();

        // Notice: we will create and bind video texture later, when user chooses the video.

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mVideoPlayerScene.bindSceneItem(mVideoCanvas);

        // Create video player 'home' button for returning to gallery, and bind it to the scene.
        mHomeButton = new SelectablePointerIcon();
        mHomeButton.setLocationPolarZXYDeg(0.0f, -65.0f, 0.0f, 0.7f);
        mHomeButton.setScale(0.1f, 0.15f);
        mHomeButton.getIcon().bindTexture(OrionTexture.createTextureFromURI(this,
                getString(R.string.asset_icon_home)));
        mHomeButton.getIcon().setAmpAlpha(0.70f);
        mHomeButton.getPieSprite().bindTexture(OrionTexture.createTextureFromURI(this,
                getString(R.string.asset_icon_home)));
        mHomeButton.setPointer(mCamera);
        mHomeButton.setSelectionTriggerFrameCount(90);
        mHomeButton.setUiThreadListener(new SelectablePointerIcon.Listener() {

            @Override
            public void onSelectionTrigger() {
                Log.d(TAG, "Home button triggered");

                // We are returning from video player to gallery, clean up video texture.
                if (null != mVideoCanvasTexture) {
                    mVideoCanvasTexture.pause();
                    mVideoCanvas.releaseTexture(0);
                    mVideoCanvasTexture.destroy();
                }

                // Switch from video player scene to gallery scene.
                OrionViewport viewport = mView.getViewports()[0];
                viewport.releaseScene();
                viewport.bindScene(mGalleryScene);
            }

            @Override
            public void onSelectionFocusLost() {
                Log.d(TAG, "Home button focus lost");
            }

            @Override
            public void onSelectionFocusGained() {
                Log.d(TAG, "Home button focus gained");
            }

        });
        mVideoPlayerScene.bindWidget(mHomeButton);
    }

    /**
     * Initializes Orion360 gallery components.
     */
    protected void initGallery() {

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mGalleryScene = new OrionScene();

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mGalleryScene.bindController(OrionContext.getSensorFusion());

        // Bind the touch controller widget to the scene. This will make it functional in the scene.
        mGalleryScene.bindWidget(mTouchController);

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mGalleryBackground = new OrionPanorama();

        // Create a new video (or image) texture from a video (or image) source URI.
        mGalleryBackgroundTexture = OrionTexture.createTextureFromURI(this,
                MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_IMAGE_FILE_LIVINGROOM_HQ);

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mGalleryBackground.bindTextureFull(0, mGalleryBackgroundTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mGalleryScene.bindSceneItem(mGalleryBackground);

        // Create a new sprite. This is a 2D plane in the 3D world for our planar thumbnail.
        mGalleryThumbnail = new OrionSprite();

        // Set sprite location in the 3D world. Here we place the video on the white screen.
        mGalleryThumbnail.setWorldTranslation(new Vec3F(0.03f, 0.19f, -0.77f));

        // Set sprite size in the 3D world. Here we make it fit on the white screen.
        mGalleryThumbnail.setScale(0.42f);

        // Create a new video (or image) texture from a video (or image) source URI.
        List<Gallery.GalleryItem> items = mGallery.getItems();
        if (items.size() > 0) {
            mCurrentItemIndex = 0;
            mGalleryThumbnailTexture = OrionTexture.createTextureFromURI(this,
                    items.get(mCurrentItemIndex).mThumbUri);
            mGalleryThumbnail.bindTexture(mGalleryThumbnailTexture);
        }

        // Bind the sprite to the scene. This will make it part of our 3D world.
        mGalleryScene.bindSceneItem(mGalleryThumbnail);

        // Create gallery 'play' button and bind it to the scene.
        mPlayButton = new SelectablePointerIcon();
        mPlayButton.setLocationPolarZXYDeg(0.0f, 15.0f, 0.0f, 0.7f);
        mPlayButton.setScale(0.2f, 0.3f);
        mPlayButton.getIcon().bindTexture(OrionTexture.createTextureFromURI(this,
                getString(R.string.asset_icon_play)));
        mPlayButton.getIcon().setAmpAlpha(0.70f);
        mPlayButton.getPieSprite().bindTexture(OrionTexture.createTextureFromURI(this,
                getString(R.string.asset_icon_play)));
        mPlayButton.setPointer(mCamera);
        mPlayButton.setSelectionTriggerFrameCount(90);
        mPlayButton.setUiThreadListener(new SelectablePointerIcon.Listener() {

            @Override
            public void onSelectionTrigger() {

                // Get video URI for the currently selected item from the gallery.
                String contentUri = mGallery.getItems().get(mCurrentItemIndex).mContentUri;
                Log.d(TAG, "Play button triggered for " + contentUri);

                // Create a video texture from that video URI.
                mVideoCanvasTexture = OrionTexture.createTextureFromURI(
                        ThumbnailPager.this, contentUri);
                mVideoCanvasTexture.setLooping(true);
                if (null != mVideoCanvasTexture) {

                    // If texture could be created, bind it to video panorama.
                    mVideoCanvas.bindTextureFull(0, mVideoCanvasTexture);

                    // Switch from gallery scene to video player scene.
                    OrionViewport viewport = mView.getViewports()[0];
                    viewport.releaseScene();
                    viewport.bindScene(mVideoPlayerScene);
                }
            }

            @Override
            public void onSelectionFocusLost() {
                Log.d(TAG, "Play button focus lost");
            }

            @Override
            public void onSelectionFocusGained() {
                Log.d(TAG, "Play button focus gained");
            }

        });
        mGalleryScene.bindWidget(mPlayButton);

        // Create gallery 'next' button and bind it to the scene.
        mNextButton = new SelectablePointerIcon();
        mNextButton.setLocationPolarZXYDeg(-32.0f, 13.0f, 0.0f, 0.7f);
        mNextButton.setScale(0.05f, 1.5f * 0.05f);
        mNextButton.getIcon().bindTexture(OrionTexture.createTextureFromURI(this,
                getString(R.string.asset_icon_arrow_right)));
        mNextButton.getIcon().setAmpAlpha(0.70f);
        mNextButton.getPieSprite().bindTexture(OrionTexture.createTextureFromURI(this,
                getString(R.string.asset_icon_arrow_right)));
        mNextButton.setPointer(mCamera);
        mNextButton.setSelectionTriggerFrameCount(90);
        mNextButton.setUiThreadListener(new SelectablePointerIcon.Listener() {

            @Override
            public void onSelectionTrigger() {
                Log.d(TAG, "Next button triggered");

                List<Gallery.GalleryItem> items = mGallery.getItems();

                // Switch to next item in the gallery, if there is more.
                if (mCurrentItemIndex < (items.size() - 1)) {
                    mCurrentItemIndex++;

                    // Switch thumbnail.
                    mGalleryThumbnail.releaseTexture();
                    mGalleryThumbnailTexture = OrionTexture.createTextureFromURI(
                            ThumbnailPager.this, items.get(mCurrentItemIndex).mThumbUri);
                    mGalleryThumbnail.bindTexture(mGalleryThumbnailTexture);

                    // Make 'previous' button visible, we can browse backwards.
                    mPreviousButton.setEnabled(true);

                    // Make 'next' button invisible, if we are now showing the last item.
                    if (mCurrentItemIndex == items.size() - 1) {
                        mNextButton.setEnabled(false);
                    }
                }
            }

            @Override
            public void onSelectionFocusLost() {
                Log.d(TAG, "Nexut button focus lost");
            }

            @Override
            public void onSelectionFocusGained() {
                Log.d(TAG, "Next button focus gained");
            }

        });
        if (mCurrentItemIndex == mGallery.getItems().size() - 1) {
            mNextButton.setEnabled(false);
        }
        mGalleryScene.bindWidget(mNextButton);

        // Create gallery 'previous' button and bind it to the scene.
        mPreviousButton = new SelectablePointerIcon();
        mPreviousButton.setLocationPolarZXYDeg(29.0f, 13.0f, 0.0f, 0.7f);
        mPreviousButton.setScale(0.05f, 1.5f * 0.05f);
        mPreviousButton.getIcon().bindTexture(OrionTexture.createTextureFromURI(this,
                getString(R.string.asset_icon_arrow_left)));
        mPreviousButton.getIcon().setAmpAlpha(0.70f);
        mPreviousButton.getPieSprite().bindTexture(OrionTexture.createTextureFromURI(this,
                getString(R.string.asset_icon_arrow_left)));
        mPreviousButton.setPointer(mCamera);
        mPreviousButton.setSelectionTriggerFrameCount(90);
        mPreviousButton.setUiThreadListener(new SelectablePointerIcon.Listener() {

            @Override
            public void onSelectionTrigger() {
                Log.d(TAG, "Previous button triggered");

                List<Gallery.GalleryItem> items = mGallery.getItems();

                // Switch to previous item in the gallery, if not currently showing the first.
                if (mCurrentItemIndex > 0) {
                    mCurrentItemIndex--;

                    // Switch thumbnail.
                    mGalleryThumbnail.releaseTexture();
                    mGalleryThumbnailTexture = OrionTexture.createTextureFromURI(
                            ThumbnailPager.this, items.get(mCurrentItemIndex).mThumbUri);
                    mGalleryThumbnail.bindTexture(mGalleryThumbnailTexture);

                    // Make 'next' button visible, we can browse forward.
                    mNextButton.setEnabled(true);

                    // Make 'previous' button invisible, if we are now showing the first item.
                    if (mCurrentItemIndex == 0) {
                        mPreviousButton.setEnabled(false);
                    }
                }
            }

            @Override
            public void onSelectionFocusLost() {
                Log.d(TAG, "Previous button focus lost");
            }

            @Override
            public void onSelectionFocusGained() {
                Log.d(TAG, "Previous button focus gained");
            }

        });
        mPreviousButton.setEnabled(false);
        mGalleryScene.bindWidget(mPreviousButton);
    }

    /**
     * Convenience class for configuring typical touch control logic.
     */
    public class TouchControllerWidget implements OrionWidget {

        /** The camera that will be controlled by this widget. */
        private OrionCamera mCamera;

        /** Touch pinch-to-zoom/pinch-to-rotate gesture handler. */
        private TouchPincher mTouchPincher;

        /** Touch drag-to-pan gesture handler. */
        private TouchRotater mTouchRotater;

        /** Rotation aligner keeps the horizon straight at all times. */
        private RotationAligner mRotationAligner;


        /**
         * Constructs the widget.
         *
         * @param camera The camera to be controlled by this widget.
         */
        TouchControllerWidget(OrionCamera camera) {

            // Keep a reference to the camera that we control.
            mCamera = camera;

            // Create pinch-to-zoom/pinch-to-rotate handler.
            mTouchPincher = new TouchPincher();
            mTouchPincher.setMinimumDistanceDp(OrionContext.getActivity(), 20);
            mTouchPincher.bindControllable(mCamera, OrionCamera.VAR_FLOAT1_ZOOM);

            // Create drag-to-pan handler.
            mTouchRotater = new TouchRotater();
            mTouchRotater.bindControllable(mCamera);

            // Create the rotation aligner, responsible for rotating the view so that the horizon
            // aligns with the user's real-life horizon when the user is not looking up or down.
            mRotationAligner = new RotationAligner();
            mRotationAligner.setDeviceAlignZ(-ContextUtil.getDisplayRotationDegreesFromNatural(
                    OrionContext.getActivity()));
            mRotationAligner.bindControllable(mCamera);

            // Rotation aligner needs sensor fusion data in order to do its job.
            OrionContext.getSensorFusion().bindControllable(mRotationAligner);
        }

        @Override
        public void onBindWidget(OrionScene scene) {
            // When widget is bound to scene, bind the controllers to it to make them functional.
            scene.bindController(mTouchPincher);
            scene.bindController(mTouchRotater);
            scene.bindController(mRotationAligner);
        }

        @Override
        public void onReleaseWidget(OrionScene scene) {
            // When widget is released from scene, release the controllers as well.
            scene.releaseController(mTouchPincher);
            scene.releaseController(mTouchRotater);
            scene.releaseController(mRotationAligner);
        }
    }

    /**
     * Convenience class for constructing a simple video item gallery from a given path.
     */
    public class Gallery {

        /** The video items contained in the gallery. */
        List<GalleryItem> mGalleryItems = new ArrayList<>();

        /**
         * Constructor with video file path.
         *
         * @param videoPath The file path to scan for video files to be added to the gallery.
         */
        Gallery(String videoPath) {
            addVideosFromPath(videoPath, ".mp4");
        }

        /**
         * Get items contained in the gallery.
         *
         * @return A list of gallery items, or an empty list if none.
         */
        List<GalleryItem> getItems() {
            return mGalleryItems;
        }

        /**
         * Scan the given path for video files, create thumbnails, and add to video gallery.
         *
         * @param path The file system path to scan for video files.
         * @param filter The filename extension for recognizing videos from other files.
         */
        void addVideosFromPath(String path, String filter) {
            for (File file : new File(path).listFiles()) {
                String fileName = file.getName();
                if (fileName.endsWith(filter)) {
                    String filePath = path + fileName;
                    String thumbPath = filePath.replace(filter, ".jpg");
                    MainMenu.createThumbnailForVideo(
                            ThumbnailPager.this, filePath,
                            10000, // Grab a frame at 10s from the beginning for a thumbnail.
                            720,   // Scale thumbnails for 720 pixels high
                            thumbPath,
                            90);   // Save thumbnails as jpg files with quality level 90.
                    mGalleryItems.add(new GalleryItem(filePath, thumbPath));
                    Log.v(TAG, "Added file " + filePath + " with thumb " + thumbPath);
                }
            }
        }

        /** Data class that represents a gallery item. */
        class GalleryItem {
            String mContentUri;
            String mThumbUri;
            GalleryItem(String contentUri, String thumbnailUri) {
                mContentUri = contentUri;
                mThumbUri = thumbnailUri;
            }
        }
    }
}
