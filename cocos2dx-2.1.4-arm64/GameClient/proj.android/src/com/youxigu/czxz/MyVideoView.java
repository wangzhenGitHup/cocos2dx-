package com.youxigu.czxz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.VideoView;

public class MyVideoView extends VideoView {

	private int mVideoWidth = 0;  
    private int mVideoHeight = 0;  
    private int mStopPosition = -1;

	public MyVideoView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@SuppressLint("WrongCall")
	public void setVideoAspect(int w,int h){
		mVideoWidth = w;
		mVideoHeight = h;
	    onMeasure(w, h);
	}
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		int width = wm.getDefaultDisplay().getWidth();
		int height = wm.getDefaultDisplay().getHeight();
		super.onMeasure(width, height);
		setMeasuredDimension(width, height);
		/*super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if(mVideoWidth != 0 && mVideoHeight !=0 ) {
			Log.w("peng", "mVideoWidth=" + mVideoWidth + ", mVideoHeight" + mVideoHeight);
			setMeasuredDimension(mVideoWidth, mVideoHeight);
		}*/
	}

	@Override
	public void pause() {
		if(mStopPosition != -1)
		{
			mStopPosition = this.getCurrentPosition();
		}

		super.pause();
	}

	@Override
	public void stopPlayback() {
		mStopPosition = -1;
		super.stopPlayback();
		this.setVisibility(View.INVISIBLE);
	}
	
	@Override
	public void start() {
		if(this.getVisibility() == View.INVISIBLE)
		{
			this.setVisibility(View.VISIBLE);
		}

		mStopPosition = 0;
		super.start();
	}

	public void restart()
	{
		if(mStopPosition >= 0)
		{
			this.seekTo(mStopPosition);
			this.start();
		}
	}
}
