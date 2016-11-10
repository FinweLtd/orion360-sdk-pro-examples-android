package fi.finwe.orion360;

import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import fi.finwe.math.QuatF;
import fi.finwe.orion360.v3.OrionActivity;
import fi.finwe.orion360.v3.OrionContext;
import fi.finwe.orion360.v3.OrionScene;
import fi.finwe.orion360.v3.OrionView;
import fi.finwe.orion360.v3.OrionViewport;
import fi.finwe.orion360.v3.item.OrionCamera;
import fi.finwe.orion360.v3.item.OrionPanorama;
import fi.finwe.orion360.v3.item.OrionSceneItem;
import fi.finwe.orion360.v3.source.OrionTexture;
import fi.finwe.orion360.v3.source.OrionVideoTexture;
import fi.finwe.orion360.v3.viewport.fx.BarrelDistortion;

/**
 * An activity that creates a simple Orion360 player configuration.
 *
 * Orion360 objects are private members to guarantee that they are created and in the correct order.
 * However, getters are provided for child classes for further configuration.
 */
public class SimpleOrionActivity extends OrionActivity implements OrionVideoTexture.Listener {

    /** Orion360 view where the content will be rendered. */
    private OrionView mView;

    /** Orion360 scene. */
    private OrionScene mScene;

    /** Orion360 camera for viewing content that is drawn into a 3D world. */
    private OrionCamera mCamera;

    /** Orion360 panorama represent a single (spherical) panorama content. */
    private OrionPanorama mPanorama;

    /** Orion360 texture encapsulates the content that will be wrapped to panorama surface. */
    private OrionTexture mTexture;

    /** Orion360 widget that handles touch controls. */
    private SimpleTouchController mTouchController;

    /** Flag for indicating if VR mode is currently enabled, or not. */
    private boolean mIsVRMode = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Some VR frames, such as Google Cardboard, may contain a magnetic switch
        // whose magnet will confuse sensor fusion -> we can usually disable magnetometer.
        OrionContext.getSensorFusion().setMagnetometerEnabled(false);

        // Create a new Orion360 scene.
        mScene = new OrionScene();

        // Bind the sensor fusion for the scene.
        mScene.bindController(OrionContext.getSensorFusion());

        // Sensor fusion will take a moment to stabilize. Don't show the scene until we are ready!
        mScene.setVisible(false);

        // Create a new Orion360 panorama.
        mPanorama = new OrionPanorama();

        // Bind the panorama to our scene.
        mScene.bindSceneItem(mPanorama);

        // Create a new camera, common to all viewports bound to this view.
        mCamera = new OrionCamera();

        // Configure zoom. The camera will never reduce the FOV below 1/3 (3.0f) of the base FOV.
        mCamera.setZoomMax(3.0f);

        // React to the camera getting bound to the SensorFusion for the first time.
        mCamera.setRotationBaseControllerListener(
                new OrionSceneItem.RotationBaseControllerListenerBase() {

            @Override
            public void onRotationBaseControllerBound(OrionSceneItem item, QuatF rotationBase) {

                // First accept the default behavior (insert the rotationBase in place as it is).
                super.onRotationBaseControllerBound(item, rotationBase);

                // Then set the yaw angle to 0. The camera will always point to the same direction
                // when starting the app, regardless of the orientation of the device.
                item.setRotationYaw(0);

                // The camera is now properly initialized. Allow rendering the scene.
                mScene.setVisible(true);

            }

        });

        // Make the sensor fusion rotate the camera.
        OrionContext.getSensorFusion().bindControllable(mCamera);

        // Create a touch controller widget (convenience class combining several OrionObjects),
        // and set it to control our camera as well.
        mTouchController = new SimpleTouchController(mCamera);

        // Bind the touch controller widget in our scene to enable it.
        mScene.bindWidget(mTouchController);

    }

    /**
     * Get Orion360 view.
     *
     * @return Orion360 view.
     */
    protected OrionView getOrionView() {
        return mView;
    }

    /**
     * Get Orion360 scene.
     *
     * @return Orion360 scene.
     */
    protected OrionScene getOrionScene() {
        return mScene;
    }

    /**
     * Get Orion360 camera.
     *
     * @return Orion360 camera.
     */
    protected OrionCamera getOrionCamera() {
        return mCamera;
    }

    /**
     * Get Orion360 panorama.
     *
     * @return Orion360 panorama.
     */
    protected OrionPanorama getOrionPanorama() {
        return mPanorama;
    }

    /**
     * Get Orion360 texture.
     *
     * @return Orion360 texture.
     */
    protected OrionTexture getOrionTexture() {
        return mTexture;
    }

    /**
     * Set Orion360 view in active XML layout to be used for rendering 360 content.
     *
     * @param resId The resource ID of the Orion360 view.
     */
    protected void setOrionView(int resId) {

        // Find the view by its resource ID.
        mView = (OrionView) findViewById(resId);
        if (null != mView) {

            // Bind camera to this view.
            mView.bindDefaultCamera(getOrionCamera());

            // Bind scene to this view.
            mView.bindDefaultScene(getOrionScene());

            // Bind viewports to this view.
            mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL,
                    OrionViewport.CoordinateType.FIXED_LANDSCAPE);

        }
    }

    /**
     * Set a URI to 360 content (image/video) that will be rendered with Orion360.
     *
     * @param uri The URI string for the 360 content.
     */
    public void setContentUri(String uri) {

        // Release and destroy old texture, if it exists.
        if (mTexture != null) {
            mPanorama.releaseTextures();
            mTexture.destroy();
        }

        // Create a texture from the given URI. This will become either image or video texture.
        mTexture = OrionTexture.createTextureFromURI(this, uri);

        // Bind the complete texture to the complete panorama sphere.
        mPanorama.bindTextureFull(0, mTexture);

        // In case of video texture, start listening for video player events.
        if (mTexture instanceof OrionVideoTexture) {
            ((OrionVideoTexture) mTexture).bindTextureListener(this);
        }

    }

    /**
     * Set VR mode enabled or disabled.
     *
     * @param enabled Set true to enable VR mode, or false to return to normal mode.
     */
    public void setVRMode(boolean enabled) {

        if (enabled) {

            // Bind the complete texture for left and right eyes.
            mPanorama.bindTextureVR(0, mTexture, new RectF(-180, 90, 180, -90),
                    OrionPanorama.TEXTURE_RECT_FULL, OrionPanorama.TEXTURE_RECT_FULL);

            // Release all existing viewports.
            OrionViewport [] viewports = mView.getViewports();
            for (OrionViewport vp : viewports) {
                mView.releaseViewport(vp);
            }

            // Set up two new viewports side by side (when looked from landscape orientation).
            mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_SPLIT_HORIZONTAL,
                    OrionViewport.CoordinateType.FIXED_LANDSCAPE);

            // Designate each viewport to render content for either left or right eye.
            mView.getViewports()[0].setVRMode(OrionViewport.VRMode.VR_LEFT);
            mView.getViewports()[1].setVRMode(OrionViewport.VRMode.VR_RIGHT);

            // Compensate for VR frame lens distortion.
            BarrelDistortion barrelFx = new BarrelDistortion();
            barrelFx.setDistortionFillScale(1.0f);
            barrelFx.setDistortionCenterOffset(0,0);
            barrelFx.setDistortionCoeffs(new float[] { 1.0f, 0.39f, -0.35f, 0.19f} );
            mView.getViewports()[0].bindFX(barrelFx);
            mView.getViewports()[1].bindFX(barrelFx);

            // Re-configure camera.
            OrionCamera camera = getOrionCamera();
            camera.setVRCameraDistance(0.035f);
            camera.setVRCameraFocalDistance(1.5f);
            camera.setZoom(1.0f);
            camera.setZoomMax(1.0f);

            // We need to hide the navigation bar, else this will be visible for the right eye.
            hideNavigationBar();

            // VR is still new for many users, hence they should be educated what this feature is
            // and how to use it, e.g. an animation about putting the device inside a VR frame.
            // Here we simply show a notification.
            Toast.makeText(this, "Please put the device inside a VR frame",
                    Toast.LENGTH_LONG).show();

            // Remember new mode.
            mIsVRMode = true;

        } else {

            // Bind the complete texture to the complete panorama sphere.
            mPanorama.bindTextureFull(0, mTexture);

            // Release all existing viewports.
            OrionViewport [] viewports = mView.getViewports();
            for (OrionViewport vp : viewports) {
                mView.releaseViewport(vp);
            }

            // Bind one new viewport.
            mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL,
                    OrionViewport.CoordinateType.FIXED_LANDSCAPE);

            // Re-configure camera.
            OrionCamera camera = getOrionCamera();
            camera.setZoom(1.0f);
            camera.setZoomMax(3.0f);

            // Show the navigation bar again.
            showNavigationBar();

            // Remember new mode.
            mIsVRMode = false;

        }
    }

    /**
     * Check if VR mode is currently enabled, or not.
     *
     * @return true if VR mode is enabled, else false.
     */
    public boolean isVRModeEnabled() {
        return mIsVRMode;
    }

    @Override
    public void onInvalidURI(OrionTexture orionTexture) {

    }

    @Override
    public void onSourceURIChanged(OrionTexture orionTexture) {

    }

    @Override
    public void onVideoPrepared(OrionVideoTexture orionVideoTexture) {

    }

    @Override
    public void onVideoRenderingStart(OrionVideoTexture orionVideoTexture) {

    }

    @Override
    public void onVideoStarted(OrionVideoTexture orionVideoTexture) {

    }

    @Override
    public void onVideoPaused(OrionVideoTexture orionVideoTexture) {

    }

    @Override
    public void onVideoStopped(OrionVideoTexture orionVideoTexture) {

    }

    @Override
    public void onVideoCompleted(OrionVideoTexture orionVideoTexture) {

    }

    @Override
    public void onVideoSeekStarted(OrionVideoTexture orionVideoTexture, long l) {

    }

    @Override
    public void onVideoSeekCompleted(OrionVideoTexture orionVideoTexture, long l) {

    }

    @Override
    public void onVideoPositionChanged(OrionVideoTexture orionVideoTexture, long l) {

    }

    @Override
    public void onVideoDurationUpdate(OrionVideoTexture orionVideoTexture, long l) {

    }

    @Override
    public void onVideoSizeChanged(OrionVideoTexture orionVideoTexture, int i, int i1) {

    }

    @Override
    public void onVideoBufferingStart(OrionVideoTexture orionVideoTexture) {

    }

    @Override
    public void onVideoBufferingEnd(OrionVideoTexture orionVideoTexture) {

    }

    @Override
    public void onVideoBufferingUpdate(OrionVideoTexture orionVideoTexture, int i, int i1) {

    }

    @Override
    public void onVideoError(OrionVideoTexture orionVideoTexture, int i, int i1) {

    }

    @Override
    public void onVideoInfo(OrionVideoTexture orionVideoTexture, int i, String s) {

    }

    /**
     * Hide navigation bar.
     */
    public void hideNavigationBar() {
        View v = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT < 19) {
            v.setSystemUiVisibility(View.GONE);
        } else {
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            v.setSystemUiVisibility(uiOptions);
        }
    }

    /**
     * Show navigation bar.
     */
    public void showNavigationBar() {
        View v = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT < 19) {
            v.setSystemUiVisibility(View.VISIBLE);
        } else {
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            v.setSystemUiVisibility(uiOptions);
        }
    }

}
