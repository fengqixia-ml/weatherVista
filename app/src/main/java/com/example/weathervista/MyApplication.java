package com.example.weathervista;

import android.app.Application;

import org.litepal.LitePal;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化LitePal，参数传入上下文
        LitePal.initialize(this);
    }
}
