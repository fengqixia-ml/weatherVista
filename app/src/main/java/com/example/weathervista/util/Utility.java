package com.example.weathervista.util;

import android.text.TextUtils;
import android.util.Log;

import com.example.weathervista.db.City;
import com.example.weathervista.db.County;
import com.example.weathervista.db.Province;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class Utility {
    //    解析和处理服务器返回的省级数据
    public static boolean handleProvinceResponse(String response) throws JSONException {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allProvinces = new JSONArray(response);
                for (int i = 0; i < allProvinces.length(); i++) {
                    JSONObject provinceObject = allProvinces.getJSONObject(i);
                    Province province = new Province();
                    try {
                        province.setProvinceName(provinceObject.getString("name"));
                        province.setProvinceCode(provinceObject.getInt("id"));
                    } catch (JSONException e) {
                        Log.e("Utility", "解析省份数据时出错，字段可能缺失或类型不匹配", e);
                        return false;
                    }
                    province.save();
                }
                return true;
            } catch (JSONException e) {
                Log.e("Utility", "解析省级数据失败", e);
                return false;
            }
        }
        return false;
    }
    //    解析和处理服务器返回的市级数据
    public static boolean handleCityResponse(String response, int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCities = new JSONArray(response);
                for (int i = 0; i < allCities.length(); i++) {
                    JSONObject cityObject = allCities.getJSONObject(i);
                    City city = new City();
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId); // 添加这一行，设置省份ID
                    city.save();
                }
                return true;
            } catch (JSONException e) {
                Log.e("Utility", "解析市级数据失败", e);
                return false;
            }
        }
        return false;
    }

        //    解析和处理服务器返回的县级数据
        public static boolean handleCountyResponse(String response, int cityId) {
            if (!TextUtils.isEmpty(response)) {
                try {
                    JSONArray allCounties = new JSONArray(response);
                    for (int i = 0; i < allCounties.length(); i++) {
                        JSONObject countyObject = allCounties.getJSONObject(i);
                        County county = new County();
                        county.setCountyName(countyObject.getString("name"));
                        county.setWeatherId(countyObject.getString("weather_id"));
                        county.setCityId(cityId);
                        county.save();
                    }
                    return true;
                } catch (JSONException e) {
                    Log.e("Utility", "解析县级数据失败", e);
                    return false;
                }
            }
            return false;
        }

}

