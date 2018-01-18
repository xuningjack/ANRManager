package com.jack.anr;

import android.content.Context;
import android.os.Build;
import android.view.Choreographer;

import com.jack.anr.collector.Collector;
import com.jack.anr.collector.StackTraceCollector;
import com.jack.anr.observer.BlockHandler;
import com.jack.anr.observer.FPSFrameCallBack;
import com.jack.anr.observer.UILooperObserver;


/**
 * ANR操作工具类
 * @author Jack
 */
public class AnrManager {

    private static AnrManager mAnrManager;
    /**
     * 触发卡顿的默认时间间隔
     */
    final static long DEFAULT_THRESHOLD_TIME = 5000;
    /**
     * 监测ANR的收集默认时间间隔
     */
    final static long DEFAULT_COLLECT_INTERVAL = 1000;
    /**
     * 触发卡顿的最小临界时间
     */
    final static long MIN_THRESHOLD_TIME = 500;
    /**
     * 最小收集时间间隔
     */
    final static long MIN_COLLECT_INTERVAL = 500;
    /**
     * 触发卡顿的最大临界时间
     */
    final static long MAX_THRESHOLD_TIME = 4000;
    /**
     * 采样的最大间隔时间
     */
    final static long MAX_COLLECT_INTERVAL = 2000;
    /**
     * 默认采样方式
     */
    static MonitorMode DEFAULT_MODE = MonitorMode.LOOPER;


    /**
     * AnrManager构造器
     * @param context
     * @param thresholdTimeMillis 最小的临界值
     * @param collectIntervalMillis
     * @param mode
     * @param loggingEnabled
     * @param callback
     */
    private AnrManager(Context context, long thresholdTimeMillis, long collectIntervalMillis,
                       MonitorMode mode, boolean loggingEnabled, AnrCallback callback) {
        long thresholdTime = Math.min(Math.max(thresholdTimeMillis, MIN_THRESHOLD_TIME), MAX_THRESHOLD_TIME);
        long collectInterval = Math.min(Math.max(collectIntervalMillis, MIN_COLLECT_INTERVAL), MAX_COLLECT_INTERVAL);
        Config.LOG_ENABLED = loggingEnabled;
        //设置卡顿的临界值时间
        Config.THRESHOLD_TIME = thresholdTime;
        Collector mTraceCollector = new StackTraceCollector(collectInterval);
        BlockHandler mBlockHandler = new BlockHandler(context, mTraceCollector, callback);
        if (mode == MonitorMode.LOOPER) {
            new UILooperObserver(mBlockHandler);
        } else if (mode == MonitorMode.FRAME) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {   //API 16上才能使用
                FPSFrameCallBack fpsFrameCallBack = new FPSFrameCallBack(context, mBlockHandler);
                Choreographer.getInstance().postFrameCallback(fpsFrameCallBack);
            } else {
                new UILooperObserver(mBlockHandler);
            }
        }
    }

    /**
     * 初始化，使用默认参数
     * @param context
     */
    public static void initialize(Context context) {
        initialize(new Builder(context));
    }

    /**
     * 初始化化，自定义处理器等参数
     * @param builder
     */
    public static void initialize(Builder builder) {
        if (mAnrManager == null) {
            synchronized (AnrManager.class) {
                if (mAnrManager == null) {
                    mAnrManager = builder.build();
                }
            }
        }
    }

    /**
     * 设置是否显示log信息
     * @param enabled
     */
    public static void setLoggingEnabled(boolean enabled) {
        Config.LOG_ENABLED = enabled;
    }

    /**
     * 构造器
     */
    public static class Builder {

        private long mThresholdTime = DEFAULT_THRESHOLD_TIME;
        private long mCollectInterval = DEFAULT_COLLECT_INTERVAL;
        private Context mContext;
        private MonitorMode mMonitorMode = DEFAULT_MODE;
        private boolean loggingEnabled = true;
        private AnrCallback mCallback;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder thresholdTime(long thresholdTimeMillis) {
            mThresholdTime = thresholdTimeMillis;
            return this;
        }

        public Builder collectInterval(long collectIntervalMillis) {
            mCollectInterval = collectIntervalMillis;
            return this;
        }

        public Builder monitorMode(MonitorMode mode) {
            this.mMonitorMode = mode;
            return this;
        }

        public Builder loggingEnabled(boolean enable) {
            loggingEnabled = enable;
            return this;
        }

        public Builder callback(AnrCallback callback) {
            mCallback = callback;
            return this;
        }

        AnrManager build() {
            return new AnrManager(mContext, mThresholdTime, mCollectInterval,
                    mMonitorMode, loggingEnabled, mCallback);
        }
    }

    /**
     * 监控ANR的模式
     */
    public enum MonitorMode {
        //通过监测主线程消息处理时间来判断。
        LOOPER(0),
        //通过监测绘制帧间隔时间来判断是否卡顿，API 16上才能使用。
        FRAME(1);


        int value;

        MonitorMode(int mode) {
            this.value = mode;
        }
    }

    /**
     * 产生ANR后的处理回调
     */
    public interface AnrCallback {

        /**
         * 当发生阻塞时的处理方法回调（这里你可以卡顿信息上传到自己服务器）
         * @param stackTraces 收集到的堆栈，以便分析卡顿原因。
         * @param anr 如果应用发生ANR，这个就我ANR相关信息，没发生ANR，则为空。
         * @param blockArgs 采用AnrManager.MonitorMode.FRAME模式监测时，blockArgs的size为1，blockArgs[0] 即是发生掉帧的数。
         *                  采用AnrManager.MonitorMode.LOOPER模式监测时，blockArgs的size为2，
         *                    blockArgs[0] 为UI线程卡顿时间值，blockArgs[1]为在此期间UI线程能执行到的时间。
         */
        void onBlockOccurs(String[] stackTraces, String anr, long... blockArgs);
    }
}
