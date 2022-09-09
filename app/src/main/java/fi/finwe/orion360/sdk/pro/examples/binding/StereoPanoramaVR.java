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

import android.graphics.RectF;
import android.os.Bundle;

import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;
import fi.finwe.orion360.sdk.pro.viewport.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;

/**
 * An example of bindings for creating a VR player for stereoscopic full spherical videos.
 * <p/>
 * See VR examples for a more complete implementation with barrel distortion compensation, etc.
 * <p/>
 * Features:
 * <ul>
 * <li>Plays one hard-coded full spherical (360x180) equirectangular video
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Auto-starts playback on load and stops when playback is completed
 * <li>Renders the video using standard rectilinear projection
 * <li>Allows navigation with movement sensors (if supported by HW) as follows:
 * <ul>
 * <li>Panning (gyro)
 * </ul>
 * </ul>
 */
public class StereoPanoramaVR extends OrionActivity {

    /** The Android view where our 3D scene (OrionView) will be added to. */
    protected OrionViewContainer mViewContainer;

    /** The Orion360 SDK view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our panorama sphere will be added to. */
    protected OrionScene mScene;

    /** The panorama sphere where our video texture will be mapped to. */
    protected OrionPanorama mPanorama;

    /** The video texture where our decoded video frames will be updated to. */
    protected OrionTexture mPanoramaTexture;

    /** The camera which will project our 3D scene to a 2D (view) surface. */
    protected OrionCamera mCamera;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene(mOrionContext);

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindRoutine(mOrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanorama = new OrionPanorama(mOrionContext);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = OrionTexture.createTextureFromURI(mOrionContext, this,
                MainMenu.PRIVATE_ASSET_FILES_PATH +
                        MainMenu.TEST_IMAGE_FILE_LIVINGROOM_OU_MQ);

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular stereoscopic over-and-under source, and wrap the top half of the
        // texture around left eye sphere and bottom half of the texture around right eye sphere.
        // Notice that we call bindTextureVR variant to define different texture based on whether
        // it's rendered to VR_LEFT or VR_RIGHT enabled OrionViewport.
        //noinspection SuspiciousNameCombination
        mPanorama.bindTextureVR(0, mPanoramaTexture,
                new RectF(-180, 90, 180, -90), // Full spherical texture
                OrionPanorama.TEXTURE_RECT_HALF_TOP,            // Left eye gets the top half
                OrionPanorama.TEXTURE_RECT_HALF_BOTTOM);        // Right eye gets the bottom half

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanorama);

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera(mOrionContext);

        // Reset view to the 'front' direction (horizontal center of the panorama).
        mCamera.setDefaultRotationYaw(0);

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        mOrionContext.getSensorFusion().bindControllable(mCamera);

        // Find Orion360 view container from the XML layout. This is an Android view for content.
        mViewContainer = (OrionViewContainer)findViewById(R.id.orion_view_container);

        // Create a new OrionView and bind it into the container.
        mView = new OrionView(mOrionContext);
        mViewContainer.bindView(mView);

        // Bind the scene to the view. This is the 3D world that we will be rendering to this view.
        mView.bindDefaultScene(mScene);

        // Bind the camera to the view. We will look into the 3D world through this camera.
        mView.bindDefaultCamera(mCamera);

        // In VR mode we have one viewport per eye, hence we use horizontal split viewport layout.
        mView.bindViewports(OrionDisplayViewport.VIEWPORT_CONFIG_SPLIT_HORIZONTAL,
                OrionDisplayViewport.CoordinateType.FIXED_LANDSCAPE);

        // Since we have stereo content i.e. left and right eye images are actually *different*,
        // splitting the view to two viewports is not enough - we also need to tell which one
        // should render the left eye image and which one the right eye image.
        // Notice that it is VERY important for a proper 3D effect that this mapping is correct!
        // If you are not sure, just try them both in VR mode; it is easy to spot the right way.
        mView.getViewports()[0].setVRMode(OrionViewport.VRMode.VR_LEFT);
        mView.getViewports()[1].setVRMode(OrionViewport.VRMode.VR_RIGHT);

        // Since some VR frames (like Google Cardboard) allow placing the device inside either
        // left side down or right side down, you have these options:
        // - instruct users which way is correct for a proper 3D effect
        // - configure the activity to use sensorLandscape orientation in the manifest file
        // - read from the accelerometer sensor which side is down and switch eye mapping manually

        // Ensure that all of your stereo content follows the same layout. The standard way:
        // - Over-and-under: top part is for left eye, bottom part is for right eye
        // - Side-by-side: left part is for left eye, right part is for right eye

        // Notice that if you want to temporarily disable the 3D effect from stereo panoramas
        // (for example if you need to render something monoscopic on top) you can do this by
        // configuring both viewports as VR_LEFT (or VR_RIGHT, just use same value for both).
    }
}
