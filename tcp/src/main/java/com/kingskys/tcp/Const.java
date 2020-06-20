package com.kingskys.tcp;

import android.util.Log;

class Const {
    static String logFlag = "TcpApp_";

    static void setLogFlag(String flag) {
        logFlag = flag;
    }

    static void log(String flag, String msg) {
        Log.e(logFlag + flag, msg);
    }
}
