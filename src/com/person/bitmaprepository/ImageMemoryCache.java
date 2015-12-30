package com.person.bitmaprepository;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;

import android.R.integer;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * @author Sony-Vaio
 *���ڴ��ȡ�����ٶ�������,Ϊ�˸����޶�ʹ���ڴ�,����ʹ�������㻺�档
 *Ӳ���û��治�����ױ����գ��������泣�����ݣ������õ�ת�������û��档
 */
public class ImageMemoryCache {
	private static LinkedHashMap<String, SoftReference<Bitmap>> mSoftCache;// �����û���
	private static LruCache<String, Bitmap> mLruCache;// Ӳ���û���
	private static final int SOFT_CACHE_SIZE = 15;// �����û�������

	public ImageMemoryCache(Context context) {

		int memClass = ((ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
		int cacheSize = 1024 * 1024 * memClass / 4;// Ӳ���û�������,Ϊ��������ڴ��1/4
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
				// Ӳ���û�����������ʱ��,�����LRU�㷨�����û�б�ʹ�õ�ͼƬת�������û���
				mSoftCache.put(key, new SoftReference<Bitmap>(oldValue));
			}
		};
		mSoftCache = new LinkedHashMap<String, SoftReference<Bitmap>>(
				SOFT_CACHE_SIZE, 0.75f, true) {
			private static final long serialVersionUID = -5737160474155996632L;

			@Override
			protected boolean removeEldestEntry(
					java.util.Map.Entry<String, SoftReference<Bitmap>> eldest) {
				//ÿ���������Ŀʱ�л���ɾ����ɵ���Ŀ
				if (size() > SOFT_CACHE_SIZE)
					return true;
				return false;
			}
		};
	}
	public Bitmap getBitmapFromCache(String url){
		Bitmap bitmap;
		//�ȴ�Ӳ���û����л�ȡ
		synchronized (mLruCache) {
			bitmap=mLruCache.get(url);
			if(bitmap!=null){
				//����ҵ��Ļ�,��Ԫ���ƶ�LinkedHashMap����ǰ��,�Ӷ���֤��LRU�㷨�������ɾ��
				mLruCache.remove(url);
				mLruCache.put(url,bitmap);
				return bitmap;
			}
		}
		//���Ӳ���û������Ҳ���,�������û�����ȥ��
		synchronized (mSoftCache) {
			SoftReference<Bitmap> bitmapReference=mSoftCache.get(url);
			if(bitmapReference!=null){
				bitmap=bitmapReference.get();
				if(bitmap!=null){
					//��ͼƬ�ƻ�Ӳ����
					mLruCache.put(url,bitmap);
					mSoftCache.remove(url);
					return bitmap;
				}else{
					mSoftCache.remove(url);
				}
			}
		}
		return null;
	}
	public void addBitmapToCache(String url,Bitmap bitmap){
		if(bitmap!=null){
			synchronized (mLruCache) {
				mLruCache.put(url,bitmap);
			}
		}
	}
	public void clearCache(){
		mSoftCache.clear();
	}
}
