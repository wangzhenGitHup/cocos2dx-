package com.youxigu.czxz;

public class TencentJniCallback {
	public static native void nativeLoginResultCallback(int code, String openID, String accessToken
			, String expiresIn, String payToken, String pf, String pfKey);
	
	public static native void nativePayResultCallback(int code);
	public static native void nativeMidCallback(String mid);
	public static native void nativeEnterGameScene();
}
