package fi.finwe.orion360.sdk.pro.examples.source;

import android.os.Bundle;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.common.BaseScene;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionCamera.CameraProjection;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama.PanoramaType;

public class SourceActivity extends OrionActivity {
	// Logging tag, ready to be copy-pasted into any other class.
	public static final String TAG = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	protected OrionView 	mView;
	protected BaseScene		mScene;
	protected OrionCamera 	mDefaultCamera;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mScene = new BaseScene();
		mScene.setSourceURI(this, "asset://Orion360_test_image_1920x960.jpg");
		mScene.getPanorama().setPanoramaType(PanoramaType.PANEL_SOURCE);
		mScene.setVisible(true);	// In BaseScene we se the visibility to false. No such need in this example.
		
		// Create the default camera, common to all viewports bound to this view
		mDefaultCamera = new OrionCamera();
		mDefaultCamera.setZoomMax(3.0f);
		mDefaultCamera.setCameraProjection(CameraProjection.ORTHOGRAPHIC);
		
		mView = (OrionView)findViewById(R.id.orion_view);
		mView.bindDefaultCamera(mDefaultCamera);
		mView.bindDefaultScene(mScene);
		mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL, OrionViewport.CoordinateType.FIXED_LANDSCAPE);
		
	}
}
