package fi.finwe.orion360.sdk.pro.examples.sprite;

import android.os.Bundle;
import android.view.View;
import android.widget.ToggleButton;
import fi.finwe.orion360.v3.OrionActivity;
import fi.finwe.orion360.v3.OrionView;
import fi.finwe.orion360.v3.OrionViewport;
import fi.finwe.orion360.v3.Hello.R;
import fi.finwe.orion360.v3.item.OrionCamera;
import fi.finwe.orion360.v3.item.sprite.OrionSprite;

public class SpriteActivity extends OrionActivity {
	// Logging tag, ready to be copy-pasted into any other class.
	public static final String TAG = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	protected OrionView 	mView;
	protected SpriteScene	mScene;
	protected OrionCamera 	mDefaultCamera;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setSystemUiVisibility(false);
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_sprite);
		
		mScene = new SpriteScene();
		mScene.setSourceURI(this, new String[] { 
				"asset://kuutio/1.png", "asset://kuutio/2.png", "asset://kuutio/3.png", "asset://kuutio/4.png", "asset://kuutio/5.png", "asset://kuutio/6.png" }, 
				"asset://Rect_simple.png",
				"asset://Pink_border.png");
		
		mDefaultCamera = new OrionCamera();
		mDefaultCamera.setZoomMax(3.0f);
		
		mView = (OrionView)findViewById(R.id.orion_view);
		mView.bindDefaultCamera(mDefaultCamera);
		mView.bindDefaultScene(mScene);
		mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL, OrionViewport.CoordinateType.FIXED_LANDSCAPE);
	}
	
	@Override
	public void onDestroy() {
		mScene.destroy();
		super.onDestroy();
	}

	
	public void onAlignTopLeftClicked(View button) {
		mScene.setTextureAlign(OrionSprite.AlignMode.TOP_LEFT);
	}
	
	public void onAlignTopCenterClicked(View button) {
		mScene.setTextureAlign(OrionSprite.AlignMode.TOP);
	}
	
	public void onAlignTopRightClicked(View button) {
		mScene.setTextureAlign(OrionSprite.AlignMode.TOP_RIGHT);
	}
	
	public void onAlignCenterLeftClicked(View button) {
		mScene.setTextureAlign(OrionSprite.AlignMode.LEFT);
	}
	
	public void onAlignCenterClicked(View button) {
		mScene.setTextureAlign(OrionSprite.AlignMode.CENTER);
	}
	
	public void onAlignCenterRightClicked(View button) {
		mScene.setTextureAlign(OrionSprite.AlignMode.RIGHT);
	}
	
	public void onAlignBottomLeftClicked(View button) {
		mScene.setTextureAlign(OrionSprite.AlignMode.BOTTOM_LEFT);
	}
	
	public void onAlignBottomCenterClicked(View button) {
		mScene.setTextureAlign(OrionSprite.AlignMode.BOTTOM);
	}
	
	public void onAlignBottomRightClicked(View button) {
		mScene.setTextureAlign(OrionSprite.AlignMode.BOTTOM_RIGHT);
	}
	
	public void onFitLongClicked(View button) {
		mScene.getSprite().setScaleMode(OrionSprite.ScaleMode.FIT_LONG);
	}
	
	public void onFitShortClicked(View button) {
		mScene.getSprite().setScaleMode(OrionSprite.ScaleMode.FIT_SHORT);
	}
	
	public void onFitWidthClicked(View button) {
		mScene.getSprite().setScaleMode(OrionSprite.ScaleMode.FIT_WIDTH);
	}
	
	public void onFitHeightClicked(View button) {
		mScene.getSprite().setScaleMode(OrionSprite.ScaleMode.FIT_HEIGHT);
	}
	
	public void onToggleCropClicked(View view) {
		ToggleButton button = (ToggleButton)view;
		if (button.isChecked() == true) {
			mScene.getSprite().setFlags(OrionSprite.FLAG_SPRITE_ENABLE_CROP);
		} else {
			mScene.getSprite().resetFlags(OrionSprite.FLAG_SPRITE_ENABLE_CROP);
		}
	}
	
	public void onToggleTextureClicked(View button) {
		mScene.toggleTexture();
	}
	
}
