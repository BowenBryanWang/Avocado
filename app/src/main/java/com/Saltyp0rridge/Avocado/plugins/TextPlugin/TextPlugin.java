package com.Saltyp0rridge.Avocado.plugins.TextPlugin;

import static android.content.ContentValues.TAG;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.Saltyp0rridge.Avocado.JWebSocketClient;
import com.Saltyp0rridge.Avocado.R;
import com.Saltyp0rridge.Avocado.plugins.BasePlugin;
import com.Saltyp0rridge.Avocado.services.OverlayService;
import com.Saltyp0rridge.Avocado.utils.SettingStruct;
import com.google.android.material.imageview.ShapeableImageView;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
        //尝试ping一下服务器，地址为http://192.168.68.96:5000
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
    private JWebSocketClient client;
    private void init() throws URISyntaxException {
        title = mView.findViewById(R.id.title);
        icon  = mView.findViewById(R.id.icon);
        Desc = mView.findViewById(R.id.description);
        cover = mView.findViewById(R.id.cover);
        tag = mView.findViewById(R.id.tag);
        inside_text = mView.findViewById(R.id.inside);
        String BASE_URL = "http://192.168.124.22:5000/test";
        //1.新建OKHttpClient客户端
        //1.新建OKHttpClient客户端
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
                    Log.d(TAG, "run: " + response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();



//        try {
//            System.out.println("mSocket.connected()");
//            mSocket = IO.socket("http://192.168.124.22:5000/test");
//            mSocket.connect();
//            System.out.println(mSocket.connected());
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
//        mSocket.emit("message", "hello");
//        System.out.println("发送成功");
//
//        mSocket.on("html", args -> {
//            inside_text.setText("检测到网址");
//            tag.setImageDrawable(ctx.getDrawable(R.drawable.html));
//            ishtml = true;
//            ctx.enqueue(this);
//        });
//        mSocket.on("agenda", args -> {
//            inside_text.setText("检测到提醒事项");
//            tag.setImageDrawable(ctx.getDrawable(R.drawable.agenda));
//            isagenda = true;
//            ctx.enqueue(this);
//        });
        inside_text.setText("检测到网址");
        tag.setImageDrawable(ctx.getDrawable(R.drawable.html));
        ishtml = true;
//        URI uri = URI.create("ws://192.168.124.22:5000/echo");
//        client = new JWebSocketClient(uri) {
//            @Override
//            public void onMessage(String message) {
//                //message就是接收到的消息
//                Log.e("JWebSClientService", message);
//            }
//        };
//        try {
//            client.connectBlocking();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        if (client != null && client.isOpen()) {
//            client.send("hello");
//        }
        ctx.enqueue(this);
    }
    /**
     * 断开连接
     */
    private void closeConnect() {
        try {
            if (null != client) {
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client = null;
        }
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
