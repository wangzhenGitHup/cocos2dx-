package com.youxigu.czxz;

import java.io.ByteArrayOutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.unipay.plugsdk.IUnipayServiceCallBack;
import com.tencent.unipay.plugsdk.UnipayPlugAPI;
import com.youxigu.czxz.R;

import com.tencent.stat.StatConfig;
import com.tencent.stat.StatReportStrategy;
import com.tencent.stat.StatService;
import com.tencent.stat.common.StatConstants;

public class TencentSDK {
	static Activity sActivity = null;
	
	public static Tencent sTencent = null;
	private static UnipayPlugAPI sUnipayAPI = null;
	private static final String sTencentAppID = "1101847351";
	private static final String sScope = "all";
	private static final String OPEN_ID = "openid";
	private static final String ACCESS_TOKEN = "access_token";
	private static final String EXPIRES_IN = "expires_in";
	private static final String PAY_TOKEN = "pay_token";
	private static final String PF = "pf";
	private static final String PF_KEY = "pfkey";
	
	private static String sPf = "";
	private static String sPfKey = "";
	private static String sPayToken = "";
	
	private static final String PREFS_NAME = "my_tencent_sdk";
	
	private static final int ON_LOGIN = 1;
	private static final int ON_PAY = 2;
	private static final int ON_START_LOGIN = 3;
	
	static class LoginRet {
		public int mCode;
		public String mMsg;
	};
	private static LoginRet sLoginRet = new LoginRet();
	
	public static void setActivity(Activity activity){ sActivity = activity;}
	
	public static void initSDK() {
		if(sTencent != null) {
			return;
		}
		
		Log.d(GameClient.sTag, "initSDK......1");
		sTencent = Tencent.createInstance(sTencentAppID, sActivity.getApplicationContext());
		
		Log.d(GameClient.sTag, "initSDK......2");
		sUnipayAPI = new UnipayPlugAPI(sActivity);
		sUnipayAPI.setCallBack(sUnipayStubCallBack);
		sUnipayAPI.setEnv("release"); //test release
		sUnipayAPI.setOfferId(sTencentAppID);
		sUnipayAPI.setLogEnable(true);
		
		StatConfig.setDebugEnable(true);
		StatService.startStatService(sActivity, null, StatConstants.VERSION);
		StatService.trackCustomEvent(sActivity, "onCreate", ""); 
		
		/*
		//获取储存的信息
		SharedPreferences sp = sActivity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
		String strOpenID = sp.getString(OPEN_ID, "");
		String strAccessToken = sp.getString(ACCESS_TOKEN, "");
		long lExpiresIn = (sp.getLong(EXPIRES_IN, 0) - System.currentTimeMillis()) / 1000;
		//long lExpiresIn = sp.getLong(EXPIRES_IN, 0);
		String strExpiresIn = String.valueOf(lExpiresIn);
		Log.d(GameClient.sTag, "prefs openid=" + strOpenID + " accessToken:" 
				 + strAccessToken + " expiresIn=" + strExpiresIn);
		
		if(!strOpenID.isEmpty() && !strAccessToken.isEmpty() && lExpiresIn > 0) {
			sTencent.setOpenId(strOpenID);
			sTencent.setAccessToken(strAccessToken, strExpiresIn);
			
			Log.d(GameClient.sTag, "getExpiresIn=" + sTencent.getExpiresIn());
		}
		*/
		
		
	}
	
	public static void login(int iDelay) {
		//doLogin();
		
		/*
		Log.v(GameClient.sTag, "login !!!!!!!!!!!!!!!!!!!!!!!!!!");
		Message msg = sHandler.obtainMessage();
		msg.what = ON_START_LOGIN;
		msg.obj = null;
		sHandler.sendMessage(msg);
		*/
		
		sHandler.postDelayed(new Runnable(){   

		    public void run() {   
		    	doLogin();
		    }   

		 }, iDelay);
	}
	
	private static void doLogin() {
		//Log.v(GameClient.sTag, "doLogin ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        //sActivity.runOnUiThread(new Runnable() {

           //@Override
           //public void run() {
        		IUiListener listener = new BaseUIListener() {
        			
        			@Override
        			public void onResult(int code, String msg) {       	
        				
        				/*
        			    Message message = sHandler.obtainMessage();
        			    message.what = ON_LOGIN;
        			    message.obj = new LoginRet(code, msg);
        			    sHandler.sendMessage(message);
        			    */
        				
        				sLoginRet.mCode = code;
        				sLoginRet.mMsg = msg;
						if(sLoginRet.mCode == 0) {
        					Log.d(GameClient.sTag, "loginTencent succeed: " + sLoginRet.mMsg);
        					//成功
            				try {	
        						JSONObject values = new JSONObject(sLoginRet.mMsg);
        						String openID = values.getString("openid");
        						String accessToken = values.getString("access_token");
        						String expiresIn = values.getString("expires_in");
        						sPayToken = values.getString("pay_token");
        						sPf = values.getString("pf");
        						sPfKey = values.getString("pfkey");
        						
        						/*
        						//储存信息
        						Editor editor = sActivity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE).edit();
        						editor.putString(OPEN_ID, openID);
        						editor.putString(ACCESS_TOKEN, accessToken);
        						//editor.putLong(EXPIRES_IN, System.currentTimeMillis() + Long.parseLong(expiresIn) * 1000);
        						editor.putLong(EXPIRES_IN, sTencent.getExpiresIn());
        						editor.putString(PAY_TOKEN, payToken);
        						editor.putString(PF, pf);
        						editor.putString(PF_KEY, pfKey);
        						editor.commit();
        						*/
        						
                				TencentJniCallback.nativeLoginResultCallback(sLoginRet.mCode, openID, accessToken
                						, expiresIn, sPayToken, sPf, sPfKey);
        						
        					} catch (JSONException e) {
        						e.printStackTrace();
        					}
        				}
        				else {
        					//失败或返回
        					Log.e(GameClient.sTag, "loginTencent failed: " + sLoginRet.mMsg);
             				TencentJniCallback.nativeLoginResultCallback(sLoginRet.mCode, "", ""
            						, "", "", "", "");
        				}
        			}
        		};
        		
        		if(sTencent.getOpenId() != null) {
        			//如果已登录，则先注销
        			sTencent.logout(sActivity);
        		}
        		
        		Log.d(GameClient.sTag, "loginTencent calling...appid=" + sTencentAppID
                		+ "  " + sScope);
        		
        		sTencent.login(sActivity, sScope, listener);
           //}
        //});
	}
	
	public static void onStart() {
		if(sUnipayAPI != null) {
			sUnipayAPI.bindUnipayService();
		}
	}
	
	public static void onStop() {
		if(sUnipayAPI != null) {
			sUnipayAPI.unbindUnipayService();
		}
	}
	

	//回调接口
	static private IUnipayServiceCallBack.Stub sUnipayStubCallBack = new IUnipayServiceCallBack.Stub() {
		
		@Override
		public void UnipayNeedLogin() throws RemoteException
		{
			Log.i("UnipayPlugAPI", "UnipayNeedLogin");
			login(0);
		}	

		@Override
		public void UnipayCallBack(int resultCode, int payChannel,
				int payState, int providerState, int saveNum, String resultMsg,
				String extendInfo) throws RemoteException
		{
			Log.i("UnipayPlugAPI", "UnipayCallBack \n" + 
					"\nresultCode = " + resultCode + 
					"\npayChannel = "+ payChannel + 
					"\npayState = "+ payState + 
					"\nproviderState = " + providerState);
			
			Message msg = sHandler.obtainMessage();
			msg.what = ON_PAY;
			msg.obj = resultCode;
			sHandler.sendMessage(msg);
		}
	};

	static private Handler sHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch(msg.what) {
			case ON_PAY:
				int iRetCode = (Integer)msg.obj;
				//Toast.makeText(sActivity, "call back retCode=" + String.valueOf(iRetCode), Toast.LENGTH_SHORT).show();
				if(iRetCode == -2)
				{//service绑定不成功
					sUnipayAPI.bindUnipayService();
				}
				TencentJniCallback.nativePayResultCallback(iRetCode);
				break;
//			case ON_START_LOGIN:
//				doLogin();
//				break;
			}
		}
	};

	public static void SaveGameCoins(int count, int zoneId) {
		final int ZONEID = zoneId;
		final int COUNT = count;
		sActivity.runOnUiThread(new Runnable() {
	            @Override
	            public void run() {
	            	Bitmap bmp = BitmapFactory.decodeResource(sActivity.getResources(), R.drawable.coin);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
					byte[] appResData = baos.toByteArray();
					
					try {
						sUnipayAPI.SaveGameCoinsWithNum(sTencent.getOpenId(), sPayToken, "openid", "kp_actoken"
								, String.valueOf(ZONEID), sPf, sPfKey, UnipayPlugAPI.ACCOUNT_TYPE_COMMON, String.valueOf(COUNT)
								, false, appResData);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
	            }
		 });
	}
	
	private static void initMTAConfig(boolean isDebugMode) {
		if (isDebugMode) { // 调试时建议设置的开关状态
			// 查看MTA日志及上报数据内容
			StatConfig.setDebugEnable(true);
			// 禁用MTA对app未处理异常的捕获，方便开发者调试时，及时获知详细错误信息。
			StatConfig.setAutoExceptionCaught(false);
			StatConfig.setEnableSmartReporting(false);
			Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			
				@Override
				public void uncaughtException(Thread thread, Throwable ex) {
					Log.e(GameClient.sTag, "setDefaultUncaughtExceptionHandler");
				}
			});
			// 调试时，使用实时发送
			StatConfig.setStatSendStrategy(StatReportStrategy.INSTANT);
		} else { // 发布时，建议设置的开关状态，请确保以下开关是否设置合理
			// 禁止MTA打印日志
			StatConfig.setDebugEnable(false);
			// 根据情况，决定是否开启MTA对app未处理异常的捕获
			StatConfig.setAutoExceptionCaught(true);
			// 选择默认的上报策略
			StatConfig.setStatSendStrategy(StatReportStrategy.APP_LAUNCH);
		}
	}
	
	public static void onResume() {
		StatService.onResume(sActivity);
	}

	public static void onPause() {
		StatService.onPause(sActivity);
	}
	
	public static void mtaLogin(String uid, String wid) {
		Properties prop = new Properties();   
		prop.setProperty("uid", uid);
		prop.setProperty("wid", wid);
		StatService.trackCustomKVEvent(sActivity, "onGameLogin", prop);   
	}
	
	public static void mtaLogout(String uid, String wid) {
		Properties prop = new Properties();   
		prop.setProperty("uid", uid);
		prop.setProperty("wid", wid);
		StatService.trackCustomKVEvent(sActivity, "onGameLogout", prop);   
	}
}
