package com.abh80.smartedge;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;


public class MainActivity extends Activity {

	public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE= 5469;
	public static DisplayMetrics display_metrics;
	public static Bitmap zoomed_screenshot_bitmap;
	public static Boolean zoomed_in = false;
	public static int start_x = 0;
	public static int start_y = 0;

	private EditText editText=null;

	@TargetApi(23)
	public void testOverlayPermission() {
		if (!Settings.canDrawOverlays(this)) {
			Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
					Uri.parse("package:" + getPackageName()));
			startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
		}
	}

	@TargetApi(23)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
			if (Settings.canDrawOverlays(this)) {
				System.out.println("WowICan");
			}
		} else {
			System.out.println("Intent");
			display_metrics = new DisplayMetrics();

			final Context context = this;
			final View upper_view = new View(context);
			final View bottom_view = new View(context);

		}
	}

	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
			"android.permission.READ_EXTERNAL_STORAGE",
			"android.permission.WRITE_EXTERNAL_STORAGE" };


	public static void verifyStoragePermissions(Activity activity) {

		try {
			//检测是否有写的权限
			int permission = ActivityCompat.checkSelfPermission(activity,
					"android.permission.WRITE_EXTERNAL_STORAGE");
			if (permission != PackageManager.PERMISSION_GRANTED) {
				// 没有写的权限，去申请写的权限，会弹出对话框
				ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
			}
		} catch (Exception e) {
			e.printStackTrace();
			//CrashReport.postCatchedException(e);
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		verifyStoragePermissions(this);
		// SophixManager.getInstance().queryAndLoadNewPatch();
		setContentView(R.layout.activity_main);

		if (Build.VERSION.SDK_INT >= 23){
			testOverlayPermission();
		}

		this.findViewById(R.id.set).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Utility.IPAddress = editText.getText().toString();
				setSharedPreferenceData("username",Utility.USER);
			}
		});

		this.findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
			}
		});
		editText = (EditText) this.findViewById(R.id.username);
		String dir;
		File externalFilesDir = getExternalFilesDir(null);
		if (externalFilesDir != null)
		{
			dir = externalFilesDir.getAbsolutePath() + "/screenshots/";
			((TextView) this.findViewById(R.id.path)).setText(dir);
			if(getSharedPreferenceData("username")!=null)
				Utility.setUsername(getSharedPreferenceData("username"));
			else
				Utility.setUsername("user"+System.currentTimeMillis());
			((EditText) this.findViewById(R.id.username)).setText(Utility.IPAddress);

		}
		Interaction.init(this, this.getWindow().getDecorView());
	}

    @Override
    protected void onResume() {
		super.onResume();
		this.findViewById(R.id.set).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Utility.IPAddress = editText.getText().toString();
				setSharedPreferenceData("username",Utility.USER);
			}
		});

		this.findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				TCPClientController.sharedCenter().setConnectedCallback(new TCPClientController.OnServerConnectedCallbackBlock() {
					@Override
					public void callback() {
						Log.e("nb","connected with server");
					}
				});
				TCPClientController.sharedCenter().setDisconnectedCallback(new TCPClientController.OnServerDisconnectedCallbackBlock() {
					@Override
					public void callback(IOException e) {
						Log.e("nb","disconnected");
					}
				});
				TCPClientController.sharedCenter().setReceivedCallback(new TCPClientController.OnReceiveCallbackBlock() {
					@Override
					public void callback(String receivedMessage) {
						try {
							Gson gson = new Gson();
							JsonObject interactionJson = gson.fromJson(receivedMessage, JsonObject.class);
							if (interactionJson.has("action_type")) {
								Interaction.performInteraction(interactionJson);
							}
						} catch (Exception e) {
						}
						Log.e("nb","got message: "+receivedMessage);
					}
				});
				Log.e("start","start connection");
				TCPClientController.sharedCenter().connect(Utility.IPAddress,5000);
				startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
			}
		});

		String dir;
		File externalFilesDir = getExternalFilesDir(null);
		if (externalFilesDir != null)
		{
			dir = externalFilesDir.getAbsolutePath() + "/screenshots/";
			((TextView) this.findViewById(R.id.path)).setText(dir);
			if(getSharedPreferenceData("username")!=null)
				Utility.setUsername(getSharedPreferenceData("username"));
			else
				Utility.setUsername("user"+System.currentTimeMillis());
			((EditText) this.findViewById(R.id.username)).setText(Utility.IPAddress);
		}
	}

	public void setSharedPreferenceData(String Name, String dataStr) {
		SharedPreferences sharedPref = getSharedPreferences("Data", MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(Name, dataStr);
		editor.apply();
	}

	public String getSharedPreferenceData(String Name) {
		SharedPreferences sharedPref = getSharedPreferences("Data", MODE_PRIVATE);
		return sharedPref.getString(Name, null);
	}

}
