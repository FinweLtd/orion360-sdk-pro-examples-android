/*
 * Copyright (c) 2017, Finwe Ltd. All rights reserved.
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

package fi.finwe.orion360.sdk.pro.examples.layout;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fi.finwe.orion360.sdk.pro.OrionContext;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;
import fi.finwe.orion360.sdk.pro.viewport.OrionDisplayViewport;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.licensing.LicenseManager;
import fi.finwe.orion360.sdk.pro.licensing.LicenseSource;
import fi.finwe.orion360.sdk.pro.licensing.LicenseStatus;
import fi.finwe.orion360.sdk.pro.licensing.LicenseVerifier;
import fi.finwe.orion360.sdk.pro.texture.AndroidMediaPlayerWrapper;
import fi.finwe.orion360.sdk.pro.texture.OrionTexture;
import fi.finwe.orion360.sdk.pro.texture.OrionVideoTexture;
import fi.finwe.orion360.sdk.pro.texture.VideoPlayerWrapper;
import fi.finwe.orion360.sdk.pro.view.OrionView;

/**
 * An example where multiple independent Orion360 views are placed inside a recycler view component.
 * <p/>
 * Orion360 allows placing multiple viewports inside a single Orion360 view via its binding
 * mechanism. This is an efficient way to create multiple presentations of the same content,
 * and also in same cases from multiple different contents. It is frequently used, for example
 * to create a side-by-side view for VR mode.
 * <p/>
 * However, sometimes multiple panoramas need to be rendered on screen completely separately,
 * each having their own instance of Orion360 view. This example demonstrates the concept by
 * using a recycler view component to create a simple video gallery. Each video item has a
 * thumbnail, title and play button overlay that will trigger video playback when tapped.
 * <p/>
 * Some devices support playing multiple videos simultaneously; this can be tested by
 * tapping next video item before the playback of the previous item has ended. However,
 * one must not overcome total video decoding capacity of the device. It is often reasonable
 * to use low-quality previews in video thumbnails and switch to high-quality full-screen
 * playback when user selects a video. This can be achieved also with adaptive HLS video,
 * for example by creating different .m3u8 playlist files that do not contain links to
 * high-quality streams. Another option is to use e.g. ExoPlayer and customize stream
 * selection for preview videos.
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
public class RecyclerViewLayout extends Activity {

    /** Tag for logging. */
    private static final String TAG = RecyclerViewLayout.class.getSimpleName();

    /**
     * OrionContext used to be a static class, but starting from Orion360 3.1.x it must
     * be instantiated as a member.
     */
    private OrionContext mOrionContext;

    /** Panorama item is a simple data model class for the recycler view. */
    @SuppressWarnings("SameParameterValue")
    private static class PanoramaItem {

        /** A title string for the panorama item, to be shown in the UI. */
        private String mTitle;

        /** A content URI for the panorama item, to be given to Orion360 for playback. */
        private String mContentUri;

        /** A volume level for the panorama item, to be given to Orion360 for playback. */
        private float mVolumeLevel = 1.0f;

        /**
         * Set the title string.
         *
         * @param title the title string to be set.
         */
        void setTitle(String title) {
            this.mTitle = title;
        }

        /**
         * Get the title string.
         *
         * @return the title string, or NULL if not set.
         */
        String getTitle() {
            return mTitle;
        }

        /**
         * Set the content URI.
         *
         * @param contentUri the URI where from the panorama content can be played.
         */
        void setContentUri(String contentUri) {
            this.mContentUri = contentUri;
        }

        /**
         * Get the content URI.
         *
         * @return the content URI, or NULL if not set.
         */
        String getContentUri() {
            return mContentUri;
        }

        /**
         * Set the volume level.
         *
         * @param volumeLevel the volume level.
         */
        void setVolumeLevel(float volumeLevel) {
            this.mVolumeLevel = volumeLevel;
        }

        /**
         * Get the volume level.
         *
         * @return the volume level, or 1.0f if not set.
         */
        float getVolumeLevel() {
            return mVolumeLevel;
        }
    }

    /** Adapter class for the recycler view. */
    private class MyRecyclerViewAdapter extends RecyclerView.Adapter<CustomViewHolder> {

        /** A set of panorama items that will be listed within the recycler view. */
        private final List<PanoramaItem> mItems;

        /**
         * Constructor.
         *
         * @param itemList The panorama items to be shown to user.
         */
        MyRecyclerViewAdapter(List<PanoramaItem> itemList) {
            this.mItems = itemList;
        }

        @NonNull
        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            // Inflate a new list row for the recycler view from our XML layout.
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                    R.layout.activity_recyclerview_row, viewGroup, false);

            // Instantiate and return a new view holder.
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final CustomViewHolder customViewHolder, int i) {
            // Bind a particular panorama item to a given view holder, so that we'll
            // show the correct title & thumbnail on screen and respond to tapping
            // by playing the correct panorama video.
            final PanoramaItem item = mItems.get(i);
            customViewHolder.mTitleText.setText(item.getTitle());
            customViewHolder.mThumbnail.setOnClickListener(view -> {
                // When clicked, simply hide the thumbnail and play button
                // and start playing the content with Orion360.
                customViewHolder.mThumbnail.setVisibility(View.INVISIBLE);
                customViewHolder.mPlayButton.setVisibility(View.INVISIBLE);
                customViewHolder.play(item.getContentUri(), item.getVolumeLevel());
            });
        }

        @Override
        public int getItemCount() {
            return (null != mItems ? mItems.size() : 0);
        }
    }

    /** Custom view holder class. */
    class CustomViewHolder extends RecyclerView.ViewHolder {

        /** The thumbnail image view. */
        final ImageView mThumbnail;

        /** The play button image view. */
        final ImageView mPlayButton;

        /** The title text view. */
        final TextView mTitleText;

        /** The Android view where our 3D scene (OrionView) will be added to. */
        protected final OrionViewContainer mViewContainer;

        /** The Orion360 SDK view where our 3D scene will be rendered to. */
        protected OrionView mView;

        /** The 3D scene where our panorama sphere will be added to. */
        OrionScene mScene;

        /** The panorama sphere where our video texture will be mapped to. */
        OrionPanorama mPanorama;

        /** The video texture where our decoded video frames will be updated to. */
        OrionTexture mPanoramaTexture;

        /** The camera which will project our 3D scene to a 2D (view) surface. */
        OrionCamera mCamera;

        /** The video player. */
        VideoPlayerWrapper mVideoPlayer;


        /**
         * Constructor.
         *
         * @param view the inflated layout.
         */
        CustomViewHolder(View view) {
            super(view);

            this.mThumbnail = (ImageView) view.findViewById(R.id.thumbnail);
            this.mPlayButton = (ImageView) view.findViewById(R.id.play_overlay);
            this.mTitleText = (TextView) view.findViewById(R.id.title);
            this.mViewContainer = (OrionViewContainer) view.findViewById(R.id.orion_view_container);
        }

        /**
         * Plays the given content using Orion360.
         *
         * This is a very simple implementation that just initializes Orion360 for playback.
         *
         * @param contentUri the URI where from the content can be played.
         * @param volumeLevel the volume level for the content to be played.
         */
        public void play(String contentUri, final float volumeLevel) {

            // Create a new scene. This represents a 3D world where various objects can be placed.
            mScene = new OrionScene(mOrionContext);

            // Bind sensor fusion as a controller. This will make it available for scene objects.
            mScene.bindRoutine(mOrionContext.getSensorFusion());

            // Create a new panorama. This is a 3D object that will represent a spherical video.
            mPanorama = new OrionPanorama(mOrionContext);

            // Create a new video (or image) texture from a video (or image) source URI.
            mVideoPlayer = new AndroidMediaPlayerWrapper(RecyclerViewLayout.this);
            mPanoramaTexture = new OrionVideoTexture(mOrionContext, mVideoPlayer, contentUri);

            // Set volume level when content has been prepared.
            ((OrionVideoTexture) mPanoramaTexture).addTextureListener(
                    new OrionVideoTexture.ListenerBase() {
                @Override
                public void onVideoPrepared(OrionVideoTexture orionVideoTexture) {
                    mVideoPlayer.setVolume(volumeLevel);
                }
            });

            // Bind the panorama texture to the panorama object.
            mPanorama.bindTextureFull(0, mPanoramaTexture);

            // Bind the panorama to the scene. This will make it part of our 3D world.
            mScene.bindSceneItem(mPanorama);

            // Create a new camera. This will become the end-user's eyes into the 3D world.
            mCamera = new OrionCamera(mOrionContext);

            // Reset view to the 'front' direction (horizontal center of the panorama).
            mCamera.setDefaultRotationYaw(0);

            // Bind camera as a controllable to sensor fusion.
            mOrionContext.getSensorFusion().bindControllable(mCamera);

            // Create a new OrionView and bind it into the container.
            mView = new OrionView(mOrionContext);
            mViewContainer.bindView(mView);

            // Bind the scene to the view. This is the 3D world that we will be rendering.
            mView.bindDefaultScene(mScene);

            // Bind the camera to the view. We will look into the 3D world through this camera.
            mView.bindDefaultCamera(mCamera);

            // The view can be divided into one or more viewports.
            mView.bindViewports(OrionDisplayViewport.VIEWPORT_CONFIG_FULL,
                    OrionDisplayViewport.CoordinateType.FIXED_LANDSCAPE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Propagate activity lifecycle callbacks to the OrionContext object (singleton).
        mOrionContext = new OrionContext();
        mOrionContext.onCreate(this);

        // Perform Orion360 license check. A valid license file should be put to /assets folder!
        verifyOrionLicense();

        // Set content view.
        setContentView(R.layout.activity_recyclerview);

        // Instantiate the recycler view.
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create a couple of panorama items. Here we reuse the same video multiple times,
        // but in a real application there would of course be different content for each item.
        PanoramaItem item1 = new PanoramaItem();
        item1.setTitle("Hello Orion 1");
        item1.setContentUri(MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);
        item1.setVolumeLevel(0.10f);
        PanoramaItem item2 = new PanoramaItem();
        item2.setTitle("Hello Orion 2");
        item2.setContentUri(MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);
        item2.setVolumeLevel(0.50f);
        PanoramaItem item3 = new PanoramaItem();
        item3.setTitle("Hello Orion 3");
        item3.setContentUri(MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);
        item3.setVolumeLevel(1.00f);

        // IMPORTANT:
        // Some Android devices can play back more than one video file at the same time,
        // while some others only play one at a time. Those devices that do play multiple
        // files at the same time typically manage only 2-3 simultaneous videos, no more.
        // Therefore, you should design your user interface so that in general only one
        // video can be played back at a time. The recommendation is to use a thumbnail
        // image as a placeholder and activate only one OrionView at a time.
        // However, multiple panorama images can be played back simultaneously and there
        // is no strict limitation for the maximum number - it depends on device resources.
        // If you want to make the thumbnails feel more 'alive' without playing preview
        // videos simultaneously, consider using a timer to change their thumbnail images.

        // Add panorama items to the item list.
        List<PanoramaItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);
        items.add(item3);

        // Create an adapter for the list and set it to be the recycler view's data adapter.
        MyRecyclerViewAdapter adapter = new MyRecyclerViewAdapter(items);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Propagate fragment lifecycle callbacks to the OrionContext object (singleton).
        mOrionContext.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Propagate fragment lifecycle callbacks to the OrionContext object (singleton).
        mOrionContext.onResume();
    }

    @Override
    public void onPause() {
        // Propagate fragment lifecycle callbacks to the OrionContext object (singleton).
        mOrionContext.onPause();

        super.onPause();
    }

    @Override
    public void onStop() {
        // Propagate fragment lifecycle callbacks to the OrionContext object (singleton).
        mOrionContext.onStop();

        super.onStop();
    }

    @Override
    public void onDestroy() {
        // Propagate fragment lifecycle callbacks to the OrionContext object (singleton).
        mOrionContext.onDestroy();

        super.onDestroy();
    }

    /**
     * Verify Orion360 license. This is the first thing to do after creating OrionContext.
     */
    protected void verifyOrionLicense() {
        LicenseManager licenseManager = mOrionContext.getLicenseManager();
        List<LicenseSource> licenses = LicenseManager.findLicensesFromAssets(this);
        for (LicenseSource license : licenses) {
            LicenseVerifier verifier = licenseManager.verifyLicense(this, license);
            Log.i(TAG, "Orion360 license " + verifier.getLicenseSource().uri + " verified: "
                    + verifier.getLicenseStatus());
            if (verifier.getLicenseStatus() == LicenseStatus.OK) break;
        }
    }
}
