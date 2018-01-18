package com.jack.anr.observer;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;


import com.jack.anr.AnrManager;
import com.jack.anr.Config;
import com.jack.anr.collector.Collector;

import java.util.List;




/**
 * ANR阻塞的处理者
 * @author Jack
 */
public class BlockHandler {

    private static final String TAG = "BlockHandler";
    private Context mContext;
    private Collector mCollector;
    private AnrManager.AnrCallback mCallback;


    public BlockHandler(Context context, Collector collector, AnrManager.AnrCallback callback) {
        mContext = context;
        mCollector = collector;
        mCallback = callback;
    }

    /**
     * 通知发生了anr阻塞，并进行后续处理
     * @param needCheckAnr
     * @param blockArgs
     */
    public void notifyBlockOccurs(boolean needCheckAnr, long... blockArgs) {
        String[] stackTraces = mCollector.getStackTraceInfo();
        printStackTrace(stackTraces);
        String anr = "";
        if (needCheckAnr) {
            anr = checkAnr();
        }
        if (mCallback != null) {
            mCallback.onBlockOccurs(stackTraces, anr, blockArgs);
        }
    }

    /**
     * 检查anr
     * @return
     */
    private String checkAnr() {
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.ProcessErrorStateInfo> errorStateInfos = activityManager.getProcessesInErrorState();
        if (errorStateInfos != null) {
            for (ActivityManager.ProcessErrorStateInfo info : errorStateInfos) {
                Log.d(TAG, "info.condition------" + info.condition);
                if (info.condition == ActivityManager.ProcessErrorStateInfo.NOT_RESPONDING) {
                    StringBuilder anrInfo = new StringBuilder();
                    Log.d(TAG, "info.processName-----" + info.processName);
                    Log.d(TAG, "info.shortMsg-----" + info.shortMsg);
                    Log.d(TAG, "info.longMsg-----" + info.longMsg);
                    //todo 拼接收集上报的信息
                    anrInfo.append(info.processName)
                            .append("\n")
                            .append(info.shortMsg)
                            .append("\n")
                            .append(info.longMsg);
                    Config.log(TAG, anrInfo.toString());
                    return anrInfo.toString();
                }
            }
        }
        return "";
    }

    /**
     * 打印stack trace的error信息
     * @param stackTraces
     */
    private void printStackTrace(String[] stackTraces) {
        for (String item : stackTraces) {
            Config.log(TAG, item);
        }
    }
}
