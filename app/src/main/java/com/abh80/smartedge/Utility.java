package com.abh80.smartedge;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.WINDOW_SERVICE;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static com.abh80.smartedge.AccessibilityNodeInfoRecordFromFile.Action.backwardsKeywords;
import static com.abh80.smartedge.xdevice.ImportantStructureUtility.getTitle;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kinnplh on 2018/5/14.
 */

public class Utility {
    public static boolean isDebug = true;

    public static TextToSpeech tts;
    static boolean ttsPrepared;
    static RecordService service;
    public static String USER = null;
    public static float screenWidthPixel;
    public static float screenHeightPixel;
    public static final Integer THREAD_MUTEX = 0;
    public static String IPAddress = "166.111.139.15";
    public static String Port = "5000";

    public static void setUsername(String name)
    {
        USER = name;
        setSharedPreferenceData("username",Utility.USER);
    }

    public static void init(RecordService s){
        Utility.service = s;
        /*
        if(tts == null){
            tts = new TextToSpeech(s, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    ttsPrepared = (i == TextToSpeech.SUCCESS);
                    if(!ttsPrepared){
                        tts = null;
                    } else {
                        tts.setLanguage(Locale.CHINESE);
                    }
                }
            });
        }
         */
        if(USER!= null)
            setSharedPreferenceData("username",Utility.USER);
        else
            USER = getSharedPreferenceData("username");
        Point p = new Point();
        ((WindowManager) s.getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealSize(p);
        screenHeightPixel = p.y;
        screenWidthPixel = p.x;
    }
    public static String printTree(AccessibilityNodeInfo root){
        StringBuffer res = new StringBuffer();
        printNodeStructure(root, 0, res);
        return res.toString();
    }

    public static void printNodeStructure(AccessibilityNodeInfo root, int depth, StringBuffer res){
        if(root == null){
            return;
        }
        root.refresh();
        Rect border = new Rect();
        root.getBoundsInScreen(border);
        for(int i = 0; i < depth; i ++){
            res.append("\t");
        }

        res.append(root.hashCode()).append(" ")
                .append(root.getClassName()).append(" ")
                .append(root.getViewIdResourceName()).append(" ")
                .append(border.toString()).append(" ")
                .append(root.getText()).append(" ")
                .append(root.getContentDescription()).append(" ")
                .append("isClickable: ").append(root.isClickable()).append(" ")
                .append("isScrollable: ").append(root.isScrollable()).append(" ")
                .append("isVisible: ").append(root.isVisibleToUser()).append(" ")
                .append("isEnabled: ").append(root.isEnabled()).append(" ")
                .append("labelBy: ").append((root.getLabeledBy() == null)? -1: root.getLabeledBy().hashCode()).append("\n");

        //res.append(root.toString()).append("\n");
        for(int i = 0; i < root.getChildCount(); ++ i){
            printNodeStructure(root.getChild(i), depth + 1, res);
        }
    }

    public static void shutdownTts(){
        if(tts != null){
            tts.shutdown();
            tts = null;
        }
    }
    public static boolean isNodeVisible(AccessibilityNodeInfo node){
        if(node == null){
            return false;
        }
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        return r.width() > 0 && r.height() > 0;
    }
    public static boolean isNodeVisible(AccessibilityNodeInfoRecord node){
        if(node == null){
            return false;
        }
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        return r.width() > 0 && r.height() > 0;
    }
    public static AccessibilityNodeInfo getNodeByNodeId(AccessibilityNodeInfo startNode, String relativeNodeId){
        if(startNode == null){
            return null;
        }
        int indexEnd = relativeNodeId.indexOf(';');
        if(indexEnd < 0){
            // 不存在分号，说明已经结束了
            if(startNode.getClassName().toString().equals(relativeNodeId)){
                return startNode;
            } else {
                return null;
            }
        }

        String focusPart = relativeNodeId.substring(0, indexEnd);
        int indexDivision = focusPart.indexOf('|');
        int childIndex = Integer.valueOf(focusPart.substring(indexDivision + 1));
        String crtPartClass = focusPart.substring(0, indexDivision);
        String remainPart = relativeNodeId.substring(indexEnd + 1);

        // 这里对 id 的处理方式，应该和 crawler 中的处理方式相一致的
        if(startNode.isScrollable() || (startNode.getClassName() != null && startNode.getClassName().toString().contains("ListView"))){
            int actualIndex = 0;
            while (actualIndex < startNode.getChildCount()) {
                if (isNodeVisible(startNode.getChild(actualIndex))) {
                    childIndex -= 1;
                    if (childIndex < 0) {
                        break;
                    }
                }
                actualIndex += 1;
            }
            childIndex = actualIndex;
        }

        if(startNode.getClassName().toString().equals(crtPartClass) && childIndex >= 0 && childIndex < startNode.getChildCount()){
            return getNodeByNodeId(startNode.getChild(childIndex), remainPart);
        } else {
            return null;
        }
    }

    public static void generateLayoutXML(AccessibilityNodeInfo crtRoot, int indexInParent, StringBuilder builder){
        // 生成描述这个节点及其子节点的 xml 字符串
        builder.append("<node ");
        appendField("index", indexInParent, builder);
        appendField("text", crtRoot.getText(), builder);
        appendField("resource-id", crtRoot.getViewIdResourceName(), builder);
        appendField("class", crtRoot.getClassName(), builder);
        appendField("package", crtRoot.getPackageName(), builder);
        appendField("content-desc", crtRoot.getContentDescription(), builder);
        appendField("checkable", crtRoot.isCheckable(), builder);
        appendField("checked", crtRoot.isChecked(), builder);
        appendField("clickable", crtRoot.isClickable(), builder);
        appendField("enabled", crtRoot.isEnabled(), builder);
        appendField("focusable", crtRoot.isFocusable(), builder);
        appendField("focused", crtRoot.isFocused(), builder);
        appendField("scrollable", crtRoot.isScrollable(), builder);
        appendField("long-clickable", crtRoot.isLongClickable(), builder);
        appendField("password", crtRoot.isPassword(), builder);
        appendField("selected", crtRoot.isSelected(), builder);
        appendField("editable", crtRoot.isEditable(), builder);
        appendField("accessibilityFocused", crtRoot.isAccessibilityFocused(), builder);
        appendField("dismissable", crtRoot.isDismissable(), builder);

        Rect r = new Rect();
        crtRoot.getBoundsInScreen(r);
        builder.append("bounds=\"").append('[').append(r.left).append(',').append(r.top).append("][").append(r.right).append(',').append(r.bottom).append(']').append('"');
        if(crtRoot.getChildCount() == 0){
            builder.append("/>");
        } else {
            builder.append(">");
            for(int i = 0; i < crtRoot.getChildCount(); ++ i){
                if(crtRoot.getChild(i) == null){
                    continue;
                }
                generateLayoutXML(crtRoot.getChild(i), i, builder);
            }
            builder.append("</node>");
        }
    }

    public static void generateLayoutXMLWithoutUselessNodes(AccessibilityNodeInfo crtRoot, int indexInParent, StringBuilder builder){
        assert indexInParent == 0;
        long start1 = System.currentTimeMillis();
        AccessibilityNodeInfoRecord root = new AccessibilityNodeInfoRecord(crtRoot, null, 0);
        long start2 = System.currentTimeMillis();
        Log.i("time spend", "build record " + String.valueOf(start2 - start1));
        root.ignoreUselessChild(false);
        long start3 = System.currentTimeMillis();
        Log.i("time spend", "ignore useless " + String.valueOf(start3 - start2));
        generateLayoutXMLWithRecord(root, 0, builder);
        long start4 = System.currentTimeMillis();
        Log.i("time spend", "get xml " + String.valueOf(start4 - start3));
    }
    public static void generateLayoutXMLWithRecord(AccessibilityNodeInfoRecord crtRoot, int indexInParent, StringBuilder builder) {
        builder.append("<node ");
        appendField("index", indexInParent, builder);
        appendField("text", crtRoot.getText(), builder);
        appendField("resource-id", crtRoot.getViewIdResourceName(), builder);
        appendField("class", crtRoot.getClassName(), builder);
        appendField("package", crtRoot.getPackageName(), builder);
        appendField("content-desc", crtRoot.getContentDescription(), builder);
        appendField("checkable", crtRoot.isCheckable(), builder);
        appendField("checked", crtRoot.isChecked(), builder);
        appendField("clickable", crtRoot.isClickable(), builder);
        appendField("enabled", crtRoot.isEnabled(), builder);
        appendField("focusable", crtRoot.isFocusable(), builder);
        appendField("focused", crtRoot.isFocused(), builder);
        appendField("scrollable", crtRoot.isScrollable(), builder);
        appendField("long-clickable", crtRoot.isLongClickable(), builder);
        appendField("password", crtRoot.isPassword(), builder);
        appendField("selected", crtRoot.isSelected(), builder);
        appendField("editable", crtRoot.isEditable(), builder);
        appendField("accessibilityFocused", crtRoot.isAccessibilityFocused(), builder);
        appendField("dismissable", crtRoot.isDismissable(), builder);

        Rect r = new Rect();
        crtRoot.getBoundsInScreen(r);
        builder.append("bounds=\"").append('[').append(r.left).append(',').append(r.top).append("][").append(r.right).append(',').append(r.bottom).append(']').append('"');
        if(crtRoot.getChildCount() == 0){
            builder.append("/>");
        } else {
            builder.append(">");
            for(int i = 0; i < crtRoot.getChildCount(); ++ i){
                if(crtRoot.getChild(i) == null){
                    continue;
                }
                generateLayoutXMLWithRecord(crtRoot.getChild(i), i, builder);
            }
            builder.append("</node>");
        }
    }
    public static JSONObject generateLayoutJSONWithoutUselessNodes(AccessibilityNodeInfo crtRoot, int indexInParent){
        assert indexInParent == 0;
        long start1 = System.currentTimeMillis();
        AccessibilityNodeInfoRecord root = new AccessibilityNodeInfoRecord(crtRoot, null, 0);
        long start2 = System.currentTimeMillis();
        Log.i("time spend", "build record " + String.valueOf(start2 - start1));
        root.ignoreUselessChild(false);
        long start3 = System.currentTimeMillis();
        Log.i("time spend", "ignore useless " + String.valueOf(start3 - start2));
        JSONObject object = generateLayoutJSONWithRecord(root, 0);
        long start4 = System.currentTimeMillis();
        Log.i("time spend", "get json " + String.valueOf(start4 - start3));
        return object;
    }

    public static JSONObject generateLayoutJSONWithRecord(AccessibilityNodeInfoRecord crtRoot, int indexInParent){
        // 生成描述这个节点及其子节点的 xml 字符串
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("index", indexInParent);
            jsonObject.put("text", crtRoot.getText());
            jsonObject.put("resource-id", crtRoot.getViewIdResourceName());
            jsonObject.put("class", crtRoot.getClassName());
            jsonObject.put("package", crtRoot.getPackageName());
            jsonObject.put("content-desc", crtRoot.getContentDescription());
            jsonObject.put("checkable", crtRoot.isCheckable());
            jsonObject.put("checked", crtRoot.isChecked());
            jsonObject.put("clickable", crtRoot.isClickable());
            jsonObject.put("enabled", crtRoot.isEnabled());
            jsonObject.put("focusable", crtRoot.isFocusable());
            jsonObject.put("focused", crtRoot.isFocused());
            jsonObject.put("scrollable", crtRoot.isScrollable());
            jsonObject.put("long-clickable", crtRoot.isLongClickable());
            jsonObject.put("password", crtRoot.isPassword());
            jsonObject.put("selected", crtRoot.isSelected());
            jsonObject.put("editable", crtRoot.isEditable());
            jsonObject.put("accessibilityFocused", crtRoot.isAccessibilityFocused());
            jsonObject.put("dismissable", crtRoot.isDismissable());


            Rect r = new Rect();
            crtRoot.getBoundsInScreen(r);
            jsonObject.put("bounds","["+r.left+","+r.top+"]["+r.right+","+r.bottom+"]");
            JSONArray jsonArray = new JSONArray();
            for(int i = 0; i < crtRoot.getChildCount(); ++ i){
                if(crtRoot.getChild(i) == null){
                    continue;
                }
                jsonArray.put(generateLayoutJSONWithRecord(crtRoot.getChild(i), i));
            }
            jsonObject.put("children",jsonArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    static void appendField(String name, String value, StringBuilder builder){
        builder.append(name).append("=\"").append(value == null? "": intoXMLFormat(value)).append("\" ");
    }

    static void appendField(String name, int value, StringBuilder builder){
        builder.append(name).append("=\"").append(value).append("\" ");
    }

    static void appendField(String name, CharSequence value, StringBuilder builder){
        builder.append(name).append("=\"").append(value == null? "": intoXMLFormat(value)).append("\" ");
    }

    static void appendField(String name, boolean value, StringBuilder builder){
        builder.append(name).append("=\"").append(value? "true": "false").append("\" ");
    }

    static String intoXMLFormat(Object s){
        return s == null? "": s.toString().replace("\n", " ")
                .replace("&", "&amp;")
                .replace("\'", "&apos;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("#", " ");
    }

    /*public static JSONArray dumpJSONLayout(String activityname)
    {
        JSONArray array = new JSONArray();
        for(AccessibilityNodeInfoRecord record: AccessibilityNodeInfoRecord.roots)
        {
            JSONObject object = convertUITreeToJson(record,0);
            try {
                if(record==null)
                    return null;
                object.put("@package-version",getVersionName(record.getPackageName()));
                object.put("@activity-name",activityname);
                object.put("@window-type",record.windowType);
                object.put("@window-title",record.windowTitle);

            } catch (Exception e) {
                e.printStackTrace();
            }
            array.put(object);
        }
        return array;
    }*/

    public static JSONObject dumpJSONBlockLayout(Block rootBlock) {
        //todo
        JSONObject object = rootBlock.convertBlockTreeToJson();
        return object;
    }

    public static JSONObject windowFilter(JSONArray layout) {
        for (int i = 0; i < layout.length(); i++) {
            try {
                JSONObject window = layout.getJSONObject(i);
                if (window.get("@window-type").equals(1)) {
                    return window;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /*public static AccessibilityNodeInfoRecord getMainWindowRoot() {
        for(AccessibilityNodeInfoRecord record: AccessibilityNodeInfoRecord.roots) {
            try {
                if(record==null)
                    return null;
                if (record.windowType == 1) {
                    AccessibilityNodeInfoRecord node = (AccessibilityNodeInfoRecord)record;
                    node.parent = null;
                    return node;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }*/

    /*public static Block generateBlockTree(AccessibilityNodeInfoRecord root) {
        root.getAllImportantNodes();
        Block rootBlock = new Block(root);
        Deque<Block> blockDeque = new LinkedList();
        blockDeque.offer(rootBlock);
        int i = 0;
        while (!blockDeque.isEmpty()) {
            i += 1;
            Block crtRootBlock = blockDeque.pollFirst();
            if (crtRootBlock.uiNode.getClassName().toString().contains("ListView")) {
                Log.i("Block","ListView");
            }
            VipsParser vipsParser = new VipsParser(crtRootBlock);
            vipsParser.parse();
            if (rootBlock == crtRootBlock) {
                rootBlock = vipsParser.rootBlock;
                while (rootBlock.parentBlock != null) rootBlock = rootBlock.parentBlock;
            }
            if (vipsParser.rootBlock.uiNode.getClassName().toString().contains("ListView")) {
                Log.i("Block","ListView");
            }
            if (vipsParser.blocksPool.size()==0) {
                if (vipsParser.rootBlock.neededProcess()) {
                    blockDeque.addLast(vipsParser.rootBlock);
                }
                continue;
            }
            if (vipsParser.rootBlock != crtRootBlock) {
                crtRootBlock = vipsParser.rootBlock;
            }

            //todo
            for (Block block : vipsParser.blocksPool) {
                block.parentBlock = crtRootBlock;
                crtRootBlock.childrenBlocks.add(block);
                if (block.neededProcess()) {
                    blockDeque.addLast(block);
                }
            }
        }
        return rootBlock;
    }*/

    public static boolean isTopLeft(AccessibilityNodeInfoRecord node) {
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        if (node.getBottom() > screenHeightPixel/8) return false;
        if (node.getRight() > screenWidthPixel/5) return false;
        return true;
    }

    public static AccessibilityNodeInfoRecord getTitieNode(AccessibilityNodeInfoRecord root) {
        if (root.getText()=="微信" || root.getText()=="发现" || root.getText()=="通讯录") {
            if (isTopLeft(root)) return root;
        }
        // TODO: 2021/3/10  
        for (AccessibilityNodeInfoRecord child : root.children)  {
            AccessibilityNodeInfoRecord res = getTitieNode(child);
            if (res!=null) return res;
        }
        return null;
    }

    public static int getPageIndex(AccessibilityNodeInfoRecord root) {
        //识别实时页面，返回所对应的页面id。
        // TODO: 2021/3/9
        AccessibilityNodeInfoRecord titleNode = getTitieNode(root);
        if (titleNode==null) {
            titleNode = getTitle(root);
        }
        //if (titleNode == null) return 11;
        if (titleNode == null) return 0;
        if (titleNode.getAllTexts().contains("微信")) return 0;
        if (titleNode.getAllTexts().contains("通讯录")) return 1;
        if (titleNode.getAllTexts().contains("发现")) return 2;
        if (titleNode.getAllTexts().contains("朋友圈")) return 12;
        AccessibilityNodeInfoRecord realRoot = root;
        if (realRoot.absoluteId.contains("fake")) realRoot = realRoot.getChild(0);
        if (root == null) return 0;
        if (root.getContentDescription() != null) {
            if (root.getContentDescription().toString().contains("朋友圈")) return 12;
            if (root.getContentDescription().toString().contains("与的聊天")) return 3; //我
            if (root.getContentDescription().toString().contains("聊天")) return 37;
        }
        Rect r = new Rect();
        titleNode.getBoundsInScreen(r);
        Rect screenR = new Rect();
        root.getBoundsInScreen(screenR);
        if (r.bottom*10 < screenR.bottom) return 37; //聊天界面
        return 0;

    }

    public static String getActivityName(CharSequence packagename,CharSequence classname,String lastclassname)
    {
        if(packagename==null||classname==null)
            return lastclassname;
        ComponentName componentName = new ComponentName(
                packagename.toString(),
                classname.toString()
        );
        ActivityInfo activityInfo = tryGetActivity(componentName);
        boolean isActivity = activityInfo != null;
        if (isActivity)
            return componentName.flattenToShortString();
        else
            return lastclassname;
    }

    private static ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return service.getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }



    public static JSONObject convertUITreeToJson(AccessibilityNodeInfoRecord node, int index){
        if(node == null){
            return null;
        }
        // 使用node info 以获得第一手的材料
        // node 由调用者负责回收
        JSONObject res = new JSONObject();
        try {
            res.put("@index", index);
            res.put("@text", node.getText());
            res.put("@resource-id", node.getViewIdResourceName());
            res.put("@class", node.getClassName());
            res.put("@package", node.getPackageName());
            res.put("@content-desc", node.getContentDescription());
            res.put("@checkable", node.isCheckable());
            res.put("@checked", node.isChecked());
            res.put("@clickable", node.isClickable());
            res.put("@enabled", node.isEnabled());
            res.put("@focusable", node.isFocusable());
            res.put("@focused", node.isFocused());
            res.put("@scrollable", node.isScrollable());
            res.put("@long-clickable", node.isLongClickable());
            res.put("@selected", node.isSelected());
            res.put("@editable", node.isEditable());
            //res.put("@accessibilityFocused", node.isAccessibilityFocused());
            //res.put("@dismissable", node.isDismissable());
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                res.put("@hint-text", node.getHintText());
            }*/
            Rect r = new Rect();
            node.getBoundsInScreen(r);
            res.put("@bounds", r.toShortString());
            res.put("@screenBounds", String.format("[%d,%d][%d,%d]", 0, 0, (int)screenWidthPixel, (int)screenHeightPixel));
            int childrenCount = node.getChildCount();
            if(childrenCount == 1){
                AccessibilityNodeInfoRecord child = node.getChild(0);
                JSONObject c = convertUITreeToJson(child, 0);
                res.put("node", c);
            } else if(childrenCount > 1){
                JSONArray children = new JSONArray();
                for(int i = 0; i < childrenCount; ++ i){
                    AccessibilityNodeInfoRecord child = node.getChild(i);
                    if(child == null){
                        continue;
                    }
                    JSONObject c = convertUITreeToJson(child, i);
                    children.put(c);
                }
                res.put("node", children);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /*public static String dumpLayout()
    {
        //service.getSoftKeyboardController().setShowMode(SHOW_MODE_HIDDEN);
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        AccessibilityNodeInfoRecord.buildAllTrees();
        int try_time = 10;
        while (root == null && try_time > 0){
            root = service.getRootInActiveWindow();
            try_time -= 1;
        }
        if(root == null){
            return null;
        }

        StringBuilder xmlBuilder = new StringBuilder();
        Utility.generateLayoutXMLWithoutUselessNodes(root, 0, xmlBuilder);
        xmlBuilder.append("\n");
        //service.getSoftKeyboardController().setShowMode(AccessibilityService.SHOW_MODE_AUTO);
        return xmlBuilder.toString();
    }*/

    public static String getVersionName(CharSequence packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }

        final PackageInfo packageInfo;
        try {
            packageInfo = service.getPackageManager().getPackageInfo(packageName.toString(),0);


        if (packageInfo == null) {
            return null;
        }
        return packageInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return  null;
    }



    /*public static String getNodeId(AccessibilityNodeInfo nodeInfo)
    {
        if(nodeInfo==null)
            return "";
        if(nodeInfo.getWindow()!=null&&nodeInfo.getWindow().getType()== AccessibilityWindowInfo.TYPE_SYSTEM&&nodeInfo.getViewIdResourceName()!=null &&
                (nodeInfo.getViewIdResourceName().contains("recent_apps")||nodeInfo.getViewIdResourceName().contains("back")||nodeInfo.getViewIdResourceName().contains("home")))
        {
            return "";
        }
        String nodeId = AccessibilityNodeInfoRecord.nodeInfoHashtoId.getOrDefault(nodeInfo.hashCode(),"");
        Log.d("nodeid",nodeInfo.toString() + nodeId);
        return nodeId;
    }

    public static String getNodeId(AccessibilityNodeInfo nodeInfo, AccessibilityNodeInfoRecord savedrecord)
    {
        if(nodeInfo==null)
            return "";
        if(nodeInfo.getWindow().getType()== AccessibilityWindowInfo.TYPE_SYSTEM&&nodeInfo.getContentDescription()!=null)
        {
            return "";
        }
        String nodeId = savedrecord.nodeInfoHashtoId.getOrDefault(nodeInfo.hashCode(),"");
        Log.d("nodeid",nodeInfo.toString() + nodeId);
        return nodeId;
    }*/


    public static String bitmap2String(Bitmap bitmap)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,50,outputStream );//压缩90%
        byte[] imagebyte = outputStream.toByteArray();
        String imageStr = Base64.encodeToString(imagebyte, Base64.DEFAULT);
        return imageStr;
    }

    public static String getReadableText(AccessibilityNodeInfo source) {
        if(source.getText()!=null)
            return source.getText().toString();
        else if(source.getContentDescription()!=null)
            return source.getContentDescription().toString();
        else
            return "";
    }
    public static void refreshListOfNodes(ArrayList<AccessibilityNodeInfo> list_nodes,
                                          AccessibilityNodeInfo info) {
        if (info == null) return;
        if (info.getChildCount() == 0) {
            list_nodes.add(info);
        } else {
            list_nodes.add(info);
            for (int i = 0; i < info.getChildCount(); i++) {
                refreshListOfNodes(list_nodes, info.getChild(i));
            }
        }
    }

    public static AccessibilityNodeInfo findItemByText(AccessibilityNodeInfo root,
                                                       String text) {
        ArrayList<AccessibilityNodeInfo> list_nodes = new ArrayList<>();
        Utility.refreshListOfNodes(list_nodes, root);
        for (AccessibilityNodeInfo node : list_nodes) {
            if (node.getText() != null
                    && node.getText().toString().equals(text)
                    && node.isVisibleToUser()) {
                AccessibilityNodeInfo crt = node;
                while (crt != null){
                    if(crt.isClickable())
                        return crt;
                    crt = crt.getParent();
                }
            }
        }
        return null;
    }
    /*public static void act()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
        List<String> nodes =new ArrayList<>();
        nodes.add("android.widget.FrameLayout|0;android.widget.LinearLayout|0;android.widget.FrameLayout|0;android.widget.FrameLayout|0;android.widget.FrameLayout|1;android.widget.FrameLayout|0;android.view.ViewGroup|0;android.view.ViewGroup|0;android.widget.TextView");
        //nodes.add("android.widget.FrameLayout|0;android.widget.LinearLayout|0;android.widget.FrameLayout|0;android.widget.FrameLayout|0;android.widget.RelativeLayout|1;android.widget.LinearLayout|1;android.widget.LinearLayout");
        nodes.add("android.widget.FrameLayout|0;android.widget.LinearLayout|0;android.widget.FrameLayout|0;android.widget.FrameLayout|0;android.widget.RelativeLayout|1;android.widget.LinearLayout|1;android.widget.LinearLayout");
        nodes.add("android.widget.FrameLayout|0;android.widget.LinearLayout|0;android.widget.FrameLayout|0;android.widget.FrameLayout|0;android.widget.RelativeLayout|0;android.widget.FrameLayout|0;android.widget.FrameLayout|0;android.widget.LinearLayout|0;android.widget.RelativeLayout|0;android.widget.FrameLayout|0;android.view.ViewGroup|1;android.widget.FrameLayout|0;android.widget.RelativeLayout|0;android.widget.RelativeLayout|0;android.widget.FrameLayout|0;android.widget.FrameLayout|0;android.widget.ListView|4;android.view.ViewGroup");
        nodes.add("android.widget.FrameLayout|0;android.widget.LinearLayout|0;android.widget.FrameLayout|0;android.widget.RelativeLayout|0;android.widget.FrameLayout|0;android.widget.FrameLayout|0;android.widget.RelativeLayout|1;android.widget.ScrollView|0;android.widget.RelativeLayout|0;android.widget.LinearLayout|0;android.widget.RelativeLayout|0;android.widget.ListView|0;android.widget.LinearLayout|0;android.widget.LinearLayout|1;android.widget.ImageView");
        for(String nodestr:nodes) {
            AccessibilityNodeInfoRecord record = AccessibilityNodeInfoRecord.idToRecord.get(nodestr);
            if(record!=null) {
                click(record.nodeInfo);
                Thread.sleep(1000);
            }
        }                }catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
        }).start();
    }*/
    private static boolean  click(AccessibilityNodeInfo node)
    {
        if (node != null)
        {
            while (!node.isClickable())
            {
                node = node.getParent();
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        return false;
    }

    public static void doVibrate()
    {
        Vibrator vibrator = (Vibrator)service.getSystemService(service.VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }


    public static void setSharedPreferenceData(String Name, String dataStr) {
        if(service==null)
            return ;
        SharedPreferences sharedPref = service.getSharedPreferences("Data", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Name, dataStr);
        editor.apply();
    }

    public static String getSharedPreferenceData(String Name) {
        SharedPreferences sharedPref = service.getSharedPreferences("Data", MODE_PRIVATE);
        return sharedPref.getString(Name, null);
    }

    public static List<String> getLauncherPackageNames(Context context) {
        List<String> names = new ArrayList<String>();
        PackageManager manager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        // intent.addCategory("android.intent.category.LAUNCHER_APP");
        List<ResolveInfo> list = manager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        ResolveInfo defaultLauncher = manager.resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if(defaultLauncher == null){
            for (ResolveInfo info : list) {
                names.add(info.activityInfo.packageName);
            }
        } else {
            names.add(defaultLauncher.activityInfo.packageName);
        }
        return names;
    }

    public static String toValidFileName(String str){
        return str.replace("/", ":");
    }

    public static AccessibilityNodeInfoRecord getDynamicItemRootForNode(AccessibilityNodeInfoRecord node){
        if(node.blockRoot != null){
            return node.blockRoot;
        }
        // todo 需要支持更加细致的判断
        AccessibilityNodeInfoRecord crtNode = node;
        while (crtNode != null && crtNode.parent != null){
            if(crtNode.parent.isDynamicEntrance){
                Rect crtRect = new Rect();
                Rect parentRect = new Rect();
                crtNode.getBoundsInScreen(crtRect);
                crtNode.parent.getBoundsInScreen(parentRect);
                if(Objects.equals(crtRect, parentRect)){
                    return null;
                } else {
                    return crtNode;
                }
            }
            crtNode = crtNode.parent;
        }

        return null;
    }

    public static boolean isCSEqual(CharSequence cs1, CharSequence cs2){
        if(isEmptyCS(cs1) && isEmptyCS(cs2)){
            return true;
        }
        return Objects.equals(String.valueOf(cs1), String.valueOf(cs2));
    }

    public static boolean isEmptyCS(CharSequence cs){
        return cs == null || cs.length() == 0;
    }

    public static void assertTrue(boolean cond){
        if (isDebug && !cond) {
            throw new AssertionError();
        }
    }

    public static Rect getMergeBound(Rect bound, AccessibilityNodeInfoRecord node) {
        assert(node != null);
        Rect res = new Rect();
        Rect nodeBound = new Rect();
        node.getBoundsInScreen(nodeBound);
        res.left = min(bound.left, nodeBound.left);
        res.right = max(bound.right, nodeBound.right);
        res.top = min(bound.top, nodeBound.top);
        res.bottom = max(bound.bottom, nodeBound.bottom);
        if (res.left < 0) res.left = 0;
        if (res.top < 0) res.top = 0;
        if (res.right < res.left) res.right = res.left;
        if (res.bottom < res.top) res.bottom = res.top;
        return res;
    }

    public static Set<AccessibilityNodeInfoRecord> getOverlapNodes(AccessibilityNodeInfoRecord crtNode, AccessibilityNodeInfoRecord node) {
        Rect crtBound = new Rect();
        crtNode.getBoundsInScreen(crtBound);
        Rect nodeBound = new Rect();
        node.getBoundsInScreen(nodeBound);
        int left = max(crtBound.left, nodeBound.left);
        int right = min(crtBound.right, nodeBound.right);
        int top = max(crtBound.top, nodeBound.top);
        int bottom = min(crtBound.bottom, nodeBound.bottom);
        double area = 1.0*(bottom-top)*(right-left);
        double ratio = area/(double)(nodeBound.width()*nodeBound.height());
        Set<AccessibilityNodeInfoRecord> res = new HashSet<>();
        if (ratio > 0.1) res.add(crtNode);
        else return res;
        for (AccessibilityNodeInfoRecord child : crtNode.children) res.addAll(getOverlapNodes(child, node));
        return res;
    }



    public static abstract class Visitor implements Serializable {
        public abstract void init();
        public abstract void visitNode(AccessibilityNodeInfoRecord record);
        public abstract boolean childrenNotAddIf(AccessibilityNodeInfoRecord record);
        public static void visit(AccessibilityNodeInfoRecord root, Visitor v){
            if(v == null){
                return;
            }
            v.init();
            visitOne(root, v);
        }

        private static void visitOne(AccessibilityNodeInfoRecord root, Visitor v){
            if(root == null || v == null){
                return;
            }
            v.visitNode(root);
            if(v.childrenNotAddIf(root)){
                return;
            }
            for(AccessibilityNodeInfoRecord child: root.getChildren()){
                visitOne(child, v);
            }
        }
    }

    public static boolean isSubTreeSame(AccessibilityNodeInfoRecord r1,
                                        AccessibilityNodeInfoRecord r2,
                                        boolean textDiffAllowed,
                                        boolean interactionDiffAllowed,
                                        boolean childrenPostfixDiffAllowed,
                                        StopInCompNodes stop){
        return isSubTreeSame(r1, r2, textDiffAllowed, interactionDiffAllowed, childrenPostfixDiffAllowed, false, stop);
    }

    public static boolean isSubTreeSame(AccessibilityNodeInfoRecord r1,
                                        AccessibilityNodeInfoRecord r2,
                                        boolean textDiffAllowed,
                                        boolean interactionDiffAllowed,
                                        boolean childrenPostfixDiffAllowed,
                                        boolean activityNameDiffAllowed,
                                        StopInCompNodes stop){
        if(!isNodeSame( r1, r2, textDiffAllowed, interactionDiffAllowed, activityNameDiffAllowed, stop)){
            return false;
        }

        if(stop != null && stop.willStop(r1, r2)){
            return true;
        }

        if(!childrenPostfixDiffAllowed){
            if(r1.getChildCount() != r2.getChildCount()){
                return false;
            }
        }

        for(int i = 0; i < Math.min(r1.getChildCount(), r2.getChildCount()); ++ i){
            AccessibilityNodeInfoRecord c1 = r1.getChild(i);
            AccessibilityNodeInfoRecord c2 = r2.getChild(i);
            if(!isSubTreeSame(c1, c2,
                    textDiffAllowed,
                    interactionDiffAllowed,
                    childrenPostfixDiffAllowed,
                    activityNameDiffAllowed,
                    stop)){
                return false;
            }
        }

        return true;
    }

    public interface StopInCompNodes {
        boolean willStop(AccessibilityNodeInfoRecord n1, AccessibilityNodeInfoRecord n2);
    }

    public static boolean isNodeSame(AccessibilityNodeInfoRecord r1,
                                     AccessibilityNodeInfoRecord r2,
                                     boolean textDiffAllowed,
                                     boolean interactionDiffAllowed,
                                     StopInCompNodes stop){
        return isNodeSame(r1, r2, textDiffAllowed, interactionDiffAllowed, false, stop);
    }

    public static boolean isNodeSame(AccessibilityNodeInfoRecord r1,
                                     AccessibilityNodeInfoRecord r2,
                                     boolean textDiffAllowed,
                                     boolean interactionDiffAllowed,
                                     boolean activityDiffAllowed,
                                     StopInCompNodes stop){
        if(r1 == null || r2 == null){
            return r1 == r2;
        }
        String nodeClass1 = String.valueOf(r1.getClassName());
        String nodeClass2 = String.valueOf(r2.getClassName());
        if(!Objects.equals(nodeClass1, nodeClass2)){
            return false;
        }

        String resourceId1 = String.valueOf(r1.getViewIdResourceName());
        String resourceId2 = String.valueOf(r2.getViewIdResourceName());
        if(!Utility.isCSEqualOrEmpty(resourceId1, resourceId2)){
            return false;
        }

        if(!activityDiffAllowed && !isCSEqualOrEmpty(r1.getActivityName(), r2.getActivityName())){
            return false;
        }

        if(r1.isEnabled() != r2.isEnabled()){
            return false;
        }

        if(stop != null && stop.willStop(r1, r2)){
            return true;
        }

        if(!interactionDiffAllowed){
            if(r1.isClickable() != r2.isClickable()){
                return false;
            }
            if(r1.isLongClickable() != r2.isLongClickable()){
                return false;
            }
            if(r1.isEditable() != r2.isEditable()){
                return false;
            }
            if(r1.isCheckable() != r2.isCheckable()){
                return false;
            }
        }

        if(!textDiffAllowed){
            String text1 = String.valueOf(r1.getText());
            String text2 = String.valueOf(r2.getText());
            if(!Objects.equals(text1, text2)){
                return false;
            }
            String content1 = String.valueOf(r1.getContentDescription());
            String content2 = String.valueOf(r2.getContentDescription());
            if(!Objects.equals(content1, content2)){
                return false;
            }
        }
        return true;
    }

    public static boolean isCSEqualOrEmpty(CharSequence cs1, CharSequence cs2){
        if(Utility.isEmptyCS(cs1) || Utility.isEmptyCS(cs2)){
            return true;
        }
        if(isEmptyCS(cs1) && isEmptyCS(cs2)){
            return true;
        }
        return Objects.equals(String.valueOf(cs1), String.valueOf(cs2));
    }

    public static AccessibilityNodeInfoRecord getFirstAncestorSupport(AccessibilityNodeInfoRecord node,
                                                                      AccessibilityNodeInfoRecordFromFile.Action.Type type){
        return getFirstAncestorSupport(node, type, null);
    }

    public static AccessibilityNodeInfoRecord getFirstAncestorSupport(AccessibilityNodeInfoRecord node,
                                                                      AccessibilityNodeInfoRecordFromFile.Action.Type type, AccessibilityNodeInfoRecord upTo){
        if(type == null){
            return node;
        }
        AccessibilityNodeInfoRecord crt = node;
        while (crt != null){
            if(canNodeSupport(crt, type)){
                return crt;
            }
            if(crt == upTo){
                break;
            }
            crt = crt.parent;
        }
        return null;
    }

    public static boolean canNodeSupport(AccessibilityNodeInfoRecord node, AccessibilityNodeInfoRecordFromFile.Action.Type type){
        if(type == AccessibilityNodeInfoRecordFromFile.Action.Type.TYPE_VIEW_CLICKED){
            return node.isClickable();
        }
        if(type == AccessibilityNodeInfoRecordFromFile.Action.Type.TYPE_VIEW_LONG_CLICKED){
            return node.isLongClickable();
        }
        if(type == AccessibilityNodeInfoRecordFromFile.Action.Type.TYPE_VIEW_TEXT_CHANGED){
            return node.isEditable();
        }
        if(type == AccessibilityNodeInfoRecordFromFile.Action.Type.TYPE_VIEW_SCROLLED){
            return node.isScrollable();
        }

        return true;
    }

    public static List<AccessibilityNodeInfoRecord> getNodesSupportingInteractionInSubTreeButNotAncestorOfGivenNodes(
            AccessibilityNodeInfoRecord root, Set<AccessibilityNodeInfoRecord> nodes,
            AccessibilityNodeInfoRecordFromFile.Action.Type needSupport
    ){
        Set<AccessibilityNodeInfoRecord> stopNodes = new HashSet<>();
        Queue<AccessibilityNodeInfoRecord> nodeQueue = new LinkedList<>(nodes);
        while (!nodeQueue.isEmpty()){
            AccessibilityNodeInfoRecord crt = nodeQueue.poll();
            if(crt == null || stopNodes.contains(crt)){
                continue;
            }
            stopNodes.add(crt);
            if(crt != root){
                nodeQueue.add(crt.parent);
            }
        }

        List<AccessibilityNodeInfoRecord> result = new ArrayList<>();
        nodeQueue = new LinkedList<>();
        nodeQueue.add(root);
        while (!nodeQueue.isEmpty()){
            AccessibilityNodeInfoRecord crt = nodeQueue.poll();
            if(crt == null){
                continue;
            }
            if(!stopNodes.contains(crt)){
                if(canNodeSupport(crt, needSupport)){
                    result.add(crt);
                }
            }
            nodeQueue.addAll(crt.children);
        }

        return result;
    }

    public static List<AccessibilityNodeInfoRecord> findNodesByText(AccessibilityNodeInfoRecord node, String keyword){
        List<AccessibilityNodeInfoRecord> result = new ArrayList<>();
        Queue<AccessibilityNodeInfoRecord> nodeQueue = new LinkedList<>();
        nodeQueue.add(node);
        while (!nodeQueue.isEmpty()){
            AccessibilityNodeInfoRecord crt = nodeQueue.poll();
            if(crt == null){
                continue;
            }
            String text = String.valueOf(crt.getText());
            String content = String.valueOf(crt.getContentDescription());
            if(Objects.equals(text, keyword) || Objects.equals(content, keyword)){ //
                result.add(crt);
            }
            nodeQueue.addAll(crt.children);
        }

        return result;
    }

    public static List<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> refreshBlockNodeInfo(AccessibilityNodeInfoRecord root){
        //Utility.assertTrue(root.parent == null);

        List<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> nodeBlockPairs = new ArrayList<>();

        Queue<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> nodeAndBlockRootQueue = new LinkedList<>();
        nodeAndBlockRootQueue.add(new Pair<>(root, root));
        while (!nodeAndBlockRootQueue.isEmpty()){
            Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord> crtPair = nodeAndBlockRootQueue.poll();
            if(crtPair == null){
                continue;
            }
            crtPair.first.blockRoot = crtPair.second;
            nodeBlockPairs.add(crtPair);

            if(crtPair.first.isDynamicEntrance){ // todo 使用更加准确的方案进行判断
                // 所有子节点的block root 就是他们自己
                for(AccessibilityNodeInfoRecord c: crtPair.first.children){
                    nodeAndBlockRootQueue.add(new Pair<>(c, c));
                }
            } else {
                // 所有子节点的block root 还是原来的节点
                for(AccessibilityNodeInfoRecord c: crtPair.first.children){
                    nodeAndBlockRootQueue.add(new Pair<>(c, crtPair.second));
                }
            }
        }

        return nodeBlockPairs;
    }

    public static List<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> refreshBlockNodeInfo(AccessibilityNodeInfoRecord root, Set<AccessibilityNodeInfoRecord> endNodes){
        //Utility.assertTrue(root.parent == null);

        List<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> nodeBlockPairs = new ArrayList<>();

        Queue<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> nodeAndBlockRootQueue = new LinkedList<>();
        nodeAndBlockRootQueue.add(new Pair<>(root, root));
        while (!nodeAndBlockRootQueue.isEmpty()){
            Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord> crtPair = nodeAndBlockRootQueue.poll();
            if(crtPair == null){
                continue;
            }
            if (endNodes.contains(crtPair.first)) continue;
            crtPair.first.blockRoot = crtPair.second;
            nodeBlockPairs.add(crtPair);

            if(crtPair.first.isDynamicEntrance){ // todo 使用更加准确的方案进行判断
                // 所有子节点的block root 就是他们自己
                for(AccessibilityNodeInfoRecord c: crtPair.first.children){
                    nodeAndBlockRootQueue.add(new Pair<>(c, c));
                }
            } else {
                // 所有子节点的block root 还是原来的节点
                for(AccessibilityNodeInfoRecord c: crtPair.first.children){
                    nodeAndBlockRootQueue.add(new Pair<>(c, crtPair.second));
                }
            }
        }

        return nodeBlockPairs;
    }


    public static boolean isTextBackwards(String text){
        if(text == null){
            return false;
        }
        for(String backKey: backwardsKeywords){
            if(text.startsWith(backKey)){
                return true;
            }
        }
        return false;
    }

    public static final int MAX_LENGTH = 13;

    public static Pair<String, Boolean> isTextFunc(String text, boolean isInStatic){
        if(text == null){
            return new Pair<>("", false);
        }

        int chineseLength = countChineseCharLengthInString(text);
        if(chineseLength <= 0 || chineseLength > MAX_LENGTH){
            return new Pair<>("", false);
        }
        return new Pair<>(text, true);
    }

    public static Pair<String, Boolean> isNodeFunction(AccessibilityNodeInfoRecord node, boolean usingText){
        CharSequence textCS = node.getText();
        if(!usingText){
            textCS = node.getContentDescription();
        }
        if(textCS == null || textCS.length() == 0){
            return new Pair<>("", false);
        }
        String text = String.valueOf(textCS);
        return isTextFunc(text, node.isInStaticRegion);
    }

    public static int countChineseCharLengthInString(String text){
        Pattern pattern = Pattern.compile("[ -~]");
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()){
            count += 1;
        }

        return text.length() - count;
    }

    public static double countTextLength(String text){
        try {
            Double.parseDouble(text);
            return 0;
        } catch (NumberFormatException ignore){}

        // 每三个英文字符、数字算一个汉字
        Pattern pattern = Pattern.compile("[ -~]");
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()){
            count += 1;
        }
        if(text.length() - count == 0 && text.length() <= 3){
            return 0;
        }
        if(count == 0 && !containsValidHan(text)){ // 单个无意义汉字
            return 0;
        }

        return text.length() - count + (count / 3.0);
    }

    public static boolean containsValidHan(String str) {
        String regex = "[\u4e00-\u9fa5]";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);

        return matcher.find();
    }

    public static double getCommonLen(String text1, String text2) {
        int len = 0;
        int i = 0;
        int maxLen = min(text1.length(),text2.length());
        for (; i < maxLen; i++)
            if (text1.charAt(i) == text2.charAt(i)) len++;
            else break;
        for (int j = maxLen-1; j>i; j--)
            if (text1.charAt(j) == text2.charAt(j)) len++;
            else break;
        return (double)len*1.0/text1.length();
    }


}

