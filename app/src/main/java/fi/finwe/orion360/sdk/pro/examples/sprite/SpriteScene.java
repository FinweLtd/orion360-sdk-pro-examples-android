package fi.finwe.orion360.sdk.pro.examples.sprite;

import android.content.Context;
import fi.finwe.math.Vec3F;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.item.sprite.OrionSprite;
import fi.finwe.orion360.sdk.pro.item.OrionSceneItem.RenderingMode;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;

public class SpriteScene extends OrionScene {
	// Logging tag, ready to be copy-pasted into any other class.
	protected static final String TAG = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	protected OrionSprite		mSprite;
	protected OrionTexture []	mTextures;
	int 						mCurrentTexture;
	protected OrionSprite		mCenterMarker;
	protected OrionSprite		mBorderMarker;
	
	public SpriteScene() {
		mSprite = new OrionSprite();
		mSprite.setWorldTranslation(new Vec3F(0,0,-1));
		mSprite.setRenderingMode(RenderingMode.CAMERA_DISABLED);
		mSprite.setScale(0.2f);
		bindSceneItem(mSprite);
		
		mBorderMarker = new OrionSprite();
		mBorderMarker.setWorldTranslation(new Vec3F(0,0,-0.95f));
		mBorderMarker.setRenderingMode(RenderingMode.CAMERA_DISABLED);
		mBorderMarker.setScale(0.2f);
		bindSceneItem(mBorderMarker);
		
		mCenterMarker = new OrionSprite();
		mCenterMarker.setWorldTranslation(new Vec3F(0,0,-0.95f));
		mCenterMarker.setRenderingMode(RenderingMode.CAMERA_DISABLED);
		mCenterMarker.setScale(0.1f);
		bindSceneItem(mCenterMarker);
	}
	
	public OrionSprite getSprite() {
		return mSprite;
	}
	
	public void setSourceURI(Context ctx, String [] spriteuris, String rectUri, String borderUri) {
		mTextures = new OrionTexture[spriteuris.length];
		int n = 0;
		for (String uri: spriteuris) {
			mTextures[n] = OrionTexture.createTextureFromURI(ctx, uri);
			++n;
		}
		mSprite.bindTexture(mTextures[0]);
		
		mCenterMarker.bindTexture(OrionTexture.createTextureFromURI(ctx, rectUri));
		mBorderMarker.bindTexture(OrionTexture.createTextureFromURI(ctx, borderUri));
	}
	
	public void setTextureAlign(OrionSprite.AlignMode mode) {
		mSprite.setTextureAlign(mode);
		mBorderMarker.setTextureAlign(mode);
	}
	
	public void toggleTexture() {
		mCurrentTexture = (mCurrentTexture + 1) % mTextures.length;
		mSprite.bindTexture(mTextures[mCurrentTexture]);
	}
}
