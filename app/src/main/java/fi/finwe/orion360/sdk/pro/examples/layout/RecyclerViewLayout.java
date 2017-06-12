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
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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

    private List<PanoramaItem> mItems;
    private RecyclerView mRecyclerView;
    private MyRecyclerViewAdapter mAdapter;

    /** Model class. */
    public class PanoramaItem {
        private String mTitle;
        private String mContentUri;

        public String getTitle() {
            return mTitle;
        }

        public void setTitle(String title) {
            this.mTitle = title;
        }

        public String getContentUri() {
            return mContentUri;
        }

        public void setContentUri(String contentUri) {
            this.mContentUri = contentUri;
        }
    }

    /** Recycler view adapter class. */
    class MyRecyclerViewAdapter extends
            RecyclerView.Adapter<MyRecyclerViewAdapter.CustomViewHolder> {
        private Context mContext;
        private List<PanoramaItem> mItemList;

        public MyRecyclerViewAdapter(Context context, List<PanoramaItem> itemList) {
            this.mContext = context;
            this.mItemList = itemList;
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                    R.layout.activity_recyclerview_row, null);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CustomViewHolder customViewHolder, int i) {
            PanoramaItem item = mItemList.get(i);

            customViewHolder.mTextView.setText(Html.fromHtml(item.getTitle()));

            customViewHolder.mScene = new OrionScene();
            customViewHolder.mScene.bindController(mOrionContext.getSensorFusion());

            customViewHolder.mPanorama = new OrionPanorama();
            customViewHolder.mPanoramaTexture = OrionTexture.createTextureFromURI(mContext,
                    item.getContentUri());

            customViewHolder.mPanorama.bindTextureFull(0, customViewHolder.mPanoramaTexture);
            customViewHolder.mScene.bindSceneItem(customViewHolder.mPanorama);

            customViewHolder.mCamera = new OrionCamera();
            customViewHolder.mCamera.setZoomMax(3.0f);
            customViewHolder.mCamera.setRotationYaw(0);

            mOrionContext.getSensorFusion().bindControllable(customViewHolder.mCamera);

            customViewHolder.mOrionView.bindDefaultScene(customViewHolder.mScene);
            customViewHolder.mOrionView.bindDefaultCamera(customViewHolder.mCamera);
            customViewHolder.mOrionView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL,
                    OrionViewport.CoordinateType.FIXED_LANDSCAPE);
        }

        @Override
        public int getItemCount() {
            return (null != mItemList ? mItemList.size() : 0);
        }

        /** View holder class. */
        class CustomViewHolder extends RecyclerView.ViewHolder {
            protected OrionView mOrionView;
            protected TextView mTextView;
            protected OrionScene mScene;
            protected OrionPanorama mPanorama;
            protected OrionTexture mPanoramaTexture;
            protected OrionCamera mCamera;

            public CustomViewHolder(View view) {
                super(view);
                this.mOrionView = (OrionView) view.findViewById(R.id.orion_view);
                this.mTextView = (TextView) view.findViewById(R.id.title);
            }
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

        // Instantiate RecyclerView.
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create an item.
        PanoramaItem item = new PanoramaItem();
        item.setTitle("Hello Orion");
        item.setContentUri(MainMenu.PRIVATE_ASSET_FILES_PATH + MainMenu.TEST_VIDEO_FILE_MQ);

        mItems = new ArrayList<>();
        mItems.add(item);

        // Set adapter.
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
