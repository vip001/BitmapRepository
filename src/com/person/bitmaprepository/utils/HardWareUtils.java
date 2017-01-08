package com.person.bitmaprepository.utils;

import com.person.bitmaprepository.app.TestApplication;

import android.app.ActivityManager;
import android.content.Context;
import android.util.DisplayMetrics;

public class HardWareUtils {
	
public static int getMemory(){
	return ((ActivityManager)TestApplication.getInstance()
			.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
}
public static DisplayMetrics getDisplayMetrics(){
	return TestApplication.getInstance().getResources().getDisplayMetrics();
}
}
