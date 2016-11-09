package fi.finwe.orion360.sdk.pro.examples.audio;

import android.os.Bundle;
import fi.finwe.math.QuatF;
import fi.finwe.orion360.v3.OrionActivity;
import fi.finwe.orion360.v3.OrionContext;
import fi.finwe.orion360.v3.OrionScene;
import fi.finwe.orion360.v3.OrionView;
import fi.finwe.orion360.v3.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.TouchControllerWidget;
import fi.finwe.orion360.v3.item.OrionCamera;
import fi.finwe.orion360.v3.item.OrionPanorama;
import fi.finwe.orion360.v3.item.OrionSceneItem;
import fi.finwe.orion360.v3.item.OrionSceneItem.RotationBaseControllerListenerBase;
import fi.finwe.orion360.v3.source.OrionTexture;

public class FB360TBEActivity extends OrionActivity {
	// Logging tag, ready to be copy-pasted into any other class.
	public static final String TAG = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	protected OrionView 			mView;
	protected OrionScene			mScene;
	protected OrionCamera 			mDefaultCamera;
	protected TouchControllerWidget mTouchController;
	
	protected OrionPanorama 		mPanorama;
	protected OrionTexture			mPanoramaTexture;
	
	int								mAudioDecoderID;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setSystemUiVisibility(false);
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		OrionContext.getSensorFusion().setMagnetometerEnabled(false);
		mScene = new OrionScene();
		mScene.bindController(OrionContext.getSensorFusion());
		// Don't show the scene until we are ready.
		mScene.setVisible(false);
		
//		mPanoramaTexture = OrionTexture.createTextureFromURI(this, "asset://Orion360_test_image_1920x960.jpg");
		mPanoramaTexture = OrionTexture.createTextureFromURI(this, "asset://Orion360_HD1080.jpg");
		
		// Create the OrionPanorama
		mPanorama = new OrionPanorama();
		mPanorama.bindTextureFull(0, mPanoramaTexture);
		mScene.bindSceneItem(mPanorama);
		
		// Create the default camera, common to all viewports bound to this view
		mDefaultCamera = new OrionCamera();
		// 3.0 -> The camera will never reduce the FOV below 1/3 of the base FOV
		mDefaultCamera.setZoomMax(3.0f);
		// Set a listener to react to the camera getting bound to the SensorFusion for the first time
		mDefaultCamera.setRotationBaseControllerListener(new RotationBaseControllerListenerBase() {
			@Override
			public void onRotationBaseControllerBound(OrionSceneItem item, QuatF rotationBase) {
				// First accept the default behavior (insert the rotationBase in place as it is)
				super.onRotationBaseControllerBound(item, rotationBase);
				// Then set the yaw to 0. The camera will always point to the same direction when starting the app, 
				// regardless of the orientation of the device. 
				item.setRotationYaw(0);
				// The camera is now properly intialized. Allow rendering the scene.
				mScene.setVisible(true);
			}
		});
		// Make the sensor fusion rotate the camera
		OrionContext.getSensorFusion().bindControllable(mDefaultCamera);
		
		// Create a touch controller widget (convenience class combining several OrionObjects), set it to control our camera
		mTouchController = new TouchControllerWidget(mDefaultCamera);
		// Bind the touch controller widget in our scene to enable it
		mScene.bindWidget(mTouchController);
		
		mView = (OrionView)findViewById(R.id.orion_view);
		// When bound to OrionView with bindDefaultCamera, the camera is used for all OrionViewports that don't have a different camera bound
		mView.bindDefaultCamera(mDefaultCamera);
		// When bound to OrionView with bindDefaultScene, the scene is used for all OrionViewports that don't have a different scene bound
		mView.bindDefaultScene(mScene);
		// Set up one fullscreen viewport
		mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL, OrionViewport.CoordinateType.FIXED_LANDSCAPE);
		
		boolean success = OrionContext.getAudioEngine().init("FB360_TBE");
		if (success) {
			mAudioDecoderID = OrionContext.getAudioEngine().createDecoder("asset://Radio.tbe");
			OrionContext.getAudioEngine().startDecoder(mAudioDecoderID);
		}
	}
	

}
