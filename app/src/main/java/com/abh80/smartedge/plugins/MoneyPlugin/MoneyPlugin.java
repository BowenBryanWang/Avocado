package com.abh80.smartedge.plugins.MoneyPlugin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.AlarmPlugin.AlarmPlugin;
import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.services.NotiService;
import com.abh80.smartedge.services.OverlayService;
import com.abh80.smartedge.utils.SettingStruct;
import com.google.android.material.imageview.ShapeableImageView;

//该文件是实现对于系统闹钟的读取和设置

import java.util.ArrayList;
import java.util.Calendar;
//倒入PendingIntent类
import android.app.PendingIntent;
import android.os.Handler;


public class MoneyPlugin extends BasePlugin{
    @Override
    public String getID() {
        return "MoneyPlugin";
    }

    @Override
    public String getName() {
        return "Money Plugin";
    }

    private OverlayService ctx;

    @Override
    public void onCreate(OverlayService context) {
        ctx = context;
        mHandler = new Handler(context.getMainLooper());//创建我们的handler
        mView = LayoutInflater.from(context).inflate(R.layout.money_layout, null);//创建我们的视图
        mView.findViewById(R.id.blank_space).setVisibility(View.VISIBLE);//设置我们的空白的空间为可见
        //1. 到点弹出激活？
        //2. 还是一直后台运行该插件，到点改变视图（弹出悬浮窗）？
        //实现1在此处更方便，onCreate 和 onBind 应用不同
        init();//初始化我们的插件
    }

    private View mView;

    @Override
    public View onBind() {
        mView = LayoutInflater.from(ctx).inflate(R.layout.money_layout, null);
        init();
        return mView;
    }
    private TextView title;
    private TextView Desc;
    private ImageView come;
    private TextView inside_text;
    private AlarmManager alarmManager;

    private void init(){
        title = mView.findViewById(R.id.title);
        cover = mView.findViewById(R.id.cover);
        Desc = mView.findViewById(R.id.description);
        come = mView.findViewById(R.id.come);
        inside_text = mView.findViewById(R.id.inside);
//        times[0] = mView.findViewById(R.id.time0);
//        times[1] = mView.findViewById(R.id.time1);
//        times[2] = mView.findViewById(R.id.time2);
//        times[3] = mView.findViewById(R.id.time3);
//        times[4] = mView.findViewById(R.id.time4);
//        alarms[0] = mView.findViewById(R.id.alarm0);
//        alarms[1] = mView.findViewById(R.id.alarm1);
//        alarms[2] = mView.findViewById(R.id.alarm2);
//        alarms[3] = mView.findViewById(R.id.alarm3);
//        alarms[4] = mView.findViewById(R.id.alarm4);
//        for (int i = 0; i < 5; i++) {
//            int finalI = i;
//            alarms[i].setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    //如果该闹钟未设置，则设置该闹钟
//                    if (alarms[finalI].getTag() == null) {
//                        alarms[finalI].setImageResource(R.drawable.alarm_on);
//                        alarms[finalI].setTag("on");
//                    } else {
//                        //如果该闹钟已经设置，则取消该闹钟
//                        alarms[finalI].setImageResource(R.drawable.alarm_off);
//                        alarms[finalI].setTag(null);
//                    }
//                }
//            });
//        }


        inside_text.setText("检测到收入+1000元");
        come.setImageDrawable(ctx.getDrawable(R.drawable.income));
//        for (int i = 0; i < 5; i++) {
//            times[i].setText("8:00");
//        }
//        ctx.enqueue(this);
//        alarmManager = (AlarmManager) ctx.getSystemService(ctx.ALARM_SERVICE);

        updateView();
    }

    private void updateView(){
        if (mView == null) return;
    }

    // 检测用户是否睡眠前的定时任务
    private Handler mHandler = new Handler();


    @Override
    public void onUnbind() {
        mView = null;
    }
    @Override
    public void onDestroy() {

    }
    private boolean expanded=false;
    private ShapeableImageView cover;
    @Override
    public void onExpand() {

        if (expanded) return;
        expanded = true;
        DisplayMetrics metrics = ctx.metrics;
        ctx.animateOverlay2(ctx.dpToInt(100), metrics.widthPixels - ctx.dpToInt(15), expanded);
        animateChild(true, ctx.dpToInt(76));
        mView.findViewById(R.id.content).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.inside_text).setVisibility(View.GONE);


    }
    private void animateChild(boolean expanding, int h) {//林：expanding开启下的动态高度
        View view1 = cover;
//        View view2 = visualizer;

        ValueAnimator height_anim = ValueAnimator.ofInt(view1.getHeight(), h);
        height_anim.setDuration(500);
        height_anim.addUpdateListener(valueAnimator -> {
            ViewGroup.LayoutParams params1 = view1.getLayoutParams();
//            ViewGroup.LayoutParams params2 = view2.getLayoutParams();
            params1.height = (int) valueAnimator.getAnimatedValue();
//            params2.height = (int) valueAnimator.getAnimatedValue();
            params1.width = (int) valueAnimator.getAnimatedValue();
//            params2.width = (int) valueAnimator.getAnimatedValue();
            view1.setLayoutParams(params1);
//            view2.setLayoutParams(params2);
        });
        height_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!expanding) {
//                    view2.setVisibility(View.VISIBLE);
//                    visualizer.paused = false;
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (expanding) {
//                    view2.setVisibility(View.GONE);
//                    visualizer.paused = true;
                }
            }
        });
        height_anim.setInterpolator(new OvershootInterpolator(0.5f));
        height_anim.start();


    }

    @Override
    public void onCollapse() {
        if (!expanded) return;
        expanded = false;
        ctx.animateOverlay2(ctx.minHeight, ViewGroup.LayoutParams.WRAP_CONTENT, expanded);
        animateChild(false, ctx.dpToInt(ctx.minHeight / 4));
        mView.findViewById(R.id.content).setVisibility(View.GONE);
        mView.findViewById(R.id.controls_holder).setVisibility(View.GONE);
        mView.findViewById(R.id.inside_text).setVisibility(View.VISIBLE);
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
