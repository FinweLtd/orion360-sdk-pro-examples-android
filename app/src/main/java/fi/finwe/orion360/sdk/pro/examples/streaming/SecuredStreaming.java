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

import androidx.annotation.NonNull;

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
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.texture.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.texture.VideoPlayerWrapper;
import fi.finwe.orion360.sdk.pro.view.OrionView;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;

/**
 * An example of using Orion360 SDK Pro to play protected content from AWS CloudFront/S3.
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

    /** Key for CloudFront cookie 'expires'. */
    public static final String CLOUDFRONT_EXPIRES_KEY = "CloudFront-Expires";

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

    /** Our example CloudFront distribution. */
    public final String DOMAIN_VALUE = "d15i6zsi2io35f.cloudfront.net";

    /** Path to content file in the distribution (with asterisk). */
    public final String PATH_VALUE = "/*";

    /** Path to content file in the distribution (without asterisk). */
    public final String PATH_VALUE_NO_ASTERISK = "/";

    /** Our access permission's expiration time. */
    public final String CLOUDFRONT_EXPIRES_VALUE = "2147483647";

    /** Our hashed, signed and encoded cookie policy JSON (signature). */
    @SuppressWarnings("SpellCheckingInspection")
    public final String CLOUDFRONT_SIGNATURE_VALUE = "uI3ott-V5IiFW-ZTgXg7AAN0iIC4Y2dnz0BLCLrPs7icTx3qghkz1HqZ9p0LnHShdEg8awMEsg5ev~ClXGBu52x80jIxI6tjBoH8ivZ3Ddt09TvNq95Q0ij2-1TsbHyxevJ3Iex29TCTMEG7Y36AWf9~IJzzJHKzp~SiflEAn-sPR0Z-9hdrQmkgalx5qSiu~Und7GM6qV2WMxwzrcGd7q8AV9N7IKnyJR-fqjOA7mEmOnQrT4iCCdkEcxmlgBxC3wRpmw53mbPP2OVr4c~b~dwB7XYr-gDbjtoSXCFwb6Ds~SdXx0hjmCbY1EynN8wGslfsYpHmiuyLFUnABOhzNQ__";

    /** Our key pair ID for signed cookies. */
    public final String CLOUDFRONT_KEY_PAIR_ID_VALUE = "K37HI3P0TW0W7Q";

    /** Android MediaPlayer. */
    protected MediaPlayer mMediaPlayer;

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


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        // Find Orion360 view container from the XML layout. This is an Android view for content.
        mViewContainer = (OrionViewContainer)findViewById(R.id.orion_view_container);

        // Define URL to the protected video file (CloudFront URL, not S3 URL).
        String videoUrl = MainMenu.TEST_VIDEO_URI_SECURED_CLOUD_FRONT;

        // Test 1: Play the protected file directly with Android MediaPlayer.
        //playVideoWithMediaPlayer(videoUrl);

        // Test 2: Play the protected file using Orion360 SDK Pro with MediaPlayer engine.
        playVideoWithOrion360_MediaPlayer(videoUrl);

	}

    @Override
    public void onPause() {
        if (null != mMediaPlayer) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
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
     */
    @SuppressWarnings("unused")
    private void playVideoWithMediaPlayer(String videoUrl) {

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

        // Passing signed cookies with Android MediaPlayer can be done in multiple ways.
        //noinspection ConstantConditions
        if (true) {

            // Recommended way: use custom CookieManager/CookieHandler.
            // This approach uses a list of HttpCookie instances that are set manually.
            // You don't need to set *anything* cookie related to the MediaPlayer,
            // hence you don't necessarily need a handle to it!
            List<HttpCookie> cookies = createCookies();
            CookieManager cookieManager = new CookieManager();
            try {
                URI uri = new URI("https://" + DOMAIN_VALUE);
                for (HttpCookie cookie : cookies) {
                    cookieManager.getCookieStore().add(uri, cookie);
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return;
            }
            CookieHandler.setDefault(cookieManager);

            Logger.logD(TAG, "Manually set cookies:");
            List<HttpCookie> setCookies = cookieManager.getCookieStore().getCookies();
            for (HttpCookie httpCookie : setCookies) {
                Logger.logD(TAG, httpCookie.toString());
            }

            // Cookies are set. We can now use MediaPlayer, it will use set cookies.
            try {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(this, Uri.parse(videoUrl));
                mMediaPlayer.setOnPreparedListener(MediaPlayer::start);
                mMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (Build.VERSION.SDK_INT > 26) {

            // Since: API level 26 (Android Oreo)
            // This approach uses a list of HttpCookies passed as a parameter.
            // You need to have a handle to MediaPlayer instance or option to pass cookie list.
            Map<String, String> headers = new HashMap<>();
            List<HttpCookie> cookies = createCookies();
            try {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                mMediaPlayer.setAudioAttributes(audioAttributes);
                mMediaPlayer.setDataSource(this, Uri.parse(videoUrl), headers, cookies);
                mMediaPlayer.setOnPreparedListener(MediaPlayer::start);
                mMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {

            // Since: API level 14 (Android Ice Cream Sandwich)
            // This approach uses headers to pass signed cookies.
            // - Merge the three required AWS cookies into a single header using semicolon ';'
            // - Some devices are reported to require return + line feed to the end: "\r\n"
            // - You need to have a handle to MediaPlayer instance or option to pass custom headers
            Map<String, String> headers = new HashMap<>();
            String cookieValue =
                    CLOUDFRONT_EXPIRES_KEY + "=" + CLOUDFRONT_EXPIRES_VALUE + "; "
                    + CLOUDFRONT_SIGNATURE_KEY + "=" + CLOUDFRONT_SIGNATURE_VALUE + "; "
                    + CLOUDFRONT_KEY_PAIR_ID_KEY + "=" + CLOUDFRONT_KEY_PAIR_ID_VALUE + "; "
                    + DOMAIN_KEY + "=" + DOMAIN_VALUE + "; "
                    + PATH_KEY + "=" + PATH_VALUE + "; "
                    + SECURE_KEY + "; "
                    + HTTP_ONLY_KEY + "\r\n";
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
        }
    }

    /**
     * An example of playing a protected video file with Orion360 SDK Pro + MediaPlayer engine.
     *
     * @param videoUrl the URL of the video to play.
     */
    @SuppressWarnings("unused")
    private void playVideoWithOrion360_MediaPlayer(String videoUrl) {

        // Set cookies manually using custom CookieHandler *before* using MediaPlayer.
        List<HttpCookie> cookies = createCookies();
        CookieManager cookieManager = new CookieManager();
        try {
            URI uri = new URI("https://" + DOMAIN_VALUE);
            for (HttpCookie cookie : cookies) {
                cookieManager.getCookieStore().add(uri, cookie);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        CookieHandler.setDefault(cookieManager);

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
     * Create a separate HttpCookie instance for each of the three required cookies.
     *
     * Note: CloudFront is very strict about cookie format, see the comments in the code.
     *
     * @return CloudFront compatible HttpCookies.
     */
    private List<HttpCookie> createCookies() {
        List<HttpCookie> cookies = new ArrayList<>();

        //HttpCookie cookie1 = new HttpCookie(
        //        CLOUDFRONT_EXPIRES_KEY, CLOUDFRONT_EXPIRES_VALUE);
        // above: this results to CloudFront-Expires="2147483647" -> NOT OK
        HttpCookie cookie1 = HttpCookie.parse(
                CLOUDFRONT_EXPIRES_KEY + "=" + CLOUDFRONT_EXPIRES_VALUE).get(0);
        // above: this results to CloudFront-Expires=2147483647 -> OK
        cookie1.setDomain(DOMAIN_VALUE);
        cookie1.setPath(PATH_VALUE_NO_ASTERISK); // '/*' -> NOT OK, '/' -> OK
        cookie1.setSecure(true);
        if (Build.VERSION.SDK_INT > 24) {
            cookie1.setHttpOnly(true);
        }
        cookies.add(cookie1);

        HttpCookie cookie2 = HttpCookie.parse(
                CLOUDFRONT_SIGNATURE_KEY + "=" + CLOUDFRONT_SIGNATURE_VALUE).get(0);
        cookie2.setDomain(DOMAIN_VALUE);
        cookie2.setPath(PATH_VALUE_NO_ASTERISK);
        cookie2.setSecure(true);
        if (Build.VERSION.SDK_INT > 24) {
            cookie2.setHttpOnly(true);
        }
        cookies.add(cookie2);

        HttpCookie cookie3 = HttpCookie.parse(
                CLOUDFRONT_KEY_PAIR_ID_KEY + "=" + CLOUDFRONT_KEY_PAIR_ID_VALUE).get(0);
        cookie3.setDomain(DOMAIN_VALUE);
        cookie3.setPath(PATH_VALUE_NO_ASTERISK);
        cookie3.setSecure(true);
        if (Build.VERSION.SDK_INT > 24) {
            cookie3.setHttpOnly(true);
        }
        cookies.add(cookie3);

        return cookies;
    }
}
