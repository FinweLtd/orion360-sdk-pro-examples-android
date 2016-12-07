/**
 * Copyright (c) 2016, Finwe Ltd. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package fi.finwe.orion360.sdk.pro.examples.sprite;

import android.os.Bundle;
import android.view.View;
import android.widget.ToggleButton;

import fi.finwe.math.Vec3F;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionSceneItem;
import fi.finwe.orion360.sdk.pro.item.sprite.OrionSprite;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;

/**
 * An example demonstrating various sprite layout and scaling options.
 * <p/>
 * Features:
 * <ul>
 * <li>Loads a set of hard-coded rectilinear images in .png format from file system
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Renders one of the images using standard rectilinear projection
 * <li>Allows experimenting with layout and scaling options via a set of button controls
 * </ul>
 */
public class SpriteLayout extends OrionActivity {

    /** The Android view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our planar sprite will be added to. */
    protected OrionScene mScene;

    /** The sprite where our image texture will be mapped to. */
    protected OrionSprite mSprite;

    /** The image textures where our decoded images will be added to. */
    protected OrionTexture [] mSpriteTextures;

    /** The index of the sprite texture that is currently active (bound to the sprite). */
    protected int mCurrentTexture;

    /** The sprite that marks the center point of the screen (visual guide). */
    protected OrionSprite mCenter;

    /** The sprite that marks the scaling bounds (visual guide). */
    protected OrionSprite mBounds;

    /** The camera which will project our 3D scene to a 2D (view) surface. */
    protected OrionCamera mCamera;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Use a special layout that contains a set of control buttons in addition to OrionView.
		setContentView(R.layout.activity_sprite);

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene();

        // Create a new sprite. This is a 2D plane in the 3D world for our planar image.
        mSprite = new OrionSprite();

        // Create a set of textures for our sprite (we will swap texture with a button press).
        String[] uris = getResources().getStringArray(R.array.sprite_layout_rects);
        mSpriteTextures = new OrionTexture[uris.length];
        for (int i = 0; i < uris.length; i++) {
            mSpriteTextures[i] = OrionTexture.createTextureFromURI(this, uris[i]);
        }

        // Set sprite location in the 3D world. Here we place it slightly ahead in front direction.
        mSprite.setWorldTranslation(new Vec3F(0.0f, 0.0f, -1.0f));

        // We don't need a perspective camera in this example.
        mSprite.setRenderingMode(OrionSceneItem.RenderingMode.CAMERA_DISABLED);

        // Set sprite size in the 3D world.
        mSprite.setScale(0.2f);

        // Bind the sprite texture to the sprite object. Here we start from the first one.
        mSprite.bindTexture(mSpriteTextures[0]);

        // Bind the sprite to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mSprite);

        // Create a sprite for marking the center point of the screen.
        mCenter = new OrionSprite();
        mCenter.setWorldTranslation(new Vec3F(0.0f, 0.0f, -0.95f));
        mCenter.setRenderingMode(OrionSceneItem.RenderingMode.CAMERA_DISABLED);
        mCenter.setScale(0.1f);
        mCenter.bindTexture(OrionTexture.createTextureFromURI(this,
                getString(R.string.sprite_layout_reticle)));
        mScene.bindSceneItem(mCenter);

        // Create a sprite for marking the scaling bounds.
        mBounds = new OrionSprite();
        mBounds.setWorldTranslation(new Vec3F(0.0f, 0.0f, -0.95f));
        mBounds.setRenderingMode(OrionSceneItem.RenderingMode.CAMERA_DISABLED);
        mBounds.setScale(0.2f);
        mBounds.bindTexture(OrionTexture.createTextureFromURI(this,
                getString(R.string.sprite_layout_bounds)));
        mScene.bindSceneItem(mBounds);

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera();

        // Find Orion360 view from the XML layout. This is an Android view where we render content.
        mView = (OrionView)findViewById(R.id.orion_view);

        // Bind the scene to the view. This is the 3D world that we will be rendering to this view.
        mView.bindDefaultScene(mScene);

        // Bind the camera to the view. We will look into the 3D world through this camera.
        mView.bindDefaultCamera(mCamera);

        // The view can be divided into one or more viewports. For example, in VR mode we have one
        // viewport per eye. Here we fill the complete view with one (landscape) viewport.
        mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL,
                OrionViewport.CoordinateType.FIXED_LANDSCAPE);
	}

    // Handle the buttons that control texture alignment rule:

    public void onAlignTopLeftClicked(View button) {
        setTextureAlign(OrionSprite.AlignMode.TOP_LEFT);
    }

    public void onAlignTopCenterClicked(View button) {
        setTextureAlign(OrionSprite.AlignMode.TOP);
    }

    public void onAlignTopRightClicked(View button) {
        setTextureAlign(OrionSprite.AlignMode.TOP_RIGHT);
    }

    public void onAlignCenterLeftClicked(View button) {
        setTextureAlign(OrionSprite.AlignMode.LEFT);
    }

    public void onAlignCenterClicked(View button) {
        setTextureAlign(OrionSprite.AlignMode.CENTER);
    }

    public void onAlignCenterRightClicked(View button) {
        setTextureAlign(OrionSprite.AlignMode.RIGHT);
    }

    public void onAlignBottomLeftClicked(View button) {
        setTextureAlign(OrionSprite.AlignMode.BOTTOM_LEFT);
    }

    public void onAlignBottomCenterClicked(View button) {
        setTextureAlign(OrionSprite.AlignMode.BOTTOM);
    }

    public void onAlignBottomRightClicked(View button) {
        setTextureAlign(OrionSprite.AlignMode.BOTTOM_RIGHT);
    }

    public void setTextureAlign(OrionSprite.AlignMode mode) {
        mSprite.setTextureAlign(mode);
        mBounds.setTextureAlign(mode);
    }

    // Handle the button that swaps next texture:

    public void onSwapTextureClicked(View button) {
        swapTexture();
    }

    public void swapTexture() {
        mCurrentTexture = (mCurrentTexture + 1) % mSpriteTextures.length;
        mSprite.bindTexture(mSpriteTextures[mCurrentTexture]);
    }

    // Handle the buttons that control texture scale mode:

    public void onFitLongClicked(View button) {
        mSprite.setScaleMode(OrionSprite.ScaleMode.FIT_LONG);
    }

    public void onFitShortClicked(View button) {
        mSprite.setScaleMode(OrionSprite.ScaleMode.FIT_SHORT);
    }

    public void onFitWidthClicked(View button) {
        mSprite.setScaleMode(OrionSprite.ScaleMode.FIT_WIDTH);
    }

    public void onFitHeightClicked(View button) {
        mSprite.setScaleMode(OrionSprite.ScaleMode.FIT_HEIGHT);
    }

    // Handle the button that controls the crop feature:

    public void onToggleCropClicked(View view) {
        ToggleButton button = (ToggleButton)view;
        if (button.isChecked()) {
            mSprite.setFlags(OrionSprite.FLAG_SPRITE_ENABLE_CROP);
        } else {
            mSprite.resetFlags(OrionSprite.FLAG_SPRITE_ENABLE_CROP);
        }
    }
}
