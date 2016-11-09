package fi.finwe.orion360.sdk.pro.examples.sharedvar;

import android.os.Bundle;
import fi.finwe.log.Logger;
import fi.finwe.math.QuatF;
import fi.finwe.math.Vec2F;
import fi.finwe.math.Vec3F;
import fi.finwe.orion360.v3.OrionActivity;
import fi.finwe.orion360.v3.OrionContext;
import fi.finwe.orion360.v3.OrionScene;
import fi.finwe.orion360.v3.OrionView;
import fi.finwe.orion360.v3.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.v3.animator.TimedFloatAnimator;
import fi.finwe.orion360.v3.animator.TimedFloatAnimator.WrappingMode;
import fi.finwe.orion360.v3.controllable.DisplayClickable;
import fi.finwe.orion360.v3.controller.Clicker.ClickType;
import fi.finwe.orion360.v3.controller.TouchDisplayClickListener;
import fi.finwe.orion360.v3.controller.TouchDisplayClickListener.Listener;
import fi.finwe.orion360.sdk.pro.examples.common.BaseScene;
import fi.finwe.orion360.v3.item.OrionCamera;
import fi.finwe.orion360.v3.item.OrionPanorama;
import fi.finwe.orion360.v3.item.OrionSceneItem;
import fi.finwe.orion360.v3.item.OrionSceneItem.RotationBaseControllerListenerBase;
import fi.finwe.orion360.v3.item.sprite.OrionSprite;
import fi.finwe.orion360.v3.source.OrionTexture;

public class SharedVariableActivity extends OrionActivity {
	// Logging tag, ready to be copy-pasted into any other class.
	public static final String TAG = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	protected OrionView 			mView;
	protected OrionScene			mScene;
	protected OrionCamera 			mDefaultCamera;
	
	protected OrionPanorama 		mPanorama1;
	protected OrionTexture			mPanoramaTexture1;
	protected OrionPanorama 		mPanorama2;
	protected OrionTexture			mPanoramaTexture2;
	
	protected OrionSprite			mSprite;
	
	protected TimedFloatAnimator 	mAlphaAnimator;
	
	protected TimedFloatAnimator	mScaleAnimator;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setSystemUiVisibility(false);
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		OrionContext.getSensorFusion().setMagnetometerEnabled(false);
		mScene = new BaseScene();
		mScene.bindController(OrionContext.getSensorFusion());
		
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
		
		mView = (OrionView)findViewById(R.id.orion_view);
//		mView.setTouchInputClickEnabled(ClickType.DOUBLE, false);
		mView.setTouchInputClickEnabled(ClickType.LONG, false);
		mView.bindDefaultCamera(mDefaultCamera);
		mView.bindDefaultScene(mScene);
		mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL, OrionViewport.CoordinateType.FIXED_LANDSCAPE);
		
		// Create the OrionPanoramas
		mPanorama1 = new OrionPanorama();
		mPanorama1.setScale(1.0f);
		mPanoramaTexture1 = OrionTexture.createTextureFromURI(this, "asset://Orion360_test_image_1920x960.jpg");
		mPanorama1.bindTextureFull(0, mPanoramaTexture1);
		mScene.bindSceneItem(mPanorama1);
		
		mPanorama2 = new OrionPanorama();
		mPanorama2.setScale(0.9f);
		mPanoramaTexture2 = OrionTexture.createTextureFromURI(this, "asset://Orion360_HD1080.jpg");
//		mPanoramaTexture2 = new OrionVideoTexture(this, "http://wowzaprod101-i.akamaihd.net/hls/live/256300/bd35fe39/playlist.m3u8");
		mPanorama2.bindTextureFull(0, mPanoramaTexture2);
		mScene.bindSceneItem(mPanorama2);
		
		mSprite =  new OrionSprite();
		mSprite.bindTexture(OrionTexture.createTextureFromURI(this, "asset://spi_icon.png"));
		mSprite.setScale(0.2f);
		mSprite.setWorldTranslation(new Vec3F(0,0,-0.8f));
		mScene.bindSceneItem(mSprite);
		
		mAlphaAnimator = TimedFloatAnimator.fromRange(0.0f, 1.0f, TimedFloatAnimator.Function.LINEAR);
		mAlphaAnimator.setDurationMs(2000);
		mAlphaAnimator.setListener(mAnimatorListener);
		mPanorama2.bindSharedVariable(OrionSceneItem.VAR_FLOAT1_AMP_ALPHA, mAlphaAnimator);
		
		mScaleAnimator = TimedFloatAnimator.fromCoeffs(0.3f, 0.1f, TimedFloatAnimator.Function.SIN_360);
		mScaleAnimator.setInputRange(0.25f, 0.75f);
		mScaleAnimator.setDurationMs(2000);
		mScaleAnimator.setWrappingMode(WrappingMode.REPEAT);
		mScaleAnimator.animateFromCurrentPhase(true);
		mSprite.bindSharedVariable(OrionSceneItem.VAR_FLOAT1_AMP_SCALE, mScaleAnimator);
		
		TouchDisplayClickListener displayClickListener = new TouchDisplayClickListener();
		displayClickListener.bindClickable(null, new Listener() {
			boolean positive = true;
			@Override
			public void onDisplayClick(DisplayClickable clickable, Vec2F displayCoords) {
				Logger.logD(TAG, "Animating to phase: " + (positive ? "1.0" : "0.0"));
				mAlphaAnimator.animateFromCurrentPhase(positive);
				positive = !positive;
			}
			public void onDisplayDoubleClick(DisplayClickable clickable, Vec2F displayCoords) {
				float phase = (float)Math.random();
				Logger.logD(TAG, "Animating to phase: " + phase);
				mAlphaAnimator.animateToPhase(phase);
			}
			public void onDisplayLongClick(DisplayClickable clickable, Vec2F displayCoords) { }
		});
		mScene.bindController(displayClickListener);
	}
	
	private TimedFloatAnimator.Listener mAnimatorListener = new TimedFloatAnimator.Listener() {
		@Override
		public void onAnimationFinished(float phase, float value) {
			// React to animation getting finished here
			Logger.logD(TAG, "Animation finished. Phase = " + phase + ", value = " + value);
		}
	};
	

}
