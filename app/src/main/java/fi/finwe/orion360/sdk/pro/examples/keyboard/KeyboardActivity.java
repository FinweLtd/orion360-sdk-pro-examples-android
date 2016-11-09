package fi.finwe.orion360.sdk.pro.examples.keyboard;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.graphics.Color;
import android.os.Bundle;
import android.view.SoundEffectConstants;
import fi.finwe.log.Logger;
import fi.finwe.math.Matrix44f;
import fi.finwe.math.QuatF;
import fi.finwe.math.Vec2F;
import fi.finwe.math.Vec3F;
import fi.finwe.orion360.v3.OrionActivity;
import fi.finwe.orion360.v3.OrionContext;
import fi.finwe.orion360.v3.OrionView;
import fi.finwe.orion360.v3.OrionViewport;
import fi.finwe.orion360.sdk.pro.examples.R;
import fi.finwe.orion360.v3.controllable.DisplayClickable;
import fi.finwe.orion360.v3.controllable.WorldClickable;
import fi.finwe.orion360.v3.controller.Clicker.ClickType;
import fi.finwe.orion360.v3.controller.TouchDisplayClickListener;
import fi.finwe.orion360.v3.controller.TouchWorldClickListener;
import fi.finwe.orion360.v3.controller.TouchWorldClickListener.Listener;
import fi.finwe.orion360.sdk.pro.examples.BaseScene;
import fi.finwe.orion360.v3.item.OrionCamera;
import fi.finwe.orion360.v3.item.OrionPolygon;
import fi.finwe.orion360.v3.item.OrionPolygon.Face;
import fi.finwe.orion360.v3.item.OrionSceneItem;
import fi.finwe.orion360.v3.item.OrionSceneItem.RenderingMode;
import fi.finwe.orion360.v3.item.OrionSceneItem.RotationBaseControllerListenerBase;
import fi.finwe.orion360.v3.item.OrionText;
import fi.finwe.orion360.v3.item.sprite.OrionSprite;
import fi.finwe.orion360.v3.source.OrionTexture;

public class KeyboardActivity extends OrionActivity implements Listener {

	private static class KeyMapping {
		KeyMapping(String mat, String ch) { material = mat; function = ch; }
		public String material;
		public String function;
	}
	
	private OrionView 		mView;
	private BaseScene		mScene;
	private OrionCamera 	mDefaultCamera;
	
	private OrionPolygon	mPolygon;
	private OrionSprite		mReticle;
	private TouchWorldClickListener 	mWorldClickListener;
	private TouchDisplayClickListener 	mDisplayClickListener;
	
	private Timer mTimer; 
	
	private String		mText = "";
	private OrionText 	mTextItem;
	
	private String 		mTextAreaTag = null;
	
	private List<String> 	mPressedButtons = new LinkedList<String>();
	private boolean			mUpperCaseEnabled = false;
	private boolean			mSpecialKeysEnabled = false;
	
	private Map<String,String> 		mKeyGroupMap = new HashMap<String,String>();
	private Map<String,KeyMapping> 	mSpecialKeyMap = createKeyMapping();
	private Map<String,KeyMapping> 	mLowerCaseKeyMap = createKeyMapping();
	private Map<String,KeyMapping> 	mUpperCaseKeyMap = createKeyMapping();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setSystemUiVisibility(false);
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		OrionContext.getSensorFusion().setMagnetometerEnabled(false);
		mScene = new BaseScene();
		mScene.bindController(OrionContext.getSensorFusion());
		mScene.setSourceURI(this, "asset://Orion360_test_image_1920x960.jpg");
		
		mDefaultCamera = new OrionCamera();
		mDefaultCamera.setZoomMax(3.0f);
		mDefaultCamera.setVRCameraDistance(0.035f);
		mDefaultCamera.setVRCameraFocalDistance(1.5f);
		mDefaultCamera.setRotationBaseControllerListener(new RotationBaseControllerListenerBase() {
			@Override
			public void onRotationBaseControllerBound(OrionSceneItem item, QuatF rotationBase) {
				super.onRotationBaseControllerBound(item, rotationBase);
				item.setRotationYaw(0);
				mScene.setVisible(true);
			}
		});
		
		OrionContext.getSensorFusion().bindControllable(mDefaultCamera);
		
		// Associate special keys with certain positions on the keyboard
		// Value is the key ID (i.e. 'a' etc.). Key is the material name to use for the corresponding special key. 
		mLowerCaseKeyMap.put("q", 	new KeyMapping("q", 			"q"));
		mLowerCaseKeyMap.put("w", 	new KeyMapping("w", 			"w"));
		mLowerCaseKeyMap.put("e", 	new KeyMapping("e", 			"e"));
		mLowerCaseKeyMap.put("r", 	new KeyMapping("r", 			"r"));
		mLowerCaseKeyMap.put("t", 	new KeyMapping("t", 			"t"));
		mLowerCaseKeyMap.put("y", 	new KeyMapping("y", 			"y"));
		mLowerCaseKeyMap.put("u", 	new KeyMapping("u", 			"u"));
		mLowerCaseKeyMap.put("i", 	new KeyMapping("i", 			"i"));
		mLowerCaseKeyMap.put("o", 	new KeyMapping("o", 			"o"));
		mLowerCaseKeyMap.put("p", 	new KeyMapping("p", 			"p"));
		mLowerCaseKeyMap.put("a", 	new KeyMapping("a", 			"a"));
		mLowerCaseKeyMap.put("s", 	new KeyMapping("s", 			"s"));
		mLowerCaseKeyMap.put("d", 	new KeyMapping("d", 			"d"));
		mLowerCaseKeyMap.put("f", 	new KeyMapping("f", 			"f"));
		mLowerCaseKeyMap.put("g", 	new KeyMapping("g", 			"g"));
		mLowerCaseKeyMap.put("h", 	new KeyMapping("h", 			"h"));
		mLowerCaseKeyMap.put("j", 	new KeyMapping("j", 			"j"));
		mLowerCaseKeyMap.put("k", 	new KeyMapping("k", 			"k"));
		mLowerCaseKeyMap.put("l", 	new KeyMapping("l", 			"l"));
		mLowerCaseKeyMap.put("spare", new KeyMapping("tyhja", 		""));
		mLowerCaseKeyMap.put("shift", new KeyMapping("shift", 				"$shift"));
		mLowerCaseKeyMap.put("z", 	new KeyMapping("z", 			"z"));
		mLowerCaseKeyMap.put("x", 	new KeyMapping("x", 			"x"));
		mLowerCaseKeyMap.put("c", 	new KeyMapping("c", 			"c"));
		mLowerCaseKeyMap.put("v", 	new KeyMapping("v", 			"v"));
		mLowerCaseKeyMap.put("b", 	new KeyMapping("b", 			"b"));
		mLowerCaseKeyMap.put("n", 	new KeyMapping("n", 			"n"));
		mLowerCaseKeyMap.put("m", 	new KeyMapping("m", 			"m"));
		mLowerCaseKeyMap.put("spare2", 	new KeyMapping("tyhja", 		""));
		
		mUpperCaseKeyMap.put("q", 	new KeyMapping("upper_q",		"Q"));
		mUpperCaseKeyMap.put("w", 	new KeyMapping("upper_w", 		"W"));
		mUpperCaseKeyMap.put("e", 	new KeyMapping("upper_e", 		"E"));
		mUpperCaseKeyMap.put("r", 	new KeyMapping("upper_r", 		"R"));
		mUpperCaseKeyMap.put("t", 	new KeyMapping("upper_t", 		"T"));
		mUpperCaseKeyMap.put("y", 	new KeyMapping("upper_y", 		"Y"));
		mUpperCaseKeyMap.put("u", 	new KeyMapping("upper_u", 		"U"));
		mUpperCaseKeyMap.put("i", 	new KeyMapping("upper_i", 		"I"));
		mUpperCaseKeyMap.put("o", 	new KeyMapping("upper_o", 		"O"));
		mUpperCaseKeyMap.put("p", 	new KeyMapping("upper_p", 		"P"));
		mUpperCaseKeyMap.put("a", 	new KeyMapping("upper_a", 		"A"));
		mUpperCaseKeyMap.put("s", 	new KeyMapping("upper_s", 		"S"));
		mUpperCaseKeyMap.put("d", 	new KeyMapping("upper_d", 		"D"));
		mUpperCaseKeyMap.put("f", 	new KeyMapping("upper_f", 		"F"));
		mUpperCaseKeyMap.put("g", 	new KeyMapping("upper_g", 		"G"));
		mUpperCaseKeyMap.put("h", 	new KeyMapping("upper_h", 		"H"));
		mUpperCaseKeyMap.put("j", 	new KeyMapping("upper_j", 		"J"));
		mUpperCaseKeyMap.put("k", 	new KeyMapping("upper_k", 		"K"));
		mUpperCaseKeyMap.put("l", 	new KeyMapping("upper_l", 		"L"));
		mUpperCaseKeyMap.put("spare", new KeyMapping("tyhja", 		""));
		mUpperCaseKeyMap.put("shift", new KeyMapping("shift", 				"$shift"));
		mUpperCaseKeyMap.put("z", 	new KeyMapping("upper_z", 		"Z"));
		mUpperCaseKeyMap.put("x", 	new KeyMapping("upper_x", 		"X"));
		mUpperCaseKeyMap.put("c", 	new KeyMapping("upper_c", 		"C"));
		mUpperCaseKeyMap.put("v", 	new KeyMapping("upper_v", 		"V"));
		mUpperCaseKeyMap.put("b", 	new KeyMapping("upper_b", 		"B"));
		mUpperCaseKeyMap.put("n", 	new KeyMapping("upper_n", 		"N"));
		mUpperCaseKeyMap.put("m", 	new KeyMapping("upper_m", 		"M"));
		mUpperCaseKeyMap.put("spare2", 	new KeyMapping("tyhja", 	""));
		
		mSpecialKeyMap.put("q", 	new KeyMapping("plus", 			"+"));
		mSpecialKeyMap.put("w", 	new KeyMapping("valiviiva", 	"-"));
		mSpecialKeyMap.put("e", 	new KeyMapping("yhtakuin", 		"="));
		mSpecialKeyMap.put("r", 	new KeyMapping("prosentti", 	"%"));
		mSpecialKeyMap.put("t", 	new KeyMapping("eksponentti", 	"^"));
		mSpecialKeyMap.put("y", 	new KeyMapping("ylamaki", 		"/"));
		mSpecialKeyMap.put("u", 	new KeyMapping("alamaki", 		"\\"));
		mSpecialKeyMap.put("i", 	new KeyMapping("euro", 			"\u20AC"));
		mSpecialKeyMap.put("o", 	new KeyMapping("taala", 		"$"));
		mSpecialKeyMap.put("p", 	new KeyMapping("punta", 		"\u00a3"));
		mSpecialKeyMap.put("a", 	new KeyMapping("at", 			"@"));
		mSpecialKeyMap.put("s", 	new KeyMapping("asteriski", 	"*"));
		mSpecialKeyMap.put("d", 	new KeyMapping("tilde", 		"~"));
		mSpecialKeyMap.put("f", 	new KeyMapping("risuaita", 		"#"));
		mSpecialKeyMap.put("g", 	new KeyMapping("kaksoispiste", 	":"));
		mSpecialKeyMap.put("h", 	new KeyMapping("puolipiste", 	";"));
		mSpecialKeyMap.put("j", 	new KeyMapping("alaviiva", 		"_"));
		mSpecialKeyMap.put("k", 	new KeyMapping("et", 			"&"));
		mSpecialKeyMap.put("l", 	new KeyMapping("sulkuauki", 	"("));
		mSpecialKeyMap.put("spare", new KeyMapping("sulkukiinni", 	")"));
		mSpecialKeyMap.put("shift", new KeyMapping("pienempi", 		"<"));
		mSpecialKeyMap.put("z", 	new KeyMapping("isompi", 		">"));
		mSpecialKeyMap.put("x", 	new KeyMapping("heitto", 		"'"));
		mSpecialKeyMap.put("c", 	new KeyMapping("lainausmerkki", "\""));
		mSpecialKeyMap.put("v", 	new KeyMapping("piste", 		"."));
		mSpecialKeyMap.put("b", 	new KeyMapping("pilkku", 		","));
		mSpecialKeyMap.put("n", 	new KeyMapping("huuto", 		"!"));
		mSpecialKeyMap.put("m", 	new KeyMapping("kysymysmerkki", "?"));
		mSpecialKeyMap.put("spare2", 	new KeyMapping("tyhja", 	""));
		
		// kertomerkki
		// jako
		// pystyviiva
		// haka auki
		// haka kiinni
		
//		mScene.getPanorama().setActive(false);
		
		mPolygon = new OrionPolygon();
		mPolygon.setSourceURI("asset://keyboard_3d_tekstuurit_3_1_1.obj", "*");
		mPolygon.setWorldTranslation(new Vec3F(0,0,-0.7f));
		mPolygon.setScale(0.2f);
		mScene.bindSceneItem(mPolygon);
		
		OrionText.Style textStyle = new OrionText.Style()
//				.setFontFamily("sans-serif")
//				.setFontFamilyLang("ja")
				.setTypefaceFromAsset("fonts/SourceSansPro-LightItalic.ttf")
//				.setPointSize(72, 72)
				.setPointSize(16, 16)
				.setTextColor(Color.argb(255, 255, 0, 0));
		mTextItem = new OrionText("", textStyle);
		mTextItem.setRenderingMode(OrionText.RenderingMode.PERSPECTIVE);	// Render the text in the 3d world using the camera
		mTextItem.setWorldTranslation(new Vec3F(0.0f, 0.0f, -0.6999f));		// Render a little bit in front of the keyboard polygon
		mTextItem.setScale(0.2f);											// Set the same scale as the keyboard polygon -> model coordinates match
		mTextItem.setTextAlign(OrionText.ALIGN_CENTER);
		mScene.bindSceneItem(mTextItem);
		
		// Create & init the reticle sprite
		mReticle = new OrionSprite();
		mReticle.bindTexture(OrionTexture.createTextureFromURI(this, "asset://rect3812_2_150.png"));
		mReticle.setRenderingMode(RenderingMode.CAMERA_DISABLED);	// Disconnect the reticle from the camera
		mReticle.setWorldTranslation(new Vec3F(0,0,-0.4f));
		mReticle.setScale(0.05f);
		mReticle.setTextureAlign(OrionSprite.AlignMode.CENTER);
		mScene.bindSceneItem(mReticle);
		
		// Get all polygon object tags in the object file
		for (String objectTag: mPolygon.getObjectTags()) {
			// Look for a particular object tag representing the text area
			if (objectTag.startsWith("text_area") == true) {
				mTextAreaTag = objectTag;
				// Get all group tags in the text area object
				for (String groupTag: mPolygon.getGroupTags(objectTag)) {
					// Get all faces in the group
					if (groupTag.endsWith("mr_white") == true) {
						List<Face> faces = mPolygon.getFacesByGroup(objectTag, groupTag);
						mTextItem.setTextTransform3D(faces.get(0));
						break;
					}
				}
			} else if (objectTag.startsWith("key_") == true) {
				for (String groupTag: mPolygon.getGroupTags(objectTag)) {
					// We need the front face group name, not sides
					if (groupTag.endsWith("_sivu") == true) {
						continue;
					}
					String key = groupTag.substring(4);
					key = key.substring(0, key.indexOf("_"));
					mKeyGroupMap.put(key, groupTag);
				}
			}
		}
		

		
		mDisplayClickListener = new TouchDisplayClickListener();
		mDisplayClickListener.setClickFilterTagEnabledAll(false);
		mDisplayClickListener.setClickFilterTagEnabled(0, true);
		mDisplayClickListener.bindClickable(null, new TouchDisplayClickListener.Listener() {
			@Override
			public void onDisplayClick(DisplayClickable clickable, Vec2F displayCoords) {
				mView.sendInputDisplayClick(1, ClickType.SINGLE, OrionContext.getDisplayCenter());
			}
			public void onDisplayDoubleClick(DisplayClickable clickable, Vec2F displayCoords) { }
			public void onDisplayLongClick(DisplayClickable clickable, Vec2F displayCoords) { }
			
		});
		mScene.bindController(mDisplayClickListener);
		
		mWorldClickListener = new TouchWorldClickListener();
		mWorldClickListener.setClickFilterTagEnabledAll(false);
		mWorldClickListener.setClickFilterTagEnabled(1, true);
		mWorldClickListener.bindClickable(mPolygon, this);
		mScene.bindController(mWorldClickListener);
		
		mView = (OrionView)findViewById(R.id.orion_view);
		mView.bindDefaultCamera(mDefaultCamera);
		mView.bindDefaultScene(mScene);
		mView.bindViewports(OrionViewport.VIEWPORT_CONFIG_FULL, OrionViewport.CoordinateType.FIXED_LANDSCAPE);
		
		// Disable double & long click types. Especially disabling double clicks improves response time.
		mView.setTouchInputClickEnabled(ClickType.DOUBLE, false);
		mView.setTouchInputClickEnabled(ClickType.LONG, false);
		
		mTimer = new Timer();
	}
	
	@Override
	public void onDestroy() {
		mTimer.cancel();
		
		super.onDestroy();
	}

	@Override
	public void onWorldClick(WorldClickable clickable, Vec2F displayCoords, final TouchWorldClickListener.RayCast rayCast) {
		// Ignore clicks on the text area
		if (mTextAreaTag != null && rayCast.modelTag.equals(mTextAreaTag) == true) {
			return;
		}
		if (rayCast.modelTag != null) {
			// Don't allow pressing the buttons again until they come up again
			synchronized (mPressedButtons) {
				if (mPressedButtons.contains(rayCast.modelTag) == true) {
					return;
				}
				// Mark the button as pressed down
				mPressedButtons.add(rayCast.modelTag);
			}
			// Play the default click sound effect, if it's enabled by the user
			mView.getHandler().post(new Runnable() {
				public void run() {
					mView.playSoundEffect(SoundEffectConstants.CLICK);
				}
			});
			// Translate the button back a little bit
			mPolygon.setObjectTransform(rayCast.modelTag, true, Matrix44f.fromTranslate(0, 0, -0.025f));
			// Set the button to return to normal position after a delay
			mTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					synchronized (mPressedButtons) {
						if (mPressedButtons.remove(rayCast.modelTag) == false) {
							return;
						}
					}
					mPolygon.resetObjectTransform(rayCast.modelTag, true);
				}
			}, 100);
			// Edit the text on the display
			if (updateText(rayCast.modelTag) == true) {
				mTextItem.setText(mText);
			}
		}
	}

	@Override
	public void onWorldDoubleClick(WorldClickable clickable, Vec2F displayCoords, TouchWorldClickListener.RayCast rayCast) {
	}

	@Override
	public void onWorldLongClick(WorldClickable clickable, Vec2F displayCoords, TouchWorldClickListener.RayCast rayCast) {
	}

	private boolean updateText(String keyObjectTag) {
		if (keyObjectTag.startsWith("cancel")) {
			Logger.logD(TAG, "CANCEL PRESSED");
			return false;
		} else if (keyObjectTag.startsWith("done")) {
			Logger.logD(TAG, "DONE PRESSED");
			return false;
		}
		
		String [] parts = keyObjectTag.split("_");
		if (parts.length < 3) {
			// At least two "_":s please
			return false;
		}
		String tag = parts[1];
		
		Map<String, KeyMapping> keymap = getCurrentKeyMapping();
		KeyMapping mapping = keymap.get(tag);
		if (mapping == null || mapping.function.length() == 0) {
			return false;
		}
		
		if (mapping.function.startsWith("$") && mapping.function.length() > 1) {
			// Special function
			if (mapping.function.equals("$enter")) {
				Logger.logD(TAG, "ENTER PRESSED");
				return false;
			} else if (mapping.function.equals("$backspace")) {
				if (mText.length() > 0) {
					mText = mText.substring(0, mText.length()-1);
					return true;
				} else {
					return false;
				}
			} else if (mapping.function.equals("$toggle_special")) {
				mSpecialKeysEnabled = !mSpecialKeysEnabled;
				updateKeyTextures();
				return false;
			} else if (mapping.function.equals("$toggle_case")) {
				mUpperCaseEnabled = !mUpperCaseEnabled;
				updateKeyTextures();
				return false;
			} else if (mapping.function.equals("$shift")) {
				Logger.logD(TAG, "SHIFT PRESSED");
				return false;
			} else {
				Logger.logW(TAG, "Unknown key function: " + mapping.function);
				return false;
			}
		} else {
			// Non-special function. Adds characters to the input string.
			mText = mText + mapping.function;
			return true;
		}
	}
	
	private void updateKeyTextures() {
		Map<String, KeyMapping> keymap = getCurrentKeyMapping();
		for (Map.Entry<String,String> entry: mKeyGroupMap.entrySet()) {
			KeyMapping mapping = keymap.get(entry.getKey());
			if (mapping == null) {
				continue;
			}
			mPolygon.setGroupMaterial("key_*", entry.getValue(), mapping.material);
		}
	}
	
	private static final Map<String,KeyMapping> createKeyMapping() {
		Map<String, KeyMapping> keymap = new HashMap<String,KeyMapping>();
		keymap.put("0", 	new KeyMapping("0", 			"0"));
		keymap.put("1", 	new KeyMapping("1", 			"1"));
		keymap.put("2", 	new KeyMapping("2", 			"2"));
		keymap.put("3", 	new KeyMapping("3", 			"3"));
		keymap.put("4", 	new KeyMapping("4", 			"4"));
		keymap.put("5", 	new KeyMapping("5", 			"5"));
		keymap.put("6", 	new KeyMapping("6", 			"6"));
		keymap.put("7", 	new KeyMapping("7", 			"7"));
		keymap.put("8", 	new KeyMapping("8", 			"8"));
		keymap.put("9", 	new KeyMapping("9", 			"9"));
		keymap.put("del", 	new KeyMapping("del",				"$backspace"));
		keymap.put("togglecase", new KeyMapping("lower_toggle",	"$toggle_case"));
		keymap.put("special", 	new KeyMapping("special",		"$toggle_special"));
		keymap.put("space", 	new KeyMapping("space",		" "));
		keymap.put("dot", 		new KeyMapping("dot",		"."));
		keymap.put("enter", 	new KeyMapping("enter",				"$enter"));
		return keymap;
	}
	
	private Map<String,KeyMapping> getCurrentKeyMapping() {
		Map<String, KeyMapping> keymap;
		if (mSpecialKeysEnabled) {
			keymap = mSpecialKeyMap;
		} else if (mUpperCaseEnabled) {
			keymap = mUpperCaseKeyMap;
		} else {
			keymap = mLowerCaseKeyMap;
		}
		return keymap;
	}
	
}
