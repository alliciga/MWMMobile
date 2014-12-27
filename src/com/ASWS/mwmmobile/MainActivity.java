package com.ASWS.mwmmobile;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.VersionInfo;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class MainActivity extends Activity {

	/**
	 * MapView 是地图主控件
	 */
	private MapView mMapView;
	private BaiduMap mBaiduMap;
	

	/**
	 * 签到按钮
	 */
	private Button checkinButton;

	/**
	 * 消息接收器
	 * 用于显示地图状态的面板
	 */
	private TextView mStateBar;
	private SDKReceiver mReceiver;


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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//在使用SDK各组件之前初始化context信息，传入ApplicationContext
		//注意该方法要再setContentView方法之前实现
		SDKInitializer.initialize(getApplicationContext());

		setContentView(R.layout.activity_main);

		mStateBar = (TextView) findViewById(R.id.text_State);
		mStateBar.setTextColor(Color.GREEN);
		mStateBar.setText("欢迎使用清运车辆监控手机版，百度SDK v" + VersionInfo.getApiVersion());

		// 注册 SDK 广播监听者
		IntentFilter iFilter = new IntentFilter();
		iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
		iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
		mReceiver = new SDKReceiver();
		registerReceiver(mReceiver, iFilter);

		//太原中心点:112.553,37.857
		LatLng p = new LatLng(37.857, 112.553);
		mMapView = (MapView) findViewById(R.id.bmapView);
		mBaiduMap = mMapView.getMap();
		
		MapStatus mMapStatus = new MapStatus.Builder().target(p).zoom(14).build();
		mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(mMapStatus));
		
		initListener();
	}

	private void initListener() {
		checkinButton = (Button) findViewById(R.id.checkinbutton);
		OnClickListener onClickListener = new OnClickListener() {
			@Override
			public void onClick(View view) {
				if(view.equals(checkinButton)) {
					performCheckin();
					
				}
				updateMapState();
			}
		};
		checkinButton.setOnClickListener(onClickListener);
	}
	
	//重置地图状态
	protected void updateMapState() {
		
	}

	//执行“签到”操作
	private void performCheckin() {
		Toast.makeText(MainActivity.this,"签到成功！清运车：xxx，医疗机构：xxx，时间：xxx",
				Toast.LENGTH_SHORT).show();
		mStateBar.setTextColor(Color.GREEN);
		mStateBar.setText("签到成功！");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
		mMapView.onDestroy();
		// 取消监听 SDK 广播
		unregisterReceiver(mReceiver);
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
