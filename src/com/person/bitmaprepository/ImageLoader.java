package com.person.bitmaprepository;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.person.bitmaprepository.bean.HttpResult;
import com.person.bitmaprepository.utils.ImageHttpDao;

public class ImageLoader {
	private static ImageLoader loader;
	private ImageMemoryCache memoryCache;
	private ImageFileCache fileCache;
	private Context context;

	public interface OnHttpListener<T> {
		void onResult(HttpResult<T> result);
	}

	private ImageLoader(Context context) {
		fileCache = new ImageFileCache();
		memoryCache = new ImageMemoryCache(context);
		this.context = context;
	}

	public void loadImage(final String url, final ImageView iv) {
		// 从内存缓存中获取图片
		Bitmap bitmap = memoryCache.getBitmapFromCache(url);
		if (bitmap == null) {
			// 文件缓存中获取
			bitmap = fileCache.getImage(url);
			if (bitmap == null) {
				new Thread() {
					public void run() {
						final HttpResult<Bitmap> result = ImageHttpDao
								.getInstance(context).httpGet(url);
						iv.post(new Runnable() {
							public void run() {
								if (result.isSuccess()) {
									iv.setImageBitmap(result.getResult());
									fileCache.saveBitmap(result.getResult(),
											url);
									memoryCache.addBitmapToCache(url,
											result.getResult());
								} else {
									iv.setImageDrawable(context.getResources()
											.getDrawable(
													R.drawable.defaultimgerror));
								}
							}
						});

					}
				}.start();
			} else {
				iv.setImageBitmap(bitmap);
				memoryCache.addBitmapToCache(url, bitmap);
			}

		} else {
			iv.setImageBitmap(bitmap);
		}
	}

	public static ImageLoader getInstance(Context context) {
		if (loader == null) {
			synchronized (ImageLoader.class) {
				if (loader == null)
					loader = new ImageLoader(context);
			}
		}
		return loader;
	}

	public void changeCacheDir(String path) {
		fileCache.setPath(path);
	}

	public void loadImage(final String url,
			final OnHttpListener<Bitmap> callback) {
		HttpResult<Bitmap> result = new HttpResult<Bitmap>();
		// 从内存缓存中获取图片
		Bitmap bitmap = memoryCache.getBitmapFromCache(url);
		if (bitmap == null) {
			// 文件缓存中获取
			bitmap = fileCache.getImage(url);
			if (bitmap == null) {
				new Thread() {
					public void run() {
						HttpResult<Bitmap> res = ImageHttpDao.getInstance(
								context).httpGet(url);
						fileCache.saveBitmap(res.getResult(), url);
						memoryCache.addBitmapToCache(url, res.getResult());
						callback.onResult(res);
					}
				}.start();
			} else {
				result.setResult(bitmap);
				result.setSuccess(true);
				memoryCache.addBitmapToCache(url, bitmap);
				callback.onResult(result);
			}

		} else {
			result.setResult(bitmap);
			result.setSuccess(true);

			callback.onResult(result);
		}
	}
}
