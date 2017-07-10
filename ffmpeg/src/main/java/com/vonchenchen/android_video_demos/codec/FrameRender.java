package com.vonchenchen.android_video_demos.codec;

import android.graphics.Paint;
import android.view.Surface;

/**
 * Created by lidechen on 5/31/17.
 */

public class FrameRender extends CodecWrapper {

    private static final String TAG = "FrameRender";

    private Paint mPaint;

    public FrameRender(Surface surface){
        super(surface);
        mPaint = new Paint();
    }

    @Override
    void getOneFrame(int[] data, int width, int height) {

    }
}
