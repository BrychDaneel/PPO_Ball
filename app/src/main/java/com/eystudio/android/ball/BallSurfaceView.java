package com.eystudio.android.ball;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;

/**
 * Created by daneel on 28.10.17.
 */

public class BallSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    Context context;
    DrawThread drawThread;

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        drawThread = new DrawThread(context, getHolder());
        drawThread.setRun(true);
        drawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        drawThread.setRun(false);
        boolean retry = true;
        while (retry){
            try {
                drawThread.join();
                retry = false;
            } catch (InterruptedException e){}
        }
    }

    public BallSurfaceView(Context context){
        super(context);
        this.context = context;
        getHolder().addCallback(this);
    }

}
