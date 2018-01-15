package com.jack.anr;



/**
 * 收集器接口
 * @author Jack
 */
public interface Collector {

    /**
     * 获取stack trace数组
     * @return
     */
    String[] getStackTraceInfo();

    /**
     * 添加stack trace
     * @param stackTrace
     */
    void add(String stackTrace);
}
