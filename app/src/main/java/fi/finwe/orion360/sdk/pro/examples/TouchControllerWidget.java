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

package fi.finwe.orion360.sdk.pro.examples;

import fi.finwe.orion360.sdk.pro.OrionContext;
import fi.finwe.orion360.sdk.pro.OrionScene;
import fi.finwe.orion360.sdk.pro.controller.RotationAligner;
import fi.finwe.orion360.sdk.pro.controller.TouchPincher;
import fi.finwe.orion360.sdk.pro.controller.TouchRotater;
import fi.finwe.orion360.sdk.pro.item.OrionCamera;
import fi.finwe.orion360.sdk.pro.widget.OrionWidget;
import fi.finwe.util.ContextUtil;

/**
 * Convenience class for configuring typical touch control logic.
 */
public class TouchControllerWidget implements OrionWidget {

    /** Touch pinch-to-zoom/pinch-to-rotate gesture handler. */
    private final TouchPincher mTouchPincher;

    /** Touch drag-to-pan gesture handler. */
    private final TouchRotater mTouchRotater;

    /** Rotation aligner keeps the horizon straight at all times. */
    private final RotationAligner mRotationAligner;


    /**
     * Constructs the widget.
     *
     * @param camera The camera to be controlled by this widget.
     */
    public TouchControllerWidget(OrionContext orionContext, OrionCamera camera) {

        // Create pinch-to-zoom/pinch-to-rotate handler.
        mTouchPincher = new TouchPincher();
        mTouchPincher.setMinimumDistanceDp(orionContext.getActivity(), 20);
        mTouchPincher.bindControllable(camera, OrionCamera.VAR_FLOAT1_ZOOM);

        // Create drag-to-pan handler.
        mTouchRotater = new TouchRotater();
        mTouchRotater.bindControllable(camera);

        // Create the rotation aligner, responsible for rotating the view so that the horizon
        // aligns with the user's real-life horizon when the user is not looking up or down.
        mRotationAligner = new RotationAligner();
        mRotationAligner.setDeviceAlignZ(-ContextUtil.getDisplayRotationDegreesFromNatural(
                orionContext.getActivity()));
        mRotationAligner.bindControllable(camera);

        // Rotation aligner needs sensor fusion data in order to do its job.
        orionContext.getSensorFusion().bindControllable(mRotationAligner);
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
