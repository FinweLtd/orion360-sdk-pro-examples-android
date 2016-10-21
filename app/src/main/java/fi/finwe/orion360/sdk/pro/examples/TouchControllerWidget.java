package fi.finwe.orion360.sdk.pro.examples;

import fi.finwe.orion360.v3.OrionContext;
import fi.finwe.orion360.v3.OrionScene;
import fi.finwe.orion360.v3.controller.RotationAligner;
import fi.finwe.orion360.v3.controller.TouchPincher;
import fi.finwe.orion360.v3.controller.TouchRotater;
import fi.finwe.orion360.v3.item.OrionCamera;
import fi.finwe.orion360.v3.widget.OrionWidget;
import fi.finwe.util.ContextUtil;

public class TouchControllerWidget implements OrionWidget {
	private OrionCamera 		mCamera;
	
	private TouchPincher		mTouchPincher;
	private TouchRotater		mTouchRotater;
	private RotationAligner 	mRotationAligner;
	
	public TouchControllerWidget(OrionCamera camera) {
		mCamera = camera;
		
		mTouchPincher = new TouchPincher();
		mTouchPincher.setMinimumDistanceDp(OrionContext.getActivity(), 20);		// Set minimum pinch distance in dp
		// Make the pinch adjust the FOV of the camera
		mTouchPincher.bindControllable(mCamera, OrionCamera.VAR_FLOAT1_ZOOM);	
		
		mTouchRotater = new TouchRotater();
		// Make touch drag rotate the main camera
		mTouchRotater.bindControllable(mCamera);
		
		// Retrieve the current display rotation
		int rotDegFromNatural = ContextUtil.getDisplayRotationDegreesFromNatural(OrionContext.getActivity());
		// Create the rotation aligner, responsible for rotating the view so that the horizon aligns with the 
		// user's real-life horizon when the user is not looking up or down 
		mRotationAligner = new RotationAligner();
		mRotationAligner.setDeviceAlignZ(-rotDegFromNatural);
		// Make the SensorFusion rotate the direction according to which the camera is rotated
		OrionContext.getSensorFusion().bindControllable(mRotationAligner);			
		// Make the aligner rotate the camera
		mRotationAligner.bindControllable(mCamera);
	}
	
	@Override
	public void onBindWidget(OrionScene scene) {
		// Bind the controllers to the scene to make them work
		scene.bindController(mTouchPincher);
		scene.bindController(mTouchRotater);
		scene.bindController(mRotationAligner);
	}

	@Override
	public void onReleaseWidget(OrionScene scene) {
		// Remove the controllers from the scene to disable them good
		scene.releaseController(mTouchPincher);
		scene.releaseController(mTouchRotater);
		scene.releaseController(mRotationAligner);
	}

}
