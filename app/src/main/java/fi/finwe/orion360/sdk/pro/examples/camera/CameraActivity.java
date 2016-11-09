package fi.finwe.orion360.sdk.pro.examples.camera;

import android.os.Bundle;
import fi.finwe.orion360.v3.OrionActivity;
import fi.finwe.orion360.v3.OrionView;
import fi.finwe.orion360.v3.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.v3.item.OrionCamera;

public class CameraActivity extends OrionActivity {
	// Logging tag, ready to be copy-pasted into any other class.
	public static final String TAG = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	protected OrionView 	mView;
	protected CameraScene	mScene;
	protected OrionCamera 	mDefaultCamera;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setSystemUiVisibility(false);
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		mScene = new CameraScene();
		
		mDefaultCamera = new OrionCamera();
		mDefaultCamera.setCameraProjection(OrionCamera.CameraProjection.ORTHOGRAPHIC);
		
		mView = (OrionView)findViewById(R.id.orion_view);
		mView.bindDefaultCamera(mDefaultCamera);
		mView.bindDefaultScene(mScene);
		mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL, OrionViewport.CoordinateType.FIXED_LANDSCAPE);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		mScene.getCameraTexture().play();
	}
	
	@Override
	public void onPause() {
		mScene.getCameraTexture().pause();
		
		super.onPause();
	}
}
