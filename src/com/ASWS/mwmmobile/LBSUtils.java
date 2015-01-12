package com.ASWS.mwmmobile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.util.Log;

public class LBSUtils {

	//常量定义
	String URL_CREATE_POI="http://api.map.baidu.com/geodata/v3/poi/create";
	String URL_UPDATE_POI="http://api.map.baidu.com/geodata/v3/poi/update";
	String URL_LIST_POI="http://api.map.baidu.com/geodata/v3/poi/list";
	
	//服务器AK，可用于LBS存储API
	String AK="AHlXvdQ2AcfSy3KAixNEOfOx";
	String GEOTABLE_ID="86142";
	String COORD_TYPE="3";

	DefaultHttpClient httpclient = new DefaultHttpClient();

	//初始化LBS工具
	public LBSUtils() {

	}

	//车辆“签到”
	public boolean checkIn(int POI_ID, String vehicleName) throws Exception {
		HttpResponse response = null;
		HttpEntity entity = null;
		HttpPost httppost = new HttpPost(URL_UPDATE_POI);

		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("ak", AK));
		nvps.add(new BasicNameValuePair("id", String.valueOf(POI_ID)));
		nvps.add(new BasicNameValuePair("vehicle", vehicleName));
		nvps.add(new BasicNameValuePair("lasttime", (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date())));
		nvps.add(new BasicNameValuePair("mode", "mobile"));
		nvps.add(new BasicNameValuePair("coord_type", COORD_TYPE));
		nvps.add(new BasicNameValuePair("geotable_id", GEOTABLE_ID));
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
				return true;
			default:
				Log.d("checkIn", statusMsg);	
			}
		}

		if (entity != null) {
			entity.consumeContent();
		}
		return false;
	}

	//标记一家医疗机构
	public boolean createHospitalPOI(String name, double latitude, double longitude, String vehicleName) throws Exception {
		HttpResponse response = null;
		HttpEntity entity = null;
		HttpPost httppost = new HttpPost(URL_CREATE_POI);

		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("ak", AK));
		nvps.add(new BasicNameValuePair("title", name));
		nvps.add(new BasicNameValuePair("vehicle", vehicleName));
		nvps.add(new BasicNameValuePair("lasttime", (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date())));
		nvps.add(new BasicNameValuePair("mode", "mobile"));
		nvps.add(new BasicNameValuePair("latitude", String.valueOf(latitude)));
		nvps.add(new BasicNameValuePair("longitude", String.valueOf(longitude)));
		nvps.add(new BasicNameValuePair("coord_type", COORD_TYPE));
		nvps.add(new BasicNameValuePair("geotable_id", GEOTABLE_ID));
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
				return true;
			default:
				Log.d("create", statusMsg);	
			}
		}

		if (entity != null) {
			entity.consumeContent();
		}
		return false;
	}

	//更新一家医疗机构的位置
	public boolean updateHospitalPOI(String name, double latitude, double longitude, String vehicleName) throws Exception {
		HttpResponse response = null;
		HttpEntity entity = null;
		HttpPost httppost = new HttpPost(URL_UPDATE_POI);

		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("ak", AK));
		nvps.add(new BasicNameValuePair("title", name));
		nvps.add(new BasicNameValuePair("vehicle", vehicleName));
		nvps.add(new BasicNameValuePair("lasttime", (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date())));
		nvps.add(new BasicNameValuePair("mode", "mobile"));
		nvps.add(new BasicNameValuePair("latitude", String.valueOf(latitude)));
		nvps.add(new BasicNameValuePair("longitude", String.valueOf(longitude)));
		nvps.add(new BasicNameValuePair("coord_type", COORD_TYPE));
		nvps.add(new BasicNameValuePair("geotable_id", GEOTABLE_ID));
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
				return true;
			default:
				Log.d("update", statusMsg);	
			}
		}

		if (entity != null) {
			entity.consumeContent();
		}
		return false;
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