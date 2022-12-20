package com.abh80.smartedge.plugins.AlarmPlugin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.content.Intent;
import android.provider.AlarmClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.services.NotiService;
import com.abh80.smartedge.services.OverlayService;
import com.abh80.smartedge.utils.SettingStruct;
import com.google.android.material.imageview.ShapeableImageView;

//该文件是实现对于系统闹钟的读取和设置

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
//倒入PendingIntent类
import android.app.PendingIntent;
import android.os.Handler;


//在该插件中，我们要完成的功能是：
//1，检测到用户睡眠前，弹出悬浮岛，并且显示用户可能明天需要设置的闹钟
//2，用户选择闹钟后，设置闹钟
//3，用户选择不设置闹钟后，关闭悬浮岛
//4，呈现闹钟时，应该根据用户的习惯，或者用户的日程安排，推荐理想合适的闹钟给用户


public class AlarmPlugin extends BasePlugin{
    @Override
    public String getID() {
        return "AlarmManager";
    }

    @Override
    public String getName() {
        return "Alarm Manager";
    }

    private OverlayService ctx;
    @Override
    public void onCreate(OverlayService context) throws URISyntaxException {
        ctx = context;
        mHandler = new Handler(context.getMainLooper());//创建我们的handler
        mView = LayoutInflater.from(context).inflate(R.layout.alarm_layout, null);//创建我们的视图
        mView.findViewById(R.id.blank_space).setVisibility(View.VISIBLE);//设置我们的空白的空间为可见
        //1. 到点弹出激活？
        //2. 还是一直后台运行该插件，到点改变视图（弹出悬浮窗）？
        //实现1在此处更方便，onCreate 和 onBind 应用不同
        init();//初始化我们的插件
    }

    private View mView;

    @Override
    public View onBind() {
        mView = LayoutInflater.from(ctx).inflate(R.layout.alarm_layout, null);
        return mView;
    }
    private TextView title;
    //    #alarms应该是四个闹钟的列表
    private TextView[] times = new TextView[4];
    private ImageView[] alarms = new ImageView[4];

    private AlarmManager alarmManager;

    public void setAlarm(int hour, int minute) {
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                //闹钟的小时
                .putExtra(AlarmClock.EXTRA_HOUR, hour)
                //闹钟的分钟
                .putExtra(AlarmClock.EXTRA_MINUTES, minute)
                //响铃时提示的信息
//                .putExtra(AlarmClock.EXTRA_MESSAGE, message)
                //用于指定该闹铃触发时是否振动
                .putExtra(AlarmClock.EXTRA_VIBRATE, true)
                //一个 content: URI，用于指定闹铃使用的铃声，也可指定 VALUE_RINGTONE_SILENT 以不使用铃声。
                //如需使用默认铃声，则无需指定此 extra。
//                .putExtra(AlarmClock.EXTRA_RINGTONE, ringtoneUri)
                //一个 ArrayList，其中包括应重复触发该闹铃的每个周日。
                // 每一天都必须使用 Calendar 类中的某个整型值（如 MONDAY）进行声明。
                //对于一次性闹铃，无需指定此 extra
//                .putExtra(AlarmClock.EXTRA_DAYS, testDays)
                //如果为true，则调用startActivity()不会进入手机的闹钟设置界面
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        //设置闹钟//TODO
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            Log.d("AlarmPlugin", "setAlarm: " + hour + " " + minute);
            ctx.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private int alarmSet = 0;
    private void init() throws URISyntaxException {

        title = mView.findViewById(R.id.title);
        cover = mView.findViewById(R.id.cover);

        times[0] = mView.findViewById(R.id.time0);
        times[1] = mView.findViewById(R.id.time1);
        times[2] = mView.findViewById(R.id.time2);
        times[3] = mView.findViewById(R.id.time3);
        alarms[0] = mView.findViewById(R.id.alarm0);
        alarms[1] = mView.findViewById(R.id.alarm1);
        alarms[2] = mView.findViewById(R.id.alarm2);
        alarms[3] = mView.findViewById(R.id.alarm3);
        for (int i = 0; i < 4; i++) {
            int finalI = i;
            alarms[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //如果该闹钟未设置，则设置该闹钟
                    if (alarms[finalI].getTag() == null) {
                        alarms[finalI].setImageResource(R.drawable.alarm_on);
                        alarms[finalI].setTag("on");
                        String text = times[finalI].getText().toString();
                        String[] split = text.split(":");
                        int hour = Integer.parseInt(split[0]);
                        int minute = Integer.parseInt(split[1]);
                        setAlarm(hour,minute);
                        alarmSet++;
                    } else {
                        //如果该闹钟已经设置，则取消该闹钟
                        alarms[finalI].setImageResource(R.drawable.alarm_off);
                        alarms[finalI].setTag(null);
                        String text = times[finalI].getText().toString();
                        String[] split = text.split(":");
                        int hour = Integer.parseInt(split[0]);
                        int minute = Integer.parseInt(split[1]);
//                        delAlarm(hour,minute);
                        alarmSet--;
                    }
                }
            });
        }
//        ctx.enqueue(this);
        alarmManager = (AlarmManager) ctx.getSystemService(ctx.ALARM_SERVICE);

        updateView();
    }

    private String[] timesArray = new String[]{"6:00","6:10","6:20","6:30","6:40","6:50","7:00","7:10","7:20","7:30","7:40","7:50","8:00","8:10","8:20","8:30","8:40","8:50","9:00"};
    private void updateView(){
        if (mView == null) return;
        title.setText("设置闹钟");
//        从timesArray中随机选取4个时间，并设置到times中
        for (int i = 0; i < 4; i++) {
            int random = new Random().nextInt(timesArray.length);
            times[i].setText(timesArray[random]);
        }
    }

    // 检测用户是否睡眠前的定时任务
    private Handler mHandler = new Handler();

    private void startTimer() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 检测用户是否睡眠前
                if (isUserAsleep()) {
                    // 检测到用户睡眠前，更新悬浮岛视图内容，并弹出悬浮岛
                    updateView();
                    ctx.enqueue(AlarmPlugin.this);
                } else {
                    // 如果用户还没有睡眠，继续检测
                    ctx.dequeue(AlarmPlugin.this);
                    startTimer();
                }
            }
        }, 1000);  // 每隔1s检测一次
    }

    private boolean isUserAsleep() {
        //上述代码中，我们通过Calendar类获取当前时间的小时数，
        // 并判断小时数是否在22:00~06:00之间。如果在这个时间段内，
        // 则返回true，表示用户处于睡眠前的状态；
        // 否则，返回false，表示用户没有处于睡眠前的状态。
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return hour >= 22 || hour < 6;
    }


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
        updateView();
        DisplayMetrics metrics = ctx.metrics;
        ctx.animateOverlay2(ctx.dpToInt(210), metrics.widthPixels - ctx.dpToInt(15), expanded);
        animateChild(true, ctx.dpToInt(76));
        //参考alarm_layout.xml,希望悬浮窗收缩的时候不显示内容，而展开时显示内容

        mView.findViewById(R.id.content).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.small).setVisibility(View.GONE);


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
        mView.findViewById(R.id.small).setVisibility(View.VISIBLE);
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
