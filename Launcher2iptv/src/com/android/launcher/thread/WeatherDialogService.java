package com.android.launcher.thread;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import com.android.launcher.R;

public class WeatherDialogService extends Service{
	private final static String TAG="WeatherDialogService";
		
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		showConfigDialog();
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	private void showConfigDialog(){
		Log.d(TAG, "showConfigDialog");
    	final EditText text = new EditText(getApplicationContext());
    	AlertDialog.Builder builder = new Builder(this);
    	
    	final SharedPreferences sharedPreferences = getSharedPreferences("cityweather",getApplicationContext().MODE_PRIVATE);
    	final String cityString = sharedPreferences.getString("city", getString(R.string.default_city));
    	    	
    	builder.setTitle(R.string.city_config).setView(text);
    	String cityTip = getString(R.string.current_city) + cityString;
    	cityTip += getString(R.string.change_city);
    	builder.setMessage(cityTip);
    	builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				
				SharedPreferences.Editor editor = sharedPreferences.edit();
				String inputString = text.getText().toString();
				if((inputString.equals("")) || (inputString.equals(" "))){
					editor.putString("city", cityString);
				}else {
					editor.putString("city", text.getText().toString());
				}
				
				editor.commit();
			}
		});
    	
    	builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
		});
    	
    	Dialog dialog = builder.create();
    	dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
    	dialog.show();
    }
}
