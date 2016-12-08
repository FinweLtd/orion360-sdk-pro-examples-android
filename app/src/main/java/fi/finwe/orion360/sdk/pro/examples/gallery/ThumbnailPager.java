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
import fi.finwe.util.ContextUtil;

/**
 * An example of a simple video gallery.
 * <p/>
 * Features:
 * <ul>
 * <li>Loads one hard-coded 360 panorama image in .jpg format from file system
 * <li>Player one hard-coded planar rectilinear video</li>
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Renders the panorama and the model using standard rectilinear projection
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

    protected final static String MEDIA_FOLDER_PATH = MainMenu.PUBLIC_EXTERNAL_MOVIES_ORION_PATH;

    /** Request code for file read permission. */
    private static final int REQUEST_READ_STORAGE = 111;

    /** The Android view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our panorama and sprite will be added to. */
    protected OrionScene mScene;

    /** The panorama sphere where our image texture will be mapped to. */
    protected OrionPanorama mPanorama;

    /** The image texture where our decoded image will be updated to. */
    protected OrionTexture mPanoramaTexture;

    /** The sprite where our video texture will be mapped to. */
    protected OrionSprite mSprite;

    /** The video texture where our decoded video frames will be updated to. */
    protected OrionTexture mSpriteTexture;

    /** The camera which will project our 3D scene to a 2D (view) surface. */
    protected OrionCamera mCamera;

    /** The widget that will handle our touch gestures. */
    protected TouchControllerWidget mTouchController;

    protected Gallery mGallery;

    public class Gallery {
        List<GalleryItem> mGalleryItems;

        class GalleryItem {
            String mContentUri;
            String mThumbUri;
            GalleryItem(String contentUri, String thumbnailUri) {
                mContentUri = contentUri;
                mThumbUri = thumbnailUri;
            }
        }

        Gallery(String videoPath) {
            mGalleryItems = new ArrayList<>();
            addVideosFromPath(videoPath, ".mp4");
        }

        List<GalleryItem> getItems() {
            return mGalleryItems;
        }

        void addVideosFromPath(String path, String filter) {
            for (File file : new File(path).listFiles()) {
                String fileName = file.getName();
                if (fileName.endsWith(filter)) {
                    String filePath = path + fileName;
                    String thumbPath = filePath.replace(filter, ".jpg");
                    MainMenu.createThumbnailForVideo(
                            ThumbnailPager.this, filePath, 10000, 720, thumbPath, 90);
                    mGalleryItems.add(new GalleryItem(filePath, thumbPath));
                    Log.d(TAG, "Added file " + filePath + " with thumb " + thumbPath);
                    break;
                }
            }
        }
    }

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

        // Create a content gallery from target path media files.
        createGallery(MEDIA_FOLDER_PATH);
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

                    // Create a content gallery from target path media files.
                    createGallery(MEDIA_FOLDER_PATH);
                }
                return;
            }
            default:
                break;
        }
    }

    protected void createGallery(String path) {

        // Create a new gallery for media content.
        mGallery = new Gallery(path);

        // Initialize Orion360 with a content gallery.
        initOrion();

    }

    protected void initOrion() {

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene();

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindController(OrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanorama = new OrionPanorama();

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = OrionTexture.createTextureFromURI(this,
                MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_IMAGE_FILE_LIVINGROOM_HQ);

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mPanorama.bindTextureFull(0, mPanoramaTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanorama);

        // Create a new sprite. This is a 2D plane in the 3D world for our planar video.
        mSprite = new OrionSprite();

        // Create a new video (or image) texture from a video (or image) source URI.
        mSpriteTexture = OrionTexture.createTextureFromURI(this,
                mGallery.getItems().get(0).mThumbUri);

        // Set sprite location in the 3D world. Here we place the video on the white screen.
        mSprite.setWorldTranslation(new Vec3F(0.03f, 0.19f, -0.77f));

        // Set sprite size in the 3D world. Here we make it fit on the white screen.
        mSprite.setScale(0.42f);

        // Bind the sprite texture to the sprite object. Here we assume planar rectilinear source.
        mSprite.bindTexture(mSpriteTexture);

        // Bind the sprite to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mSprite);

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera();

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        OrionContext.getSensorFusion().bindControllable(mCamera);

        // Create a new touch controller widget (convenience class), and let it control our camera.
        mTouchController = new TouchControllerWidget(mCamera);

        // Bind the touch controller widget to the scene. This will make it functional in the scene.
        mScene.bindWidget(mTouchController);

        // Find Orion360 view from the XML layout. This is an Android view where we render content.
        mView = (OrionView)findViewById(R.id.orion_view);

        // Bind the scene to the view. This is the 3D world that we will be rendering to this view.
        mView.bindDefaultScene(mScene);

        // Bind the camera to the view. We will look into the 3D world through this camera.
        mView.bindDefaultCamera(mCamera);

        // The view can be divided into one or more viewports. For example, in VR mode we have one
        // viewport per eye. Here we fill the complete view with one (landscape) viewport.
        mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL,
                OrionViewport.CoordinateType.FIXED_LANDSCAPE);
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
}
