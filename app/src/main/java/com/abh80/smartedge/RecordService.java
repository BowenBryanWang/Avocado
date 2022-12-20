package com.abh80.smartedge;

import static com.abh80.smartedge.ScreenCaptureImageActivity.STORE_DIRECTORY;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.abh80.smartedge.Views.RecordButton;

public class RecordService extends AccessibilityService{
    public static ActionRecordControlller mActionRecordController;
    private RecordButton recordButton;
    private SoftKeyboardController softKeyboardController;
    private long isOverLayShowed = -1;
    private boolean lastIshome= false;
    public Map<String,Integer> packageCountMap;
    private AudioManager audioManager;

    private HomeWatcherReceiver mHomeKeyReceiver;
    public static MergedBlock mergedBlock;
    public static ExecutorService exec;

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getKeyCode()==KeyEvent.KEYCODE_VOLUME_UP)
        {
        }
        return super.onKeyEvent(event);
    }

    public interface SoftKeyboardStateChangeListener {
        boolean onSoftKeyboardStateChanged(boolean appear);
    }

    public boolean IsOverlayShowed()
    {
        return isOverLayShowed==0;
    }

    private static final Integer LISTENER_OP_MUTEX = "LISTENER_OP_MUTEX".hashCode();
    boolean softKeyboardOpened = false;
    public boolean getSoftKeyboardOpened(){
        return softKeyboardOpened;
    }
    List<SoftKeyboardStateChangeListener> softKeyboardsStateChangeListeners;
    public void registerSoftKeyboardStateChangeListener(SoftKeyboardStateChangeListener l){
        synchronized (LISTENER_OP_MUTEX){
            softKeyboardsStateChangeListeners.add(l);
        }
    }
    public boolean unregisterSoftKeyboardStateChangeListener(SoftKeyboardStateChangeListener l){
        synchronized (LISTENER_OP_MUTEX) {
            return softKeyboardsStateChangeListeners.remove(l);
        }
    }
    public void clearSoftKeyboardStateChangeListener(){
        synchronized (LISTENER_OP_MUTEX) {
            softKeyboardsStateChangeListeners.clear();
        }
    }
    public static long softKeyboardStateChangeMinInterval = 1000;
    private long lastKeyboardStateChangeTime = -1;
    public void enableImmediateKeyboardStateChange(){
        lastKeyboardStateChangeTime = -1;
    }


    @Override
    public AccessibilityNodeInfo getRootInActiveWindow() {
        List<AccessibilityNodeInfo> result = getRootsInActiveWindow();
        if(result==null||result.size()<1)
            return null;
        return getRootsInActiveWindow().get(0);
    }

    public List<AccessibilityNodeInfo> getRootsInActiveWindow()
    {
        List<AccessibilityWindowInfo> windows = super.getWindows();
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        Collections.sort(windows, new Comparator<AccessibilityWindowInfo>() {
            @Override
            public int compare(AccessibilityWindowInfo accessibilityWindowInfo, AccessibilityWindowInfo t1) {
                Rect bound1=new Rect();
                Rect bound2=new Rect();
                accessibilityWindowInfo.getBoundsInScreen(bound1);
                t1.getBoundsInScreen(bound2);
                int are1 = (bound1.bottom-bound1.top) * (bound1.right-bound1.left);
                int are2 = (bound2.bottom-bound2.top) * (bound2.right-bound2.left);
                if(are1>are2){
                    return -1;
                }
                if(are1<are2){
                    return 1;
                }
                if(accessibilityWindowInfo.getParent() != null && t1.getParent() == null){
                    return -1;
                }
                if(accessibilityWindowInfo.getParent() == null && t1.getParent() != null){
                    return 1;
                }
                if(accessibilityWindowInfo.isActive()){
                    return -1;
                }
                if(t1.isActive()){
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        if(windows.isEmpty())
            return null;
        for(AccessibilityWindowInfo window : windows)
        {
            if(window.getRoot()!=null)
                nodes.add(window.getRoot());
        }
        return nodes;
    }


    @Override
    public AccessibilityNodeInfo findFocus(int focus) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.findFocus(focus);
        } else {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            return root == null ? null : root.findFocus(focus);
        }
    }
    private List<String> launcherAppPackageNames;
    @Override
    public void onCreate() {
        super.onCreate();
        self = this;
        softKeyboardsStateChangeListeners = new ArrayList<>();
        launcherAppPackageNames  =Utility.getLauncherPackageNames(self);
        //CrashReport.initCrashReport(getApplicationContext(), "30362557b7", true);


    }

    public static RecordService self;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        self = this;
        Utility.init(self);
        Toast.makeText(this, String.format("version: %s", "1.0.0"), Toast.LENGTH_LONG).show();

        initializeCountMap();


        Intent mainIntent = new Intent(this.getApplicationContext(),ScreenCaptureImageActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(2000,50,0,0,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        recordButton = new RecordButton((Context) self,(RecordService)self,mActionRecordController,
                (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE),params);
        //((WindowManager) self.getSystemService(WINDOW_SERVICE)).addView(recordButton, params);
        isOverLayShowed = 0;

        mActionRecordController = new ActionRecordControlller(self);
        mActionRecordController.start();

        mHomeKeyReceiver = new HomeWatcherReceiver();
        final IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mHomeKeyReceiver, homeFilter);

        registerSoftKeyboardStateChangeListener(new SoftKeyboardStateChangeListener() {
            @Override
            public boolean onSoftKeyboardStateChanged(boolean appear) {
                if(!appear&& !IsOverlayShowed())
                    createButton();
                return true;
            }
        });

//        mergedBlock = new MergedBlock("wechat8.0.2");
//        try {
//            mergedBlock.loadPages();
//            mergedBlock.loadSemanticBlocks();
//            mergedBlock.loadFindNodesMethod();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

        exec = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS,
                new LinkedBlockingDeque<Runnable>(1), new ThreadPoolExecutor.DiscardOldestPolicy());

        Timer checkTokenTimer = new Timer();
        //PeriodTask periodTask = new PeriodTask();
        checkTokenTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mActionRecordController!=null) {
                    mActionRecordController.checkShotChange(mergedBlock, exec);
                }
            }
        }, 10, 200);
    }

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent accessibilityEvent) {
        double timeS = System.currentTimeMillis();
//        try {
//            if (accessibilityEvent.getPackageName() != null && (accessibilityEvent.getPackageName().toString().contains("tback") || accessibilityEvent.getPackageName().toString().contains("talkback")))
//                return;
//            if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
//                if (accessibilityEvent.getSource() != null) {
//                    if (accessibilityEvent.getSource().isEditable() && IsOverlayShowed()) {
//                        removeButton();
//                        accessibilityEvent.getSource().performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                        return;
//                    }
//                }
//            } else if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
//                    || accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
//                    || accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
//                if (AccessibilityNodeInfoRecord.root != null && AccessibilityNodeInfoRecord.root.nodeInfo != null && AccessibilityNodeInfoRecord.root.getPackageName() != null) {
//                    lastIshome = launcherAppPackageNames.contains(String.valueOf(AccessibilityNodeInfoRecord.root.getPackageName()));
////                if(AccessibilityNodeInfoRecord.root.getPackageName().toString().contains("tback"))
////                    lastIshome = true;
//                }
//                List<AccessibilityWindowInfo> windowInfos = getWindows();
//                Log.i("WindowSize", String.valueOf(windowInfos.size()));
//                boolean hasKeyboardFound = false;
//                for (AccessibilityWindowInfo windowInfo : windowInfos) {
//                    if (windowInfo.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
//                        hasKeyboardFound = true;
//                    }
//                }
//                recycleAllWindowInfo(windowInfos);
//                if (hasKeyboardFound != softKeyboardOpened) {
//                    long crtTime = System.currentTimeMillis();
//                    if (crtTime - lastKeyboardStateChangeTime > softKeyboardStateChangeMinInterval) {
//                        lastKeyboardStateChangeTime = crtTime;
//                        softKeyboardOpened = hasKeyboardFound;
//                        synchronized (LISTENER_OP_MUTEX) {
//                            for (SoftKeyboardStateChangeListener l : softKeyboardsStateChangeListeners) {
//                                l.onSoftKeyboardStateChanged(softKeyboardOpened);
//                            }
//                        }
//                    }
//                } else if (!softKeyboardOpened && !IsOverlayShowed() && System.currentTimeMillis() - isOverLayShowed > 1500)
//                    createButton();
//            }
//            System.out.println("on Accessibility event softkeyboardtest" + accessibilityEvent.getEventType() + " : " + (System.currentTimeMillis() - timeS));
//        }catch (Exception e)
//        {
//         e.printStackTrace();
//        }
        ActionEventRecord record = new ActionEventRecord(accessibilityEvent);
        new Thread("motion event") {
            @Override
            public void run() {
                synchronized (mActionRecordController.actionEventRecords) {
                    mActionRecordController.actionEventRecords.offer(record);
                    mActionRecordController.actionEventRecords.notify();
                }
            }
        }.start();

        System.out.println("on Accessibility event" + accessibilityEvent.getEventType() + " : " + (System.currentTimeMillis() - timeS));

    }

    private void recycleAllWindowInfo(Collection<AccessibilityWindowInfo> windowInfos){
        for(AccessibilityWindowInfo windowInfo: windowInfos){
            windowInfo.recycle();
        }
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public boolean onUnbind(Intent intent) {

        unregisterReceiver(mHomeKeyReceiver);
        Utility.shutdownTts();
        mActionRecordController.finisheSelf();
        boolean res =  super.onUnbind(intent);
        return res;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void createButton()
    {
        if(IsOverlayShowed())
            return;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(2000,80,0,0,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        recordButton = new RecordButton((Context) self,(RecordService)self,mActionRecordController,
                (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE),params);
        ((WindowManager) self.getSystemService(WINDOW_SERVICE)).addView(recordButton, params);
        isOverLayShowed = 0;
    }
    public void removeButton()
    {
        if(!IsOverlayShowed())
            return;
        isOverLayShowed = System.currentTimeMillis();
        ((WindowManager) self.getSystemService(WINDOW_SERVICE)).removeViewImmediate(recordButton);
    }

    class HomeWatcherReceiver extends BroadcastReceiver {
        public static final String SYSTEM_DIALOG_REASON_KEY = "reason";
        public static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        public static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                    //Toast.makeText(context, "Home按键", Toast.LENGTH_SHORT).show();
                    ActionEventRecord record = new ActionEventRecord(new Action(AccessibilityEvent.TYPE_VIEW_CLICKED, "home", "home", "system",System.currentTimeMillis()));
                    new Thread("motion event") {
                        @Override
                        public void run() {
                            synchronized (mActionRecordController.actionEventRecords) {
                                mActionRecordController.actionEventRecords.offer(record);
                                mActionRecordController.actionEventRecords.notify();
                            }
                        }
                    }.start();
                }
                if (SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason)) {
                    //Toast.makeText(context, "recentapps按键", Toast.LENGTH_SHORT).show();
                    ActionEventRecord record = new ActionEventRecord(new Action(AccessibilityEvent.TYPE_VIEW_CLICKED, "recentapps", "recentapps", "system",System.currentTimeMillis()));

                    new Thread("motion event") {
                        @Override
                        public void run() {
                            synchronized (mActionRecordController.actionEventRecords) {
                                mActionRecordController.actionEventRecords.offer(record);
                                mActionRecordController.actionEventRecords.notify();
                            }
                        }
                    }.start();
                }
            }
        }
    }

    public void onBack()
    {
        if(IsOverlayShowed())
            removeButton();
        //Toast.makeText(self, "返回按键", Toast.LENGTH_SHORT).show();
        ActionEventRecord record = new ActionEventRecord(new Action(AccessibilityEvent.TYPE_VIEW_CLICKED, "back", "back", "system",System.currentTimeMillis()));
        new Thread("motion event") {
            @Override
            public void run() {
                synchronized (mActionRecordController.actionEventRecords) {
                    mActionRecordController.actionEventRecords.offer(record);
                    mActionRecordController.actionEventRecords.notify();
                }
            }
        }.start();
        performGlobalAction(GLOBAL_ACTION_BACK);
        if(!IsOverlayShowed())
            createButton();
    }

    private void initializeCountMap()
    {
        if(packageCountMap==null)
            packageCountMap=new HashMap<>();
        else
            packageCountMap.clear();
        FileOutputStream fos = null;
        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir != null) {
            STORE_DIRECTORY = externalFilesDir.getAbsolutePath();
            File storeDirectory = new File(STORE_DIRECTORY);
            if (!storeDirectory.exists()) {
                boolean success = storeDirectory.mkdirs();
                if (!success) {
                    Log.e("error", "failed to create file storage directory.");
                    return;
                }
            }
        } else {
            Log.e("error", "failed to create file storage directory, getExternalFilesDir is null.");
            return;
        }
        String curDir = STORE_DIRECTORY+"/screenshots/"+Utility.USER+"/";
        File storeDirectory = new File(curDir);
        if (!storeDirectory.exists()) {
            boolean success = storeDirectory.mkdirs();
            if (!success) {
                Log.e("error", "failed to create file storage directory.");
                return;
            }
        }
        for(File file:storeDirectory.listFiles())
        {
            Pattern p = Pattern.compile(".*_actions_(.*)\\.json");
            Matcher matcher = p.matcher(file.getName());
            if(matcher.find()) {
                String key = matcher.group(1);
                packageCountMap.put(key,packageCountMap.getOrDefault(key,0)+1);
            }
        }
    }

    public void increaseCount(CharSequence packageName)
    {
        if(packageName!=null) {
            packageCountMap.put(packageName.toString(), packageCountMap.getOrDefault(packageName.toString(), 0) + 1);
            //recordButton.refreshText(packageName.toString() + " " + packageCountMap.get(packageName));
        }
    }

}
