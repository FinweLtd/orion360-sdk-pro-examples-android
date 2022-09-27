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
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import fi.finwe.log.Logger;
import fi.finwe.math.Quatf;
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
 * The UI of the app should be designed differently for TVs. There are various helpful AndroidX
 * libraries as well as instructions for creating proper layouts & navigation:
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
 * Of course, nothing is stopping your from adding support for optional keys as shortcuts.
 *
 * For proper 360° viewing user experience, users should be able to control panning, zooming and
 * perhaps also projection changes. For video content there should be play/pause, seek, etc. usual
 * video player controls. Obviously, one cannot just assign a separate remote control key for each
 * feature, since the minimal key set is so limited. Some kind of a control panel, where users
 * can change what feature to control, is needed. This activity contains simple example design.
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
 * as well as holding a key pressed (repeated events). This variety makes it a bit tricky to create
 * smooth animations: it is very typical, that new key event will interrupt an ongoing animation
 * for example when panning a 360° view. User is not happy if we completely disregard new event,
 * but on the other hand, restarting the animation with new values may produce jerky movement.
 */
@SuppressWarnings("unused")
public class TVStreamPlayer extends SimpleOrionActivity implements OrionVideoTexture.Listener,
        SeekBar.OnSeekBarChangeListener {

    /** Tag for logging. */
    public static final String TAG = TVStreamPlayer.class.getSimpleName();

    /** Automatically hide the control panel after this delay of inactivity. */
    private static final int AUTO_HIDE_CONTROL_PANEL_DELAY_MS = 4000;

    /** Rotation animation length, impulse from single key press (in ms). */
    private static final int ROTATION_ANIMATION_LENGTH_IMPULSE_MS = 800;

    /** Rotation animation length, continuous panning when key is held down (in ms). */
    private static final int ROTATION_ANIMATION_LENGTH_INFINITE_MS = 72000;

    /** Zoom animation length (in ms). */
    private static final int ZOOM_ANIMATION_LENGTH_MS = 800;

    /** The number of zoom levels (steps). */
    private static final int ZOOM_LEVEL_COUNT = 10;

    /** Hysteresis for zoom (do not perform additional "zoom in" step if we are "almost there"). */
    private static final float ZOOM_HYSTERESIS = 0.05f;

    /**
     * The amount of time to step when user seeks backward/forward (in ms). Notice that this
     * will be a very approximate value in practice, as media players seek to the nearest keyframe.
     */
    private static final int SEEK_STEP_MS = 5000;

    /** Buffering indicator, to be shown while buffering video from the network. */
    private ProgressBar mBufferingIndicator;

    /** Control panel view. */
    private View mControlPanelView;

    /** Elapsed time text. */
    private TextView mElapsedTime;

    /** Seek bar. */
    private SeekBar mSeekBar;

    /** Duration time text. */
    private TextView mDurationTime;

    /** Play button. */
    private ImageButton mPlayButton;

    /** Zoom in button. */
    @SuppressWarnings("FieldCanBeLocal")
    private ImageButton mZoomInButton;

    /** Zoom out button. */
    @SuppressWarnings("FieldCanBeLocal")
    private ImageButton mZoomOutButton;

    /** Projection button. */
    private ImageButton mProjectionButton;

    /** Current video position. */
    protected long mPositionMs = 0;

    /** Current video duration. */
    protected long mDurationMs = 0;

    /** Flag for indicating if video is currently being played. */
    private boolean mIsPlaying = false;

    /** Flag for turning rotation animation on/off. We recommend using animation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean mAnimateRotation = true;

    /** Flag for turning zoom animation on/off. */
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean mAnimateZoom = true;

    /** Yaw rotation value animator. */
    private ValueAnimator mRotationYawValueAnimator;

    /** Pitch rotation value animator. */
    private ValueAnimator mRotationPitchValueAnimator;

    /** Zoom value animator. */
    private ValueAnimator mZoomValueAnimator;

    /** Last key event. */
    private KeyEvent mLastKeyEventDown = null;

    /** Current zoom level. */
    private float mZoomLevel = 0.0f;

    /** Supported projection types. */
    private enum OutputProjection {
        EQUIRECTANGULAR,
        RECTILINEAR,
        LITTLEPLANET,
    }

    /** Currently selected output projection. */
    private OutputProjection mOutputProjection = OutputProjection.RECTILINEAR;

    /** Animation for showing the control panel. */
    private Animation mShowControlPanelAnimation;

    /** Animation for hiding the control panel. */
    private Animation mHideControlPanelAnimation;

    /** Handler for automatically hiding the control panel after a delay. */
    private final Handler mAutoHideControlPanelHandler = new Handler();


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

        // Position (elapsed time) text.
        mElapsedTime = findViewById(R.id.player_controls_position_text);
        updatePositionLabel();

        // Seek bar.
        mSeekBar = findViewById(R.id.player_controls_seekbar);
        mSeekBar.setOnSeekBarChangeListener(this);
        updateSeekBar();

        // Duration (total time) text.
        mDurationTime = findViewById(R.id.player_controls_duration_text);
        updateDurationLabel();

        // Play/pause button.
        mPlayButton = findViewById(R.id.player_controls_play_button);
        mPlayButton.setOnClickListener(view -> {
            Logger.logF();

            // Control video playback via OrionVideoTexture.
            if (getOrionTexture() instanceof OrionVideoTexture) {
                if (mIsPlaying) {
                    getOrionTexture().pause();
                } else {
                    getOrionTexture().play();
                }
            }
            restartAutoHideDelay();
        });

        // Zoom in button.
        mZoomInButton = findViewById(R.id.player_controls_zoom_in_button);
        mZoomInButton.setOnClickListener(view -> {
            Logger.logF();

            zoomIn();
            restartAutoHideDelay();
        });

        // Zoom out button.
        mZoomOutButton = findViewById(R.id.player_controls_zoom_out_button);
        mZoomOutButton.setOnClickListener(view -> {
            Logger.logF();

            zoomOut();
            restartAutoHideDelay();
        });

        // Projection button.
        mProjectionButton = findViewById(R.id.player_controls_projection_button);
        mProjectionButton.setOnClickListener(view -> {
            Logger.logF();

            toggleProjection();
            restartAutoHideDelay();
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

        // Re-configure 3D world camera.
        OrionCamera camera = getOrionCamera();
        camera.setProjectionPerspectiveDeg(OrionCamera.FovType.HORIZONTAL,
                120.0f, 0.1f, 100.0f);
        camera.setZoomMax(7.0f);

        // Load animations.
        mShowControlPanelAnimation = AnimationUtils.loadAnimation(this,
                R.anim.slide_in_from_bottom);
        mShowControlPanelAnimation.setFillAfter(true);
        mHideControlPanelAnimation = AnimationUtils.loadAnimation(this,
                R.anim.slide_out_to_bottom);
        mHideControlPanelAnimation.setFillAfter(true);
        mHideControlPanelAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mControlPanelView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        restartAutoHideDelay();
    }

    // ------------------------------------- Key handling ------------------------------------------

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Logger.logD(TAG, "onKeyDown(), keyCode=" + keyCode + " keyEvent=" + event);

        // If control panel is visible, any key press resets auto hide delay counting.
        if (mControlPanelView.getVisibility() == View.VISIBLE) {
            restartAutoHideDelay();
        }

        OrionTexture orionTexture = getOrionTexture();

        // Handle TV's remote controller key presses (key down events).

        // Handle basic navigation keys.
        switch (keyCode) {
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_MOVE_HOME:
                // Home -> return to Android TV home (default action).
                return false;
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_BACK:
                // Back -> hide control panel if visible, or quit the app.
                if (mControlPanelView.getVisibility() == View.VISIBLE) {
                    hideControlPanel();
                } else {
                    finish();
                }
                return true;
            case KeyEvent.KEYCODE_BUTTON_SELECT:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                // Selection -> show control panel if not visible, else select (default action).
                if (mControlPanelView.getVisibility() != View.VISIBLE) {
                    showControlPanel();
                    return true;
                }
                return false;
        }

        // Here we handle only the case where an arrow key is held down to pan with constant speed.
        // We also have to store the key event, so that when the key is lifted, we can check whether
        // user short pressed or long pressed and act accordingly. That is handled in onKeyUp().
        mLastKeyEventDown = event;
        if (mControlPanelView.getVisibility() != View.VISIBLE) {
            if (event.getRepeatCount() == 1) {
                // Long press detected, start constant speed pan animation.
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                        rotatePitch(3600, true, true);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        rotatePitch(-3600, true, true);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        rotateYaw(3600, true, true);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        rotateYaw(-3600, true, true);
                        return true;
                }
            }
        }

        // Optional extra keys that are found from many Android TV remote controllers:
        switch (keyCode) {

            // Allow zooming with PROGRAM UP/DOWN keys. Quick zooming by holding the key also works.
            case KeyEvent.KEYCODE_CHANNEL_UP:
                // Prog+ -> zoom in.
                zoomIn();
                return true;
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
                // Prog- -> zoom out.
                zoomOut();
                return true;

            // Reset view with red key. Allow selecting projection with other color keys.
            case KeyEvent.KEYCODE_PROG_RED:
                // Red -> reset view to center and default zoom level.
                if (event.getRepeatCount() == 0) {
                    resetProjection();
                }
                return true;
            case KeyEvent.KEYCODE_PROG_GREEN:
                // Green -> Rectilinear.
                if (event.getRepeatCount() == 0) {
                    setOutputProjection(OutputProjection.RECTILINEAR);
                }
                return true;
            case KeyEvent.KEYCODE_PROG_YELLOW:
                // Yellow -> Little planet.
                if (event.getRepeatCount() == 0) {
                    setOutputProjection(OutputProjection.LITTLEPLANET);
                }
                return true;
            case KeyEvent.KEYCODE_PROG_BLUE:
                // Red -> Source.
                if (event.getRepeatCount() == 0) {
                    setOutputProjection(OutputProjection.EQUIRECTANGULAR);
                }
                return true;

            // Control media with play/pause keys.
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                // Play -> play/pause playback.
                if (getOrionTexture() instanceof OrionVideoTexture) {
                    if (mIsPlaying) {
                        getOrionTexture().pause();
                    } else {
                        getOrionTexture().play();
                    }
                    return true;
                }

            // Seek media with seek backward/forward keys.
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                // Rewind -> seek backward.
                if (orionTexture instanceof OrionVideoTexture) {
                    OrionVideoTexture orionVideoTexture = (OrionVideoTexture) orionTexture;
                    long position = orionVideoTexture.getCurrentPosition();
                    orionVideoTexture.seekTo(Math.max(0, (int)position - SEEK_STEP_MS));
                    return true;
                }
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                // Fast forward -> seek forward.
                if (orionTexture instanceof OrionVideoTexture) {
                    OrionVideoTexture orionVideoTexture = (OrionVideoTexture) orionTexture;
                    long position = orionVideoTexture.getCurrentPosition();
                    long duration = orionVideoTexture.getDuration();
                    orionVideoTexture.seekTo(Math.min((int)duration, (int)position + SEEK_STEP_MS));
                    return true;
                }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Logger.logD(TAG, "onKeyUp(), keyCode=" + keyCode + " keyEvent=" + event);

        // Handle TV's remote controller key presses (key up events).

        // If long press occurred, stop pan animation when key is lifted.
        if (null != mLastKeyEventDown && mLastKeyEventDown.getRepeatCount() > 0) {
            if (null != mRotationYawValueAnimator) {
                mRotationYawValueAnimator.cancel();
                mRotationYawValueAnimator = null;
            }
            if (null != mRotationPitchValueAnimator) {
                mRotationPitchValueAnimator.cancel();
                mRotationPitchValueAnimator = null;
            }
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                // Up -> pan up.
                if (mControlPanelView.getVisibility() != View.VISIBLE
                        && mLastKeyEventDown.getRepeatCount() == 0) {
                    rotatePitch(30, mAnimateRotation, false);
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // Down -> pan down.
                if (mControlPanelView.getVisibility() != View.VISIBLE
                        && mLastKeyEventDown.getRepeatCount() == 0) {
                    rotatePitch(-30, mAnimateRotation, false);
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                // Left -> pan left.
                if (mControlPanelView.getVisibility() != View.VISIBLE
                        && mLastKeyEventDown.getRepeatCount() == 0) {
                    rotateYaw(45, mAnimateRotation, false);
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // Right -> pan right.
                if (mControlPanelView.getVisibility() != View.VISIBLE
                        && mLastKeyEventDown.getRepeatCount() == 0) {
                    rotateYaw(-45, mAnimateRotation, false);
                    return true;
                }
                return false;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    // ------------------------------------- Control Panel -----------------------------------------

    /**
     * Show control panel by sliding and fading it in.
     */
    public void showControlPanel() {
        Logger.logF();

        mShowControlPanelAnimation.cancel();
        mControlPanelView.bringToFront();
        mControlPanelView.setVisibility(View.VISIBLE);
        mControlPanelView.startAnimation(mShowControlPanelAnimation);
        mPlayButton.requestFocus();

        restartAutoHideDelay();
    }

    /**
     * Hide control panel by sliding and fading it out.
     */
    public void hideControlPanel() {
        Logger.logF();

        mAutoHideControlPanelHandler.removeCallbacks(mAutoHideRunnable);

        if (null != mShowControlPanelAnimation) {
            mShowControlPanelAnimation.cancel();
        }

        if (null != mControlPanelView) {
            mControlPanelView.bringToFront();
            mControlPanelView.setVisibility(View.VISIBLE);
            mControlPanelView.startAnimation(mHideControlPanelAnimation);
        }
    }

    /**
     * Restart auto hide delay for the control panel.
     */
    private void restartAutoHideDelay() {
        Logger.logF();

        mAutoHideControlPanelHandler.removeCallbacks(mAutoHideRunnable);
        mAutoHideControlPanelHandler.postDelayed(mAutoHideRunnable,
                AUTO_HIDE_CONTROL_PANEL_DELAY_MS);
    }

    /**
     * Hide control panel after a delay of inactivity.
     */
    private final Runnable mAutoHideRunnable = () -> {
        Logger.logF();

        hideControlPanel();
    };

    /**
     * Update current video position to control panel label.
     */
    protected void updatePositionLabel() {
        //Logger.logF(); // Prevent flooding the log.

        if (mElapsedTime != null) {
            mElapsedTime.setText(getTimeString(mPositionMs));
        }
    }

    /**
     * Update current video duration to control panel label.
     */
    protected void updateDurationLabel() {
        Logger.logF();

        if (mDurationTime != null) {
            mDurationTime.setText(getTimeString(mDurationMs));
        }
    }

    /**
     * Update seekbar.
     */
    protected void updateSeekBar() {
        //Logger.logF(); // Prevent flooding the log.

        if (mSeekBar != null) {
            mSeekBar.setMax((int)mDurationMs);
            mSeekBar.setProgress((int)mPositionMs);
        }
    }

    /**
     * Format given time value into a time string.
     *
     * @param timeMs the time value to format.
     * @return formatted string.
     */
    protected String getTimeString(long timeMs) {
        Logger.logF();

        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs)
                - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.US, "%d:%02d", minutes,	seconds);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Logger.logF();

        // Nothing to do
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Logger.logD(TAG, "onProgressChanged() progress=" + progress + " fromUser=" + fromUser);

        if (fromUser) {
            OrionTexture orionTexture = getOrionTexture();
            if (orionTexture instanceof OrionVideoTexture) {
                OrionVideoTexture orionVideoTexture = (OrionVideoTexture) orionTexture;
                orionVideoTexture.seekTo(progress);
            }
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Logger.logF();
    }

    // ------------------------------------------ Pan ----------------------------------------------

    /**
     * Rotate camera yaw angle. Positive value towards right (clockwise), negative towards left.
     *
     * @param degrees the amount of degrees to rotate and direction of rotation.
     * @param animate set to true to animate rotation.
     * @param infinite set to true to keep rotating infinitely until cancelled.
     */
    public void rotateYaw(float degrees, boolean animate, boolean infinite) {
        Logger.logD(TAG, "rotateYaw(): " + degrees);

        // Take current zoom level into account and reduce rotation angle when zoomed in.
        degrees = adjustRotationAngle(degrees, mZoomLevel);

        // Cancel previous rotation animation, if any.
        if (null != mRotationYawValueAnimator) {
            mRotationYawValueAnimator.cancel();
            mRotationYawValueAnimator = null;
        }

        OrionCamera camera = getOrionCamera();
        if (!animate) {
            // Apply rotation step directly to the current camera offset without animation.
            Quatf currentRotation = camera.getRotationOffset();
            Quatf deltaRotation = Quatf.fromEulerRotationZXYDeg(
                    degrees, 0.0f, 0.0f);
            camera.setRotationOffset(deltaRotation.multiply(currentRotation));
        } else {
            // Apply rotation step incrementally with animation.
            mRotationYawValueAnimator = ValueAnimator.ofFloat(0, degrees);
            AtomicReference<Float> previousValue = new AtomicReference<>(0.0f);
            mRotationYawValueAnimator.addUpdateListener(valueAnimator -> {
                //Logger.logF(); // Prevent flooding the log.
                float value = (float) valueAnimator.getAnimatedValue();
                float delta = value - previousValue.get();
                previousValue.set(value);
                Quatf currentRotation = camera.getRotationOffset();
                Quatf deltaRotation = Quatf.fromEulerRotationZXYDeg(
                        delta, 0.0f, 0.0f);
                camera.setRotationOffset(deltaRotation.multiply(currentRotation));
            });
            if (infinite) {
                mRotationYawValueAnimator.setInterpolator(new LinearInterpolator());
                mRotationYawValueAnimator.setRepeatMode(ValueAnimator.RESTART);
                mRotationYawValueAnimator.setRepeatCount(ValueAnimator.INFINITE);
                mRotationYawValueAnimator.setDuration(ROTATION_ANIMATION_LENGTH_INFINITE_MS);
            } else {
                mRotationYawValueAnimator.setDuration(ROTATION_ANIMATION_LENGTH_IMPULSE_MS);
            }
            mRotationYawValueAnimator.start();
        }
    }

    /**
     * Rotate camera pitch angle. Positive value upwards, negative downwards.
     *
     * @param degrees the amount of degrees to rotate and direction of rotation.
     * @param animate set to true to animate rotation.
     * @param infinite set to true to keep rotating infinitely until cancelled.
     */
    public void rotatePitch(float degrees, boolean animate, boolean infinite) {
        Logger.logD(TAG, "rotatePitch(): " + degrees);

        // Take current zoom level into account and reduce rotation angle when zoomed in.
        degrees = adjustRotationAngle(degrees, mZoomLevel);

        // Cancel previous rotation animation, if any.
        if (null != mRotationPitchValueAnimator) {
            mRotationPitchValueAnimator.cancel();
            mRotationPitchValueAnimator = null;
        }

        OrionCamera camera = getOrionCamera();
        if (!animate) {
            // Apply rotation step directly to the current camera offset without animation.
            Quatf currentRotation = camera.getRotationOffset();
            Quatf deltaRotation = Quatf.fromEulerRotationZXYDeg(
                    0.0f, degrees, 0.0f);
            camera.setRotationOffset(currentRotation.multiply(deltaRotation));
        } else {
            // Apply rotation step incrementally with animation.
            mRotationPitchValueAnimator = ValueAnimator.ofFloat(0, degrees);
            AtomicReference<Float> previousValue = new AtomicReference<>(0.0f);
            mRotationPitchValueAnimator.addUpdateListener(valueAnimator -> {
                //Logger.logF(); // Prevent flooding the log.
                float value = (float) valueAnimator.getAnimatedValue();
                float delta = value - previousValue.get();
                previousValue.set(value);
                Quatf currentRotation = camera.getRotationOffset();
                Quatf deltaRotation = Quatf.fromEulerRotationZXYDeg(
                        0.0f, delta, 0.0f);
                camera.setRotationOffset(currentRotation.multiply(deltaRotation));
            });
            if (infinite) {
                mRotationPitchValueAnimator.setInterpolator(new LinearInterpolator());
                mRotationPitchValueAnimator.setRepeatMode(ValueAnimator.RESTART);
                mRotationPitchValueAnimator.setRepeatCount(ValueAnimator.INFINITE);
                mRotationPitchValueAnimator.setDuration(ROTATION_ANIMATION_LENGTH_INFINITE_MS);
            } else {
                mRotationPitchValueAnimator.setDuration(ROTATION_ANIMATION_LENGTH_IMPULSE_MS);
            }
            mRotationPitchValueAnimator.start();
        }
    }

    /**
     * Adjust rotation angle with current zoom level.
     *
     * @param originalAngle the angle to be adjusted.
     * @param zoom the current zoom level.
     * @return the adjusted angle (same or smaller than the original angle).
     */
    private float adjustRotationAngle(float originalAngle, float zoom) {
        //Logger.logF(); // Prevent flooding the log.

        // The amount of rotation should depend on the current zoom level (zoomed in -> turn less).
        // Here we adjust rotation step by zoom level using equation 1/2^x, which gradually
        // decreases rotation angle when zoom level increases, giving a fairly natural response.
        float rotationFactor = (float) (Math.pow(2.0f, zoom));
        float angle = originalAngle / Math.max(1.0f, rotationFactor);
        Logger.logD(TAG, "Zoom level: " + mZoomLevel + ", adjusted rotation angle=" + angle);

        return angle;
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

        if (null != mRotationPitchValueAnimator) {
            mRotationPitchValueAnimator.cancel();
            mRotationPitchValueAnimator = null;
        }

        switch (mOutputProjection) {
            case RECTILINEAR:
                getOrionCamera().setRotationOffset(Quatf.fromEulerRotationZXYDeg(
                        0.0f, 0.0f, 0.0f));
                setPredefinedZoomLevel(1);
                setProjectionRectilinear();
                break;
            case LITTLEPLANET:
                getOrionCamera().setRotationOffset(Quatf.fromEulerRotationZXYDeg(
                        0.0f, -90.0f, 0.0f));
                setPredefinedZoomLevel(4);
                setProjectionLittlePlanet();
                break;
            case EQUIRECTANGULAR:
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
    private void setProjectionRectilinear() {
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
    private void setProjectionLittlePlanet() {
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
    private void setProjectionSource() {
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
        mPositionMs = positionMs;
        updatePositionLabel();
        updateSeekBar();
    }

    @Override
    public void onVideoSeekCompleted(OrionVideoTexture texture, long positionMs) {
        mPositionMs = positionMs;
        updatePositionLabel();
        updateSeekBar();
    }

    @Override
    public void onVideoPositionChanged(OrionVideoTexture texture, long positionMs) {
        mPositionMs = positionMs;
        updatePositionLabel();
        updateSeekBar();
    }

    @Override
    public void onVideoDurationUpdate(OrionVideoTexture texture, long durationMs) {
        mDurationMs = durationMs;
        updateDurationLabel();
        updateSeekBar();
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
