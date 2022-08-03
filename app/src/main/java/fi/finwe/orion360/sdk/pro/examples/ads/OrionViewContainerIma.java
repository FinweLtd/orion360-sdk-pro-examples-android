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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ui.AdOverlayInfo;
import com.google.android.exoplayer2.ui.AdViewProvider;
import com.google.android.exoplayer2.util.Assertions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.sdk.pro.view.OrionViewContainer;

/**
 * OrionViewContainer with support for Google IMA ads.
 */
public class OrionViewContainerIma extends FrameLayout implements AdViewProvider {

    /** Actual OrionViewContainer. */
    private OrionViewContainer mOrionViewContainer;

    /** This overlay is mandatory and will show the ads. */
    private FrameLayout mAdOverlay;

    /** This overlay, if present, is transparent and may capture touch events etc. */
    @Nullable
    private FrameLayout mTransparentOverlay;

    /** This overlay, if present, may contain transient mandatory controls. */
    @Nullable
    private FrameLayout mControlOverlay;


    /**
     * Constructor.
     *
     * @param context the context.
     */
    public OrionViewContainerIma(Context context) {
        this(context, (AttributeSet) null);
    }

    /**
     * Constructor.
     *
     * @param context the context.
     * @param attrs attributes.
     */
    public OrionViewContainerIma(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor.
     *
     * @param context the context.
     * @param attrs attributes.
     * @param defStyle style.
     */
    public OrionViewContainerIma(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        int playerLayoutId = R.layout.orion_view_container_ima;
        LayoutInflater.from(context).inflate(playerLayoutId, this);

        mOrionViewContainer = (OrionViewContainer) this.findViewById(
                R.id.orion_view_container);
        mAdOverlay = (FrameLayout) this.findViewById(
                R.id.orion_ad_overlay);
        mTransparentOverlay = (FrameLayout) this.findViewById(
                R.id.orion_transparent_overlay);
        mControlOverlay = (FrameLayout) this.findViewById(
                R.id.orion_control_overlay);
    }

    public OrionViewContainer getOrionViewContainer() {
        return mOrionViewContainer;
    }

    @Override
    public ViewGroup getAdViewGroup() {
        return (ViewGroup)Assertions.checkStateNotNull(mAdOverlay,
                "orion_ad_overlay must be present for ad playback");
    }

    @NonNull
    @Override
    public List<AdOverlayInfo> getAdOverlayInfos() {
        List<AdOverlayInfo> overlayViews = new ArrayList<>();

        if (mTransparentOverlay != null) {
            overlayViews.add(new AdOverlayInfo.Builder(
                    mTransparentOverlay, AdOverlayInfo.PURPOSE_NOT_VISIBLE).build());
        }
        if (mControlOverlay != null) {
            overlayViews.add(new AdOverlayInfo.Builder(
                    mControlOverlay, AdOverlayInfo.PURPOSE_CONTROLS).build());
        }

        return ImmutableList.copyOf(overlayViews);
    }
}
