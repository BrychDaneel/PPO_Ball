package com.eystudio.android.ball;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;

/**
 * Created by daneel on 28.10.17.
 */

public class DrawThread extends Thread implements SensorEventListener{

    private static long MAX_FPS = 30;
    public static float ROTAR = 3000f;
    public static float SPEED_FRICTION = 0.3f;
    public static float ANGLE_FRICTION = 0.3f;
    public static float ELASTIC = 0.8f;
    public static final float MASS = 0.01f;
    private final float LOGICAL_RADIUS = 0.3f;
    public static final float INC2METR = 0.025f;

    private SurfaceHolder surfaceHolder;
    private Matrix matrix;
    private Bitmap ball;

    private boolean run;

    private float width, height;
    private float maxX, maxY;
    private float gx, gy, gz;
    private float x = 0, y = 0;
    private float angle;
    private float vx = 0, vy = 0;
    private float rotation;
    private float xdpm, ydpm;
    private float radius;
    float scale;
    Sensor gyroscope;

    public void setRun(boolean run){
        this.run = run;
    }

    public DrawThread(Context context, SurfaceHolder surfaceHolder){
        super();
        matrix = new Matrix();
        this.surfaceHolder = surfaceHolder;

        width = surfaceHolder.getSurfaceFrame().width();
        height = surfaceHolder.getSurfaceFrame().height();

        ball = BitmapFactory.decodeResource(context.getResources(), R.drawable.ball);

        float pWidth = ball.getWidth();
        float pHeight = ball.getHeight();

        float scaleX = LOGICAL_RADIUS*width/pWidth;
        float scaleY = LOGICAL_RADIUS*pHeight/height;
        scale = Math.min(scaleX, scaleY);
        matrix.setScale(scale, scale);

        radius = scale * pWidth;

        SensorManager sensorManager = (SensorManager) context.
                getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        xdpm = metrics.xdpi / INC2METR;
        ydpm = metrics.ydpi / INC2METR;

        maxX = width - radius;
        maxY = height - radius;
    }

    public long getTime() {
        return System.nanoTime() / 1_000_000;
    }

    private void redraw(Canvas canvas, float dtime){
        vx += gx * MASS * dtime * xdpm;
        vy += gy * MASS * dtime * ydpm;
        x -= vx * dtime;
        y += vy * dtime;
        if (x > maxX) {
            x = maxX;
            vx = - vx * ELASTIC;
            rotation = - ROTAR * dtime * vy / ydpm;
        }
        if (y > maxY){
            y = maxY;
            vy = - vy * ELASTIC;
            rotation = - ROTAR * dtime *  vx / xdpm;
        }
        if (x < 0) {
            x = 0;
            vx = - vx * ELASTIC;
            rotation = ROTAR * dtime * vy / ydpm;
        }
        if (y < 0){
            y = 0;
            vy = - vy * ELASTIC;
            rotation = ROTAR * dtime * vx / xdpm;
        }

        vx *= (1 - SPEED_FRICTION * dtime);
        vy *= (1 - SPEED_FRICTION * dtime);
        rotation *= (1 - ANGLE_FRICTION * dtime);

        angle += dtime * rotation;
        canvas.drawColor(Color.BLACK);
        matrix.setScale(scale, scale);
        matrix.postRotate(angle / (float)Math.PI * 180, radius / 2, radius / 2);
        matrix.postTranslate(x, y);
        canvas.drawBitmap(ball, matrix, null);
    }

    @Override
    public void run(){
        long last_time = getTime();
        while (run){
            long time = getTime();
            long dtime = time - last_time;
            if (dtime >= 1000 / MAX_FPS){
                last_time = time;

                Canvas canvas = surfaceHolder.lockCanvas(null);
                if (canvas == null)
                    continue;
                redraw(canvas, dtime / 1000f);
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        gx = sensorEvent.values[0]; //Плоскость XY
        gy = sensorEvent.values[1]; //Плоскость XZ
        gz = sensorEvent.values[2]; //Плоскость ZY
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}