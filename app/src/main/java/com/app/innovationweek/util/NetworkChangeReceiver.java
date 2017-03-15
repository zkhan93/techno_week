package com.app.innovationweek.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by 1036870 on 3/15/2017.
 */

public class NetworkChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Utils.isInternetConnected(context)) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean("is_internet_connected", true)
                    .apply();
        } else {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean("is_internet_connected", false)
                    .apply();
        }
    }
}
