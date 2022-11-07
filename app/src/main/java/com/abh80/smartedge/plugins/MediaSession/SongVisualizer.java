package com.abh80.smartedge.plugins.MediaSession;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SongVisualizer extends View {
    Visualizer visualizer;

    public SongVisualizer(Context context) {
        super(context);
        init();
    }

    public SongVisualizer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SongVisualizer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public boolean paused;//这里定义了一个boolean变量，用于判断是否暂停
    private byte[] bytes;//这里定义了一个byte数组，用于存储我们的音频数据

    public void setPlayerId(int sessionID) {//这里定义了一个setPlayerId函数，用于设置我们的音频数据
        try {
            if (visualizer != null) {
                release();//这里的release函数的作用是释放visualizer
                visualizer = null;
            }
            visualizer = new Visualizer(sessionID);//这里的sessionID是
            visualizer.setEnabled(false);//这里的setEnabled函数的作用是设置visualizer是否可用
            visualizer.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);//这里的setScalingMode函数的作用是设置visualizer的缩放模式
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);//这里的setCaptureSize函数的作用是设置visualizer的采样大小
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {//这里的setDataCaptureListener函数的作用是设置visualizer的数据监听器
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                                                  int samplingRate) {//这里的onWaveFormDataCapture函数的作用是设置visualizer的波形数据监听器
                    SongVisualizer.this.bytes = bytes;//这里的bytes是我们的音频数据
                    if (!paused) invalidate();//这里的invalidate函数的作用是刷新视图
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] bytes,
                                             int samplingRate) {//这里的onFftDataCapture函数的作用是设置visualizer的FFT数据监听器
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, false);//这里的setMaxCaptureRate函数的作用是设置visualizer的采样率


            visualizer.setEnabled(true);//这里的setEnabled函数的作用是设置visualizer是否可用
        } catch (Exception e) {
            // do nothing lol
        }
    }

    public void release() {
        //will be null if setPlayer hasn't yet been called
        if (visualizer == null)
            return;

        visualizer.release();
        bytes = null;
        invalidate();
    }

    private void init() {
        setColor(Color.BLUE);//这里的setColor函数的作用是设置画笔的颜色
        paint.setStyle(Paint.Style.FILL);//这里的setStyle函数的作用是设置画笔的样式
        paint.setStrokeCap(Paint.Cap.ROUND);//这里的setStrokeCap函数的作用是设置画笔的笔触样式
    }

    public void setColor(int Color) {
        paint.setColor(Color);
    }

    private final Paint paint = new Paint();

    @Override
    protected void onDraw(Canvas canvas) {//这里的onDraw函数的作用是绘制视图
        float density = 8;

        if (bytes != null) {
            float barWidth = getWidth() / density;//这里的getWidth函数的作用是获取视图的宽度
            float div = bytes.length / density;//这里的bytes.length函数的作用是获取bytes的长度
            paint.setStrokeWidth(barWidth - 4);//这里的setStrokeWidth函数的作用是设置画笔的笔触宽度

            for (int i = 0; i < density; i++) {//这里的density是我们的音频数据的长度
                int bytePosition = (int) Math.ceil(i * div);//这里的ceil函数的作用是向上取整
                float barX = (i * barWidth) + (barWidth / 2);//这里的barX是我们的音频数据的X坐标
                if (bytes[bytePosition] == 0 || bytes[bytePosition] + 128 == 0) {//这里的bytes[bytePosition]是我们的音频数据
                    canvas.drawLine(barX, (getHeight() / 2f), barX, (getHeight() / 2f), paint);//这里的getHeight函数的作用是获取视图的高度
                } else {
                    int top = (getHeight() - 20) +
                            ((byte) (Math.abs(bytes[bytePosition]) + 128)) * (getHeight() - 20) / 128;//这里的getHeight函数的作用是获取视图的高度
                    canvas.drawLine(barX, ((getHeight() + 20) - top) / 2f, barX, top, paint);//这里的getHeight函数的作用是获取视图的高度
                }
            }
            super.onDraw(canvas);//这里的super.onDraw函数的作用是调用父类的onDraw函数
        }
    }
}
