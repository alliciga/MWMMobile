package com.ASWS.mwmmobile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.VersionInfo;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.baidu.navisdk.BNaviEngineManager.NaviEngineInitListener;
import com.baidu.navisdk.BaiduNaviManager;
import com.baidu.navisdk.comapi.tts.BNTTSPlayer;
import com.baidu.navisdk.model.datastruct.LocData;
import com.baidu.navisdk.model.datastruct.SensorData;
import com.baidu.navisdk.ui.routeguide.BNavigator;
import com.baidu.navisdk.ui.routeguide.IBNavigatorListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	//常量定义
	String URL_CREATE_POI="http://api.map.baidu.com/geodata/v3/poi/create";
	String URL_UPDATE_POI="http://api.map.baidu.com/geodata/v3/poi/update";
	String URL_LIST_POI="http://api.map.baidu.com/geodata/v3/poi/list";

	//服务器AK，可用于LBS存储API
	String AK="AHlXvdQ2AcfSy3KAixNEOfOx";
	String GEOTABLE_ID="92292";
	String COORD_TYPE="3";

	DefaultHttpClient httpclient = new DefaultHttpClient();

	//百度地图控件
	private MapView mMapView;
	private BaiduMap mBaiduMap;
	private LocationClient mLocationClient = null;
	private BitmapDescriptor mCurrentMarker; 
	private SDKReceiver mReceiver;
	private BDLocationListener myListener = new MyLocationListener();

	//本地变量
	private boolean isFirstLoc = true; //是否首次定位
	private int NotifyRadius = 250; //电子围栏半径
	private double curLatitude = 0.0;
	private double curLongitude = 0.0;
	private String curHospital = "";
	private HashMap<String, String> possibleHospitalList = new HashMap<String, String>(); //当前所在位置可能的医疗机构列表
	private String tempHospital = "";
	private String curPOIID = "";
	private String curVehicle = "";
	private String voiceText = "";
	private int voiceControl = 0;

	private Button checkinButton;
	private TextView mStateBar;

	JSONArray pois = null;
	
	//导航相关
	private boolean mIsEngineInitSuccess = false;  
	private NaviEngineInitListener mNaviEngineInitListener = new NaviEngineInitListener() {  
	        public void engineInitSuccess() {  
	            //导航初始化是异步的，需要一小段时间，以这个标志来识别引擎是否初始化成功，为true时候才能发起导航  
	            mIsEngineInitSuccess = true;  
	        }
	        public void engineInitStart() {}
	        public void engineInitFail() {}
	};
	
	private String getSdcardDir() {  
	    if (Environment.getExternalStorageState().equalsIgnoreCase(  
	            Environment.MEDIA_MOUNTED)) {  
	        return Environment.getExternalStorageDirectory().toString();  
	    }  
	    return null;  
	}
	 
	private IBNavigatorListener mBNavigatorListener = new IBNavigatorListener() {  
	    	 
	        @Override  
	        public void onYawingRequestSuccess() {  
	            // TODO 偏航请求成功
	        }
	 
	        @Override  
	        public void onYawingRequestStart() {  
	            // TODO 开始偏航请求
	        }
	 
	        @Override  
	        public void onPageJump(int jumpTiming, Object arg) {  
	            // TODO 页面跳转回调  
	            if(IBNavigatorListener.PAGE_JUMP_WHEN_GUIDE_END == jumpTiming){  
	                finish();  
	            }else if(IBNavigatorListener.PAGE_JUMP_WHEN_ROUTE_PLAN_FAIL == jumpTiming){  
	                finish();  
	            }
	        }
	        
	        @Override  
	        public void notifyGPSStatusData(int arg0) {  
	            // TODO Auto-generated method stub
	        }
	 
	        @Override  
	        public void notifyLoacteData(LocData arg0) {  
	            // TODO Auto-generated method stub
	        }
	 
	        @Override  
	        public void notifyNmeaData(String arg0) {  
	            // TODO Auto-generated method stub
	        }
	 
	        @Override  
	        public void notifySensorData(SensorData arg0) {  
	            // TODO Auto-generated method stub
	        }
	 
	        @Override  
	        public void notifyStartNav() {  
	            // TODO Auto-generated method stub  
	            BaiduNaviManager.getInstance().dismissWaitProgressDialog();  
	        }
	 
	        @Override  
	        public void notifyViewModeChanged(int arg0) {  
	            // TODO Auto-generated method stub
	        }
	};
	
	
	//初始化入口
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		//读取身份注册信息
		SharedPreferences sharedata = getSharedPreferences("mwmdata", 0);  
		String data = sharedata.getString("vehicle", null);
		curVehicle = data;
		if(curVehicle == null) {
			curVehicle = "未设置";
		}

		mStateBar = (TextView) findViewById(R.id.text_State);
		mStateBar.setTextColor(Color.GREEN);
		mStateBar.setText("欢迎使用清运车辆监控手机版，百度SDK v"
				+ VersionInfo.getApiVersion()
				+ "\n登录车辆："
				+ curVehicle);

		// 注册 SDK 广播监听者
		IntentFilter iFilter = new IntentFilter();
		iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
		iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
		mReceiver = new SDKReceiver();
		registerReceiver(mReceiver, iFilter);

		initBaseMap();
		initLocation();
		initListener();
		initTTSPlayer();

		//获取当前车辆POI列表
		new Thread(FetchPOIsTask).start();

	}
	
	//初始化语音播放模块
	@SuppressWarnings("deprecation")
	private void initTTSPlayer() {
		BaiduNaviManager.getInstance().initEngine(this, getSdcardDir(), mNaviEngineInitListener, "34kFFFqMnjiNPcNmAVcHiXgl",null);
		BNavigator.getInstance().setListener(mBNavigatorListener);  
        BNavigator.getInstance().startNav();
		BNTTSPlayer.initPlayer();
	}

	//设置菜单
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0,Menu.FIRST,0,"新增医疗机构").setIcon(android.R.drawable.ic_menu_add);
		menu.add(0,Menu.FIRST+1,0,"车牌号设置").setIcon(android.R.drawable.ic_menu_manage);
		menu.add(0,Menu.FIRST+2,0,"版本说明").setIcon(android.R.drawable.ic_menu_info_details);
		menu.add(0,Menu.FIRST+3,0,"离线地图下载").setIcon(android.R.drawable.ic_menu_mapmode);
		menu.add(0,Menu.FIRST+4,0,"刷新数据").setIcon(android.R.drawable.ic_menu_rotate);
		return super.onCreateOptionsMenu(menu);
	}

	//点击菜单选项的响应事件
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case 1:
			performCreatePOI();
			break;
		case 2:
			performSettings();
			break;
		case 3:
			performVersion();
			break;
		case 4:
			performDownloadOfflineMap();
			break;
		case 5:
			new Thread(FetchPOIsTask).start();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	//初始化地图
	private void initBaseMap() {		
		mMapView = (MapView) findViewById(R.id.bmapView);
		mBaiduMap = mMapView.getMap();
	}

	//初始化定位图层
	private void initLocation() {
		mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(LocationMode.FOLLOWING,true,mCurrentMarker));
		mCurrentMarker = BitmapDescriptorFactory.fromResource(R.drawable.ok);
		//开启定位图层
		mBaiduMap.setMyLocationEnabled(true);
		//构造定位客户端对象
		mLocationClient = new LocationClient(getApplicationContext());
		//注册监听函数
		mLocationClient.registerLocationListener(myListener);

		LocationClientOption option = new LocationClientOption();
		option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//设置定位模式
		option.setCoorType("bd09ll");//返回的定位结果是百度经纬度,默认值gcj02
		option.setScanSpan(5000);//设置发起定位请求的间隔时间为5000ms
		option.setIsNeedAddress(false);//返回的定位结果不包含地址信息
		option.setNeedDeviceDirect(false);//返回的定位结果不包含手机机头的方向
		mLocationClient.setLocOption(option);

		mLocationClient.start();
	}
	
	//判断是否到达某个医疗机构提醒半径内
	private void whereIAm(BDLocation curLocation) {
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putString("type", "general");

		//清空目前可能的医疗机构列表，准备更新
		possibleHospitalList.clear();
		
		try {
			//第一次启动应用，还未读取后台数据
			//或服务器端没有目前车辆的任何数据
			if(pois == null) {
				return;
			}
			//遍历当前车辆负责的医疗机构
			for(int i=0;i<pois.length();i++) {
				JSONObject poi = pois.getJSONObject(i);
				JSONArray loc = poi.getJSONArray("location");
				String id = poi.getString("id");
				String name = poi.getString("title");

				//判断目前我所在位置可能的医疗机构名称，加入可能性列表
				double distance = DistanceUtil.getDistance(new LatLng(loc.getDouble(1), loc.getDouble(0)), new LatLng(curLatitude,curLongitude));

				if(distance < NotifyRadius) {
					possibleHospitalList.put(name, id);
					curPOIID = id;
					curHospital = name;
				}
			}
			//如果只有一个，设置为当前医疗机构（以上代码已实现）
			//提示司机签到
			if (voiceControl == 0) {
				if (possibleHospitalList.size() > 1) {
					voiceText = "附近有多个医院，请选择签到";
					new Thread(playVoice).start();
				} else if (possibleHospitalList.size() == 1) {
					voiceText = "已到达" + curHospital;
					new Thread(playVoice).start();
				}
				voiceControl++;
			} else {
				if(voiceControl<10)
					voiceControl++;
				else
					voiceControl = 0;
			}
		} catch (Exception e) {
			data.putString("message","whereIAm" + e.getLocalizedMessage());
			msg.setData(data);
			handler.sendMessage(msg);
		}
	}

	//初始化按钮动作侦听器
	private void initListener() {
		checkinButton = (Button) findViewById(R.id.checkinbutton);
		OnClickListener onClickListener = new OnClickListener() {
			public void onClick(View view) {
				if(view.equals(checkinButton)) {
					performCheckin();
				}
			}
		};
		checkinButton.setOnClickListener(onClickListener);
	}

	//更新手机地图标记
	private void updatePOI() {
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putString("type", "general");
		//清除所有Overlay和信息窗口
		mBaiduMap.clear();
		//读取pois数据
		SharedPreferences sharedata = getSharedPreferences("mwmdata", 0);  
		String s = sharedata.getString("pois", null);
		if(s == null) {
			data.putString("message","未找到本车负责的医疗机构");
			msg.setData(data);
			handler.sendMessage(msg);
			return;
		} else if(s.isEmpty()) {
			data.putString("message","未找到本车负责的医疗机构");
			msg.setData(data);
			handler.sendMessage(msg);
			return;
		}
		try {
			pois = new JSONArray(s);
			for(int i=0;i<pois.length();i++) {
				JSONObject poi = pois.getJSONObject(i);
				JSONArray loc = poi.getJSONArray("location");
				String time = poi.getString("lasttime");
				String name = poi.getString("title");
				String id = poi.getString("id");
				Bundle b = new Bundle();
				b.putString("lasttime", time);
				b.putString("id", id);

				LatLng point = new LatLng(loc.getDouble(1),loc.getDouble(0));
				Date now = new Date(System.currentTimeMillis());
				Date lt = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(time);
				BitmapDescriptor bitmap;

				if(Math.abs((now.getTime()-lt.getTime())/(24*3600*1000)) >=2 ) {
					bitmap = BitmapDescriptorFactory.fromResource(R.drawable.miss);
				} else {
					bitmap = BitmapDescriptorFactory.fromResource(R.drawable.ok);
				}
				OverlayOptions option = new MarkerOptions().title(name).extraInfo(b).position(point).icon(bitmap);
				mBaiduMap.addOverlay(option);
				//构建圆圈的Option对象  
				OverlayOptions circleOption = new CircleOptions()  
				    .center(point)
				    .radius(NotifyRadius)
				    .fillColor(0x77FFFF00); //透明度+RGB？？
				//在地图上添加圆圈Option，用于标示提醒范围  
				mBaiduMap.addOverlay(circleOption);
			}
		} catch (Exception e) {
			data.putString("message","updatePOI" + e.getLocalizedMessage());
			msg.setData(data);
			handler.sendMessage(msg);
		}

		mBaiduMap.setOnMarkerClickListener(new OnMarkerClickListener() {
			public boolean onMarkerClick(Marker m) {
				String s = "上次清运：" + m.getExtraInfo().getString("lasttime");
				new AlertDialog.Builder(MainActivity.this)
				.setTitle(m.getTitle())
				.setMessage(s)
				.setNegativeButton("取消", null)
				.show();
				return false;
			}
		});

	}

	//定位回调方法
	public class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			if(location == null || mMapView == null)
				return;

			//更新当前经纬度
			curLatitude = location.getLatitude();
			curLongitude = location.getLongitude();

			//更新当前车辆信息
			if(curVehicle.isEmpty()) {
				SharedPreferences sharedata = getSharedPreferences("mwmdata", 0);  
				String data = sharedata.getString("vehicle", null);
				curVehicle = data;
			}

			//输出到窗口
			StringBuffer sb = new StringBuffer(256);
			sb.append("时间:");
			sb.append((new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date()));
			sb.append("\n");
			
			//利用LBS显示目前位置最近的医疗机构POI
			whereIAm(location);
			
			if(possibleHospitalList.isEmpty()) {
				sb.append("附近没有需要清运的医院。");
			} else if(possibleHospitalList.size() == 1) {
				for(String key : possibleHospitalList.keySet()) {
					sb.append("现在位于 " + key);
				}
			} else {
				sb.append("在附近有多家医院");
			}
			
			mStateBar.setTextColor(Color.GREEN);
			mStateBar.setText(sb.toString());
			MyLocationData locData = new MyLocationData.Builder()
			.latitude(location.getLatitude())
			.longitude(location.getLongitude()).build();
			mBaiduMap.setMyLocationData(locData);
			if (isFirstLoc) {
				isFirstLoc = false;
				LatLng ll = new LatLng(location.getLatitude(),
						location.getLongitude());
				MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
				mBaiduMap.animateMapStatus(u);
			}
		}
	}

	//多子线程消息处理方法
	Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Bundle data = msg.getData();
			String type = data.getString("type");
			String value = data.getString("value");
			String message = data.getString("message");
			if(message == null) {
				message = " ";
			}
			if(type.equals("create")) {
				if(value.equals("success")) {
					Toast.makeText(MainActivity.this, "成功标记一家医疗机构", Toast.LENGTH_LONG).show();
					voiceText = "标记成功" + curHospital;
					new Thread(playVoice).start();
				} else {
					Toast.makeText(MainActivity.this, "标记医疗机构失败，消息："+message, Toast.LENGTH_LONG).show();
					voiceText = "标记失败" + curHospital;
					new Thread(playVoice).start();
				}
			} else if(type.equals("checkIn")) {
				if(value.equals("success")) {
					Toast.makeText(MainActivity.this, "签到成功："+curHospital, Toast.LENGTH_SHORT).show();
					voiceText = "签到成功" + curHospital;
					new Thread(playVoice).start();
				} else {
					Toast.makeText(MainActivity.this, "签到失败：" + message, Toast.LENGTH_SHORT).show();
					voiceText = "签到失败" + curHospital;
					new Thread(playVoice).start();
				}
			} else if(type.equals("list")) {
				if(value.equals("success")) {
					Toast.makeText(MainActivity.this, "成功获取医疗机构列表", Toast.LENGTH_SHORT).show();
					voiceText = "数据更新成功";
					new Thread(playVoice).start();
					updatePOI();
				} else {
					Toast.makeText(MainActivity.this, "获取医疗机构列表失败，" + message, Toast.LENGTH_LONG).show();
					voiceText = "数据更新失败";
					new Thread(playVoice).start();
				}
			} else if(type.equals("general")) {
				Toast.makeText(MainActivity.this, "通用消息：" + message, Toast.LENGTH_SHORT).show();
				voiceText = "错误";
				new Thread(playVoice).start();
			}
		}
	};

	//执行"车辆设置"操作
	private void performSettings() {
		//读取上次设置的车牌号
		SharedPreferences sharedata = getSharedPreferences("mwmdata", 0);  
		String data = sharedata.getString("vehicle", null);
		if(data == null) {
			curVehicle = "未设置";
		} else {
			curVehicle = data;
		}
		//弹出对话框，要求输入车牌号，并存储
		LayoutInflater factory = LayoutInflater.from(this);
		final View view = factory.inflate(R.layout.settings, null);
		((EditText)view.findViewById(R.id.txt_vehicle)).setText(curVehicle);
		AlertDialog dialog = new AlertDialog.Builder(this)
		.setTitle("设置")
		.setView(view)
		.setPositiveButton("保存", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				SharedPreferences.Editor sharedata = getSharedPreferences("mwmdata", 0).edit();
				String s = ((EditText)view.findViewById(R.id.txt_vehicle)).getText().toString();
				sharedata.putString("vehicle", s);  
				sharedata.commit();
				curVehicle = s;
				new Thread(FetchPOIsTask).start();
			}
		})
		.setNegativeButton("取消", null)
		.show();
	}

	//执行“下载离线地图”操作
	private void performDownloadOfflineMap() {
		//打开离线地图管理窗口
		Intent intent = new Intent(this, OfflineManager.class);
		startActivity(intent);
	}

	//执行“签到”操作
	private void performCheckin() {
		// 提醒半径内没有医疗机构
		if (possibleHospitalList.isEmpty()) {
			Toast.makeText(MainActivity.this, "还未到达医疗机构或当前医疗机构未标记！",
					Toast.LENGTH_LONG).show();
			return;
		} else if (possibleHospitalList.size() == 1) {
			new Thread(checkInTask).start();
		} else {

			// 遍历附近可能的医疗机构，弹出对话框选择本次要签到的一个机构
			ArrayList<String> items = new ArrayList<String>();

			for (String key : possibleHospitalList.keySet()) {
				items.add(key);
			}
			final CharSequence[] csItems = items.toArray(new CharSequence[items
					.size()]);

			new AlertDialog.Builder(this).setTitle("选择签到医院")
					.setItems(csItems, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							// 被选择的医院POIID赋值给curPOIID
							curPOIID = possibleHospitalList.get(csItems[item]);
							curHospital = csItems[item].toString();
							// 启动网络签到线程
							new Thread(checkInTask).start();
						}
					}).show();
		}
	}

	//执行"标记医疗机构"操作
	private void performCreatePOI() {
		//读取上次设置的车牌号
		if(curVehicle.isEmpty()) {
			SharedPreferences sharedata = getSharedPreferences("mwmdata", 0);  
			String data = sharedata.getString("vehicle", null);
			curVehicle = data;
		}
		//弹出对话框，要求输入医疗机构，并存储
		LayoutInflater factory = LayoutInflater.from(this);
		final View view = factory.inflate(R.layout.create, null);
		((EditText)view.findViewById(R.id.txt_vehicle)).setText(curVehicle);
		((EditText)view.findViewById(R.id.txt_vehicle)).setEnabled(false);
		new AlertDialog.Builder(this)
		.setTitle("标记新的医疗机构")
		.setView(view)
		.setPositiveButton("保存", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				String sHospital = ((EditText)view.findViewById(R.id.txt_hospital)).getText().toString();
				if(sHospital.isEmpty()) {
					Toast.makeText(MainActivity.this, "请填写医疗机构名称", Toast.LENGTH_LONG).show();
				} else {
					tempHospital = sHospital;
					new Thread(createPOITask).start();
				}
			}
		})
		.setNegativeButton("取消", null)
		.show();
	}

	//执行"显示版本信息"操作
	private void performVersion() {
		new AlertDialog.Builder(this)
		.setTitle("版本信息")
		.setMessage("医废清运车辆管理系统移动手机端 V1.5\n版权所有：北京环天伟业科技有限公司\n技术支持：18503514032")
		.setPositiveButton("确定", null)
		.show();
	}

	//车辆“签到”
	Runnable checkInTask = new Runnable() {
		public void run() {
			Message msg = new Message();
			Bundle data = new Bundle();
			data.putString("type", "checkIn");

			HttpResponse response = null;
			HttpEntity entity = null;
			HttpPost httppost = new HttpPost(URL_UPDATE_POI);

			List <NameValuePair> nvps = new ArrayList <NameValuePair>();
			nvps.add(new BasicNameValuePair("ak", AK));
			nvps.add(new BasicNameValuePair("id", curPOIID));
			nvps.add(new BasicNameValuePair("vehicle", curVehicle));
			nvps.add(new BasicNameValuePair("lasttime", (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date())));
			nvps.add(new BasicNameValuePair("mode", "mobile"));
			nvps.add(new BasicNameValuePair("coord_type", COORD_TYPE));
			nvps.add(new BasicNameValuePair("geotable_id", GEOTABLE_ID));
			try {
				httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

				response = httpclient.execute(httppost);
				if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					entity = response.getEntity();
					String resp = EntityUtils.toString(entity);
					JSONObject jsonObject = new JSONObject(resp);
					String statusCode = jsonObject.getString("status");
					String statusMsg = jsonObject.getString("message");
					switch(Integer.parseInt(statusCode)) {
					case 0:
					case 21:
						data.putString("value","success");
						msg.setData(data);
						handler.sendMessage(msg);
						break;
					default:
						data.putString("value","failed");
						data.putString("message",statusMsg);
						msg.setData(data);
						handler.sendMessage(msg);
					}
				} else {
					data.putString("value","failed");
					data.putString("message",String.valueOf(response.getStatusLine().getStatusCode()));
					msg.setData(data);
					handler.sendMessage(msg);
				}

				if (entity != null) {
					entity.consumeContent();
				}
			} catch (Exception e) {
				data.putString("value","failed");
				data.putString("message",e.getClass().toString());
			}
		}
	};

	//获取当前车辆医疗机构列表
	Runnable FetchPOIsTask = new Runnable() {
		public void run() {
			Message msg = new Message();
			Bundle data = new Bundle();
			data.putString("type", "list");

			HttpResponse response = null;
			HttpEntity entity = null;

			List <NameValuePair> nvps = new ArrayList <NameValuePair>();
			nvps.add(new BasicNameValuePair("ak", AK));
			nvps.add(new BasicNameValuePair("page_size", "200"));  //假设每辆车负责的医疗机构不会超过200家
			nvps.add(new BasicNameValuePair("page_index", "0"));
			nvps.add(new BasicNameValuePair("geotable_id", GEOTABLE_ID));
			if(curVehicle.isEmpty()) {
				nvps.add(new BasicNameValuePair("vehicle", "--"));
			} else {
				nvps.add(new BasicNameValuePair("vehicle", curVehicle));
			}

			String param = URLEncodedUtils.format(nvps, "UTF-8");

			HttpGet httpget = new HttpGet(URL_LIST_POI + "?" + param);

			try {
				response = httpclient.execute(httpget);

				if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					entity = response.getEntity();
					String resp = "";
					resp = EntityUtils.toString(entity);

					String statusCode = "-1";
					String statusMsg = "";
					JSONObject jsonObject = new JSONObject(resp);
					statusCode = jsonObject.getString("status");
					statusMsg = jsonObject.getString("message");

					switch(Integer.parseInt(statusCode)) {
					case 0:
					case 21:
						if(jsonObject.getInt("size") == 0) {
							pois = null;
							mBaiduMap.clear();
							data.putString("value","failed");
							data.putString("message", "无数据");
							msg.setData(data);
							handler.sendMessage(msg);
							
							break;
						}
						if(jsonObject.getJSONArray("pois").length() > 0) {
							//数据写入永久存储备用
							SharedPreferences.Editor sharedata = getSharedPreferences("mwmdata", 0).edit();
							sharedata.putString("pois", jsonObject.getJSONArray("pois").toString());  
							sharedata.commit();
							//立刻判断是否附近有新增的本车负责的医院
							BDLocation loc = new BDLocation();
							loc.setLongitude(curLongitude);
							loc.setLatitude(curLatitude);
							whereIAm(loc);
						}
						data.putString("value","success");
						msg.setData(data);
						handler.sendMessage(msg);
						break;
					default:
						data.putString("value","failed");
						data.putString("message",statusMsg);
						msg.setData(data);
						handler.sendMessage(msg);
					}
				} else {
					data.putString("value","failed");
					data.putString("message",String.valueOf(response.getStatusLine().getStatusCode()));
					msg.setData(data);
					handler.sendMessage(msg);
				}

				if (entity != null) {
					entity.consumeContent();
				}

			} catch (Exception e) {
				data.putString("value","failed");
				data.putString("message",e.getClass().toString() + e.getMessage());
				msg.setData(data);
				handler.sendMessage(msg);
			}
		}
	};

	
	//TTS异步线程
	Runnable playVoice = new Runnable() {
		public void run() {
			BNTTSPlayer.playTTSText(voiceText, -1);
		}
	};
	
	//标记一家医疗机构
	Runnable createPOITask = new Runnable() {
		public void run() {
			Message msg = new Message();
			Bundle data = new Bundle();
			data.putString("type", "create");

			HttpResponse response = null;
			HttpEntity entity = null;
			HttpPost httppost = new HttpPost(URL_CREATE_POI);

			List <NameValuePair> nvps = new ArrayList <NameValuePair>();
			nvps.add(new BasicNameValuePair("ak", AK));
			nvps.add(new BasicNameValuePair("title", tempHospital));
			nvps.add(new BasicNameValuePair("vehicle", curVehicle));
			nvps.add(new BasicNameValuePair("lasttime", (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date())));
			nvps.add(new BasicNameValuePair("mode", "mobile"));
			nvps.add(new BasicNameValuePair("latitude", String.valueOf(curLatitude)));
			nvps.add(new BasicNameValuePair("longitude", String.valueOf(curLongitude)));
			nvps.add(new BasicNameValuePair("coord_type", COORD_TYPE));
			nvps.add(new BasicNameValuePair("geotable_id", GEOTABLE_ID));
			try {
				httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
				response = httpclient.execute(httppost);

				if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					entity = response.getEntity();
					String resp = "";
					resp = EntityUtils.toString(entity);

					String statusCode = "-1";
					String statusMsg = "";
					String id = "";
					JSONObject jsonObject = new JSONObject(resp);
					statusCode = jsonObject.getString("status");
					statusMsg = jsonObject.getString("message");
					id = jsonObject.getString("id");

					switch(Integer.parseInt(statusCode)) {
					case 0:
					case 21:
						curHospital = tempHospital;
						curPOIID = id;
						data.putString("value","success");
						msg.setData(data);
						handler.sendMessage(msg);
						break;
					default:
						data.putString("value","failed");
						data.putString("message",statusMsg);
						msg.setData(data);
						handler.sendMessage(msg);
					}
				} else {
					data.putString("value","failed");
					data.putString("message",String.valueOf(response.getStatusLine().getStatusCode()));
					msg.setData(data);
					handler.sendMessage(msg);
				}

				if (entity != null) {
					entity.consumeContent();
				}
			} catch (Exception e) {
				data.putString("value","failed");
				data.putString("message",e.getClass().toString());
			}
			// 获取最新医疗机构列表
			if (data.getString("value") == "success") {
				new Thread(FetchPOIsTask).start();
			}
		}
	};


	/**
	 * 构造广播监听类，监听 SDK key 验证以及网络异常广播
	 */
	public class SDKReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			String s = intent.getAction();
			mStateBar.setTextColor(Color.RED);
			if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {
				mStateBar.setText("key验证出错,请检查 key 设置!");
			} else if (s.equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
				mStateBar.setText("网络出错!");
			}
		}
	}

	@Override
	protected void onDestroy() {
		//在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
		mMapView.onDestroy();
		// 取消监听 SDK 广播
		unregisterReceiver(mReceiver);

		mLocationClient.stop();
		
		super.onDestroy();
	}
	@Override
	protected void onResume() {
		super.onResume();
		//在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
		mMapView.onResume();
	}
	@Override
	protected void onPause() {
		super.onPause();
		//在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
		mMapView.onPause();
	}
}

//0: 成功
//1: 服务器内部错误
//2: 参数错误
//3: http method错误
//21: 此操作为批量操作
//22: 同步到检索失败
//31: 服务端加锁失败
//32: 服务端释放锁失败
//1001: 表的name重复
//1002: 表的数量达到了最大值
//1003: 表中存在poi数据，不允许删除
//2001: 列的key重复
//2002: 列的key是保留字段
//2003: 列的数量达到了最大值
//2004: 唯一索引只能创建一个
//2005: 更新为唯一索引失败，原poi数据中有重复
//2011: 排序筛选字段只能用于整数或小数类型的列
//2012: 排序筛选的列已经达到了最大值
//2021: 检索字段只能用于字符串类型的列且最大长度不能超过512个字节
//2022: 检索的列已经达到了最大值
//2031: 索引的列已经达到了最大值
//2041: 指定的列不存在
//2042: 修改max_length必须比原值大
//3001: 更新坐标必须包含经纬度和类型
//3002: 唯一索引字段存在重复
//3031: 上传的文件太大