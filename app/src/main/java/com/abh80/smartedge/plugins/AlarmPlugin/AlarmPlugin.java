package com.abh80.smartedge.plugins.AlarmPlugin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.services.NotiService;
import com.abh80.smartedge.services.OverlayService;
import com.abh80.smartedge.utils.SettingStruct;

//该文件是实现对于系统闹钟的读取和设置

import java.util.ArrayList;
import java.util.Calendar;
//倒入PendingIntent类
import android.app.PendingIntent;
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
    public void onCreate(OverlayService context) {
        mView = LayoutInflater.from(ctx).inflate(R.layout.alarm_layout, null);
        ctx = context;
        init();
    }

    private View mView;

    @Override
    public View onBind() {
        return mView;
    }
    private TextView title;
    //    #alarms应该是四个闹钟的列表
    private TextView[] alarms = new TextView[4];

    private AlarmManager alarmManager;

    public void setAlarm(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Intent intent = new Intent(ctx, NotiService.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }
    private void init(){
        title = mView.findViewById(R.id.title);
//        alarms = [mView.findViewById(R.id.alarm1), mView.findViewById(R.id.alarm2), mView.findViewById(R.id.alarm3), mView.findViewById(R.id.alarm4)];
        alarms[0] = mView.findViewById(R.id.alarm1);
        alarms[1] = mView.findViewById(R.id.alarm2);
        alarms[2] = mView.findViewById(R.id.alarm3);
        alarms[3] = mView.findViewById(R.id.alarm4);

        alarmManager = (AlarmManager) ctx.getSystemService(ctx.ALARM_SERVICE);

        updateView();
    }
    private void updateView(){
        if (mView == null) return;
        title.setText("设置闹钟");
        //TODO
        for (int i = 0; i < alarms.length; i++) {
            alarms[i].setText("闹钟" + i);
        }
    }
    @Override
    public void onUnbind() {
        mView = null;
    }
    @Override
    public void onDestroy() {

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
