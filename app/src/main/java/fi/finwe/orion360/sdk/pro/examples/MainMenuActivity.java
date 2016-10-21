package fi.finwe.orion360.sdk.pro.examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import fi.finwe.log.Logger;
import fi.finwe.orion360.v3.Hello.R;

public class MainMenuActivity extends ListActivity {
	// Logging tag, ready to be copy-pasted into any other class.
	public static final String TAG = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	private static class ActivityData extends HashMap<String, String> {
		private static final long serialVersionUID = 1L;
	}
	
	private SimpleAdapter 		mAdapter;
	private List<ActivityData>	mActivityDataList = new ArrayList<ActivityData>();
	
	public MainMenuActivity() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_mainmenu);
		
		PackageInfo packageInfo = getPackageManager().getPackageArchiveInfo(getPackageCodePath(), PackageManager.GET_ACTIVITIES);
		for (ActivityInfo activityInfo : packageInfo.activities) {
			if (activityInfo.name.equals(this.getClass().getName())) {
				continue;
			}
			Logger.logI(TAG, "Found activity: " + activityInfo.name);
			ActivityData activityData = new ActivityData();
			String [] nameParts = activityInfo.name.split("\\.");
			String activityName = nameParts[nameParts.length-1];
			String name = activityName;
			if (name.endsWith("Activity") == true) {
				name = name.substring(0, name.length() - "Activity".length());
			}
			activityData.put("ACTIVITY_NAME", name);
			activityData.put("ACTIVITY_PACKAGE", activityInfo.packageName);
			activityData.put("ACTIVITY_FULLNAME", activityInfo.name);
			
			mActivityDataList.add(activityData);
		}
		
		String [] rowNames = new String[] {
			"ACTIVITY_NAME"
		};
		
		int [] cellResIds = new int [] {
			R.id.textview_activity_name
		};
		
		mAdapter = new SimpleAdapter(this, mActivityDataList, R.layout.layout_mainmenu_row, rowNames, cellResIds);
        setListAdapter(mAdapter);

	}
	
	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		ActivityData activityData = (ActivityData)listView.getItemAtPosition(position);
		try {
			Intent intent = new Intent(this, Class.forName(activityData.get("ACTIVITY_FULLNAME")));
			startActivity(intent);
		} catch (ClassNotFoundException e) {
			Logger.logE(TAG, Log.getStackTraceString(e));
		}
	}
	
	static {
		System.loadLibrary("helloorion-v3");
		System.loadLibrary("orion360");
	}
}
