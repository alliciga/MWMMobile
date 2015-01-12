package com.ASWS.mwmmobile;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.VersionInfo;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.model.LatLng;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.GeofenceClient;
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

	//百度地图控件
	private MapView mMapView;
	private BaiduMap mBaiduMap;
	private LocationClient mLocationClient = null;
	private GeofenceClient mGeofenceClient = null;
	private BitmapDescriptor mCurrentMarker; 
	private SDKReceiver mReceiver;
	private BDLocationListener myListener = new MyLocationListener();

	//本地变量
	private boolean isFirstLoc = true; //是否首次定位
	private double curLat = 0.0;
	private double curLon = 0.0;
	private String curHospital = null;
	private int curPOIID = 0;
	private String curVehicle = null;

	//LBS云存储及查询工具
	private LBSUtils lbsUtils = new LBSUtils();


	private Button checkinButton;
	private TextView mStateBar;

	//初始化入口
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		//读取身份注册信息
		SharedPreferences sharedata = getSharedPreferences("mwmdata", 0);  
		String data = sharedata.getString("vehicle", null);
		curVehicle = data;

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
	}

	//设置菜单
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0,Menu.FIRST,0,"标记医疗机构");
		menu.add(0,Menu.FIRST+1,0,"系统设置");
		menu.add(0,Menu.FIRST+2,0,"版本说明");
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
		mCurrentMarker = BitmapDescriptorFactory.fromResource(R.drawable.me);
		//开启定位图层
		mBaiduMap.setMyLocationEnabled(true);
		//构造定位客户端对象
		mLocationClient = new LocationClient(getApplicationContext());
		//注册监听函数
		mLocationClient.registerLocationListener(myListener);
		//构造电子围栏对象
		mGeofenceClient = new GeofenceClient(getApplicationContext());

		LocationClientOption option = new LocationClientOption();
		option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//设置定位模式
		option.setCoorType("bd09ll");//返回的定位结果是百度经纬度,默认值gcj02
		option.setScanSpan(10000);//设置发起定位请求的间隔时间为10000ms
		option.setIsNeedAddress(false);//返回的定位结果不包含地址信息
		option.setNeedDeviceDirect(false);//返回的定位结果不包含手机机头的方向
		mLocationClient.setLocOption(option);

		mLocationClient.start();
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

	//定位回调方法
	public class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			if(location == null || mMapView == null)
				return;

			//更新当前经纬度
			curLat = location.getLatitude();
			curLon = location.getLongitude();

			//更新当前车辆信息
			SharedPreferences sharedata = getSharedPreferences("mwmdata", 0);  
			String data = sharedata.getString("vehicle", null);
			curVehicle = data;

			//输出到窗口
			StringBuffer sb = new StringBuffer(256);
			sb.append("时间:");
			sb.append(location.getTime());
			sb.append("\t当前位置:");
			//利用LBS显示目前位置最近的医疗机构POI
			sb.append(curHospital);
			sb.append("\n经度:");
			sb.append(String.format("%.6f", location.getLatitude()));
			sb.append("\t纬度:");
			sb.append(String.format("%.6f", location.getLongitude()));
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

	//执行"系统设置"操作
	private void performSettings() {
		//读取上次设置的车牌号
		SharedPreferences sharedata = getSharedPreferences("mwmdata", 0);  
		String data = sharedata.getString("vehicle", null);
		curVehicle = data;
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
			}
		})
		.setNegativeButton("取消", null).show();
	}

	//执行“签到”操作
	private void performCheckin() {
		try {
			lbsUtils.checkIn(curPOIID, curVehicle);
		} catch (Exception e) {
			e.printStackTrace();
			mStateBar.setTextColor(Color.RED);
			mStateBar.setText(e.getMessage());
			return;
		}
		new AlertDialog.Builder(this)
		.setTitle("消息")
		.setMessage("签到成功！\n"
				+ "清运车：" + curVehicle +"\n"
				+ "医疗机构：" + curHospital + "\n"
				+ "时间：" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date()))
				.setPositiveButton("确定", null)
				.show();
		mStateBar.setTextColor(Color.GREEN);
		mStateBar.setText("签到成功！");
	}

	//执行"标记医疗机构"操作
	private void performCreatePOI() {
		//弹出对话框，要求输入医疗机构，并存储
		LayoutInflater factory = LayoutInflater.from(this);
		final View view = factory.inflate(R.layout.create, null);
		AlertDialog dialog = new AlertDialog.Builder(this)
		.setTitle("标记新的医疗机构")
		.setView(view)
		.setPositiveButton("保存", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					lbsUtils.createHospitalPOI("医疗机构名称", curLat, curLon, curVehicle);
				} catch (Exception e) {
					e.printStackTrace();
					mStateBar.setTextColor(Color.RED);
					mStateBar.setText(e.getMessage());
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
		.setMessage("MWM 医废清运车辆管理系统移动手机客户端 V1.0\n版权所有：北京环天伟业科技有限公司")
		.setPositiveButton("确定", null)
		.show();
	}

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
		super.onDestroy();
		//在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
		mMapView.onDestroy();
		// 取消监听 SDK 广播
		unregisterReceiver(mReceiver);

		mLocationClient.stop();
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
