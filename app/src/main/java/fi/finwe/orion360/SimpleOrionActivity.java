package fi.finwe.orion360;

import android.os.Bundle;

import fi.finwe.math.QuatF;
import fi.finwe.orion360.sdk.pro.examples.R;
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

public class SimpleOrionActivity extends OrionActivity implements OrionVideoTexture.Listener {

    protected OrionView mOrionView;
    protected OrionScene mDefaultScene;
    protected OrionCamera mDefaultCamera;
    protected SimpleTouchController mTouchController;

    protected OrionPanorama mPanorama;
    protected OrionTexture mPanoramaTexture;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDefaultScene = new OrionScene();
        mDefaultScene.bindController(OrionContext.getSensorFusion());
        mDefaultScene.setVisible(false); // Don't show the scene until we are ready.


        // Create the OrionPanorama
        mPanorama = new OrionPanorama();
        mDefaultScene.bindSceneItem(mPanorama);

        // Create the default camera, common to all viewports bound to this view
        mDefaultCamera = new OrionCamera();
        // 3.0 -> The camera will never reduce the FOV below 1/3 of the base FOV
        mDefaultCamera.setZoomMax(3.0f);
        // Set a listener to react to the camera getting bound to the SensorFusion for the first time
        mDefaultCamera.setRotationBaseControllerListener(new OrionSceneItem.RotationBaseControllerListenerBase() {
            @Override
            public void onRotationBaseControllerBound(OrionSceneItem item, QuatF rotationBase) {
                // First accept the default behavior (insert the rotationBase in place as it is)
                super.onRotationBaseControllerBound(item, rotationBase);
                // Then set the yaw to 0. The camera will always point to the same direction when starting the app,
                // regardless of the orientation of the device.
                item.setRotationYaw(0);
                // The camera is now properly intialized. Allow rendering the scene.
                mDefaultScene.setVisible(true);
            }
        });
        // Make the sensor fusion rotate the camera
        OrionContext.getSensorFusion().bindControllable(mDefaultCamera);

        // Create a touch controller widget (convenience class combining several OrionObjects), set it to control our camera
        mTouchController = new SimpleTouchController(mDefaultCamera);
        // Bind the touch controller widget in our scene to enable it
        mDefaultScene.bindWidget(mTouchController);
    }

    protected OrionCamera getDefaultCamera() {
        return mDefaultCamera;
    }

    protected OrionScene getDefaultScene() {
        return mDefaultScene;
    }

    public void setOrionView(int resId) {
        mOrionView = (OrionView) findViewById(resId);
        if (null != mOrionView) {
            mOrionView.bindDefaultCamera(getDefaultCamera());
            mOrionView.bindDefaultScene(getDefaultScene());
            mOrionView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL,
                    OrionViewport.CoordinateType.FIXED_LANDSCAPE);
        }
    }

    public void setContentUri(String uri) {
        mPanoramaTexture = OrionTexture.createTextureFromURI(this, uri);
        mPanorama.bindTextureFull(0, mPanoramaTexture);

        if (mPanoramaTexture instanceof OrionVideoTexture) {
            ((OrionVideoTexture) mPanoramaTexture).bindTextureListener(this);
        }
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
}
