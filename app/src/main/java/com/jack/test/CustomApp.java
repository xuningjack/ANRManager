package com.jack.test;

import android.app.Application;
import android.util.Log;

import com.jack.anr.AnrManager;



/**
 * 自定义Application
 * @author Jack
 */
public class CustomApp extends Application {

    private final String TAG = "Jack";

    @Override
    public void onCreate() {
        super.onCreate();
//        AnrManager.initialize(this);   //default
//        AnrManager.setLoggingEnabled(false);   //close log
        // use builder build your custom way
        AnrManager.Builder builder = new AnrManager.Builder(this)
                //默认监测模式为AnrManager.MonitorMode.LOOPER，这样指定AnrManager.MonitorMode.FRAME
                .monitorMode(AnrManager.MonitorMode.FRAME)
                .loggingEnabled(true)// 是否打印log
                .collectInterval(1000) //监测采集堆栈时间间隔
                .thresholdTime(2000) // 触发卡顿时间阈值
                .callback(new AnrManager.AnrCallback() { //设置触发卡顿时回调
                    @Override
                    public void onBlockOccurs(String[] stackTraces, String anr, long... blockArgs) {
                        for(String temp : stackTraces){
                            Log.e(TAG,"stackTraces------------" + temp);
                        }
                        Log.e(TAG, "anr------------" + anr);
                        Log.e(TAG, "blockArgs------------" + blockArgs);

                        // stackTraces : 收集到的堆栈，以便分析卡顿原因。 anr : 如果应用发生ANR，这个就我ANR相关信息，没发生ANR，则为空。
                        //采用AnrManager.MonitorMode.FRAME模式监测时，blockArgs的size为1，blockArgs[0] 即是发生掉帧的数。
                        //采用AnrManager.MonitorMode.LOOPER模式监测时，blockArgs的size为2，blockArgs[0] 为UI线程卡顿时间值，blockArgs[1]为在此期间UI线程能执行到的时间。
                        //这里你可以卡顿信息上传到自己服务器
                    }
                });
        AnrManager.initialize(builder);
    }
}
