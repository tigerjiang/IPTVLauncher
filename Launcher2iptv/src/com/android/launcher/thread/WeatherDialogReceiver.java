package com.android.launcher.thread;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WeatherDialogReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if(intent.getAction().equals("android.intent.action.weathercity")){
			Intent serviceIntent = new Intent(context, WeatherDialogService.class);
			context.startService(serviceIntent);
		}
	}

}
