package com.abh80.smartedge.plugins.AlarmPlugin;

import static android.content.ContentValues.TAG;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.services.NotiService;
import com.abh80.smartedge.services.OverlayService;
import com.abh80.smartedge.utils.SettingStruct;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;



//在该插件中，我们要完成的功能是：
//1，检测到用户睡眠前，弹出悬浮岛，并且显示用户可能明天需要设置的闹钟
//2，用户选择闹钟后，设置闹钟
//3，用户选择不设置闹钟后，关闭悬浮岛
//4，呈现闹钟时，应该根据用户的习惯，或者用户的日程安排，推荐理想合适的闹钟给用户


public class AlarmPlugin extends BasePlugin{
    @Override
    public String getID() {
        return "AlarmPlugin";
    }

    @Override
    public String getName() {
        return "Alarm Plugin";
    }

    private OverlayService ctx;

    private Socket socket;
    @Override
    public void onCreate(OverlayService context) throws URISyntaxException {
        mView = LayoutInflater.from(ctx).inflate(R.layout.alarm_layout, null);
        ctx = context;
        init();
        NetInit();

    }
    public void NetInit() throws URISyntaxException {
        socket = IO.socket("http://localhost:5000");
        socket.on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String message = (String) args[0];
                Log.d(TAG, "received message: " + message);
            }
        });
        socket.connect();
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
