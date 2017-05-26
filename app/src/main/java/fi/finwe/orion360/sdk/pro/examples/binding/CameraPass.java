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

package fi.finwe.orion360.sdk.pro.examples.binding;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionSceneItem;
import fi.finwe.orion360.sdk.pro.item.sprite.OrionSprite;
import fi.finwe.orion360.sdk.pro.source.OrionCameraTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;

/**
 * An example of bindings for creating a camera pass-through with device's back camera.
 * <p/>
 * Features:
 * <ul>
 * <li>Plays video stream from device's back camera into an OrionSprite surface.
 * </ul>
 */
public class CameraPass extends OrionActivity {

    /** Tag for logging. */
    public static final String TAG = CameraPass.class.getSimpleName();

    /** Request code for camera access permission. */
    private static final int REQUEST_CAMERA = 112;

    /** The Android view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our camera sprite will be added to. */
    protected OrionScene mScene;

    /** The sprite where our video texture will be mapped to. */
    protected OrionSprite mCameraSprite;

    /** The video texture where our HW camera preview frames will be updated to. */
    protected OrionCameraTexture mCameraTexture;

    /** The camera which will project our 3D scene to a 2D (view) surface. */
    protected OrionCamera mCamera;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // In case we want to access device's hardware camera on Android 6.0 or above, we must
        // ensure that CAMERA permission is granted *before* attempting to capture frames with it.
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest
                .permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            // Camera permission has not been granted. As user can give the permission when
            // requested, the operation now becomes asynchronous: we must wait for user's
            // decision, and act when we receive a callback.
            ActivityCompat.requestPermissions(this, new String [] {
                    Manifest.permission.CAMERA }, REQUEST_CAMERA);

        } else {

            // Camera permission is already granted, let's go ahead and initialize Orion360.
            initOrion();

        }
	}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String [] permissions,
                                           @NonNull int [] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CAMERA: {

                // User has now answered to our read permission request. Let's see how:
                if (grantResults.length == 0 || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Camera permission was denied by user");

                    // Bail out with a notification for user.
                    Toast.makeText(this, R.string.player_camera_permission_denied,
                            Toast.LENGTH_LONG).show();

                } else {
                    Log.i(TAG, "Camera permission was granted by user");

                    // Camera is accessible, let's go ahead and initialize Orion360.
                    initOrion();

                }
                return;
            }
            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCameraTexture != null) {

            // Start providing frames from hardware camera.
            mCameraTexture.play();

        }
    }

    @Override
    public void onPause() {
        if (mCameraTexture != null) {

            // Stop providing frames from hardware camera.
            mCameraTexture.pause();

        }

        super.onPause();
    }

    /**
     * Initialize Orion360 as a hardware camera preview.
     */
    protected void initOrion() {

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene();

        // Create a new sprite. This is a 3D object with a flat 2D surface for an image/video.
        mCameraSprite = new OrionSprite();

        // We don't need perspective camera for viewing camera sprite.
        mCameraSprite.setRenderingMode(OrionSceneItem.RenderingMode.CAMERA_DISABLED);

        // Create a new texture where frames will be captured from device's hardware camera.
        mCameraTexture = new OrionCameraTexture(OrionCameraTexture.CameraFacing.BACK);

        // Bind the camera texture to the camera sprite.
        mCameraSprite.bindTexture(mCameraTexture);

        // Bind the sprite to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mCameraSprite);

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera();

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

}
