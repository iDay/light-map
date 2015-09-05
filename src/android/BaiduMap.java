package com.baidu.light.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.AbsoluteLayout.LayoutParams;

public class BaiduMap extends CordovaPlugin {
	private static final String LOG_TAG = "BaiduMap";
	private static final boolean DEBUG = true;

	private static Handler mHandler = new Handler(Looper.getMainLooper());
	private static MapView mapView;
	private static LocationClient mLocClient;
	private static Map<String, BitmapDescriptor> icons = new HashMap<>();

	private CallbackContext mCallbackContext = null;

	/**
	 * Executes the request and returns PluginResult.
	 *
	 * @param action
	 *            The action to execute.
	 * @param args
	 *            JSONArry of arguments for the plugin.
	 * @param callbackContext
	 *            The callback id used when calling back into JavaScript.
	 * @return True if the action was valid, false if not.
	 */
	@SuppressWarnings("unchecked")
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if (DEBUG) {
			Log.d(LOG_TAG, String.format("execute({ action: %s, args: %s })", action, args));
		}

		if ("init".equals(action)) {
			if (args == null) {
				return false;
			}
			
			if (mapView != null) {
				return false;
			}

			JSONObject params = args.optJSONObject(0);
			JSONObject center = params.optJSONObject("center");

			int left = params.optInt("left");
			int top = params.optInt("top");
			int width = params.optInt("width");
			int height = params.optInt("height");
			String guid = params.optString("id");
			int zoom = params.optInt("zoom");
			createMap(guid, left, top, width, height, (float) center.optDouble("longitude"), (float) center.optDouble("latitude"), zoom);
			mCallbackContext = callbackContext;
		}
		if ("setCenter".equals(action)) {
			if (args == null) {
				return false;
			}
			JSONObject params = args.optJSONObject(0);
			float longitude = (float)params.optDouble("longitude");
			float latitude = (float)params.optDouble("latitude");
			mapView.getMap().setMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
		}
		if ("setZoom".equals(action)) {
			if (args == null) {
				return false;
			}
			JSONObject params = args.optJSONObject(0);
			float zoom = (float)params.optDouble("zoom");
			mapView.getMap().setMapStatus(MapStatusUpdateFactory.zoomTo(zoom));
		}
		return true;
	}

	/**
	 * @param cordova
	 *            The context of the main Activity.
	 * @param webView
	 *            The associated CordovaWebView.
	 */
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		if (DEBUG) {
			Log.d(LOG_TAG, String.format("initialize()"));
		}
		SDKInitializer.initialize(webView.getContext().getApplicationContext());
	}

	public void createMap(String guid, int left, int top, int width,
			int height, float lng, float lat, int zoom) {
		if (DEBUG) {
			Log.d(LOG_TAG,
					String.format(
							"createMap(guid: %s, left: %s, top: %s, width: %s, height: %s, lng: %s, lat: %s, zoom: %s)",
							guid, left, top, width, height, lng, lat, zoom));
		}

		mHandler.post(new Runnable() {
			private String mGuid;
			private int mLeft;
			private int mTop;
			private int mWidth;
			private int mHeight;
			private float mLng;
			private float mLat;
			private int mZoom;

			public Runnable config(String guid, int left, int top, int width,
					int height, float lng, float lat, int zoom) {
				mGuid = guid;
				mLeft = left;
				mTop = top;
				mHeight = height;
				mWidth = width;
				mLng = lng;
				mLat = lat;
				mZoom = zoom;
				return this;
			}

			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				mapView = new MapView(BaiduMap.this.webView
						.getContext());
				mapView.getMap().setMapStatus(
						MapStatusUpdateFactory.newLatLngZoom(new LatLng(mLat, mLng), mZoom));

				float scale = BaiduMap.this.webView.getScale();

				LayoutParams params = new LayoutParams((int) (mWidth * scale),
						(int) (mHeight * scale), (int) (mLeft * scale),
						(int) (mTop * scale));
				mapView.setLayoutParams(params);
				mapView.showZoomControls(true);
				BaiduMap.this.webView.addView(mapView);
			}

		}.config(guid, left, top, width, height, lng, lat, zoom));
	}

	private void clearMarker() {
		mapView.getMap().clear();
	}

	private void addMarker(JSONObject params) {
		float longitude = (float)params.optDouble("longitude");
		float latitude = (float)params.optDouble("latitude");
		String iconPath = params.optString("icon");
		BitmapDescriptor icon;
		if (icons.get(iconPath) != null) {
			icon = icons.get(iconPath);
		} else {
			icon = BitmapDescriptorFactory.fromPath(iconPath);
			icons.put(iconPath, icon);
		}
		
		mapView.getMap().addOverlay(new MarkerOptions().position(
				new LatLng(longitude, latitude)).icon(icon));
	}

}
