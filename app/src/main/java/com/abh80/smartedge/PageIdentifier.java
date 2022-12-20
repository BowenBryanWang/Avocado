package com.abh80.smartedge;

import android.content.res.AssetManager;
import android.graphics.Rect;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;



class RecComparator implements Comparator <Pair<Pair<String, Rect>, String>> {
    public int compare(Pair<Pair<String, Rect>, String> a, Pair<Pair<String, Rect>, String> b) {
        //按照左上角的y值进行排序
        if(a.first.second.bottom > b.first.second.bottom){
            return 1;
        }else if(a.first.second.bottom == b.first.second.bottom){
            return 0;
        }else{
            return -1;
        }
    }
}

public class PageIdentifier {
    private static String TAG = "PageIdentifier";
    private static JSONArray list = null;

    public static void init() {
//        Context context = MainApplication.getInstance().getContext();
        try {
//            AssetManager assetManager = context.getAssets(); // 获得assets资源管理器（assets中的文件无法直接访问，可以使用AssetManager访问）
            AssetManager assetManager = RecordService.self.getApplicationContext().getAssets(); // 获得assets资源管理器（assets中的文件无法直接访问，可以使用AssetManager访问）
            InputStreamReader inputStreamReader = new InputStreamReader(assetManager.open("correct2.json"), "UTF-8"); // 使用IO流读取json文件内容
            BufferedReader br = new BufferedReader(inputStreamReader);
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
            br.close();
            inputStreamReader.close();
            JSONObject correctJson = new JSONObject(builder.toString()); // 从builder中读取了json中的数据。
            list = correctJson.getJSONArray("list");
        } catch (Exception e) {
            e.printStackTrace();
        }
//        if (list == null) {
//            LogUtil.e(TAG, "init correct2.json fail!");
//        }

    }

    /**
     * 页面           |      页面识别id    |     service构建id
     * -------------------------------------------------------
     * 微信主页        |    2            | 0
     * 通讯录          |    3             | 1
     * 聊天页面
     * 文本框输入    |     5          |37   可sendMessage
     * 表情页面    |     231                可sendMessage
     * 打开键盘     |     229               可sendMessage
     * 按住说话     |     232
     * 打开加号     |    230                可sendMessage
     * 用户头像页面     |    47,12,204,119   | 4
     * 发起通话页面     |    38             | 5
     * 用户头像页面     |    47             | 4
     * 视频通话        |    39             |6
     * 视频通话弹窗     |    226            |  226
     * 语音通话弹窗     |    227             | 227
     * 长按发语音      |     233             | 233
     * 发红包         |     300             | 300
     **/
    public static Map<Integer, Integer> pageIndexMapping = new HashMap<Integer, Integer>() {{
        put(2, 0);
        put(3, 1);
        put(5, 37);
        put(229, 37);
        put(230, 230);
        put(231, 37);
        put(232, 232);
        put(38, 5);
        //put(47, 4);
        put(12, 4);
        put(204, 4);
        //put(119, 4);
        put(223, 6);
        put(224, 7);
        put(225, 8);
        put(39, 9);
        put(226, 226);
        put(227, 227);
        put(233, 233);
        put(300,300);
    }};

    public static Map<Integer, Integer> reversePageIndexMapping = new HashMap<Integer, Integer>() {{
        put(0, 2);
        put(1, 3);
        put(37, 5);
        put(229, 229);
        put(230, 230);
        put(231, 231);
        put(232, 232);
        put(4, 12);
        put(5, 38);
        put(6, 223);
        put(7, 224);
        put(8, 225);
        put(9, 39);
        put(226, 226);
        put(227, 227);
        put(300, 300);
        put(233, 233);
        put(300,300);
    }};

    //把形似[a,b][c,d]的字符串，转换为4个数字并返回
    public static int[] str2num(String str) {
        int res[] = new int[4];
        int i = 0;
        int cnt = 0;
        while (i < str.length()) {
            int now = -1;
            for (int j = i + 1; j <= str.length(); j++) {
                String temp = str.substring(i, j);
                try {
                    now = Integer.parseInt(temp);
                } catch (NumberFormatException e) {
                    if (now != -1 || j == str.length()) {
                        res[cnt] = now;
                        i = j;
                        cnt++;
                    } else {
                        i++;
                    }
                    break;
                }
            }
        }
        return res;
    }

    //Shape1是标注的真值 Shape2是当前的数据
    public static int[] get_similiarity(JSONArray shape1, Vector<Pair<Pair<String, Rect>, String>> shape2,int screenHeightPixel,int screenWidthPixel) throws JSONException {
        int res[] = new int[3];
        int cnt = 0, weight = 0, bound_sim = 0;
        double err = 0.1; //5%的容错率
//        DisplayMetrics dm = MainApplication.getInstance().getDisplayMetrics();
//
//        int screenHeightPixel = dm.heightPixels;
//        int screenWidthPixel = dm.widthPixels;
        boolean flag1 = false, flag2 = false;
        Set<Integer> s = new HashSet<Integer>();

        for (int idx2 = 0; idx2 < shape2.size(); idx2++) {
            Pair<Pair<String, Rect>, String> item2 = shape2.get(idx2);
//            Log.i("PageIndex","idx2:"+idx2+"label2: "+item2.first.first);
            for (int idx1 = 0; idx1 < shape1.length(); idx1++) {
                if (s.contains(idx1))
                    continue;
                JSONObject item1 = shape1.getJSONObject(idx1);
                String[] all = item1.getString("label").split("\\|\\|"); //某个位置上可能出现不一样的字符串
                for(int i=0;i<all.length;i++) {
                    String label = all[i];
                    if (label.equals(item2.first.first)) {
                        String points1 = item1.getString("points");
                        int[] cor1 = str2num(points1);
                        int lx1 = cor1[0], ly1 = cor1[1];
                        int w1 = cor1[2] - lx1;
                        int h1 = cor1[3] - ly1;
                        int lx2 = item2.first.second.left, ly2 = item2.first.second.top;
                        int w2 = item2.first.second.width(), h2 = item2.first.second.height();
                        if (Math.abs(w1 - w2) < screenWidthPixel * err && Math.abs(h1 - h2) < screenHeightPixel * err && Math.abs(lx1 - lx2) < screenWidthPixel * err && Math.abs(ly1 - ly2) < screenHeightPixel * err)
                            bound_sim += 1;
                        if (cor1[3] < screenHeightPixel * 0.2 && item2.first.second.bottom < screenHeightPixel * 0.2 && label != "返回" && label != "更多" && label != "完成") {
                            if (item2.first.first == "EDIT_TEXT")
                                flag1 = true;
                            if (item2.first.first == "取消" && flag1)
                                flag2 = true;
                            if (item2.second.equals("TYPE_SYSTEM")) {
                                Log.i("", "label1: " + item2.first.first);
                                weight += 20;
                            } else
                                weight += 5;
                            cnt++; //匹配的数目增加了
                            s.add(idx1);
                        } else {
                            weight += 1;
                            cnt++; //匹配的数目增加了
                            s.add(idx1);
                        }
                        break;
                    }
                    else if(label.equals("表情")){
                        if(item2.first.first.indexOf("表情")!=-1){
                            weight += 1;
                            cnt++; //匹配的数目增加了
                        }
                    }
                }
            }
        }
        if (flag2)
            weight += 10;
        res[0] = cnt;
        res[1] = weight;
        res[2] = bound_sim;
//        Log.i("","cnt:"+cnt);
//        Log.i("","weight:"+weight);
        return res;
    }

    //去除括号及其中的内容
    public static String remove_bracket(String str) {
        while (str.indexOf("(") != -1) {
            int idx1 = str.indexOf("(");
            int idx2 = str.indexOf(")");
            if (idx2 == -1 || idx2 < idx1)
                break;
            String new_str = str.substring(0, idx1);
            new_str += str.substring(idx2 + 1, str.length());
            str = new_str;
        }
        return str;
    }

    //去除冒号后的东西
    public static String remove_colon(String str) {
        int idx1 = str.indexOf(":");
        String new_str = str;
        if (idx1 != -1)
            new_str = str.substring(0, idx1);
        return new_str;
    }

    //去除冒号后的东西
    public static String remove_dot(String str) {
        int idx1 = str.indexOf(".");
        String new_str = str;
        if (idx1 != -1)
            new_str = str.substring(0, idx1);
        return new_str;
    }

    //获取页面Id时 只需要可见的元素 因此要用bounds进行判断
    public static Vector<Pair<Pair<String, Rect>, String>> convertUITreeToVisibleJson(AccessibilityNodeInfoRecord node) {
        if (node == null) {
//            LogUtil.i("PageIndex", "nullRoot");
            return null;
        }
        Vector<Pair<Pair<String, Rect>, String>> res = new Vector<>();
        // 使用node info 以获得第一手的材料
        // node 由调用者负责回收
//        JSONObject res = new JSONObject();
        try {
            Rect r = new Rect();
            node.getBoundsInScreen(r);
            String bounds = r.toShortString();
            int[] cor = str2num(bounds);
            if (cor[0] >= cor[2] || cor[1] >= cor[3]) {
                return null;
            }
//            String type = node.getWindowTypeString();
            String label = "";
            if (node.isScrollable()) {
                label = "SCROLLABLE";
            } else if (node.getClassName() != null && node.getClassName() == "EditText") {
                label = "EDIT_TEXT";
            } else if (node.getText() != null && node.getText().toString().length() > 0) {
                label = remove_dot(remove_colon(remove_bracket(node.getText().toString())));
//                    Log.i("PageIndex", "text: " + label);
            } else if (node.getContentDescription() != null && node.getContentDescription().toString().length() > 0) {
                label = remove_dot(remove_colon(remove_bracket(node.getContentDescription().toString())));
//                Log.i("PageIndex", "content: " + label);
            }
            if (label != "") {
//                if(type.equals("TYPE_SYSTEM"))
//                    Log.i("PageIndex","TYPE_SYSTEM:"+label);
//                Log.i("PageIndex", "label: " + label);
//                Log.i("PageIndex", "bounds: " + bounds);
                res.add(new Pair<Pair<String, Rect>, String>(new Pair<>(label, r), ""));
            }
            int childrenCount = node.getChildCount();
            if (childrenCount == 1) {
                AccessibilityNodeInfoRecord child = node.getChild(0);
                Vector<Pair<Pair<String, Rect>, String>> c = convertUITreeToVisibleJson(child);
                if (c != null) {
                    res.addAll(c);
                }
            } else if (childrenCount > 1) {
                for (int i = 0; i < childrenCount; ++i) {
                    AccessibilityNodeInfoRecord child = node.getChild(i);
                    if (child == null) {
                        continue;
                    }
                    Vector<Pair<Pair<String, Rect>, String>> c = convertUITreeToVisibleJson(child);
                    if (c != null) {
                        res.addAll(c);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static void test(){
        String str = "松开 发送||上滑取消或转文字";

        String[] all = str.split("\\|\\|"); //某个位置上可能出现不一样的字符串
        Log.i("PageIndex","str:"+all.length);
        for(int i =0;i< all.length;i++){
            Log.i("PageIndex","str:"+all[i]);
        }
    }

    public static int getPageIndex(AccessibilityNodeInfoRecord root,int pageheight,int pagewidth) {
        if (list == null) init();
//        test();
        try {
            //读取现在的json文件
            //Log.i("PageIndex","label:--------------------");
            Vector<Pair<Pair<String, Rect>, String>> data = convertUITreeToVisibleJson(root);


            if (data == null) {
//                LogUtil.d("PageIndex", "convert ui tree null");
                return -1;
            }

            //对data进行排序
            Comparator<Pair<Pair<String, Rect>, String>> cmp =new RecComparator();
            Collections.sort(data, cmp);

//            Log.i("PageIndex","label:--------------------");
//
//            for(int idx2=0;idx2<data.size();idx2++) {
//                Pair<Pair<String,Rect>,String> item2 = data.get(idx2);
////                if(item2.first.first.equals("邀请你进行视频通话...")){
////                    Log.i("PageIndex","type:"+item2.second);
////                }
//                if (!item2.second.equals("TYPE_SYSTEM"))
//                    Log.i("PageIndex",item2.first.first);
//            }
//            Log.i("PageIndex","label:--------------------");


            int maxx_weight = 0, max_id = -1, min_len = 0, max_sim = 0;

            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.getJSONObject(i);
                int res[] = get_similiarity(item.getJSONArray("shapes"), data,pageheight,pagewidth);
                int now_len = item.getJSONArray("shapes").length();

                int cnt = res[0], weight = res[1], bound_sim = res[2];

//                if(item.getInt("id")==5 || item.getInt("id")==229 || item.getInt("id")==231 ){
//                    Log.i("PageIndex","id: "+item.getInt("id")+" cnt: "+cnt+" w:"+weight +" sim:"+bound_sim);
//                }

                if (((double) cnt / (double) now_len) < 0.25 || (cnt == 1 && weight == 1 && now_len > 1))
                    continue;

                if (weight > maxx_weight) {
                    maxx_weight = weight;
                    max_sim = bound_sim;
                    max_id = item.getInt("id");
                    min_len = now_len;
                } else if (weight == maxx_weight) {
                    if (now_len < min_len) {
                        max_id = item.getInt("id");
                        min_len = now_len;
                        max_sim = bound_sim;
                    } else if (now_len == min_len) {
                        if (bound_sim > max_sim) {
                            max_id = item.getInt("id");
                            max_sim = bound_sim;
                        }
                    }
                }
            }
            Log.i("PageIndex", "max_id:" + max_id);
//            LogUtil.d("PageIndex", "original: "+ max_id);
            Log.d("PageIndex_RESULT", max_id + ", " + pageIndexMapping.getOrDefault(max_id, -1));
//            Log.e("PageIndex", " " + pageIndexMapping.getOrDefault(max_id, -1));
            if (max_id == -1) {
                Log.d("PageIndex_ROOT", "root.dumpTree().toString()");
            }
            return pageIndexMapping.getOrDefault(max_id, -1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e("PageIndex", "out of try, return -1");
        return -1;
    }
}

