package com.abh80.smartedge;

import android.accessibilityservice.AccessibilityService.GestureResultCallback;
import android.accessibilityservice.GestureDescription;
import android.app.Activity;
import android.graphics.Path;
import android.view.View;

import com.google.gson.JsonObject;

public class Interaction {
    static View view = null;
    static Activity activity;
    static void init(Activity _activity, View _view) {
        activity = _activity;
        view = _view;
    }
    static void performInteraction(JsonObject interactionJson) {
            System.out.println("performInteraction");
            RecordService server = RecordService.self;
            String nodeId = interactionJson.get("node_id").getAsString();
            String trailStr = interactionJson.get("trail").toString();
            trailStr = trailStr.substring(2, trailStr.length() - 2);
            String[] splitStr = trailStr.split("]\\[");
            if (interactionJson.get("action_type").getAsString().equals("click")) {
                performClick(splitStr);

            }
            if (interactionJson.get("action_type").getAsString().equals("scroll")) {
                performScroll(splitStr);
            }
    }

    static public void performClick(String[] splitStr) {
        for (String trail : splitStr) {
            String[] pos = trail.split(",");
            float x = Float.parseFloat(pos[0]);
            float y = Float.parseFloat(pos[1]);
            clickView(x, y);
            System.out.println("click: " + Float.toString(x) + "," + Float.toString(y));
            if (RecordService.mActionRecordController != null) {
                RecordService.mActionRecordController.checkShotChange(RecordService.mergedBlock, RecordService.exec);
            }
        }
    }

    static public void performScroll(String[] splitStr) {
        String[] pos = splitStr[0].split(",");
        float fromX = Float.parseFloat(pos[0]);
        float fromY = Float.parseFloat(pos[1]);
        pos = splitStr[1].split(",");
        float endX = Float.parseFloat(pos[0]);
        float endY = Float.parseFloat(pos[1]);
        scrollView(fromX, fromY, endX, endY);
        System.out.println("scroll: " + Float.toString(fromX) + "," + Float.toString(fromY) + Float.toString(endX) + "," + Float.toString(endY) );
        if (RecordService.mActionRecordController != null) {
            RecordService.mActionRecordController.checkShotChange(RecordService.mergedBlock, RecordService.exec);
        }
    }

    static public void clickView(float x, float y) {
        /*long downTime = SystemClock.currentThreadTimeMillis();
        final MotionEvent downEvent = MotionEvent.obtain(
                downTime, downTime+100, MotionEvent.ACTION_DOWN, x, y, 0);
        downTime = SystemClock.currentThreadTimeMillis();
        final MotionEvent upEvent = MotionEvent.obtain(
                downTime, downTime+100, MotionEvent.ACTION_UP, x, y, 0);
        activity.dispatchTouchEvent(downEvent);
        activity.dispatchTouchEvent(upEvent);
        downEvent.recycle();
        upEvent.recycle();*/
        /*new Thread() {
            public void run() {
                Instrumentation inst = new Instrumentation();
                inst.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis()+100,
                        MotionEvent.ACTION_DOWN, x, y, 0));
                inst.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis()+300,SystemClock.uptimeMillis()+400,
                        MotionEvent.ACTION_UP, x, y, 0));
            }
        }.start();*/
        new Thread() {
            public void run() {
                GestureDescription.Builder builder = new GestureDescription.Builder();
                Path path = new Path();
                path.moveTo(x, y);
                path.lineTo(x, y);
                /**
                 * 参数path：笔画路径
                 * 参数startTime：时间 (以毫秒为单位)，从手势开始到开始笔划的时间，非负数
                 * 参数duration：笔划经过路径的持续时间(以毫秒为单位)，非负数
                 */
                builder.addStroke(new GestureDescription.StrokeDescription(path,1,100));
                final GestureDescription build = builder.build();
                /**
                 * 参数GestureDescription：翻译过来就是手势的描述，如果要实现模拟，首先要描述你的腰模拟的手势嘛
                 * 参数GestureResultCallback：翻译过来就是手势的回调，手势模拟执行以后回调结果
                 * 参数handler：大部分情况我们不用的话传空就可以了
                 * 一般我们关注GestureDescription这个参数就够了，下边就重点介绍一下这个参数
                 */
                if (
                RecordService.self.dispatchGesture(build,new GestureResultCallback()
                {
                    public void onCancelled (GestureDescription gestureDescription){
                        System.out.println("模拟手势失败");
                        super.onCancelled(gestureDescription);
                    }
                    public void onCompleted (GestureDescription gestureDescription){
                        System.out.println("模拟手势成功");
                        ActionRecordControlller.islastSent = false;
                        super.onCompleted(gestureDescription);
                    }
                },null) ) {
                    System.out.println("触发模拟手势");
                }
                else {
                    System.out.println("触发失败");
                }
            }
        }.start();
    }

    static public void scrollView(float fromX, float fromY, float endX, float endY) {
        new Thread() {
            public void run() {
                GestureDescription.Builder builder = new GestureDescription.Builder();
                Path path = new Path();
                path.moveTo(fromX, fromY);
                path.lineTo(endX, endY);
                /**
                 * 参数path：笔画路径
                 * 参数startTime：时间 (以毫秒为单位)，从手势开始到开始笔划的时间，非负数
                 * 参数duration：笔划经过路径的持续时间(以毫秒为单位)，非负数
                 */
                builder.addStroke(new GestureDescription.StrokeDescription(path,1,100));
                final GestureDescription build = builder.build();
                /**
                 * 参数GestureDescription：翻译过来就是手势的描述，如果要实现模拟，首先要描述你的腰模拟的手势嘛
                 * 参数GestureResultCallback：翻译过来就是手势的回调，手势模拟执行以后回调结果
                 * 参数handler：大部分情况我们不用的话传空就可以了
                 * 一般我们关注GestureDescription这个参数就够了，下边就重点介绍一下这个参数
                 */
                if (
                        RecordService.self.dispatchGesture(build,new GestureResultCallback()
                        {
                            public void onCancelled (GestureDescription gestureDescription){
                                System.out.println("模拟手势失败");
                                super.onCancelled(gestureDescription);
                            }
                            public void onCompleted (GestureDescription gestureDescription){
                                System.out.println("模拟手势成功");
                                ActionRecordControlller.islastSent = false;
                                super.onCompleted(gestureDescription);
                            }
                        },null) ) {
                    System.out.println("触发模拟手势");
                }
                else {
                    System.out.println("触发失败");
                }
            }
        }.start();
    }
}
