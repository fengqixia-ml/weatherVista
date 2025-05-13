package com.example.weathervista;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.weathervista.db.City;
import com.example.weathervista.db.County;
import com.example.weathervista.db.Province;
import com.example.weathervista.util.HttpUtil;
import com.example.weathervista.util.Utility;

import org.json.JSONException;
import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.spec.DESedeKeySpec;
import okhttp3.Callback;

import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList=new ArrayList<>();

    //    省列表
    private List<Province> provinceList;
    //    市列表
    private List<City> cityList;
    //    县列表
    private  List<County> countyList;
    //    选中的省份
    private Province selectedProvince;
    //    选中的城市
    private City selectedCity;
    //    当前选中的级别
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText=(TextView) view.findViewById(R.id.title_text);
        backButton=(Button)view.findViewById(R.id.back_button);
        listView=(ListView)view.findViewById(R.id.list_view);
        adapter=new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if(currentLevel==LEVEL_PROVINCE){
                    selectedProvince=provinceList.get(position);
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    selectedCity=cityList.get(position);
                    queryCounties();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLevel==LEVEL_COUNTY){
                    currentLevel=LEVEL_CITY;
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    currentLevel=LEVEL_PROVINCE;
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    //    查询全国所有的省，优先从数据库查询，如果没有查询到再到服务器上查询
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList= LitePal.findAll(Province.class);
        if (provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else {
            Log.d("Another", "数据库中未找到省份数据，准备从服务器获取");
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        };
    }

    //    查询选中省内所有的市，优先从数据库中查询，如果没有查询到再去服务器上查询，
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        Log.d("ChooseAreaFragment", "查询城市，省份ID: " + selectedProvince.getId());
        cityList = LitePal.where("provinceid=?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    //    查询选中市内所有的县，优先从数据库中查询，如果没有查询到再去服务器上查询
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        Log.d("ChooseAreaFragment", "查询县，城市ID: " + selectedCity.getId());
        countyList = LitePal.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    //    根据传入的地址和类型从服务器上查询省市县数据
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(@NonNull okhttp3.Call call, Response response) throws IOException {
                Log.d("ChooseAreaFragment", "网络请求成功，type: " + type);
                String responseText = response.body().string();
                boolean result = false;

                if ("province".equals(type)) {
                    try {
                        result = Utility.handleProvinceResponse(responseText);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        getActivity().runOnUiThread(() -> {
                            closeProgressDialog();
                            Toast.makeText(getContext(), "解析省份数据失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }

                if (result) {
                    getActivity().runOnUiThread(() -> {
                        closeProgressDialog();
                        if ("province".equals(type)) {
                            queryProvinces();
                        } else if ("city".equals(type)) {
                            queryCities();
                        } else if ("county".equals(type)) {
                            queryCounties();
                        }
                    });
                } else {
                    Log.e("ChooseAreaFragment", "数据解析失败，type: " + type);
                    getActivity().runOnUiThread(() -> {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "获取数据失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(@NonNull okhttp3.Call call, IOException e) {
                Log.e("ChooseAreaFragment", "网络请求失败，type: " + type);
                e.printStackTrace();//打印异常堆栈信息
                getActivity().runOnUiThread(() -> {
                    closeProgressDialog();
                    Toast.makeText(getContext(), "加载失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    //    显示进度对话框
    private void showProgressDialog() {
        if (getActivity() != null && progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载中...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        if (progressDialog != null) {
            progressDialog.show();
        }
    }

    //    关闭进度对话框
    private void closeProgressDialog() {
        if (progressDialog!=null){
            progressDialog.dismiss();

        }    }


}