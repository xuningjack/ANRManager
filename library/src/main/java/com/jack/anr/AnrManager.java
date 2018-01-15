package com.jack.anr;

import android.content.Context;
import android.os.Build;
import android.view.Choreographer;



/**
 *
 * @author Jack
 */
public class AnrManager {

    private static AnrManager sCaton;
    final static long DEFAULT_THRESHOLD_TIME = 3000;
    final static long DEFAULT_COLLECT_INTERVAL = 1000;
    final static long MIN_THRESHOLD_TIME = 500;
    final static long MIN_COLLECT_INTERVAL = 500;
    final static long MAX_THRESHOLD_TIME = 4000;
    final static long MAX_COLLECT_INTERVAL = 2000;
    static MonitorMode DEFAULT_MODE = MonitorMode.LOOPER;


    private AnrManager(Context context, long thresholdTimeMillis, long collectIntervalMillis, MonitorMode mode, boolean loggingEnabled, Callback callback) {
        long thresholdTime = Math.min(Math.max(thresholdTimeMillis, MIN_THRESHOLD_TIME), MAX_THRESHOLD_TIME);
        long collectInterval = Math.min(Math.max(collectIntervalMillis, MIN_COLLECT_INTERVAL), MAX_COLLECT_INTERVAL);
        Config.LOG_ENABLED = loggingEnabled;
        Config.THRESHOLD_TIME = thresholdTime;
        Collector mTraceCollector = new StackTraceCollector(collectInterval);
        BlockHandler mBlockHandler = new BlockHandler(context, mTraceCollector, callback);
        if (mode == MonitorMode.LOOPER) {
            new UILooperObserver(mBlockHandler);
        } else if (mode == MonitorMode.FRAME) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                FPSFrameCallBack fpsFrameCallBack = new FPSFrameCallBack(context, mBlockHandler);
                Choreographer.getInstance().postFrameCallback(fpsFrameCallBack);
            } else {
                new UILooperObserver(mBlockHandler);
            }
        }
    }

    public static void initialize(Context context) {
        initialize(new Builder(context));
    }

    public static void initialize(Builder builder) {
        if (sCaton == null) {
            synchronized (AnrManager.class) {
                if (sCaton == null) {
                    sCaton = builder.build();
                }
            }
        }
    }

    public static void setLoggingEnabled(boolean enabled) {
        Config.LOG_ENABLED = enabled;
    }


    public static class Builder {
        private long mThresholdTime = DEFAULT_THRESHOLD_TIME;
        private long mCollectInterval = DEFAULT_COLLECT_INTERVAL;
        private Context mContext;
        private MonitorMode mMonitorMode = DEFAULT_MODE;
        private boolean loggingEnabled = true;
        private Callback mCallback;

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

        public Builder callback(Callback callback) {
            mCallback = callback;
            return this;
        }

        AnrManager build() {
            return new AnrManager(mContext, mThresholdTime, mCollectInterval, mMonitorMode, loggingEnabled, mCallback);
        }
    }

    public enum MonitorMode {
        LOOPER(0), FRAME(1);
        int value;

        MonitorMode(int mode) {
            this.value = mode;
        }
    }

    public interface Callback {
        void onBlockOccurs(String[] stackTraces, String anr, long... blockArgs);
    }
}
