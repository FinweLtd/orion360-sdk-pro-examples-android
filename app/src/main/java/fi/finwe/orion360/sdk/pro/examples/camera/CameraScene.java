package fi.finwe.orion360.sdk.pro.examples.camera;

import fi.finwe.orion360.v3.OrionScene;
import fi.finwe.orion360.v3.item.sprite.OrionSprite;
import fi.finwe.orion360.v3.source.OrionCameraTexture;

public class CameraScene extends OrionScene implements OrionCameraTexture.Listener {
	// Logging tag, ready to be copy-pasted into any other class.
	protected static final String TAG = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	protected OrionSprite		 	mCameraSprite;
	protected OrionCameraTexture	mCameraTexture;
	
	public CameraScene() {
		mCameraTexture = new OrionCameraTexture(OrionCameraTexture.CameraFacing.BACK);
		mCameraTexture.setUpdateListener(this);
		mCameraSprite = new OrionSprite();
		mCameraSprite.bindTexture(mCameraTexture);
		mCameraSprite.setRotationOffset(mCameraTexture.getTextureRotation());
		bindSceneItem(mCameraSprite);
	}
	
	public OrionCameraTexture getCameraTexture() {
		return mCameraTexture;
	}

	@Override
	public void onCameraPreviewFrame(OrionCameraTexture cameraTexture, byte[] frame) {
		// Process the frame as you see fit, and only then call the following method to re-enable the callback.
		// By default the camera has only one preview frame buffer added upon calling setUpdateListener
		mCameraTexture.addPreviewFrameBuffer(frame);
	}
}
