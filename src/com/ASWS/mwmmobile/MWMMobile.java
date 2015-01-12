package com.ASWS.mwmmobile;

import android.app.Application;

import com.baidu.mapapi.SDKInitializer;

public class MWMMobile extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		// 在使用 SDK 各组件之前初始化 context 信息，传入 ApplicationContext
		SDKInitializer.initialize(this);
	}

}