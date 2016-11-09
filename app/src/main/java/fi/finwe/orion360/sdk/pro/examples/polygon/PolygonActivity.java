package fi.finwe.orion360.sdk.pro.examples.polygon;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import fi.finwe.math.QuatF;
import fi.finwe.math.Vec3F;
import fi.finwe.orion360.v3.OrionActivity;
import fi.finwe.orion360.v3.OrionContext;
import fi.finwe.orion360.v3.OrionView;
import fi.finwe.orion360.v3.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.BaseScene;
import fi.finwe.orion360.sdk.pro.examples.TouchControllerWidget;
import fi.finwe.orion360.v3.item.OrionCamera;
import fi.finwe.orion360.v3.item.OrionPolygon;
import fi.finwe.orion360.v3.item.OrionSceneItem;
import fi.finwe.orion360.v3.item.OrionSceneItem.RotationBaseControllerListenerBase;

public class PolygonActivity extends OrionActivity {

	protected OrionView 			mView;
	protected BaseScene				mScene;
	protected OrionCamera 			mDefaultCamera;
	protected TouchControllerWidget mTouchController;
	
	private OrionPolygon	mPolygon = null;
	
	private Timer mTimer = null; 
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setSystemUiVisibility(false);
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		OrionContext.getSensorFusion().setMagnetometerEnabled(false);
		mScene = new BaseScene();
		mScene.bindController(OrionContext.getSensorFusion());
		mScene.setSourceURI(this, "asset://Orion360_test_image_1920x960.jpg");
		mScene.getPanorama().setScale(2.0f);// Make sure the polygon model won't overlap with the panorama background
		
//		mScene.getPanorama().setActive(false);
		
		mPolygon = new OrionPolygon();
//		mPolygon.setSourceURI("asset://kuutio.obj", "*");
//		mPolygon.setSourceURI("asset://tekstuurikuutio_2.obj", "*");
		mPolygon.setWorldTranslation(new Vec3F(0,0,-0.7f));
//		mPolygon.setScale(0.2f);
		mPolygon.setSourceURI("asset://vanille_obj.obj", "*");
		mPolygon.setScale(0.01f);
		// TODO: Alpha with polygons doesn't work right, and won't work without solving a major rendering problem.
//		mPolygon.setAmpAlpha(0.5f);	
//		mPolygon.setObjectTransform("*", true, Matrix44f.IDENTITY);
		mScene.bindSceneItem(mPolygon);
		
		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {
			private int mRotation = 0;
			
			@Override
			public void run() {
				mRotation++;
				QuatF yrot = QuatF.fromRotationAxisY((float)((Math.PI) * Math.sin(mRotation / 60.f)));
				QuatF xrot = QuatF.fromRotationAxisX((float)((0.8f * Math.PI/2) * Math.sin(0.33f * mRotation / 60.f)));
				mPolygon.setRotation(xrot.multiply(yrot));
			}
		}, 0, 16);
		
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
		
		mTouchController = new TouchControllerWidget(mDefaultCamera);
		mScene.bindWidget(mTouchController);
		
		mView = (OrionView)findViewById(R.id.orion_view);
		mView.bindDefaultCamera(mDefaultCamera);
		mView.bindDefaultScene(mScene);
		mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL, OrionViewport.CoordinateType.FIXED_LANDSCAPE);
	}
	
	@Override
	public void onDestroy() {
		mTimer.cancel();
		
		super.onDestroy();
	}

}
