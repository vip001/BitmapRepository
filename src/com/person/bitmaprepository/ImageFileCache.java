package com.person.bitmaprepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

public class ImageFileCache {
	private static final String CACHE_SUFFIX = ".cache";
	private static final long CACHE_SIZE = 10 * 1024 * 1024;
	private static String path = "";
	private static long cacheSize;

	public ImageFileCache() {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {

			path = Environment.getExternalStorageDirectory().toString()
					+ File.separator + "ImgCache";
			this.setPath(path);

		}
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
		String filename = url.substring(url.lastIndexOf("/") + 1)
				+ CACHE_SUFFIX;
		String filepath = path + "/" + filename;
		File file = new File(filepath);
		if (file.exists()) {
			Bitmap bmp = BitmapFactory.decodeFile(filepath);
			if (bmp == null) {
				file.delete();
			} else {
				file.setLastModified(System.currentTimeMillis());
				return bmp;
			}
		}
		return null;
	}

	public void saveBitmap(Bitmap bm, String url) {
		if (bm == null) {
			return;
		}

		/*
		 * if (freeSpaceOnSd() < bm.getByteCount()) { return; }
		 */
		String filename = url.substring(url.lastIndexOf("/") + 1)
				+ CACHE_SUFFIX;
		File file = new File(path, filename);
		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
			out.flush();
			cacheSize +=file.length();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
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
		File dir = new File(path);
		File[] files = dir.listFiles();
		if (files == null || files.length == 0) {
			return;
		}
		cacheSize = 0;
		for (int i = 0; i < files.length; i++) {
			if (files[i].getName().contains(CACHE_SUFFIX)) {
				cacheSize += files[i].length();
			}
		}
	}

	private void checkCache() {
		if (cacheSize > CACHE_SIZE) {
			// 删除文件数的40%
			File dir = new File(path);
			File[] files = dir.listFiles();
			if (files == null || files.length == 0) {
				return;
			}
			int removeFactor = (int) ((0.4 * files.length) + 1);
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
			for (int i = 0; i < removeFactor; i++) {
				if (files[i].getName().contains(CACHE_SUFFIX)) {	
					cacheSize-=files[i].length();
					files[i].delete();
				}
			}
		}
	}

}
