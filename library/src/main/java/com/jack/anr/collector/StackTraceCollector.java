package com.jack.anr.collector;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import java.util.ArrayDeque;
import java.util.Map;

import static com.jack.anr.AnrConstant.THREAD_TAG;





/**
 * stack trace的收集器（收集ANR的错误日志）
 * @author Jack
 */
public class StackTraceCollector implements Collector {

    private final static String TAG = "StackTraceCollector";
    /**
     * 收集ANR的异常标志
     */
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
    /**
     * 消息收集器处理handler
     */
    private volatile CollectorHandler mCollectorHandler;
    /**
     * 双端队列
     */
    private ArrayDeque<String> mStackQueue;
    /**
     * 采样的次数
     */
    private int mCollectCount;

    private volatile Looper mLooper;



    public StackTraceCollector(long collectInterval) {
        mCollectInterval = collectInterval;
        HandlerThread thread = new HandlerThread(TAG);
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mLooper = thread.getLooper();
        mCollectorHandler = new CollectorHandler(mLooper);
        int space = (int) (COLLECT_SPACE_TIME / mCollectInterval);
        mCollectCount = space <= MIN_COLLECT_COUNT ? MIN_COLLECT_COUNT : space;
        mStackQueue = new ArrayDeque<String>(mCollectCount);
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
        if (mStackQueue.size() >= mCollectCount) {
            mStackQueue.poll();   //获取并移除此双端队列所表示的队列的头
        }
        mStackQueue.offer(stackTrace);   //将指定元素插入此双端队列的末尾
    }

    /**
     * 获取所有的stack trace信息
     * @return
     */
    private String getAllStackInfo() {
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
}