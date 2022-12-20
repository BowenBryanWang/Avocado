package com.abh80.smartedge.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import com.abh80.smartedge.ActionRecordControlller;
import com.abh80.smartedge.RecordService;

public class RecordButton extends View
{
    private Paint mPaint;
    private RecordService service;
    private String textToShow="";


    public RecordButton(Context context,RecordService service,ActionRecordControlller actionRecordControlller,WindowManager wm,WindowManager.LayoutParams wmParams)
    {
        super(context);
        this.service = service;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.setClickable(false);
        this.setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        int length = textToShow.length();
        mPaint.setTextSize(50);
        mPaint.setColor(Color.BLACK);
        mPaint.setAlpha(255);
        canvas.drawText(textToShow,0,50,mPaint);
        mPaint.setColor(Color.GRAY);
        mPaint.setAlpha(127);
        canvas.drawRect(0,0,Math.min(50+length*50,2000),80,mPaint);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK)
            service.onBack();
        else if(keyCode==KeyEvent.KEYCODE_VOLUME_UP)
            service.removeButton();
        return super.onKeyDown(keyCode, event);
    }

    public void refreshText(String text)
    {
        if(text!=null)
            textToShow = text;
        this.postInvalidate();
    }
}
