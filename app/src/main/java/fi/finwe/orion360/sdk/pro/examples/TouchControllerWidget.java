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
    @SuppressWarnings("CommentedOutCode")
    public TouchControllerWidget(OrionContext orionContext, OrionCamera camera) {

        // Create pinch-to-zoom/pinch-to-rotate handler.
        mTouchPincher = new TouchPincher(orionContext);
        mTouchPincher.setMinimumDistanceDp(orionContext.getActivity(), 20);
        mTouchPincher.bindControllable(camera, OrionCamera.VAR_FLOAT1_ZOOM);

        // Create drag-to-pan handler.
        mTouchRotater = new TouchRotater(orionContext);
        mTouchRotater.bindControllable(camera);

        // Create the rotation aligner, responsible for rotating the view so that the horizon
        // aligns with the user's real-life horizon when the user is not looking up or down.
        mRotationAligner = new RotationAligner(orionContext);
        mRotationAligner.setDeviceAlignZ(-ContextUtil.getDisplayRotationDegreesFromNatural(
                orionContext.getActivity()));
        mRotationAligner.bindControllable(camera);

        // Rotation aligner needs sensor fusion data in order to do its job.
        orionContext.getSensorFusion().bindControllable(mRotationAligner);

        // If you want to fine-tune touch control, see example below.

        // Tune rotation aligner less aggressive. It should take action when the speed is
        // approaching to zero ie. when we are soon stopping and there is a clear risk that
        // horizon will be left unaligned if we don't correct it.
        //mRotationAligner.setRotationSpeedLimits(0.5f, 1.5f);

        // Set rotation aligner dead zone width (in degrees). This defines how far behind
        // the nadir or zenith the user can pan until the rotation aligner takes action.
        // Here we use a value that allows examining nadir and zenith quite freely.
        //mRotationAligner.setDeadZoneDeg(30.0f);

        // The approach above has a serious problem: It is common that user holds the device
        // slightly tilted down, for example pitch = -45 degrees (typical reading position).
        // Then, he wants to rotate the view horizontally (pan along the yaw angle). So he
        // swipes to left or right. Instead of rotating the yaw angle (only), the view is
        // rotated along a plane that is tilted -45 degrees and user ends up viewing the roof.

        // This can be solved by using a different configuration for the rotation aligner:
        // We must disable speed limits so that the rotation aligner is always active, and
        // expand coverage by making the dead zone areas smaller:
        //mRotationAligner.setRotationSpeedLimits(-1, -1);
        //mRotationAligner.setDeadZoneDeg(20.0f);
    }

    @Override
    public void onBindWidget(OrionScene scene) {
        // When widget is bound to scene, bind the controllers to it to make them functional.
        scene.bindRoutine(mTouchPincher);
        scene.bindRoutine(mTouchRotater);
        scene.bindRoutine(mRotationAligner);
    }

    @Override
    public void onReleaseWidget(OrionScene scene) {
        // When widget is released from scene, release the controllers as well.
        scene.releaseRoutine(mTouchPincher);
        scene.releaseRoutine(mTouchRotater);
        scene.releaseRoutine(mRotationAligner);
    }
}
