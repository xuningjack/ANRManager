package com.jack.anr;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import java.util.ArrayDeque;
import java.util.Map;



/**
 * stack trace的收集器
 * @author Jack
 */
public class StackTraceCollector implements Collector {

    private final static String TAG = "StackTraceCollector";
    private final static String THREAD_TAG = "------";
    private final static int COLLECT_MSG = 0x0037;
    /**
     * 收集日志的间隔时间
     */
    private final static int COLLECT_SPACE_TIME = 5000;
    /**
     * 最小间隔收集次数
     */
    private final static int MIN_COLLECT_COUNT = 5;
    /**
     * 收集的时间间隔
     */
    private long mCollectInterval;
    private volatile Looper mLooper;
    private volatile CollectorHandler mCollectorHandler;
    private ArrayDeque<String> mStackQueue;
    private int mLimitLength;


    public StackTraceCollector(long collectInterval) {
        mCollectInterval = collectInterval;
        HandlerThread thread = new HandlerThread(TAG);
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mLooper = thread.getLooper();
        mCollectorHandler = new CollectorHandler(mLooper);
        int space = (int) (COLLECT_SPACE_TIME / mCollectInterval);
        mLimitLength = space <= MIN_COLLECT_COUNT ? MIN_COLLECT_COUNT : space;
        mStackQueue = new ArrayDeque<>(mLimitLength);
        trigger();
    }

    /**
     * 触发收集日志
     */
    public void trigger() {
        Message message = mCollectorHandler.obtainMessage();
        message.obj = this;
        message.what = COLLECT_MSG;
        mCollectorHandler.sendMessageDelayed(message, mCollectInterval);
    }

    @Override
    public String[] getStackTraceInfo() {
        return mStackQueue.toArray(new String[0]);
    }

    @Override
    public void add(String stackTrace) {
        if (mStackQueue.size() >= mLimitLength) {
            mStackQueue.poll();
        }
        mStackQueue.offer(stackTrace);
    }

    /**
     * 消息收集器处理handler
     */
    private static class CollectorHandler extends Handler {

        public CollectorHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == COLLECT_MSG) {
                StackTraceCollector traceCollector = (StackTraceCollector) msg.obj;
                traceCollector.add(traceCollector.getAllStackInfo());
                traceCollector.trigger();
            }
        }
    }

    /**
     * 获取所有的stack trace信息
     * @return
     */
    private String getAllStackInfo() {
        Thread main = Looper.getMainLooper().getThread();
        Map<Thread, StackTraceElement[]> allLiveThreadStackMap = Thread.getAllStackTraces();
        StringBuilder stackBuilder = new StringBuilder(128);
        for (Thread item : allLiveThreadStackMap.keySet()) {
            StackTraceElement[] stackTraceElements = item.getStackTrace();
            if (stackTraceElements != null && stackTraceElements.length > 0) {
                stackBuilder.append(THREAD_TAG).append(item.getName()).append("\n");
                for (StackTraceElement stackTraceElement : stackTraceElements) {
                    stackBuilder.append("\tat ").append(stackTraceElement.toString()).append("\n");
                }
            }
        }
        return stackBuilder.toString();
    }

}
