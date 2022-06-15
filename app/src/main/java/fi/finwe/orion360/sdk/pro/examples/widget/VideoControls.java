/*
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

import fi.finwe.log.Logger;
import fi.finwe.math.Vec2f;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionContext;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;
import fi.finwe.orion360.sdk.pro.viewport.OrionViewport;
import fi.finwe.orion360.sdk.pro.controllable.DisplayClickable;
import fi.finwe.orion360.sdk.pro.controller.TouchDisplayClickListener;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.TouchControllerWidget;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.texture.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.widget.ControlPanel;


/**
 * An example of creating custom video player controls.
 * <p/>
 * Note: To see how to use Android MediaController as controls, see streaming/PlayerState example!
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

    /** The Android view where our 3D scene (OrionView) will be added to. */
    protected OrionViewContainer mViewContainer;

    /** The Orion360 SDK view where our 3D scene will be rendered to. */
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
        mControlPanel = new MyControlPanel(mOrionContext, this);

        // Get the placeholder for our control panel from the XML layout.
        mControlPanelContainer = (ViewGroup) findViewById(R.id.control_panel_container);

        // Inflate the control panel layout using the placeholder as the anchor view.
        mControlPanelView = mControlPanel.createLayout(getLayoutInflater(), mControlPanelContainer);

        // Add the inflated control panel root view to its container.
        mControlPanelContainer.addView(mControlPanelView);

        // Let control panel control our video texture (video, audio)
        mControlPanel.setControlledContent((OrionVideoTexture) mPanoramaTexture);

        // Let control panel control our camera (toggle VR mode).
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
        TouchDisplayClickListener listener = new TouchDisplayClickListener(mOrionContext);
        listener.bindClickable(null, new TouchDisplayClickListener.Listener() {

            @Override
            public void onDisplayClick(DisplayClickable clickable, Vec2f displayCoords) {
                runOnUiThread (new Thread(() -> {

                    // Toggle panels visibility in normal mode; hint about long tap in VR mode.
                    if (!mControlPanel.isVRModeEnabled()) {
                        mControlPanel.toggleTitlePanel();
                        mControlPanel.toggleControlPanel();
                    } else {
                        String message = getString(R.string.player_long_tap_hint_exit_vr_mode);
                        Toast.makeText(VideoControls.this, message,
                                Toast.LENGTH_SHORT).show();
                    }

                }));
            }

            @Override
            public void onDisplayDoubleClick(DisplayClickable clickable, Vec2f displayCoords) {
                runOnUiThread (new Thread(() -> {

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

                }));
            }

            @Override
            public void onDisplayLongClick(DisplayClickable clickable, Vec2f displayCoords) {

                runOnUiThread (new Thread(() -> {

                    // Change VR mode (via control panel so that it stays in sync).
                    mControlPanel.toggleVRMode();

                }));
            }
        });

        // Bind click listener to the scene to make it functional.
        mScene.bindRoutine(listener);
	}

    /** Interface for listening component events. */
    interface PlayerControlsListener {

        /** Called when logo button has been clicked. */
        void onLogoButtonClicked();

        /** Called when close button has been clicked. */
        void onCloseButtonClicked();

        /** Called when VR mode button has been clicked. */
        void onVRModeChanged(boolean isEnabled);

    }

    /** Custom Orion360 control panel implementation. */
    private static class MyControlPanel extends ControlPanel {

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
         * @param context The Orion context.
         * @param activity The activity.
         */
        MyControlPanel(OrionContext context, Activity activity) {
            super(context, activity);
        }

        @Override
        public View createLayout(LayoutInflater inflater, ViewGroup anchorView) {

            // Inflate the layout for this component.
            ViewGroup mRootView = (ViewGroup) inflater.inflate(
                    R.layout.video_controls, anchorView, false);

            // Title panel.
            mTitlePanelView = mRootView.findViewById(R.id.player_title_panel);
            mTitlePanelAnimationIn = AnimationUtils.loadAnimation(
                    mOrionContext.getActivity(), R.anim.player_title_panel_enter);
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
                    mOrionContext.getActivity(), R.anim.player_title_panel_exit);
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
            mLogoButton.setVisibility(View.VISIBLE);
            mLogoButton.setOnClickListener(v -> {
                if (null != mListener) {
                    mListener.onLogoButtonClicked();
                }
            });

            // Title text.
            mTitle = (TextView) mRootView.findViewById(R.id.player_title_panel_title);

            // Close button.
            mCloseButton = (ImageView) mRootView.findViewById(R.id.player_title_panel_close_button);
            mCloseButton.setOnClickListener(v -> {
                if (null != mListener) {
                    mListener.onCloseButtonClicked();
                }
            });

            // Control panel.
            mControlPanelView = mRootView.findViewById(R.id.player_controls_panel);
            mControlPanelAnimationIn = AnimationUtils.loadAnimation(
                    mOrionContext.getActivity(), R.anim.player_control_panel_enter);
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
                    mOrionContext.getActivity(), R.anim.player_control_panel_exit);
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
            mDurationTime.setOnClickListener(v -> {
                mDurationTime.setVisibility(View.GONE);
                mRemainingTime.setVisibility(View.VISIBLE);
            });

            // Remaining time text.
            mRemainingTime = (TextView) mRootView.findViewById(R.id.player_controls_remaining_text);
            setRemainingLabel(mRemainingTime);
            mRemainingTime.setOnClickListener(v -> {
                mDurationTime.setVisibility(View.VISIBLE);
                mRemainingTime.setVisibility(View.GONE);
            });

            // Audio mute button.
            mAudioMuteButton = (ImageButton) mRootView.findViewById(
                    R.id.player_controls_audio_button);
            setAudioMuteButton(mAudioMuteButton,
                    R.mipmap.player_unmute_icon,
                    R.mipmap.player_mute_icon);

            // Configure VR mode on/off button.
            mVRModeButton = (ImageButton) mRootView.findViewById(R.id.player_controls_vr_button);
            mVRModeButton.setOnClickListener(v -> toggleVRMode());

            // Play overlay.
            mPlayOverlay = (ImageView) mRootView.findViewById(R.id.play_overlay);
            mPlayAnimation = AnimationUtils.loadAnimation(
                    mOrionContext.getActivity(), R.anim.fast_fadeinout_animation);
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
                    mOrionContext.getActivity(), R.anim.fast_fadeinout_animation);
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
                    mOrionContext.getActivity(), R.anim.rotate_around_center_point);

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
     * Configure Orion360 as a typical mono panorama video player.
     */
    protected void initOrion() {

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene(mOrionContext);

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindRoutine(mOrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanorama = new OrionPanorama(mOrionContext);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = OrionTexture.createTextureFromURI(mOrionContext, this,
                MainMenu.TEST_VIDEO_URI_1280x640);

        // Bind the panorama texture to the panorama object. Here we assume full spherical
        // equirectangular monoscopic source, and wrap the complete texture around the sphere.
        // If you have stereoscopic content or doughnut shape video, use other method variants.
        mPanorama.bindTextureFull(0, mPanoramaTexture);

        // Bind the panorama to the scene. This will make it part of our 3D world.
        mScene.bindSceneItem(mPanorama);

        // Create a new camera. This will become the end-user's eyes into the 3D world.
        mCamera = new OrionCamera(mOrionContext);

        // Reset view to the 'front' direction (horizontal center of the panorama).
        mCamera.setDefaultRotationYaw(0);

        // Bind camera as a controllable to sensor fusion. This will let sensors rotate the camera.
        mOrionContext.getSensorFusion().bindControllable(mCamera);

        // Create a new touch controller widget (convenience class), and let it control our camera.
        mTouchController = new TouchControllerWidget(mOrionContext, mCamera);

        // Bind the touch controller widget to the scene. This will make it functional in the scene.
        mScene.bindWidget(mTouchController);

        // Find Orion360 view container from the XML layout. This is an Android view for content.
        mViewContainer = (OrionViewContainer)findViewById(R.id.orion_view_container);

        // Create a new OrionView and bind it into the container.
        mView = new OrionView(mOrionContext);
        mViewContainer.bindView(mView);

        // Bind the scene to the view. This is the 3D world that we will be rendering to this view.
        mView.bindDefaultScene(mScene);

        // Bind the camera to the view. We will look into the 3D world through this camera.
        mView.bindDefaultCamera(mCamera);

        // The view can be divided into one or more viewports. For example, in VR mode we have one
        // viewport per eye. Here we fill the complete view with one (landscape) viewport.
        mView.bindViewports(OrionDisplayViewport.VIEWPORT_CONFIG_FULL,
                OrionDisplayViewport.CoordinateType.FIXED_LANDSCAPE);
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
            mPanorama.bindTextureVR(0, mPanoramaTexture,
                    new RectF(-180, 90, 180, -90),
                    OrionPanorama.TEXTURE_RECT_FULL, OrionPanorama.TEXTURE_RECT_FULL);

            // Set up two new viewports side by side (when looked from landscape orientation).
            mView.bindViewports(OrionDisplayViewport.VIEWPORT_CONFIG_SPLIT_HORIZONTAL,
                    OrionDisplayViewport.CoordinateType.FIXED_LANDSCAPE);

            // Designate each viewport to render content for either left or right eye.
            mView.getViewports()[0].setVRMode(OrionViewport.VRMode.VR_LEFT);
            mView.getViewports()[1].setVRMode(OrionViewport.VRMode.VR_RIGHT);

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
            mView.bindViewports(OrionDisplayViewport.VIEWPORT_CONFIG_FULL,
                    OrionDisplayViewport.CoordinateType.FIXED_LANDSCAPE);

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
        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        v.setSystemUiVisibility(uiOptions);
    }

    /**
     * Hide navigation bar.
     */
    public void hideNavigationBar() {
        View v = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        v.setSystemUiVisibility(uiOptions);
    }

    // From OrionVideoTexture.Listener:

    @Override
    public void onException(OrionTexture orionTexture, Exception e) {
        Logger.logD(TAG, "onException() " + e.toString());
    }

    @Override
    public void onVideoPlayerCreated(OrionVideoTexture orionVideoTexture) {
        Logger.logD(TAG, "onVideoPlayerCreated()");
    }

    @Override
    public void onVideoSourceURISet(OrionVideoTexture orionVideoTexture) {
        Logger.logD(TAG, "onVideoSourceURISet()");

        // Assume buffering is needed when a new video stream URI is set. Show indicator.
        mControlPanel.showBufferingIndicator();

    }

    @Override
    public void onVideoBufferingStart(OrionVideoTexture orionVideoTexture) {
        Logger.logD(TAG, "onVideoBufferingStart()");

        // Video player tells it has started buffering. Show indicator.
        // Notice that this can happen also during playback, not only in the beginning.
        mControlPanel.showBufferingIndicator();

    }

    @Override
    public void onVideoBufferingEnd(OrionVideoTexture orionVideoTexture) {
        Logger.logD(TAG, "onVideoBufferingEnd()");

        // Video player tells it has stopped buffering. Hide indicator.
        // Notice that this can happen also during playback, not only in the beginning.
        mControlPanel.hideBufferingIndicator();

    }

    @Override
    public void onVideoBufferingUpdate(OrionVideoTexture orionVideoTexture,
                                       int fromPercent, int toPercent) {
        Logger.logD(TAG, "onVideoBufferingUpdate() " + fromPercent + " -> " + toPercent);
    }

    @Override
    public void onVideoPrepared(OrionVideoTexture orionVideoTexture) {
        Logger.logD(TAG, "onVideoPrepared()");
    }

    @Override
    public void onVideoRenderingStart(OrionVideoTexture orionVideoTexture) {
        Logger.logD(TAG, "onVideoRenderingStart()");

        // Video player tells it has buffered enough and decoded first frame.
        // Playback starts now, so hide buffering indicator. If buffering occurs
        // again during playback, will show/hide indicator by responding to
        // buffering callbacks.
        mControlPanel.hideBufferingIndicator();
    }

    @Override
    public void onVideoStarted(OrionVideoTexture orionVideoTexture) {
        Logger.logD(TAG, "onVideoStarted()");
    }

    @Override
    public void onVideoPaused(OrionVideoTexture orionVideoTexture) {
        Logger.logD(TAG, "onVideoPaused()");
    }

    @Override
    public void onVideoStopped(OrionVideoTexture orionVideoTexture) {
        Logger.logD(TAG, "onVideoStopped()");
    }

    @Override
    public void onVideoCompleted(OrionVideoTexture orionVideoTexture) {
        Logger.logD(TAG, "onVideoCompleted()");
    }

    @Override
    public void onVideoReleased(OrionVideoTexture texture) {
        Logger.logD(TAG, "onVideoReleased()");
    }

    @Override
    public void onVideoSeekStarted(OrionVideoTexture orionVideoTexture, long ms) {
        Logger.logD(TAG, "onVideoSeekStarted() " + ms);
    }

    @Override
    public void onVideoSeekCompleted(OrionVideoTexture orionVideoTexture, long ms) {
        Logger.logD(TAG, "onVideoSeekCompleted() " + ms);
    }

    @Override
    public void onVideoPositionChanged(OrionVideoTexture orionVideoTexture, long ms) {
        Logger.logD(TAG, "onVideoPositionChanged() " + ms);
    }

    @Override
    public void onVideoDurationUpdate(OrionVideoTexture orionVideoTexture, long ms) {
        Logger.logD(TAG, "onVideoDurationUpdate() " + ms);
    }

    @Override
    public void onVideoSizeChanged(OrionVideoTexture orionVideoTexture, int w, int h) {
        Logger.logD(TAG, "onVideoSizeChanged() " + w + "x" + h);
    }

    @Override
    public void onVideoError(OrionVideoTexture orionVideoTexture, int e1, int e2) {
        Logger.logD(TAG, "onVideoError() " + e1 + ", " + e2);
    }

    @Override
    public void onVideoInfo(OrionVideoTexture orionVideoTexture, int i, String s) {
        Logger.logD(TAG, "onVideoInfo() " + i + ", " + s);
    }
}
