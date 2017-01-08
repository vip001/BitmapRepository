package com.person.bitmaprepository;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import com.person.bitmaprepository.utils.HardWareUtils;
import com.person.bitmaprepository.utils.LogUtils;

import android.R.integer;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Looper;
import android.support.v4.util.LruCache;

/**
 * @author Sony-Vaio 从内存读取数据速度是最快的,为了更大限度使用内存,这里使用了两层缓存。
 *         硬引用缓存不会轻易被回收，用来保存常用数据，不常用的转入软引用缓存。
 */
public class ImageMemoryCache {
	private static LinkedHashMap<String, SoftReference<Bitmap>> mSoftCache;// 软引用缓存
	private static LruCache<String, Bitmap> mLruCache;// 硬引用缓存
	private static final int SOFT_CACHE_SIZE = 15;// 软引用缓存容量
	public ImageMemoryCache() {

		int memClass = HardWareUtils.getMemory();
		int cacheSize = 1024 * 1024 * memClass / 4;// 硬引用缓存容量,为程序可用内存的1/4
		mLruCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				// TODO Auto-generated method stub
				if (value != null)
					return value.getRowBytes() * value.getHeight();
				else
					return 0;
			}

			@Override
			protected void entryRemoved(boolean evicted, String key,
					Bitmap oldValue, Bitmap newValue) {
				// 硬引用缓存容量满的时候,会根据LRU算法把最近没有被使用的图片转入软引用缓存
					mSoftCache.put(key, new SoftReference<Bitmap>(oldValue));
			}
		};
		mSoftCache = new LinkedHashMap<String, SoftReference<Bitmap>>(
				SOFT_CACHE_SIZE, 0.75f, true) {
			private static final long serialVersionUID = -5737160474155996632L;

			@Override
			protected boolean removeEldestEntry(
					java.util.Map.Entry<String, SoftReference<Bitmap>> eldest) {
				// 每次新添加条目时有机会删除最旧的条目
				if (size() > SOFT_CACHE_SIZE)
					return true;
				return false;
			}
		};
	}

	public Bitmap getBitmapFromCache(String url) {
		Bitmap bitmap;
		// 先从硬引用缓存中获取

		bitmap = mLruCache.get(url);
		if (bitmap != null) {
			LogUtils.info("hit strongreference cache");
			return bitmap;
		}
		// 如果硬引用缓存中找不到,到软引用缓存中去找

		SoftReference<Bitmap> bitmapReference = mSoftCache.get(url);
		if (bitmapReference != null) {
			bitmap = bitmapReference.get();
			if (bitmap != null) {
				// 将图片移回硬缓存
				mLruCache.put(url, bitmap);
				mSoftCache.remove(url);
				LogUtils.info("hit softreference cache");
				return bitmap;
			} else {
				mSoftCache.remove(url);
			}
		}

		return null;
	}
private void checkOnMainThread(){
	if(Looper.myLooper()!=Looper.getMainLooper()){
		throw new IllegalArgumentException("This  method should be called from the main/UI thread");
	}
}
	public void addBitmapToCache(String url, Bitmap bitmap) {
		if (bitmap != null) {
			checkOnMainThread();
				mLruCache.put(url, bitmap);
		}
	}

	public void clearCache() {
		checkOnMainThread();
		mSoftCache.clear();
	}
}
