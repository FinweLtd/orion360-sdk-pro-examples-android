/*
 * Copyright (c) 2017, Finwe Ltd. All rights reserved.
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
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.controller.RotationAligner;
import fi.finwe.orion360.sdk.pro.controller.TouchPincher;
import fi.finwe.orion360.sdk.pro.controller.TouchRotater;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.widget.OrionWidget;
import fi.finwe.util.ContextUtil;

/**
 * An example of bindings for creating a player for tiled panoramas.
 * <p/>
 * Typically, a single OpenGL texture is used for holding a single panorama image or
 * video frame, and mapped onto a spherical surface for representation. However, if
 * you attempt to render an image whose resolution is larger than the particular
 * device's maximum texture size, Orion360 must automatically downscale the image to
 * make it fit inside a texture. This feature allows rendering images on all devices
 * without worrying about their texture size limits, but may result to quality loss
 * especially in the case of high-resolution panorama photos.
 * <p/>
 * Whether this is relevant for you depends - many old devices had their maximum
 * texture size limited to 2K (2048x2048) or 4K (4096x4096), although some new high
 * end devices have even 16K (16384x16384) textures. If you target for a large set of
 * devices or have very high-resolution images, then splitting the panorama image
 * (or video) into tiles allows you to overcome the texture size limitation by using
 * multiple textures to represent a single panorama image.
 * <p/>
 * Since full spherical images have 2:1 aspect ratio and textures are square (1:1),
 * you could easily double the resolution that can be rendered on screen simply by
 * splitting your image to left and right halves. For example, 4096x2048 image would
 * become left (2048x2048) and right (2048x2048) tiles that could be loaded into two
 * textures, and thus rendered without quality loss even on old devices whose texture
 * size limit is set to 2048x2048. Notice that while tiled panoramas solve this
 * problem, you still need to stay within the limits of the device's total texture memory!
 * <p/>
 * In addition to rendering higher resolution panoramas, tiling also allows faster
 * playback for remote content. The idea is that the player could download first tiles
 * that must be rendered to the direction where the user is currently looking at, and
 * download tiles in the opposite direction last. This makes the image appear on screen
 * much quicker compared to loading the whole panorama as a single image.
 * <p/>
 * Finally, by combining the both approaches you could even render gigapixel panoramas
 * by splitting them to a large set of tiles and then downloading the tiles to local
 * cache and further to GPU memory as textures based on where the user is currently
 * looking at. The user experience can be surprisingly smooth and allows amazing zoom
 * capability when a few layers of tiles (at different total resolution) are created.
 * <p/>
 * In this example, a panorama image is split to 2x2 tiles just as an example of handling
 * both rows and columns.
 * <p/>
 * Features:
 * <ul>
 * <li>Plays one hard-coded full spherical (360x180) equirectangular tiled photo
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Renders the photo using standard rectilinear projection
 * <li>Allows navigation with touch & movement sensors (if supported by HW) as follows:
 * <ul>
 * <li>Panning (gyro or swipe)
 * <li>Zooming (pinch)
 * <li>Tilting (pinch rotate)
 * </ul>
 * <li>Auto Horizon Aligner (AHL) feature straightens the horizon</li>
 * </ul>
 */
public class Tiled extends OrionActivity {

    /** The Android view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our panorama sphere will be added to. */
    protected OrionScene mScene;

    /** The panorama sphere where our video textures will be mapped to. */
    protected OrionPanorama mPanorama;

    /** The top-left image texture. */
    protected OrionTexture mPanoramaTextureTopLeft;

    /** The top-right image texture. */
    protected OrionTexture mPanoramaTextureTopRight;

    /** The bottom-left image texture. */
    protected OrionTexture mPanoramaTextureBottomLeft;

    /** The bottom-right image texture. */
    protected OrionTexture mPanoramaTextureBottomRight;

    /** The camera which will project our 3D scene to a 2D (view) surface. */
    protected OrionCamera mCamera;

    /** The widget that will handle our touch gestures. */
    protected TouchControllerWidget mTouchController;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene();

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindController(mOrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a doughnut video/image.
        mPanorama = new OrionPanorama();

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTextureTopLeft = OrionTexture.createTextureFromURI(this,
                MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_IMAGE_FILE_HQ_TILE_TL);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTextureTopRight = OrionTexture.createTextureFromURI(this,
                MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_IMAGE_FILE_HQ_TILE_TR);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTextureBottomLeft = OrionTexture.createTextureFromURI(this,
                MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_IMAGE_FILE_HQ_TILE_BL);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTextureBottomRight = OrionTexture.createTextureFromURI(this,
                MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_IMAGE_FILE_HQ_TILE_BR);

        // Bind the panorama texture to the panorama object.
        mPanorama.bindTexture(0, mPanoramaTextureTopLeft,
                new RectF(-180.0f, 90.0f, 0.0f, 0.0f),
                new RectF(0.0f, 1.0f, 1.0f, 0.0f));
        mPanorama.bindTexture(1, mPanoramaTextureTopRight,
                new RectF(0.0f, 90.0f, 180.0f, 0.0f),
                new RectF(0.0f, 1.0f, 1.0f, 0.0f));
        mPanorama.bindTexture(2, mPanoramaTextureBottomLeft,
                new RectF(-180.0f, 0.0f, 0.0f, -90.0f),
                new RectF(0.0f, 1.0f, 1.0f, 0.0f));
        mPanorama.bindTexture(3, mPanoramaTextureBottomRight,
                new RectF(0.0f, 0.0f, 180.0f, -90.0f),
                new RectF(0.0f, 1.0f, 1.0f, 0.0f));

        // Notice the coordinate system:
        //
        // First rect (sphereRectDeg) tells the spherical field-of-view span in degrees.
        // As an example, full spherical surface spans horizontally from -180 to +180 degrees
        // and vertically from -90 to +90 degrees. A doughnut shape video has full horizontal
        // span, but its vertical span does not reach down to the nadir nor up to the zenith,
        // leaving holes around the poles and thus requires a doughnut shape surface instead
        // of a sphere. Here we have divided a full spherical image to four equal size tiles
        // that each span horizontally 180 degrees and vertically 90 degrees (2x2 tiles).
        //
        // 1st param: horizontal span from center point to left in degrees
        // 2nd param: vertical span from center point to top in degrees
        // 3rd param: horizontal span from center point to right in degrees
        // 4th parama: vertical span from center point to bottom in degrees
        //
        // The second rect (textureRect) tells the part of the texture that will be mapped.
        // As an example, full texture spans horizontally from 0.0 to 1.0, and also vertically
        // from 0.0 to 1.0.
        //
        // 1st param: texture part left edge coordinate
        // 2nd param: texture part top edge coordinate
        // 3rd param: texture part right edge coordinate
        // 4th param: texture part bottom edge coordinate

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanorama);

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera();

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        mOrionContext.getSensorFusion().bindControllable(mCamera);

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
            mTouchPincher.setMinimumDistanceDp(mOrionContext.getActivity(), 20);
            mTouchPincher.bindControllable(mCamera, OrionCamera.VAR_FLOAT1_ZOOM);

            // Create drag-to-pan handler.
            mTouchRotater = new TouchRotater();
            mTouchRotater.bindControllable(mCamera);

            // Create the rotation aligner, responsible for rotating the view so that the horizon
            // aligns with the user's real-life horizon when the user is not looking up or down.
            mRotationAligner = new RotationAligner();
            mRotationAligner.setDeviceAlignZ(-ContextUtil.getDisplayRotationDegreesFromNatural(
                    mOrionContext.getActivity()));
            mRotationAligner.bindControllable(mCamera);

            // Rotation aligner needs sensor fusion data in order to do its job.
            mOrionContext.getSensorFusion().bindControllable(mRotationAligner);
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
