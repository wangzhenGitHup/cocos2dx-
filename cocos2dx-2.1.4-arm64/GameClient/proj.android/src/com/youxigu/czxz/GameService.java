package com.youxigu.czxz;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;

public class GameService extends Service {

	static final int ON_NOTIFY = 1;
	static final int ON_CANCEL = 2;
	//protected long mLastChibaoziTime = 0;
	protected NotificationManager mNotificationManager = null;
	static protected Service sService = null;
	private final IBinder mBinder = new GameBinder();
	public static final String TAG = "CZXZ_Service";
	public static final String TAG_CHIBAOZI_TIME = "Chibaozi_TIME";
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.v(GameService.TAG, "GameService onCreate");
		sService = this;
		
		mNotificationManager = 
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		mThread.start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Log.v(GameService.TAG, "GameService onDestroy");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(GameService.TAG, "GameService onStartCommand");
		
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		Log.v(GameService.TAG, "GameService onBind");
		return mBinder;
	}

	protected Thread mThread= new Thread(new Runnable()  
    {  
        @Override  
        public void run()  
        {  
           while(true) 
           {
        	   Log.v(GameService.TAG, "GameService thread...");
        	   Time timeNow = new Time(Time.getCurrentTimezone());
         	   timeNow.setToNow();

        	   if( (timeNow.hour >= 12 && timeNow.hour < 14) 
        			   || (timeNow.hour >= 18 && timeNow.hour < 20)) 
        	   {
        		  
        		   Message message = new Message();
                   message.what=ON_NOTIFY;  
                   mHandler.sendMessage(message);  
        	   }
        	   else
        	   {
        		   Message message = new Message();
                   message.what=ON_CANCEL;  
                   mHandler.sendMessage(message);  
        	   }

        	   try {
				Thread.sleep(30000);
        	   } catch (InterruptedException e) {
					e.printStackTrace();
        	   }
           }
        }  
    });  
	
	Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			Log.v(GameService.TAG, "GameService handleMessage...");
			
			createNotification();
			
			SharedPreferences sp = sService.getSharedPreferences(GameClient.NOTIFICAION_TAG, Activity.MODE_MULTI_PROCESS);
			boolean bEnable = sp.getBoolean(GameClient.NOTIFICAION_ENABLE, true);
			if(!bEnable) {
				Log.v(GameService.TAG, "NOTIFICAION disabled !!!");
				mNotificationManager.cancelAll();
				return;
			}
			
			switch(msg.what)  
            {  
            case ON_NOTIFY: 
            	Time timeNow = new Time(Time.getCurrentTimezone());
          	    timeNow.setToNow();
            	long timeMillis = timeNow.toMillis(true);
            	if(timeMillis - getNotifyChibaoziTime() >= 2*60*60) {
            		setNotifyChibaoziTime(timeMillis);
            		//mLastChibaoziTime = timeMillis;
            		mNotificationManager.notify(0, createNotification().build());
            	}
            	
            	break;  
            case ON_CANCEL:
            	mNotificationManager.cancelAll();
            	break;
            default:  
                break;        
            }  
			super.handleMessage(msg);
		}
		
	};
	
	public Notification.Builder createNotification() {
		/*
		Intent resultIntent = new Intent(this, GameClient.class);
		//resultIntent.putExtra("BootFromService", true);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		Bitmap btm = BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher);
		Notification.Builder builder = new Notification.Builder(this)
        	.setContentTitle((String)getResources().getText(R.string.new_notification))
        	.setContentText((String)getResources().getText(R.string.chi_bao_zi))
        	.setSmallIcon(R.drawable.ic_launcher)
        	.setLargeIcon(btm)
        	.setContentIntent(resultPendingIntent);
		return builder;
		*/
		return null;
	}
	
	class GameBinder extends Binder {  
		  
        public void cancelNotification() {  
            Log.d(GameService.TAG, "cancelNotification() executed");  
            
            mNotificationManager.cancelAll();
            
            //Time timeNow = new Time(Time.getCurrentTimezone());
      	    //timeNow.setToNow();
      	    //setNotifyChibaoziTime(timeNow.toMillis(true));
        }  
  
    }  
	
	static long getNotifyChibaoziTime()
	{
		SharedPreferences sp = sService.getSharedPreferences(GameClient.NOTIFICAION_TAG, Activity.MODE_MULTI_PROCESS);
		return sp.getLong(TAG_CHIBAOZI_TIME, 0);
	}
	
	static void setNotifyChibaoziTime(long time) {
		Editor editor = sService.getSharedPreferences(GameClient.NOTIFICAION_TAG
				, Activity.MODE_MULTI_PROCESS).edit();
		editor.putLong(TAG_CHIBAOZI_TIME, time);
		editor.commit();
	}
}
