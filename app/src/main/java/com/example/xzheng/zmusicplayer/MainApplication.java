package com.example.xzheng.zmusicplayer;

import android.app.Application;
import android.content.Context;

/**
 * Created by xzheng on 15/1/3.
 */
public class MainApplication extends Application {
    private static Context _context;


    @Override
    public void onCreate() {
        super.onCreate();

        _context = getApplicationContext();
    }

    public static Context getAppContext() {
        return _context;
    }
}
