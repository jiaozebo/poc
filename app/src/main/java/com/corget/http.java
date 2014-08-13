package com.corget;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;


public class http {
	static private final int METHOD_GET = 0;
	static private final int METHOD_POST = 1;

	static {
		System.loadLibrary("poc");
	}
	

	static public native void HttpCallBack(long callback, HttpResponse response);

	
	static class ThreadHttp extends Thread {
		public ThreadHttp(String aUrl, int aMethod, String aHeaders,
				byte aData[], int aLen, long callback) {
			Url = aUrl;
			Method = aMethod;
			Headers = aHeaders;
			Data = aData;
			Len = aLen;
			CallBack = callback;
		}

		private String Url;
		private int Method;
		String Headers;
		byte Data[];
		int Len;
		long CallBack;
		HttpResponse response = null;

		public void run() {
			if (Method == METHOD_GET) {
				response = GetRequest(Url, Headers);
			} else if (Method == METHOD_POST) {
				response = PostRequest(Url, Headers, Data, Len);
			}
			int code = GetResponseCode(response);
			Log.i("poc", "GetResponseCode_thread:" + code);
			HttpCallBack(CallBack, response);
		}
	}

	
	
	static public int RequestAsync(String aUrl, int aMethod, String aHeaders,
			byte aData[], int aLen, long callback) {
		Log.i("poc", "RequestAsync" + aUrl);
		new ThreadHttp(aUrl, aMethod, aHeaders, aData, aLen, callback).start();
		return 0;
	}

	static public HttpResponse RequestSync(String aUrl, int aMethod,
			String aHeaders, byte aData[], int aLen) {
		Log.i("poc", "RequestSync" + aUrl);
		HttpResponse response = null;
		if (aMethod == METHOD_GET) {
			response = GetRequest(aUrl, aHeaders);
		} else if (aMethod == METHOD_POST) {
			response = PostRequest(aUrl, aHeaders, aData, aLen);
		}
		return response;
	}
	
	static public int GetResponseCode(HttpResponse response) {
		Log.i("poc", "GetResponseCode");
		if (response != null) {
			return response.getStatusLine().getStatusCode();
		}
		return -1;
	}

	
	static public String GetHeader(HttpResponse response, String aName) {
		if (response != null) {
			Header h = response.getFirstHeader(aName);
			if (h != null) {
				return h.getValue();
			}
		}
		return null;
	}

	static public int GetContentLength(HttpResponse response) {
		int res = 0;
		if (response != null) {
			HttpEntity h = response.getEntity();
			if (h != null) {
				res = (int) h.getContentLength();
			}
		}
		return res;
	}
	

	static public void Close(HttpResponse response) {

	}

	
	static public int ReadContent(HttpResponse response, byte aBuffer[], int aLen) {
		if(aLen <= 0){
			return 0;
		}

		if (response != null) {
			HttpEntity h = response.getEntity();
			if (h != null) {
				long length = h.getContentLength();
				if(length < aLen){
					aLen = (int)length;
				}
				try {
					InputStream in = h.getContent();
					int readLength = in.read(aBuffer);
					while(readLength < aLen ){
						int len = in.read(aBuffer, readLength, aLen - readLength);
						if(len < 0){
							return readLength;
						}
						readLength += len;
					}
					return readLength;
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return 0;
	}
	
	

	static private HttpResponse GetRequest(String aUrl, String aHeaders) {
		HttpClient client = null;
		if(!aUrl.startsWith("http://")){
			aUrl = "http://" + aUrl;
		}
		
		HttpGet httpRequest = new HttpGet(aUrl);
		try {
			if (aHeaders != null) {
				StringTokenizer strToken = new StringTokenizer(aHeaders, "\r\n");
				while (strToken.hasMoreTokens()) {
					String itemString = strToken.nextToken();
					int pos = itemString.indexOf(':');
					if (pos > 0) {
						httpRequest.addHeader(itemString.substring(0, pos)
								.trim(), itemString.substring(pos + 1).trim());
					}
				}
			}

			client = new DefaultHttpClient();
			return client.execute(httpRequest);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

		}
		return null;
	}

	static private HttpResponse PostRequest(String aUrl, String aHeaders,
			byte aData[], int aLen) {
		HttpClient client = null;
		HttpResponse response = null;
		if(!aUrl.startsWith("http://")){
			aUrl = "http://" + aUrl;
		}
		HttpPost httpRequest = new HttpPost(aUrl);
		client = new DefaultHttpClient();
		try {
			if (aHeaders != null) {
				StringTokenizer strToken = new StringTokenizer(aHeaders, "\r\n");
				while (strToken.hasMoreTokens()) {
					String itemString = strToken.nextToken();
					int pos = itemString.indexOf(':');
					String header_name = itemString.substring(0, pos).trim();
					String header_value = itemString.substring(pos + 1).trim();
					Log.i("poc", "header:" + header_name + ":" + header_value);
					httpRequest.addHeader(header_name, header_value);
				}
			}
			if (aLen > 0) {
				HttpEntity da = new ByteArrayEntity(aData);
				httpRequest.setEntity(da);
			}
			response = client.execute(httpRequest);
		} catch (Exception e) {
			Log.i("poc", e.getMessage());
		}
		return response;
	}

	
}
