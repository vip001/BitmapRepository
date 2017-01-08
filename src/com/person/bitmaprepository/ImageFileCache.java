package com.person.bitmaprepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.person.bitmaprepository.utils.LogUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

public class ImageFileCache {
	private static final String CACHE_SUFFIX = ".cache";
	private static final String TEMP_SUFFIX = ".temp";
	private static final long CACHE_SIZE = 10 * 1024 * 1024;
	private static final long RETAIN_FACTOR=(long) (CACHE_SIZE*0.6);
	private static String path = "";
	private static AtomicLong mCacheSize;
	private HashMap<String, ReentrantLock> mLockMap;
	private ReadWriteLock mRWLock;

	public ImageFileCache() {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {

			path = Environment.getExternalStorageDirectory().toString()
					+ File.separator + "ImgCache";
			this.setPath(path);

		}
		mLockMap = new HashMap<String, ReentrantLock>();
		mRWLock = new ReentrantReadWriteLock();
		countCacheSize();
		checkCache();
	}

	public String getPath() {
		return path;
	}

	public File getSystemCacheFile(Context context) {
		File file = context.getExternalCacheDir();
		if (file == null) {
			file = context.getCacheDir();
		}
		return file;
	}

	public void setPath(String path) {
		ImageFileCache.path = path;
		File file = new File(path);
		if (!file.exists()) {
			if (!file.mkdirs()) {
				Throwable throwable = new Throwable(
						"create imgcache directory fail");
				throwable.printStackTrace();
			}
		}

	}

	/**
	 * 计算sdcard上的剩余空间,单位byte 会发生数据溢出,得使用BigInteger
	 * 
	 * @return
	 */
	private long freeSpaceOnSd() {
		StatFs statFs = new StatFs(Environment.getExternalStorageDirectory()
				.getPath());
		return statFs.getAvailableBlocks() * statFs.getBlockSize();
	}

	/**
	 * 从缓存中获取图片
	 * 
	 * @param url
	 * @return
	 */
	public Bitmap getImage(String url) {
		if (TextUtils.isEmpty(url))
			return null;
		String filename = url.substring(url.lastIndexOf("/") + 1)
				+ CACHE_SUFFIX;
		String filepath = path + File.separator + filename;
		File file = new File(filepath);
		if (file.exists()) {
			Bitmap bmp = null;
			try {
				bmp = BitmapFactory.decodeStream(new FileInputStream(file));
			LogUtils.info("hit filecahe");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (bmp == null) {
				file.delete();
			} else {
				file.setLastModified(System.currentTimeMillis());
				return bmp;
			}
		}
		return null;
	}

	private ReentrantLock getLockForFileCache(String filename) {
		mRWLock.readLock().lock();
		ReentrantLock value = null;
		try {
			value = mLockMap.get(filename);
			if (value == null) {
				mRWLock.readLock().unlock();
				mRWLock.writeLock().lock();
				try {
					if (value == null) {
						value = new ReentrantLock();
						mLockMap.put(filename, value);
					}
					mRWLock.readLock().lock();
				} finally {
					mRWLock.writeLock().unlock();
				}

			}
		} finally {
			mRWLock.readLock().unlock();
		}
		return value;
	}

	public void saveBitmap(String url,Bitmap bm) {
		if (bm == null) {
			return;
		}

		/*
		 * if (freeSpaceOnSd() < bm.getByteCount()) { return; }
		 */
		String prefix = url.substring(url.lastIndexOf("/") + 1);

		File tempFile = new File(path, prefix + TEMP_SUFFIX);
		File cacheFile = new File(path, prefix + CACHE_SUFFIX);
		OutputStream out = null;
		ReentrantLock lock = getLockForFileCache(prefix + CACHE_SUFFIX);
		try {
			lock.lock();
			out = new FileOutputStream(tempFile);
			bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
			out.flush();
			if (tempFile.renameTo(cacheFile)) {
				mCacheSize.addAndGet(tempFile.length());
				LogUtils.info("save as file success");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					out = null;
				}
			}

		}

		checkCache();

	}

	private void countCacheSize() {
		mCacheSize = new AtomicLong(0);
		File dir = new File(path);
		File[] files = dir.listFiles();
		if (files == null || files.length == 0) {
			return;
		}

		for (int i = 0; i < files.length; i++) {
			if (files[i].getName().contains(CACHE_SUFFIX)) {
				mCacheSize.addAndGet(files[i].length());
			}
		}
	}

	private void checkCache() {
		if (mCacheSize.get() > CACHE_SIZE) {
			File dir = new File(path);
			File[] files = dir.listFiles();
			if (files == null || files.length == 0) {
				return;
			}

			Arrays.sort(files, new Comparator<File>() {

				@Override
				public int compare(File lhs, File rhs) {
					// TODO Auto-generated method stub
					if (lhs.lastModified() > rhs.lastModified()) {
						return 1;
					} else if (lhs.lastModified() == rhs.lastModified()) {
						return 0;
					} else {
						return -1;
					}
				}
			});
			for (int i = 0, len = files.length; i < len
					&& mCacheSize.get() > RETAIN_FACTOR; i++) {
				if (files[i].getName().contains(CACHE_SUFFIX)) {
					if (files[i].delete()) {
						mCacheSize.addAndGet(-files[i].length());
					}
				}
			}
			LogUtils.info("clear file cache");
		}
	}

}
