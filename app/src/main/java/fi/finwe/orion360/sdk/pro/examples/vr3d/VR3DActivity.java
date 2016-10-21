package fi.finwe.orion360.sdk.pro.examples.vr3d;

import android.os.Bundle;
import android.view.KeyEvent;
import fi.finwe.log.Logger;
import fi.finwe.math.QuatF;
import fi.finwe.math.Vec2F;
import fi.finwe.orion360.v3.OrionActivity;
import fi.finwe.orion360.v3.OrionContext;
import fi.finwe.orion360.v3.OrionView;
import fi.finwe.orion360.v3.OrionViewport;
import fi.finwe.orion360.v3.Hello.R;
import fi.finwe.orion360.v3.controllable.DisplayClickable;
import fi.finwe.orion360.v3.controller.TouchDisplayClickListener;
import fi.finwe.orion360.v3.item.OrionCamera;
import fi.finwe.orion360.v3.item.OrionPanorama;
import fi.finwe.orion360.v3.item.OrionSceneItem;
import fi.finwe.orion360.v3.item.OrionSceneItem.RotationBaseControllerListenerBase;
import fi.finwe.orion360.v3.item.sprite.OrionSprite;
import fi.finwe.orion360.v3.source.OrionTexture;
import fi.finwe.orion360.v3.viewport.fx.BarrelDistortion;

public class VR3DActivity extends OrionActivity {
	// Logging tag, ready to be copy-pasted into any other class.
	public static final String TAG = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	protected OrionView 	mView;
	protected VR3DScene		mScene;
	protected OrionCamera 	mDefaultCamera;
	
	protected static final int	SPRITE_COUNT = 5;
	protected OrionTexture		mSpriteTexture1;
	protected OrionTexture		mSpriteTexture2;
	protected OrionSprite []	mSprites;
	
	float mVRCameraDistance = 0.035f;
	float mVRCameraFocalDistance = 1.5f;
	
	boolean mAdjustMode = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setSystemUiVisibility(false);
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		mView = (OrionView)findViewById(R.id.orion_view);
		
		OrionContext.getSensorFusion().setMagnetometerEnabled(false);
		mScene = new VR3DScene();
		mScene.bindController(OrionContext.getSensorFusion());
		mScene.setSourceURI(this, "asset://pimgpsh_fullsize_distr.jpg", 
				OrionPanorama.TEXTURE_RECT_HALF_TOP, 		// Left eye view
				OrionPanorama.TEXTURE_RECT_HALF_BOTTOM);	// Right eye view
		// Make the panorama a bit larger, so sprites won't get clipped off
		mScene.getPanorama().setScale(3.0f);
		mView.bindDefaultScene(mScene);
		
		mDefaultCamera = new OrionCamera();
		mDefaultCamera.setZoomMax(3.0f);				// 3.0 -> The camera will never reduce the FOV below 1/3 of the base FOV
		mDefaultCamera.setVRCameraDistance(0.035f);		// Set the VR cameras a little bit apart from each other
		mDefaultCamera.setVRCameraFocalDistance(1.5f);	// Set the VR cameras to point inwards so that the centerlines cross at a given distance
		mDefaultCamera.setRotationBaseControllerListener(new RotationBaseControllerListenerBase() {
			@Override
			public void onRotationBaseControllerBound(OrionSceneItem item, QuatF rotationBase) {
				super.onRotationBaseControllerBound(item, rotationBase);
				item.setRotationYaw(0);
				mScene.setVisible(true);
			}
		});
		
		OrionContext.getSensorFusion().bindControllable(mDefaultCamera);
		mView.bindDefaultCamera(mDefaultCamera);

		// Set up two viewports split side by side (when looked from landscape orientation)
		mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_SPLIT_HORIZONTAL, OrionViewport.CoordinateType.FIXED_LANDSCAPE);
		// Designate each viewport to render content for either left or right eye
		mView.getViewports()[0].setVRMode(OrionViewport.VRMode.VR_LEFT);
		mView.getViewports()[1].setVRMode(OrionViewport.VRMode.VR_RIGHT);
		
		// Add some sprites at regular intervals to test VR rendering
		mSprites = new OrionSprite[SPRITE_COUNT];
		mSpriteTexture1 = OrionTexture.createTextureFromURI(this, "asset://spi_icon.png");
		mSpriteTexture2 = OrionTexture.createTextureFromURI(this, "asset://spi_icon2.png");
		for (int n = 0; n < SPRITE_COUNT; ++n) {
			mSprites[n] = new OrionSprite();
			mSprites[n].bindTexture(mSpriteTexture1);
			mSprites[n].setScale(0.1f);
			mSprites[n].setWorldTranslation(-0.3f, 0, -2.0f + n * 2.0f / SPRITE_COUNT);
			mScene.bindSceneItem(mSprites[n]);
		}
		
		BarrelDistortion barrelFx = new BarrelDistortion();
		barrelFx.setDistortionFillScale(1.0f);
		barrelFx.setDistortionCenterOffset(0,0);
		barrelFx.setDistortionCoeffs(new float[] { 1.0f, 0.39f, -0.35f, 0.19f} );
		mView.getViewports()[0].bindFX(barrelFx);
		mView.getViewports()[1].bindFX(barrelFx);
		
		// Make the double click toggle what variable the volume buttons adjust
		TouchDisplayClickListener listener = new TouchDisplayClickListener();
		listener.bindClickable(null, new TouchDisplayClickListener.Listener() {
			@Override
			public void onDisplayLongClick(DisplayClickable clickable, Vec2F displayCoords) {
			}
			
			@Override
			public void onDisplayDoubleClick(DisplayClickable clickable, Vec2F displayCoords) {
				// TODO Auto-generated method stub
				mAdjustMode = !mAdjustMode;
				// Adjust the sprite texture to show which mode is on
				for (int n = 0; n < SPRITE_COUNT; ++n) {
					mSprites[n].bindTexture(mAdjustMode ? mSpriteTexture2 : mSpriteTexture1);
				}
			}
			
			@Override
			public void onDisplayClick(DisplayClickable clickable, Vec2F displayCoords) {
			}
		});
		mScene.bindController(listener);
	}
	
	// Have the volume buttons adjust a variable
	@Override
	public boolean dispatchKeyEvent(KeyEvent keyEvent) {
		float newVRCameraDistance = mVRCameraDistance;
		float newVRCameraFocalDistance = mVRCameraFocalDistance;
		if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
			if (mAdjustMode) {
				newVRCameraFocalDistance *= 1.015f;
			} else {
				newVRCameraDistance *= 1.015f;
			}
		} else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
			if (mAdjustMode) {
				newVRCameraFocalDistance /= 1.015f;
			} else {
				newVRCameraDistance /= 1.015f;
			}
		}
		if (newVRCameraDistance != mVRCameraDistance) {
			mVRCameraDistance = newVRCameraDistance;
			mDefaultCamera.setVRCameraDistance(mVRCameraDistance);
			mDefaultCamera.setVRCameraFocalDistance(mVRCameraFocalDistance);	// Need to update the focal distance too, to make the cameras still point in the same spot
			Logger.logD(TAG, "VR camera distance: " + mVRCameraDistance);
			return true;
		} else if (newVRCameraFocalDistance != mVRCameraFocalDistance) {
			mVRCameraFocalDistance = newVRCameraFocalDistance;
			mDefaultCamera.setVRCameraFocalDistance(mVRCameraFocalDistance);
			Logger.logD(TAG, "VR camera focal distance: " + mVRCameraFocalDistance);
			return true;
		} else {
			return super.dispatchKeyEvent(keyEvent);
		}
	}

	
}
