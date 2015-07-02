package com.android.launcher.thread;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.android.launcher.home.Home;
import com.android.launcher.utils.Utility;
import com.android.launcher.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;


public class UpdateStatusThread implements Runnable {

	private String mCurrentTime;
	private String mCurrentDate;
	private String mLastDate = "";

	private static final String TAG = "UpdateStatusThread";
	private int mCurrentNetStatus;
	private int oldLevel = 0;
	private int curentLevel = 0;
	private int mLastNetStatus = Utility.NetworkStatus.NET_STATUS_DISCONNECT;
	private Context mContext;
	private Home mHomeActivity;

	private TextView mTime;
	private TextView mDate;
	private ImageView mNetstatusView;
	
	private TextView mCityTextView;
	private TextView mWeatherTextView;
	private TextView mTemptureTextView;

	private String mCity;
	private String mWeatherDetail;
	private String mTempture;
	
	private boolean mFlag = true;
	private WifiManager mWifiManager;
	
	private Timer timer = new Timer();
	private boolean startNtpFlag = false;

	public UpdateStatusThread(Context mContext) {
		this.mContext = mContext;
		this.mHomeActivity = (Home) mContext;
		initView();
	}
	
	private Handler cityChangeHandler = new Handler(){
		public void handleMessage(Message msg){
			switch(msg.what){
			case 0x11:
				mCityTextView.setText(mCity);
				mWeatherTextView.setText(mWeatherDetail);
				mTemptureTextView.setText(mTempture);
				break;
			}
		}
	
	};

	private void initView() {
		
		mTime = (TextView) mHomeActivity.findViewById(R.id.time);
		mDate = (TextView) mHomeActivity.findViewById(R.id.data);
		mNetstatusView = (ImageView) mHomeActivity.findViewById(R.id.wifi);
		mWifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		
		mCityTextView = (TextView)mHomeActivity.findViewById(R.id.city);
		mCityTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
		mCityTextView.setMaxLines(1);
		mTemptureTextView = (TextView)mHomeActivity.findViewById(R.id.city_tempture);
		mWeatherTextView = (TextView)mHomeActivity.findViewById(R.id.weatherdetail);
	}

	boolean flag1 = true;

	private String getUserCity(){
//		SharedPreferences sharedPreferences = mContext.getSharedPreferences("cityweather",Context.MODE_PRIVATE);
//    	String cityString = sharedPreferences.getString("city", null);
		String cityString = Settings.System.getString(mContext.getContentResolver(), "city"); 
    	return cityString;
	}
	
	public void run() {
		while (mFlag) {

			Log.d(TAG, "update time and weather");
			SimpleDateFormat time;
			time = new SimpleDateFormat("kk : mm");
			mCurrentTime = time.format(new Date());
			
			Calendar c = Calendar.getInstance();
			
		    int dayIndex = c.get(Calendar.DAY_OF_WEEK);  
		    if (dayIndex >= 1 && dayIndex <= 7) {  
		    	String week;
		    	if(dayIndex == 1){
		    		week = mHomeActivity.getString(R.string.SUNDAY);
		    	}
		    	else if(dayIndex == 2){
		    		week = mHomeActivity.getString(R.string.MONDAY);
		    	}
		    	else if(dayIndex == 3){
		    		week = mHomeActivity.getString(R.string.TUESDAY);
		    	}
		    	else if(dayIndex == 4){
		    		week = mHomeActivity.getString(R.string.WEDNESDAY);
		    	}
		    	else if(dayIndex == 5){
		    		week = mHomeActivity.getString(R.string.THRUSDAY);
		    	}
		    	else if(dayIndex == 6){
		    		week = mHomeActivity.getString(R.string.FRIDAY);
		    	}else
		    		week = mHomeActivity.getString(R.string.SATURDAY);
		    	mCurrentTime = mCurrentTime + " " + week;
		    	TimeHandler.post(mTimeUpdateResults);
		    }

		    String userCityString = getUserCity();
	    	if((userCityString == null)){
	    		Log.d(TAG, "#### set city by ip ####");
				String localIPString = getCityIP();
				String cityString = getCityByIp(localIPString);
				mCity = cityString;
				Settings.System.putString(mContext.getContentResolver(),
	        			"city", mCity); 
				getWeatherByCity(cityString);
				
//				SharedPreferences sharedPreferences =mContext.getSharedPreferences("cityweather",Context.MODE_PRIVATE);
//				SharedPreferences.Editor editor = sharedPreferences.edit();
//				editor.putString("city", mCity);
//				editor.commit();
	    	}else 
	    	{
				searchWeather(userCityString);
			}
	    	
			Message msg = cityChangeHandler.obtainMessage();
			msg.what = 0x11;
			cityChangeHandler.sendMessage(msg);

			SimpleDateFormat data;
			String yead = mHomeActivity.getString(R.string.year);
			String month = mHomeActivity.getString(R.string.month);
			String day = mHomeActivity.getString(R.string.day);
			String format = "yyyy" + yead + "MM" + month + "dd" + day;
			data = new SimpleDateFormat(format);
			mCurrentDate = data.format(new Date());

			if (!mCurrentDate.equals(mLastDate)) {
				updateStatus(0);
				mLastDate = mCurrentDate;
			}
			mCurrentNetStatus = getNetStatus();
			if (mCurrentNetStatus == Utility.NetworkStatus.NET_STATUS_WIFI) {
				curentLevel = getWiFiSignalLevel();
			}
			
			if (mCurrentNetStatus != mLastNetStatus || curentLevel != oldLevel) {
//				if(mCurrentNetStatus != mLastNetStatus && ((Home)mContext).isNetConfigSuccess() && mCurrentNetStatus == CommonValues.HiNetStatus.NET_STATUS_DISCONNECT){
//					updateStatus(2);
//				}
				if(mCurrentNetStatus != mLastNetStatus ){
					if( mCurrentNetStatus == Utility.NetworkStatus.NET_STATUS_DISCONNECT){
						Intent intent = new Intent();
						intent.setAction(Utility.NETWORK_FAULT);
						mContext.sendBroadcast(intent);
					}else if(mLastNetStatus == Utility.NetworkStatus.NET_STATUS_DISCONNECT){
						Intent intent = new Intent();
						intent.setAction(Utility.NETWORK_SUCCESS);
						mContext.sendBroadcast(intent);
					}
				}
				oldLevel = curentLevel;
				mLastNetStatus = mCurrentNetStatus;
				updateStatus(1);
			}
            int year = c.get(Calendar.YEAR);
            if(year != 1970)
            	updateStatus(3);
            else{
            	if(year == 1970 && mCurrentNetStatus != Utility.NetworkStatus.NET_STATUS_DISCONNECT){
            		
            		Intent intent = new Intent();
            		intent.setAction(Utility.NETWORK_CONNECT_SUCCESS);
            		mHomeActivity.sendBroadcast(intent);
            	}
            }
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public void setFlag(boolean flag) {
		mFlag = flag;
	}

	public boolean getFlag() {
		return mFlag;
	}

	Handler TimeHandler = new Handler();
	Runnable mTimeUpdateResults = new Runnable() {
		public void run() {
			mTime.setText(mCurrentTime);
		}
	};
	
	Handler WeatherHandler = new Handler();
	Runnable mWeatherResults = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if ((mCity != null) && (mWeatherDetail != null) && (mTempture != null)) {
				mCityTextView.setText(mCity);
				mWeatherTextView.setText(mWeatherDetail);
				mTemptureTextView.setText(mTempture);
			}
		}
	};

	private void updateStatus(int update) {
		int msgId = update;
		Message msgMessage = new Message();
		msgMessage.arg1 = msgId;
		StatusHandler.sendMessage(msgMessage);

	}

	static int flag = 0;
	Handler StatusHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case 0:
				mDate.setText(mCurrentDate);
				break;
			case 1:
				if (mCurrentNetStatus == Utility.NetworkStatus.NET_STATUS_DISCONNECT) {
					mNetstatusView.setImageLevel(6);
					
				} else if (mCurrentNetStatus == Utility.NetworkStatus.NET_STATUS_ETH) {
					mNetstatusView.setImageLevel(5);
				} else if (mCurrentNetStatus == Utility.NetworkStatus.NET_STATUS_WIFI) {
					mNetstatusView.setImageLevel(curentLevel + 1);
				}
				break;
			case 2:
				
//				startSettingApp();
//				sartSetNetWork();
				break;
//			case 3:
//				break;

			}
		}
	};
	
	private void sartSetNetWork(){
		TimerTask task = new TimerTask() {
			final Handler handler = new Handler(){
				public void handleMessage(Message msg){
					switch(msg.what){
					case 0x66:
						if (mCurrentNetStatus == Utility.NetworkStatus.NET_STATUS_DISCONNECT){
							SendBroadCastToCheckNet();
						}
						break;
					}
				}
			};
			@Override
			public void run() {
				Message message = Message.obtain();
				message.what = 0x66;
				handler.sendMessage(message);
			}
		};
		
		timer.cancel();
		timer = new Timer(true);
		timer.schedule(task, 60000 * 10);
	}

	private void SendBroadCastToCheckNet(){
//		Log.i(TAG,"SendBroadCastToCheckNet");
//		((Home)mContext).startNetCheckService();
	}

	private int getNetStatus() {
		int status = Utility.NetworkStatus.NET_STATUS_DISCONNECT;
		ConnectivityManager connectivity = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState().equals(NetworkInfo.State.CONNECTED)) {

						if (info[i].getTypeName().equals("WIFI") && checkWifi()) {
							// Log.i(TAG,"****************************getWiFiSignalLevel()="+getWiFiSignalLevel());
							status = Utility.NetworkStatus.NET_STATUS_WIFI;
						} else {
							status = Utility.NetworkStatus.NET_STATUS_ETH;
						}
						return status;
					}
				}
			}
		}
		return status;
	}

	private boolean checkWifi() {
		WifiManager wifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		int state = wifiManager.getWifiState();
		if (state == WifiManager.WIFI_STATE_DISABLED) {
			return false;
		} else if (state == WifiManager.WIFI_STATE_ENABLED) {
			return true;
		} else {
			return false;
		}
	}

	private int getWiFiSignalLevel() {
		int strength = 0;
		WifiInfo info = mWifiManager.getConnectionInfo();
		if (info.getBSSID() != null) {
			strength = WifiManager.calculateSignalLevel(info.getRssi(), 4);
			if (strength == 0)
				strength = WifiManager.calculateSignalLevel(
						getSignalByBssid(info.getBSSID()), 4);
			return strength;
		}
		return strength;
	}

	private int getSignalByBssid(String Bssid) {
		final List<ScanResult> results = mWifiManager.getScanResults();
		if (results != null) {
			for (ScanResult result : results) {

				if (Bssid.equals(result.BSSID))
					return result.level;
			}
		}
		return 0;
	}
	
	public void setLastNetStatus(int status){
		mLastNetStatus = status;
	}
	
	public String getCityIP() {
		URL url;
		URLConnection conn = null;
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		String str = "";
		String localIPString = null;
		try {
			url = new URL("http://iframe.ip138.com/ic.asp");
			conn = url.openConnection();
			is = conn.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String input = "";
			org.jsoup.nodes.Document doc;  
			while ((input = br.readLine()) != null) {
				str += input;
			}
			doc = Jsoup.parse(str);  

			String ip1 = doc.body().toString();
			int start = ip1.indexOf("[");  
            int end = ip1.indexOf("]");  
            
			localIPString = ip1.substring(start+1, end);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return localIPString;

	}
    
    public String getCityByIp(String ipString) {
    	String cityString = null;
		try {
			URL url = new URL("http://whois.pconline.com.cn/ip.jsp?ip=" + ipString);
			HttpURLConnection connect = (HttpURLConnection) url
					.openConnection();
			InputStream is = connect.getInputStream();
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			byte[] buff = new byte[256];
			int rc = 0;
			while ((rc = is.read(buff, 0, 256)) > 0) {
				outStream.write(buff, 0, rc);
				
			}

			byte[] b = outStream.toByteArray();
			
			//关闭
			outStream.close();
			is.close();
			connect.disconnect();
			String address = new String(b,"GBK");
			
			if (address.startsWith("北")||address.startsWith("上")||address.startsWith("重")){
				cityString = (address.substring(0,address.indexOf("市")));
			}
			if(address.startsWith("香")){
				cityString = (address.substring(0,address.indexOf("港")));
			}
			if(address.startsWith("澳")){
				cityString = (address.substring(0,address.indexOf("门")));
			}
			if (address.indexOf("省") != -1) {
				cityString = (address.substring(address.indexOf("省") + 1, address.indexOf("市")));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cityString;
	}

public int getWeatherByCity(String cityString) {
		int r = 0;
		String today_templow = null, today_temphigh = null;
		String today_conditon = null;
		InputStream is = null;
		HttpURLConnection connection = null;
		
		try {
			// today forecast
			URL url = new URL("http://php.weather.sina.com.cn/xml.php?city="
					+ URLEncoder.encode(cityString, "gb2312")
					+ "&password=DJOYnieT8234jlsK&day=0");
			connection = (HttpURLConnection) url
					.openConnection();
			connection.connect();
			is = connection.getInputStream();

			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dombuilder = factory.newDocumentBuilder();
			Document doc = null;
			doc = dombuilder.parse(is);
			Element element = doc.getDocumentElement();

			NodeList Profiles = element.getChildNodes();

			if ((Profiles != null) && Profiles.getLength() > 1) {
				for (int i = 0; i < Profiles.getLength(); i++) {
					Node weather = Profiles.item(i);
					if (weather.getNodeType() == Node.ELEMENT_NODE) {
						for (Node node = weather.getFirstChild(); node != null; node = node
								.getNextSibling()) {
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								if (node.getNodeName().equals("figure1")) {
									today_conditon = node.getFirstChild()
											.getNodeValue();
									String chString = changeWeatherToChinese(today_conditon);
									mWeatherDetail = chString;
								}
								if (node.getNodeName().equals("temperature1")) {
									today_temphigh = node.getFirstChild()
											.getNodeValue();
								}
								if (node.getNodeName().equals("temperature2")) {
									today_templow = node.getFirstChild()
											.getNodeValue();
								}

								if (today_conditon != null
										&& today_temphigh != null && today_templow != null){
									//TODO
									mTempture = today_templow+mHomeActivity.getString(R.string.temp_degree)+"~"
									   +today_temphigh+ mHomeActivity.getString(R.string.temp_degree) ;
						        	break;
								}else{
								}
							}
						}
					}
				}
			}else{
			}

		} catch (Exception e) {
			r = -1;
			e.printStackTrace();
		} finally {
			try {
				if (connection != null){
					connection.disconnect();
				}
				if (is != null)
					is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return r;
	}

	public String changeWeatherToChinese(String engString){
		if(engString.equals("qing"))
			return mHomeActivity.getString(R.string.qing);
		if(engString.equals("duoyun")){
			return mHomeActivity.getString(R.string.duoyun);
		}
		if (engString.equals("dafeng"))
			return mHomeActivity.getString(R.string.dafeng);
		if(engString.equals("ying")){
			return mHomeActivity.getString(R.string.ying);
		}	
		if(engString.equals("zhenyu")){
			return mHomeActivity.getString(R.string.zhenyu);
		}
		if(engString.equals("leizhenyu")){
			return mHomeActivity.getString(R.string.leizhenyu);
		}
		if(engString.equals("binbao_leizhenyu")){
			return mHomeActivity.getString(R.string.binbao_leizhenyu);
		}
		if(engString.equals("yujiaxue")){
			return mHomeActivity.getString(R.string.yujiaxue);
		}
		if(engString.equals("xiaoyu")){
			return mHomeActivity.getString(R.string.xiaoyu);
		}
		if(engString.equals("zhongyu")){
			return mHomeActivity.getString(R.string.zhongyu);
		}
		if(engString.equals("dayu") ){
			return mHomeActivity.getString(R.string.dayu);
		}
		if( engString.equals("dongyu")){
			return mHomeActivity.getString(R.string.dongyu);
		}
		if(engString.equals("baoyu") ){
			return mHomeActivity.getString(R.string.baoyu);
		}
		if(engString.equals("dabaoyu")){
			return mHomeActivity.getString(R.string.dabaoyu);
		}
		if(engString.equals("te_dabaoyu")){
			return mHomeActivity.getString(R.string.te_dabaoyu);
		}
		if(engString.equals("xiaoxue")){
			return mHomeActivity.getString(R.string.xiaoxue);
		}
		if(engString.equals("zhongxue")){
			return mHomeActivity.getString(R.string.zhongxue);
		}
		if(engString.equals("daxue") ){
			return mHomeActivity.getString(R.string.daxue);
		}
		if(engString.equals("baoxue") ){
			return mHomeActivity.getString(R.string.baoxue);
		}
		if(engString.equals("shachengbao")  ){
			return mHomeActivity.getString(R.string.shachengbao);
		}
		if(engString.equals("qiang_shachengbao")){
			return mHomeActivity.getString(R.string.qiang_shachengbao);
		}
		if(engString.equals("wu") ){
			return mHomeActivity.getString(R.string.wu);
		}
		if(engString.equals("fuchen") ){
			return mHomeActivity.getString(R.string.fuchen);
		}
		if( engString.equals("yangsha")){
			return mHomeActivity.getString(R.string.yangsha);
		}
		if(engString.equals("mai")){
			return mHomeActivity.getString(R.string.mai);
		}
		if(engString.equals("zhenxue")){
			return mHomeActivity.getString(R.string.zhenxue);
		}
		
		return "";
	}
	
	private String filterSuffix(String cityName){
    	if(cityName.endsWith(mHomeActivity.getString(R.string.string_shi))){
    		if(cityName.subSequence(0, cityName.length()-1).equals(
    				mHomeActivity.getString(R.string.string_sha))){
    			return cityName; //沙市
    		}else {
				return cityName.substring(0,cityName.length()-1);
			}
    	}else if(cityName.endsWith(mHomeActivity.getString(R.string.string_sheng))){
    		return cityName.substring(0, cityName.length()-1);
    	}else if(cityName.endsWith(mHomeActivity.getString(R.string.string_qu))){
    		return cityName.substring(0,cityName.length()-1);
    	}else if(cityName.endsWith(mHomeActivity.getString(R.string.string_xian))){
    		if(cityName.length() == 2){
    			return cityName;
    		}else {
				return cityName.substring(0,cityName.length()-1);
			}
    	}else {
			return cityName;
		}
    }
    
    private void searchWeather(String cityString){
    	String weatherCity = null;
    	String today_templow = null, today_temphigh = null;
		String today_conditon = null;
    	HttpURLConnection connection = null;
    	InputStream isInputStream = null;
    	int r = 0;
    	try {
			weatherCity = filterSuffix(cityString);
			mCity = weatherCity;
			
			if(mCity == ""){
				mWeatherDetail="";
				mTempture="";
				return;
			}
			
			URL url = new URL("http://php.weather.sina.com.cn/xml.php?city="
					+ URLEncoder.encode(weatherCity, "gb2312")
					+ "&password=DJOYnieT8234jlsK&day=0");
			Log.d(TAG, "weather search: " + url.toString());
			connection = (HttpURLConnection)url.openConnection();
			connection.connect();
			isInputStream = connection.getInputStream();
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder domBuilder = factory.newDocumentBuilder();
			Document doc = null;
			doc = domBuilder.parse(isInputStream);
			org.w3c.dom.Element element = doc.getDocumentElement();
			
			NodeList profilesList = element.getChildNodes();
			if((profilesList != null) && (profilesList.getLength() > 1)){

				for (int i = 0; i < profilesList.getLength(); i++) {
					Node weather = profilesList.item(i);
					
					if(weather.getNodeType() == Node.ELEMENT_NODE){
						for (Node node = weather.getFirstChild(); node != null; node = node
						.getNextSibling()) 
						{
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								if (node.getNodeName().equals("figure1")) {
									today_conditon = node.getFirstChild()
											.getNodeValue();
									String chString = changeWeatherToChinese(today_conditon);
									mWeatherDetail = chString;
								}
								if (node.getNodeName().equals("temperature1")) {
									today_temphigh = node.getFirstChild()
											.getNodeValue();
								}
								if (node.getNodeName().equals("temperature2")) {
									today_templow = node.getFirstChild()
											.getNodeValue();
								}

								if (today_conditon != null
										&& today_temphigh != null && today_templow != null){
									//TODO
									mTempture = today_templow+mHomeActivity.getString(R.string.temp_degree)+"~"
									   +today_temphigh+ mHomeActivity.getString(R.string.temp_degree) ;
						        	break;
								}else{
								}
							}
						}
					}
				}
			}else { 
				mWeatherDetail="";
				mTempture = mHomeActivity.getString(R.string.city_weather_error);
			}
		} catch (Exception e) {
			// TODO: handle exception
			r = -1;
			mWeatherDetail="";
			mTempture = mHomeActivity.getString(R.string.city_weather_error);
			e.printStackTrace();
		}
		
		finally{
			try {
				if(connection != null){
					connection.disconnect();
				}
				if(isInputStream != null){
					isInputStream.close();
				}
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
    }


}


