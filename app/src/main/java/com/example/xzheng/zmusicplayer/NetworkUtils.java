package com.example.xzheng.zmusicplayer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by xzheng on 15/1/3.
 */
public class NetworkUtils {

    public static NetworkInfo getActiveNetworkInfo() {
        Context context = MainApplication.getAppContext();
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo();
    }

    public static boolean isConnected() {
        NetworkInfo activeNetwork = getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static boolean isWiFiConnected() {
        NetworkInfo activeNetwork = getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }
}
