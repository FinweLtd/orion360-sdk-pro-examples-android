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

package fi.finwe.orion360.sdk.pro.examples.streaming;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.finwe.log.Logger;
import fi.finwe.orion360.sdk.pro.OrionActivity;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.TouchControllerWidget;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.texture.AndroidMediaPlayerWrapper;
import fi.finwe.orion360.sdk.pro.texture.ExoPlayerWrapper;
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.texture.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.texture.VideoPlayerWrapper;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;

/**
 * An example of using Orion360 SDK Pro to play protected content from AWS CloudFront/S3.
 * <p/>
 * Read SECURE_STREAMING.md for more information and detailed explanation.
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
public class SecuredStreaming extends OrionActivity {

    /** Key for CloudFront cookie 'expires' (canned policy). */
    public static final String CLOUDFRONT_EXPIRES_KEY = "CloudFront-Expires";

    /** Key for CloudFront cookie 'policy' (custom policy). */
    public static final String CLOUDFRONT_POLICY_KEY = "CloudFront-Policy";

    /** Key for CloudFront cookie 'signature'. */
    public static final String CLOUDFRONT_SIGNATURE_KEY = "CloudFront-Signature";

    /** Key for CloudFront cookie 'key pair id'. */
    public static final String CLOUDFRONT_KEY_PAIR_ID_KEY = "CloudFront-Key-Pair-Id";

    /** Key for 'Domain'. */
    public static final String DOMAIN_KEY = "Domain";

    /** Key for 'Path'. */
    public static final String PATH_KEY = "Path";

    /** Key for 'Secure'. */
    public static final String SECURE_KEY = "Secure";

    /** Key for 'HttpOnly'. */
    public static final String HTTP_ONLY_KEY = "HttpOnly";

    /** Key for 'Cookie'. */
    public static final String COOKIE_KEY = "Cookie";

    /** Our example CloudFront distribution. Replace with your own. */
    public final String DOMAIN_VALUE = "d15i6zsi2io35f.cloudfront.net";

    /** Path to content files in the distribution (with asterisk). */
    public final String PATH_VALUE = "/*";

    /** Path to content files in the distribution (without asterisk). */
    public final String PATH_VALUE_NO_ASTERISK = "/";

    /** Our access permission's expiration time. Replace with desired future timestamp. */
    public final String CLOUDFRONT_EXPIRES_VALUE = "2147483647";

    /**
     * Our hashed, signed and encoded cookie policy JSON (signature). This comes from project's
     * /cookies/cookie_policy.json file, which has been processed to generate a signature.
     * Works for a single .mp4 video file with a specific URL. Replace with your own signature.
     */
    @SuppressWarnings("SpellCheckingInspection")
    public final String CLOUDFRONT_FILE_SIGNATURE_VALUE = "uI3ott-V5IiFW-ZTgXg7AAN0iIC4Y2dnz0BLCLrPs7icTx3qghkz1HqZ9p0LnHShdEg8awMEsg5ev~ClXGBu52x80jIxI6tjBoH8ivZ3Ddt09TvNq95Q0ij2-1TsbHyxevJ3Iex29TCTMEG7Y36AWf9~IJzzJHKzp~SiflEAn-sPR0Z-9hdrQmkgalx5qSiu~Und7GM6qV2WMxwzrcGd7q8AV9N7IKnyJR-fqjOA7mEmOnQrT4iCCdkEcxmlgBxC3wRpmw53mbPP2OVr4c~b~dwB7XYr-gDbjtoSXCFwb6Ds~SdXx0hjmCbY1EynN8wGslfsYpHmiuyLFUnABOhzNQ__";

    /**
     * Our base64 encoded cookie policy JSON (directory). This comes from project's
     * /cookies/cookie_policy_directory.json file, which has been base64 encoded.
     * Works for all files in the directory. Replace with your own policy.
     */
    @SuppressWarnings("SpellCheckingInspection")
    public final String CLOUDFRONT_DIRECTORY_POLICY_VALUE = "eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9kMTVpNnpzaTJpbzM1Zi5jbG91ZGZyb250Lm5ldC8qIiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoyMTQ3NDgzNjQ3fX19XX0_";

    /**
     * Our hashed, signed and encoded cookie policy JSON (signature). This comes from project's
     * /cookies/cookie_policy_directory.json file, which has been processed to generate a signature.
     * Works for all files in the directory. Replace with your own signature.
     */
    @SuppressWarnings({"SpellCheckingInspection", "unused"})
    public final String CLOUDFRONT_DIRECTORY_SIGNATURE_VALUE = "Lc6cKuiY6wFqY7Gz7PEMdI7ZCeYgc9HazxgVP4xwtNzSWy0j3nwUeQtom15sG2JyX51v9h-BEAWBbH2buPIcaZp2FyOKOZBDd3~R-Wzk7lKHtCqakUCL8BSXtiDqCrCK9kR8gyqXDQLC6RId2QRKCt7hQTnR81pGWYZ8DOwXIKGop9PGoogPGmUBlj1pMN0OvDNtQBlK~W2vNfBl~bruZqNq798PbfJD-mQNB9Ohan67~3E-pMMk9MajeJ5Paxm04hk67xl0WorHyK7NCBn8wE~6KsTOvApbKlBInga8Q80hYwhYMOZmJU-6Z2GzviwqVdWIfmQuG8I~lpNLMBSbxg__";

    /** Our key pair ID for signed cookies. Replace with your own key pair ID. */
    public final String CLOUDFRONT_KEY_PAIR_ID_VALUE = "K37HI3P0TW0W7Q";

    /** Android MediaPlayer. */
    protected MediaPlayer mMediaPlayer;

    /** Google ExoPlayer. */
    protected SimpleExoPlayer mExoPlayer;

    /** The Android view where our 3D scene (OrionView) will be added to. */
    protected OrionViewContainer mViewContainer;

    /** The Orion360 SDK view where our 3D scene will be rendered to. */
    protected OrionView mView;

    /** The 3D scene where our panorama sphere will be added to. */
    protected OrionScene mScene;

    /** The panorama sphere where our video texture will be mapped to. */
    protected OrionPanorama mPanorama;

    /** The video player. */
    protected VideoPlayerWrapper mVideoPlayer;

    /** The video texture where our decoded video frames will be updated to. */
    protected OrionTexture mPanoramaTexture;

    /** The camera which will project our 3D scene to a 2D (view) surface. */
    protected OrionCamera mCamera;

    /** The widget that will handle our touch gestures. */
    protected TouchControllerWidget mTouchController;

    /** Different methods of passing signed cookies. */
    protected enum Method {
        HEADER_COOKIE,
        COOKIE_PARAMETER,
        COOKIE_MANAGER,
    }

    /** Different CloudFront policy options. */
    protected enum Policy {
        CANNED,
        CUSTOM
    }


    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // Find Orion360 view container from the XML layout. This is an Android view for content.
        mViewContainer = (OrionViewContainer)findViewById(R.id.orion_view_container);

        // Example 1: Play protected .mp4 video file directly with Android MediaPlayer.
        // You can choose between 3 types of setting signed cookies, and canned or custom policy.
//        playVideoWithMediaPlayer(
//                MainMenu.TEST_VIDEO_URI_SECURED_MP4_CLOUD_FRONT,
//                //Method.HEADER_COOKIE,
//                //Method.COOKIE_PARAMETER,
//                Method.COOKIE_MANAGER,
//                Policy.CANNED
//                //Policy.CUSTOM
//        );

        // Example 2: Play the protected .mp4 file using Orion360 SDK Pro with MediaPlayer engine.
//        playVideoWithOrion360_MediaPlayer(
//                MainMenu.TEST_VIDEO_URI_SECURED_MP4_CLOUD_FRONT,
//                Policy.CANNED
//                //Policy.CUSTOM
//        );

        // Example 3: Play the protected .m3u8 HLS stream directly with Google Exoplayer.
        playVideoWithExoPlayer(
                MainMenu.TEST_VIDEO_URI_SECURED_MP4_CLOUD_FRONT,
                //MainMenu.TEST_VIDEO_URI_SECURED_HLS_CLOUD_FRONT,
                Method.HEADER_COOKIE,
                //Method.COOKIE_MANAGER,
                Policy.CUSTOM
        );

        // Example 4: Play the protected file using Orion360 SDK Pro with ExoPlayer engine.
//        playVideoWithOrion360_ExoPlayer(videoUrl);
	}

    @Override
    public void onPause() {
        if (null != mMediaPlayer) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (null != mExoPlayer) {
            mExoPlayer.stop();
            mExoPlayer.release();
            mExoPlayer = null;
        }

        super.onPause();
    }

    /**
     * An example of playing a protected video file with Android MediaPlayer.
     *
     * This simple example does not use Orion360 and cannot render 360 video properly, but it
     * shows how we can pass signed cookies to CloudFront to access protected content.
     * Multiple different approaches are demonstrated.
     *
     * @param videoUrl the URL of the video to play.
     * @param method the method for setting signed cookies.
     * @param policy the policy for access control (canned/custom).
     */
    @SuppressWarnings({"unused", "SameParameterValue"})
    private void playVideoWithMediaPlayer(@NonNull String videoUrl, Method method, Policy policy) {

        // We will use Android MediaPlayer as the video engine.
        if (null == mMediaPlayer) {
            mMediaPlayer = new MediaPlayer();
        }

        // OrionViewContainer extends Android SurfaceView. Hence, we can use this already
        // existing view in our XML layout as our SurfaceView in this example. The video
        // frames decoded by Android MediaPlayer will appear in the SurfaceView as-is.
        // We just need to get its SurfaceHolder and pass it to the MediaPlayer instance.
        SurfaceHolder holder = mViewContainer.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                mMediaPlayer.setDisplay(holder);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder h, int i, int i1, int i2) {}

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {}
        });

        // Pass CloudFront signed cookies using selected method.
        Map<String, String> headers = new HashMap<>();
        switch (method) {
            case HEADER_COOKIE:
                // Since: API level 14 (Android Ice Cream Sandwich)
                // This approach uses HTTP request headers to pass signed cookies. We need
                // a handle to MediaPlayer instance or option to pass custom headers.
                String cookieValue = createCloudFrontHeaderCookie(policy);
                headers.put(COOKIE_KEY, cookieValue);
                Logger.logD(TAG, COOKIE_KEY + "=" + cookieValue);

                try {
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaPlayer.setDataSource(this, Uri.parse(videoUrl), headers);
                    mMediaPlayer.setOnPreparedListener(MediaPlayer::start);
                    mMediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case COOKIE_PARAMETER:
                if (Build.VERSION.SDK_INT > 26) {
                    // Since: API level 26 (Android Oreo)
                    // This approach uses a list of HttpCookies passed as a parameter.
                    // You need to have a handle to MediaPlayer instance or option to pass cookies.
                    List<HttpCookie> cookies = createCloudFrontHttpCookies(policy);
                    try {
                        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build();
                        mMediaPlayer.setAudioAttributes(audioAttributes);
                        mMediaPlayer.setDataSource(this,
                                Uri.parse(videoUrl), headers, cookies);
                        mMediaPlayer.setOnPreparedListener(MediaPlayer::start);
                        mMediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Logger.logE(TAG, "Cannot use this method before API level 26!");
                }
                break;
            case COOKIE_MANAGER:
                // Recommended way: use custom CookieManager/CookieHandler.
                // This approach uses a list of HttpCookie instances that are set manually.
                // You don't need to set *anything* cookie related to the MediaPlayer,
                // hence you don't necessarily need a handle to it!
                List<HttpCookie> cookies = createCloudFrontHttpCookies(policy);
                CookieManager cookieManager = setCookiesViaCookieManager(
                        "https://" + DOMAIN_VALUE, cookies);

                // Print set cookies (for debugging).
                Logger.logD(TAG, "Manually set cookies:");
                List<HttpCookie> setCookies = cookieManager.getCookieStore().getCookies();
                for (HttpCookie httpCookie : setCookies) {
                    Logger.logD(TAG, httpCookie.toString());
                }

                // Cookies are set. We can now use MediaPlayer, it will utilize set cookies.
                try {
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaPlayer.setDataSource(this, Uri.parse(videoUrl));
                    mMediaPlayer.setOnPreparedListener(MediaPlayer::start);
                    mMediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                Logger.logD(TAG, "You must select a method for passing signed cookies!");
        }
    }

    /**
     * An example of playing a protected video file with Orion360 SDK Pro + MediaPlayer engine.
     *
     * @param videoUrl the URL of the video to play.
     * @param policy the policy for access control (canned/custom).
     */
    @SuppressWarnings("unused")
    private void playVideoWithOrion360_MediaPlayer(@NonNull String videoUrl, Policy policy) {

        // This approach uses CookieManager. No need for access to MediaPlayer.
        List<HttpCookie> cookies = createCloudFrontHttpCookies(policy);
        setCookiesViaCookieManager("https://" + DOMAIN_VALUE, cookies);

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene(mOrionContext);

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindRoutine(mOrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanorama = new OrionPanorama(mOrionContext);

        // Create a new video player that uses Android MediaPlayer as an audio/video engine.
        mVideoPlayer = new AndroidMediaPlayerWrapper(this);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = new OrionVideoTexture(mOrionContext,
                mVideoPlayer, videoUrl);

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
     * An example of playing a protected video file with Google ExoPlayer.
     *
     * This simple example does not use Orion360 and cannot render 360 video properly, but it
     * shows how we can pass signed cookies to CloudFront to access protected content.
     * Multiple different approaches are demonstrated.
     *
     * @param videoUrl the URL of the video to play.
     * @param method the method to set signed cookies.
     * @param policy the policy for access control (canned/custom).
     */
    @SuppressWarnings({"unused", "SameParameterValue"})
    private void playVideoWithExoPlayer(@NonNull String videoUrl, Method method, Policy policy) {

        // Replace Orion player view with ExoPlayer player view in the layout.
        SimpleExoPlayerView exoPlayerView = new SimpleExoPlayerView(this);
        replaceView(mViewContainer, exoPlayerView);

        // Create ExoPlayer instance and play the content with it.
        try {
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelector trackSelector = new DefaultTrackSelector(
                    new AdaptiveTrackSelection.Factory(bandwidthMeter));
            mExoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

            Uri videoUri = Uri.parse(videoUrl);
            DefaultHttpDataSourceFactory dataSourceFactory =
                    new DefaultHttpDataSourceFactory("exoplayer_example");
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

            // Pass CloudFront signed cookies using selected method.
            switch (method) {
                case HEADER_COOKIE:
                    // This approach uses HTTP request headers to pass signed cookies.
                    // We need access to DefaultHttpDataSourceFactory or a possibility
                    // to pass custom headers to the request.
                    HttpDataSource.RequestProperties requestProperties =
                            dataSourceFactory.getDefaultRequestProperties();
                    String cookieValue = createCloudFrontHeaderCookie(Policy.CUSTOM);
                    requestProperties.set(COOKIE_KEY, cookieValue);
                    break;
                case COOKIE_MANAGER:
                    // This approach uses CookieManager. No need for access to ExoPlayer.
                    List<HttpCookie> cookies = createCloudFrontHttpCookies(Policy.CUSTOM);
                    setCookiesViaCookieManager("https://" + DOMAIN_VALUE, cookies);
                    break;
                default:
                    Logger.logD(TAG, "You must select a method for passing signed cookies!");
            }

            MediaSource mediaSource = new ExtractorMediaSource(videoUri,
                    dataSourceFactory, extractorsFactory, null, null);
            exoPlayerView.setPlayer(mExoPlayer);
            mExoPlayer.prepare(mediaSource);
            mExoPlayer.setPlayWhenReady(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * An example of playing a protected video file with Orion360 SDK Pro + ExoPlayer engine.
     *
     * @param videoUrl the URL of the video to play.
     */
    @SuppressWarnings("unused")
    private void playVideoWithOrion360_ExoPlayer(@NonNull String videoUrl) {

        // This approach uses CookieManager. No need for access to ExoPlayer.
        List<HttpCookie> cookies = createCloudFrontHttpCookies(Policy.CANNED);
        setCookiesViaCookieManager("https://" + DOMAIN_VALUE, cookies);

        // Create a new scene. This represents a 3D world where various objects can be placed.
        mScene = new OrionScene(mOrionContext);

        // Bind sensor fusion as a controller. This will make it available for scene objects.
        mScene.bindRoutine(mOrionContext.getSensorFusion());

        // Create a new panorama. This is a 3D object that will represent a spherical video/image.
        mPanorama = new OrionPanorama(mOrionContext);

        // Create a new video player that uses Google ExoPlayer as an audio/video engine.
        mVideoPlayer = new ExoPlayerWrapper(this);

        // Create a new video (or image) texture from a video (or image) source URI.
        mPanoramaTexture = new OrionVideoTexture(mOrionContext,
                mVideoPlayer, videoUrl);

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
     * Create a combined cookie value as a string to be set into an HTTP request's header.
     *
     * @param policy the policy for access control (canned/custom).
     * @return header cookie string value.
     */
    private String createCloudFrontHeaderCookie(@NonNull Policy policy) {
        Logger.logD(TAG, "Using HEADER cookie");
        String cookie;
        switch (policy) {
            case CUSTOM:
                cookie = CLOUDFRONT_POLICY_KEY + "=" + CLOUDFRONT_DIRECTORY_POLICY_VALUE + "; "
                        + CLOUDFRONT_SIGNATURE_KEY + "=" + CLOUDFRONT_DIRECTORY_SIGNATURE_VALUE
                        + "; ";
                        Logger.logD(TAG, "Using CUSTOM policy, full directory");
                break;
            case CANNED:
            default:
                cookie = CLOUDFRONT_EXPIRES_KEY + "=" + CLOUDFRONT_EXPIRES_VALUE + "; "
                        + CLOUDFRONT_SIGNATURE_KEY + "=" + CLOUDFRONT_FILE_SIGNATURE_VALUE + "; ";
                Logger.logD(TAG, "Using CANNED policy, single mp4 file");
                break;
        }
        cookie += CLOUDFRONT_KEY_PAIR_ID_KEY + "=" + CLOUDFRONT_KEY_PAIR_ID_VALUE + "; "
                + DOMAIN_KEY + "=" + DOMAIN_VALUE + "; "
                + PATH_KEY + "=" + PATH_VALUE + "; "
                + SECURE_KEY + "; "
                + HTTP_ONLY_KEY + "\r\n";
        return cookie;
    }

    /**
     * Create a separate HttpCookie instance for each of the three required CloudFront cookies.
     *
     * @param policy the policy for access control (canned/custom).
     * @return created HttpCookies.
     */
    private List<HttpCookie> createCloudFrontHttpCookies(@NonNull Policy policy) {
        Logger.logD(TAG, "Using HTTP cookie");
        List<HttpCookie> cookies = new ArrayList<>();
        switch (policy) {
            case CUSTOM:
                cookies.add(createHttpCookie(CLOUDFRONT_POLICY_KEY,
                        CLOUDFRONT_DIRECTORY_POLICY_VALUE));
                cookies.add(createHttpCookie(CLOUDFRONT_SIGNATURE_KEY,
                        CLOUDFRONT_DIRECTORY_SIGNATURE_VALUE));
                Logger.logD(TAG, "Using CUSTOM policy, full directory");
                break;
            case CANNED:
            default:
                cookies.add(createHttpCookie(CLOUDFRONT_EXPIRES_KEY,
                        CLOUDFRONT_EXPIRES_VALUE));
                cookies.add(createHttpCookie(CLOUDFRONT_SIGNATURE_KEY,
                        CLOUDFRONT_FILE_SIGNATURE_VALUE));
                Logger.logD(TAG, "Using CANNED policy, single mp4 file");
                break;
        }
        cookies.add(createHttpCookie(CLOUDFRONT_KEY_PAIR_ID_KEY, CLOUDFRONT_KEY_PAIR_ID_VALUE));
        return cookies;
    }

    /**
     * Create a CloudFront compatible HttpCookie with given key and value strings.
     *
     * Note: CloudFront is very strict about cookie format, see the comments in the code.
     *
     * @param key the key for the cookie.
     * @param value the value for the cookie.
     * @return created HttpCookie instance with given key and value.
     */
    private HttpCookie createHttpCookie(@NonNull String key, @NonNull String value) {
        //HttpCookie cookie = new HttpCookie(key, value);
        // above: this results to key="value" -> does not work with CloudFront!
        HttpCookie cookie = HttpCookie.parse(key + "=" + value).get(0);
        // above: this results to key=value -> OK
        cookie.setDomain(DOMAIN_VALUE);
        cookie.setPath(PATH_VALUE_NO_ASTERISK); // '/*' -> does not work with CloudFront, '/' -> OK
        cookie.setSecure(true);
        if (Build.VERSION.SDK_INT > 24) {
            cookie.setHttpOnly(true);
        }
        return cookie;
    }

    /**
     * Set cookies via CookieManager. This works for the following HTTP requests.
     *
     * @param url the URL for which to set cookies.
     * @param cookies the cookies to be set.
     * @return CookieManager, where cookies were set.
     */
    @SuppressWarnings("SameParameterValue")
    private CookieManager setCookiesViaCookieManager(@NonNull String url,
                                                     @NonNull List<HttpCookie> cookies) {
        Logger.logD(TAG, "Using COOKIE MANAGER");
        CookieManager cookieManager = new CookieManager();
        try {
            URI uri = new URI(url);
            for (HttpCookie cookie : cookies) {
                cookieManager.getCookieStore().add(uri, cookie);
            }
            CookieHandler.setDefault(cookieManager);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return cookieManager;
    }

    /**
     * Replace a view in a view hierarchy with another view. Copy layout params from the old view.
     *
     * @param oldView the view to be removed.
     * @param newView the view to be added.
     */
    private void replaceView(@NonNull View oldView, @NonNull View newView) {
        ViewGroup parent = (ViewGroup) oldView.getParent();
        if (parent != null) {
            final int position = parent.indexOfChild(oldView);
            ViewGroup.LayoutParams layoutParams = oldView.getLayoutParams();
            parent.removeView(oldView);
            newView.setLayoutParams(layoutParams);
            parent.addView(newView, position);
        } else {
            Logger.logE(TAG, "Failed to replace view: parent is NULL!");
        }
    }
}
