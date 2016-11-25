package fi.finwe.orion360.sdk.pro.examples.vr3d;

import android.content.Context;
import android.graphics.RectF;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;

public class VR3DScene extends OrionScene {
	// Logging tag, ready to be copy-pasted into any other class.
	protected static final String TAG = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	protected OrionPanorama 	mPanorama;
	protected OrionTexture		mPanoramaTexture;
	
	public VR3DScene() {
		// Don't show the scene until we are ready.
		setVisible(false);
		
		mPanorama = new OrionPanorama();
		bindSceneItem(mPanorama);
	}
	
	public OrionPanorama getPanorama() {
		return mPanorama;
	}
	
	public OrionTexture getPanoramaTexture() {
		return mPanoramaTexture;
	}
	
	public void setSourceURI(Context ctx, String sourceURI, RectF leftTexRect, RectF rightTexRect) {
		if (mPanoramaTexture != null) {
			mPanorama.releaseTextures();
			mPanoramaTexture.destroy();
		}
		mPanoramaTexture = OrionTexture.createTextureFromURI(ctx, sourceURI);
		// Note the call to bindTextureVR, not bindTexture
		// This call makes it possible to define different texture based on whether it's rendered VR_LEFT or VR_RIGHT enabled OrionViewport 
		mPanorama.bindTextureVR(0, mPanoramaTexture, new RectF(-180, 90, 180, -90), leftTexRect, rightTexRect);
	}
	
}
