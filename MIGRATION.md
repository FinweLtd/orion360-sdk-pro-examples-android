![alt tag](https://cloud.githubusercontent.com/assets/12032146/20262054/3d5de056-aa69-11e6-8ecd-31f02d96c4d8.png)

# Migration Guide for Orion360 SDK (Pro) for Android

This file contains instructions for migrating from one Orion360 SDK (Pro) version to another.


## From 4.0.12.x to 4.0.13.x

### ExoPlayer

ExoPlayer support has been updated to version 2.18.

ExoPlayer is now separate from Orion360 SDK binaries. You can choose ExoPlayer version that you want to use and define it in your .gradle file. For example:
```
dependencies {

    api 'com.google.android.exoplayer:exoplayer:2.18.1'

}
```

You must use a wrapper that is compatible with your selected ExoPlayer version. This example project contains an example wrapper under /engine/ExoPlayerWrapper.java, which you are free to use and modify in your own app.

### OrionVideoTexture.Listener

1. Implement new callback:
```
@Override
public void onVideoPlayerDestroyed(OrionVideoTexture texture) {
    
}
```

> VideoPlayerWrappers now post the onVideoReleased / onVideoPlayerDestroyed callback *before* deleting anything


## From 3.x to 4.0.12.x

### Gradle file

1. Change Orion 360 SDK (Pro) dependency from 3.x to Orion 4.x. For example:
```
//  implementation 'fi.finwe.orion360:orion360-sdk-pro-public:3.1.02.100'
    implementation 'fi.finwe.orion360:orion360-sdk-pro-public:4.0.12.002'
```

2. Change FinweUtil dependency from 1.x to FinweUtil 2.x. For example:
```
//  debugImplementation 'fi.finwe.util:finwe-util-public:1.0.04-debug'
    debugImplementation 'fi.finwe.util:finwe-util-public:2.0.00-debug'
```

3. Increase 'minSdkVersion' from 18 to 21.

4. If you have dependency to ExoPlayer2 v. 2.4.1, remove it. For example:
```
    // ExoPlayer as an alternative video engine.
    //noinspection GradleDependency
//    implementation 'com.google.android.exoplayer:exoplayer-core:r2.4.1'
    //noinspection GradleDependency
//    implementation 'com.google.android.exoplayer:exoplayer-dash:r2.4.1'
    //noinspection GradleDependency
//    implementation 'com.google.android.exoplayer:exoplayer-hls:r2.4.1'
    //noinspection GradleDependency
//    implementation 'com.google.android.exoplayer:exoplayer-smoothstreaming:r2.4.1'
    //noinspection GradleDependency
//    implementation 'com.google.android.exoplayer:exoplayer-ui:r2.4.1'

```

> ExoPlayer2 v. 2.4.1 is bundled with Orion360 SDK Pro 4.x.

Refresh & rebuild project.

### XML layouts / OrionView

1. Replace OrionView with OrionViewContainer.
```
<!--
<fi.finwe.orion360.sdk.pro.view.OrionView
        android:id="@+id/orion_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
-->

<fi.finwe.orion360.sdk.pro.view.OrionViewContainer
        android:id="@+id/orion_view_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
```

> OrionView does not inherit from Android View class anymore, it is now purely Orion 360 SDK specific class. Therefore, if you have old code that assumes OrionView to be an Android View, use there OrionViewContainer class instead. Example:
> ```
> //mMediaController.setAnchorView(mOrionView);
> mMediaController.setAnchorView(findViewById(R.id.orion_view_container));
> ```

> You may need to create OrionView manually and bind it to the container. An example:
> ```
> mViewContainer = (OrionViewContainer)findViewById(R.id.orion_view_container);
> mView = new OrionView(mOrionContext);
> mViewContainer.bindView(mView);
> ```

### Viewports

1. Change 'OrionViewport' to 'OrionDisplayViewport'. Example:
```
//mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL, 
//    OrionViewport.CoordinateType.FIXED_LANDSCAPE);
mView.bindViewports(OrionDisplayViewport.VIEWPORT_CONFIG_FULL,
                OrionDisplayViewport.CoordinateType.FIXED_LANDSCAPE);
```

### Java Imports

1. Change OrionTexture package (source -> texture).
```
//import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
```

2. Change OrionVideoTexture package (source -> texture).
```
//import fi.finwe.orion360.sdk.pro.source.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.texture.OrionVideoTexture;
```

3. Change OrionCameraTexture package (source -> texture).
```
//import fi.finwe.orion360.sdk.pro.source.OrionCameraTexture;
import fi.finwe.orion360.sdk.pro.texture.OrionCameraTexture;
```

4. Change VideoPlayerWrapper package (source -> texture).
```
//import fi.finwe.orion360.sdk.pro.source.VideoPlayerWrapper;
import fi.finwe.orion360.sdk.pro.texture.VideoPlayerWrapper;
```

5. Change AndroidMediaPlayerWrapper package (source -> texture).
```
//import fi.finwe.orion360.sdk.pro.source.AndroidMediaPlayerWrapper;
import fi.finwe.orion360.sdk.pro.texture.AndroidMediaPlayerWrapper;
```

6. Change TimedFloatFunction to TimedFloat1ToFloat1Function.
```
//import fi.finwe.orion360.sdk.pro.variable.TimedFloatFunction;
import fi.finwe.orion360.sdk.pro.variable.TimedFloat1ToFloat1Function;
```

7. Change OrionViewport package (root -> viewport).
```
//import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.viewport.OrionViewport;
```

8. Change SensorFusion to OrionSensorFusion.
```
//import fi.finwe.orion360.sdk.pro.controller.SensorFusion;
import fi.finwe.orion360.sdk.pro.controller.OrionSensorFusion;
```

9. Change BarrelDistortion to OrionBarrelDistortion.
```
//import fi.finwe.orion360.sdk.pro.viewport.fx.BarrelDistortion;
import fi.finwe.orion360.sdk.pro.viewport.fx.OrionBarrelDistortion;
```

### OrionContext

Orion 360 SDK (Pro) supports multiple simultaneous OrionContexts. Therefore, we must pass OrionContext to most of the APIs, so that correct OrionContext will be used for performing the requested API call.

Example:
```
//mTouchPincher = new TouchPincher();
mTouchPincher = new TouchPincher(orionContext);
```

> There is a fairly large number of similar changes to be made, throughout your use of Orion360 SDK (Pro) APIs. We apologize for the inconvenience, but this change was necessary.

### Bindings

1. Change 'bindController' to 'bindRoutine', and 'releaseController' to 'releaseRoutine'. For example:
```
//scene.bindController(mTouchPincher);
//scene.bindController(mTouchRotater);
//scene.bindController(mRotationAligner);
scene.bindRoutine(mTouchPincher);
scene.bindRoutine(mTouchRotater);
scene.bindRoutine(mRotationAligner);

//scene.releaseController(mTouchPincher);
//scene.releaseController(mTouchRotater);
//scene.releaseController(mRotationAligner);
scene.releaseRoutine(mTouchPincher);
scene.releaseRoutine(mTouchRotater);
scene.releaseRoutine(mRotationAligner);

//scene.bindController(mOrionContext.getSensorFusion());
scene.bindRoutine(mOrionContext.getSensorFusion());
```

2. Change 'registerOrientationListener' to 'bindOrientationListener', and 'unregisterOrientationListener' to 'releaseOrientationListener'. For example:
```
//mOrionContext.getSensorFusion().registerOrientationListener(this);
mOrionContext.getSensorFusion().bindOrientationListener(this);

//mOrionContext.getSensorFusion().unregisterOrientationChangeListener(this);
mOrionContext.getSensorFusion().releaseOrientationListener(this);
```

### TouchWorldClickListener.Listener has changed:
Old:
```
import fi.finwe.orion360.sdk.pro.controllable.RaycastReceiver;

new TouchWorldClickListener.Listener() {
    @Override
    public void onWorldClick(RaycastReceiver receiver, Vec2f vec2F,
                             Raycast raycast) {
    }

    @Override
    public void onWorldDoubleClick(RaycastReceiver receiver, Vec2f vec2F,
                                   Raycast raycast) {
    }

    @Override
    public void onWorldLongClick(RaycastReceiver receiver, Vec2f vec2F,
                                 Raycast raycast) {
    }
}        
```

New:
```
import fi.finwe.orion360.sdk.pro.controllable.RaycastHit;
import fi.finwe.orion360.sdk.pro.controllable.Raycastable;

new TouchWorldClickListener.Listener() {
    @Override
    public void onWorldClick(Raycastable clickable, Vec2f displayCoords, 
        Raycast raycast, RaycastHit raycastHit) {
    }

    @Override
    public void onWorldDoubleClick(Raycastable clickable, Vec2f displayCoords, 
        Raycast raycast, RaycastHit raycastHit) {
    }

    @Override
    public void onWorldLongClick(Raycastable clickable, Vec2f displayCoords, 
        Raycast raycast, RaycastHit raycastHit) {
    }
}
```

### Animations

1. Change 'TimedFloatFunction' to 'TimedFloat1ToFloat1Function' and 'TimedFloatFunction.Function.LINEAR' to 'BasicFunction.LINEAR'. Example:
```
//protected TimedVariable mAlphaAnimator;
//mAlphaAnimator = TimedFloatFunction.fromRange(0.0f, 1.0f,
//                TimedFloatFunction.Function.LINEAR);

protected TimedFloat1ToFloat1Function mAlphaAnimator;
mAlphaAnimator = TimedFloat1ToFloat1Function.fromRange(mOrionContext,
                0.0f, 1.0f, BasicFunction.LINEAR);
```

> We now have support for more complex mappings between values and functions, therefore the API had to be changed.

2. Change 'bindSharedVariable' to 'bindVariable'. Example:
```
//mPanoramaImage.bindSharedVariable(OrionSceneItem.VAR_FLOAT1_AMP_ALPHA, mAlphaAnimator);
mPanoramaImage.bindSharedVariable(OrionSceneItem.VAR_FLOAT1_AMP_ALPHA, mAlphaAnimator);
```

3. Use new Color FX API. Example:
```
//mPanoramaImage.bindSharedVariable(OrionSceneItem.VAR_FLOAT1_AMP_ALPHA, mAlphaAnimator);
mBackgroundSprite.getColorFx().bindAmpAlpha(errorAnimator);
```

### OrionVideoTexture.Listener

1. Remove deleted API call:
```
//@Override
//public void onInvalidURI(OrionTexture orionTexture) {
//}
```

2. Implement new callback:
```
@Override
public void onVideoReleased(OrionVideoTexture texture) {
    
}
```

### VR

1. Change 'BarrelDistortion' to 'OrionBarrelDistortion':
```
//BarrelDistortion barrelFx = new BarrelDistortion();
OrionBarrelDistortion barrelFx = new OrionBarrelDistortion(mOrionContext);
```

### Sprite

1. Change 'OrionSprite.ScaleMode.FIT_LONG' to 'OrionSprite.ScaleMode.FIT_OUTSIDE'.
2. Change 'OrionSprite.ScaleMode.FIT_SHORT' to 'OrionSprite.ScaleMode.FIT_INSIDE'.
3. Change 'mSprite.resetFlags()' to 'mSprite.clearFlags()'.

### SelectablePointerIcon

1. Change 'setLocationPolarZXYDeg' to 'setWorldTransformFromPolarZXYDeg'.

2. Change 'setSelectionTriggerFrameCount()' to 'setSelectionMaxFrameCount()'. For example:
```
//mHomeButton.setSelectionTriggerFrameCount(90);
mHomeButton.setTriggerOnMaxFrameCount(true);
mHomeButton.setSelectionMaxFrameCount(90);
```

### Math

1. Change 'Vec3f.AXIS_FRONT' to 'Vec3f.FRONT'.

### License

Existing Orion 360 SDK (Pro) licenses work with both 3.x and 4.x, no changes required.

