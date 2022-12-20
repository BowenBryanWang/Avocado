package com.abh80.smartedge;

import android.graphics.Rect;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.abh80.smartedge.xdevice.SpecialNodeDescriber;

class Gesture {
    public final static List<String> gestureSet = Arrays.asList("单指上滑","单指下滑","单指左滑","单指右滑","双指上滑","双指下滑","双指左滑","双指右滑","单击","双击","单指长按","双指长按");
    public String gesture;
    public String funcDesc;
    Gesture(String gesture, String funcDesc) {
        this.gesture = gesture;
        this.funcDesc = funcDesc;
    }

    public static Gesture loadFromJson(JSONObject tmpObject, AccessibilityNodeInfoRecordFromFile uiRoot) throws JSONException {
        Gesture res = new Gesture(tmpObject.getString("gesture"), tmpObject.getString("func_desc"));
        return res;
    }

    JSONObject dumpToJson() {
        JSONObject res = new JSONObject();
        try {
            res.put("gesture", this.gesture);
            res.put("func_desc",this.funcDesc);
        } catch (Exception e) {
        e.printStackTrace();
        }
        return res;
    }
}

class ListItem {
    public String nodeId;
    public AccessibilityNodeInfoRecord node;
    public int level; //level == 0 为普通列表项， 其他为level级标题

    ListItem(String nodeId, int level) {
        this.nodeId = nodeId;
        this.node = null;
        this.level = level;
    }

    ListItem(AccessibilityNodeInfoRecord node, int level) {
        this.node = node;
        this.nodeId = node.absoluteId;
        this.level = level;
    }

    ListItem(String nodeId, int level, AccessibilityNodeInfoRecord uiRoot) {
        this.nodeId = nodeId;
        this.node = null;
        if (nodeId != null) this.node = uiRoot.getNodeByOriAbsoluteId(nodeId);
        this.level = level;
    }

    JSONObject dumpToJson() {
        JSONObject res = new JSONObject();
        try {
            res.put("node_id", this.nodeId);
            res.put("node_info",this.node.getNodeInfoJson(false));
            res.put("level", this.level);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static ListItem loadFromJson(JSONObject jsonFile, AccessibilityNodeInfoRecord uiRoot) throws JSONException {
        ListItem res = new ListItem(jsonFile.getString("node_id"),jsonFile.getInt("level"), uiRoot);
        return res;
    }
}

class FuncButton {
    public final static List<String> descSet = Arrays.asList("刷新","更多","删除");
    public String nodeId;
    public AccessibilityNodeInfoRecord node;
    public int descMethod;
    public String desc;
    private double dist;
    FuncButton(String nodeId) {
        this.nodeId = nodeId;
        this.node = null;
        this.descMethod = 0; //默认是看按钮内部的text
        this.desc = "";
        this.dist = 0;
    }

    FuncButton(String nodeId, int method, String desc, AccessibilityNodeInfoRecord uiRoot) {
        this.nodeId = nodeId;
        this.node = null;
        if (this.nodeId != null) this.node = uiRoot.getNodeByOriAbsoluteId(this.nodeId);
        this.descMethod = method; //默认是看按钮内部的text
        this.desc = desc;
        this.dist = 0;
    }

    public FuncButton(AccessibilityNodeInfoRecord node, FuncButton patternFuncButton) {
        this.nodeId = node.absoluteId;
        this.node = node;
        this.descMethod = patternFuncButton.descMethod;
        this.desc = patternFuncButton.desc;
        SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(patternFuncButton.node);
        this.dist = specialNodeDescriber.calNodeDistanceToDescriber(node);
    }

    public static FuncButton loadFromJson(JSONObject jsonFile, AccessibilityNodeInfoRecordFromFile uiRoot) throws JSONException {
        FuncButton res = new FuncButton(jsonFile.getString("node_id"), jsonFile.getInt("desc_method"),
                    jsonFile.getString("desc"), uiRoot);
        return res;
    }

    void addDesc(String desc) {
        this.desc = desc;
        this.descMethod = 1; //改成标注的按钮描述
    }

    JSONObject dumpToJson() {
        JSONObject res = new JSONObject();
        try {
            res.put("node_id", this.nodeId);
            res.put("node_info",this.node.getNodeInfoJson(false));
            res.put("desc_method", this.descMethod);
            res.put("desc",this.desc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public double getDist() {
        return this.dist;
    }
}

class InfoList {
    public final static List<String> titleSet = Arrays.asList("一级列表标题","二级列表标题","三级列表标题","四级列表标题");
    public String title;
    public String titleNodeId;
    public AccessibilityNodeInfoRecord titleNode;
    public List<ListItem> listItems;
    public List<FuncButton> funcButtons;
    public List<String> candidateTitleNodeIds;
    Rect bound;

    InfoList(List<String> items) {
        this.title = "";
        this.titleNodeId = null;
        this.titleNode = null;
        this.listItems = new ArrayList<>();
        for (String item : items) {
            this.listItems.add(new ListItem(item,0));
        }
        this.funcButtons = new ArrayList<>();
        this.candidateTitleNodeIds = new ArrayList<>();
        this.bound = new Rect(1<<30,1<<30,0,0);
    }

    public JSONObject dumpToJson() {
        JSONObject res = new JSONObject();
        try {
            res.put("title", this.title);
            res.put("title_node_id", this.titleNodeId);
            if (this.titleNode != null)
                res.put("title_node_info",this.titleNode.getNodeInfoJson(false));
            else
                res.put("title_node_info",new JSONObject());
            JSONArray listItemJson = new JSONArray();
            for (ListItem item : this.listItems) {
                listItemJson.put(item.dumpToJson());
            }
            res.put("list_items",listItemJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static InfoList loadFromJson(JSONObject jsonFile, AccessibilityNodeInfoRecord uiRoot) throws JSONException {
        InfoList res = new InfoList(new ArrayList<>());
        res.title = jsonFile.getString("title");
        res.titleNodeId = jsonFile.getString("title_node_id");
        if (res.titleNodeId != null) res.titleNode = uiRoot.getNodeByOriAbsoluteId(res.titleNodeId);
        JSONArray tmpArray = jsonFile.getJSONArray("list_items");
        for (int i = 0; i < tmpArray.length(); i++) {
            JSONObject tmpObject = (JSONObject) tmpArray.get(i);
            res.listItems.add(ListItem.loadFromJson(tmpObject, uiRoot));
        }
        if (res.titleNode != null) {
            res.bound = Utility.getMergeBound(res.bound, res.titleNode);
        }
        for (ListItem listItem : res.listItems) {
            res.bound = Utility.getMergeBound(res.bound, listItem.node);
        }
        return res;
    }

    public ListItem findItem(AccessibilityNodeInfoRecord node) {
        for (ListItem listItem : this.listItems)
            if (listItem.node == node) return listItem;
        return null;
    }

    public void addItemAsPattern(ListItem pattern, AccessibilityNodeInfoRecord node) {
        ListItem crtListItem = new ListItem(node, pattern.level);
        this.listItems.add(crtListItem);
        this.bound = Utility.getMergeBound(this.bound, crtListItem.node);
    }

    public boolean inSubTree(AccessibilityNodeInfoRecord uiRoot) {
        for (ListItem listItem : this.listItems)
            if (!listItem.node.absoluteId.startsWith(uiRoot.absoluteId)) return false;
        if (this.titleNode != null)
            if (!this.titleNode.absoluteId.startsWith(uiRoot.absoluteId)) return false;
        return true;
    }

    public boolean isOverlapped(InfoList otherInfoList) {
        if (this.bound.left>=otherInfoList.bound.right) return false;
        if (this.bound.right<=otherInfoList.bound.left) return false;
        if (this.bound.top>=otherInfoList.bound.bottom) return false;
        if (this.bound.bottom<=otherInfoList.bound.top) return false;
        return true;
    }

    public boolean inNodeSet(Set<AccessibilityNodeInfoRecord> usedNodes) {
        if (this.titleNode != null)
            if (usedNodes.contains(this.titleNode)) return true;
        for (ListItem listItem : this.listItems)
            if (usedNodes.contains(listItem.node)) return true;
        return false;
    }

    public void addToNodeSet(Set<AccessibilityNodeInfoRecord> usedNodes) {
        if (this.titleNode != null) usedNodes.add(this.titleNode);
        for (ListItem listItem : this.listItems)
            usedNodes.add(listItem.node);
    }

    public InfoList getSubListInSubTree(AccessibilityNodeInfoRecord uiRoot, Set<AccessibilityNodeInfoRecord> usedNodes) {
        List<ListItem> listItems = new ArrayList<>();
        for (ListItem listItem : this.listItems) {
            if (usedNodes.contains(listItem.node)) continue;
            if (hasBeenCoverd(usedNodes,listItem.node)) continue;
            if (listItem.node.absoluteId.contains(uiRoot.absoluteId)) listItems.add(listItem);
        }
        InfoList res = new InfoList(new ArrayList<>());
        res.listItems.addAll(listItems);
        Set<AccessibilityNodeInfoRecord> rootSet = new ArraySet<>();
        rootSet.add(uiRoot);
        res.clonePatternInfo(this, rootSet);
        return res;
    }

    private boolean hasBeenCoverd(Set<AccessibilityNodeInfoRecord> usedNodes, AccessibilityNodeInfoRecord node) {
        for (AccessibilityNodeInfoRecord refNode : usedNodes)
            if (node.hasBeenCoverd(refNode)) return true;
        return false;
    }

    public void clonePatternInfo(InfoList infoList, Set<AccessibilityNodeInfoRecord> rootSet) {
        this.title = infoList.title;
        if (infoList.titleNode != null) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(infoList.titleNode);
            double minDis = 1<<30;
            for (AccessibilityNodeInfoRecord root : rootSet) {
                AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root, null, null, new ArraySet<>(), new HashSet<>());
                if (node != null) {
                    double dis = specialNodeDescriber.calNodeDistanceToDescriber(node);
                    if (dis < minDis) {
                        minDis = dis;
                        this.titleNode = node;
                    }
                }
            }
        }
    }

    public void sortInfoItemByPos() {
        Collections.sort(this.listItems,new Comparator<Object>(){
            public int compare(Object a , Object b)
            {
                ListItem item1 = (ListItem) a;
                ListItem item2 = (ListItem) b;
                Rect r1 = new Rect();
                Rect r2 = new Rect();
                item1.node.getBoundsInScreen(r1);
                item2.node.getBoundsInScreen(r2);
                int tmp = r1.top - r2.top;
                if (tmp!=0) return tmp;
                tmp = r1.left - r2.left;
                if (tmp!=0) return tmp;
                tmp = r1.bottom - r2.bottom;
                if (tmp!=0) return tmp;
                tmp = r1.right - r2.right;
                if (tmp!=0) return tmp;
                return item1.hashCode()-item2.hashCode();
            }
        });
    }

    public boolean tryMergedList(InfoList infoList) {
        if (this.titleNode != null && infoList.titleNode!=null) return false;
        for (ListItem listItem : infoList.listItems) this.listItems.add(listItem);
        return true;
    }
}

class Dialog {
    public String nodeId;
    public AccessibilityNodeInfoRecord node;
    public String desc;
    public boolean descIsStatic;
    public String descNodeId;
    public AccessibilityNodeInfoRecord descNode;
    public int descMethod;
    public String editNodeId;
    public AccessibilityNodeInfoRecord editNode;
    public String triggerNodeId;
    public AccessibilityNodeInfoRecord triggerNode;

    Dialog() {
        this.nodeId = null;
        this.node = null;
        this.desc = "";
        this.descIsStatic = false;
        this.descMethod = -1;
        this.descNodeId = "";
        this.descNode = null;
        this.editNodeId = null;
        this.editNode = null;
        this.triggerNodeId = null;
        this.triggerNode = null;
    }

    Dialog(Dialog dialog, AccessibilityNodeInfoRecord node) {
        this.node = node;
        this.nodeId = node.absoluteId;
        this.desc = dialog.desc;
        this.descIsStatic = dialog.descIsStatic;
        this.descMethod = dialog.descMethod;
        if (dialog.descNode != null) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(dialog.descNode);
            this.descNode = specialNodeDescriber.findNode(this.node,null,null);
            if (this.descNode != null) this.descNodeId = this.descNode.absoluteId;
        }
        else {
            this.descNodeId = "";
            this.descNode = null;
        }
        if (dialog.editNode != null) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(dialog.editNode);
            this.editNode = specialNodeDescriber.findNode(this.node,null,null);
            if (this.editNode != null) this.editNodeId = this.editNode.absoluteId;
        }
        else {
            this.editNodeId = "";
            this.editNode = null;
        }
        if (dialog.triggerNode != null) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(dialog.triggerNode);
            this.triggerNode = specialNodeDescriber.findNode(this.node,null,null);
            if (this.triggerNode != null) this.triggerNodeId = this.triggerNode.absoluteId;
        }
        else {
            this.triggerNodeId = "";
            this.triggerNode = null;
        }
    }

    void addInfo(String type, String nodeId) {
        if (type.equals("对话框触发按钮")) {
            this.triggerNodeId = nodeId;
        }
        if (type.equals("对话框输入框")) {
            this.editNodeId = nodeId;
        }
    }

    public static Dialog loadFromJson(JSONObject jsonObject, AccessibilityNodeInfoRecordFromFile uiRoot) throws JSONException {
        Dialog res = new Dialog();
        res.nodeId = jsonObject.getString("node_id");
        if (res.nodeId != null) res.node = uiRoot.getNodeByOriAbsoluteId(res.nodeId);
        res.desc = jsonObject.getString("desc");
        res.descIsStatic = jsonObject.getBoolean("desc_is_static");
        res.descMethod = jsonObject.getInt("desc_method");
        res.descNodeId = jsonObject.getString("desc_node_id");
        if (res.descNodeId != null) res.descNode = uiRoot.getNodeByOriAbsoluteId(res.descNodeId);
        res.editNodeId = jsonObject.getString("edit_node_id");
        if (res.editNodeId != null) res.editNode = uiRoot.getNodeByOriAbsoluteId(res.editNodeId);
        res.triggerNodeId = jsonObject.getString("trigger_node_id");
        if (res.triggerNodeId != null) res.triggerNode = uiRoot.getNodeByOriAbsoluteId(res.triggerNodeId);
        return res;
    }

    JSONObject dumpToJson() {
        JSONObject res = new JSONObject();
        try {
            res.put("node_id", this.nodeId);
            res.put("node_info",this.node.getNodeInfoJson(false));
            res.put("desc",this.desc);
            res.put("desc_is_static",this.descIsStatic);
            res.put("desc_node_id",this.descNodeId);
            res.put("desc_node_info",this.descNode.getNodeInfoJson(false));
            res.put("desc_method",this.descMethod);
            res.put("edit_node_id",this.editNodeId);
            res.put("edit_node_info",this.editNode.getNodeInfoJson(false));
            res.put("trigger_node_id",this.triggerNodeId);
            res.put("trigger_node_info",this.triggerNode.getNodeInfoJson(false));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public boolean inSubTree(AccessibilityNodeInfoRecord uiRoot) {
        if (this.node != null)
            if (!this.nodeId.startsWith(uiRoot.absoluteId)) return false;
        if (this.descNode != null)
            if (!this.descNodeId.startsWith(uiRoot.absoluteId)) return false;
        if (this.editNode != null)
            if (!this.editNodeId.startsWith(uiRoot.absoluteId)) return false;
        if (this.triggerNode != null)
            if (!this.triggerNodeId.startsWith(uiRoot.absoluteId)) return false;
        return true;
    }

    public void addToNodeSet(Set<AccessibilityNodeInfoRecord> usedNodes) {
        if (this.node != null) usedNodes.add(this.node);
        if (this.descNode != null) usedNodes.add(this.descNode);
        if (this.editNode != null) usedNodes.add(this.editNode);
        if (this.triggerNode != null) usedNodes.add(this.triggerNode);
    }

    public boolean inNodeSet(Set<AccessibilityNodeInfoRecord> usedNodes) {
        if (this.node != null)
            if (usedNodes.contains(this.node)) return true;
        if (this.descNode != null)
            if (usedNodes.contains(this.descNode)) return true;
        if (this.editNode != null)
            if (usedNodes.contains(this.editNode)) return true;
        if (this.triggerNode != null)
            if (usedNodes.contains(this.triggerNode)) return true;
        return false;
    }
}

class CheckBox {
    public String nodeId;
    public AccessibilityNodeInfoRecord node;
    public String desc;
    public boolean descIsStatic;
    public String descNodeId;
    public AccessibilityNodeInfoRecord descNode;
    public int descMethod;
    public int state;
    public String stateNodeId;
    public AccessibilityNodeInfoRecord stateNode;
    public int stateMethod;

    CheckBox() {
        this.nodeId = "";
        this.node = null;
        this.desc = "";
        this.descIsStatic = false;
        this.descMethod = -1;
        this.descNodeId = "";
        this.descNode = null;
        this.state = -1;
        this.stateNodeId = "";
        this.stateNode = null;
        this.stateMethod = -1;
    }

    CheckBox(CheckBox checkBox, AccessibilityNodeInfoRecord node) {
        this.nodeId = node.absoluteId;
        this.node = node;
        this.desc = checkBox.desc;
        this.descIsStatic = checkBox.descIsStatic;
        this.descMethod = checkBox.descMethod;
        if (checkBox.descNode != null) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(checkBox.descNode);
            this.descNode = specialNodeDescriber.findNode(this.node,null,null);
            if (this.descNode != null) this.descNodeId = this.descNode.absoluteId;
        }
        else {
            this.descNodeId = "";
            this.descNode = null;
        }
        this.state = checkBox.state;
        this.stateMethod = checkBox.stateMethod;
        if (checkBox.stateNode != null) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(checkBox.stateNode);
            this.stateNode = specialNodeDescriber.findNode(this.node, null, null);
            if (this.stateNode != null) this.stateNodeId = this.stateNode.absoluteId;
        }
        else {
            this.stateNodeId = "";
            this.stateNode = null;
        }
    }

    JSONObject dumpToJson() {
        JSONObject res = new JSONObject();
        try {
            res.put("node_id", this.nodeId);
            res.put("node_info",this.node.getNodeInfoJson(false));
            res.put("desc",this.desc);
            res.put("desc_is_static",this.descIsStatic);
            res.put("desc_node_id",this.descNodeId);
            res.put("desc_node_info",this.descNode.getNodeInfoJson(false));
            res.put("desc_method",this.descMethod);
            res.put("state",this.state);
            res.put("state_node_id",this.stateNodeId);
            res.put("state_node_info",this.stateNode.getNodeInfoJson(false));
            res.put("state_method", this.stateMethod);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static CheckBox loadFromJson(JSONObject jsonFile, AccessibilityNodeInfoRecord uiRoot) throws JSONException {
        CheckBox res = new CheckBox();
        res.nodeId = jsonFile.getString("node_id");
        if (res.nodeId != null) res.node = uiRoot.getNodeByOriAbsoluteId(res.nodeId);
        res.desc = jsonFile.getString("desc");
        res.descIsStatic = jsonFile.getBoolean("desc_is_static");
        res.descMethod = jsonFile.getInt("desc_method");
        res.descNodeId = jsonFile.getString("desc_node_id");
        if (res.descNodeId != null) res.descNode = uiRoot.getNodeByOriAbsoluteId(res.descNodeId);
        res.state = jsonFile.getInt("state");
        res.stateNodeId = jsonFile.getString("state_node_id");
        if (res.stateNodeId != null) res.stateNode = uiRoot.getNodeByOriAbsoluteId(res.stateNodeId);
        res.stateMethod = jsonFile.getInt("state_method");
        return res;
    }

    public boolean inSubTree(AccessibilityNodeInfoRecord uiRoot) {
        if (this.node != null)
            if (!this.nodeId.startsWith(uiRoot.absoluteId)) return false;
        if (this.descNode != null)
            if (!this.descNodeId.startsWith(uiRoot.absoluteId)) return false;
        if (this.stateNode != null)
            if (!this.stateNodeId.startsWith(uiRoot.absoluteId)) return false;
        return true;
    }

    public void addToNodeSet(Set<AccessibilityNodeInfoRecord> usedNodes) {
        if (this.node != null) usedNodes.add(this.node);
        if (this.descNode != null) usedNodes.add(this.descNode);
        if (this.stateNode != null) usedNodes.add(this.stateNode);
    }

    public boolean inNodeSet(Set<AccessibilityNodeInfoRecord> usedNodes) {
        if (this.node != null)
            if (usedNodes.contains(this.node)) return true;
        if (this.descNode != null)
            if (usedNodes.contains(this.descNode)) return true;
        if (this.stateNode != null)
            if (usedNodes.contains(this.stateNode)) return true;
        return false;
    }
}

public class SemanticBlock {
    public static final int titleMethodText = 4;
    public static final int titleMethodDesc = 5;
    public static final int titleMethodOCR = 6;

    public int depth;
    public AccessibilityNodeInfoRecord uiRoot;
    public String blockType;
    public SemanticBlock parentBlock;
    private List<String> rootIds;
    public List<AccessibilityNodeInfoRecord> roots;
    public String title;
    private String titleNodeId;
    public AccessibilityNodeInfoRecord titleNode;
    public boolean titleIsStatic;
    private int titleMethod;
    private List<InfoList> infoLists;
    private List<FuncButton> funcButtons;
    public List<SemanticBlock> childrenBlock;
    public List<String> forwardButtonIds;
    public List<AccessibilityNodeInfoRecord> forwardButtons;
    public List<String> backwardButtonIds;
    public List<AccessibilityNodeInfoRecord> backwardButtons;
    public List<String> tabItemIds;
    public List<AccessibilityNodeInfoRecord> tabItems;
    public List<String> highlightItemIds;
    public List<AccessibilityNodeInfoRecord> highlightItems;
    public List<String> correspondingRegionIds;
    public List<AccessibilityNodeInfoRecord> correspondingRegions;
    public List<String> menuButtonIds;
    public List<AccessibilityNodeInfoRecord> menuButtons;
    public List<String> buttonIdsIntoSearchPage;
    public List<AccessibilityNodeInfoRecord> buttonsIntoSearchPage;
    private String voiceInputNodeId;
    public AccessibilityNodeInfoRecord voiceInputNode;
    private String textInputNodeId;
    public AccessibilityNodeInfoRecord textInputNode;
    public List<String> searchTriggerIds;
    public List<AccessibilityNodeInfoRecord> searchTriggers;
    private List<CheckBox> checkBoxes;
    private List<Dialog> dialogs;
    public List<Gesture> gestures;
    public int mode;

    public SemanticBlock patternBlock;

    SemanticBlock(SemanticBlock parentBlock, AccessibilityNodeInfoRecord uiRoot, SemanticBlock patternBlock) {
        this.parentBlock = parentBlock;
        this.title = null;
        this.titleNodeId = null;
        this.titleNode = null;
        this.titleIsStatic = false;
        this.titleMethod = -1;
        this.rootIds = new ArrayList<>();
        this.roots = new ArrayList<>();
        if (parentBlock!=null) this.depth = parentBlock.depth + 1;
        else this.depth = 0;
        this.blockType = "block";
        this.infoLists = new ArrayList<>();
        this.childrenBlock = new ArrayList<>();
        this.forwardButtonIds = new ArrayList<>();
        this.forwardButtons = new ArrayList<>();
        this.backwardButtonIds = new ArrayList<>();
        this.backwardButtons = new ArrayList<>();
        this.tabItemIds = new ArrayList<>();
        this.tabItems = new ArrayList<>();
        this.highlightItemIds = new ArrayList<>();
        this.highlightItems = new ArrayList<>();
        this.correspondingRegionIds = new ArrayList<>();
        this.correspondingRegions = new ArrayList<>();
        this.menuButtonIds = new ArrayList<>();
        this.menuButtons = new ArrayList<>();
        this.buttonIdsIntoSearchPage = new ArrayList<>();
        this.buttonsIntoSearchPage = new ArrayList<>();
        this.voiceInputNodeId = null;
        this.voiceInputNode = null;
        this.textInputNodeId = null;
        this.textInputNode = null;
        this.searchTriggerIds = new ArrayList<>();
        this.searchTriggers = new ArrayList<>();
        this.checkBoxes = new ArrayList<>();
        this.dialogs = new ArrayList<>();
        this.gestures = new ArrayList<>();
        this.funcButtons = new ArrayList<>();
        this.mode = -1;
        this.uiRoot = uiRoot;
        this.patternBlock = patternBlock;

        if (patternBlock != null) {
            if (patternBlock.title != "")
                this.title = patternBlock.title;
            this.titleIsStatic = patternBlock.titleIsStatic;
            this.titleMethod = patternBlock.titleMethod;
            this.blockType = patternBlock.blockType;
            this.gestures = patternBlock.gestures;
            this.mode = patternBlock.mode;
        }

    }

    SemanticBlock(JSONObject blockInfo, SemanticBlock parentBlock, AccessibilityNodeInfoRecordFromFile uiRoot) throws JSONException {
        this.patternBlock = null;
        this.parentBlock = parentBlock;
        this.depth = blockInfo.getInt("depth");
        this.blockType = blockInfo.getString("block_type");
        this.uiRoot = uiRoot;
        JSONArray tmpArray = blockInfo.getJSONArray("root_ids");
        this.rootIds = new ArrayList<>();
        for (int i = 0; i < tmpArray.length(); i++) {
            String rootId = (String)tmpArray.get(i);
            this.rootIds.add(rootId);
        }
        this.roots = new ArrayList<>();
        for (String rootId : this.rootIds) {
            AccessibilityNodeInfoRecord root = uiRoot.getNodeByOriAbsoluteId(rootId);
            if (root != null) this.roots.add(root);
            else {
                Log.e("SemanticBlock", "ROOT NOT FOUND");
            }
        }
        this.title = blockInfo.getString("title");
        this.titleNodeId = blockInfo.getString("title_node_id");
        if (this.titleNodeId != null) {
            this.titleNode = uiRoot.getNodeByOriAbsoluteId(this.titleNodeId);
        }
        try {
            this.titleIsStatic = blockInfo.getBoolean("title_is_static");
            this.titleMethod = Integer.parseInt(blockInfo.getString("title_method"));
        } catch (JSONException e){
            this.titleIsStatic = false;
            this.titleMethod = -1;
        }
        this.infoLists = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("info_lists");
        for (int i = 0; i < tmpArray.length(); i++) {
            JSONObject tmpObject = (JSONObject) tmpArray.get(i);
            this.infoLists.add(InfoList.loadFromJson(tmpObject, uiRoot));
        }
        this.funcButtons = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("func_buttons");
        for (int i = 0; i < tmpArray.length(); i++) {
            JSONObject tmpObject = (JSONObject) tmpArray.get(i);
            this.funcButtons.add(FuncButton.loadFromJson(tmpObject, uiRoot));
        }
        this.childrenBlock = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("children_block");
        for (int i = 0; i < tmpArray.length(); i++) {
            JSONObject tmpObject = (JSONObject) tmpArray.get(i);
            this.childrenBlock.add(new SemanticBlock(tmpObject, this, uiRoot));
        }
        this.forwardButtonIds = new ArrayList<>();
        this.forwardButtons = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("forwardbutton_ids");
        for (int i = 0; i < tmpArray.length(); i++) {
            this.forwardButtonIds.add((String)tmpArray.get(i));
            AccessibilityNodeInfoRecord node = uiRoot.getNodeByOriAbsoluteId((String)tmpArray.get(i));
            this.forwardButtons.add(node);
        }
        this.backwardButtonIds = new ArrayList<>();
        this.backwardButtons = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("backwardbutton_ids");
        for (int i = 0; i < tmpArray.length(); i++) {
            this.backwardButtonIds.add((String)tmpArray.get(i));
            AccessibilityNodeInfoRecord node = uiRoot.getNodeByOriAbsoluteId((String)tmpArray.get(i));
            this.backwardButtons.add(node);
        }
        this.tabItemIds = new ArrayList<>();
        this.tabItems = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("tab_item_ids");
        for (int i = 0; i < tmpArray.length(); i++) {
            this.tabItemIds.add((String)tmpArray.get(i));
            AccessibilityNodeInfoRecord node = uiRoot.getNodeByOriAbsoluteId((String)tmpArray.get(i));
            this.tabItems.add(node);
        }
        this.highlightItemIds = new ArrayList<>();
        this.highlightItems = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("highlight_item_ids");
        for (int i = 0; i < tmpArray.length(); i++) {
            this.highlightItemIds.add((String)tmpArray.get(i));
            AccessibilityNodeInfoRecord node = uiRoot.getNodeByOriAbsoluteId((String)tmpArray.get(i));
            this.highlightItems.add(node);
        }
        this.correspondingRegionIds = new ArrayList<>();
        this.correspondingRegions = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("corresponding_regions");
        for (int i = 0; i < tmpArray.length(); i++) {
            this.correspondingRegionIds.add((String)tmpArray.get(i));
            AccessibilityNodeInfoRecord node = uiRoot.getNodeByOriAbsoluteId((String)tmpArray.get(i));
            this.correspondingRegions.add(node);
        }
        this.menuButtonIds = new ArrayList<>();
        this.menuButtons = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("menu_button_ids");
        for (int i = 0; i < tmpArray.length(); i++) {
            this.menuButtonIds.add((String)tmpArray.get(i));
            AccessibilityNodeInfoRecord node = uiRoot.getNodeByOriAbsoluteId((String)tmpArray.get(i));
            this.menuButtons.add(node);
        }
        this.buttonIdsIntoSearchPage = new ArrayList<>();
        this.buttonsIntoSearchPage = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("button_ids_into_search_page");
        for (int i = 0; i < tmpArray.length(); i++) {
            this.buttonIdsIntoSearchPage.add((String)tmpArray.get(i));
            AccessibilityNodeInfoRecord node = uiRoot.getNodeByOriAbsoluteId((String)tmpArray.get(i));
            this.buttonsIntoSearchPage.add(node);
        }
        try {
            this.voiceInputNodeId = blockInfo.getString("voice_input_node_id");
            if (this.voiceInputNodeId != null)
                this.voiceInputNode = uiRoot.getNodeByOriAbsoluteId(this.voiceInputNodeId);
            else this.voiceInputNode = null;
            this.textInputNodeId = blockInfo.getString("text_input_node_id");
            if (this.textInputNodeId != null)
                this.textInputNode = uiRoot.getNodeByOriAbsoluteId(this.textInputNodeId);
            else this.textInputNode = null;
        } catch (JSONException e) {
            this.voiceInputNodeId = null;
            this.voiceInputNode = null;
            this.textInputNodeId = null;
            this.textInputNode = null;
        }

        this.searchTriggerIds = new ArrayList<>();
        this.searchTriggers = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("search_trigger_ids");
        for (int i = 0; i < tmpArray.length(); i++) {
            this.searchTriggerIds.add((String)tmpArray.get(i));
            AccessibilityNodeInfoRecord node = uiRoot.getNodeByOriAbsoluteId((String)tmpArray.get(i));
            this.searchTriggers.add(node);
        }

        this.checkBoxes = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("check_boxes");
        for (int i = 0; i < tmpArray.length(); i++) {
            JSONObject tmpObject = (JSONObject) tmpArray.get(i);
            this.checkBoxes.add(CheckBox.loadFromJson(tmpObject, uiRoot));
        }

        this.dialogs = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("dialoges");
        for (int i = 0; i < tmpArray.length(); i++) {
            JSONObject tmpObject = (JSONObject) tmpArray.get(i);
            this.dialogs.add(Dialog.loadFromJson(tmpObject, uiRoot));
        }

        this.gestures = new ArrayList<>();
        tmpArray = blockInfo.getJSONArray("gestures");
        for (int i = 0; i < tmpArray.length(); i++) {
            JSONObject tmpObject = (JSONObject) tmpArray.get(i);
            this.gestures.add(Gesture.loadFromJson(tmpObject, uiRoot));
        }

        this.mode = blockInfo.getInt("mode");
    }

    static SemanticBlock buildBlockTreeFromFile(String jsonFilePath, AccessibilityNodeInfoRecordFromFile uiRoot) throws IOException, JSONException {
        InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(jsonFilePath));
        BufferedReader reader = new BufferedReader(inputStreamReader, 5 * 1024);
        char[] buffer = new char[5 * 1024];
        int length;
        StringBuilder builder = new StringBuilder();
        while ((length = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, length);
        }
        reader.close();
        inputStreamReader.close();
        JSONObject wholeTree;
        try {
            wholeTree = new JSONObject(builder.toString());
            SemanticBlock crtRoot = new SemanticBlock(wholeTree, null, uiRoot);
            return crtRoot;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean findBlockRoots(Set<AccessibilityNodeInfoRecord> rootSet, AccessibilityNodeInfoRecord crtRoot) {
        int lastCnt = rootSet.size();
        for (AccessibilityNodeInfoRecord pattenRoot : this.roots) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(pattenRoot);
            AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(crtRoot, null, null);
            if (node!=null) {
                rootSet.addAll(specialNodeDescriber.findParallelBlocks(crtRoot, node, new HashSet<>(), new HashSet<>()));
            }
        }
        if (rootSet.size() != lastCnt) return true;
        return false;
    }

    public JSONObject dumpToJson() throws JSONException {
        // TODO: 2021/3/12
        JSONObject res = new JSONObject();
        res.put("depth", this.depth);
        res.put("block_type", this.blockType);
        res.put("root_ids", this.rootIds);
        JSONArray rootsInfo = new JSONArray();
        for (AccessibilityNodeInfoRecord root : this.roots) rootsInfo.put(root.getNodeInfoJson(true));
        res.put("roots_info",rootsInfo);
        res.put("title",this.title);
        res.put("title_node_id",this.titleNodeId);
        if (this.titleNode != null)
            res.put("title_node_info",this.titleNode.getNodeInfoJson(false));
        else
            res.put("title_node_info",new JSONObject());
        res.put("title_is_static",this.titleIsStatic);
        res.put("title_method",this.titleMethod);
        JSONArray infoLists = new JSONArray();
        for (InfoList infoList : this.infoLists) infoLists.put(infoList.dumpToJson());
        res.put("info_lists",infoLists);
        JSONArray funcButtons = new JSONArray();
        for (FuncButton funcButton : this.funcButtons) funcButtons.put(funcButton.dumpToJson());
        res.put("func_buttons",funcButtons);
        JSONArray children = new JSONArray();
        for (SemanticBlock child : this.childrenBlock) children.put(child.dumpToJson());
        res.put("children_block",children);
        res.put("forwardbutton_ids",this.forwardButtonIds);
        JSONArray forwardbuttonsInfo = new JSONArray();
        for (AccessibilityNodeInfoRecord button : this.forwardButtons) forwardbuttonsInfo.put(button.getNodeInfoJson(false));
        res.put("forwardbuttons_info",forwardbuttonsInfo);
        res.put("backwardbutton_ids",this.backwardButtonIds);
        JSONArray backwardbuttonsInfo = new JSONArray();
        for (AccessibilityNodeInfoRecord button : this.backwardButtons) backwardbuttonsInfo.put(button.getNodeInfoJson(false));
        res.put("backwardbuttons_info",backwardbuttonsInfo);
        res.put("tab_item_ids",this.tabItemIds);
        JSONArray tabItemsInfo = new JSONArray();
        for (AccessibilityNodeInfoRecord item : this.tabItems) tabItemsInfo.put(item.getNodeInfoJson(false));
        res.put("tab_items_info",tabItemsInfo);
        res.put("highlight_item_ids",this.highlightItemIds);
        JSONArray highlightItemsInfo = new JSONArray();
        for (AccessibilityNodeInfoRecord item : this.highlightItems) highlightItemsInfo.put(item.getNodeInfoJson(false));
        res.put("highlight_items_info",highlightItemsInfo);
        res.put("corresponding_regions",this.correspondingRegionIds);
        JSONArray correspondingRegionsInfo = new JSONArray();
        for (AccessibilityNodeInfoRecord region : this.correspondingRegions) correspondingRegionsInfo.put(region.getNodeInfoJson(false));
        res.put("corresponding_regions_info",correspondingRegionsInfo);
        res.put("menu_button_ids",this.menuButtonIds);
        JSONArray menuButtonsInfo = new JSONArray();
        for (AccessibilityNodeInfoRecord button : this.menuButtons) menuButtonsInfo.put(button.getNodeInfoJson(false));
        res.put("menu_buttons_info",menuButtonsInfo);
        res.put("button_ids_into_search_page",this.buttonIdsIntoSearchPage);
        JSONArray buttonsInfo = new JSONArray();
        for (AccessibilityNodeInfoRecord button : this.buttonsIntoSearchPage) buttonsInfo.put(button.getNodeInfoJson(false));
        res.put("buttons_info_into_search_page",buttonsInfo);
        res.put("voice_input_node_id",this.voiceInputNodeId);
        if (this.voiceInputNode != null)
            res.put("voice_input_node_info",this.voiceInputNode.getNodeInfoJson(false));
        else
            res.put("voice_input_node_info", new JSONObject());
        res.put("text_input_node_id",this.textInputNodeId);
        if (this.textInputNode != null)
            res.put("text_input_node_info",this.textInputNode.getNodeInfoJson(false));
        else
            res.put("text_input_node_info",new JSONObject());
        res.put("search_trigger_ids",this.searchTriggerIds);
        JSONArray triggers = new JSONArray();
        for (AccessibilityNodeInfoRecord trigger : this.searchTriggers) triggers.put(trigger.getNodeInfoJson(false));
        res.put("search_triggers_info",triggers);
        JSONArray checkBoxes = new JSONArray();
        for (CheckBox checkBox : this.checkBoxes) checkBoxes.put(checkBox.dumpToJson());
        res.put("check_boxes",checkBoxes);
        JSONArray dialoges = new JSONArray();
        for (Dialog dialog : this.dialogs) dialoges.put(dialog.dumpToJson());
        res.put("dialoges",dialoges);
        JSONArray gestures = new JSONArray();
        for (Gesture gesture : this.gestures) gestures.put(gesture.dumpToJson());
        res.put("gestures",gestures);
        res.put("mode",this.mode);
        return res;
    }

    public void addRoot(AccessibilityNodeInfoRecord root) {
        this.roots.add(root);
        this.rootIds.add(root.absoluteId);
    }

    public boolean findTitle(SemanticBlock semanticBlock, AccessibilityNodeInfoRecord root) {
        //this为模板，semanticBlock为能够生成的区块
        if (this.title != null) {
            semanticBlock.setTitle(this.title, null, true, -1);
            return true;
        }
        else {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(this.titleNode);
            AccessibilityNodeInfoRecord titleNode = specialNodeDescriber.findNode(root,null,null);
            if (titleNode != null) {
                if (!this.titleIsStatic) {
                    semanticBlock.setTitle(null, titleNode, this.titleIsStatic, this.titleMethod);
                    return true;
                }
                else {
                    if ((this.titleMethod == 4 && this.titleNode.getText().equals(titleNode.getText())) ||
                            (this.titleMethod == 5 && this.titleNode.getContentDescription().equals(titleNode.getContentDescription()))) {
                        semanticBlock.setTitle(null, titleNode, this.titleIsStatic, this.titleMethod);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void setTitle(String title, AccessibilityNodeInfoRecord titleNode, boolean isStatic, int method) {
        if (title != null)
            this.title = title;
        this.titleNode = titleNode;
        this.titleNodeId = titleNode.absoluteId;
        this.titleIsStatic = isStatic;
        this.titleMethod = method;
    }

    public void findForwardButtons(Set<AccessibilityNodeInfoRecord> buttons, AccessibilityNodeInfoRecord root) {
        for (AccessibilityNodeInfoRecord patternButton : this.forwardButtons) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(patternButton);
            AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root,null,null);
            if (node != null) buttons.add(node);
        }
    }

    public void setForwardButtons(Set<AccessibilityNodeInfoRecord> buttons) {
        this.forwardButtons.addAll(buttons);
        for (AccessibilityNodeInfoRecord button : buttons) this.forwardButtonIds.add(button.absoluteId);
    }

    public void findBackwardButtons(Set<AccessibilityNodeInfoRecord> buttons, AccessibilityNodeInfoRecord root) {
        for (AccessibilityNodeInfoRecord patternButton : this.backwardButtons) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(patternButton);
            AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root,null,null);
            if (node != null) buttons.add(node);
        }
    }

    public void setBackwardButtons(Set<AccessibilityNodeInfoRecord> buttons) {
        this.backwardButtons.addAll(buttons);
        for (AccessibilityNodeInfoRecord button : buttons) this.backwardButtonIds.add(button.absoluteId);
    }

    public void findInfoLists(SemanticBlock semanticBlock, Set<AccessibilityNodeInfoRecord> itemSet, Set<AccessibilityNodeInfoRecord> rootSet, Set<AccessibilityNodeInfoRecord> endNodes) {
        long parallelTime = 0;
        long findNodeTime = 0;
        int cnt = 0;
        while (true) {
            boolean flag = false;
            for (InfoList infoList : this.infoLists) {
                cnt += 1;
                InfoList crtInfoList = new InfoList(new ArrayList<>());
                for (ListItem listItem : infoList.listItems) {
                    SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(listItem.node);
                    for (AccessibilityNodeInfoRecord root : rootSet) {
                        long startTime = System.currentTimeMillis();
                        AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root, null, null, itemSet, endNodes); //先找到其中一个最相近的
                        long endTime = System.currentTimeMillis();
                        findNodeTime += endTime-startTime;
                        if (node != null && (!itemSet.contains(node))) {
                            Log.i("find one item",node.getAllTexts());
                            startTime = System.currentTimeMillis();
                            if (specialNodeDescriber.calNodeDistanceToDescriber(node) > 0.6) break;
                            List<AccessibilityNodeInfoRecord> similarNodes = specialNodeDescriber.findParallelBlocks(root, node, itemSet, endNodes); //再找并列的节点
                            for (AccessibilityNodeInfoRecord similarNode : similarNodes)
                                if (similarNode != null && (!itemSet.contains(similarNode))) {
                                    crtInfoList.addItemAsPattern(listItem, similarNode);
                                    itemSet.addAll(Utility.getOverlapNodes(root, similarNode));
                                }
                            endTime = System.currentTimeMillis();
                            findNodeTime += endTime-startTime;
                        }
                    }
                }
                if (crtInfoList.listItems.size() > 0) {
                    crtInfoList.sortInfoItemByPos();
                    crtInfoList.clonePatternInfo(infoList, rootSet);
                    semanticBlock.addInfoList(crtInfoList);
                    System.out.println("InfoList length: "+crtInfoList.listItems.size());
                    for (ListItem listItem : crtInfoList.listItems) System.out.println(listItem.node.getAllTexts()+" "+listItem.node.depth);
                    System.out.println("InfoList Pattern:");
                    for (ListItem listItem : infoList.listItems) System.out.println(listItem.node.getAllTexts());
                    System.out.println("EndNodes Num: "+endNodes.size());
                    Log.i("infolist try times",Integer.toString(cnt));
                    Log.i("infolist find node",Long.toString(findNodeTime));
                    Log.i("infolist find parallel nodes",Long.toString(parallelTime));
                    flag = true;
                }
            }
            if (!flag) break;
        }
    }

    public void addInfoList(InfoList crtInfoList) {
        this.infoLists.add(crtInfoList);
    }

    public void setMode(int mode) {
        if (this.mode == -1) this.mode = mode;
        else if (this.mode != mode) this.mode = 100;
    }

    public void findTabItems(Set<AccessibilityNodeInfoRecord> tabItems, AccessibilityNodeInfoRecord root) {
        for (AccessibilityNodeInfoRecord patternTabItem : this.tabItems) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(patternTabItem);
            AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root,null,null);
            if (node != null && (!tabItems.contains(node))) {
                List<AccessibilityNodeInfoRecord> similarNodes = specialNodeDescriber.findParallelBlocks(root, node, new HashSet<>(), new HashSet<>());
                for (AccessibilityNodeInfoRecord similarNode : similarNodes)
                    if (similarNode!= null && (!tabItems.contains(similarNode))) {
                        tabItems.add(similarNode);
                    }
            }
        }
    }

    public void setTabItems(Set<AccessibilityNodeInfoRecord> tabItems) {
        this.tabItems.addAll(tabItems);
        for (AccessibilityNodeInfoRecord tabItem : tabItems) this.tabItemIds.add(tabItem.absoluteId);
    }

    public void findFuncButtons(SemanticBlock semanticBlock, Set<AccessibilityNodeInfoRecord> buttons, AccessibilityNodeInfoRecord crtNode,
                                Set<AccessibilityNodeInfoRecord> endNodes) {
        /*for (FuncButton funcButton : this.funcButtons) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(funcButton.node);
            AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root, null, null, buttons, endNodes);
            if (funcButton.descMethod == 0 && node != null && (!buttons.contains(node))) {
                //如果按钮本身不包含特殊描述，则需要在列表里看一遍同类节点
                List<AccessibilityNodeInfoRecord> similarNodes = specialNodeDescriber.findParallelBlocks(root, node, buttons, endNodes);
                for (AccessibilityNodeInfoRecord similarNode : similarNodes)
                    if (similarNode != null && (!buttons.contains(similarNode))) {
                        buttons.add(similarNode);
                        semanticBlock.funcButtons.add(new FuncButton(similarNode, funcButton));
                    }
            }
            else if (node != null) {
                buttons.add(node);
                semanticBlock.funcButtons.add(new FuncButton(node, funcButton));
            }
        }*/
        if (endNodes.contains(crtNode)) return;
        double minDis = 1;
        for (FuncButton funcButton :  this.funcButtons) {
            AccessibilityNodeInfoRecord node = funcButton.node;
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(node);
            AccessibilityNodeInfoRecord similarNode = specialNodeDescriber.findNodeByText(crtNode, endNodes);
            if (similarNode == null) similarNode = specialNodeDescriber.findNodeByDesc(crtNode,endNodes);
            if (similarNode == null) similarNode = specialNodeDescriber.findNodeByPos(crtNode,endNodes);
            if (similarNode != null) {
                semanticBlock.funcButtons.add(new FuncButton(similarNode, funcButton));
                endNodes.add(similarNode);
                continue;
            }
            else{
                Log.i("funcButton not found",funcButton.node.getAllTexts());
            }
        }
    }

    public void findCorrespondingRegions(Set<AccessibilityNodeInfoRecord> regions, AccessibilityNodeInfoRecord root) {
        for (AccessibilityNodeInfoRecord patternRegion : this.correspondingRegions) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(patternRegion);
            AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root,null,null);
            if (node != null) regions.add(node);
        }
    }

    public void setCorrespondingRegions(Set<AccessibilityNodeInfoRecord> regions) {
        this.correspondingRegions.addAll(regions);
        for (AccessibilityNodeInfoRecord region : regions) this.correspondingRegionIds.add(region.absoluteId);
    }

    public void findMenuButtons(Set<AccessibilityNodeInfoRecord> buttons, AccessibilityNodeInfoRecord root) {
        for (AccessibilityNodeInfoRecord patternButton : this.menuButtons) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(patternButton);
            AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root,null,null);
            if (node != null) buttons.add(node);
        }
    }

    public void setMenuButtons(Set<AccessibilityNodeInfoRecord> buttons) {
        this.menuButtons.addAll(buttons);
        for (AccessibilityNodeInfoRecord button : buttons) this.menuButtonIds.add(button.absoluteId);
    }

    public void findButtonsIntoSearchPage(Set<AccessibilityNodeInfoRecord> buttons, AccessibilityNodeInfoRecord root) {
        for (AccessibilityNodeInfoRecord patternButton : this.buttonsIntoSearchPage) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(patternButton);
            AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root,null,null);
            if (node != null) buttons.add(node);
        }
    }

    public void setButtonsIntoSearchPage(Set<AccessibilityNodeInfoRecord> buttons) {
        this.buttonsIntoSearchPage.addAll(buttons);
        for (AccessibilityNodeInfoRecord button : buttons) this.buttonIdsIntoSearchPage.add(button.absoluteId);
    }

    public void findSearchTriggers(Set<AccessibilityNodeInfoRecord> buttons, AccessibilityNodeInfoRecord root) {
        for (AccessibilityNodeInfoRecord patternButton : this.searchTriggers) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(patternButton);
            AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root,null,null);
            if (node != null) buttons.add(node);
        }
    }

    public void setSearchTriggers(Set<AccessibilityNodeInfoRecord> buttons) {
        this.searchTriggers.addAll(buttons);
        for (AccessibilityNodeInfoRecord button : buttons) this.searchTriggerIds.add(button.absoluteId);
    }

    public void findVoiceInputButton(SemanticBlock semanticBlock, AccessibilityNodeInfoRecord root) {
        if (semanticBlock.voiceInputNode != null) return;
        if (this.voiceInputNode == null) return;
        SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(this.voiceInputNode);
        AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root,null,null);
        if (node != null) {
            semanticBlock.voiceInputNode = node;
            semanticBlock.voiceInputNodeId = node.absoluteId;
        }
    }

    public void findTextInputButton(SemanticBlock semanticBlock, AccessibilityNodeInfoRecord root) {
        if (semanticBlock.textInputNode != null) return;
        if (this.textInputNode == null) return;
        SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(this.textInputNode);
        AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root,null,null);
        if (node != null) {
            semanticBlock.textInputNode = node;
            semanticBlock.textInputNodeId = node.absoluteId;
        }
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    public void findCheckBoxes(SemanticBlock semanticBlock, Set<AccessibilityNodeInfoRecord> nodeSet, AccessibilityNodeInfoRecord root,
                               Set<AccessibilityNodeInfoRecord> endNodes) {
        for (CheckBox checkBox : this.checkBoxes) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(checkBox.node);
            AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root,null,null, nodeSet, endNodes);
            if (node != null && (!nodeSet.contains(node))) {
                List<AccessibilityNodeInfoRecord> similarNodes = specialNodeDescriber.findParallelBlocks(root, node, nodeSet, endNodes);
                for (AccessibilityNodeInfoRecord similarNode : similarNodes)
                    if (similarNode != null && (!nodeSet.contains(similarNode))) {
                        nodeSet.add(similarNode);
                        CheckBox newCheckBox = new CheckBox(checkBox,similarNode);
                        semanticBlock.checkBoxes.add(newCheckBox);
                    }
            }
        }
    }

    public void findDialogs(SemanticBlock semanticBlock, Set<AccessibilityNodeInfoRecord> nodeSet, AccessibilityNodeInfoRecord root,
                            Set<AccessibilityNodeInfoRecord> endNodes) {
        for (Dialog dialog : this.dialogs) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(dialog.node);
            AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(root, null, null, nodeSet, endNodes);
            if (node != null && (!nodeSet.contains(node))) {
                List<AccessibilityNodeInfoRecord> similarNodes = specialNodeDescriber.findParallelBlocks(root, node, nodeSet, endNodes);
                for (AccessibilityNodeInfoRecord similarNode : similarNodes)
                    if (similarNode != null && (!nodeSet.contains(similarNode))) {
                        nodeSet.add(similarNode);
                        Dialog newDialog = new Dialog(dialog,similarNode);
                        semanticBlock.dialogs.add(newDialog);
                    }
            }
        }
    }

    public void setGesture(List<Gesture> gestures) {
        this.gestures.addAll(gestures);
    }

    public double calNodeDistanceToTitle(AccessibilityNodeInfoRecord crtNode) {
        if (this.titleNode == null) return 1.0;
        SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(this.titleNode);
        if (this.titleMethod == titleMethodText) {
            CharSequence text = crtNode.getText();
            if (text == null) return 1.0;
            if (text.length()==0) return 1.0;
            if ((!text.equals(this.titleNode.getText())) && this.titleIsStatic) return 1.0;
        }
        if (this.titleMethod == titleMethodDesc) {
            CharSequence desc = crtNode.getContentDescription();
            if (desc.length() == 0) return 1.0;
            if ((!desc.equals(this.titleNode.getContentDescription())) && this.titleIsStatic) return 1.0;

        }
        double dis = specialNodeDescriber.calNodeDistanceToDescriber(crtNode);
        Log.i("node distance",new Double(dis).toString());
        /*if (this.titleMethod == titleMethodOCR) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(this.titleNode);
            //TODO: OCR
            return specialNodeDescriber.calNodeDistanceToDescriber(crtNode);
        }*/
        return dis;
    }

    public List<InfoList> getInfoLists() {
        return this.infoLists;
    }

    public Map<FuncButton, Double> getFuncButtons() {
        Map<FuncButton, Double> res = new HashMap<>();
        for (FuncButton funcButton : this.funcButtons) {
            res.put(funcButton,funcButton.getDist());
        }
        return res;
    }

    public List<CheckBox> getCheckBoxes() {
        return this.checkBoxes;
    }

    public List<Dialog> getDialogs() {
        return this.dialogs;
    }

    public double setTitleFromSubTree(List<Pair<Double, SemanticBlock>> childrenRes, List<AccessibilityNodeInfoRecord> titles,
                                      Set<AccessibilityNodeInfoRecord> usedNodes) {
        for (Pair<Double, SemanticBlock> childRes : childrenRes)
            if (childRes.second.titleNode != null) {
                if (!this.setTitle(childRes.second)) {
                    usedNodes.add(this.titleNode);
                    return 100.0; //在当前子树中找到了多个title
                }
            }
        if (titles.contains(this.uiRoot)) {
            if (this.titleNode != null) {
                usedNodes.add(this.titleNode);
                return 100.0; //在当前子树中找到了多个title
            }
            assert(this.patternBlock != null);
            this.setTitle(this.patternBlock.title, this.uiRoot, this.patternBlock.titleIsStatic, this.patternBlock.titleMethod);
            usedNodes.add(this.titleNode);
            return 1000.0;
        }
        if (this.titleNode != null) {
            usedNodes.add(this.titleNode);
            return 1000.0;
        }
        return 0.0;
    }

    private boolean setTitle(SemanticBlock patternBlock) {
        if (this.titleNode != null) return false;
        this.title = patternBlock.title;
        this.titleNode = patternBlock.titleNode;
        this.titleNodeId = patternBlock.titleNodeId;
        this.titleIsStatic = patternBlock.titleIsStatic;
        this.titleMethod = patternBlock.titleMethod;
        return true;
    }

    public void setKeyNodesFromSubTree(List<Pair<Double, SemanticBlock>> childrenRes) {
        //完成所有节点列表形式的合并，包括forwardbutton、backwardbutton、tabitem
        List<AccessibilityNodeInfoRecord> nodes = new ArrayList<>();
        for (Pair<Double, SemanticBlock> childRes : childrenRes) {
            nodes = childRes.second.forwardButtons;
            this.addNodeToNodeList(this.forwardButtons, this.forwardButtonIds, nodes);
            nodes = childRes.second.backwardButtons;
            this.addNodeToNodeList(this.backwardButtons, this.backwardButtonIds, nodes);
            nodes = childRes.second.tabItems;
            this.addNodeToNodeList(this.tabItems, this.tabItemIds, nodes);
            nodes = childRes.second.highlightItems;
            this.addNodeToNodeList(this.highlightItems, this.highlightItemIds, nodes);
            nodes = childRes.second.correspondingRegions;
            this.addNodeToNodeList(this.correspondingRegions, this.correspondingRegionIds, nodes);
            nodes = childRes.second.menuButtons;
            this.addNodeToNodeList(this.menuButtons, this.menuButtonIds, nodes);
            nodes = childRes.second.buttonsIntoSearchPage;
            this.addNodeToNodeList(this.buttonsIntoSearchPage, this.buttonIdsIntoSearchPage, nodes);
            nodes = childRes.second.searchTriggers;
            this.addNodeToNodeList(this.searchTriggers, this.searchTriggerIds, nodes);
        }

    }

    private void addNodeToNodeList(List<AccessibilityNodeInfoRecord> nodesListToUpdate, List<String> nodeIdsListToUpdate, List<AccessibilityNodeInfoRecord> nodes) {
        //将nodes的所有元素添加到nodesListToUpdate中
        for (AccessibilityNodeInfoRecord node : nodes) {
            assert(!nodesListToUpdate.contains(node));
            nodesListToUpdate.add(node);
            nodeIdsListToUpdate.add(node.absoluteId);
        }
    }

    public void updateRootInfo(List<AccessibilityNodeInfoRecord> nodesListToUpdate, List<String> nodeIdsListToUpdate, Map<AccessibilityNodeInfoRecord, Double> keyNodeToDist,
                               Set<AccessibilityNodeInfoRecord> usedNodes) {
        if (keyNodeToDist.containsKey(this.uiRoot) && (!usedNodes.contains(this.uiRoot))) {
            nodesListToUpdate.add(this.uiRoot);
            usedNodes.add(this.uiRoot);
            nodeIdsListToUpdate.add(this.uiRoot.absoluteId);
        }
    }

    public double calcNodeScore(List<AccessibilityNodeInfoRecord> keyNodes, Map<AccessibilityNodeInfoRecord, Double> keyNodeToDist) {
        //完成所有节点列表形式的分数计算
        double score = 0.0;
        for (AccessibilityNodeInfoRecord node : keyNodes) {
            score += 5/Math.pow(Math.E,keyNodeToDist.get(node)); //累加1/e^dis
        }
        return score;
    }

    public double setInfoListsFromSubTree(List<Pair<Double, SemanticBlock>> childrenRes, List<InfoList> infoLists, Set<AccessibilityNodeInfoRecord> usedNodes) {

        //找出只要当前层的infolist
        for (InfoList infoList : infoLists) {
            /*if (infoList.inSubTree(this.uiRoot) && (!this.containInfoList(infoList, usedNodes))) {
                this.infoLists.add(infoList);
                infoList.addToNodeSet(usedNodes);
            }
            else {*/
                InfoList subInfoList = infoList.getSubListInSubTree(this.uiRoot, usedNodes);
                if (subInfoList.listItems.size() == 0) continue;
                this.infoLists.add(subInfoList);
                subInfoList.addToNodeSet(usedNodes);
            }
        //}
        for (Pair<Double, SemanticBlock> childRes : childrenRes) {
            for (InfoList infoList : childRes.second.infoLists)
                if (!this.containInfoList(infoList, usedNodes)) {
                    this.infoLists.add(infoList);
                    infoList.addToNodeSet(usedNodes);
                }
        }
        if (this.infoLists.size()>1) {
            List<InfoList> tmpInfoLists = new ArrayList<>(this.infoLists);
            this.infoLists.clear();
            this.infoLists.add(tmpInfoLists.get(0));
            for (int i = 1; i <tmpInfoLists.size(); i++) {
                boolean flag = true;
                for (InfoList infoList : this.infoLists)
                    if (infoList.tryMergedList(tmpInfoLists.get(i))) {
                        flag = false;
                        break;
                    }
                if (flag) this.infoLists.add(tmpInfoLists.get(i));
            }
        }

        double score = 0.0;
        for (InfoList infoList : this.infoLists) {
            score += infoList.listItems.size()*5.0;    //累加每个列表的长度
        }
        return score;
    }

    private boolean containInfoList(InfoList otherInfoList, Set<AccessibilityNodeInfoRecord> usedNodes) {
        for (InfoList infoList : this.infoLists) {
            if (infoList.isOverlapped(otherInfoList)) return true;
            if (infoList.inNodeSet(usedNodes)) return true;
        }
        return false;
    }

    public double setFuncButtonsFromSubTree(List<Pair<Double, SemanticBlock>> childrenRes, Map<FuncButton, Double> funcButtons, Set<AccessibilityNodeInfoRecord> usedNodes) {
        for (Pair<Double, SemanticBlock> childRes : childrenRes)
            this.funcButtons.addAll(childRes.second.funcButtons);
        //找到当前位置的funcButton
        for (FuncButton funcButton : funcButtons.keySet()) {
            if (funcButton.node == this.uiRoot) {
                this.funcButtons.add(funcButton);
                break;
            }
        }
        double score = 0;
        for (FuncButton funcButton : this.funcButtons) {
            score += 5/Math.pow(Math.E,funcButtons.get(funcButton)); //累加1/e^dis
            usedNodes.add(funcButton.node);
        }
        return score;
    }

    public double setVoiceInputButtonFromSubTree(List<Pair<Double, SemanticBlock>> childrenRes, Map<AccessibilityNodeInfoRecord, Double> voiceInputButtons,
                                                 Set<AccessibilityNodeInfoRecord> usedNodes) {
        AccessibilityNodeInfoRecord node = null;
        double minDis = 1;
        for (Pair<Double, SemanticBlock> childRes : childrenRes)
            if (childRes.second.voiceInputNode != null) {
                if (!voiceInputButtons.containsKey(childRes.second.voiceInputNode)) continue;
                if (usedNodes.contains(childRes.second.voiceInputNode)) continue;;
                double dis = voiceInputButtons.get(childRes.second.voiceInputNode);
                if (dis < minDis) {
                    minDis = dis;
                    node = childRes.second.voiceInputNode;
                }
            }
        //判断当前节点是否为语音输入
        if (voiceInputButtons.containsKey(this.uiRoot) && (!usedNodes.contains(this.uiRoot))) {
            double dis = voiceInputButtons.get(this.uiRoot);
            if (dis < minDis) {
                minDis = dis;
                node = this.uiRoot;
            }
        }
        if (node != null) {
            this.voiceInputNode = node;
            this.voiceInputNodeId = node.absoluteId;
            usedNodes.add(node);
            return 5/Math.pow(Math.E,voiceInputButtons.get(node));
        }
        return 0.0;
    }

    public double setTextInputButtonFromSubTree(List<Pair<Double, SemanticBlock>> childrenRes, Map<AccessibilityNodeInfoRecord, Double> textInputButtons,
                                                Set<AccessibilityNodeInfoRecord> usedNodes) {
        AccessibilityNodeInfoRecord node = null;
        double minDis = 1;
        for (Pair<Double, SemanticBlock> childRes : childrenRes)
            if (childRes.second.textInputNode != null) {
                if (!textInputButtons.containsKey(childRes.second.textInputNode)) continue;
                if (usedNodes.contains(childRes.second.textInputNode)) continue;
                double dis = textInputButtons.get(childRes.second.textInputNode);
                if (dis < minDis) {
                    minDis = dis;
                    node = childRes.second.textInputNode;
                }
            }
        //判断当前节点是否为文本输入
        if (textInputButtons.containsKey(this.uiRoot) && (!usedNodes.contains(this.uiRoot))) {
            double dis = textInputButtons.get(this.uiRoot);
            if (dis < minDis) {
                minDis = dis;
                node = this.uiRoot;
            }
        }
        if (node != null) {
            this.textInputNode = node;
            this.textInputNodeId = node.absoluteId;
            usedNodes.add(node);
            return 5/Math.pow(Math.E,textInputButtons.get(node));
        }
        return 0.0;
    }

    public double setCheckBoxesFromSubTree(List<Pair<Double, SemanticBlock>> childrenRes, List<CheckBox> checkBoxes,
                                           Set<AccessibilityNodeInfoRecord> usedNodes) {
        for (Pair<Double, SemanticBlock> childRes : childrenRes) {
            this.checkBoxes.addAll(childRes.second.checkBoxes);
            for (CheckBox checkBox : childRes.second.checkBoxes) checkBox.addToNodeSet(usedNodes);
        }
        //判断当前节点新增的
        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.inSubTree(this.uiRoot) && (!this.containCheckBox(checkBox)) && (!checkBox.inNodeSet(usedNodes))) {
                this.checkBoxes.add(checkBox);
                checkBox.addToNodeSet(usedNodes);
            }
        }
        double score = 0.0;
        for (CheckBox checkBox : checkBoxes) {
            score += 10;    //累加每个对话框
        }
        return score;
    }

    private boolean containCheckBox(CheckBox checkBox) {
        return this.checkBoxes.contains(checkBox);
    }

    public double setDialogsFromSubTree(List<Pair<Double, SemanticBlock>> childrenRes, List<Dialog> dialogs,
                                        Set<AccessibilityNodeInfoRecord> usedNodes) {
        for (Pair<Double, SemanticBlock> childRes : childrenRes) {
            this.dialogs.addAll(childRes.second.dialogs);
            for (Dialog dialog : childRes.second.dialogs) dialog.addToNodeSet(usedNodes);
        }
        //判断当前节点新增的
        for (Dialog dialog : dialogs) {
            if (dialog.inSubTree(this.uiRoot) && (!this.containDialog(dialog)) && (!dialog.inNodeSet(usedNodes))) {
                this.dialogs.add(dialog);
                dialog.addToNodeSet(usedNodes);
            }
        }
        double score = 0.0;
        for (Dialog dialog : dialogs) {
            score += 20;    //累加每个对话框
        }
        return score;
    }

    private boolean containDialog(Dialog dialog) {
        return this.dialogs.contains(dialog);
    }


    public double setChildrenBlockFromSubTree(List<SemanticBlock> matchedBlocks) {
        double score = 0;
        for (SemanticBlock semanticBlock : matchedBlocks)
            if (this.containBlock(semanticBlock)) {
                this.childrenBlock.add(semanticBlock);
                score += 100.0;
            }
        return score;
    }

    private boolean containBlock(SemanticBlock semanticBlock) {
        for (AccessibilityNodeInfoRecord root : semanticBlock.roots)
            if (!root.absoluteId.startsWith(this.uiRoot.absoluteId)) return false;
        return true;
    }

    public double setSingleRoot(AccessibilityNodeInfoRecord crtRoot, List<AccessibilityNodeInfoRecord> patternRoots,
                          Set<AccessibilityNodeInfoRecord> usedNodes, Set<AccessibilityNodeInfoRecord> endNodes) {
        this.roots.add(crtRoot);
        this.rootIds.add(crtRoot.absoluteId);
        double minDis = 1<<30;
        for (AccessibilityNodeInfoRecord root : patternRoots) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(root);
            double dist = specialNodeDescriber.calNodeDistanceToDescriber(crtRoot);
            if (dist < minDis) minDis = dist;
        }
        usedNodes.add(crtRoot);
        return 50.0/Math.pow(20.0,minDis);
    }

    public double setMultiRoots(AccessibilityNodeInfoRecord crtRoot, List<AccessibilityNodeInfoRecord> patternRoots,
                                Set<AccessibilityNodeInfoRecord> usedNodes, Set<AccessibilityNodeInfoRecord> endNodes) {
        double res = 0;
        for (AccessibilityNodeInfoRecord root : patternRoots) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(root);
            AccessibilityNodeInfoRecord node = specialNodeDescriber.findNode(crtRoot, null, null, usedNodes, endNodes);
            if (node == null) continue;
            if (usedNodes.contains(node)) continue;
            List<AccessibilityNodeInfoRecord> newRoots = specialNodeDescriber.findParallelBlocks(crtRoot, node, usedNodes, endNodes);
            for (AccessibilityNodeInfoRecord newRoot : newRoots) {
                this.roots.add(newRoot);
                this.rootIds.add(newRoot.absoluteId);
                double dis =  specialNodeDescriber.calNodeDistanceToDescriber(newRoot);
                res += 50.0/Math.pow(20.0,dis);
            }

        }
        return res;
    }

    public void refreshTree() {
        for (SemanticBlock child : this.childrenBlock) {
            child.parentBlock = this;
            child.depth = this.depth + 1;
            child.refreshTree();
        }
    }

    public boolean isSameType(SemanticBlock patternBlock) {
        if (this.depth != patternBlock.depth) return false;
        if (!(this.uiRoot.getClassName().toString().equals(uiRoot.getClassName().toString()))) return false;
        SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(this.uiRoot);
        if (specialNodeDescriber.calNodeDistanceToDescriber(patternBlock.uiRoot)>0.5) return false;
        return true;
    }
}
