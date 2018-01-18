package com.jack.anr;

import android.util.Log;


/**
 * 配置参数
 * @author Jack
 */
public class Config {

    /**
     * 卡顿跳帧的临界值时间
     */
    public static long THRESHOLD_TIME = 0;
    /**
     * 是否显示log
     */
    public static boolean LOG_ENABLED = true;


    /**
     * 显示error的log
     * @param tag
     * @param msg
     */
    public static void log(String tag, String msg) {
        if (LOG_ENABLED) {
            Log.e(tag, msg);
        }
    }
}
