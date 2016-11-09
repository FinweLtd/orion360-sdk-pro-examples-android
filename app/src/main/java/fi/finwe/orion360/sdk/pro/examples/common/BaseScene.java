package fi.finwe.orion360.sdk.pro.examples.common;

import android.content.Context;
import fi.finwe.orion360.v3.OrionScene;
import fi.finwe.orion360.v3.item.OrionPanorama;
import fi.finwe.orion360.v3.source.OrionTexture;

public class BaseScene extends OrionScene {
	// Logging tag, ready to be copy-pasted into any other class.
	protected static final String TAG = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	protected OrionPanorama 	mPanorama;
	protected OrionTexture		mPanoramaTexture;
	
	public BaseScene() {
		// Don't show the scene until we are ready.
		setVisible(false);
		
		// Create the OrionPanorama
		mPanorama = new OrionPanorama();
		bindSceneItem(mPanorama);
	}
	
	public OrionPanorama getPanorama() {
		return mPanorama;
	}
	
	public OrionTexture getPanoramaTexture() {
		return mPanoramaTexture;
	}
	
	public void setSourceURI(Context ctx, String sourceURI) {
		if (mPanoramaTexture != null) {
			mPanorama.releaseTextures();
			mPanoramaTexture.destroy();
		}
		mPanoramaTexture = OrionTexture.createTextureFromURI(ctx, sourceURI);
		mPanorama.bindTextureFull(0, mPanoramaTexture);
	}
}
