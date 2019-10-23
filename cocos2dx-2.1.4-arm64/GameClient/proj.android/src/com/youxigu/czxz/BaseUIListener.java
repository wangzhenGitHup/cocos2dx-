package com.youxigu.czxz;

import org.json.JSONObject;

import com.tencent.tauth.IUiListener;
import com.tencent.tauth.UiError;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


public class BaseUIListener implements IUiListener {
	private static final int ON_COMPLETE = 0;
	private static final int ON_ERROR = 1;
	private static final int ON_CANCEL = 2;
	
	/*
	private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case ON_COMPLETE:
                JSONObject response = (JSONObject)msg.obj;
                Log.v("CZXZ", "onComplete: " + response.toString());
                break;
            case ON_ERROR:
                UiError e = (UiError)msg.obj;
                Log.v("CZXZ", "errorMsg: " + e.errorMessage 
                		+ "errorDetail:" + e.errorDetail);
                break;
            case ON_CANCEL:
            	Log.v("CZXZ", "onCancel");
                break;
            }
        }	    
	};
	*/

	@Override
	public void onComplete(Object response) {
		/*
	    Message msg = mHandler.obtainMessage();
	    msg.what = ON_COMPLETE;
	    msg.obj = response;
	    mHandler.sendMessage(msg);
	    */
		onResult(ON_COMPLETE, response.toString());
	}

	@Override
	public void onError(UiError e) {
		/*
	    Message msg = mHandler.obtainMessage();
        msg.what = ON_ERROR;
        msg.obj = e;
        mHandler.sendMessage(msg);
        */
		onResult(ON_ERROR, "errorMsg:" + e.errorMessage + ", errorDetail:" + e.errorDetail);
	}

	@Override
	public void onCancel() {
		/*
	    Message msg = mHandler.obtainMessage();
        msg.what = ON_CANCEL;
        mHandler.sendMessage(msg);
        */
		Log.v("CZXZ", "onCancel");
		onResult(ON_CANCEL, null);
	}

	public void onResult(int code, String msg) {
		
	}

}