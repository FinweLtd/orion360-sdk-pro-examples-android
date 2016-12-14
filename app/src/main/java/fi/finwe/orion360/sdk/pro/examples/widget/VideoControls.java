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

package fi.finwe.orion360.sdk.pro.examples.widget;

import android.app.Activity;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import fi.finwe.math.Vec2F;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionContext;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.controllable.DisplayClickable;
import fi.finwe.orion360.sdk.pro.controller.RotationAligner;
import fi.finwe.orion360.sdk.pro.controller.TouchDisplayClickListener;
import fi.finwe.orion360.sdk.pro.controller.TouchPincher;
import fi.finwe.orion360.sdk.pro.controller.TouchRotater;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.source.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.viewport.fx.BarrelDistortion;
import fi.finwe.orion360.sdk.pro.widget.ControlPanel;
import fi.finwe.orion360.sdk.pro.widget.OrionWidget;
import fi.finwe.util.ContextUtil;

import static fi.finwe.orion360.sdk.pro.OrionContext.getActivity;

/**
 * An example of creating custom video controls.
 * <p/>
 * Features:
 * <ul>
 * <li>Plays one hard-coded full spherical (360x180) equirectangular video
 * <li>Creates a fullscreen view locked to landscape orientation
 * <li>Auto-starts playback on load and stops when playback is completed
 * <li>Renders the video using standard rectilinear projection
 * <li>Allows navigation with touch & movement sensors (if supported by HW) as follows:
 * <ul>
 * <li>Panning (gyro or swipe)
 * <li>Zooming (pinch)
 * <li>Tilting (pinch rotate)
 * </ul>
 * <li>Auto Horizon Aligner (AHL) feature straightens the horizon</li>
 * </ul>
 */
public class VideoControls extends OrionActivity implements OrionVideoTexture.Listener {

    /** The Android view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our panorama sphere will be added to. */
    protected OrionScene mScene;

    /** The panorama sphere where our video texture will be mapped to. */
    protected OrionPanorama mPanorama;

    /** The video texture where our decoded video frames will be updated to. */
    protected OrionTexture mPanoramaTexture;

    /** The camera which will project our 3D scene to a 2D (view) surface. */
    protected OrionCamera mCamera;

    /** The widget that will handle our panning, zooming & rotating touch gestures. */
    protected TouchControllerWidget mTouchController;

    /** Our custom implementation of an Orion360 control panel. */
    private MyControlPanel mControlPanel;

    /** The root view of the inflated control panel. */
    View mControlPanelView;

    /** The layout container that will hold our inflated control panel. */
    ViewGroup mControlPanelContainer;


    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Use an XML layout that has an Orion360 view and a placeholder for control panel.
		setContentView(R.layout.activity_video_player_with_controls);

        // Initialize Orion360.
        initOrion();

        // Listen for video texture events (buffering indicator).
        ((OrionVideoTexture)mPanoramaTexture).addTextureListener(this);

        // Create an instance of our custom control panel.
        mControlPanel = new MyControlPanel(this);

        // Get the placeholder for our control panel from the XML layout.
        mControlPanelContainer = (ViewGroup) findViewById(R.id.control_panel_container);

        // Inflate the control panel layout using the placeholder as the anchor view.
        mControlPanelView = mControlPanel.createLayout(getLayoutInflater(), mControlPanelContainer);

        // Add the inflated control panel root view to its container.
        mControlPanelContainer.addView(mControlPanelView);

        // Let control panel control our video texture (video, audio)
        mControlPanel.setControlledContent((OrionVideoTexture) mPanoramaTexture);

        // Let control panel control our camera (VR mode).
        mControlPanel.setControlledCamera(mCamera);

        // Listen for control panel events.
        mControlPanel.setPlayerControlsListener(new PlayerControlsListener() {
            @Override
            public void onLogoButtonClicked() {}

            @Override
            public void onCloseButtonClicked() {
                VideoControls.this.finish();
            }

            @Override
            public void onVRModeChanged(boolean isEnabled) {
                setVRMode(isEnabled);
                if (isEnabled) {
                    mControlPanel.hideTitlePanel();
                    mControlPanel.hideControlPanel();
                } else {
                    mControlPanel.showTitlePanel();
                    mControlPanel.showControlPanel();
                }
            }
        });

        // Listen for single, double and long clicks.
        TouchDisplayClickListener listener = new TouchDisplayClickListener();
        listener.bindClickable(null, new TouchDisplayClickListener.Listener() {

            @Override
            public void onDisplayClick(DisplayClickable clickable, Vec2F displayCoords) {
                runOnUiThread (new Thread(new Runnable() {
                    public void run() {

                        // Toggle panels visibility in normal mode; hint about long tap in VR mode.
                        if (!mControlPanel.isVRModeEnabled()) {
                            mControlPanel.toggleTitlePanel();
                            mControlPanel.toggleControlPanel();
                        } else {
                            String message = getString(R.string.player_long_tap_hint_exit_vr_mode);
                            Toast.makeText(VideoControls.this, message, Toast.LENGTH_SHORT).show();
                        }

                    }
                }));
            }

            @Override
            public void onDisplayDoubleClick(DisplayClickable clickable, Vec2F displayCoords) {
                runOnUiThread (new Thread(new Runnable() {
                    public void run() {

                        // Toggle between play and pause states in normal mode.
                        if (!mControlPanel.mIsVRModeEnabled) {
                            OrionVideoTexture t = (OrionVideoTexture) mPanoramaTexture;
                            if (t.getActualPlaybackState() == OrionTexture.PlaybackState.PLAYING) {
                                mControlPanel.pause();
                                mControlPanel.runPauseAnimation();
                            } else {
                                mControlPanel.play();
                                mControlPanel.runPlayAnimation();
                            }
                        }

                    }
                }));
            }

            @Override
            public void onDisplayLongClick(DisplayClickable clickable, Vec2F displayCoords) {

                runOnUiThread (new Thread(new Runnable() {
                    public void run() {

                        // Change VR mode (via control panel so that it stays in sync).
                        mControlPanel.toggleVRMode();

                    }
                }));
            }
        });

        // Bind click listener to the scene to make it functional.
        mScene.bindController(listener);
	}

    /** Interface for listening component events. */
    public interface PlayerControlsListener {

        /** Called when logo button has been clicked. */
        void onLogoButtonClicked();

        /** Called when close button has been clicked. */
        void onCloseButtonClicked();

        /** Called when VR mode button has been clicked. */
        void onVRModeChanged(boolean isEnabled);

    }

    /** Custom Orion360 control panel implementation. */
    public class MyControlPanel extends ControlPanel {

        /** Layout root view. */
        private ViewGroup mRootView;

        /** Title panel. */
        private View mTitlePanelView;

        /** Title panel animation in. */
        private Animation mTitlePanelAnimationIn;

        /** Title panel animation out. */
        private Animation mTitlePanelAnimationOut;

        /** Logo button. */
        private ImageView mLogoButton;

        /** Title text. */
        private TextView mTitle;

        /** Close button. */
        private ImageView mCloseButton;

        /** Control panel view. */
        private View mControlPanelView;

        /** Control panel animation in. */
        private Animation mControlPanelAnimationIn;

        /** Control panel animation out. */
        private Animation mControlPanelAnimationOut;

        /** Play button. */
        private ImageButton mPlayButton;

        /** Elapsed time text. */
        private TextView mElapsedTime;

        /** Seek bar. */
        private SeekBar mSeekBar;

        /** Duration time text. */
        private TextView mDurationTime;

        /** Remaining time text. */
        private TextView mRemainingTime;

        /** Audio mute/unmute button. */
        private ImageButton mAudioMuteButton;

        /** VR/normal mode button. */
        private ImageButton mVRModeButton;

        /** Play overlay. */
        private ImageView mPlayOverlay;

        /** Play animation. */
        private Animation mPlayAnimation;

        /** Pause overlay. */
        private ImageView mPauseOverlay;

        /** Pause animation. */
        private Animation mPauseAnimation;

        /** Normal mode (single) buffering indicator. */
        private ImageView mBufferingIndicatorNormal;

        /** VR mode (left) buffering indicator. */
        private ImageView mBufferingIndicatorVRLeft;

        /** VR mode (right) buffering indicator. */
        private ImageView mBufferingIndicatorVRRight;

        /** Buffering indicator custom animation. */
        private Animation mBufferingIndicatorAnimation;

        /** Flag for indicating if buffering indicators are currently visible, or not. */
        private boolean mBufferingIndicatorVisible = false;

        /** Flag for indicating if VR mode is active, or not. */
        private boolean mIsVRModeEnabled = false;

        /** Flag indicating if control panel is visible or not. */
        private boolean mControlPanelVisible = true;

        /** Flag indicating if title panel is visible or not. */
        private boolean mTitlePanelVisible = true;

        /** Listener for component events. */
        private PlayerControlsListener mListener;


        /**
         * Constructor with activity.
         *
         * @param activity The activity.
         */
        MyControlPanel(Activity activity) {
            super(activity);
        }

        @Override
        public View createLayout(LayoutInflater inflater, ViewGroup anchorView) {

            // Inflate the layout for this component.
            mRootView = (ViewGroup) inflater.inflate(R.layout.video_controls, anchorView, false);

            // Title panel.
            mTitlePanelView = mRootView.findViewById(R.id.player_title_panel);
            mTitlePanelAnimationIn = AnimationUtils.loadAnimation(
                    getActivity(), R.anim.player_title_panel_enter);
            mTitlePanelAnimationIn.setFillAfter(true);
            mTitlePanelAnimationIn.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    mTitlePanelVisible = true;
                    mLogoButton.setClickable(true);
                    mLogoButton.setEnabled(true);
                    mTitle.setClickable(true);
                    mTitle.setEnabled(true);
                    mCloseButton.setClickable(true);
                    mCloseButton.setEnabled(true);
                }

            });
            mTitlePanelAnimationOut = AnimationUtils.loadAnimation(
                    getActivity(), R.anim.player_title_panel_exit);
            mTitlePanelAnimationOut.setFillAfter(true);
            mTitlePanelAnimationOut.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    mTitlePanelVisible = false;
                    mLogoButton.setClickable(false);
                    mLogoButton.setEnabled(false);
                    mTitle.setClickable(false);
                    mTitle.setEnabled(false);
                    mCloseButton.setClickable(false);
                    mCloseButton.setEnabled(false);
                }

            });

            // Logo button.
            mLogoButton = (ImageView) mRootView.findViewById(R.id.player_title_panel_logo_button);
            mLogoButton.setVisibility(View.GONE);
            mLogoButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (null != mListener) {
                        mListener.onLogoButtonClicked();
                    }
                }

            });

            // Title text.
            mTitle = (TextView) mRootView.findViewById(R.id.player_title_panel_title);

            // Close button.
            mCloseButton = (ImageView) mRootView.findViewById(R.id.player_title_panel_close_button);
            mCloseButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (null != mListener) {
                        mListener.onCloseButtonClicked();
                    }
                }

            });

            // Control panel.
            mControlPanelView = mRootView.findViewById(R.id.player_controls_panel);
            mControlPanelAnimationIn = AnimationUtils.loadAnimation(
                    getActivity(), R.anim.player_control_panel_enter);
            mControlPanelAnimationIn.setFillAfter(true);
            mControlPanelAnimationIn.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    mControlPanelVisible = true;

                    // Enable all control panel components.
                    mPlayButton.setClickable(true);
                    mPlayButton.setEnabled(true);
                    mElapsedTime.setClickable(true);
                    mElapsedTime.setEnabled(true);
                    mSeekBar.setClickable(true);
                    mSeekBar.setEnabled(true);
                    mRemainingTime.setClickable(true);
                    mRemainingTime.setEnabled(true);
                    mDurationTime.setClickable(true);
                    mDurationTime.setEnabled(true);
                    mAudioMuteButton.setClickable(true);
                    mAudioMuteButton.setEnabled(true);
                    mVRModeButton.setClickable(true);
                    mVRModeButton.setEnabled(true);
                }

            });
            mControlPanelAnimationOut = AnimationUtils.loadAnimation(
                    getActivity(), R.anim.player_control_panel_exit);
            mControlPanelAnimationOut.setFillAfter(true);
            mControlPanelAnimationOut.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    mControlPanelVisible = false;

                    // Disable all control panel components.
                    mPlayButton.setClickable(false);
                    mPlayButton.setEnabled(false);
                    mElapsedTime.setClickable(false);
                    mElapsedTime.setEnabled(false);
                    mSeekBar.setClickable(false);
                    mSeekBar.setEnabled(false);
                    mRemainingTime.setClickable(false);
                    mRemainingTime.setEnabled(false);
                    mDurationTime.setClickable(false);
                    mDurationTime.setEnabled(false);
                    mAudioMuteButton.setClickable(false);
                    mAudioMuteButton.setEnabled(false);
                    mVRModeButton.setClickable(false);
                    mVRModeButton.setEnabled(false);
                }
            });

            // Play/pause button.
            mPlayButton = (ImageButton) mRootView.findViewById(R.id.player_controls_play_button);
            setPlayButton(mPlayButton,
                    R.mipmap.player_play_icon,
                    R.mipmap.player_pause_icon,
                    R.mipmap.player_play_icon);

            // Position (elapsed time) text.
            mElapsedTime = (TextView) mRootView.findViewById(R.id.player_controls_position_text);
            setPositionLabel(mElapsedTime);

            // Seek bar.
            mSeekBar = (SeekBar) mRootView.findViewById(R.id.player_controls_seekbar);
            setSeekBar(mSeekBar);

            // Duration (total time) text.
            mDurationTime = (TextView) mRootView.findViewById(R.id.player_controls_duration_text);
            setDurationLabel(mDurationTime);
            mDurationTime.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mDurationTime.setVisibility(View.GONE);
                    mRemainingTime.setVisibility(View.VISIBLE);
                }

            });

            // Remaining time text.
            mRemainingTime = (TextView) mRootView.findViewById(R.id.player_controls_remaining_text);
            setRemainingLabel(mRemainingTime);
            mRemainingTime.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mDurationTime.setVisibility(View.VISIBLE);
                    mRemainingTime.setVisibility(View.GONE);
                }

            });

            // Audio mute button.
            mAudioMuteButton = (ImageButton) mRootView.findViewById(
                    R.id.player_controls_audio_button);
            setAudioMuteButton(mAudioMuteButton,
                    R.mipmap.player_unmute_icon,
                    R.mipmap.player_mute_icon);

            // Configure VR mode on/off button.
            mVRModeButton = (ImageButton) mRootView.findViewById(R.id.player_controls_vr_button);
            mVRModeButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    toggleVRMode();
                }

            });

            // Play overlay.
            mPlayOverlay = (ImageView) mRootView.findViewById(R.id.play_overlay);
            mPlayAnimation = AnimationUtils.loadAnimation(
                    getActivity(), R.anim.fast_fadeinout_animation);
            mPlayAnimation.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    mPlayOverlay.setVisibility(View.GONE);
                }

            });

            // Pause overlay.
            mPauseOverlay = (ImageView) mRootView.findViewById(R.id.pause_overlay);
            mPauseAnimation = AnimationUtils.loadAnimation(
                    getActivity(), R.anim.fast_fadeinout_animation);
            mPauseAnimation.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    mPauseOverlay.setVisibility(View.GONE);
                }

            });

            // Buffering indicator.
            mBufferingIndicatorNormal = (ImageView) mRootView.findViewById(
                    R.id.player_hud_progressbar_normal_image);
            mBufferingIndicatorVRLeft = (ImageView) mRootView.findViewById(
                    R.id.player_hud_progressbar_vr_left_image);
            mBufferingIndicatorVRRight = (ImageView) mRootView.findViewById(
                    R.id.player_hud_progressbar_vr_right_image);
            mBufferingIndicatorAnimation = AnimationUtils.loadAnimation(
                    getActivity(), R.anim.rotate_around_center_point);

            return mRootView;
        }

        /**
         * Set listener for control panel events.
         *
         * @param listener the listener to be set.
         */
        void setPlayerControlsListener(PlayerControlsListener listener) {
            mListener = listener;
        }

        /**
         * Check if VR mode is currently enabled, or not.
         */
        boolean isVRModeEnabled() {
            return mIsVRModeEnabled;
        }

        /**
         * Toggle VR mode.
         */
        void toggleVRMode() {
            if (mIsVRModeEnabled) {
                disableVRMode();
            } else {
                enableVRMode();
            }
        }

        /**
         * Enable VR mode.
         */
        void enableVRMode() {
            mIsVRModeEnabled = true;

            // Handle the case where VR mode is toggled and progress bar
            // also needs to be swapped between normal and VR mode.
            if (mBufferingIndicatorVisible) {
                showBufferingIndicator();
            }

            if (null != mListener) {
                mListener.onVRModeChanged(mIsVRModeEnabled);
            }
        }

        /**
         * Disable VR mode.
         */
        void disableVRMode() {
            mIsVRModeEnabled = false;

            // Handle the case where VR mode is toggled and progress bar
            // also needs to be swapped between normal and VR mode.
            if (mBufferingIndicatorVisible) {
                showBufferingIndicator();
            }

            if (null != mListener) {
                mListener.onVRModeChanged(mIsVRModeEnabled);
            }
        }

        /**
         * Toggle title panel.
         */
        void toggleTitlePanel() {
            if (mTitlePanelVisible) {
                hideTitlePanel();
            } else {
                showTitlePanel();
            }
        }

        /**
         * Show title panel.
         */
        void showTitlePanel() {
            if(!mTitlePanelVisible) {
                mTitlePanelView.startAnimation(mTitlePanelAnimationIn);
            }
        }

        /**
         * Hide title panel.
         */
        void hideTitlePanel() {
            if (mTitlePanelVisible) {
                mTitlePanelView.startAnimation(mTitlePanelAnimationOut);
            }
        }

        /**
         * Toggle control panel.
         */
        void toggleControlPanel() {
            if (mControlPanelVisible) {
                hideControlPanel();
            } else {
                showControlPanel();
            }
        }

        /**
         * Show control panel.
         */
        void showControlPanel() {
            if(!mControlPanelVisible) {
                mControlPanelView.startAnimation(mControlPanelAnimationIn);
            }
        }

        /**
         * Hide control panel.
         */
        void hideControlPanel() {
            if(mControlPanelVisible) {
                mControlPanelView.startAnimation(mControlPanelAnimationOut);
            }
        }

        /**
         * Run Play animation.
         */
        void runPlayAnimation() {
            mPlayOverlay.setVisibility(View.VISIBLE);
            mPlayOverlay.startAnimation(mPlayAnimation);
        }

        /**
         * Run Pause animation.
         */
        void runPauseAnimation() {
            mPauseOverlay.setVisibility(View.VISIBLE);
            mPauseOverlay.startAnimation(mPauseAnimation);
        }

        /**
         * Show buffering indicator.
         */
        void showBufferingIndicator() {
            mBufferingIndicatorVisible = true;

            mBufferingIndicatorAnimation.cancel();
            mBufferingIndicatorNormal.clearAnimation();
            mBufferingIndicatorVRLeft.clearAnimation();
            mBufferingIndicatorVRRight.clearAnimation();

            if (mIsVRModeEnabled) {
                mBufferingIndicatorNormal.setVisibility(View.INVISIBLE);
                mBufferingIndicatorVRLeft.startAnimation(mBufferingIndicatorAnimation);
                mBufferingIndicatorVRRight.startAnimation(mBufferingIndicatorAnimation);
                mBufferingIndicatorVRLeft.setVisibility(View.VISIBLE);
                mBufferingIndicatorVRRight.setVisibility(View.VISIBLE);
            } else {
                mBufferingIndicatorVRLeft.setVisibility(View.INVISIBLE);
                mBufferingIndicatorVRRight.setVisibility(View.INVISIBLE);
                mBufferingIndicatorNormal.startAnimation(mBufferingIndicatorAnimation);
                mBufferingIndicatorNormal.setVisibility(ImageView.VISIBLE);
            }
        }

        /**
         * Hide buffering indicator.
         */
        void hideBufferingIndicator() {
            mBufferingIndicatorVisible = false;

            mBufferingIndicatorAnimation.cancel();
            mBufferingIndicatorNormal.clearAnimation();
            mBufferingIndicatorVRLeft.clearAnimation();
            mBufferingIndicatorVRRight.clearAnimation();

            mBufferingIndicatorVRRight.setVisibility(ImageView.INVISIBLE);
            mBufferingIndicatorVRLeft.setVisibility(ImageView.INVISIBLE);
            mBufferingIndicatorNormal.setVisibility(ImageView.INVISIBLE);
        }
    }

    /**
     * Convenience class for configuring typical touch control logic.
     */
    public class TouchControllerWidget implements OrionWidget {

        /** The camera that will be controlled by this widget. */
        private OrionCamera mCamera;

        /** Touch pinch-to-zoom/pinch-to-rotate gesture handler. */
        private TouchPincher mTouchPincher;

        /** Touch drag-to-pan gesture handler. */
        private TouchRotater mTouchRotater;

        /** Rotation aligner keeps the horizon straight at all times. */
        private RotationAligner mRotationAligner;


        /**
         * Constructs the widget.
         *
         * @param camera The camera to be controlled by this widget.
         */
        TouchControllerWidget(OrionCamera camera) {

            // Keep a reference to the camera that we control.
            mCamera = camera;

            // Create pinch-to-zoom/pinch-to-rotate handler.
            mTouchPincher = new TouchPincher();
            mTouchPincher.setMinimumDistanceDp(OrionContext.getActivity(), 20);
            mTouchPincher.bindControllable(mCamera, OrionCamera.VAR_FLOAT1_ZOOM);

            // Create drag-to-pan handler.
            mTouchRotater = new TouchRotater();
            mTouchRotater.bindControllable(mCamera);

            // Create the rotation aligner, responsible for rotating the view so that the horizon
            // aligns with the user's real-life horizon when the user is not looking up or down.
            mRotationAligner = new RotationAligner();
            mRotationAligner.setDeviceAlignZ(-ContextUtil.getDisplayRotationDegreesFromNatural(
                    OrionContext.getActivity()));
            mRotationAligner.bindControllable(mCamera);

            // Rotation aligner needs sensor fusion data in order to do its job.
            OrionContext.getSensorFusion().bindControllable(mRotationAligner);
        }

        @Override
        public void onBindWidget(OrionScene scene) {
            // When widget is bound to scene, bind the controllers to it to make them functional.
            scene.bindController(mTouchPincher);
            scene.bindController(mTouchRotater);
            scene.bindController(mRotationAligner);
        }

        @Override
        public void onReleaseWidget(OrionScene scene) {
            // When widget is released from scene, release the controllers as well.
            scene.releaseController(mTouchPincher);
            scene.releaseController(mTouchRotater);
            scene.releaseController(mRotationAligner);
        }
    }

    /**
     * Configure Orion360 as a typical mono panorama video player.
     */
    protected void initOrion() {

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene();

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindController(OrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanorama = new OrionPanorama();

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = OrionTexture.createTextureFromURI(this,
                MainMenu.TEST_VIDEO_URI_1280x640);

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mPanorama.bindTextureFull(0, mPanoramaTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanorama);

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera();

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        OrionContext.getSensorFusion().bindControllable(mCamera);

        // Create a new touch controller widget (convenience class), and let it control our camera.
        mTouchController = new TouchControllerWidget(mCamera);

        // Bind the touch controller widget to the scene. This will make it functional in the scene.
        mScene.bindWidget(mTouchController);

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

    /**
     * Set VR mode enabled or disabled.
     *
     * @param enabled Set true to enable VR mode, or false to return to normal mode.
     */
    protected void setVRMode(boolean enabled) {

        // We need to bind the texture to the panorama again, so release the current binding.
        mPanorama.releaseTexture(0);

        // We need to reconfigure viewports, so release the current ones.
        OrionViewport [] viewports = mView.getViewports();
        for (OrionViewport vp : viewports) {
            mView.releaseViewport(vp);
        }

        if (enabled) {

            // Bind the complete texture to both left and right eyes. Assume full spherical mono.
            mPanorama.bindTextureVR(0, mPanoramaTexture, new RectF(-180, 90, 180, -90),
                    OrionPanorama.TEXTURE_RECT_FULL, OrionPanorama.TEXTURE_RECT_FULL);

            // Set up two new viewports side by side (when looked from landscape orientation).
            mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_SPLIT_HORIZONTAL,
                    OrionViewport.CoordinateType.FIXED_LANDSCAPE);

            // Designate each viewport to render content for either left or right eye.
            mView.getViewports()[0].setVRMode(OrionViewport.VRMode.VR_LEFT);
            mView.getViewports()[1].setVRMode(OrionViewport.VRMode.VR_RIGHT);

            // Compensate for VR frame lens distortion using barrel distortion FX.
            BarrelDistortion barrelFx = new BarrelDistortion();
            barrelFx.setDistortionFillScale(1.0f);
            barrelFx.setDistortionCenterOffset(0, 0);
            barrelFx.setDistortionCoeffs(new float[] { 1.0f, 0.39f, -0.35f, 0.19f} );
            mView.getViewports()[0].bindFX(barrelFx);
            mView.getViewports()[1].bindFX(barrelFx);

            // Re-configure camera for VR mode.
            mCamera.setVRCameraDistance(0.035f);
            mCamera.setVRCameraFocalDistance(1.5f);
            mCamera.setZoom(1.0f);
            mCamera.setZoomMax(1.0f);

            // Hide the navigation bar, else it will be visible for the right eye!
            hideNavigationBar();

            // VR is still new for many users, hence they should be educated what this feature is
            // and how to use it, e.g. an animation about putting the device inside a VR frame.
            // Here we simply show a notification.
            Toast.makeText(this, "Please put the device inside a VR frame",
                    Toast.LENGTH_LONG).show();
        } else {

            // Bind the complete texture to the panorama sphere. Assume full spherical mono.
            mPanorama.bindTextureFull(0, mPanoramaTexture);

            // Bind one new viewport to landscape orientation.
            mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL,
                    OrionViewport.CoordinateType.FIXED_LANDSCAPE);

            // Re-configure camera.
            mCamera.setZoom(1.0f);
            mCamera.setZoomMax(3.0f);

            // Show the navigation bar again.
            showNavigationBar();
        }
    }

    /**
     * Show navigation bar.
     */
    public void showNavigationBar() {
        View v = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT < 19) {
            v.setSystemUiVisibility(View.VISIBLE);
        } else {
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            v.setSystemUiVisibility(uiOptions);
        }
    }

    /**
     * Hide navigation bar.
     */
    public void hideNavigationBar() {
        View v = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT < 19) {
            v.setSystemUiVisibility(View.GONE);
        } else {
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            v.setSystemUiVisibility(uiOptions);
        }
    }

    // From OrionVideoTexture.Listener:

    @Override
    public void onSourceURIChanged(OrionTexture orionTexture) {

        // Assume buffering is needed when a new video stream URI is set. Show indicator.
        mControlPanel.showBufferingIndicator();

    }

    @Override
    public void onInvalidURI(OrionTexture orionTexture) {

        // If the set video stream URI was invalid, we can't play it. Hide indicator.
        mControlPanel.hideBufferingIndicator();

    }

    @Override
    public void onVideoBufferingStart(OrionVideoTexture orionVideoTexture) {

        // Video player tells it has started buffering. Show indicator.
        mControlPanel.showBufferingIndicator();

    }

    @Override
    public void onVideoBufferingEnd(OrionVideoTexture orionVideoTexture) {

        // Video player tells it has stopped buffering. Hide indicator.
        mControlPanel.hideBufferingIndicator();

    }

    @Override
    public void onVideoBufferingUpdate(OrionVideoTexture orionVideoTexture,
                                       int fromPercent, int toPercent) {}

    @Override
    public void onVideoPrepared(OrionVideoTexture orionVideoTexture) {}

    @Override
    public void onVideoRenderingStart(OrionVideoTexture orionVideoTexture) {}

    @Override
    public void onVideoStarted(OrionVideoTexture orionVideoTexture) {}

    @Override
    public void onVideoPaused(OrionVideoTexture orionVideoTexture) {}

    @Override
    public void onVideoStopped(OrionVideoTexture orionVideoTexture) {}

    @Override
    public void onVideoCompleted(OrionVideoTexture orionVideoTexture) {}

    @Override
    public void onVideoSeekStarted(OrionVideoTexture orionVideoTexture, long l) {}

    @Override
    public void onVideoSeekCompleted(OrionVideoTexture orionVideoTexture, long l) {}

    @Override
    public void onVideoPositionChanged(OrionVideoTexture orionVideoTexture, long l) {}

    @Override
    public void onVideoDurationUpdate(OrionVideoTexture orionVideoTexture, long l) {}

    @Override
    public void onVideoSizeChanged(OrionVideoTexture orionVideoTexture, int i, int i1) {}

    @Override
    public void onVideoError(OrionVideoTexture orionVideoTexture, int i, int i1) {}

    @Override
    public void onVideoInfo(OrionVideoTexture orionVideoTexture, int i, String s) {}
}
