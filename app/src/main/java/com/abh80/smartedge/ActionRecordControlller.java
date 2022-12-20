package com.abh80.smartedge;

import static com.google.android.gms.internal.zzir.runOnUiThread;
import static com.abh80.smartedge.ScreenCaptureImageActivity.STORE_DIRECTORY;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ActionRecordControlller extends Thread
{
    public final Queue<ActionEventRecord> actionEventRecords;
    boolean hasFinished;
    AccessibilityNodeInfoRecord lastHoverNode;
    long startTime, endTime;
    private Action speculationAction;
    private int cnt = 0;

    ActionRecordControlller(RecordService service){
        actionEventRecords = new LinkedList<>();
        startTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis();
        lastHoverNode = null;
        recordService = service;

    }
    public void finisheSelf(){
        hasFinished = true;
        this.interrupt();
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(-20);
        ActionEventRecord crtAction;
        while (!hasFinished){
            synchronized (actionEventRecords){
                if(actionEventRecords.isEmpty()){
                    try {
                        actionEventRecords.wait();
                    } catch (InterruptedException e) {
                        if(!hasFinished)
                            e.printStackTrace();
                    }
                }

                if(!actionEventRecords.isEmpty()) {
                    crtAction = actionEventRecords.poll();
                }
                else
                    crtAction = null;
            }
            synchronized (Utility.THREAD_MUTEX){
                if(crtAction != null){
                    if(crtAction.actionType==ActionEventRecord.ACTION_TYPE_ACTION)
                        record(crtAction.action);
                    /*else
                        record(crtAction);*/
                }
            }
        }
        Log.d("ThreadFinished", "EventThreadFinished");
    }

    private int state = 0;
    private boolean interactionStarted = false;
    private long currentStamp = 0;
    private long lastStamp = 0;
    private int currentIndex = 0;
    private String srcLayout;
    private String crtLayout;
    private JSONArray srcLayoutJson;
    private JSONArray crtLayoutJson;

    private CharSequence crtPackageName;

    private Bitmap srcBitmap;
    private Bitmap crtBitmap;
    private Bitmap lastBitmap;
    private Bitmap crtShot;
    private Bitmap lastShot;
    private long lastTimeStamp = 0;

    private List<Action> lastActions = new ArrayList<>();
    private List<Action> curAction = new ArrayList<>();
    private List<ActionRecord> actionRecords = new ArrayList<>();
    private long RECORD_PRODUCEDS = 0;
    private RecordService recordService;
    private String lastActivityName;

    public static boolean islastSent = false;
    public static boolean canSend = true;

    private Thread windowStableWaitingThread;
    private Thread sendMessageThread;

    final ReentrantLock lock = new ReentrantLock();

    public void record(Action action)
    {
        if(action==null)
            return ;
        try
        {
            lastActions.add(action);
            this.state = 1;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void checkShotChange(MergedBlock mergedBlock, ExecutorService exec) { //根据像素判断页面是否发生改变
        long startTime =  System.currentTimeMillis();
        if (lastShot != null) {
            lastShot.recycle();
            lastShot = null;
        }
        if (crtShot!=null) {
            lastShot = crtShot.copy(Bitmap.Config.ARGB_8888, true);
            crtShot.recycle();
            crtShot = null;
        }
        Bitmap tempShot = ScreenCaptureImageActivity.getImage();
        if (tempShot!=null) {
            crtShot = tempShot.copy(Bitmap.Config.ARGB_8888, true);
        }
        if (lastShot == null || crtShot == null) return;

        int width = crtShot.getWidth();
        int height = crtShot.getHeight();
        if (lastShot.getWidth()!=width || lastShot.getHeight()!=height) return;

        int lastPixels[] = new int[width*height+5];
        int crtPixels[] = new int[width*height+5];
        lastShot.getPixels(lastPixels, 0, width, 0, 0, width, height);
        System.out.println(crtShot);
        crtShot.getPixels(crtPixels, 0, width, 0, 0, width, height);
        int stepLen = 5;
        int totalSize = width*height;
        int sameSize = 0;
        for (int i = 0; i < width*height; i+=stepLen)
            if (lastPixels[i] - crtPixels[i] == 0) sameSize++;
        double ratio = (double)(sameSize)*1.0*stepLen/(double)(totalSize)*1.0;
        //TODO: 后续添加不参与比较部分
        long endTime = System.currentTimeMillis();
        System.out.println("ratio: "+ratio);
        System.out.println("checkImageTime: "+(endTime-startTime));
        if (ratio>0.9) {
            if ((!islastSent) && canSend) {
                islastSent = true;
                //TODO：改record
                lastTimeStamp = System.currentTimeMillis();
                try {
                    exec.submit(new Runnable() {
                        @Override
                        public void run() {
                            //saveToFile(saveRecord,stamp,index,crtPackageName);
                            if (crtShot!=null) {
                                sendMessageToServer(crtShot.copy(Bitmap.Config.ARGB_8888, true), lastTimeStamp, mergedBlock);
                            }
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }catch (Exception e) {
                }
                /*
                if (sendMessageThread!=null) {
                    sendMessageThread.interrupt();
                }
                sendMessageThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        //saveToFile(saveRecord,stamp,index,crtPackageName);
                        if (crtShot!=null) {
                            sendMessageToServer(crtShot.copy(Bitmap.Config.ARGB_8888, true), lastTimeStamp, mergedBlock);
                         }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //receiveMessageFromServer();
                    }
                });
                sendMessageThread.start();
                 */
                lastStamp = System.currentTimeMillis();
            }
        }
        else islastSent = false;
    }


    synchronized private void recordNew()
    {
        return;
        /*try
        {
            if(srcLayout!=null && srcBitmap!=null&&crtBitmap!=null&&crtLayout!=null) {
                ActionRecord record = new ActionRecord(srcLayout, crtLayout,
                                                        srcBitmap, crtBitmap, curAction);
                lastActions = lastActions.subList(curAction.size(),lastActions.size());
                curAction.clear();
                actionRecords.add(record);
                final ActionRecord saveRecord = record;
                final long stamp = currentStamp;
                final int index = currentIndex;
                currentIndex++;
                lock.lock();
                lastTimeStamp = System.currentTimeMillis();
                lock.unlock();
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        //saveToFile(saveRecord,stamp,index,crtPackageName);
                        sendMessageToServer(saveRecord.getDstImage(), lastTimeStamp);
                        //receiveMessageFromServer();
                    }
                }).start();
                lastStamp=System.currentTimeMillis();
            }
            if(crtBitmap!=null&&crtLayout!=null) {
                srcBitmap = crtBitmap.copy(Bitmap.Config.ARGB_8888, true);
                srcLayout = crtLayout;
                srcLayoutJson = crtLayoutJson;
            }
        }catch (Exception e)
        {
            e.printStackTrace();
            CrashReport.postCatchedException(e);
        }*/
    }

    synchronized private void saveToFile(final ActionRecord record,long stamp,int index,CharSequence packageName)
    {
        try
        {
            if(record.getSrcImage()==null || record.getSrcLayout()==null || record.getDstImage()==null || record.getDstLayout()==null)
                return ;
            FileOutputStream fos = null;
            RECORD_PRODUCEDS = stamp;
            String curDir = STORE_DIRECTORY+"/screenshots/"+Utility.USER+"/";
            File storeDirectory = new File(curDir);
            if (!storeDirectory.exists()) {
                boolean success = storeDirectory.mkdirs();
                if (!success) {
                    Log.e("error", "failed to create file storage directory.");
                    return;
                }
            }
            Log.d("save file",curDir+ RECORD_PRODUCEDS);
            fos = new FileOutputStream(curDir+ RECORD_PRODUCEDS + "_src.png");
            record.getSrcImage().compress(Bitmap.CompressFormat.JPEG, 10, fos);

            fos.close();
            fos = new FileOutputStream(curDir+ RECORD_PRODUCEDS + "_src_layout.json");
            fos.write(record.getSrcLayout().getBytes());
            fos.close();
            if(packageName!=null)
                fos = new FileOutputStream(curDir+ RECORD_PRODUCEDS + "_actions_"+packageName+".json");
            else
                fos = new FileOutputStream(curDir+ RECORD_PRODUCEDS + "_actions_.json");
            Gson gson = new Gson();
            fos.write(gson.toJson(record.getActionList()).getBytes());
            fos.close();
            fos = new FileOutputStream(curDir+ RECORD_PRODUCEDS + "_dst_layout.json");
            fos.write(record.getDstLayout().getBytes());
            fos.close();
            fos = new FileOutputStream(curDir+ RECORD_PRODUCEDS + "_dst.png");
            record.getDstImage().compress(Bitmap.CompressFormat.JPEG, 10, fos);
            //upload(record,RECORD_PRODUCEDS,index);

            record.getSrcImage().recycle();
            record.getDstImage().recycle();
            fos.close();
            recordService.increaseCount(packageName);

        }catch (Exception e)
        {
            e.printStackTrace();
            //CrashReport.postCatchedException(e);
        }
    }

    synchronized private void sendMessageToServer(Bitmap img, long stamp, MergedBlock mergedBlock) {
        try {
            Log.e("on process","sending messages to server");
            String url = "http://"+Utility.IPAddress+":"+Utility.Port+"/demo";
            System.out.print("url: "+url);
            long startTime =  System.currentTimeMillis();

            OkHttpClient client = new OkHttpClient();
            FormBody.Builder formBuilder = new FormBody.Builder();
            AccessibilityNodeInfoRecord virRoot = AccessibilityNodeInfoRecord.buildTree();
            AccessibilityNodeInfoRecord crtRoot = virRoot;
            if (virRoot.children.size() != 0) {
                crtRoot = virRoot.children.get(0);
            }
            boolean testFromFile = false;
            int testPageId = 37;
            int testStateId = 0;
            int testInstanceId = 0;
            if (testFromFile) {
                crtRoot = mergedBlock.getUIRootFromFile(testPageId, testStateId,testInstanceId);
            }
            if (crtRoot != null) {
                crtRoot.getAllImportantNodes();
//                int pageIndex = PageIdentifier.getPageIndex(crtRoot,img.getHeight(),img.getWidth());
//
//                System.out.println("testpageindex"+pageIndex);
//                SemanticBlock rootBlock = mergedBlock.generateSemanticBlockFromBottom(crtRoot, mergedBlock.pageId2SemanticBlocks.get(pageIndex));
//                String blockLayoutJson = rootBlock.dumpToJson().toString();
//                formBuilder.add("block", blockLayoutJson);
//                formBuilder.add("pageindex", pageIndex+"");
                formBuilder.add("layout", Utility.convertUITreeToJson(crtRoot,0).toString());
            }

            String screenshot = null;
            if (!testFromFile) screenshot=Utility.bitmap2String(img);
            else screenshot = Utility.bitmap2String(mergedBlock.getImageFromFile(testPageId, testStateId,testInstanceId));
            formBuilder.add("screenshot",screenshot);
            long midTime = System.currentTimeMillis();
            System.out.println("mergedBlock: "+(midTime-startTime));
            Request request = new Request.Builder().url(url).post(formBuilder.build()).build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //Context context = ActivityManager.getInstance().getActivity();
                                    //Toast.makeText(MainActivity.this,"服务器错误",Toast.LENGTH_SHORT).show();
                                    Log.e("error","服务器错误");
                                    //System.out.println("服务器错误");
                                }
                            });
                        }
                    });
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    final String res = response.body().string();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (res.equals("0")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Toast.makeText(recordService.getApplicationContext(),"失败",Toast.LENGTH_SHORT).show();
                                        Log.e("error","失败");
                                        //System.out.println("失败");
                                    }
                                });
                            } else {
                                long endTime =  System.currentTimeMillis();
                                long transTime = endTime-midTime;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e("info","成功");
                                        //System.out.println("net: "+transTime);
                                    }
                                });

                            }

                        }
                    });
                }
            });
        }catch (Exception e)
        {
            e.printStackTrace();
            //CrashReport.postCatchedException(e);
        }

    }


    /*
    synchronized private void sendMessageToServer(Bitmap img, long stamp, MergedBlock mergedBlock) {
        try {
            Log.e("on process","sending messages to server");

            long startTime =  System.currentTimeMillis();
            JsonObject jsonObject = new JsonObject();
            Gson gson = new Gson();
            AccessibilityNodeInfoRecord virRoot = AccessibilityNodeInfoRecord.buildTree();
            AccessibilityNodeInfoRecord crtRoot = virRoot;
            if (virRoot.children.size() != 0) {
                crtRoot = virRoot.children.get(0);
            }
            boolean testFromFile = false;
            cnt += 1;
            int testPageId = 0;
            int testStateId = 0;
            int testInstanceId = 0;
            if (testFromFile) {
                crtRoot = mergedBlock.getUIRootFromFile(testPageId, testStateId, testInstanceId);
            }
            int pageIndex = -1;
            if (crtRoot != null) {
                crtRoot.getAllImportantNodes();
                pageIndex = Utility.getPageIndex(crtRoot);
                SemanticBlock rootBlock = mergedBlock.generateSemanticBlockFromBottom(crtRoot, mergedBlock.pageId2SemanticBlocks.get(pageIndex));
                MergedBlock.lastRootBlock = rootBlock;
                String blockLayoutJson = rootBlock.dumpToJson().toString();
                JsonObject blockJson = gson.fromJson(blockLayoutJson, JsonObject.class);
                jsonObject.add("block", blockJson);
                jsonObject.addProperty("pageIndex",pageIndex);
            }
            else jsonObject.addProperty("pageIndex",0);
            String screenshot;
            if (!testFromFile) screenshot=Utility.bitmap2String(img);
            else screenshot = Utility.bitmap2String(mergedBlock.getImageFromFile(testPageId, testStateId,testInstanceId));
            jsonObject.addProperty("screenshot",screenshot);
            String jsonString = gson.toJson(jsonObject);
            int stringLength = jsonString.length();
            byte[] byteData = ("="+String.valueOf(stringLength)+"="+jsonString).getBytes();
            Log.e("nb","length of json: "+stringLength);
            long midTime = System.currentTimeMillis();
            System.out.println("Page Process Time: "+(midTime - startTime));
            System.out.println("page_index: "+pageIndex);
            TCPClientController.sharedCenter().send(byteData);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    */
    synchronized private void receiveMessageFromServer() {
        try {
            String url = "http://192.168.1.4:5000/pageinfo";

            OkHttpClient client = new OkHttpClient();
            FormBody.Builder formBuilder = new FormBody.Builder();
            Request request = new Request.Builder().url(url).post(formBuilder.build()).build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //Context context = ActivityManager.getInstance().getActivity();
                                    //Toast.makeText(MainActivity.this,"服务器错误",Toast.LENGTH_SHORT).show();
                                    Log.e("error","服务器错误");
                                    System.out.println("服务器错误");
                                }
                            });
                        }
                    });
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    final String res = response.body().string();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (res.equals("0")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Toast.makeText(recordService.getApplicationContext(),"失败",Toast.LENGTH_SHORT).show();
                                        Log.e("error","失败");
                                        System.out.println("失败");
                                    }
                                });
                            } else {
                                String screenshot= "";
                                String layout = "";
                                String page = "";
                                try {
                                    JSONObject  jsonObject  = new JSONObject (res);
                                    //JSONArray jsonarray = new JSONArray(res);//将返回的信息转换成JSON形式
                                    screenshot = jsonObject.getString("screenshot");
                                    layout = jsonObject.getString("layout");
                                    page = jsonObject.getString("page");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                String finalScreenshot = screenshot;
                                String finalLayout = layout;
                                String finalPage = page;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e("info","成功"+res);
                                        System.out.println("成功"+res);
                                    }
                                });

                            }

                        }
                    });
                }
            });
        }catch (Exception e) {
            e.printStackTrace();
            //CrashReport.postCatchedException(e);
        }
    }
}
