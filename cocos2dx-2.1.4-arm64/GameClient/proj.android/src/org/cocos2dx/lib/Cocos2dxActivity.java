/****************************************************************************
Copyright (c) 2010-2013 cocos2d-x.org

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
package org.cocos2dx.lib;

import org.cocos2dx.lib.Cocos2dxHelper.Cocos2dxHelperListener;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import org.cocos2dx.lib.Cocos2dxActivity;
import org.cocos2dx.lib.Cocos2dxGLSurfaceView;

public abstract class Cocos2dxActivity extends Activity implements Cocos2dxHelperListener {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final String TAG = Cocos2dxActivity.class.getSimpleName();

	// ===========================================================
	// Fields
	// ===========================================================
	
	protected Cocos2dxGLSurfaceView mGLSurfaceView;
	private Cocos2dxHandler mHandler;
	private static Context sContext = null;
	protected FrameLayout mFramelayout;
	protected View mLaunchingView = null;
	protected boolean mInitDone = false;
	
	public static Context getContext() {
		return sContext;
	}
	
	// ===========================================================
	// Constructors
	// ===========================================================
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sContext = this;
		this.mHandler = new Cocos2dxHandler(this);
		
        ViewGroup.LayoutParams framelayout_params =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                                           ViewGroup.LayoutParams.FILL_PARENT);
        mFramelayout = new FrameLayout(this);
        mFramelayout.setLayoutParams(framelayout_params);
        this.setContentView(mFramelayout);
	}
	
	protected void initCocos2dx() {
		
	}
	
	protected void LoadMyLibrary() {
    	System.loadLibrary("game");
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onResume() {
		super.onResume();

		if(mInitDone) {
			Cocos2dxHelper.onResume();
			this.mGLSurfaceView.onResume();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if(mInitDone) {
			Cocos2dxHelper.onPause();
			this.mGLSurfaceView.onPause();
		}
	}

	@Override
	public void showDialog(final String pTitle, final String pMessage) {
		Message msg = new Message();
		msg.what = Cocos2dxHandler.HANDLER_SHOW_DIALOG;
		msg.obj = new Cocos2dxHandler.DialogMessage(pTitle, pMessage);
		this.mHandler.sendMessage(msg);
	}

	@Override
	public void showEditTextDialog(final String pTitle, final String pContent, final int pInputMode, final int pInputFlag, final int pReturnType, final int pMaxLength) { 
		Message msg = new Message();
		msg.what = Cocos2dxHandler.HANDLER_SHOW_EDITBOX_DIALOG;
		msg.obj = new Cocos2dxHandler.EditBoxMessage(pTitle, pContent, pInputMode, pInputFlag, pReturnType, pMaxLength);
		this.mHandler.sendMessage(msg);
	}
	
	@Override
	public void runOnGLThread(final Runnable pRunnable) {
		this.mGLSurfaceView.queueEvent(pRunnable);
	}

	// ===========================================================
	// Methods
	// ===========================================================
	public void init() {
		Log.v("CZXZ", "init...");

        // Cocos2dxEditText layout
        ViewGroup.LayoutParams edittext_layout_params =
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                                       ViewGroup.LayoutParams.WRAP_CONTENT);
        Cocos2dxEditText edittext = new Cocos2dxEditText(this);
        edittext.setLayoutParams(edittext_layout_params);

        // ...add to FrameLayout
        mFramelayout.addView(edittext, 0);

        // Cocos2dxGLSurfaceView
        this.mGLSurfaceView = this.onCreateView();
        mGLSurfaceView.setZOrderOnTop(true);
        
        // ...add to FrameLayout
        mFramelayout.addView(this.mGLSurfaceView);

        //����͸�� modify by shipeng
        mGLSurfaceView.setZOrderOnTop(true);
        mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		
		mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		
        this.mGLSurfaceView.setCocos2dxRenderer(new Cocos2dxRenderer());
        this.mGLSurfaceView.setCocos2dxEditText(edittext);
	}
	
    public Cocos2dxGLSurfaceView onCreateView() {
    	return new Cocos2dxGLSurfaceView(this);
		//Cocos2dxGLSurfaceView glSurfaceView = new Cocos2dxGLSurfaceView(this);
		//glSurfaceView.setEGLConfigChooser(5, 6, 5, 0, 16, 8);
		//return glSurfaceView;
    }

   private final static boolean isAndroidEmulator() {
      String model = Build.MODEL;
      Log.d(TAG, "model=" + model);
      String product = Build.PRODUCT;
      Log.d(TAG, "product=" + product);
      boolean isEmulator = false;
      if (product != null) {
         isEmulator = product.equals("sdk") || product.contains("_sdk") || product.contains("sdk_");
      }
      Log.d(TAG, "isEmulator=" + isEmulator);
      return isEmulator;
   }

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
