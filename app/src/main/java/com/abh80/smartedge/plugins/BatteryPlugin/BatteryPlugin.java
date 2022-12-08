package com.abh80.smartedge.plugins.BatteryPlugin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.services.OverlayService;
import com.abh80.smartedge.utils.SettingStruct;
import com.abh80.smartedge.views.BatteryImageView;

import java.util.ArrayList;

public class BatteryPlugin extends BasePlugin {
    @Override
    public String getID() {
        return "BatteryPlugin";
    }

    @Override
    public String getName() {
        return "Battery Plugin";
    }

    private OverlayService ctx;
    //这里的OverlayService是一个Service，用于显示悬浮窗，可以理解为一个Activity

    @Override
    public void onCreate(OverlayService context) {
        ctx = context;
        ctx.registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        //这里的intentFilter是一个action，这个action是系统定义好的，我们只要监听这个action就可以了
    }//每个plugin都需要onCreate方法，作用是初始化并在需要时注册广播接收器

    private View mView;
    //这里定义的是一个view，这个view就是我们要显示的view，也就是battery的view

    @Override
    public View onBind() //onbind函数的作用是获取我们要显示的view

    {
        mView = LayoutInflater.from(ctx).inflate(R.layout.battery_layout, null);
        //这里的LayoutInflater.from(ctx).inflate(R.layout.battery_layout, null)的作用是获取我们的battery_layout.xml文件
        init();
        //init函数的作用是初始化我们的view
        return mView;
    }

    private void init() {
        tv = mView.findViewById(R.id.text_percent);
        //这里的mView.findViewById(R.id.text_percent)的作用是获取我们的battery_layout.xml文件中的text_percent这个id
        batteryImageView = mView.findViewById(R.id.cover);
        //这里的R.id.cover指的是我们的battery_layout.xml文件中的cover这个id
        //获取完之后，我们就可以对这个view进行操作了
        updateView();
    }

    float batteryPercent;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {//这里的onReceive函数的作用是监听我们的电量
            int status = intent.getExtras().getInt(BatteryManager.EXTRA_STATUS);//status是电量的状态
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;//isCharging是一个boolean值，用于判断是否在充电
            if (isCharging) {
                ctx.enqueue(BatteryPlugin.this);//这里的ctx.enqueue(BatteryPlugin.this)的作用是将我们的view显示出来
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);//这里的level是电量的百分比
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);//这里的scale是电量的最大值
                batteryPercent = level * 100 / (float) scale;//这里的batteryPercent是电量的百分比
                updateView();//这里的updateView()的作用是更新我们的view
            } else {
                ctx.dequeue(BatteryPlugin.this);//这里的ctx.dequeue(BatteryPlugin.this)的作用是将我们的view隐藏起来
                if (tv != null && batteryImageView != null) {//这里的tv != null && batteryImageView != null的作用是判断我们的view是否为空
                    ValueAnimator valueAnimator = ValueAnimator.ofInt(0, ctx.dpToInt(0));//这里的ValueAnimator.ofInt(0, ctx.dpToInt(0))的作用是设置我们的view的动画
                    valueAnimator.setDuration(300);//这里的valueAnimator.setDuration(300)的作用是设置我们的view的动画时间
                    valueAnimator.addUpdateListener(valueAnimator1 -> {//这里的valueAnimator.addUpdateListener(valueAnimator1 -> {的作用是设置我们的view的动画监听
                        ViewGroup.LayoutParams p = batteryImageView.getLayoutParams();//这里的ViewGroup.LayoutParams p = batteryImageView.getLayoutParams()的作用是获取我们的view的LayoutParams
                        p.width = (int) valueAnimator1.getAnimatedValue();
                        p.height = (int) valueAnimator1.getAnimatedValue();
                        batteryImageView.setLayoutParams(p);//这里的batteryImageView.setLayoutParams(p)的作用是设置我们的view的LayoutParams
                    });
                    valueAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {//这里的onAnimationStart(Animator animation)的作用是设置我们的view的动画开始时的监听
                            super.onAnimationEnd(animation);//这里的super.onAnimationEnd(animation)的作用是设置我们的view的动画结束时的监听
                            tv.setVisibility(View.INVISIBLE);//这里的tv.setVisibility(View.INVISIBLE)的作用是将我们的view隐藏起来
                        }
                    });
                    valueAnimator.start();//这里的valueAnimator.start()的作用是开始我们的view的动画
                }
            }
        }
    };
    private TextView tv;
    private BatteryImageView batteryImageView;

    private void updateView()//这个updateView函数的通过当前充电信号之类的来更新我们的view
    {
        if (mView != null) {
            tv.setText((int) batteryPercent + "%");//这里的tv.setText((int) batteryPercent + "%")的作用是设置我们的text_percent这个id的text
            batteryImageView.updateBatteryPercent(batteryPercent);//这里的batteryImageView.updateBatteryPercent(batteryPercent)的作用是设置我们的cover这个id的图片
            if (batteryPercent > 80) {
                batteryImageView.setStrokeColor(Color.GREEN);
                tv.setTextColor(Color.GREEN);
            } else if (batteryPercent < 80 && batteryPercent > 20) {
                batteryImageView.setStrokeColor(Color.YELLOW);
                tv.setTextColor(Color.YELLOW);
            } else {
                batteryImageView.setStrokeColor(Color.RED);
                tv.setTextColor(Color.RED);
            }
        }
    }

    @Override
    public void onUnbind() {

        mView = null;
    }

    @Override
    public void onBindComplete() {
        if (mView == null) return;
        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, ctx.dpToInt(ctx.minHeight / 4));
        valueAnimator.setDuration(300);
        valueAnimator.addUpdateListener(valueAnimator1 -> {
            ViewGroup.LayoutParams p = batteryImageView.getLayoutParams();
            p.width = (int) valueAnimator1.getAnimatedValue();
            p.height = (int) valueAnimator1.getAnimatedValue();
            batteryImageView.setLayoutParams(p);
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                tv.setVisibility(View.VISIBLE);
                batteryImageView.requestLayout();
                batteryImageView.updateBatteryPercent(batteryPercent);
            }
        });
        valueAnimator.start();
    }

    @Override
    public void onDestroy() {
        ctx.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onExpand() {

    }

    @Override
    public void onCollapse() {

    }

    @Override
    public void onClick() {

    }

    @Override
    public String[] permissionsRequired() {
        return null;
    }

    @Override
    public ArrayList<SettingStruct> getSettings() {
        return null;
    }
}
