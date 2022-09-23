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
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

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
 * An example of an Orion360 video player for playing a video stream on Android TV devices.
 *
 * Adding support for Android TV devices is not difficult. Follow official instructions
 * to create mandatory configurations and a separate activity that will be started on TVs:
 * https://developer.android.com/training/tv/start
 *
 * Here TVStreamPlayer activity is configured as the leanback launcher in the app's manifest.
 * Hence, Android TVs should launch this activity instead of the MainActivity. However, for
 * Android Studio, you may need to create a separate Run Configuration, where you manually select
 * this activity. More information:
 * https://stackoverflow.com/questions/28298009/android-tv-not-starting-launch-leanback-activity
 * Since this activity is intended for TVs, it is hidden from phone/tablet examples list.
 *
 * The UI of the app should be designed differently for TVs. There are various AndroidX libraries
 * that provide suitable widgets, and also instructions for creating proper layouts & navigation:
 * https://developer.android.com/training/tv/start/libraries
 * https://developer.android.com/training/tv/start/layouts
 * https://developer.android.com/training/tv/start/navigation
 *
 * From Orion360 and 360° content point of view, the major difference is how users control the view.
 * TVs typically don't have touch screen nor movement sensors. Hence, we need to listen to the
 * remote control unit's key events. In some cases, there can be a game controller connected, which
 * has more keys. Yet, you still need to design the UI so that it works with the minimal key set:
 * - D-pad (up, down, left, right)
 * - Select button
 * - Home button
 * - Back button
 *
 * For proper 360° viewing user experience, users should be able to control panning, zooming and
 * perhaps also projection changes. For video content there should be play/pause, seek, etc. usual
 * video player controls. Obviously, one cannot assign a separate remote control key for each
 * feature, since the minimal key set is so limited. Some kind of a toolbar UI, where user can
 * change what feature to control, is needed. This activity contains one example design.
 *
 * Since panning and zooming via key presses results to jerky movement, it is highly recommended
 * to use animation. More specifically, we recommend using animations that apply inertia - the
 * sphere that contains the 360° content should appear to have a non-zero mass, so that it
 * accelerates and decelerates at the ends of the animation. This makes viewing more pleasant
 * especially for viewers with large TV screens. There are multiple options, such as Android's
 * animation framework, Orion360's animation framework, and custom code implementation.
 *
 * Notice that we need to handle different kinds of key event sequences: single key presses,
 * multiple key presses in quick sequence (i.e. new event comes before previous animation ends),
 * as well as holding a key pressed (repeated events). This variety makes it tricky to create
 * smooth animations: it is very typical, that new key event will interrupt an ongoing animation
 * for example when panning a 360° view. User is not happy if we disregard new event, but on the
 * other hand, restarting the animation with new values produces jerky movement.
 *
 * We need to acknowledge that this particular use case is not the usual "move this from here
 * to there smoothly" kind of animation, where existing animation frameworks are targeted.
 * This requires a more dynamic system, like controlling an object in a computer game. Hence, we
 * will use custom code solution and simple physics based animation with inertia and mass.
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

    /** Hysteresis for zoom (do not perform additional "zoom in" step if we are "almost there"). */
    private static final float ZOOM_HYSTERESIS = 0.05f;

    /** Buffering indicator, to be shown while buffering video from the network. */
    private ProgressBar mBufferingIndicator;

    /** Control panel view. */
    private View mControlPanelView;

    /** Play button. */
    private ImageButton mPlayButton;

    /** Zoom in button. */
    private ImageButton mZoomInButton;

    /** Zoom out button. */
    private ImageButton mZoomOutButton;

    /** Projection button. */
    private ImageButton mProjectionButton;

    /** Flag for indicating if video is currently being played. */
    private boolean mIsPlaying = false;

    /** Flag for turning rotation animation on/off. */
    private boolean mAnimateRotation = true;

    /** Yaw rotation value animator. */
    private ValueAnimator mRotationYawValueAnimator;

    /** Target value for yaw rotation (in degrees). */
    private float mRotationYawValueTargetDeg = 0.0f;

    /** Pitch rotation value animator. */
    private ValueAnimator mRotationPitchValueAnimator;

    /** Target value for pitch rotation (in degrees). */
    private float mRotationPitchValueTargetDeg = 0.0f;

    /** Current zoom level. */
    private float mZoomLevel = 0.0f;

    /** Zoom value animator. */
    private ValueAnimator mZoomValueAnimator;

    /** Flag for turning zoom animation on/off. */
    private boolean mAnimateZoom = true;

    /** Supported projection types. */
    private enum OutputProjection {
        EQUIRECTANGULAR,
        RECTILINEAR,
        LITTLEPLANET,
    }

    /** Currently selected output projection. */
    private OutputProjection mOutputProjection = OutputProjection.RECTILINEAR;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // Call super class implementation FIRST to set up a simple Orion360 player configuration.
        super.onCreate(savedInstanceState);

        // Above call will fail if a valid Orion360 license file for the package name defined in
        // the application's manifest/build.gradle files cannot be found!

        // Set layout.
        setContentView(R.layout.activity_video_player_tv);

        // Get buffering indicator.
        mBufferingIndicator = findViewById(R.id.buffering_indicator);

        // Get control panel.
        mControlPanelView = findViewById(R.id.player_controls_panel);

        // Play button.
        mPlayButton = findViewById(R.id.player_controls_play_button);
        mPlayButton.setOnClickListener(view -> {
            Logger.logF();

            if (mIsPlaying) {
                getOrionTexture().pause();
            } else {
                getOrionTexture().play();
            }
        });

        // Zoom in button.
        mZoomInButton = findViewById(R.id.player_controls_zoom_in_button);
        mZoomInButton.setOnClickListener(view -> {
            Logger.logF();

            zoomIn();
        });

        // Zoom out button.
        mZoomOutButton = findViewById(R.id.player_controls_zoom_out_button);
        mZoomOutButton.setOnClickListener(view -> {
            Logger.logF();

            zoomOut();
        });

        // Projection button.
        mProjectionButton = findViewById(R.id.player_controls_projection_button);
        mProjectionButton.setOnClickListener(view -> {
            Logger.logF();

            toggleProjection();
        });

        // Set Orion360 view (defined in the layout) that will be used for rendering 360 content.
        setOrionView(R.id.orion_view_container);

        // Set a URI that points to an image or video stream URL.
        //setContentUri(MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_IMAGE_FILE_LIVINGROOM_HQ);
        //setContentUri(MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);
        setContentUri(MainMenu.TEST_VIDEO_URI_HLS);

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
                // Navigate back -> hide control panel or quit the app.
                if (mControlPanelView.getVisibility() == View.VISIBLE) {
                    mControlPanelView.setVisibility(View.GONE);
                } else {
                    finish();
                }
                return true;
            case KeyEvent.KEYCODE_BUTTON_SELECT:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                // Selection -> show control panel if not visible.
                if (mControlPanelView.getVisibility() == View.GONE) {
                    mControlPanelView.setVisibility(View.VISIBLE);
                    mPlayButton.requestFocus();
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_UP:
                // Up -> pan up.
                if (mControlPanelView.getVisibility() == View.GONE) {
                    rotatePitch(15);
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // Down -> pan down.
                if (mControlPanelView.getVisibility() == View.GONE) {
                    rotatePitch(-15);
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                // Left -> pan left.
                if (mControlPanelView.getVisibility() == View.GONE) {
                    rotateYaw(45);
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // Right -> pan right.
                if (mControlPanelView.getVisibility() == View.GONE) {
                    rotateYaw(-45);
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_MOVE_HOME:
                // Home -> reset view to default position.
                // NOTE: capturing HOME key may not work, could navigate to Android TV home instead.
                resetProjection();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    // ------------------------------------------ Zoom ---------------------------------------------

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
            Quatf rotationDelta = Quatf.fromEulerRotationZXYDeg(degrees, 0.0f, 0.0f);
            camera.setRotationOffset(currentRotation.multiply(rotationDelta));
        }
    }

    /**
     * Rotate camera pitch angle. Positive value upwards, negative downwards.
     *
     * @param degrees the amount of degrees to rotate.
     */
    public void rotatePitch(float degrees) {
        Logger.logD(TAG, "rotatePitch(): " + degrees);

        // Adjust rotation step by zoom level.
        float rotationFactor = (float) (Math.pow(2.0f, mZoomLevel));
        degrees /= Math.max(1.0f, rotationFactor);

        OrionCamera camera = getOrionCamera();

        if (mAnimateRotation) {

            if (null != mRotationPitchValueAnimator) {
                mRotationPitchValueAnimator.cancel();
                mRotationPitchValueAnimator = null;
            }

            mRotationPitchValueTargetDeg += degrees;

            Quatf currentRotation = camera.getRotationOffset();
            Vec3f lookAt = Vec3f.FRONT.rotate(currentRotation);
            float yawDeg = (float) Math.toDegrees(-lookAt.getYaw());
            float pitchDeg = (float) Math.toDegrees(lookAt.getPitch());

            float from = pitchDeg;
            float to = mRotationPitchValueTargetDeg % 360.0f;
            float min = Math.min(from, to);
            if (Math.abs(to - from) > HALF_CIRCLE) {
                if (min == from) from += WHOLE_CIRCLE;
                else to += WHOLE_CIRCLE;
            }
            Logger.logD(TAG, "Rotation from=" + pitchDeg + " to="
                    + mRotationPitchValueTargetDeg + " -> from=" + from + " to=" + to);

            mRotationPitchValueAnimator = ValueAnimator.ofFloat(from, to);
            mRotationPitchValueAnimator.addUpdateListener(valueAnimator -> {
                //Logger.logF(); // Prevent flooding the log.

                float animatedValue = (float) valueAnimator.getAnimatedValue();
                //Logger.logD(TAG, "Anim pitch value " + animatedValue);
                Quatf newRotationOffset = Quatf.fromEulerRotationZXYDeg(
                        yawDeg, animatedValue, 0.0f);
                camera.setRotationOffset(newRotationOffset);

            });
            mRotationPitchValueAnimator.setDuration(ROTATION_ANIMATION_LENGTH_MS);
            mRotationPitchValueAnimator.start();

        } else {
            Quatf currentRotation = camera.getRotationOffset();
            Quatf rotationDelta = Quatf.fromEulerRotationZXYDeg(0.0f, degrees, 0.0f);
            camera.setRotationOffset(currentRotation.multiply(rotationDelta));
        }
    }

    // ------------------------------------------ Zoom ---------------------------------------------

    /**
     * Set zoom level in range [0-10].
     *
     * @param zoomLevel the new zoom level.
     */
    public void setPredefinedZoomLevel(int zoomLevel) {
        Logger.logF();

        OrionCamera camera = getOrionCamera();

        float maxValue = (float) (Math.log(camera.getZoomMax()) / Math.log(2));
        float stepSize = maxValue / ZOOM_LEVEL_COUNT;
        float newValue = stepSize * zoomLevel;
        float orionValue = (float) (Math.pow(2.0f, newValue));

        if (orionValue < 1.0f) {
            doZoom(0.0f, maxValue, 1.0f);
        } else if (orionValue <= camera.getZoomMax()) {
            doZoom(newValue, maxValue, orionValue);
        } else if ((camera.getZoom() + ZOOM_HYSTERESIS) < camera.getZoomMax()) {
            doZoom(newValue, maxValue, camera.getZoomMax());
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

        float maxValue = (float) (Math.log(camera.getZoomMax()) / Math.log(2));
        float stepSize = maxValue / ZOOM_LEVEL_COUNT;
        float newValue = mZoomLevel + stepSize;
        float orionValue = (float) (Math.pow(2.0f, newValue));

        if (orionValue <= camera.getZoomMax()) {
            doZoom(newValue, maxValue, orionValue);
        } else if ((camera.getZoom() + ZOOM_HYSTERESIS) < camera.getZoomMax()) {
            doZoom(newValue, maxValue, camera.getZoomMax());
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

        float maxValue = (float) (Math.log(camera.getZoomMax()) / Math.log(2));
        float stepSize = maxValue / ZOOM_LEVEL_COUNT;
        float newValue = mZoomLevel - stepSize;
        float orionValue = (float) (Math.pow(2.0f, newValue));

        if (orionValue >= 1.0f) {
            doZoom(newValue, maxValue, orionValue);
        } else {
            doZoom(0.0f, maxValue, 1.0f);
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

    // --------------------------------------- Projection ------------------------------------------

    /**
     * Toggle next projection. Called when control panel button is clicked.
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
     * @param outputProjection the desired output projection.
     */
    public void setOutputProjection(@NonNull OutputProjection outputProjection) {
        Logger.logF();

        if (outputProjection == mOutputProjection) {
            Logger.logD(TAG, "Output projection is already " + outputProjection);
            return;
        }

        // Change projection and configure other view parameters for the new projection.
        mOutputProjection = outputProjection;
        resetProjection();
    }

    /**
     * Reset view orientation, zoom etc. based on currently selected projection.
     */
    private void resetProjection() {
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

    /**
     * Set rectilinear projection.
     */
    public void setProjectionRectilinear() {
        Logger.logF();

        getOrionPanorama().setPanoramaType(OrionPanorama.PanoramaType.SPHERE);
        getOrionPanorama().setRenderingMode(OrionSceneItem.RenderingMode.PERSPECTIVE);
        getOrionCamera().setProjectionMode(OrionCamera.ProjectionMode.RECTILINEAR);
        mProjectionButton.setImageDrawable(AppCompatResources.getDrawable(
                this, R.drawable.rectilinear));
    }

    /**
     * Set little planet projection.
     */
    public void setProjectionLittlePlanet() {
        Logger.logF();

        getOrionPanorama().setPanoramaType(OrionPanorama.PanoramaType.SPHERE);
        getOrionPanorama().setRenderingMode(OrionSceneItem.RenderingMode.PERSPECTIVE);
        getOrionCamera().setProjectionMode(OrionCamera.ProjectionMode.LITTLEPLANET);
        mProjectionButton.setImageDrawable(AppCompatResources.getDrawable(
                this, R.drawable.littleplanet));
    }

    /**
     * Set source projection.
     */
    public void setProjectionSource() {
        Logger.logF();

        getOrionPanorama().setPanoramaType(OrionPanorama.PanoramaType.PANEL_SOURCE);
        getOrionPanorama().setRenderingMode(OrionSceneItem.RenderingMode.CAMERA_DISABLED);
        getOrionCamera().setProjectionMode(OrionCamera.ProjectionMode.RECTILINEAR);
        mProjectionButton.setImageDrawable(AppCompatResources.getDrawable(
                this, R.drawable.source));
    }

    // -------------------------------- OrionVideoTexture.Listener ---------------------------------

    @Override
    public void onException(OrionTexture texture, Exception e) {
    }

    @Override
    public void onVideoPlayerCreated(OrionVideoTexture texture) {
    }

    @Override
    public void onVideoSourceURISet(OrionVideoTexture texture) {
        Logger.logF();

        // Assume buffering is needed when a new video stream URI is set. Show indicator.
        if (null != mBufferingIndicator) {
            mBufferingIndicator.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onVideoPrepared(OrionVideoTexture texture) {
    }

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
        mPlayButton.setImageDrawable(AppCompatResources.getDrawable(
                this, R.drawable.pause));
    }

    @Override
    public void onVideoPaused(OrionVideoTexture texture) {
        Logger.logF();

        mIsPlaying = false;
        mPlayButton.setImageDrawable(AppCompatResources.getDrawable(
                this, R.drawable.play));
    }

    @Override
    public void onVideoStopped(OrionVideoTexture texture) {
        Logger.logF();

        mIsPlaying = false;
        mPlayButton.setImageDrawable(AppCompatResources.getDrawable(
                this, R.drawable.play));
    }

    @Override
    public void onVideoCompleted(OrionVideoTexture texture) {
        Logger.logF();

        // Loop.
        texture.seekTo(0);
        texture.play();
    }

    @Override
    public void onVideoPlayerDestroyed(OrionVideoTexture texture) {
    }

    @Override
    public void onVideoSeekStarted(OrionVideoTexture texture, long positionMs) {
    }

    @Override
    public void onVideoSeekCompleted(OrionVideoTexture texture, long positionMs) {
    }

    @Override
    public void onVideoPositionChanged(OrionVideoTexture texture, long positionMs) {
    }

    @Override
    public void onVideoDurationUpdate(OrionVideoTexture texture, long durationMs) {
    }

    @Override
    public void onVideoSizeChanged(OrionVideoTexture texture, int width, int height) {
    }

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
    public void onVideoBufferingUpdate(OrionVideoTexture texture, int fromPercent, int toPercent) {
    }

    @Override
    public void onVideoError(OrionVideoTexture texture, int what, int extra) {
    }

    @Override
    public void onVideoInfo(OrionVideoTexture texture, int what, String message) {
    }
}
