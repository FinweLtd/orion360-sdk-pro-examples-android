/*
 * Copyright (c) 2022, Finwe Ltd. All rights reserved.
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

package fi.finwe.orion360.sdk.pro.examples.tv;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;

import fi.finwe.log.Logger;
import fi.finwe.math.Quatf;
import fi.finwe.math.Vec3f;
import fi.finwe.orion360.sdk.pro.SimpleOrionActivity;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.item.OrionSceneItem;
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.texture.OrionVideoTexture;

/**
 * An example of a minimal Orion360 video player, for playing a video stream on Android TV device.
 *
 * This activity is configured as leanback launcher in the manifest and Android TVs should launch
 * this activity instead of the MainActivity. However, for Android Studio, you may need to create
 * a separate Run Configuration and manually select this activity. More information:
 * https://stackoverflow.com/questions/28298009/android-tv-not-starting-launch-leanback-activity
 */
@SuppressWarnings("unused")
public class TVStreamPlayer extends SimpleOrionActivity implements OrionVideoTexture.Listener {

    /** Tag for logging. */
    public static final String TAG = TVStreamPlayer.class.getSimpleName();

    /** The number of degrees in a half circle. */
    private static final int HALF_CIRCLE = 180;

    /** The number of degrees in a whole circle. */
    private static final int WHOLE_CIRCLE = 360;

    /** Rotation animation length (in ms). */
    private static final int ROTATION_ANIMATION_LENGTH_MS = 800;

    /** Zoom animation length (in ms). */
    private static final int ZOOM_ANIMATION_LENGTH_MS = 800;

    /** The number of zoom levels (steps). */
    private static final int ZOOM_LEVEL_COUNT = 10;

    /** Hysteresis for zoom. Do not perform additional "zoom in" step if we are "almost there". */
    private static final float ZOOM_HYSTERESIS = 0.05f;

    /** Buffering indicator, to be shown while buffering video from the network. */
    private ProgressBar mBufferingIndicator;

    /** Flag for indicating if video is being played. */
    private boolean mIsPlaying = false;

    /** Flag for turning rotation animation on/off. */
    private boolean mAnimateRotation = true;

    /** Yaw rotation value animator. */
    private ValueAnimator mRotationYawValueAnimator;

    /** Target value for yaw rotation (in degrees). */
    private float mRotationYawValueTargetDeg = 0.0f;

    /** Flag for indicating if up/down keys should control zoom instead of pan up/down. */
    private boolean mZoomWithUpDownKeys = true;

    /** Current zoom level. */
    private float mZoomLevel = 0.0f;

    /** Zoom value animator. */
    private ValueAnimator mZoomValueAnimator;

    /** Flag for turning zoom animation on/off. */
    private boolean mAnimateZoom = true;

    /** Supported projection type. */
    public enum OutputProjection {
        UNKNOWN,
        EQUIRECTANGULAR,
        RECTILINEAR,
        LITTLEPLANET,
    }

    /** Currently selected output projection. */
    public OutputProjection mOutputProjection =
            OutputProjection.RECTILINEAR;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        // Call super class implementation FIRST to set up a simple Orion360 player configuration.
        super.onCreate(savedInstanceState);

        // Above call will fail if a valid Orion360 license file for the package name defined in
        // the application's manifest/build.gradle files cannot be found!

        // Set layout.
        setContentView(R.layout.activity_video_player);

        // Get buffering indicator, and make it visible initially (buffering will be needed).
        mBufferingIndicator = findViewById(R.id.buffering_indicator);
        mBufferingIndicator.setVisibility(View.VISIBLE);

        // Set Orion360 view (defined in the layout) that will be used for rendering 360 content.
        setOrionView(R.id.orion_view_container);

        // Set a URI that points to a video stream URL in the network.
        // Encode video with web/progressive setting enabled for best performance, or use
        // adaptive HLS stream.
        //setContentUri(MainMenu.TEST_VIDEO_URI_HLS);
        setContentUri(MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_IMAGE_FILE_LIVINGROOM_HQ);

        // Notice that accessing video streams over a network connection requires INTERNET
        // permission to be specified in the manifest file.

        // When you run the app, you may get a warning from MediaPlayer component to the LogCat:
        // W/MediaPlayer: Couldn't open []: java.io.FileNotFoundException: No content provider: []
        // Here Android MediaPlayer is using an exception for control flow; you can disregard it.

        // Set content listener.
        setVideoContentListener(this);

        // Re-configure camera.
        OrionCamera camera = getOrionCamera();
        camera.setProjectionPerspectiveDeg(OrionCamera.FovType.HORIZONTAL,
                120.0f, 0.1f, 100.0f);
        camera.setZoomMax(7.0f);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Logger.logD(TAG, "onKeyDown(), keyCode=" + keyCode + " keyEvent=" + event);

        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_BACK:
                // Navigate back -> quit the app.
                finish();
                return true;
            case KeyEvent.KEYCODE_BUTTON_SELECT:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                // Selection -> play/pause.
                /*
                if (mIsPlaying) {
                    getOrionTexture().pause();
                } else {
                    getOrionTexture().play();
                }
                 */

                toggleProjection();

                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                // Up -> pan up OR zoom in.
                if (mZoomWithUpDownKeys) {
                    zoomIn();
                } else {
                    rotatePitch(5);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // Down -> pan down OR zoom out.
                if (mZoomWithUpDownKeys) {
                    zoomOut();
                } else {
                    rotatePitch(-5);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                // Left -> pan left.
                rotateYaw(45);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // Right -> pan right.
                rotateYaw(-45);
                return true;
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_MOVE_HOME:
                // Home -> reset view to default position.
                // NOTE: capturing HOME key may not work, could navigate to Android TV home instead.
                resetView();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    /**
     * Rotate camera yaw angle. Positive value towards right (clockwise), negative towards left.
     *
     * @param degrees the amount of degrees to rotate.
     */
    public void rotateYaw(float degrees) {
        Logger.logF();

        // Adjust rotation step by zoom level.
        float rotationFactor = (float) (Math.pow(2.0f, mZoomLevel));
        degrees /= Math.max(1.0f, rotationFactor);

        OrionCamera camera = getOrionCamera();

        if (mAnimateRotation) {

            if (null != mRotationYawValueAnimator) {
                mRotationYawValueAnimator.cancel();
                mRotationYawValueAnimator = null;
            }

            mRotationYawValueTargetDeg += degrees;

            Quatf currentRotation = camera.getRotationOffset();
            Vec3f lookAt = Vec3f.FRONT.rotate(currentRotation);
            float yawDeg = (float) Math.toDegrees(-lookAt.getYaw());
            float pitchDeg = (float) Math.toDegrees(lookAt.getPitch());

            float from = yawDeg;
            float to = mRotationYawValueTargetDeg % 360.0f;
            float min = Math.min(from, to);
            if (Math.abs(to - from) > HALF_CIRCLE) {
                if (min == from) from += WHOLE_CIRCLE;
                else to += WHOLE_CIRCLE;
            }
            Logger.logD(TAG, "Rotation from=" + yawDeg + " to="
                    + mRotationYawValueTargetDeg + " -> from=" + from + " to=" + to);

            mRotationYawValueAnimator = ValueAnimator.ofFloat(from, to);
            mRotationYawValueAnimator.addUpdateListener(valueAnimator -> {
                //Logger.logF(); // Prevent flooding the log.

                float animatedValue = (float) valueAnimator.getAnimatedValue();
                //Logger.logD(TAG, "Anim yaw value " + animatedValue);
                Quatf newRotationOffset = Quatf.fromEulerRotationZXYDeg(
                        animatedValue, pitchDeg, 0.0f);
                camera.setRotationOffset(newRotationOffset);

            });
            mRotationYawValueAnimator.setDuration(ROTATION_ANIMATION_LENGTH_MS);
            mRotationYawValueAnimator.start();

        } else {
            Quatf currentRotation = camera.getRotationOffset();
            Quatf rotationDelta = Quatf.fromEulerRotationZXYDeg(degrees, 0.0f,0.0f);
            camera.setRotationOffset(currentRotation.multiply(rotationDelta));
        }
    }

    /**
     * Rotate camera pitch angle. Positive value upwards, negative downwards.
     *
     * @param degrees the amount of degrees to rotate.
     */
    public void rotatePitch(float degrees) {
        Logger.logF();

        // Adjust rotation step by zoom level.
        float rotationFactor = (float) (Math.pow(2.0f, mZoomLevel));
        degrees /= Math.max(1.0f, rotationFactor);

        OrionCamera camera = getOrionCamera();
        Quatf currentRotation = camera.getRotationOffset();
        Quatf rotationDelta = Quatf.fromEulerRotationZXYDeg(0.0f, degrees, 0.0f);
        camera.setRotationOffset(currentRotation.multiply(rotationDelta));
    }

    /**
     * Set camera zoom level in range [0-10].
     *
     * @param zoomLevel the new zoom level.
     */
    public void setPredefinedZoomLevel(int zoomLevel) {
        Logger.logF(); // Prevent flooding the log.

        OrionCamera camera = getOrionCamera();

        float max = (float) (Math.log(camera.getZoomMax()) / Math.log(2));
        float step = max / ZOOM_LEVEL_COUNT;
        float newZoomLevel = 0.0f + step * zoomLevel;
        float zoomOrionValue = (float) (Math.pow(2.0f, newZoomLevel));
        if (zoomOrionValue < 1.0f) {
            doZoom(0.0f, max, 1.0f);
        } else if (zoomOrionValue <= camera.getZoomMax()) {
            doZoom(newZoomLevel, max, zoomOrionValue);
        } else if ((camera.getZoom() + ZOOM_HYSTERESIS) < camera.getZoomMax()) {
            doZoom(newZoomLevel, max, camera.getZoomMax());
        } else {
            Logger.logD(TAG, "Zoom level=" + mZoomLevel + " Orion value="
                    + camera.getZoomMax());
        }
    }

    /**
     * Zoom in one step.
     */
    public void zoomIn() {
        Logger.logF();

        OrionCamera camera = getOrionCamera();

        float max = (float) (Math.log(camera.getZoomMax()) / Math.log(2));
        float step = max / ZOOM_LEVEL_COUNT;
        float newZoomLevel = mZoomLevel + step;
        float zoomOrionValue = (float) (Math.pow(2.0f, newZoomLevel));
        if (zoomOrionValue <= camera.getZoomMax()) {
            doZoom(newZoomLevel, max, zoomOrionValue);
        } else if ((camera.getZoom() + ZOOM_HYSTERESIS) < camera.getZoomMax()) {
            doZoom(newZoomLevel, max, camera.getZoomMax());
        } else {
            Logger.logD(TAG, "Zoom level=" + mZoomLevel + " Orion value="
                    + camera.getZoomMax());
        }
    }

    /**
     * Zoom out one step.
     */
    public void zoomOut() {
        Logger.logF();

        OrionCamera camera = getOrionCamera();

        float max = (float) (Math.log(camera.getZoomMax()) / Math.log(2));
        float step = max / ZOOM_LEVEL_COUNT;
        float newZoomLevel = mZoomLevel - step;
        float zoomOrionValue = (float) (Math.pow(2.0f, newZoomLevel));
        if (zoomOrionValue >= 1.0f) {
            doZoom(newZoomLevel, max, zoomOrionValue);
        } else {
            doZoom(0.0f, max, 1.0f);
        }
    }

    /**
     * Actually perform zooming.
     *
     * @param zoomLevel the new zoom level (internal value).
     * @param zoomLevelMax the maximum zoom level (internal value, based on max in Orion).
     * @param zoomOrionValue the value to be given to Orion (difference scale).
     */
    private void doZoom(float zoomLevel, float zoomLevelMax, float zoomOrionValue) {
        Logger.logF();

        Logger.logD(TAG, "Zoom level=" + zoomLevel + " maxLevel=" + zoomLevelMax
                + " percentage=" + zoomLevel / zoomLevelMax
                + " Orion value=" + zoomOrionValue);

        OrionCamera camera = getOrionCamera();
        if (mAnimateZoom) {
            if (null != mZoomValueAnimator) {
                mZoomValueAnimator.cancel();
                mZoomValueAnimator = null;
            }
            mZoomValueAnimator = ValueAnimator.ofFloat(mZoomLevel, zoomLevel);
            mZoomValueAnimator.addUpdateListener(valueAnimator -> {
                float animatedValue = (float) valueAnimator.getAnimatedValue();
                float orionValue = (float) (Math.pow(2.0f, animatedValue));
                camera.setZoom(orionValue);
                mZoomLevel = zoomLevel;
            });
            mZoomValueAnimator.setDuration(ZOOM_ANIMATION_LENGTH_MS);
            mZoomValueAnimator.start();
        } else {
            mZoomLevel = zoomLevel;
            camera.setZoom(zoomOrionValue);
        }
    }

    /**
     * Set source projection.
     */
    public void setProjectionSource() {
        Logger.logF();

        getOrionPanorama().setPanoramaType(OrionPanorama.PanoramaType.PANEL_SOURCE);
        getOrionPanorama().setRenderingMode(OrionSceneItem.RenderingMode.CAMERA_DISABLED);
        getOrionCamera().setProjectionMode(OrionCamera.ProjectionMode.RECTILINEAR);
    }

    /**
     * Set little planet projection.
     */
    public void setProjectionLittlePlanet() {
        Logger.logF();

        getOrionPanorama().setPanoramaType(OrionPanorama.PanoramaType.SPHERE);
        getOrionPanorama().setRenderingMode(OrionSceneItem.RenderingMode.PERSPECTIVE);
        getOrionCamera().setProjectionMode(OrionCamera.ProjectionMode.LITTLEPLANET);
    }

    /**
     * Set rectilinear projection.
     */
    public void setProjectionRectilinear() {
        Logger.logF();

        getOrionPanorama().setPanoramaType(OrionPanorama.PanoramaType.SPHERE);
        getOrionPanorama().setRenderingMode(OrionSceneItem.RenderingMode.PERSPECTIVE);
        getOrionCamera().setProjectionMode(OrionCamera.ProjectionMode.RECTILINEAR);
    }

    /**
     * Toggle next projection.
     */
    public void toggleProjection() {
        Logger.logF();

        switch (mOutputProjection) {
            case RECTILINEAR:
                setOutputProjection(OutputProjection.LITTLEPLANET);
                break;
            case LITTLEPLANET:
                setOutputProjection(OutputProjection.EQUIRECTANGULAR);
                break;
            case EQUIRECTANGULAR:
            default:
                setOutputProjection(OutputProjection.RECTILINEAR);
                break;
        }
    }

    /**
     * Set output projection.
     *
     * @param outputProjection the new output projection.
     */
    public void setOutputProjection(OutputProjection outputProjection) {
        Logger.logF();

        if (outputProjection == mOutputProjection) {
            Logger.logD(TAG, "Output projection is already " + outputProjection);
            return;
        }

        mOutputProjection = outputProjection;
        resetView();
    }

    /**
     * Reset view to default orientation and zoom level.
     */
    public void resetView() {
        Logger.logF();

        if (null != mRotationYawValueAnimator) {
            mRotationYawValueAnimator.cancel();
            mRotationYawValueAnimator = null;
        }
/*
        if (null != mRotationPitchValueAnimator) {
            mRotationPitchValueAnimator.cancel();
            mRotationPitchValueAnimator = null;
        }
*/

        switch (mOutputProjection) {
            case RECTILINEAR:
                mRotationYawValueTargetDeg = 0.0f;
//                mRotationPitchValueTargetDeg = 0.0f;
                getOrionCamera().setRotationOffset(Quatf.fromEulerRotationZXYDeg(
                        0.0f, 0.0f, 0.0f));
                setPredefinedZoomLevel(1);
                setProjectionRectilinear();
                break;
            case LITTLEPLANET:
                mRotationYawValueTargetDeg = 0.0f;
//                mRotationPitchValueTargetDeg = -90.0f;
                getOrionCamera().setRotationOffset(Quatf.fromEulerRotationZXYDeg(
                        0.0f, -90.0f, 0.0f));
                setPredefinedZoomLevel(4);
                setProjectionLittlePlanet();
                break;
            case EQUIRECTANGULAR:
                mRotationYawValueTargetDeg = 0.0f;
                getOrionCamera().setRotationOffset(Quatf.fromEulerRotationZXYDeg(
                        0.0f, 0.0f, 0.0f));
                setPredefinedZoomLevel(1);
                setProjectionSource();
                break;
            default:
                Logger.logE(TAG, "Unhandled projection type: " + mOutputProjection);
        }
     }

    @Override
    public void onException(OrionTexture texture, Exception e) {}

    @Override
    public void onVideoPlayerCreated(OrionVideoTexture texture) {}

    @Override
    public void onVideoSourceURISet(OrionVideoTexture texture) {
        Logger.logF();

        // Assume buffering is needed when a new video stream URI is set. Show indicator.
        if (null != mBufferingIndicator) {
            mBufferingIndicator.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onVideoPrepared(OrionVideoTexture texture) {}

    @Override
    public void onVideoRenderingStart(OrionVideoTexture texture) {
        Logger.logF();

        // Video player tells it is ready to render the very first frame.
        // Playback starts now, so hide buffering indicator.
        if (null != mBufferingIndicator) {
            mBufferingIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public void onVideoStarted(OrionVideoTexture texture) {
        Logger.logF();

        mIsPlaying = true;
    }

    @Override
    public void onVideoPaused(OrionVideoTexture texture) {
        Logger.logF();

        mIsPlaying = false;
    }

    @Override
    public void onVideoStopped(OrionVideoTexture texture) {
        Logger.logF();

        mIsPlaying = false;
    }

    @Override
    public void onVideoCompleted(OrionVideoTexture texture) {
        Logger.logF();

        texture.seekTo(0);
        texture.play();
    }

    @Override
    public void onVideoPlayerDestroyed(OrionVideoTexture texture) {}

    @Override
    public void onVideoSeekStarted(OrionVideoTexture texture, long positionMs) {}

    @Override
    public void onVideoSeekCompleted(OrionVideoTexture texture, long positionMs) {}

    @Override
    public void onVideoPositionChanged(OrionVideoTexture texture, long positionMs) {}

    @Override
    public void onVideoDurationUpdate(OrionVideoTexture texture, long durationMs) {}

    @Override
    public void onVideoSizeChanged(OrionVideoTexture texture, int width, int height) {}

    @Override
    public void onVideoBufferingStart(OrionVideoTexture texture) {
        Logger.logF();

        // Show buffering indicator whenever the player begins buffering video.
        if (null != mBufferingIndicator) {
            mBufferingIndicator.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onVideoBufferingEnd(OrionVideoTexture texture) {
        Logger.logF();

        // Hide buffering indicator whenever the player ends buffering video.
        if (null != mBufferingIndicator) {
            mBufferingIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public void onVideoBufferingUpdate(OrionVideoTexture texture, int fromPercent, int toPercent) {}

    @Override
    public void onVideoError(OrionVideoTexture texture, int what, int extra) {}

    @Override
    public void onVideoInfo(OrionVideoTexture texture, int what, String message) {}
}
