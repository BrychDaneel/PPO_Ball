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
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
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
    public static float KVOLUME = 100000f;
    public static float MIN_VALUE = 0.003f;
    public static final float MASS = 0.01f;
    private final float LOGICAL_RADIUS = 0.3f;
    public static final float INC2METR = 0.025f;

    private SurfaceHolder surfaceHolder;
    private Matrix bgMatrix;
    private Bitmap ball, background;
    private SoundPool soundPool;
    private int sound;

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
        this.surfaceHolder = surfaceHolder;
        ball = BitmapFactory.decodeResource(context.getResources(), R.drawable.ball);
        background = BitmapFactory.decodeResource(context.getResources(), R.drawable.background);


        SensorManager sensorManager = (SensorManager) context.
                getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);

        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        sound = soundPool.load(context, R.raw.caughtball, 0);

        width = surfaceHolder.getSurfaceFrame().width();
        height = surfaceHolder.getSurfaceFrame().height();

        float pWidth = ball.getWidth();
        float pHeight = ball.getHeight();

        float scaleX = LOGICAL_RADIUS*width/pWidth;
        float scaleY = LOGICAL_RADIUS*height/pHeight;
        scale = Math.min(scaleX, scaleY);

        radius = scale * pWidth;

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        xdpm = metrics.xdpi / INC2METR;
        ydpm = metrics.ydpi / INC2METR;

        maxX = width - radius;
        maxY = height - radius;

        bgMatrix = new Matrix();
        bgMatrix.setScale(width / background.getWidth(), height / background.getHeight());
    }

    public long getTime() {
        return System.nanoTime() / 1_000_000;
    }

    private void playSound(float dE){
        float level = dE * KVOLUME;
        if (level > MIN_VALUE)
            soundPool.play(sound, level, level, 0, 0, 1);
    }

    private void redraw(Canvas canvas, float dtime){
        vx += gx * MASS * dtime * xdpm;
        vy += gy * MASS * dtime * ydpm;
        x -= vx * dtime;
        y += vy * dtime;
        if (x > maxX) {
            x = maxX;
            playSound((float)Math.pow(vx/xdpm*(1-ELASTIC), 2)*MASS);
            vx = - vx * ELASTIC;
            rotation = - ROTAR * dtime * vy/ydpm;
        }
        if (y > maxY){
            y = maxY;
            playSound((float)Math.pow(vy/ydpm*(1-ELASTIC), 2)*MASS);
            vy = - vy * ELASTIC;
            rotation = - ROTAR * dtime *  vx / xdpm;
        }
        if (x < 0) {
            x = 0;
            playSound((float)Math.pow(vx/xdpm*(1-ELASTIC), 2)*MASS);
            vx = - vx * ELASTIC;
            rotation = ROTAR * dtime * vy / ydpm;
        }
        if (y < 0){
            y = 0;
            playSound((float)Math.pow(vy/ydpm*(1-ELASTIC), 2)*MASS);
            vy = - vy * ELASTIC;
            rotation = ROTAR * dtime * vx / xdpm;
        }

        vx *= (1 - SPEED_FRICTION * dtime);
        vy *= (1 - SPEED_FRICTION * dtime);
        rotation *= (1 - ANGLE_FRICTION * dtime);

        angle += dtime * rotation;

        canvas.drawBitmap(background, bgMatrix, null);

        Matrix matrix = new Matrix();
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
            } else{
                try {
                    sleep(1000 / MAX_FPS / 3);
                } catch (InterruptedException ignored){}

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
