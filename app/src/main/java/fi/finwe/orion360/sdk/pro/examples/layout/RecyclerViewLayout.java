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
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fi.finwe.log.Logger;
import fi.finwe.orion360.sdk.pro.OrionContext;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.MainMenu;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.examples.appfw.CustomFragmentActivity;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.item.OrionPanorama;
import fi.finwe.orion360.sdk.pro.licensing.LicenseManager;
import fi.finwe.orion360.sdk.pro.licensing.LicenseSource;
import fi.finwe.orion360.sdk.pro.licensing.LicenseStatus;
import fi.finwe.orion360.sdk.pro.licensing.LicenseVerifier;
import fi.finwe.orion360.sdk.pro.source.OrionTexture;
import fi.finwe.orion360.sdk.pro.view.OrionView;

/**
 * An example of placing an Orion360 view inside a recycler view component.
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
    public static final String TAG = CustomFragmentActivity.class.getSimpleName();

    /**
     * OrionContext used to be a static class, but starting from Orion360 3.1.x it must
     * be instantiated as a member.
     */
    protected OrionContext mOrionContext;

    /** A recycler view for listing a set of panorama items in the UI. */
    RecyclerView mRecyclerView;

    /** The data adapter for the recycler view. */
    MyRecyclerViewAdapter mAdapter;

     /** A set of panorama items to be shown in the recycler view. */
    List<PanoramaItem> mItems;

    /** Recycler view data model class. */
    private class PanoramaItem {
        private String mTitle;
        private String mContentUri;

        String getTitle() {
            return mTitle;
        }
        void setTitle(String title) {
            this.mTitle = title;
        }
        String getContentUri() {
            return mContentUri;
        }
        void setContentUri(String contentUri) {
            this.mContentUri = contentUri;
        }
    }

    /** Recycler view data adapter class. */
    private class MyRecyclerViewAdapter extends RecyclerView.Adapter<CustomViewHolder> {
        private Context mContext;
        private List<PanoramaItem> mItems;

        MyRecyclerViewAdapter(Context context, List<PanoramaItem> itemList) {
            this.mContext = context;
            this.mItems = itemList;
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                    R.layout.activity_recyclerview_row, null);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final CustomViewHolder customViewHolder, int i) {
            final PanoramaItem item = mItems.get(i);
            customViewHolder.mTitleText.setText(item.getTitle());
            customViewHolder.mThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    customViewHolder.mThumbnail.setVisibility(View.INVISIBLE);
                    customViewHolder.mPlayButton.setVisibility(View.INVISIBLE);
                    customViewHolder.play(item.getContentUri());
                }
            });
        }

        @Override
        public int getItemCount() {
            return (null != mItems ? mItems.size() : 0);
        }
    }

    /** Custom view holder class. */
    class CustomViewHolder extends RecyclerView.ViewHolder {
        ImageView mThumbnail;
        ImageView mPlayButton;
        TextView mTitleText;
        OrionView mOrionView;
        OrionScene mScene;
        OrionPanorama mPanorama;
        OrionTexture mPanoramaTexture;
        OrionCamera mCamera;

        CustomViewHolder(View view) {
            super(view);
            this.mThumbnail = (ImageView) view.findViewById(R.id.thumbnail);
            this.mPlayButton = (ImageView) view.findViewById(R.id.play_overlay);
            this.mTitleText = (TextView) view.findViewById(R.id.title);
            this.mOrionView = (OrionView) view.findViewById(R.id.orion_view);
        }

        public void play(String contentUri) {
            Logger.logF();

            mScene = new OrionScene();
            mScene.bindController(mOrionContext.getSensorFusion());

            mPanorama = new OrionPanorama();
            mPanoramaTexture = OrionTexture.createTextureFromURI(
                    RecyclerViewLayout.this, contentUri);
            mPanorama.bindTextureFull(0, mPanoramaTexture);
            mScene.bindSceneItem(mPanorama);

            mCamera = new OrionCamera();
            mCamera.setZoomMax(3.0f);
            mCamera.setRotationYaw(0);
            mOrionContext.getSensorFusion().bindControllable(mCamera);

            mOrionView.bindDefaultScene(mScene);
            mOrionView.bindDefaultCamera(mCamera);
            mOrionView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL,
                    OrionViewport.CoordinateType.FIXED_LANDSCAPE);
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
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create a couple of panorama items.
        PanoramaItem item1 = new PanoramaItem();
        item1.setTitle("Hello Orion 1");
        item1.setContentUri(MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);
        PanoramaItem item2 = new PanoramaItem();
        item2.setTitle("Hello Orion 2");
        item2.setContentUri(MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);
        PanoramaItem item3 = new PanoramaItem();
        item3.setTitle("Hello Orion 3");
        item3.setContentUri(MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);

        // IMPORTANT:
        // Some Android devices can play back more than one video file at the same time,
        // while some others only play one at a time. Those devices that do play multiple
        // files at the same time typically manage only 2-3 simultaneous videos, no more.
        // Therefore, you should design your user interface so that in general only one
        // video can be played back at a time. The recommendation is to use a thumbnail
        // image as a placeholder and activate only one OrionView at a time.
        // However, multiple panorama images can be played back simultaneously and there
        // is no strict limitation for the maximum number - it depends on device resources.

        // Add panorama item to the item list.
        mItems = new ArrayList<>();
        mItems.add(item1);
        mItems.add(item2);
        mItems.add(item3);

        // Create an adapter for the list and set it to be the recycler view's adapter.
        mAdapter = new MyRecyclerViewAdapter(this, mItems);
        mRecyclerView.setAdapter(mAdapter);
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
