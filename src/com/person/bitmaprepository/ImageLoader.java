package com.person.bitmaprepository;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.widget.ImageView;

import com.person.bitmaprepository.bean.HttpResult;
import com.person.bitmaprepository.utils.ImageHttpDao;
import com.person.bitmaprepository.utils.LogUtils;

/**
 * @author Administrator �����ֹͼƬ��λ��fileCache�Ĳ������
 *         hashmap��¼url���������Ĺ�ϵ��ImageHttpDao�����
 */
public class ImageLoader {
	private static ImageLoader loader;
	private ImageMemoryCache memoryCache;
	private ImageFileCache fileCache;
	private WeakHashMap<ImageView, String> mImageViews = new WeakHashMap<ImageView, String>();

	public static ImageLoader getInstance() {
		if (loader == null) {
			synchronized (ImageLoader.class) {
				if (loader == null)
					loader = new ImageLoader();
			}
		}
		return loader;
	}

	public void changeCacheDir(String path) {
		fileCache.setPath(path);
	}

	void addToMemoryCache(String url, Bitmap bitmap) {
		memoryCache.addBitmapToCache(url, bitmap);
	}

	void addToFileCache(String url, Bitmap bitmap) {
		fileCache.saveBitmap(url, bitmap);
	}

	public Bitmap getBitmapFromFileCache(String url) {
		return fileCache.getImage(url);
	}

	private ImageLoader() {
		fileCache = new ImageFileCache();
		memoryCache = new ImageMemoryCache();
	}

	boolean ivToURL(ImageView imageview, String url) {
		String tag = mImageViews.get(imageview);
		if (tag != null || tag.equals(url)) {
			LogUtils.info("imageview matchs url successfully");
			return true;
		}
		return false;
	}

	/**
	 * @param url
	 * @param iv
	 *            ����nullֵ
	 * @param listener
	 *            ����nullֵ
	 */
	public void loadImage(final String url, final ImageView iv,
			OnAysncGetListener<Bitmap> listener) {
		if (iv != null&&!TextUtils.isEmpty(url)) {
			mImageViews.put(iv, url);
		}

		// ���ڴ滺���л�ȡͼƬ
		Bitmap bitmap = memoryCache.getBitmapFromCache(url);
		if (bitmap != null && ivToURL(iv, url)) {
			iv.setImageBitmap(bitmap);
			return;
		} else {
			iv.setImageDrawable(iv.getResources().getDrawable(
					R.drawable.defaultimg));
		}
		// �첽��ȡ����
		ImageUrlAsyncTask task = new ImageUrlAsyncTask(iv, listener);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
		} else {
			task.execute(url);
		}
	}

	public interface OnAysncGetListener<T> {
		boolean onResult(HttpResult<T> result);
	}

	private static class ImageUrlAsyncTask extends
			AsyncTask<String, Void, HttpResult<Bitmap>> {
		private final WeakReference<ImageView> mImageViewRef;
		private final OnAysncGetListener<Bitmap> mListener;

		public ImageUrlAsyncTask(ImageView imageView,
				OnAysncGetListener<Bitmap> listener) {
			mImageViewRef = new WeakReference<ImageView>(imageView);
			this.mListener = listener;
		}

		@Override
		protected HttpResult<Bitmap> doInBackground(String... params) {
			Bitmap bitmap = ImageLoader.getInstance().getBitmapFromFileCache(
					params[0]);
			HttpResult<Bitmap> result = null;
			if (bitmap == null) {
				result = ImageHttpDao.getInstance().httpGet(params[0]);
				if (result.getResult() != null) {
					LogUtils.info("download image success");
					ImageLoader.getInstance().addToFileCache(params[0],
							result.getResult());
				}

			} else {
				result = new HttpResult<Bitmap>();
				result.setResult(bitmap);

			}
			result.setUrl(params[0]);
			return result;
		}

		@Override
		protected void onPostExecute(HttpResult<Bitmap> result) {
			super.onPostExecute(result);
			if (result.getResult() != null) {
				ImageLoader.getInstance().addToMemoryCache(result.getUrl(),
						result.getResult());
				if (mListener != null && mListener.onResult(result)) {
					return;
				} else {
					ImageView iv = mImageViewRef.get();
					if (iv != null
							&& ImageLoader.getInstance().ivToURL(iv,
									result.getUrl())) {
						iv.setImageBitmap(result.getResult());
					}
				}

			} else {
				ImageView iv = mImageViewRef.get();
				if (iv != null) {
					iv.setImageDrawable(iv.getResources().getDrawable(
							R.drawable.defaultimgerror));
				}
			}
		}
	}

}
