package com.person.bitmaprepository.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

import com.person.bitmaprepository.bean.HttpResult;

public class ImageHttpDao {
	private static ImageHttpDao dao;
	private int screenwidth;
	private int screenheight;
	private int reqwidth;
	private int reqheight;
	private int samplesize;

	public static ImageHttpDao getInstance() {
		if (dao == null) {
			synchronized (ImageHttpDao.class) {
				if (dao == null) {
					dao = new ImageHttpDao();
				}
			}
		}
		return dao;
	}

	private ImageHttpDao() {
		DisplayMetrics metrics = HardWareUtils.getDisplayMetrics();
		screenwidth = metrics.widthPixels;
		screenheight = metrics.heightPixels;
		reqwidth = screenwidth;
		reqheight = 1;
		samplesize = 1 * 1024 * 1024;
	}

	public int getScreenwidth() {
		return screenwidth;
	}

	public int getScreenheight() {
		return screenheight;
	}

	public void setSampleParams(Integer reqwidth, Integer reqheight,
			Integer samplesize) {
		this.reqwidth = reqwidth == null ? this.reqwidth : reqwidth;
		this.reqheight = reqheight == null ? this.reqheight : reqheight;
		this.samplesize = samplesize == null ? this.samplesize : samplesize;
	}

	public void resetSampleParams() {
		reqwidth = screenwidth;
		reqheight = 1;
		samplesize = 1 * 1024 * 1024;
	}

	public int getReqwidth() {
		return reqwidth;
	}

	public void setReqwidth(int reqwidth) {
		this.reqwidth = reqwidth;
	}

	public int getReqheight() {
		return reqheight;
	}

	public void setReqheight(int reqheight) {
		this.reqheight = reqheight;
	}

	public int getSamplesize() {
		return samplesize;
	}

	public void setSamplesize(int samplesize) {
		this.samplesize = samplesize;
	}

	public HttpResult<Bitmap> httpGet(String url) {
		HttpURLConnection conn = null;
		HttpResult<Bitmap> result = new HttpResult<Bitmap>();
		InputStream is = null;
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(1000 * 30);
			conn.connect();
			int responsecode = conn.getResponseCode();
			if (responsecode == 200) {
				is = conn.getInputStream();
				Bitmap bitmap = null;
				if (is.available() > samplesize) {
					bitmap = BitmapUtils.decodeBitmap(is, reqwidth, reqheight);
				} else {
					bitmap = BitmapFactory.decodeStream(is);
				}
				result.setSuccess(true);
				result.setResult(bitmap);
			} else {
				result.setSuccess(false);
				result.setErrorCode(responsecode);
				result.setErrorMsg("获取图片失败!");
			}

		} catch (Exception e) {
			e.printStackTrace();
			result.setSuccess(false);
			result.setErrorMsg("网络连接异常");
			result.setErrorCode(00);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					is = null;
				}
			}
			if (conn != null) {
				conn.disconnect();
				conn = null;
			}

		}
		return result;
	}
}
