package com.abh80.smartedge;

import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

//import com.mongodb.DBObject;

public class AccessibilityNodeInfoRecordFromFile extends AccessibilityNodeInfoRecord implements Serializable {
    public static class Action implements Serializable{
        public static final String TAG = "Action";
        public enum Type implements Serializable {
            TYPE_VIEW_CLICKED, TYPE_VIEW_LONG_CLICKED, TYPE_VIEW_SCROLLED, TYPE_VIEW_TEXT_CHANGED,
            TYPE_UNKNOWN, TYPE_GLOBAL_BACK, TYPE_GLOBAL_HOME, TYPE_GLOBAL_RECENT_APPS, TYPE_NOTIFICATION_EXPANDED,
            TYPE_END
        }
        public static Set<String> backwardsKeywords;
        static {
            backwardsKeywords = new HashSet<>();
            backwardsKeywords.add("返回");
            backwardsKeywords.add("取消");
            backwardsKeywords.add("上一步");
            backwardsKeywords.add("向上导航");
            backwardsKeywords.add("关闭");
            backwardsKeywords.add("退出");
            backwardsKeywords.add("结束");
            backwardsKeywords.add("放弃");
        }

        public String hintText;
        public String para;
        public boolean isSpeculation;
        public boolean isSystemAction;
        public Type type;
        public String nodeId;
        public AccessibilityNodeInfoRecordFromFile node;
        private Action(){}
        public static Action obtainByJSON(JSONObject obj, AccessibilityNodeInfoRecordFromFile root){
            try {
                Action res = new Action();
                res.isSpeculation = Objects.equals("speculation", obj.getString("hint"));
                if(!res.isSpeculation){
                    res.hintText = obj.getString("hint");
                }
                res.isSystemAction = Objects.equals("system", obj.getString("hint"))
                        || Objects.equals("通知栏", obj.getString("param"))
                        || Objects.equals("back", obj.getString("param"));
                if(res.isSystemAction){
                    String actionId = "null";
                    if(obj.has("targetNodeId")){
                        actionId = obj.getString("targetNodeId");
                    }
                    switch (actionId) {
                        case "back":
                            res.type = Type.TYPE_GLOBAL_BACK;
                            break;
                        case "home":
                            res.type = Type.TYPE_GLOBAL_HOME;
                            break;
                        case "recentsapps":
                            res.type = Type.TYPE_GLOBAL_RECENT_APPS;
                            break;
                        case "通知栏":
                            res.type = Type.TYPE_NOTIFICATION_EXPANDED;
                            break;
                        default:
                            res.type = Type.TYPE_UNKNOWN;
                            break;
                    }
                } else {
                    int actionType = obj.getInt("type");
                    switch (actionType){
                        case AccessibilityEvent.TYPE_VIEW_CLICKED:
                            res.type = Type.TYPE_VIEW_CLICKED;
                            break;
                        case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                            res.type = Type.TYPE_VIEW_LONG_CLICKED;
                            break;
                        case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                            res.type = Type.TYPE_VIEW_SCROLLED;
                            break;
                        case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                            res.type = Type.TYPE_VIEW_TEXT_CHANGED;
                            break;
                        default:
                            res.type = Type.TYPE_UNKNOWN;
                    }
                    if(!obj.has("targetNodeId")){
                        res.nodeId = null;
                    } else {
                        res.nodeId = obj.getString("targetNodeId");
                        if(Objects.equals(String.valueOf(root.getClassName()), "fake.root")){
                            for(int i = 0; i < root.getChildCount(); ++ i){
                                res.node = (AccessibilityNodeInfoRecordFromFile) root.getNodeByOriAbsoluteId(root.getClassName() + "|" + i + ';' + res.nodeId);
                                if(res.node != null){
                                    break;
                                }
                            }
                        } else {
                            res.node = (AccessibilityNodeInfoRecordFromFile) root.getNodeByOriAbsoluteId(res.nodeId);
                        }
                        if(res.node == null){
                            Log.w(TAG, "obtainByJSON: node not found");
                        }
                    }

                    if(res.type == Type.TYPE_VIEW_TEXT_CHANGED){
                        res.para = obj.getString("param");
                    }
                }
                return res;
            } catch (JSONException e){
                e.printStackTrace();
                return null;
            }
        }
        public static Action obtainByJSONTBPVersion(JSONObject obj, AccessibilityNodeInfoRecordFromFile root) throws JSONException {
            Action res = new Action();
            res.isSpeculation = false;
            String eventType = obj.getString("eventType");
            if(Objects.equals(eventType, "VIEW_CLICK")){
                res.type = Type.TYPE_VIEW_CLICKED;
                res.node = (AccessibilityNodeInfoRecordFromFile) root.getNodeByOriAbsoluteId(obj.getString("path"));
            } else if(Objects.equals(eventType, "VIEW_LONG_CLICK")){
                res.type = Type.TYPE_VIEW_LONG_CLICKED;
                res.node = (AccessibilityNodeInfoRecordFromFile) root.getNodeByOriAbsoluteId(obj.getString("path"));
            } else if(Objects.equals(eventType, "VIEW_SCROLL")){
                res.type = Type.TYPE_VIEW_SCROLLED;
                if(obj.has("direction")){
                    res.para = String.valueOf(obj.get("direction"));
                }
                res.node = (AccessibilityNodeInfoRecordFromFile) root.getNodeByOriAbsoluteId(obj.getString("path"));
            } else if(Objects.equals(eventType, "VIEW_TEXT_CHANGED")){
                res.type = Type.TYPE_VIEW_TEXT_CHANGED;
                if(obj.has("text")){
                    res.para = obj.getString("text");
                }
                res.node = (AccessibilityNodeInfoRecordFromFile) root.getNodeByOriAbsoluteId(obj.getString("path"));
            } else if(Objects.equals(eventType, "BACK")){
                res.isSystemAction = true;
                res.type = Type.TYPE_GLOBAL_BACK;
            } else if(Objects.equals(eventType, "HOME")){
                res.isSystemAction = true;
                res.type = Type.TYPE_GLOBAL_HOME;
            } else if(Objects.equals(eventType, "SUMMARY")){
                res.isSystemAction = true;
                res.type = Type.TYPE_GLOBAL_RECENT_APPS;
            } else if(Objects.equals(eventType, "NOTIFICATIONS")){
                res.isSystemAction = true;
                res.type = Type.TYPE_NOTIFICATION_EXPANDED;
            }
            if(!res.isSystemAction && res.node == null){
                if(Utility.isDebug){
                    root.getNodeByOriAbsoluteId(obj.getString("path"));
                }
                Log.w(TAG, "obtainByJSONTBPVersion: node not found");
            }

            return res;
        }

        /*public static Action obtainByDB(DBObject step, AccessibilityNodeInfoRecord root){
            Action res = new Action();
            res.isSpeculation = false;
            res.hintText = (String) step.get("display_hint");
            switch ((String) step.get("op_type")){
                case "TYPE_GLOBAL_BACK":
                    res.type = Type.TYPE_GLOBAL_BACK;
                    res.isSystemAction = true;
                    break;
                case "TYPE_GLOBAL_HOME":
                    res.type = Type.TYPE_GLOBAL_HOME;
                    res.isSystemAction = true;
                    break;
                case "TYPE_GLOBAL_RECENT_APPS":
                    res.type = Type.TYPE_GLOBAL_RECENT_APPS;
                    res.isSystemAction = true;
                    break;
                case "TYPE_NOTIFICATION_EXPANDED":
                    res.type = Type.TYPE_NOTIFICATION_EXPANDED;
                    res.isSystemAction = true;
                    break;
                case "TYPE_VIEW_CLICKED":
                    res.type = Type.TYPE_VIEW_CLICKED;
                    res.isSystemAction = false;
                    break;
                case "TYPE_VIEW_LONG_CLICKED":
                    res.type = Type.TYPE_VIEW_LONG_CLICKED;
                    res.isSystemAction = false;
                    break;
                case "TYPE_VIEW_SCROLLED":
                    res.type = Type.TYPE_VIEW_SCROLLED;
                    res.isSystemAction = false;
                    break;
                case "TYPE_VIEW_TEXT_CHANGED":
                    res.type = Type.TYPE_VIEW_TEXT_CHANGED;
                    res.isSystemAction = false;
                    res.para = (String) step.get("op_para");
                    break;
                case "TYPE_END":
                    res.type = Type.TYPE_END;
                    res.isSystemAction = true;
                    break;
                default:
                    res.type = Type.TYPE_UNKNOWN;
                    res.isSystemAction = true;
                    break;
            }
            res.nodeId = (String) step.get("op_element");
            if(!res.isSystemAction){
                res.node = (AccessibilityNodeInfoRecordFromFile) root.getNodeByOriAbsoluteId(res.nodeId);
                if(res.node == null){
                    Log.w(TAG, "obtainByDB: node not found");
                }
            }
            return res;
        }*/
        public boolean isBackwards(){
            if(type == Type.TYPE_GLOBAL_BACK){
                return true;
            }
            if(type == Type.TYPE_VIEW_CLICKED && node != null){
                Queue<AccessibilityNodeInfoRecord> nodeQueue = new LinkedList<>();
                nodeQueue.add(node);
                while (!nodeQueue.isEmpty()){
                    AccessibilityNodeInfoRecord crtNode = nodeQueue.poll();
                    if(crtNode == null){
                        continue;
                    }
                    if(Utility.isTextBackwards(String.valueOf(crtNode.getText()))
                            || Utility.isTextBackwards(String.valueOf(crtNode.getContentDescription()))){
                        return true;
                    }
                    nodeQueue.addAll(crtNode.children);
                }
            }
            return false;
        }

        public boolean isGlobalBack(){
            return type == Type.TYPE_GLOBAL_BACK;
        }
    }
    public static AccessibilityNodeInfoRecordFromFile buildTreeFromFile(String jsonFilePath) throws IOException, JSONException {
        return buildTreeFromFile(jsonFilePath, false);
    }


    public static AccessibilityNodeInfoRecordFromFile buildTreeFromFile(String jsonFilePath, boolean tbpVersion) throws IOException, JSONException {
        InputStreamReader inputStreamReader = new InputStreamReader(
                new FileInputStream(jsonFilePath));
        BufferedReader reader = new BufferedReader(inputStreamReader, 5 * 1024);
        char[] buffer = new char[5 * 1024];
        int length;
        StringBuilder builder = new StringBuilder();

        while ((length = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, length);
        }

        reader.close();
        inputStreamReader.close();

        return buildTreeFromFileContent(builder.toString(), tbpVersion, jsonFilePath);
    }
    public static AccessibilityNodeInfoRecordFromFile buildTreeFromFileContent(String builder, boolean tbpVersion, String jsonFilePath) throws JSONException {
        JSONObject wholeTree;
        try {
            wholeTree = new JSONObject(builder);
        } catch (JSONException e){
            JSONArray roots = new JSONArray(builder);
            JSONArray rootsNotSystem = new JSONArray();
            for(int i = 0; i < roots.length(); ++ i){
                if(roots.getJSONObject(i).getInt("@window-type") == AccessibilityWindowInfo.TYPE_INPUT_METHOD
                        || roots.getJSONObject(i).getInt("@window-type") == AccessibilityWindowInfo.TYPE_SYSTEM){
                    continue;
                }

                String rootPackage = roots.getJSONObject(i).getString("@package");
                if(Objects.equals(rootPackage, "com.android.systemui")
                        || Objects.equals(rootPackage, "android") || Objects.equals(rootPackage, "com.abh80.smartedge")){
                    continue;
                }
                rootsNotSystem.put(roots.getJSONObject(i));
            }



            wholeTree = new JSONObject();
            wholeTree.put("@index", 0);
            wholeTree.put("@class", "fake.root");
            wholeTree.put("@checkable", false);
            wholeTree.put("@checked", false);
            wholeTree.put("@clickable", false);
            wholeTree.put("@enabled", true);
            wholeTree.put("@focusable", false);
            wholeTree.put("@focused", false);
            wholeTree.put("@scrollable", false);
            wholeTree.put("@long-clickable", false);
            wholeTree.put("@password", false);
            wholeTree.put("@selected", false);
            wholeTree.put("@editable", false);
            wholeTree.put("@accessibilityFocused", false);
            wholeTree.put("@dismissable", false);
            if(rootsNotSystem.length() == 0){
                wholeTree.put("@package", "UNKNOWN");
                wholeTree.put("@bounds", "[0,0][0,0]");
                wholeTree.put("@screenBounds", "[0,0][0,0]");
            } else {
                wholeTree.put("@package", rootsNotSystem.getJSONObject(0).getString("@package"));
                wholeTree.put("@bounds", rootsNotSystem.getJSONObject(0).getString("@bounds"));
                wholeTree.put("@screenBounds", rootsNotSystem.getJSONObject(0).getString("@screenBounds"));
            }

            wholeTree.put("node", rootsNotSystem);
        }

        AccessibilityNodeInfoRecordFromFile result;
        if(!tbpVersion){
            result = buildSubTreeFromJsonObject(null, 0, wholeTree, jsonFilePath);
        } else {
            result = buildSubTreeFromJsonObjectInTBPVersion(null, 0, wholeTree, jsonFilePath);
        }

        if(Objects.equals(String.valueOf(result.getClassName()), "fake.root")){
            for(int i = result.getChildCount() - 1; i >= 0; -- i){
                AccessibilityNodeInfoRecord nodeMayBeDeleted = result.getChild(i);
                for(int j = i - 1; j >= 0; -- j){
                    AccessibilityNodeInfoRecord nodeToBeCompared = result.getChild(j);
                    if(Utility.isSubTreeSame(nodeMayBeDeleted, nodeToBeCompared, false, false, false, null)){
                        result.children.remove(i);
                        break;
                    }
                }
            }
        }
        removeInvisibleChildrenInList(result);
        result.refreshAbsoluteOriId();
        result.ignoreUselessChild(false);
        // removeSiblingsCoveredUseDrawingOrder(result);
        refreshIsInStaticRegion(result);
        result.refreshIndex(0);
        result.refreshAbsoluteId();
        return result;
    }

    public static void removeRedundantNodes(AccessibilityNodeInfoRecordFromFile root){
        removeInvisibleChildrenInList(root);
        root.refreshAbsoluteOriId();
        root.ignoreUselessChild(false);
        refreshIsInStaticRegion(root);
        root.refreshIndex(0);
        root.refreshAbsoluteId();
    }


    public static AccessibilityNodeInfoRecordFromFile buildSubTreeFromJsonObject(
            AccessibilityNodeInfoRecordFromFile parent, int index, JSONObject uiNodeJsonObject, String jsonFilePath) throws JSONException {
        AccessibilityNodeInfoRecordFromFile crtNode = new AccessibilityNodeInfoRecordFromFile(parent, index);

        crtNode._isClickable = uiNodeJsonObject.getBoolean("@clickable");
        crtNode._isScrollable = uiNodeJsonObject.getBoolean("@scrollable");
        crtNode._isLongClickable = uiNodeJsonObject.getBoolean("@long-clickable");
        crtNode._isEditable = uiNodeJsonObject.getBoolean("@editable");
        crtNode._isCheckable = uiNodeJsonObject.getBoolean("@checkable");
        crtNode._isChecked = uiNodeJsonObject.getBoolean("@checked");
        crtNode._isEnabled = uiNodeJsonObject.getBoolean("@enabled");
        if (uiNodeJsonObject.has("@drawingOrder")){
            crtNode._drawingOrder = uiNodeJsonObject.getInt("@drawingOrder");
        } else {
            crtNode._drawingOrder = 0;
        }

        if(uiNodeJsonObject.has("@hint-text")){
            crtNode._hintText = uiNodeJsonObject.getString("@hint-text");
        } else {
            crtNode._hintText = "";
        }

        if (uiNodeJsonObject.has("@text"))
            crtNode._text = uiNodeJsonObject.getString("@text");
        if(uiNodeJsonObject.has("@content-desc"))
            crtNode._contentDescription = uiNodeJsonObject.getString("@content-desc");

        String boundStr = uiNodeJsonObject.getString("@bounds");
        boundStr = boundStr.substring(1, boundStr.length() - 1);
        String[] splitStr = boundStr.split("]\\[");
        String[] split0 = splitStr[0].split(",");
        crtNode.left = Integer.parseInt(split0[0]);
        crtNode.top = Integer.parseInt(split0[1]);
        String[] split1 = splitStr[1].split(",");
        crtNode.right = Integer.parseInt(split1[0]);
        crtNode.bottom = Integer.parseInt(split1[1]);

        if(uiNodeJsonObject.has("@class")){
            crtNode._className = uiNodeJsonObject.getString("@class");
        } else {
            crtNode._className = "unknown.class";
            if(!uiNodeJsonObject.getString("@bounds").equals("[0,0][0,0]")){
                Log.w(TAG, "buildSubTreeFromJsonObject: no class name for visible node");
            }
        }

        if(uiNodeJsonObject.has("@index")){
            crtNode.index = uiNodeJsonObject.getInt("@index");
        }

        crtNode._isSelected = uiNodeJsonObject.getBoolean("@selected");
        crtNode._packageName = uiNodeJsonObject.getString("@package");
        if(uiNodeJsonObject.has("@resource-id"))
            crtNode._viewIdResourceName = uiNodeJsonObject.getString("@resource-id");
        Rect r = new Rect(crtNode.left, crtNode.top, crtNode.right, crtNode.bottom);
        crtNode._isVisibleToUser = r.width() >= 0 && r.height() >= 0;
        crtNode._isFocusable = uiNodeJsonObject.getBoolean("@focusable");
        crtNode._isFocused = uiNodeJsonObject.getBoolean("@focused");
        if (uiNodeJsonObject.has("dismissable")) crtNode._isDismissable = uiNodeJsonObject.getBoolean("@dismissable");
        crtNode.filePath = jsonFilePath;
        if(parent == null && uiNodeJsonObject.has("@activity-name")){
            crtNode.activityName = uiNodeJsonObject.getString("@activity-name");
            String activityName = String.valueOf(crtNode.activityName);
            String packageName = String.valueOf(crtNode._packageName);
            if(!activityName.startsWith(packageName)){
                crtNode.activityName = null;
            }
            crtNode.rawActivityName = crtNode.activityName;
        }
        
        if(uiNodeJsonObject.has("node")) {
            Object node = uiNodeJsonObject.get("node");
            if (node instanceof JSONArray) {
                JSONArray nodeArray = (JSONArray) node;
                for (int i = 0; i < nodeArray.length(); ++i) {
                    if(nodeArray.getJSONObject(i).length() > 0)
                        crtNode.children.add(AccessibilityNodeInfoRecordFromFile.buildSubTreeFromJsonObject(crtNode, i, nodeArray.getJSONObject(i), jsonFilePath));
                }
            } else if (node instanceof JSONObject) {
                crtNode.children.add(AccessibilityNodeInfoRecordFromFile.buildSubTreeFromJsonObject(crtNode, 0, (JSONObject) node, jsonFilePath));
            } else {
                Utility.assertTrue(false);
            }
        }

        return crtNode;
    }

    public static AccessibilityNodeInfoRecordFromFile buildSubTreeFromJsonObjectInTBPVersion(
            AccessibilityNodeInfoRecordFromFile parent, int index, JSONObject uiNodeJsonObject, String jsonFilePath
    ) throws JSONException {
        AccessibilityNodeInfoRecordFromFile crtNode = new AccessibilityNodeInfoRecordFromFile(parent, index);
        crtNode._isClickable = uiNodeJsonObject.getBoolean("clickable");
        crtNode._isScrollable = uiNodeJsonObject.getBoolean("scrollable");
        crtNode._isLongClickable = uiNodeJsonObject.getBoolean("long-clickable");
        crtNode._isEditable = uiNodeJsonObject.getBoolean("editable");
        crtNode._isCheckable = uiNodeJsonObject.getBoolean("checkable");
        crtNode._isEnabled = uiNodeJsonObject.getBoolean("enabled");
        crtNode._className = uiNodeJsonObject.getString("class");
        if(uiNodeJsonObject.has("text")){
            crtNode._text = uiNodeJsonObject.getString("text");
        }
        if(uiNodeJsonObject.has("content-desc")){
            crtNode._contentDescription = uiNodeJsonObject.getString("content-desc");
        }
        if(uiNodeJsonObject.has("index")){
            crtNode.index = uiNodeJsonObject.getInt("index");
        }

        JSONObject boundInfo = uiNodeJsonObject.getJSONObject("bounds");
        crtNode.left = boundInfo.getInt("left");
        crtNode.top = boundInfo.getInt("top");
        crtNode.right = boundInfo.getInt("right");
        crtNode.bottom = boundInfo.getInt("bottom");

        crtNode._isSelected = uiNodeJsonObject.getBoolean("selected");
        crtNode._packageName = uiNodeJsonObject.getString("package");
        if(uiNodeJsonObject.has("resource-id")){
            crtNode._viewIdResourceName = uiNodeJsonObject.getString("resource-id");
        }

        Rect r = new Rect(crtNode.left, crtNode.top, crtNode.right, crtNode.bottom);
        crtNode._isVisibleToUser = r.width() >= 0 && r.height() >= 0;
        crtNode._isFocusable = uiNodeJsonObject.getBoolean("focusable");
        crtNode._isDismissable = uiNodeJsonObject.getBoolean("dismissable");
        crtNode.filePath = jsonFilePath;
        // todo package name not set!!
        if(uiNodeJsonObject.has("children")){
            JSONArray childList = uiNodeJsonObject.getJSONArray("children");
            for(int i = 0; i < childList.length(); ++ i){
                crtNode.children.add(AccessibilityNodeInfoRecordFromFile.buildSubTreeFromJsonObjectInTBPVersion(
                        crtNode, i, childList.getJSONObject(i), jsonFilePath
                ));
            }
        }

        return crtNode;
    }


    // 从文件中载入 ui 树 用来对程序进行验证
    boolean _isClickable;
    boolean _isScrollable;
    boolean _isLongClickable;
    boolean _isEditable;
    boolean _isCheckable;
    boolean _isChecked;
    boolean _isEnabled;
    CharSequence _text;  // nullable
    CharSequence _contentDescription;  // nullable
    CharSequence _className;
    CharSequence _hintText;

    int _drawingOrder;

    int top;
    int left;
    int bottom;
    int right;

    boolean _isSelected;
    CharSequence _packageName;
    CharSequence _viewIdResourceName;  // nullable

    boolean _isVisibleToUser;
    boolean _isFocusable;
    boolean _isFocused;
    boolean _isDismissable;

    public String filePath;
    public CharSequence rawActivityName;

    AccessibilityNodeInfoRecordFromFile(AccessibilityNodeInfoRecordFromFile parent, int index) {
        super(null, parent, index);
    }

    public AccessibilityNodeInfoRecordFromFile(){
        _className = "fake.root";
        children = new ArrayList<>();
        uselessChildren = new ArrayList<>();
        _isEnabled = true;
    }

    @Override
    public boolean isClickable() {
        return _isClickable;
    }

    @Override
    public boolean isScrollable() {
        return _isScrollable;
    }

    @Override
    public boolean isLongClickable() {
        return _isLongClickable;
    }

    @Override
    public boolean isEditable() {
        return _isEditable;
    }

    @Override
    public boolean isEnabled(){
        return _isEnabled;
    }

    @Override
    public boolean isCheckable() {
        return _isCheckable;
    }

    @Override
    public boolean isChecked() { return _isChecked; }

    @Override
    public CharSequence getText() {
        return _text;
    }

    @Override
    public CharSequence getContentDescription() {
        return _contentDescription;
    }

    @Override
    public CharSequence getClassName() {
        return _className;
    }

    @Override
    public void getBoundsInScreen(Rect r) {
        r.left = left;
        r.right = right;
        r.top = top;
        r.bottom = bottom;
    }

    @Override
    public boolean isSelected() {
        return _isSelected;
    }

    @Override
    public CharSequence getPackageName() {
        return _packageName;
    }

    @Override
    public CharSequence getViewIdResourceName() {
        return _viewIdResourceName;
    }

    @Override
    public boolean isVisibleToUser() {
        return _isVisibleToUser;
    }

    @Override
    public boolean isFocusable() {
        return _isFocusable;
    }

    @Override
    public boolean isFocused() {
        return _isFocused;
    }

    @Override
    public boolean isDismissable() {
        return _isDismissable;
    }

    @Override
    public int getDrawingOrder() {
        return _drawingOrder;
    }

    @Override
    public CharSequence getHintText() {
        return _hintText;
    }
}
