package com.jack.anr;

import android.os.Looper;
import android.os.SystemClock;
import android.util.Printer;


/**
 * UI线程的ANR观察者
 * @author Jack
 */
public class UILooperObserver implements Printer {

    private final static String TAG = "UILooperObserver";
    private final static String LOG_BEGIN = ">>>>> Dispatching to";
    private final static String LOG_END = "<<<<< Finished to";
    /**UI线程anr的时间*/
    public final static long ANR_TRIGGER_TIME = 5 * 10000 + 1;
    private long mPreMessageTime = 0;
    private long mPreThreadTime = 0;
    private BlockHandler mBlockHandler;


    public UILooperObserver(BlockHandler blockHandler) {
        this.mBlockHandler = blockHandler;
        Looper.getMainLooper().setMessageLogging(this);
    }

    @Override
    public void println(String msg) {
        if (msg.startsWith(LOG_BEGIN)) {
            mPreMessageTime = SystemClock.elapsedRealtime();
            mPreThreadTime = SystemClock.currentThreadTimeMillis();
        } else if (msg.startsWith(LOG_END)) {
            if (mPreMessageTime != 0) {
                long messageElapseTime = SystemClock.elapsedRealtime() - mPreMessageTime;
                long threadElapseTime = SystemClock.currentThreadTimeMillis() - mPreThreadTime;
                if (messageElapseTime > Config.THRESHOLD_TIME) {
                    Config.log(TAG, String.format("messageElapseTime : %s, threadElapseTime : %s",
                            messageElapseTime, threadElapseTime));
                    mBlockHandler.notifyBlockOccurs(
                            messageElapseTime >= ANR_TRIGGER_TIME,
                            messageElapseTime, threadElapseTime);
                }
            }
        }
    }

}
