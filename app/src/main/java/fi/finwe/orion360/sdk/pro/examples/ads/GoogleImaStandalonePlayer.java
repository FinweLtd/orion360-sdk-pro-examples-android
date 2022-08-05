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

package fi.finwe.orion360.sdk.pro.examples.ads;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.analytics.DefaultAnalyticsCollector;
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.Util;

import fi.finwe.log.Logger;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.engine.ExoPlayerWrapper;

/**
 * An example of using Google ExoPlayer and Google IMA SDK *without* Orion360 (standalone).
 * <p/>
 * Read SERVING_ADS.md for more information and detailed explanation.
 */
public class GoogleImaStandalonePlayer extends Activity {

    /** Tag for logging. */
    public static final String TAG = ExoPlayerWrapper.class.getSimpleName();

    /** Google ExoPlayer. */
    protected ExoPlayer mExoPlayer;

    /** ExoPlayer's own styled player view. */
    protected StyledPlayerView mStyledPlayerView;

    /** Google IMA ad loader. */
    protected ImaAdsLoader mImaAdsLoader;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Layout for IMA standalone example.
        setContentView(R.layout.activity_ad_standalone);

        // Find styled player view. This is where both ads and media content will appear.
        mStyledPlayerView = findViewById(R.id.exoplayer_styled_player_view);

        // Create an AdsLoader. This will handle everything related to ads.
        mImaAdsLoader = new ImaAdsLoader.Builder(this).build();
	}

    /**
     * Initialize player.
     */
    private void initializePlayer() {
        Logger.logF();

        // Create ExoPlayer instance and play content with it. Parts related to ads
        // are marked with IMPORTANT comment.
        try {
            DefaultRenderersFactory defaultRenderersFactory =
                    new DefaultRenderersFactory(this);

            // Set up the factory for media sources, passing the ads loader and ad view providers.
            String userAgent = Util.getUserAgent(this, getString(R.string.app_name));
            DefaultHttpDataSource.Factory defaultHttpDataSourceFactory =
                    new DefaultHttpDataSource.Factory().setUserAgent(userAgent);
            DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
                    this, defaultHttpDataSourceFactory);
            DefaultMediaSourceFactory defaultMediaSourceFactory =
                    new DefaultMediaSourceFactory(dataSourceFactory)
                            .setLocalAdInsertionComponents(         // IMPORTANT
                                    unusedAdTagUri -> mImaAdsLoader,
                                    mStyledPlayerView);

            AdaptiveTrackSelection.Factory videoTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory();
            TrackSelector trackSelector = new DefaultTrackSelector(this,
                    videoTrackSelectionFactory);

            DefaultLoadControl defaultLoadControl = new DefaultLoadControl();

            BandwidthMeter bandwidthMeter =
                    new DefaultBandwidthMeter.Builder(this).build();

            DefaultAnalyticsCollector defaultAnalyticsCollector =
                    new DefaultAnalyticsCollector(Clock.DEFAULT);

            // Create an ExoPlayer and set it as the player for content and ads.
            ExoPlayer.Builder exoplayerBuilder = new ExoPlayer.Builder(this,
                    defaultRenderersFactory, defaultMediaSourceFactory, trackSelector,
                    defaultLoadControl, bandwidthMeter, defaultAnalyticsCollector);
            mExoPlayer = exoplayerBuilder.build();
            mStyledPlayerView.setPlayer(mExoPlayer);
            mImaAdsLoader.setPlayer(mExoPlayer);                    // IMPORTANT

            // Create the MediaItem to play, specifying the content URI and ad tag URI.
            Uri contentUri = Uri.parse(MainMenu.TEST_VIDEO_URI_HLS);
            Uri adTagUri = Uri.parse(getString(R.string.ad_tag_url));
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(contentUri)
                    .setAdsConfiguration(                           // IMPORTANT
                            new MediaItem.AdsConfiguration.Builder(adTagUri).build())
                    .build();

            // Prepare the content (and ad) to be played with the ExoPlayer instance.
            mExoPlayer.setMediaItem(mediaItem);
            mExoPlayer.prepare();

            // Set PlayWhenReady. If true, content and ads will autoplay.
            mExoPlayer.setPlayWhenReady(false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Release player.
     */
    private void releasePlayer() {
        Logger.logF();

        if (null != mImaAdsLoader) {
            mImaAdsLoader.setPlayer(null);
        }
        if (null != mStyledPlayerView) {
            mStyledPlayerView.setPlayer(null);
            mStyledPlayerView = null;
        }
        if (null != mExoPlayer) {
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (Util.SDK_INT > 23) {
            initializePlayer();

            if (mStyledPlayerView != null) {
                mStyledPlayerView.onResume();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Util.SDK_INT <= 23 || mStyledPlayerView == null) {
            initializePlayer();

            if (mStyledPlayerView != null) {
                mStyledPlayerView.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (Util.SDK_INT <= 23) {
            if (mStyledPlayerView != null) {
                mStyledPlayerView.onPause();
            }

            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (Util.SDK_INT > 23) {
            if (mStyledPlayerView != null) {
                mStyledPlayerView.onPause();
            }

            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mImaAdsLoader.release();
    }
}
