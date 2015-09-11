package com.baidu.light.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.io.InputStream;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMapStatusChangeListener;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.baidu.mapapi.utils.CoordinateConverter.CoordType;

import com.baidu.mapapi.map.MapViewLayoutParams;
import com.baidu.mapapi.map.MapViewLayoutParams.Builder;

import android.view.View;
import android.view.ViewGroup;
import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.AbsoluteLayout.LayoutParams;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.graphics.drawable.Drawable;
import android.graphics.Point;


public class BaiduMap extends CordovaPlugin implements OnMapStatusChangeListener, OnMarkerClickListener {
	private static final String LOG_TAG = "BaiduMap";
	private static final boolean DEBUG = true;
	private static Handler mHandler = new Handler(Looper.getMainLooper());
	private static MapView mapView;
	private static LocationClient mLocClient;
	private static Map<String, BitmapDescriptor> icons = new HashMap<String, BitmapDescriptor>();
	private static Map<String, Marker> markers = new HashMap<String, Marker>();

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
		} else if ("setCenter".equals(action)) {
			if (args == null) {
				return false;
			}
			JSONObject params = args.optJSONObject(0);
			float longitude = (float)params.optDouble("longitude");
			float latitude = (float)params.optDouble("latitude");
			mapView.getMap().setMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
		} else if ("setZoom".equals(action)) {
			if (args == null) {
				return false;
			}
			JSONObject params = args.optJSONObject(0);
			float zoom = (float)params.optDouble("zoom");
			mapView.getMap().setMapStatus(MapStatusUpdateFactory.zoomTo(zoom));
		} else if ("convert".equals(action)) {
			if (args == null) {
				return false;
			}
			JSONObject params = args.optJSONObject(0);
			float longitude = (float)params.optDouble("longitude");
			float latitude = (float)params.optDouble("latitude");
			String type = args.optString(1);
			convert(new LatLng(latitude, longitude), type, callbackContext);
		} else if ("revert".equals(action)) {
			if (args == null) {
				return false;
			}
			JSONObject params = args.optJSONObject(0);
			float longitude = (float)params.optDouble("longitude");
			float latitude = (float)params.optDouble("latitude");
			revert(new LatLng(latitude, longitude), callbackContext);
		} else if ("close".equals(action)) {
			close();
	    } else if ("getZoom".equals(action)) {
	    	getZoom(callbackContext);
	    } else if ("addMarker".equals(action)) {
	    	if (args == null) {
				return false;
			}
			JSONObject params = args.optJSONObject(0);
			addMarker(params);
	    } else if ("addImage".equals(action)) {
	    	if (args == null) {
				return false;
			}
	    	JSONObject params = args.optJSONObject(0);
			addImage(params);
	    } else if ("clearMarker".equals(action)) {
	    	clearMarker();
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
				if (mapView != null) {
					if (mapView.getParent() == null) {
						BaiduMap.this.webView.addView(mapView);
						mapView.onResume();
						return;
					}
					return;
				}

				mapView = new MapView(BaiduMap.this.cordova.getActivity());
				mapView.getMap().setMapStatus(
						MapStatusUpdateFactory.newLatLngZoom(new LatLng(mLat, mLng), mZoom));

//				float scale = BaiduMap.this.webView.getScale();
				float scale = BaiduMap.this.cordova.getActivity().getResources().getDisplayMetrics().density;

				LayoutParams params = new LayoutParams((int) (mWidth * scale),
						(int) (mHeight * scale), (int) (mLeft * scale),
						(int) (mTop * scale));
				mapView.setLayoutParams(params);
				mapView.showZoomControls(true);
				BaiduMap.this.webView.addView(mapView);
				mapView.getMap().setOnMapStatusChangeListener(BaiduMap.this);
				mapView.getMap().setOnMarkerClickListener(BaiduMap.this);
				
				if (mCallbackContext != null) {
					mCallbackContext.success();
					mCallbackContext = null;
				}
			}

		}.config(guid, left, top, width, height, lng, lat, zoom));
	}

	private void clearMarker() {
		markers.clear();
		mapView.getMap().clear();
	}

	private void addMarker(JSONObject params) {
		float longitude = (float)params.optDouble("longitude");
		float latitude = (float)params.optDouble("latitude");
		String iconPath = params.optString("icon");
		String id = params.optString("id");
		BitmapDescriptor icon;
		if (icons.get(iconPath) != null) {
			icon = icons.get(iconPath);
		} else {
			icon = BitmapDescriptorFactory.fromAsset(iconPath);
			icons.put(iconPath, icon);
		}
		
		Marker marker = (Marker)mapView.getMap().addOverlay(new MarkerOptions().position(
				new LatLng(latitude, longitude)).icon(icon));
		markers.put(id, marker);
	}
	
	private LatLng convert(LatLng sourceLatLng, String coordType,final CallbackContext callback) throws JSONException {
		CoordinateConverter converter  = new CoordinateConverter();  
		converter.from(CoordType.valueOf(coordType));
		converter.coord(sourceLatLng);
		LatLng desLatLng = converter.convert();
		
		JSONObject result = new JSONObject();
	    result.put("latitude", desLatLng.latitude);
	    result.put("longitude", desLatLng.longitude);
	    if (callback != null) {
	    	callback.success(result);
	    }
		return desLatLng;
	}
	
	private void revert(LatLng sourceLatLng, final CallbackContext callback) throws JSONException {
		LatLng midLatlng = convert(sourceLatLng, "GPS", null);
		float latitude = (float)(sourceLatLng.latitude * 2 - midLatlng.latitude);
		float longitude = (float)(sourceLatLng.longitude * 2 - midLatlng.longitude);

		JSONObject result = new JSONObject();
	    result.put("latitude", latitude);
	    result.put("longitude", longitude);
	    if (callback != null) {
	    	callback.success(result);
	    }
	}
	
	private void close() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mapView != null) {
					mapView.onPause();
					ViewGroup vg = (ViewGroup)mapView.getParent();
					if (vg != null) {
						vg.removeView(mapView);
					}
				}
			}
		});
	}
	
	/** 
    * 手势操作地图，设置地图状态等操作导致地图状态开始改变。 
    * @param status 地图状态改变开始时的地图状态 
    */  
    public void onMapStatusChangeStart(MapStatus status){  
    }  
    /** 
    * 地图状态变化中 
    * @param status 当前地图状态 
    */  
    public void onMapStatusChange(MapStatus status){  
    }  
    /** 
    * 地图状态改变结束 
    * @param status 地图状态改变结束后的地图状态 
    */  
    public void onMapStatusChangeFinish(MapStatus status){
		LatLng latlng = status.target;
		final String receiveHook = "light.map.onMapStatusChange({latitude:" + 
				latlng.latitude + ",longitude:" + latlng.longitude + ", zoom: " + 
				status.zoom + "})";
		if (BaiduMap.this.cordova == null) {
			return;
		}
		BaiduMap.this.cordova.getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				BaiduMap.this.webView.loadUrl("javascript:" + receiveHook);
			}
			
		});
    }
    
    private void getZoom(final CallbackContext callback) {
    	float zoom = mapView.getMap().getMapStatus().zoom;
    	callback.success((int)zoom);
    }
    
    private void addImage(JSONObject params) {
    	float scale = BaiduMap.this.cordova.getActivity().getResources().getDisplayMetrics().density;
    	final int width = (int)(params.optInt("width") * scale);
    	final int height = (int)(params.optInt("height") * scale);
    	final int left = (int)(params.optInt("left") * scale);
    	final int top = (int)(params.optInt("top") * scale);
		final String imagePath = params.optString("image");
		final boolean clickable = params.optBoolean("clickable");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
			    	ImageView imageView = new ImageView(BaiduMap.this.cordova.getActivity());
			    	InputStream is = cordova.getActivity().getAssets().open(imagePath);  
			    	Drawable drawable = Drawable.createFromStream(is, null);
			    	imageView.setImageDrawable(drawable);
					if (clickable) {
						imageView.setOnClickListener(new View.OnClickListener() {
							
							@Override
							public void onClick(View view) {
								BaiduMap.this.webView.loadUrl("javascript:light.map.onImageClick('" + imagePath + "')");
							}
						});
					}
//			    	BaiduMap.this.webView.addView(imageView);
			    	MapViewLayoutParams.Builder lm = new MapViewLayoutParams.Builder();
			    	lm.align(MapViewLayoutParams.ALIGN_CENTER_HORIZONTAL, MapViewLayoutParams.ALIGN_CENTER_VERTICAL)
			    	.width(width).height(height).point(new Point(left, top));
			    	mapView.addView(imageView, lm.build());
//			    	mapView.refreshDrawableState();
		    	} catch(Exception e) {
		    		e.printStackTrace();
		    	}
			}
		});
    }
    
    public boolean onMarkerClick(final Marker marker) {
		if (BaiduMap.this.cordova == null) {
			return false;
		}
		BaiduMap.this.cordova.getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
		    	String id = null;
		    	for (Map.Entry<String, Marker> entry : BaiduMap.this.markers.entrySet()) {
					if (entry.getValue().equals(marker)) {
						id = entry.getKey();
						break;
					}
				}
				BaiduMap.this.webView.loadUrl("javascript:light.map.onMarkerClick('" + id + "')");
			}
			
		});
		return true;
    }
    
}
