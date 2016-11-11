package fi.finwe.orion360.sdk.pro.examples.widget;

import android.os.Bundle;
import fi.finwe.log.Logger;
import fi.finwe.math.QuatF;
import fi.finwe.orion360.v3.OrionActivity;
import fi.finwe.orion360.v3.OrionContext;
import fi.finwe.orion360.v3.view.OrionView;
import fi.finwe.orion360.v3.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.common.BaseScene;
import fi.finwe.orion360.v3.item.OrionCamera;
import fi.finwe.orion360.v3.item.OrionSceneItem;
import fi.finwe.orion360.v3.item.OrionSceneItem.RotationBaseControllerListenerBase;
import fi.finwe.orion360.v3.source.OrionTexture;
import fi.finwe.orion360.v3.widget.SelectablePointerIcon;

public class SelectablePointerIconActivity extends OrionActivity {
	// Logging tag, ready to be copy-pasted into any other class.
	public static final String TAG = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	protected OrionView 	mView;
	protected BaseScene		mScene;
	protected OrionCamera 	mDefaultCamera;
	
	protected SelectablePointerIcon	mSPI;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setSystemUiVisibility(false);
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		OrionContext.getSensorFusion().setMagnetometerEnabled(false);
		mScene = new BaseScene();
		mScene.bindController(OrionContext.getSensorFusion());
		mScene.setSourceURI(this, "asset://Orion360_test_image_1920x960.jpg");
		
		mDefaultCamera = new OrionCamera();
		mDefaultCamera.setZoomMax(3.0f);
		mDefaultCamera.setRotationBaseControllerListener(new RotationBaseControllerListenerBase() {
			@Override
			public void onRotationBaseControllerBound(OrionSceneItem item, QuatF rotationBase) {
				super.onRotationBaseControllerBound(item, rotationBase);
				item.setRotationYaw(0);
				mScene.setVisible(true);
			}
		});
		OrionContext.getSensorFusion().bindControllable(mDefaultCamera);
		
		mSPI = new SelectablePointerIcon();
		mSPI.setUiThreadListener(new SelectablePointerIcon.Listener() {
			@Override
			public void onSelectionTrigger() {
				Logger.logD(TAG, "Trigger");
			}
			
			@Override
			public void onSelectionFocusLost() {
				Logger.logD(TAG, "Focus lost");
			}
			
			@Override
			public void onSelectionFocusGained() {
				Logger.logD(TAG, "Focus gained");
			}
		});
		// Set the location of the icon by rotating 0.9f * (0,0,-1) 
		mSPI.setLocationPolarZXYDeg(0,10,0,0.9f);	
		mSPI.setScale(0.15f, 0.2f);
		mSPI.getIcon().bindTexture(OrionTexture.createTextureFromURI(this, "asset://spi_icon.png"));
		mSPI.getPieSprite().bindTexture(OrionTexture.createTextureFromURI(this, "asset://spi_selection.png"));
//		mSPI.getFocusSprite().bindTexture(OrionTexture.createTextureFromURI(this, "asset://spi_selection.png"));
		mSPI.setPointer(mDefaultCamera);
		mSPI.setSelectionTriggerFrameCount(120);
		
		mScene.bindWidget(mSPI);
		
		mView = (OrionView)findViewById(R.id.orion_view);
		mView.bindDefaultCamera(mDefaultCamera);
		mView.bindDefaultScene(mScene);
		mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL, OrionViewport.CoordinateType.FIXED_LANDSCAPE);
	}
}
