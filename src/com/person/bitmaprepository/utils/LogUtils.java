package com.person.bitmaprepository.utils;

import android.util.Log;

public class LogUtils {
private static final boolean mSwitch=true;
private static final String TAG="app";
public static void info(String tag,String msg){
	if(mSwitch){
		Log.i(tag, msg);
	}
}
public static void info(String msg){
	if(mSwitch){
		Log.i(TAG, msg);
	}
}
}
