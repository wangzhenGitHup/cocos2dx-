/****************************************************************************
Copyright (c) 2010-2012 cocos2d-x.org

http://www.cocos2d-x.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
****************************************************************************/
package com.youxigu.czxz;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.cocos2dx.lib.Cocos2dxActivity;
import org.cocos2dx.lib.Cocos2dxHelper;

import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.mid.api.MidCallback;
import com.tencent.mid.api.MidService;
import com.tendcloud.tenddata.TalkingDataGA;
import com.youxigu.czxz.GameService.GameBinder;
import com.youxigu.czxz.R;

//import cn.uc.gamesdk.UCGameSDKStatusCode;
//import cn.uc.gamesdk.jni.UCGameSdk;

import android.app.Activity;
import android.os.Build.VERSION;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;
import android.app.ProgressDialog;
import android.graphics.Bitmap;

public class GameClient extends Cocos2dxActivity{

	static String strVersion;
	public static final String PREFS_NAME = "MyPrefsFile";  
	public static final String PREFS_CONFIG_IS_FIRST_RUN = "isFirstRun";
	public static GameClient sGameClient = null;
	private ProgressBar mProgress = null;
	private TextView mTextView = null;
	private boolean mLogoTimeout = true;
	private String mUpdateResult = null;
	private String mUpdateURL = null;
	
	private int mCpID = 32226;
	private int mGameID = 539027;
	private int mServerID = 2888;
	private String mServerName = "czxu-test";
	
	public static final String sTag = "CZXZ";
	public static final String NOTIFICAION_TAG = "game_notification";
	public static final String NOTIFICAION_ENABLE = "notification_enable";
	
	private boolean mBound = false;
	protected ProgressDialog mProgressDialog = null;
	protected ProgressDialog mWebViewDialog = null;
	
	protected static final int TASK_DOWNLOAD = 1;
	protected static final int TASK_GET_ZIP_LEN = 2;
	protected static final int TASK_CHECK_UPDATE = 3;
	protected String[] mCheckUpdateParams = null;
	protected String[] mDownloadParams = null;
	
	public boolean mNeedInitOnResume = false;
	
	protected MyVideoView mMyVideoView = null;
	
	protected static final int MSG_PLAY_VIDEO = 10;
	protected static final int MSG_STOP_VIDEO = 11;

	protected View gonggaoView;
	protected WebView m_webView;//WebView控件  
	protected Button m_backButton;//关闭按钮  
	
	protected View mUpdateView = null;
	protected View mImageViewBg = null;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		sGameClient = this;
		
		//talkingdata init  getApplicationContext()
		TalkingDataGA.init(this, getMetaData("TDGA_APP_ID"), getMetaData("TDGA_CHANNEL_ID"));
		
		CrashReport.initCrashReport(getApplicationContext());
		CrashReport.setUserId(getIMEI());
		Log.v("8089", "onCreate " + this.hashCode());
		mUpdateURL = getMetaData("UPDATE_URL");
		
		//显示等待圈圈
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setMessage(getResources().getString(R.string.launching));
		//mProgressDialog.show();
		
		//setContentView(R.layout.layout_launching);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		long maxMemory = Runtime.getRuntime().maxMemory();
		cocosLog("maxMemory: " + Long.toString(maxMemory));
		cocosLog("getPackageCodePath: " + this.getPackageCodePath());
		
		//检查网络连接
		if(checkNetwork())
		{
			checkUpdateAndInit();
		}
		else
		{
	        new AlertDialog.Builder(this)
			.setTitle(R.string.app_name)
			.setMessage(R.string.no_network)
			.setCancelable(false)
			.setPositiveButton(R.string.open_wifi, 
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							sGameClient.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
							sGameClient.mNeedInitOnResume = true;
							//Log.v(GameClient.sTag, "open_wifi");
						}
					})
					/*
			.setNeutralButton(R.string.open_3G, 
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							sGameClient.startActivity(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
							sGameClient.mNeedInitOnResume = true;
							//Log.v(GameClient.sTag, "open_3G");
						}
					})
					*/
			.setNegativeButton(R.string.cancel, 
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							sGameClient.checkUpdateAndInit();
						}
					})
			.create().show();
		}
	}
	
	public void checkUpdateAndInit()
	{
		Log.v(sTag, "checkUpdateAndInit................");
		mNeedInitOnResume = false;
		//获取版本更新信息
		String strVersion = getCurrentVersion();
		String strDeviceId = getIMEI();
		try {
			if(strVersion != null) {
				Log.v(sTag, "version=" + strVersion);
				
				SendHttpRequestTask t = new SendHttpRequestTask();
				mCheckUpdateParams = new String[]{getVersionUrl(), strVersion, strDeviceId};
				t.execute(mCheckUpdateParams);
			}
			else {
				//TODO 弹出提示
				Log.v(sTag, "Cannot get version");
				Cocos2dxHelper.terminateProcess();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Cocos2dxHelper.terminateProcess();
		}
		
		//初始化ucsdk
		//initUcSdk();
		initTencentSdk();

		//显示logo
		/*
		new Handler().postDelayed(new Runnable(){   

		    public void run() {   
		    	Log.v(sTag, "getRootDir="+getRootDir().getAbsolutePath());
		    	mLogoTimeout = true;
		    	onDealWithUpdate(mUpdateResult);
		    }   
		 }, 2500);   
		*/
		
		//启动service
		//Intent startIntent = new Intent(this, GameService.class);  
        //startService(startIntent);  
	}
	
	 private ServiceConnection mConnection = new ServiceConnection() {  
		  
	        @Override  
	        public void onServiceDisconnected(ComponentName name) 
	        {  
	        	mBound = false;
	        }  
	  
	        @Override  
	        public void onServiceConnected(ComponentName name, IBinder service) 
	        {  
	        	GameBinder myBinder = (GameService.GameBinder)service;
	        	myBinder.cancelNotification();
	        	mBound = true;
	        }  
	};

	public static void initResultCallback(int iCode) {
		/*
		if(iCode ==  UCGameSDKStatusCode.SUCCESS) {
			//初始化换成功,切换更新界面
			sGameClient.mLogoTimeout = true;
			sGameClient.onDealWithUpdate(sGameClient.mUpdateResult);
		}
		else {
			//初始化失败
			Log.v("CZXZ", "Retry init ucsdk...");
			sGameClient.initUcSdk();
			
		}
		*/
	}
	
	protected void initUcSdk() {
		/*
		UCGameSdk.setCurrentActivity(this);//ucsdk
		UCGameSdk.setLoginUISwitch(1);
		UCGameSdk.initSDK(true, 0, mCpID, mGameID, mServerID, mServerName, true, false);
		*/
	}
	
	//初始化腾讯sdk
	protected void initTencentSdk() {
		TencentSDK.setActivity(this);
		TencentSDK.initSDK();
	}
	
	@Override
	protected void initCocos2dx() {
		Log.v("CZXZ", "initCocos2dx...");
		if(!mInitDone) {
			LoadMyLibrary();
	    	this.init();
			Cocos2dxHelper.init(this, this);
			mInitDone = true;
		}
	}
	
	@Override
	protected void LoadMyLibrary() {
		Log.v("CZXZ", "LoadMyLibrary...");
		File fileDir = new File(this.getFilesDir(), "libgame.so");
		cocosLog("load: " + fileDir.getAbsolutePath());
    	try
    	{
    		System.load(fileDir.getAbsolutePath());
    	}
    	catch(UnsatisfiedLinkError e)
    	{
    		cocosLog(e.toString());
    		System.loadLibrary("game");
    	}
    	
    	strVersion = new String("0");
		
		InputStream is = null;
		File versionFile = new File(this.getFilesDir(), "version");
		if (versionFile.exists()) {
			cocosLog("version file exists");
			try {
				is = new FileInputStream(versionFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			cocosLog("version file not exists");
			
			try {
				is = this.getAssets().open("version");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		try {
			InputStreamReader in = new InputStreamReader(is);
			BufferedReader bufferReader = new BufferedReader(in);
			strVersion = bufferReader.readLine();
			if(strVersion == null) {
				cocosLog("Cannot get version");
			}
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void cocosLog(String str) {
		Log.v(sTag, str);
	}
	
	public static String getClinetVersion() {
		Log.v(sTag, "get client version through jni" + strVersion);
		return strVersion;
	}
	public static String getIMEI()
	{
		String imei = "";
		try
		{
			TelephonyManager tm = (TelephonyManager) (sGameClient.getSystemService(Context.TELEPHONY_SERVICE));     
			imei = tm.getDeviceId();
		}
		catch(Exception e)
		{
			
		}
		return imei;
	}
	public static String getIMSI()
	{
		String imsi = "";
		try
		{
			TelephonyManager tm = (TelephonyManager) (sGameClient.getSystemService(Context.TELEPHONY_SERVICE));
			imsi = tm.getSubscriberId();
		}
		catch( Exception e)
		{
			
		}
		return imsi;
	}
	public static String getTelephoneNum()
	{
		String num = "";
		try
		{
			TelephonyManager tm = (TelephonyManager) (sGameClient.getSystemService(Context.TELEPHONY_SERVICE));
			num = tm.getLine1Number();
		}
		catch(Exception e)
		{
			
		}
		return num;
	}
	public static String getYunyingshang() {
		String ret = "中国移动";
		try {
			TelephonyManager telephonyManager = (TelephonyManager) (sGameClient.getSystemService(Context.TELEPHONY_SERVICE));  
			String IMSI = telephonyManager.getSubscriberId();  
	        // IMSI号前面3位460是国家，紧接着后面2位00 02是中国移动，01是中国联通，03是中国电信。  
	        System.out.println(IMSI);  
	        if (IMSI.startsWith("46000") || IMSI.startsWith("46002")) {  
	        	ret = "中国移动";  
	        } else if (IMSI.startsWith("46001")) {  
	        	ret = "中国联通";  
	        } else if (IMSI.startsWith("46003")) {  
	        	ret = "中国电信";  
	        }  
		}
		catch(Exception e){
			
		}
		
		Log.v(sTag, "getYunyingshang" + ret);
		return ret;
	}
	public static String getWangluomoshi() {
		String str = "wifi";
		try {
	        ConnectivityManager manager = (ConnectivityManager) (sGameClient.getSystemService(Context.CONNECTIVITY_SERVICE));  
	        if(manager == null) {
	        	Log.d(GameClient.sTag, "getSystemService(Context.CONNECTIVITY_SERVICE) is null!!");
	        }
	        else{
	        	State mobileState = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState(); 
		        if(mobileState==State.CONNECTED || mobileState==State.CONNECTING)
		        {
		        	str = "3g";
		        }
		        
		        State wifiState = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();  
		        if(wifiState==State.CONNECTED||wifiState==State.CONNECTING)
		        {
		        	str = "wifi";
		        }
	        } 
	    }
    	catch(Exception e) {
    	}
		
		Log.v(sTag, "getWangluomoshi through jni" + str);
		return str;
	}
	public static String getJixing() {
		String str = "Meizu";
		try {
			str = android.os.Build.MODEL; //设备型号，机型
	    }
    	catch(Exception e) {
    	}
		Log.v(sTag, "getJixing through jni" + str);
		return str;
	}
	public static boolean isFirstRun() {
		// get preference  
		SharedPreferences settings = sGameClient.getSharedPreferences(PREFS_NAME, 0);  
		boolean firstRunValue = settings.getBoolean(PREFS_CONFIG_IS_FIRST_RUN, true); 
		String str = "check isFirstRun No";
		if (firstRunValue)
		{
			str = "check isFirstRun Yes";
		}
		sGameClient.cocosLog(str);
		return firstRunValue;
	}

	public static void markFirstRun(boolean firstRunValue) {
		// set preference  
		SharedPreferences settings = sGameClient.getSharedPreferences(PREFS_NAME, 0);  
		SharedPreferences.Editor editor = settings.edit();  
		editor.putBoolean(PREFS_CONFIG_IS_FIRST_RUN, firstRunValue);  
		editor.commit();  
		String str = "markFirstRun No";
		if (firstRunValue)
		{
			str = "markFirstRun Yes";
		}
		sGameClient.cocosLog(str);
	}
	
	public static void setCocosLayout() {
		if(sGameClient != null) {
			sGameClient.runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					Log.v(sTag, "setCocosLayout!!!");
					sGameClient.mProgressDialog.cancel();
					//sGameClient.checkGonggao();
					TencentJniCallback.nativeEnterGameScene();
					StopVideo();  
				}
			});
		}
	}

	@Override
	protected void onDestroy() {
		
		//ucsdk
	    // UCGameSdk.destroyFloatButton();    //销毁悬浮按钮
	    // UCGameSdk.exitSDK();
		Log.v("8089", "onDestroy " + this.hashCode());
		
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if(mNeedInitOnResume)
		{
			checkUpdateAndInit();
		}
		
		TalkingDataGA.onResume(this);
		TencentSDK.onResume();
		
		if(null == mMyVideoView)
		{
			//初始化视频播放
			this.initVideoView();
			GameClient.PlayVideo("movie.mp4");
		}
		else
		{
			mMyVideoView.restart();
		}
		
		Log.v("8089", "onResume " + this.hashCode());
	}

	@Override
	protected void onPause() {
		super.onPause();
		TalkingDataGA.onPause(this);
		TencentSDK.onPause();
		mMyVideoView.pause();
		Log.v("8089", "onPause " + this.hashCode());
	}
	
	protected void clearVersion()
	{
		InputStream is = null;
		String zipVersion = "0";
		File versionFile = new File(this.getFilesDir(), "version");	//��ѹ��ʱ��version�ļ������������Ŀ¼��	
		if(versionFile.exists()) {
			try {
				is = new FileInputStream(versionFile);
				InputStreamReader in = new InputStreamReader(is);
				BufferedReader bufferReader = new BufferedReader(in);
				zipVersion = bufferReader.readLine();
				bufferReader.close();
			} catch (Exception e) {
				e.printStackTrace();
				Cocos2dxHelper.terminateProcess();
			}
			
			int zipVersionNum = Integer.parseInt(zipVersion);
			if(zipVersionNum < 1117) {
				Log.v(sTag, "vervison < 1117, ver=" + zipVersionNum);
				clearNativeSo();
			}
		} 
	}
	
	protected void clearNativeSo()
	{
		File versionFile = new File(this.getFilesDir(), "version");	
		if(versionFile.exists()) {
			versionFile.delete();
		}
		File soFile = new File(this.getFilesDir(), "libgame.so");
		if(soFile.exists()) {
			soFile.delete();
		}
	}
	
	private String getCurrentVersion() {
		InputStream is = null;
		
		//���°��version
		String zipVersion = "0";
		File versionFile = new File(this.getFilesDir(), "version");	//��ѹ��ʱ��version�ļ������������Ŀ¼��	
		if(versionFile.exists()) {
			try {
				is = new FileInputStream(versionFile);
				InputStreamReader in = new InputStreamReader(is);
				BufferedReader bufferReader = new BufferedReader(in);
				zipVersion = bufferReader.readLine();
				bufferReader.close();
			} catch (Exception e) {
				e.printStackTrace();
				Cocos2dxHelper.terminateProcess();
			}
		} 
		//apk��version
		String apkVersion = "0";
		try {
			is = this.getAssets().open("version");
			InputStreamReader in = new InputStreamReader(is);
			BufferedReader bufferReader = new BufferedReader(in);
			apkVersion = bufferReader.readLine();
		} catch (Exception e1) {
			e1.printStackTrace();
			Cocos2dxHelper.terminateProcess();
		}
		Log.v(sTag, "zipVersion=" + zipVersion);
		Log.v(sTag, "apkVersion" + apkVersion);
		int zipVersionNum = Integer.parseInt(zipVersion);
		int apkVersionNum = Integer.parseInt(apkVersion);
		String strVersion = zipVersion;
		if (apkVersionNum >= zipVersionNum)
		{
			strVersion = apkVersion;
			//ɾ�����Ŀ¼
			deleteAllUpdateFiles();
		}
		return strVersion;
	}
	
	private static void deleteAllUpdateFiles() {
		File rootDir = sGameClient.getRootDir();
		if (rootDir.exists())
		{
			Log.v(sTag, "clear RootDir");
			GameClient.clearFolder(rootDir);
		}
		sGameClient.clearNativeSo();
	}
	
	public static void clearFolder(File file)
	{
		if (file.isDirectory() == false)
			return;
		File files[] = file.listFiles();
		for (int i=0; i<files.length; i++)
		{
			deleteFileOrDirectory(files[i]);
		}
	}
	
	public static void deleteFileOrDirectory(File file)
	{
		if (file.exists())
		{
			if (file.isFile())
			{
				String nameStr = file.getName();
				if (!nameStr.contains("lastUpdateInfo.ini")
						&& !nameStr.contains(".apk")
						&& !nameStr.contains(".zip")) {
					file.delete();
				}
			}
			else if (file.isDirectory())
			{
				File files[] = file.listFiles();
				for (int i=0; i<files.length; i++)
				{
					deleteFileOrDirectory(files[i]);
				}
			}
		}
	}
	
	public static void claerDownloadTempFiles() {
		File rootDir = sGameClient.getRootDir();
		File files[] = rootDir.listFiles();
		for (int i=0; i<files.length; i++)
		{
			String nameStr = files[i].getName();
			if (nameStr.contains("lastUpdateInfo.ini")
					|| nameStr.contains(".apk")
					|| nameStr.contains(".zip")) {
				
				Log.v(sTag, "delete tmpfile: " + nameStr);
				files[i].delete();
			}
		}
	}
	
	private File getRootDir() {
		if((this.getApplicationInfo().flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) {
			return this.getExternalFilesDir(null);
		}
		else {
			return this.getFilesDir();
		}
	}
	
	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			String url = params[0];
			String version = params[1];
			String deviceId = params[2];
			
			StringBuffer buffer = new StringBuffer();
			try {
				Log.v(sTag, "URL [" + url + "]");
				url += "?version=" + version;
				url += "&deviceid=" + deviceId;
				
				HttpURLConnection con = (HttpURLConnection) ( new URL(url)).openConnection();
				con.setRequestMethod("GET");
				con.setDoInput(true);
				con.setConnectTimeout(10*1000);
				con.setReadTimeout(10*1000);
				
				InputStream is = con.getInputStream();
				System.out.println("4...");
				
				BufferedReader bufferReader = new BufferedReader(new InputStreamReader(is, "utf-8"));
				String str = null;
				while((str = bufferReader.readLine()) != null) {
					buffer.append(str);
				}		

				con.disconnect();
			}
			catch(Throwable t) {
				t.printStackTrace();
			}
			
			return buffer.toString();
		}

		@Override
		protected void onPostExecute(String result) {
			Log.v(sTag, "Data ["+result+"]");
			
			mUpdateResult = result;
			onDealWithUpdate(result);
		}
	}
	
	private void onDealWithUpdate(String result){
		
		if(result == null) {
			return;
		}
			
		if(!mLogoTimeout) {
			return;
		}
		
		String []strs = result.split("<br/>");
		Log.v(sTag, "strs length = " + strs.length);
		if(strs.length == 2) {
			//setContentView(R.layout.activity_main);
			mProgressDialog.cancel();

			mUpdateView = getLayoutInflater().inflate(R.layout.activity_main, null);
			mFramelayout.addView(mUpdateView);

			mProgress = (ProgressBar)findViewById(R.id.progressBar1);
			mTextView = (TextView)findViewById(R.id.textViewUpdate);
			
			String zipUrl = strs[0];
			String fileMd5 = strs[1];
			Log.v(sTag, "strs[0] = " + zipUrl);
			Log.v(sTag, "strs[1] = " + fileMd5);
			
			//下载更新
			GetZiplengthTask zipDownload = new GetZiplengthTask();
			mDownloadParams = new String[]{zipUrl, zipUrl.substring(zipUrl.lastIndexOf("/")+1), fileMd5};
			zipDownload.execute(mDownloadParams);
		} else if(result.length() == 0) {
			//MessageBoxQuit(R.string.cannot_connect);
			MessageBoxRetry(R.string.cannot_connect, TASK_CHECK_UPDATE);
		} else {
			enterGame();
		}
	}
	
	private class GetZiplengthTask extends AsyncTask<String, Integer, Boolean> {
		@Override
		protected Boolean doInBackground(String... params) {
			mParams = params;
			String url = params[0];
			String zipName = params[1];
			String fileMd5 = params[2];
			int lastDownloadedSize = 0;
			
			//查看是否有未下完的旧文件
			File lastDownloadedZipFile = new File(sGameClient.getRootDir(), zipName);
			File lastUpdateInfoFile = new File(sGameClient.getRootDir(), "lastUpdateInfo.ini");
			if(lastDownloadedZipFile.exists()) 
			{
				boolean bIsContinue = false;
				InputStream is = null;
				if(lastUpdateInfoFile.exists()) 
				{
					try 
					{
						is = new FileInputStream(lastUpdateInfoFile);
						InputStreamReader in = new InputStreamReader(is);
						BufferedReader bufferReader = new BufferedReader(in);
						String tempMd5 = bufferReader.readLine();
						if (fileMd5.equalsIgnoreCase(tempMd5))
						{
							//文件存在，且md5码相同，断点续传
							bIsContinue = true;
						}
						bufferReader.close();
					} 
					catch (Exception e) 
					{
						e.printStackTrace();
					}
				}
				
				if(bIsContinue) 
				{
					//断点续传
					FileInputStream fis = null;
					try 
					{
						fis = new FileInputStream(lastDownloadedZipFile);
						lastDownloadedSize= fis.available();
						if(lastDownloadedSize > 0) 
						{
							--lastDownloadedSize;
						}
						fis.close();
					} 
					catch (FileNotFoundException e) 
					{
						e.printStackTrace();
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
			}
			
			Log.v(sTag, "!!!!!!!!!!already downloaded + " + lastDownloadedSize);
			int iUpdateLen = 0;
			try {
				HttpURLConnection con = (HttpURLConnection) ( new URL(url)).openConnection();
				con.setRequestMethod("GET");
				con.setDoInput(true);
				con.setRequestProperty("Range", "bytes=" + lastDownloadedSize + "-"); 
				iUpdateLen = con.getHeaderFieldInt("Content-Length", -1);
				System.out.println("Content-Length = " + iUpdateLen);
				         
				con.disconnect();
			}
			catch(Throwable t) {
				return false;
			}
			
			mUpdateLen = iUpdateLen / 1024.0 / 1024.0;
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if(result) 
			{
				//获取内部存储空间剩余大小
				long availabeSize = getAvailableInternalMemorySize();
				availabeSize = availabeSize / 1024 / 1024;
				long needSize = (long) (mUpdateLen * 2);
				Log.v(sTag, "CHECK SIZE availabeSize=" + availabeSize + "， needSize=" + needSize);
				if(availabeSize < needSize)
				{
					//存储空间不足
					String strNeedSize = String.format(getResources().getString(R.string.need_size)
							, mUpdateLen * 2.0f);
					
					new AlertDialog.Builder(sGameClient)
						.setTitle(R.string.app_name)
						.setMessage(strNeedSize)
						.setCancelable(false)
						.setPositiveButton(R.string.quit, 
								new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										Cocos2dxHelper.terminateProcess();
									}
								})
						.create().show();
				}
				else
				{
					String strUpdate = String.format(getResources().getString(R.string.need_update)
							, mUpdateLen);
					
					new AlertDialog.Builder(sGameClient)
						.setTitle(R.string.app_name)
						.setMessage(strUpdate)
						.setCancelable(false)
						.setPositiveButton(R.string.update, 
								new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										DownloadZipTask zipDownload = new DownloadZipTask();
										zipDownload.execute(mParams);
									}
								})
						.setNegativeButton(R.string.quit
								, new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										Cocos2dxHelper.terminateProcess();
									}
								})
						.create().show();
				}
			}
			else
			{
				//MessageBoxQuit(R.string.cannot_connect);
				MessageBoxRetry(R.string.cannot_connect, TASK_GET_ZIP_LEN);
			}
		}
		
		String[] mParams;
		double mUpdateLen = 0.0;
	}
	
private class DownloadZipTask extends AsyncTask<String, Integer, Boolean> {
		
		@Override
		protected Boolean doInBackground(String... params) {
			String url = params[0];
			String zipName = params[1];
			String fileMd5 = params[2];
			int lastDownloadedSize = 0;
			
			//查看是否有未下完的旧文件
			File lastDownloadedZipFile = new File(sGameClient.getRootDir(), zipName);
			File lastUpdateInfoFile = new File(sGameClient.getRootDir(), "lastUpdateInfo.ini");
			if(lastDownloadedZipFile.exists()) {
				boolean bIsContinue = false;
				InputStream is = null;
				if(lastUpdateInfoFile.exists()) {
					try {
						is = new FileInputStream(lastUpdateInfoFile);
						InputStreamReader in = new InputStreamReader(is);
						BufferedReader bufferReader = new BufferedReader(in);
						String tempMd5 = bufferReader.readLine();
						if (fileMd5.equalsIgnoreCase(tempMd5))
						{
							//文件存在，且md5码相同，断点续传
							bIsContinue = true;
						}
						bufferReader.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				if(bIsContinue) {
					//断点续传
					FileInputStream fis = null;
					try {
						fis = new FileInputStream(lastDownloadedZipFile);
						lastDownloadedSize= fis.available();
						if(lastDownloadedSize > 0) {
							--lastDownloadedSize;
						}
						fis.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					Log.v(sTag, "continue download...already downloaded + " + lastDownloadedSize);
				}
				else {
					//不是断点续传，删除旧文件
					lastDownloadedZipFile.delete();
					lastUpdateInfoFile.delete();
				}
			}
			
			//写入md5信息
			BufferedOutputStream bosLastUpdateInfoFile = null;
			try {
				bosLastUpdateInfoFile = new BufferedOutputStream(new FileOutputStream(lastUpdateInfoFile));
				bosLastUpdateInfoFile.write(fileMd5.getBytes());
				bosLastUpdateInfoFile.close();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			int iUpdateLen = 0;
			int iDownloadLen = lastDownloadedSize;
			int totalFileSize = iDownloadLen;
			try {
				System.out.println("URL ["+url+"] - file ["+zipName+"]");
				mZipName = zipName;
				
				HttpURLConnection con = (HttpURLConnection) ( new URL(url)).openConnection();
				con.setRequestMethod("GET");
				con.setDoInput(true);
				con.setRequestProperty("Range", "bytes=" + lastDownloadedSize + "-"); 
				iUpdateLen = con.getHeaderFieldInt("Content-Length", -1);
				System.out.println("Content-Length = " + iUpdateLen);
				totalFileSize = iUpdateLen + lastDownloadedSize;
				BufferedInputStream in = new BufferedInputStream(con.getInputStream());
				
				File zipFile = new File(sGameClient.getRootDir(), mZipName);
				RandomAccessFile out = null;	            
				try {
					out = new RandomAccessFile(zipFile, "rw");
					out.seek(lastDownloadedSize); 
					byte[] b = new byte[500 * 1024];
					int iRet = 0;

					while ( (iRet=in.read(b)) != -1) {
						out.write(b, 0, iRet);
						iDownloadLen += iRet;
						
						Integer []iParams = new Integer[2];
						iParams[0] = iDownloadLen;
						iParams[1] = totalFileSize;
						
						publishProgress(iParams);
					}
					out.close();	
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
					
				con.disconnect();
				
			}
			catch(Throwable t) {
				t.printStackTrace();
				Log.v(sTag, "!! iDownloadLen=" + iDownloadLen + ", totalFileSize=" + totalFileSize);
				return iDownloadLen == totalFileSize;
			}
			
			Log.v(sTag, "iDownloadLen=" + iDownloadLen + ", totalFileSize=" + totalFileSize);
			return iDownloadLen == totalFileSize;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			int iDownload = values[0];
			int iTotal = values[1];
			
			mTextView.setText(R.string.downloading);
			String str = null;
			if(iTotal > 1024) {
				String strDownload = String.format("%.2f", iDownload / 1024.0 / 1024.0);
				String strTotal = String.format("%.2f", iTotal / 1024.0 / 1024.0);
				str = new String("(" + strDownload + "M" + " / " + strTotal + "M" + ")");
			} else {
				String strDownload = String.format("%.2f", iDownload / 1024.0);
				String strTotal = String.format("%.2f", iTotal / 1024.0);
				str = new String("(" + strDownload + "K" + " / " + strTotal + "K" + ")");
			}

			mTextView.append(str);
			
			//���½��
			int iProgress = (int) (iDownload / (double)iTotal * 100);
			mProgress.setProgress(iProgress);
		}

		@Override
		protected void onPostExecute(Boolean result) {
	
			if(result) {
				Log.v(sTag, "[" + mZipName + "] downloaded");
				//GameClient.deleteLastDownloadInfo(); 
				//����ļ���׺������ǰ�װ���ǽ�ѹ��
				if (true)
				{
					String postfix = mZipName.substring(mZipName.lastIndexOf(".")+1);
					if (postfix.equalsIgnoreCase("apk"))
					{
						//��װapk
						//GameClient.deleteAllUpdateFiles();
						String zipFullPath = sGameClient.getRootDir() + "/" +mZipName;
						Log.v(sTag, "install [" + zipFullPath + "]");
						GameClient.installAPK(zipFullPath);
					}
					else if (postfix.equalsIgnoreCase("zip"))
					{
						//��ѹ��zip��
						Log.v(sTag, "uncompress [" + mZipName + "]");
						UnpackZipTask unpackZipTask = new UnpackZipTask();
						String[] zipParams = new String[]{mZipName};
						unpackZipTask.execute(zipParams);
					}
				}
				
			} else {
				//MessageBoxQuit(R.string.cannot_connect);
				MessageBoxRetry(R.string.cannot_connect, TASK_DOWNLOAD);
			}

		}
		
		private String mZipName = null;
	}

	private class UnpackZipTask extends AsyncTask<String, Integer, Boolean> {
	
		@Override
		protected void onPreExecute() {
			mTextView.setText(R.string.unpacking);
			mProgress.setProgress(0);
		}
	
		@Override
		protected Boolean doInBackground(String... arg0) {
			
			File rootDir = new File(sGameClient.getRootDir(), "updates");
			File zipFile = new File(sGameClient.getRootDir(), arg0[0]);
			
			ZipInputStream in = null;
			
			try {
				FileInputStream fin = new FileInputStream(zipFile);
				int iTotalSize = fin.available();
				Log.v(sTag, "iTotalSize [" + iTotalSize + "]");
				int iUnpackedSize = 0;
				
				in = new ZipInputStream(fin);
				ZipEntry entry = null;
				while((entry = in.getNextEntry()) != null) {
					File tmpFile = new File(rootDir, entry.getName());
					tmpFile.getParentFile().mkdirs();
					
					if(entry.isDirectory()) {
						tmpFile.mkdir();
					} else {
						//Log.v(sTag, "name[" + entry.getName() + "]");
						FileOutputStream fos = null;
						
						String fileName = entry.getName();
						String []strs = entry.getName().split("/");
						if(strs.length > 0) {
							fileName = strs[strs.length-1];
						}
						if(fileName.equals("libgame.so") || fileName.equals("version")) {
							Log.v(sTag, "unpack file : " + fileName);
							File soFile = new File(sGameClient.getFilesDir(), fileName);
							fos = new FileOutputStream(soFile);
						} else {
							fos = new FileOutputStream(tmpFile);
						}
						BufferedOutputStream output = new BufferedOutputStream(fos);
						byte []buffer = new byte[1024];
						int iRet = 0;
						while((iRet = in.read(buffer)) != -1) {
							output.write(buffer, 0, iRet);
						}
						output.close();
					}
					in.closeEntry();
					iUnpackedSize += (int)entry.getCompressedSize();
					publishProgress(new Integer[]{iUnpackedSize, iTotalSize});
				}
				in.close();
				
				//��ѹ��ɣ�ɾ��zip�ļ�
				zipFile.delete();
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return true;
			}
			
			return true;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			//���½��
			//Log.v(sTag, "values[0]="+values[0]+"  values[1]="+values[1]);
			int iProgress = (int) (values[0] / (double)values[1] * 100);
			mProgress.setProgress(iProgress);
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if(result) {
				mProgress.setProgress(100);
				
				mProgressDialog = new ProgressDialog(GameClient.this);
				mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mProgressDialog.setCancelable(false);
				mProgressDialog.setCanceledOnTouchOutside(false);
				mProgressDialog.setMessage(getResources().getString(R.string.launching));
				//mProgressDialog.show();
				
				enterGame();
			} else {
				MessageBoxQuit(R.string.cannot_unpack);
			}
		}
	}
	
	private void MessageBoxQuit(int iReason) {
		new AlertDialog.Builder(this)
		.setTitle(R.string.app_name)
		.setMessage(iReason)
		.setCancelable(false)
		.setPositiveButton(R.string.ok, mQuitListener)
		.create().show();
	}
	
	private void MessageBoxRetry(int iReason, final int iType) {
		new AlertDialog.Builder(this)
		.setTitle(R.string.app_name)
		.setMessage(iReason)
		.setCancelable(false)
		.setPositiveButton(R.string.retry, 
				new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch(iType)
						{
						case TASK_CHECK_UPDATE:
							SendHttpRequestTask t = new SendHttpRequestTask();
							t.execute(mCheckUpdateParams);
							break;
						case TASK_DOWNLOAD:
							DownloadZipTask zipDownload = new DownloadZipTask();
							zipDownload.execute(mDownloadParams);
							break;
						case TASK_GET_ZIP_LEN:
							GetZiplengthTask getLenTask = new GetZiplengthTask();
							getLenTask.execute(mDownloadParams);
							break;
						}
					}
				})
		.setNegativeButton(R.string.quit, mQuitListener)
		.create().show();
	}
	
	public static void installAPK(String filePath)
	{
		//���޸��ļ�Ȩ��
		String chmodCommandStr = "chmod 666 " + filePath;
		Log.v(sTag, chmodCommandStr);
		try
		{
			Process p = Runtime.getRuntime().exec(chmodCommandStr);
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				Log.v(sTag, "chmod process InterruptedException");
				e.printStackTrace();
			}
		}
		catch (IOException e)
		{
			Log.v(sTag, "chmod failed");
			e.printStackTrace();
		}
		
		Log.v(sTag, "installAPK="+filePath);
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(Uri.parse("file://" + filePath), "application/vnd.android.package-archive");
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		sGameClient.startActivity(i);
	}
	
	private String getVersionUrl() {
		File urlFile = new File(this.getRootDir(), "versionUrl");
		if(urlFile.exists()) {
			try {
				InputStreamReader in = new InputStreamReader(new FileInputStream(urlFile));
				BufferedReader bufferReader = new BufferedReader(in);
				String tmpUrl = bufferReader.readLine();
				if(tmpUrl != null && !tmpUrl.isEmpty() && tmpUrl.indexOf("http") != -1) {
					Log.v(sTag, "tmpUrl=" + tmpUrl);
					mUpdateURL = tmpUrl;
				}
				bufferReader.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		} else {
			try {
				urlFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return mUpdateURL;
	}
	
	private void enterGame() {
		moveVideoFile();
		if(mUpdateView != null)
		{
			mFramelayout.removeView(mUpdateView);
			mUpdateView = null;
		}
		claerDownloadTempFiles();
		sGameClient.checkGonggao();
		//initCocos2dx();
	}

	public static void onExitGame() {
		Log.v("CZXZ", "onExitGame...");
		
		/*
        sGameClient.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    UCGameSDK.defaultSDK().exitSDK(sGameClient, UcExitListener.getInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("CZXZ", e.getMessage(), e);
                }
            }
        });
        */
	}
	
	public static void exitResultCallback(int iCode, String msg) {
		/*
		Log.v("CZXZ", "exitResultCallback...code=" + iCode + ", msg=" + msg);
        switch (iCode) {
        case UCGameSDKStatusCode.SDK_EXIT:
        	Cocos2dxHelper.terminateProcess();
            break;
        case UCGameSDKStatusCode.SDK_EXIT_CONTINUE:
            break;
        default:
            break;
        }
        */
	}

	@Override
	protected void onStart() {
		super.onStart();
		TencentSDK.onStart();
		
		Log.v("8089", "onStart " + this.hashCode());
		
		//Intent intent = new Intent(this, GameService.class);
		//bindService(intent, mConnection, BIND_AUTO_CREATE);  
	}

	@Override
	protected void onStop() {
		super.onStop();
		TencentSDK.onStop();
		
		if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
		
		Log.v("8089", "onStop " + this.hashCode());
	}
	
	public static void setNotificationEnable(boolean bEnable) {
		//储存信息
		Editor editor = sGameClient.getSharedPreferences(NOTIFICAION_TAG, Activity.MODE_MULTI_PROCESS).edit();
		editor.putBoolean(NOTIFICAION_ENABLE, bEnable);
		editor.commit();
	}
	
	public static void requestMid() {
		
		MidService.requestMid(sGameClient, new MidCallback() {

			@Override
			public void onFail(int errCode, String msg) {
				Log.e(sTag,  "get mid failed, error code:" + errCode + " ,msg:" + msg); 
			}

			@Override
			public void onSuccess(Object data) {
				Log.d(sTag, "get mid from midService:" + data.toString()); 
				TencentJniCallback.nativeMidCallback(data.toString());
			}
			
		});
	}
	
	@Override
	public void onBackPressed() 
	{
		//MessageBoxQuit(R.string.hello_world);
		Log.d(sTag, "onBackPressed"); 
		return;
	}
	
	protected DialogInterface.OnClickListener mQuitListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			Cocos2dxHelper.terminateProcess();
		}
	};
	
    public boolean checkNetwork(){  
    	
    	try {
	        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);  
	        if(manager == null) {
	        	Log.d(GameClient.sTag, "getSystemService(Context.CONNECTIVITY_SERVICE) is null!!");
	        	return true;
	        }
	        
	        State mobileState = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState(); 
	        if(mobileState==State.CONNECTED || mobileState==State.CONNECTING)
	        {
	        	return true;
	        }
	        
	        State wifiState = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();  
	        if(wifiState==State.CONNECTED||wifiState==State.CONNECTING)
	        {
	        	return true;
	        }
	        
	        return false;
    	}
    	catch(Exception e) {
    		return true;
    	}
    }  
    
    public static String getMetaData(String key) {
	Log.v(sTag, "#############" + key);
    	if (key.equals("yunyingshang"))
    	{
			Log.v(sTag, "#############" + 1);
    		return getYunyingshang();
    	}
    	if (key.equals("jixing"))
    	{
			Log.v(sTag, "#############" + 2);
    		return getJixing();
    	}
    	if (key.equals("wangluomoshi"))
    	{
			Log.v(sTag, "#############" + 3);
    		return getWangluomoshi();
    	}
    	if( key.equals("getimei"))
    	{
			Log.v(sTag, "###########imei" + 4);
    		return getIMEI();
    	}
    	if( key.equals("getimsi"))
    	{
			Log.v(sTag, "###########imsi" + 5);
    		return getIMSI();
    	}
    	if( key.equals("gettelephonenum"))
    	{
			Log.v(sTag, "###########telephonenum" + 6);
    		return getTelephoneNum();
    	}
    	ApplicationInfo appInfo;
		try {
			 appInfo = sGameClient.getPackageManager()
			            .getApplicationInfo(sGameClient.getPackageName(),PackageManager.GET_META_DATA);
			    //appInfo.metaData.getString("meta_name");
			
			return appInfo.metaData.getString(key);  
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}  
    }
    
    public static void onTerminateProcess()
    {
    	Log.w(sTag, "onTerminateProcess...");
    	sGameClient.mMyVideoView.stopPlayback();
    }
    
    protected void initVideoView()
    {
    	if(null == mMyVideoView)
		{
			DisplayMetrics displaysMetrics = new DisplayMetrics(); 
			getWindowManager().getDefaultDisplay().getMetrics(displaysMetrics); 

			//播放背景动画
			mMyVideoView = new MyVideoView(this);
			//mFramelayout.addView( mMyVideoView );
			mMyVideoView.setZOrderOnTop(false);
			mMyVideoView.setZOrderMediaOverlay(false);
			
			//FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(displaysMetrics.widthPixels, displaysMetrics.heightPixels);
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		    FrameLayout fl = new FrameLayout(this);
		    fl.setLayoutParams(lp);
		    
		    Gallery gallery = new Gallery(this);
		    gallery.setBackgroundColor(Color.BLACK);
		    gallery.setLayoutParams(lp);
		    fl.addView(gallery);

		    //mImageViewBg = getLayoutInflater().inflate(R.layout.layout_fitbg, null);
		    //fl.addView(mImageViewBg);
		    
		    FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		    lp2.gravity = Gravity.CENTER;
		    mMyVideoView.setLayoutParams(lp2);
		    fl.addView(mMyVideoView);
		    mFramelayout.addView( fl );

			
			double ratio = 640.0 / 1136.0f;
			int height = displaysMetrics.heightPixels;
			int width =  (int)(displaysMetrics.heightPixels * ratio);
			if(width > displaysMetrics.widthPixels)
			{
				//算出来的长度大,
				width = displaysMetrics.widthPixels;
				height = (int)(displaysMetrics.widthPixels / ratio);
			}
	
            Log.w("peng", "~~~ width=" + width + "  height=" + height);
            mMyVideoView.setVideoAspect(width,height);
		}

    }
    
    public static void PlayVideo(String videoPath)
    {
    	Message msg = new Message();
    	msg.what = MSG_PLAY_VIDEO;
    	msg.obj = videoPath;
    	sGameClient.mHandler.sendMessage(msg);
    }
    
    public static void StopVideo()
    {
    	Message msg = new Message();
    	msg.what = MSG_STOP_VIDEO;
    	msg.obj = null;
    	sGameClient.mHandler.sendMessage(msg);
    }
    
    protected Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
            case MSG_PLAY_VIDEO:
            	String videoFilename = (String)msg.obj;
            	//String videoPath = "/updates/res/raw/" + videoFilename;
            	
            	//选择资源
            	File videoFile = null;
            	File externalFile = GameClient.this.getExternalFilesDir(null);
            	if(externalFile != null)
            	{
            		videoFile = new File(externalFile, videoFilename);
            	}
        		if(videoFile != null && videoFile.exists())
        		{
        			Log.w(sTag, "PlayVideo " + videoFile.getAbsolutePath() );
        			mMyVideoView.setVideoPath(videoFile.getAbsolutePath());
        		}
        		else
        		{
        			int id = 0;
        			if(videoFilename.equals("movie.mp4"))
        			{
        				id = R.raw.movie;
        			}
        			
        			if(0 == id)
        			{
        				Log.e(sTag, "No Video ID!!  " + videoFilename );
        				break;
        			}
        			
        			Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + id);  
        			mMyVideoView.setVideoURI(uri);
        		}

        		mMyVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {  
                    @Override  
                     public void onCompletion(MediaPlayer mp) {  
                         mp.start();  
                         mp.setLooping(true);  
                     }  
        		});  
        		mMyVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
        			
        			@Override
        			public boolean onError(MediaPlayer arg0, int what, int extra) {
        				Log.e(sTag, "VideoView ERROR what=" + what + " extra=" + extra);
        				/*
        				if(1 == what && -1004 ==extra)
        				{
        					mMyVideoView.start();
        				}
        				*/
        				return true;
        			}
        		});
        		mMyVideoView.start();
        		
                break;
			case MSG_STOP_VIDEO:
				mMyVideoView.stopPlayback();
				break;
            }
		}
    };
    
    
    private void checkGonggao(){
	    Log.v(sTag, "checkGonggao"); 
	    
	    mWebViewDialog = new ProgressDialog(this);
	    mWebViewDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    mWebViewDialog.setCancelable(false);
	    mWebViewDialog.setCanceledOnTouchOutside(false);
	    mWebViewDialog.setMessage(getResources().getString(R.string.loadingGonggao));
	    mWebViewDialog.show();
		
        //初始化webView  
    	gonggaoView = getLayoutInflater().inflate(R.layout.gonggao, null);
        mFramelayout.addView(gonggaoView);
        m_webView = (WebView)findViewById(R.id.gonggaowebview);
        m_webView.setBackgroundColor(0);  
		  
		int sysVersion = VERSION.SDK_INT;  
        if(sysVersion>11){
			m_webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}
        //设置webView能够执行javascript脚本  
        m_webView.getSettings().setJavaScriptEnabled(false);  
        m_webView.getSettings().setBlockNetworkImage(true);  
        //设置可以支持缩放  
        m_webView.getSettings().setSupportZoom(false);//设置出现缩放工具  
        m_webView.getSettings().setBuiltInZoomControls(false);  
        //载入URL  
        m_webView.getSettings().setCacheMode(m_webView.getSettings().LOAD_NO_CACHE);
        m_webView.loadUrl(getMetaData("GONGGAO_URL"));  
        //使页面获得焦点  
        m_webView.requestFocus();  
        //如果页面中链接，如果希望点击链接继续在当前browser中响应  
        m_webView.setWebViewClient(new WebViewClient(){         
            public boolean shouldOverrideUrlLoading(WebView view, String url) {     
                if(url.indexOf("tel:")<0){  
                    view.loadUrl(url);   
                }  
                return true;         
            } 
            @Override			
			public void onPageFinished(WebView view, String url)   
			{  
				//结束  
            	sGameClient.mWebViewDialog.cancel();
				super.onPageFinished(view, url);  
			}
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon)
			{
				//开始
				super.onPageStarted(view, url, favicon);
			} 
        });  
        //初始化返回按钮  
        m_backButton =  (Button)findViewById(R.id.gonggaobtn);
        m_backButton.setOnClickListener(new OnClickListener() {                      
            public void onClick(View v) {  
                removeWebView();  
            }  
        }); 
        
        DisplayMetrics displaysMetrics = new DisplayMetrics(); 
		getWindowManager().getDefaultDisplay().getMetrics(displaysMetrics); 
		Log.w(sTag, "density=" + displaysMetrics.density
				+ " densityDpi=" + displaysMetrics.densityDpi
				+ " scaledDensity=" + displaysMetrics.scaledDensity);
	}
	public void removeWebView() {
		//gonggaoView.setVisibility(View.INVISIBLE);
		mFramelayout.removeView(gonggaoView);
		//mGLSurfaceView.requestFocus();
		
		this.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				mProgressDialog = new ProgressDialog(GameClient.this);
				mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mProgressDialog.setCancelable(false);
				mProgressDialog.setCanceledOnTouchOutside(false);
				mProgressDialog.setMessage(getResources().getString(R.string.launching));
				mProgressDialog.show();
				initCocos2dx();
				//TencentJniCallback.nativeEnterGameScene();
			}
		});
		
	}  
	
	protected void moveVideoFile()
	{
		try {
			File rootFile = new File(getRootDir(), "/updates/res/raw");
			if(null != rootFile && rootFile.exists())
			{
				File[] files = rootFile.listFiles();
				for(File file:files)
				{
					if(!file.isDirectory())
					{
						Log.v(sTag, "moveVideoFile : " + file.getName());
						if(file.getName().contains(".mp4"))
						{
							File externalFile = getExternalFilesDir(null);
							if(externalFile != null)
							{
								if(file.getName().equals("movie.mp4"))
								{
									mMyVideoView.stopPlayback();
								}
								
								File mp4File = new File(externalFile, file.getName());
								FileOutputStream fos = new FileOutputStream(mp4File);
								
								FileInputStream fin = new FileInputStream(file);
								BufferedInputStream in = new BufferedInputStream(fin);
								BufferedOutputStream output = new BufferedOutputStream(fos);
								byte []buffer = new byte[1024];
								int iRet = 0;
								while((iRet = in.read(buffer)) != -1) {
									output.write(buffer, 0, iRet);
								}
								output.close();
								in.close();
								file.delete(); //删除旧文件
								
								if(file.getName().equals("movie.mp4"))
								{
									PlayVideo("movie.mp4");
								}
							}
							else
							{
								Log.w(sTag, "No External Storage~");
								break;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			Log.e(sTag, "moveVideoFile " + e.toString());
		}
	}

	public static void setLeBianResExtracting(boolean extracting) {
		Log.v("LEBIAN", "setLeBianResExtracting...");
		setResExtracting(sGameClient, extracting);
	}

	public static void setResExtracting(Context context, boolean extracting) {
		try {
			Log.v("LEBIAN", "setResExtracting...");
			Class clazz = Class.forName("com.excelliance.lbsdk.LebianSdk");
			Method m= clazz.getDeclaredMethod("setResExtracting", Context.class, boolean.class);
			m.invoke(clazz, context, extracting);
		} catch (Exception e) {
			Log.v("LEBIAN", "setResExtractingCatch...");
			e.printStackTrace();
		}
	}
	
	public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }
}
