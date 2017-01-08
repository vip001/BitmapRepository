package com.person.bitmaprepository.app;

import android.app.Application;

public class TestApplication extends Application {
	private static TestApplication mInstance;

	@Override
	public void onCreate() {
		super.onCreate();
		mInstance = this;
	}

	public static TestApplication getInstance() {
		return mInstance;
	}
}
