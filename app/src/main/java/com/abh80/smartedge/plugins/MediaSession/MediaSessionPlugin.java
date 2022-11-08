package com.abh80.smartedge.plugins.MediaSession;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.abh80.smartedge.utils.CallBack;
import com.abh80.smartedge.R;
import com.abh80.smartedge.plugins.BasePlugin;
import com.abh80.smartedge.services.NotiService;
import com.abh80.smartedge.services.OverlayService;
import com.abh80.smartedge.utils.SettingStruct;
import com.google.android.material.imageview.ShapeableImageView;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.w3c.dom.Text;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MediaSessionPlugin extends BasePlugin {//这个类的作用是获取我们的mediaSession的信息，然后显示在我们的悬浮窗上

    private SeekBar seekBar;//这个是我们的进度条
    private TextView elapsedView;//这个是我们的已经播放的时间
    private TextView remainingView;//这个是我们的剩余时间
    public String current_package_name = "";//这个是我们当前的包名
    public boolean expanded = false;//这个是我们的悬浮窗是否展开的标志
    public Map<String, MediaController.Callback> callbackMap = new HashMap<>();//这个是我们的回调函数的map
    private boolean seekbar_dragging = false;//这个是我们的进度条是否正在拖动的标志
    public Instant last_played;//这个是我们最后一次播放的时间
    OverlayService ctx;//这个是我们的悬浮窗
    Handler mHandler;//这里Handler的作用是用来更新我们的进度条的
    public MediaController mCurrent;//这个是我们的当前的mediaController，作用是用来获取我们的mediaSession的信息

    private final Runnable r = new Runnable() {//这里的Runnable的作用是用来更新我们的进度条的
        @Override
        public void run() {
            if (!expanded) return;//如果我们的悬浮窗没有展开，那么就不更新进度条
            if (mCurrent == null) {//如果我们的mediaController为空，那么就不更新进度条
                closeOverlay();//关闭我们的悬浮窗
                return;
            }
            long elapsed = mCurrent.getPlaybackState().getPosition();//获取我们的已经播放的时间
            if (elapsed < 0) {//如果我们的已经播放的时间小于0，那么就不更新进度条
                closeOverlay();
                return;
            }
            if (mCurrent.getMetadata() == null) {//如果我们的mediaMetadata为空，那么就不更新进度条
                closeOverlay();
                return;
            }
            long total = mCurrent.getMetadata().getLong(MediaMetadata.METADATA_KEY_DURATION);//获取我们的总时间
            elapsedView.setText(DurationFormatUtils.formatDuration(elapsed, "mm:ss", true));//设置我们的已经播放的时间
            remainingView.setText("-" + DurationFormatUtils.formatDuration(Math.abs(total - elapsed), "mm:ss", true));//设置我们的剩余时间
            if (!seekbar_dragging) seekBar.setProgress((int) ((((float) elapsed / total) * 100)));//如果我们的进度条没有拖动，那么就更新我们的进度条
            mHandler.post(r);//更新我们的进度条
        }
    };


    private boolean overlayOpen = false;//这个是我们的悬浮窗是否打开的标志

    public boolean overlayOpen() {
        return overlayOpen;
    }//这个是我们的悬浮窗是否打开的标志

    public void closeOverlay() {
        animateChild(0, new CallBack());//关闭我们的悬浮窗
        overlayOpen = false;
        shouldRemoveOverlay();//移除我们的悬浮窗
    }

    public void closeOverlay(CallBack callBack) {//关闭特定的悬浮窗
        animateChild(0, callBack);
        overlayOpen = false;
    }

    private ImageView pause_play;//这个是我们的暂停播放的按钮

    public MediaController getActiveCurrent(List<MediaController> mediaControllers) {//这个是用来获取我们的当前的mediaController的
        if (mediaControllers.size() == 0) return null;//如果我们的mediaController的数量为0，那么就返回null
        try {
            Optional<MediaController> controller = mediaControllers.stream().filter(x -> x.getPlaybackState().getState() == PlaybackState.STATE_PLAYING).findFirst();//获取我们的当前的mediaController
            return controller.orElse(null);//返回我们的当前的mediaController
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isColorDark(int color) {//这个是用来判断我们的颜色是否是深色的
        // Source : https://stackoverflow.com/a/24261119
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;//获取我们的颜色的深浅
        // It's a dark color
        return !(darkness < 0.5); // It's a light color
    }

    @SuppressLint("UseCompatLoadingForDrawables")//这个是用来忽略我们的警告的
    public void onPlayerResume(boolean b) {//这个是用来更新我们的悬浮窗的，resume意思是恢复
        if (expanded && b) {//如果我们的悬浮窗展开了，并且我们的悬浮窗是打开的
            pause_play.setImageDrawable(ctx.getDrawable(R.drawable.avd_play_to_pause));//设置我们的暂停播放的按钮的图
            pause_play.setImageTintList(ColorStateList.valueOf(ctx.textColor));//设置我们的暂停播放的按钮的颜色
            ((AnimatedVectorDrawable) pause_play.getDrawable()).start();
        }
        if (mCurrent == null) return;
        int index = -1;//这个是我们的当前的mediaController的索引
        List<MediaController> controllerList = mediaSessionManager.getActiveSessions(new ComponentName(ctx.getBaseContext(), NotiService.class));//获取我们的当前的mediaController的列表
        for (int v = 0; v < controllerList.size(); v++) {//遍历我们的mediaController的列表
            if (Objects.equals(controllerList.get(v).getPackageName(), mCurrent.getPackageName())) {//如果我们的当前的mediaController的包名和我们的当前的mediaController的包名相同
                index = v;//那么就把我们的当前的mediaController的索引设置为v
                break;
            }

        }
        if (index == -1) return;
        visualizer.setPlayerId(index);//设置我们的可视化的播放器的id
        if (mCurrent.getMetadata() == null) return;//如果我们的当前的mediaController的元数据为空，那么就返回
        Bitmap bm = mCurrent.getMetadata().getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);//获取我们的当前的mediaController的专辑封面
        if (bm == null) return;//如果我们的专辑封面为空，那么就返回
        int dc = getDominantColor(bm);//获取我们的专辑封面的主要颜色
        if (isColorDark(dc)) {//如果我们的颜色是深色的
            dc = lightenColor(dc);//那么就把我们的颜色变浅
        }
        visualizer.setColor(dc);//设置我们的可视化的颜色
    }

    private int lightenColor(int colorin) {
        Color color = Color.valueOf(colorin);
        double fraction = 0.3;
        float red = (float) (Math.min(255, color.red() * 255f + 255 * fraction) / 225f);
        float green = (float) (Math.min(255, color.green() * 255f + 255 * fraction) / 225f);
        float blue = (float) (Math.min(255, color.blue() * 255f + 255 * fraction) / 225f);
        float alpha = color.alpha();

        return Color.valueOf(red, green, blue, alpha).toArgb();

    }

    private SongVisualizer visualizer;
    private MediaSessionManager mediaSessionManager;

    @SuppressLint("UseCompatLoadingForDrawables")
    public void onPlayerPaused(boolean b) {
        if (expanded && b) {
            pause_play.setImageDrawable(ctx.getDrawable(R.drawable.avd_pause_to_play));
            pause_play.setImageTintList(ColorStateList.valueOf(ctx.textColor));
            ((AnimatedVectorDrawable) pause_play.getDrawable()).start();
        }
        last_played = Instant.now();
        mHandler.postDelayed(() -> {
            if (Math.abs(Instant.now().toEpochMilli() - last_played.toEpochMilli()) >= 60 * 1000) {
                if (getActiveCurrent(mediaSessionManager.getActiveSessions(new ComponentName(ctx, NotiService.class))) == null)
                    closeOverlay();
            }
        }, 60 * 1000);
    }

    public static int getDominantColor(Bitmap bitmap) {
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        final int color = newBitmap.getPixel(0, 0);
        newBitmap.recycle();
        return color;
    }

    @Override
    public String getID() {
        return "MediaSessionPlugin";
    }

    private MediaSessionManager.OnActiveSessionsChangedListener listnerForActiveSessions = list -> {
        list.forEach(x -> {
            if (callbackMap.get(x.getPackageName()) != null) return;
            MediaCallback c = new MediaCallback(x, this);
            callbackMap.put(x.getPackageName(), c);
            x.registerCallback(c);
        });
    };

    @Override
    public void onCreate(OverlayService context) {//这个是我们的插件的创建
        ctx = context;//把我们的上下文设置为我们的context
        mHandler = new Handler(context.getMainLooper());//创建我们的handler
        mView = LayoutInflater.from(context).inflate(R.layout.media_session_layout, null);//创建我们的视图
        mView.findViewById(R.id.blank_space).setVisibility(View.VISIBLE);//设置我们的空白的空间为可见
        init();//初始化我们的插件

        mediaSessionManager = (MediaSessionManager) ctx.getSystemService(Context.MEDIA_SESSION_SERVICE);//获取我们的mediaSessionManager
        mediaSessionManager.addOnActiveSessionsChangedListener(listnerForActiveSessions, new//添加我们的活跃的会话改变的监听器

                ComponentName(ctx, NotiService.class));//设置我们的组件名为我们的NotiService
        mediaSessionManager.getActiveSessions(new

                        ComponentName(ctx, NotiService.class)).

                forEach(x ->

                {
                    if (callbackMap.get(x.getPackageName()) != null) return;
                    MediaCallback c = new MediaCallback(x, this);
                    callbackMap.put(x.getPackageName(), c);
                    x.registerCallback(c);
                });
    }

    public void shouldRemoveOverlay() {
        if (getActiveCurrent(mediaSessionManager.getActiveSessions(new ComponentName(ctx, NotiService.class))) == null) {
            ctx.dequeue(this);
        }
    }

    private void layoutHandle(View v, int width) {
        int width1 = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        v.measure(width1, height);
        v.getLayoutParams().width = v.getMeasuredWidth();
        v.getLayoutParams().height = v.getMeasuredHeight();
        v.setLayoutParams(v.getLayoutParams());
    }

    public void openOverlay(String pkg_name) {
        if (overlayOpen) return;
        overlayOpen = true;
        current_package_name = pkg_name;
        animateChild(ctx.dpToInt(ctx.minHeight / 4), new CallBack());
    }

    private View mView;
    private ShapeableImageView cover;
    ImageView back;
    ImageView next;

    private void init() {
        seekBar = mView.findViewById(R.id.progressBar);
        elapsedView = mView.findViewById(R.id.elapsed);
        remainingView = mView.findViewById(R.id.remaining);
        pause_play = mView.findViewById(R.id.pause_play);
        next = mView.findViewById(R.id.next_play);
        back = mView.findViewById(R.id.back_play);
        cover = mView.findViewById(R.id.cover);
        coverHolder = mView.findViewById(R.id.relativeLayout);
        text_info = mView.findViewById(R.id.text_info);
        controls_holder = mView.findViewById(R.id.controls_holder);

        pause_play.setOnClickListener(l -> {
            if (mCurrent == null) return;
            if (mCurrent.getPlaybackState().getState() == PlaybackState.STATE_PAUSED) {
                mCurrent.getTransportControls().play();

            } else {
                mCurrent.getTransportControls().pause();

            }
        });
        TextView titleView = mView.findViewById(R.id.title);
        TextView artistView = mView.findViewById(R.id.artist_subtitle);
        elapsedView.setTextColor(ctx.textColor);
        remainingView.setTextColor(ctx.textColor);
        titleView.setTextColor(ctx.textColor);
        artistView.setTextColor(ctx.textColor);
        back.setImageTintList(ColorStateList.valueOf(ctx.textColor));
        next.setImageTintList(ColorStateList.valueOf(ctx.textColor));
        pause_play.setImageTintList(ColorStateList.valueOf(ctx.textColor));
        seekBar.getProgressDrawable().setColorFilter(ctx.textColor, PorterDuff.Mode.SRC_ATOP);

        next.setOnClickListener(l -> {
            if (mCurrent == null) return;
            mCurrent.getTransportControls().skipToNext();
        });
        back.setOnClickListener(l -> {
            if (mCurrent == null) return;
            mCurrent.getTransportControls().skipToPrevious();
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekbar_dragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekbar_dragging = false;
                mCurrent.getTransportControls().seekTo((long) ((float) seekBar.getProgress() / 100 * mCurrent.getMetadata().getLong(MediaMetadata.METADATA_KEY_DURATION)));
            }
        });
        visualizer = mView.findViewById(R.id.visualizer);
    }

    @Override
    public View onBind() {
        return mView;
    }

    @Override
    public void onUnbind() {
        mHandler.removeCallbacks(r);
    }

    @Override
    public void onDestroy() {
        if (visualizer != null) visualizer.release();
        if (mediaSessionManager != null)
            mediaSessionManager.removeOnActiveSessionsChangedListener(listnerForActiveSessions);
        mediaSessionManager = null;
        mCurrent = null;
        mView = null;

    }

    @Override
    public void onTextColorChange() {
        TextView titleView = mView.findViewById(R.id.title);
        TextView artistView = mView.findViewById(R.id.artist_subtitle);
        elapsedView.setTextColor(ctx.textColor);
        remainingView.setTextColor(ctx.textColor);
        titleView.setTextColor(ctx.textColor);
        artistView.setTextColor(ctx.textColor);
        back.setImageTintList(ColorStateList.valueOf(ctx.textColor));
        next.setImageTintList(ColorStateList.valueOf(ctx.textColor));
        pause_play.setImageTintList(ColorStateList.valueOf(ctx.textColor));
        seekBar.getProgressDrawable().setColorFilter(ctx.textColor, PorterDuff.Mode.SRC_ATOP);
    }

    RelativeLayout coverHolder;
    private final CallBack onChange = new CallBack() {
        @Override
        public void onChange(float p) {
            float f;
            if (expanded) {
                f = p;
            } else {
                f = 1 - p;
            }
            mView.setPadding(0, (int) (f * ctx.statusBarHeight), 0, 0);
            ((RelativeLayout.LayoutParams) coverHolder.getLayoutParams()).leftMargin = (int) (f * ctx.dpToInt(20));
        }
    };

    @Override
    public void onExpand() {
        if (expanded) return;
        expanded = true;
        DisplayMetrics metrics = ctx.metrics;
        ctx.animateOverlay(ctx.dpToInt(210), metrics.widthPixels - ctx.dpToInt(15), expanded, OverLayCallBackStart, overLayCallBackEnd, onChange, false);
        animateChild(true, ctx.dpToInt(76));

    }

    LinearLayout text_info;
    LinearLayout controls_holder;
    CallBack OverLayCallBackStart = new CallBack() {
        @Override
        public void onFinish() {
            super.onFinish();
            if (expanded) {
                mView.findViewById(R.id.blank_space).setVisibility(View.GONE);
                ViewGroup.LayoutParams layoutParams = mView.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                text_info.setVisibility(View.VISIBLE);
                controls_holder.setVisibility(View.VISIBLE);
                seekBar.setVisibility(View.VISIBLE);
                mView.findViewById(R.id.title).setSelected(true);
                mView.findViewById(R.id.artist_subtitle).setSelected(true);
                elapsedView.setVisibility(View.VISIBLE);
                remainingView.setVisibility(View.VISIBLE);
                ((RelativeLayout.LayoutParams) coverHolder.getLayoutParams()).removeRule(RelativeLayout.CENTER_VERTICAL);
                coverHolder.setLayoutParams(coverHolder.getLayoutParams());
            } else {
                mHandler.removeCallbacks(r);
                text_info.setVisibility(View.GONE);
                controls_holder.setVisibility(View.GONE);
                seekBar.setVisibility(View.GONE);
                mView.findViewById(R.id.blank_space).setVisibility(View.VISIBLE);
                elapsedView.setVisibility(View.GONE);
                remainingView.setVisibility(View.GONE);

                ((RelativeLayout.LayoutParams) coverHolder.getLayoutParams()).addRule(RelativeLayout.CENTER_VERTICAL);
                coverHolder.setLayoutParams(coverHolder.getLayoutParams());

            }
        }
    };
    CallBack overLayCallBackEnd = new CallBack() {
        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onFinish() {
            super.onFinish();
            if (expanded) {
                mView.setPadding(0, ctx.statusBarHeight, 0, 0);
                ((RelativeLayout.LayoutParams) coverHolder.getLayoutParams()).leftMargin = ctx.dpToInt(20);
                if (mCurrent != null && mCurrent.getPlaybackState() != null) {
                    if (mCurrent.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) {
                        pause_play.setImageDrawable(ctx.getDrawable(R.drawable.pause));
                        pause_play.setImageTintList(ColorStateList.valueOf(ctx.textColor));
                    } else {
                        pause_play.setImageDrawable(ctx.getDrawable(R.drawable.play));
                        pause_play.setImageTintList(ColorStateList.valueOf(ctx.textColor));
                    }
                }
                mHandler.post(r);
                text_info.setAlpha(1);
                controls_holder.setAlpha(1);
                seekBar.setAlpha(1);
                elapsedView.setAlpha(1);
                remainingView.setAlpha(1);
            } else {
                ViewGroup.LayoutParams layoutParams = mView.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                mView.setLayoutParams(layoutParams);
                mView.setPadding(0, 0, 0, 0);
                ((RelativeLayout.LayoutParams) coverHolder.getLayoutParams()).leftMargin = 0;
            }
        }
    };

    @Override
    public void onCollapse() {
        if (!expanded) return;
        expanded = false;
        ctx.animateOverlay(ctx.minHeight, ViewGroup.LayoutParams.WRAP_CONTENT, expanded, OverLayCallBackStart, overLayCallBackEnd, onChange, false);
        animateChild(false, ctx.dpToInt(ctx.minHeight / 4));
    }

    @Override
    public void onClick() {
        if (expanded && !ctx.sharedPreferences.getBoolean("ms_enable_touch_expanded", false))
            return;
        if (mCurrent != null && mCurrent.getSessionActivity() != null) {
            try {
                mCurrent.getSessionActivity().send(0);
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String[] permissionsRequired() {
        return null;
    }

    @Override
    public String getName() {
        return "Media Session";
    }

    @Override
    public ArrayList<SettingStruct> getSettings() {
        ArrayList<SettingStruct> s = new ArrayList<>();
        s.add(new SettingStruct("Open music app on touch when expanded", "Media Session", SettingStruct.TYPE_TOGGLE) {
            @Override
            public boolean onAttach(Context ctx) {
                return ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE).getBoolean("ms_enable_touch_expanded", false);
            }

            @Override
            public void onCheckChanged(boolean checked, Context ctx) {
                ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE).edit().putBoolean("ms_enable_touch_expanded", checked).apply();
                if (MediaSessionPlugin.this.ctx != null) {
                    MediaSessionPlugin.this.ctx.sharedPreferences.putBoolean("ms_enable_touch_expanded", checked);
                }
            }
        });
        return s;
    }


    public void queueUpdate(UpdateQueueStruct queueStruct) {
        ctx.enqueue(this);
        TextView titleView = mView.findViewById(R.id.title);
        TextView artistView = mView.findViewById(R.id.artist_subtitle);
        ShapeableImageView imageView = cover;
        titleView.setText(queueStruct.getTitle());
        artistView.setText(queueStruct.getArtist());
        imageView.setImageBitmap(queueStruct.getCover());

    }


    private void animateChild(boolean expanding, int h) {
        View view1 = cover;
        View view2 = visualizer;

        ValueAnimator height_anim = ValueAnimator.ofInt(view1.getHeight(), h);
        height_anim.setDuration(500);
        height_anim.addUpdateListener(valueAnimator -> {
            ViewGroup.LayoutParams params1 = view1.getLayoutParams();
            ViewGroup.LayoutParams params2 = view2.getLayoutParams();
            params1.height = (int) valueAnimator.getAnimatedValue();
            params2.height = (int) valueAnimator.getAnimatedValue();
            params1.width = (int) valueAnimator.getAnimatedValue();
            params2.width = (int) valueAnimator.getAnimatedValue();
            view1.setLayoutParams(params1);
            view2.setLayoutParams(params2);
        });
        height_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!expanding) {
                    view2.setVisibility(View.VISIBLE);
                    visualizer.paused = false;
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (expanding) {
                    view2.setVisibility(View.GONE);
                    visualizer.paused = true;
                }
            }
        });
        height_anim.setInterpolator(new OvershootInterpolator(0.5f));
        height_anim.start();


    }

    private void animateChild(int h, CallBack callback) {
        View view1 = cover;
        View view2 = visualizer;
        if (h != 0) {
            ViewGroup.LayoutParams params1 = view1.getLayoutParams();
            ViewGroup.LayoutParams params2 = view2.getLayoutParams();
            params1.height = h;
            params2.height = h;
            params1.width = h;
            params2.width = h;
            view1.setScaleY(0);
            view1.setScaleX(0);
            view2.setScaleX(0);
            view2.setScaleY(0);
            view1.setLayoutParams(params1);
            view2.setLayoutParams(params2);
        }
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(h != 0 ? 0 : 1, h != 0 ? 1 : 0);
        valueAnimator.addUpdateListener(l -> {
            float f = (float) l.getAnimatedValue();
            view1.setScaleX(f);
            view1.setScaleY(f);
            view2.setScaleX(f);
            view2.setScaleY(f);
        });
        valueAnimator.setDuration(300);
        valueAnimator.start();
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                callback.onFinish();
            }
        });


    }

}
