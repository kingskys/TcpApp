package com.kingskys.tcpapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class LauncherApplication extends Application {

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        try {
            log("内存警告");
        }
        catch (Throwable e) {}

    }

    private static void log(String msg) {
        Log.w("AAA", "TcpApp - " + msg);
    }
}

