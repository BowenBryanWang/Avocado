package com.abh80.smartedge.plugins.TextPlugin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.services.OverlayService;
import com.abh80.smartedge.utils.SettingStruct;
import com.google.android.material.imageview.ShapeableImageView;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;

public class TextPlugin extends BasePlugin {
    @Override
    public String getID() {
        return "TextPlugin";
    }

    @Override
    public String getName() {
        return "Text Plugin";
    }

    private OverlayService ctx;

    @Override
    public void onCreate(OverlayService context) throws URISyntaxException {
        ctx = context;
        mView = LayoutInflater.from(context).inflate(R.layout.text_layout, null);//创建我们的视图
        mView.findViewById(R.id.blank_space).setVisibility(View.VISIBLE);//设置我们的空白的空间为可见
        init();//初始化我们的插件
    }

    private View mView;

    @Override
    public View onBind() {
        mView = LayoutInflater.from(ctx).inflate(R.layout.text_layout, null);

        return mView;
    }
    private TextView title;
    private TextView Desc;
    private ImageView tag;
    private ImageView icon;
    private TextView inside_text;
    private Socket mSocket;
    private boolean ishtml = false;
    private boolean isagenda = false;

    private void init() throws URISyntaxException {
        try {
            mSocket = IO.socket("https://baidu.com");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mSocket.connect();
        System.out.println(mSocket.connected());
        //打印以确认连接成功
        System.out.println("连接成功");
        //向后端发送一条确认信息
        mSocket.emit("message", "hello");
        System.out.println("发送成功");
        title = mView.findViewById(R.id.title);
        icon  = mView.findViewById(R.id.icon);
        Desc = mView.findViewById(R.id.description);
        cover = mView.findViewById(R.id.cover);
        tag = mView.findViewById(R.id.tag);
        inside_text = mView.findViewById(R.id.inside);


        mSocket.on("html", args -> {
            inside_text.setText("检测到网址");
            tag.setImageDrawable(ctx.getDrawable(R.drawable.html));
            ishtml = true;
            ctx.enqueue(this);
        });
        mSocket.on("agenda", args -> {
            inside_text.setText("检测到提醒事项");
            tag.setImageDrawable(ctx.getDrawable(R.drawable.agenda));
            isagenda = true;
            ctx.enqueue(this);
        });
        inside_text.setText("检测到网址");
        tag.setImageDrawable(ctx.getDrawable(R.drawable.html));
        ishtml = true;
        ctx.enqueue(this);
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
//        if (ishtml){
//            title.setText("检测到网址");
//            Desc.setText("网页描述");
//            icon.setImageDrawable(ctx.getDrawable(R.drawable.html));
//        }
//        else if (isagenda){
//            title.setText("检测到提醒事项");
//            Desc.setText("提醒事项描述");
//            icon.setImageDrawable(ctx.getDrawable(R.drawable.agenda));
//        }
        mView.findViewById(R.id.content).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.inside_text).setVisibility(View.GONE);
        mSocket.emit("message", "hello");
        System.out.println("发送成功");

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
