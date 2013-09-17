package com.jcheed06.myhealthapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Base64;

public final class RestClient {
	public static String API_URL = ""; // TODO : Add URL
	public static String CHARSET = "UTF-8";
		
	private String username;
	private String password;
	
	public RestClient(String username, String password){
		this.username = username;
		this.password = password;
	}
	
	public boolean login(){
		try{
			JSONObject result = this.put("/login");
			
			return result.getBoolean(MainActivity.REST_CALL_SUCCESS);
			
		}catch(JSONException ex){
			return false;
		}
		
	}
	
	public JSONObject get(String location) throws JSONException {
		try {
			HttpURLConnection httpConn = this.getHttpConnection(location);
			httpConn.connect();
			String json = this.convertStreamToString(httpConn.getInputStream());
			httpConn.disconnect();
			return (JSONObject) new JSONTokener(json).nextValue();
		}
		catch(Exception e) {
			return this.getJSONErrorObject();
		}
	}
	
	public JSONObject put(String location) throws JSONException {
		try {
			HttpURLConnection httpConn = this.getHttpConnection(location);
			httpConn.setRequestMethod("PUT");
			httpConn.connect();
			String json = this.convertStreamToString(httpConn.getInputStream());
			httpConn.disconnect();
			return (JSONObject) new JSONTokener(json).nextValue();
		}
		catch(Exception e) {
			return this.getJSONErrorObject();
		}
	}
	
	public JSONObject delete(String location) throws JSONException {
		try {
			HttpURLConnection httpConn = this.getHttpConnection(location);
			httpConn.setRequestMethod("DELETE");
			httpConn.connect();
			String json = this.convertStreamToString(httpConn.getInputStream());
			httpConn.disconnect();
			return (JSONObject) new JSONTokener(json).nextValue();
		}
		catch(Exception e) {
			return this.getJSONErrorObject();
		}
	}
	
	public JSONObject post(String location, ArrayList<NameValuePair> params) throws JSONException {
		try {
			HttpURLConnection httpConn = this.getHttpConnection(location);
			httpConn.setDoOutput(true);
			httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset="+RestClient.CHARSET);
			httpConn.connect();
			this.writeParams(httpConn.getOutputStream(), params);
			String json = this.convertStreamToString(httpConn.getInputStream());
			httpConn.disconnect();
			return (JSONObject) new JSONTokener(json).nextValue();
		}
		catch(Exception e) {
			return this.getJSONErrorObject();
		}
	}
	
	private JSONObject getJSONErrorObject() throws JSONException{
		return new JSONObject("{success : false}");
	}
	
	private String getB64Auth(){
		String credentials = this.username+":"+this.password;
		String b64credentials = Base64.encodeToString(credentials.getBytes(), Base64.URL_SAFE|Base64.NO_WRAP);
		return "Basic "+b64credentials;
	}
	
	private void writeParams(OutputStream os, ArrayList<NameValuePair> params) throws IOException{
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		
		for (NameValuePair pair : params){
			if (first){
				first = false;
			}else{
				sb.append("&");
			}
			
			sb.append(String.format("%s=%s", 
					URLEncoder.encode(pair.getName(), RestClient.CHARSET),
					URLEncoder.encode(pair.getValue(), RestClient.CHARSET)));
			
		}
		
		os.write(sb.toString().getBytes(RestClient.CHARSET));
		os.close();
	}
	
	private String convertStreamToString(InputStream is) throws IOException {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();
	    String line = null;

	    while ((line = reader.readLine()) != null) {
	        sb.append(line);
	    }

	    is.close();

	    return sb.toString();
	}
	
	private HttpURLConnection getHttpConnection(String location) throws IOException {
		URL url = new URL(RestClient.API_URL+location);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setChunkedStreamingMode(0);
		httpConn.addRequestProperty("Authorization", getB64Auth());
		return httpConn;
	}
}
