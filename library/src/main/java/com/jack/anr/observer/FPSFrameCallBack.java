package com.jack.anr.observer;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Choreographer;
import android.view.Display;
import android.view.WindowManager;

import com.jack.anr.Config;


/**
 * ANR回调(API 16上才能使用)
 * @author Jack
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class FPSFrameCallBack implements Choreographer.FrameCallback {

    private static final String TAG = "FPSFrameCallBack";
    private static long SKIPPED_FRAME_ANR_TRIGGER = 0;
    /**
     * 跳帧的临界值，大于此值可能发生ANR
     */
    private static long SKIPPED_FRAME_WARNING_LIMIT = 0;
    private long mLastFrameTimeNanos;
    private long mFrameIntervalNanos;
    private BlockHandler mBlockHandler;



    public FPSFrameCallBack(Context context, BlockHandler blockHandler) {
        float mRefreshRate = getRefreshRate(context);
        mFrameIntervalNanos = (long) (1000000000l / mRefreshRate);
        SKIPPED_FRAME_WARNING_LIMIT = Config.THRESHOLD_TIME * 1000l * 1000l / mFrameIntervalNanos;
        SKIPPED_FRAME_ANR_TRIGGER = 5000000000l / mFrameIntervalNanos;
        Config.log(TAG, "SKIPPED_FRAME_WARNING_LIMIT : " + SKIPPED_FRAME_WARNING_LIMIT +
                " ,SKIPPED_FRAME_ANR_TRIGGER : " + SKIPPED_FRAME_ANR_TRIGGER);
        mBlockHandler = blockHandler;
    }


    private float getRefreshRate(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        return display.getRefreshRate();
    }


    @Override
    public void doFrame(long frameTimeNanos) {
        if (mLastFrameTimeNanos == 0) {
            mLastFrameTimeNanos = frameTimeNanos;
            Choreographer.getInstance().postFrameCallback(this);
            return;
        }
        final long jitterNanos = frameTimeNanos - mLastFrameTimeNanos;
        if (jitterNanos >= mFrameIntervalNanos) {
            final long skippedFrames = jitterNanos / mFrameIntervalNanos;
            if (skippedFrames >= SKIPPED_FRAME_WARNING_LIMIT) {
                Config.log(TAG, "Skipped " + skippedFrames + " frames!  "
                        + "The application may be doing too much work on its main thread.");
                mBlockHandler.notifyBlockOccurs(
                        skippedFrames >= SKIPPED_FRAME_ANR_TRIGGER,
                        skippedFrames);
            }
        }
        mLastFrameTimeNanos = frameTimeNanos;
        Choreographer.getInstance().postFrameCallback(this);
    }
}
