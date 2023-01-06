package com.Saltyp0rridge.Avocado.plugins.MoneyPlugin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.Saltyp0rridge.Avocado.R;
import com.Saltyp0rridge.Avocado.plugins.BasePlugin;
import com.Saltyp0rridge.Avocado.utils.ResponseData;
import com.Saltyp0rridge.Avocado.services.OverlayService;
import com.Saltyp0rridge.Avocado.utils.SettingStruct;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.Gson;

//该文件是实现对于系统闹钟的读取和设置

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
//倒入PendingIntent类
import android.os.Handler;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


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
        mView = LayoutInflater.from(context).inflate(R.layout.money_layout, null);//创建我们的视图
        mView.findViewById(R.id.blank_space).setVisibility(View.VISIBLE);//设置我们的空白的空间为可见
        init();
    }

    private View mView;
    private boolean ismoney = false;
    private String money_number = "";
    private String come_ior = "";
    @Override
    public View onBind() {
        mView = LayoutInflater.from(ctx).inflate(R.layout.money_layout, null);
        init();
        return mView;
    }
    public boolean connect2server(){
        String BASE_URL = "http://192.168.124.22:5000/detect_money";
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1000, TimeUnit.SECONDS)
                .writeTimeout(1000, TimeUnit.SECONDS)
                .readTimeout(1000, TimeUnit.SECONDS)
                .build();

        //2. 新建一个Request对象
        final Request request = new Request.Builder()
                .url(BASE_URL)
                .build();

        final Call call = okHttpClient.newCall(request);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //3.Response为 OKHttp 中的响应
                    Response response = call.execute();
                    //返回值是python里的dict，用java获取
                    String responseBodyString = response.body().string();
                    Log.d("responseBodyString", responseBodyString);
                    Gson gson = new Gson();
                    ResponseData responseData = gson.fromJson(responseBodyString, ResponseData.class);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (responseData.getType().equals("money")) {
                                ismoney = true;
                                money_number = responseData.getMoney();
                                come_ior=responseData.getCome();
                            }
                            if (ismoney) {
                                ctx.enqueue(MoneyPlugin.this);
                                //获取ctx enqueue后的视图
                                if (come_ior.equals("income")) {
                                    inside.setText("检测到收入");
                                    come.setImageDrawable(ctx.getDrawable(R.drawable.income));
                                    Desc.setText("收入金额为" + money_number + "元");
                                }
                                else {
                                    inside.setText("检测到支出");
                                    come.setImageDrawable(ctx.getDrawable(R.drawable.outcome));
                                    Desc.setText("支出金额为" + money_number + "元");
                                }
                                mView.findViewById(R.id.inside_text).setVisibility(View.VISIBLE);
                            }
                            return;
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }).start();
        return true;
    }
    private TextView title;
    private TextView Desc;
    private ImageView come;
    private TextView inside;
    private AlarmManager alarmManager;

    private void init(){
        title = mView.findViewById(R.id.title);
        cover = mView.findViewById(R.id.cover);
        Desc = mView.findViewById(R.id.desc);
        come = mView.findViewById(R.id.tag);
        inside = mView.findViewById(R.id.inside);
        inside.setText("检测到收入+1000元");
        come.setImageDrawable(ctx.getDrawable(R.drawable.income));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                connect2server();
                new Handler().postDelayed(this, 3000);
            }
        }, 3000);
        updateView();
    }

    private void updateView(){
        if (mView == null) return;
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
        ctx.dequeue(this);
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
