package com.abh80.smartedge;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Pair;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class AccessibilityNodeInfoRecord {
    public static final String TAG = "AccessibilityNodeInfoRecord";

    private static Method getZMethod;
    private static Method getAlphaMethod;

    public static int AREATHRESHOLD = 350000;

    static {
        try {
            getZMethod = AccessibilityNodeInfo.class.getDeclaredMethod("getZ");
            getZMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            getZMethod = null;
        }

        try {
            getAlphaMethod = AccessibilityNodeInfo.class.getDeclaredMethod("getAlpha");
            getAlphaMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            getAlphaMethod = null;
        }
    }

    public static AccessibilityNodeInfoRecord buildTree(AccessibilityNodeInfo root){
        if(root == null){
            Log.i("warning", "buildTree: null root");
            return null;
        }
        AccessibilityNodeInfoRecord res = new AccessibilityNodeInfoRecord(root, null, 0);
        removeInvisibleChildrenInList(res);
        res.refreshAbsoluteOriId();
        res.ignoreUselessChild(false);

        removeSiblingsCoveredUseDrawingOrder(res);
        refreshIsInStaticRegion(res);
        res.refreshIndex(0);
        res.refreshAbsoluteId();
        return res;
    }

    public static AccessibilityNodeInfoRecord buildTree(){
        //建立了一个虚拟根节点
        RecordService server = RecordService.self;
        List<AccessibilityWindowInfo> windows = server.getWindows();
        List<AccessibilityWindowInfo> appWindow = new ArrayList<>();
        for(AccessibilityWindowInfo w: windows){
            if(w == null){
                continue;
            }
            if(w.getType() != AccessibilityWindowInfo.TYPE_APPLICATION){
                w.recycle();
                continue;
            }

            appWindow.add(w);
        }
        if(windows.isEmpty()){
            return  null;
        }
        AccessibilityNodeInfoRecord res = new AccessibilityNodeInfoRecordFromFile();
        for(AccessibilityWindowInfo w: appWindow){
            res.children.add(new AccessibilityNodeInfoRecord(w.getRoot(), res, 1));
        }
        removeInvisibleChildrenInList(res);
        res.refreshAbsoluteOriId();
        res.ignoreUselessChild(false);
        // removeSiblingsCoveredUseDrawingOrder(res);
        refreshIsInStaticRegion(res);
        res.refreshIndex(0);
        res.refreshAbsoluteId();
        return res;
    }

    public static void removeSiblingsCoveredUseDrawingOrder(AccessibilityNodeInfoRecord root){
        List<Pair<Integer, AccessibilityNodeInfoRecord>> childrenNodesAndOrder =
                new ArrayList<>(root.children).stream().map(n->new Pair<>(n.getDrawingOrder(), n))
                        .sorted((o1, o2) -> - Integer.compare(o1.first, o2.first)) // 降序
                        .collect(Collectors.toList());
        boolean sameOrderFound = false;
        for(int i = 1; i < childrenNodesAndOrder.size(); ++i){
            if(childrenNodesAndOrder.get(i).first.equals(childrenNodesAndOrder.get(i - 1).first)){
                sameOrderFound = true;
                break;
            }
        }
        if(!sameOrderFound){
            // 检查是否需要删除节点
            Set<AccessibilityNodeInfoRecord> nodesToRemove = new HashSet<>();
            List<Rect> nodesRectNotRemove = new ArrayList<>();
            for(int i = 0; i < childrenNodesAndOrder.size(); ++ i){
                Pair<Integer, AccessibilityNodeInfoRecord> p = childrenNodesAndOrder.get(i);
                AccessibilityNodeInfoRecord crt = p.second;
                Rect crtRect = new Rect();
                crt.getBoundsInScreen(crtRect);
                boolean containThisFound = false;
                for(Rect r: nodesRectNotRemove){
                    if(r.contains(crtRect)){
                        containThisFound = true;
                        break;
                    }
                }
                if(containThisFound){
                    nodesToRemove.add(crt);
                } else {
                    nodesRectNotRemove.add(crtRect);
                }
            }

            root.children.removeAll(nodesToRemove);
        }
        for(AccessibilityNodeInfoRecord c: root.children){
            removeSiblingsCoveredUseDrawingOrder(c);
        }
    }

    public static AccessibilityNodeInfoRecord buildOriginalTree(AccessibilityNodeInfo root){
        if(root == null){
            Log.i("warning", "buildTree: null root");
            return null;
        }
        AccessibilityNodeInfoRecord res = new AccessibilityNodeInfoRecord(root, null, 0);
        return res;
    }


    public static void removeInvisibleChildrenInList(AccessibilityNodeInfoRecord crtNode){
        if(crtNode.isScrollable()
                || crtNode.getClassName().toString().contains("RecyclerView")
                || crtNode.getClassName().toString().contains("GridView")
                || crtNode.getClassName().toString().contains("ListView")
                || crtNode.getClassName().toString().contains("ScrollView")){

            int countWidthValid = 0;
            int oriSize = crtNode.getChildCount();
            for(int i = crtNode.getChildCount() - 1; i >= 0; -- i){
                Rect r = new Rect();
                crtNode.getChild(i).getBoundsInScreen(r);
                if(r.height() <= 0 || r.width() <= 0){
                    clearSubTree(crtNode.getChild(i));
                    crtNode.children.remove(i);
                } else if(r.width() > 0){
                    countWidthValid += 1;
                }
            }
            if(countWidthValid == 1 && oriSize != 1){
                crtNode.isDynamicEntrance = false;
            } else if(countWidthValid == 1) {
                // 判断唯一存在的子节点
                Rect thisRect = new Rect();
                crtNode.getBoundsInScreen(thisRect);

                Rect childRect = new Rect();
                crtNode.getChild(0).getBoundsInScreen(childRect);
                int thisArea  = thisRect.width() * thisRect.height();
                int childArea = childRect.width() * childRect.height();
                crtNode.isDynamicEntrance = childArea < thisArea * 2 / 3;  // 仅有一个子节点，并且这个子节点过分大大时候，不认为是一个有效的动态入口
            } else {
                crtNode.isDynamicEntrance = true;
            }
        }
        for(AccessibilityNodeInfoRecord child: crtNode.children){
            removeInvisibleChildrenInList(child);
        }

    }

    public static void refreshIsInStaticRegion(AccessibilityNodeInfoRecord crtNode){
        if(crtNode.parent == null){
            crtNode.isInStaticRegion = true;
        } else {
            AccessibilityNodeInfoRecord parent = crtNode.parent;
            if(parent.isDynamicEntrance || !parent.isInStaticRegion){
                crtNode.isInStaticRegion = false;
            } else {
                crtNode.isInStaticRegion = true;
            }
        }
        for(AccessibilityNodeInfoRecord child: crtNode.children){
            refreshIsInStaticRegion(child);
        }
    }

    public static void clearTree(AccessibilityNodeInfoRecord root){
        clearSubTree(root);
    }

    private static void clearSubTree(AccessibilityNodeInfoRecord record){
        if(record == null || record.cleaned){
            return;
        }

        record.cleaned = true;
        for(AccessibilityNodeInfoRecord child: record.children){
            clearSubTree(child);
        }
        if(record.nodeInfo != null) {
            record.nodeInfo.recycle();
            record.nodeInfo = null;
        } else {
            // Utility.assertTrue(record instanceof AccessibilityNodeInfoRecordFromFile);
        }
        record.children.clear();
        record.parent = null;
    }

    boolean cleaned;
    public AccessibilityNodeInfo nodeInfo;
    public List<AccessibilityNodeInfoRecord> children;
    public AccessibilityNodeInfoRecord parent;
    public int index;
    public String absoluteId;
    public String oriAbsoluteId;
    public String dynamicId;
    public boolean isImportant;
    public boolean isValid;
    public int depth;
    public List<AccessibilityNodeInfoRecord> uselessChildren;

    public String allTexts;
    public List<String> allTextsSplit = null;
    public String allContents;
    public List<String> allContentsSplit = null;
    public boolean isInStaticRegion;
    public boolean isDynamicEntrance;

    public CharSequence activityName;

    public AccessibilityNodeInfoRecord blockRoot;

    public int findNodeMethod = 0;

    public int findMethod = 0; //默认是specialNodeDescriber里的查找方法
    public int considerPara = 0;

    AccessibilityNodeInfoRecord(){

    }

    AccessibilityNodeInfoRecord(AccessibilityNodeInfo nodeInfo, AccessibilityNodeInfoRecord parent, int index) {
        this.nodeInfo = nodeInfo;
        cleaned = false;
        /*if(parent == null && MainService.instance != null){
            activityName = MainService.instance.getCurrentActivityName();
        }*/
        this.isDynamicEntrance = false;
        /*if(this.nodeInfo == null){
            Log.i("warning", "AccessibilityNodeInfoRecord: null node info");
        }*/
        this.children = new ArrayList<>();
        this.uselessChildren = new ArrayList<>();
        this.parent = parent;
        this.index = index;
        if(nodeInfo != null) {
            for (int i = 0; i < nodeInfo.getChildCount(); ++i) {
                AccessibilityNodeInfo crtNode = nodeInfo.getChild(i);
                if (crtNode == null) {
                    continue;
                }
                children.add(new AccessibilityNodeInfoRecord(crtNode, this, i));

            }
        }
    }

    public boolean ignoreUselessChild(boolean isForceUseless){
        /*if(getClassName() != null && getClassName().toString().contains("WebView")){
            // 删除所有的 webview
            children.clear();
            return true;
        }*/


        isImportant = false;
        //boolean isRefresh = getViewIdResourceName() != null && Objects.equals("uik_refresh_header", getViewIdResourceName().toString());
        boolean isRefresh = false;
        for(AccessibilityNodeInfoRecord child: children){
            Rect r = new Rect();
            child.getBoundsInScreen(r);
            if(r.width() <= 0 || r.height() <= 0){
                child.isImportant = false;
            } else {
                if(child.ignoreUselessChild(isRefresh)){
                    isImportant = true;
                }
            }
        }

        if(!isImportant){
            isImportant = isClickable() || isCheckable() || isScrollable() || isEditable()
                    || isLongClickable() || (getText() != null && getText().length() > 0)
                    || (getContentDescription() != null && getContentDescription().length() > 0)
                    /*|| (getViewIdResourceName() != null && getViewIdResourceName().length() > 0)*/;
        }

        isImportant = isImportant && !isForceUseless && !isRefresh;
        // 把所有不重要的节点从 children 里转移到 uselessChild 里
        uselessChildren.addAll(children);
        for(AccessibilityNodeInfoRecord child: children){
            if(child.isImportant){
                uselessChildren.remove(child);
            }
        }

        children.removeAll(uselessChildren);

        return isImportant;
    }


    public void refreshIndex(int newIndex){
        // 修改了树之后进行更新
        index = newIndex;
        for(int i = 0; i < getChildCount(); ++ i){
            getChild(i).refreshIndex(i);
        }
    }

    public void refreshAbsoluteOriId(){
        if(parent == null || Utility.isCSEqual("fake.root", parent.getClassName())){
            oriAbsoluteId = String.valueOf(getClassName());
        } else {
            oriAbsoluteId = parent.oriAbsoluteId + "|" + String.valueOf(index) + ";" + String.valueOf(getClassName());
        }
        for(AccessibilityNodeInfoRecord child: children){
            child.refreshAbsoluteOriId();
        }
    }

    public void refreshAbsoluteId(){
        if(parent == null){
            absoluteId = getClassName().toString();
            dynamicId = getClassName().toString();
        } else {
            absoluteId = parent.absoluteId + "|" + String.valueOf(index) + ";" + getClassName().toString();
            dynamicId = parent.dynamicId + "|" + (parent.isDynamicEntrance? "*": index + ';') + getClassName();
        }
        for(AccessibilityNodeInfoRecord child: children){
            child.refreshAbsoluteId();
        }
    }

    public AccessibilityNodeInfoRecord getParent(){
        return parent;
    }

    public boolean isClickable(){
        return nodeInfo.isClickable();
    }

    public boolean isScrollable(){
        return nodeInfo.isScrollable();
    }

    public boolean isLongClickable(){
        return nodeInfo.isLongClickable();
    }

    public boolean isEditable(){
        return nodeInfo.isEditable();
    }

    public boolean isCheckable(){
        return nodeInfo.isCheckable();
    }

    public boolean isChecked(){
        return nodeInfo.isChecked();
    }

    public boolean isFocused(){
        return nodeInfo.isFocused();
    }

    public boolean isPassword(){
        return nodeInfo.isPassword();
    }

    public boolean isAccessibilityFocused(){
        return nodeInfo.isAccessibilityFocused();
    }

    public int getChildCount(){
        return children.size();
    }

    public AccessibilityNodeInfoRecord getChild(int index){
        return children.get(index);
    }

    public CharSequence getText(){
        return nodeInfo.getText();
    }

    public CharSequence getContentDescription(){
        return nodeInfo.getContentDescription();
    }

    public boolean performAction(int action){
        return nodeInfo.performAction(action);
    }

    public boolean performAction(int action, Bundle info){
        return nodeInfo.performAction(action, info);
    }

    public boolean isEnabled(){
        return nodeInfo.isEnabled();
    }
    public CharSequence getClassName(){
        if (nodeInfo != null)
            return nodeInfo.getClassName();
        return null;
    }

    public AccessibilityWindowInfo getWindow(){
        return nodeInfo.getWindow();
    }
//    public int getWindowType() {
//        if (nodeInfo == null) return 0; //fake.root
//        AccessibilityWindowInfo window = nodeInfo.getWindow();
//        if (window == null) return 0;
//        return window.getType();
//    }
//    public String getWindowTypeString() {
//        return windowTypeToString(getWindowType());
//    }
//    public static String windowTypeToString(int windowType) {
//        switch (windowType) {
//            case 0:
//                return "TYPE_FAKE_ROOT";
//            case AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY:
//                return "TYPE_ACCESSIBILITY_OVERLAY";
//            case AccessibilityWindowInfo.TYPE_APPLICATION:
//                return "TYPE_APPLICATION";
//            case AccessibilityWindowInfo.TYPE_INPUT_METHOD:
//                return "TYPE_INPUT_METHOD";
//            case AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER:
//                return "TYPE_SPLIT_SCREEN_DIVIDER";
//            case AccessibilityWindowInfo.TYPE_SYSTEM:
//                return "TYPE_SYSTEM";
//            default:
//                return "UNKNOWN_TYPE:" + windowType;
//        }
//    }
    public void getBoundsInScreen(Rect r){
        if(nodeInfo == null){
            r.left = r.right = r.bottom = r.top = 0;
            return;
        }
        nodeInfo.getBoundsInScreen(r);
    }

    public List<AccessibilityNodeInfoRecord> findAccessibilityNodeInfosByText(String str){
        List<AccessibilityNodeInfoRecord> res = new ArrayList<>();
        if(Objects.equals(getText().toString(), str)){
            res.add(this);
        }

        for(AccessibilityNodeInfoRecord child: children){
            res.addAll(child.findAccessibilityNodeInfosByText(str));
        }

        return res;
    }

    public boolean isSelected(){
        return nodeInfo.isSelected();
    }

    public CharSequence getPackageName(){
        return nodeInfo.getPackageName();
    }

/*    public int getDrawingOrder(){
        return nodeInfo.getDrawingOrder();
    }*/

    public CharSequence getViewIdResourceName(){
        return nodeInfo.getViewIdResourceName();
    }

    public boolean isVisibleToUser(){
        return nodeInfo.isVisibleToUser();
    }

    public boolean isFocusable(){
        return nodeInfo.isFocusable();
    }

    public boolean isDismissable(){
        return nodeInfo.isDismissable();
    }

    public AccessibilityNodeInfo getNodeInfo(){
        return nodeInfo;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        if(this.nodeInfo != null && ((AccessibilityNodeInfoRecord) obj).nodeInfo != null){
            return this.nodeInfo.equals(((AccessibilityNodeInfoRecord) obj).nodeInfo);
        } else if(this.nodeInfo == null && ((AccessibilityNodeInfoRecord) obj).nodeInfo == null){
            return this == obj;
        } else {
            return false;
        }

    }

    public int getIndex(){
        return index;
    }

    public List<AccessibilityNodeInfoRecord> getChildren() {
        return children;
    }


    public List<AccessibilityNodeInfoRecord> findNodeByViewIdResourceName(String str){
        List<AccessibilityNodeInfoRecord> res = new ArrayList<>();
        String crtId = getViewIdResourceName() == null? "": getViewIdResourceName().toString();
        if(Objects.equals(crtId, str)){
            res.add(this);
        }
        for(AccessibilityNodeInfoRecord child: children){
            res.addAll(child.findNodeByViewIdResourceName(str));
        }
        return res;

    }

    public boolean isMeaningful(){
        if(isCheckable() || isEditable() || isScrollable() || isLongClickable() || isClickable()){
            return true;
        }
        if(getText() != null && getText().length() > 0){
            return true;
        }
        if(getContentDescription() != null && getContentDescription().length() > 0){
            return true;
        }
        if(getViewIdResourceName() != null && getViewIdResourceName().length() > 0){
            return true;
        }
        if(children.size() != 1){
            return true;
        }
        return false;
    }

    public Pair<AccessibilityNodeInfoRecord, Integer> moveToMeaningfulChild(){
        AccessibilityNodeInfoRecord crtNode = this;
        int countSkipNum = 0;
        while (!crtNode.isMeaningful()){
            countSkipNum += 1;
            crtNode = crtNode.getChild(0);
        }
        return new Pair<>(crtNode, countSkipNum);
    }

    public String getAllTexts() {
        if (allTexts != null)
            return allTexts;
        allTexts = getText() == null? "": getText().toString();
        for(AccessibilityNodeInfoRecord child: children){
            allTexts += child.getAllTexts();
        }
        return allTexts;
    }

    public List<String> getAllSplitTexts() {
        if (allTextsSplit != null) return allTextsSplit;
        allTextsSplit = new ArrayList<>();
        CharSequence crtText = getText();
        if (crtText != null) {
            if (!(crtText.toString().equals("")))
                allTextsSplit.add(crtText.toString());
        }
        for(AccessibilityNodeInfoRecord child: children){
            allTextsSplit.addAll(child.getAllSplitTexts());
        }
        return allTextsSplit;
    }

    public String getAllContents(){
        if (allContents != null)
            return allContents;
        allContents = getContentDescription() == null? "": getContentDescription().toString();
        for(AccessibilityNodeInfoRecord child: children){
            allContents += child.getAllContents();
        }
        return allContents;
    }

    public List<String> getAllContentsSplit() {
        if (allContentsSplit != null) return allContentsSplit;
        allContentsSplit = new ArrayList<>();
        CharSequence crtContent = getContentDescription();
        if (crtContent != null) {
            if (!(crtContent.toString().equals("")))
                allContentsSplit.add(crtContent.toString());
        }
        for(AccessibilityNodeInfoRecord child: children){
            allContentsSplit.addAll(child.getAllContentsSplit());
        }
        return allContentsSplit;
    }

    public AccessibilityNodeInfoRecord getNodeByOriAbsoluteId(String oriAbsoluteId){
        Utility.assertTrue(this.parent == null);
        Queue<AccessibilityNodeInfoRecord> nodeQueue = new LinkedList<>();
        nodeQueue.add(this);
        while (!nodeQueue.isEmpty()){
            AccessibilityNodeInfoRecord node = nodeQueue.poll();
            if(node == null){
                continue;
            }
            if(Objects.equals(node.oriAbsoluteId, oriAbsoluteId)){
                return node;
            }
            nodeQueue.addAll(node.children);
        }
        return this.getNodeByRelativeId(oriAbsoluteId);
        //return null;
    }

    public AccessibilityNodeInfoRecord getNodeByRelativeId(String relativeId){
        String[] subIdList = relativeId.split(";");
        AccessibilityNodeInfoRecord crtNode = this;
        for(int i = 0; i < subIdList.length - 1; ++ i){
            String subId = subIdList[i];
            String[] subIdSplited = subId.split("\\|");
            if(!crtNode.getClassName().toString().equals(subIdSplited[0])){
                return null;
            }

            int intendedIndex = Integer.valueOf(subIdSplited[1]);
            AccessibilityNodeInfoRecord targetChild = null;
            for(AccessibilityNodeInfoRecord child: crtNode.children){
                if (child.index == intendedIndex){
                    targetChild = child;
                    break;
                }
            }

            if(targetChild == null){
                return null;
            }
            crtNode = targetChild;
        }
        if(!crtNode.getClassName().toString().equals(subIdList[subIdList.length - 1])){
            return null;
        }
        return crtNode;
    }

    public String getIdRelatedTo(AccessibilityNodeInfoRecord node){
        if(node == null || node.absoluteId == null){
            Log.w(TAG, "getIdRelatedTo: null node or id");
            return null;
        }
        if(absoluteId.startsWith(node.absoluteId)){
            String postFix = absoluteId.substring(node.absoluteId.length());
            return node.getClassName() + postFix;
        }

        return null;
    }

    public AccessibilityNodeInfoRecord getParentSupportInteract(){
        AccessibilityNodeInfoRecord crtNode = this;
        while (crtNode != null){
            if (crtNode.isClickable() || crtNode.isLongClickable()
                    || crtNode.isEditable() || crtNode.isScrollable() || crtNode.isCheckable()){
                // 要求这个节点及其子树之外 没有包含任何可以交互、或者包含文本的节点
                boolean otherImportantNodeFound = false;
                Queue<AccessibilityNodeInfoRecord> q = new LinkedList<>(crtNode.children);
                while (!q.isEmpty()){
                    AccessibilityNodeInfoRecord nodeFromQueue = q.poll();
                    if(nodeFromQueue == this || nodeFromQueue == null){
                        continue;
                    }
                    if(nodeFromQueue.getText() != null && nodeFromQueue.getText().length() > 0){
                        otherImportantNodeFound = true;
                        break;
                    }

                    if(nodeFromQueue.getContentDescription() != null && nodeFromQueue.getContentDescription().length() > 0){
                        otherImportantNodeFound = true;
                        break;
                    }

                    if(nodeFromQueue.isClickable() || nodeFromQueue.isLongClickable()
                            || nodeFromQueue.isEditable() || nodeFromQueue.isScrollable() || nodeFromQueue.isCheckable()){
                        otherImportantNodeFound = true;
                        break;
                    }
                    q.addAll(nodeFromQueue.children);
                }
                if(otherImportantNodeFound){
                    return null;
                }
                return crtNode;
            }
            crtNode = crtNode.parent;
        }

        return null;
    }

    public boolean isInfoOrInteract(){
        if(isCheckable() || isEditable() || isScrollable() || isLongClickable() || isClickable()){
            return true;
        }
        if(getText() != null && getText().length() > 0){
            return true;
        }
        if(getContentDescription() != null && getContentDescription().length() > 0){
            return true;
        }

        return false;
    }

    public List<AccessibilityNodeInfoRecord> getAllInfoAndInteractNode(){
        List<AccessibilityNodeInfoRecord> result = new ArrayList<>();
        Utility.Visitor.visit(this, new Utility.Visitor() {
            @Override
            public void init() {

            }

            @Override
            public void visitNode(AccessibilityNodeInfoRecord record) {
                boolean important = record.isClickable() || record.isEditable()
                        || record.isScrollable() || record.isLongClickable() || record.isCheckable()
                        || !Utility.isEmptyCS(record.getText())
                        || !Utility.isEmptyCS(record.getContentDescription());
                if(important){
                    result.add(record);
                }
            }

            @Override
            public boolean childrenNotAddIf(AccessibilityNodeInfoRecord record) {
                return false;
            }
        });
        return result;
    }

    public static final int MAX_TEXT_LENGTH_FOR_NODE = 10;
    public int getAreaByInfoAndInteractNode(){
        List<AccessibilityNodeInfoRecord> nodes = getAllInfoAndInteractNode();
        if(nodes.isEmpty()){
            return 0;
        }
        Rect r = new Rect();
        int minTop = Integer.MAX_VALUE;
        int minLeft = Integer.MAX_VALUE;
        int maxRight = Integer.MIN_VALUE;
        int maxBottom = Integer.MIN_VALUE;
        for(AccessibilityNodeInfoRecord node: nodes){
            node.getBoundsInScreen(r);
            minTop = Math.min(minTop, r.top);
            minLeft = Math.min(minLeft, r.left);
            maxRight = Math.max(maxRight, r.right);
            maxBottom = Math.max(maxBottom, r.bottom);
        }

        StringBuilder allText = new StringBuilder();
        int countNodeWithText = 0;
        for(AccessibilityNodeInfoRecord node: nodes){
            if(node.getText() != null && node.getText().length() > 0){
                allText.append(node.getText());
                countNodeWithText += 1;
            }
        }

        double ratio = 1;
        if(countNodeWithText > 0){
            int aveTextLength = allText.length() / countNodeWithText;
            ratio = Math.max(MAX_TEXT_LENGTH_FOR_NODE, aveTextLength) / (double)MAX_TEXT_LENGTH_FOR_NODE;
        }
        return (int) (Math.max(0, (maxRight - minLeft) * (maxBottom - minTop)) / ratio);
    }

    private AccessibilityNodeInfoRecord rootCache = null;
    public AccessibilityNodeInfoRecord getRoot(){
        if(rootCache != null){
            return rootCache;
        }
        AccessibilityNodeInfoRecord crtNode = this;
        while (crtNode.parent != null){
            crtNode = crtNode.parent;
        }
        if (crtNode.getClassName().toString().contains("fake"))
            if (crtNode.children.size()>0) crtNode = crtNode.children.get(0);
        rootCache = crtNode;
        return crtNode;
    }

    public boolean maybeDynamicEntrance(){
        return isScrollable()
                || getClassName().toString().contains("RecyclerView")
                || getClassName().toString().contains("GridView")
                || getClassName().toString().contains("ListView")
                || getClassName().toString().contains("ScrollView");
    }

    public boolean isAncestorOf(AccessibilityNodeInfoRecord node){
        AccessibilityNodeInfoRecord crtNode = node;
        while (crtNode != null){
            if(crtNode == this){
                return true;
            }
            crtNode = crtNode.parent;
        }
        return false;
    }

    public CharSequence getActivityName(){
        return activityName;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if(parent == null){
            clearTree(this);
        }
    }

    private Boolean containsInfoOrInteractNodesCache = null;

    public boolean containsInfoOrInteractNodes(){
        if(containsInfoOrInteractNodesCache != null){
            return containsInfoOrInteractNodesCache;
        }
        for(AccessibilityNodeInfoRecord child: children){
            if(child.isInfoOrInteract() || child.containsInfoOrInteractNodes()){
                containsInfoOrInteractNodesCache = true;
                return true;
            }
        }

        containsInfoOrInteractNodesCache = false;
        return false;
    }

    private Rect boundCache = null;
    public int getCenterAtWidth(){
        if(boundCache == null){
            boundCache = new Rect();
            getBoundsInScreen(boundCache);
        }
        return boundCache.centerX();
    }

    public int getArea(){
        if(boundCache == null){
            boundCache = new Rect();
            getBoundsInScreen(boundCache);
        }
        return boundCache.width() * boundCache.height();
    }

    public int getCenterAtHeight(){
        if(boundCache == null){
            boundCache = new Rect();
            getBoundsInScreen(boundCache);
        }
        return boundCache.centerY();
    }

    public int getWidth(){
        if(boundCache == null){
            boundCache = new Rect();
            getBoundsInScreen(boundCache);
        }
        return boundCache.width();
    }

    public int getHeight(){
        if(boundCache == null){
            boundCache = new Rect();
            getBoundsInScreen(boundCache);
        }
        return boundCache.height();
    }

    public int getBottom(){
        if(boundCache == null){
            boundCache = new Rect();
            getBoundsInScreen(boundCache);
        }
        return boundCache.bottom;
    }

    public int getTop(){
        if(boundCache == null){
            boundCache = new Rect();
            getBoundsInScreen(boundCache);
        }
        return boundCache.top;
    }

    public int getLeft(){
        if(boundCache == null){
            boundCache = new Rect();
            getBoundsInScreen(boundCache);
        }
        return boundCache.left;
    }

    public int getRight(){
        if(boundCache == null){
            boundCache = new Rect();
            getBoundsInScreen(boundCache);
        }
        return boundCache.right;
    }

    public AccessibilityNodeInfoRecord getClickableContainsThis() {
        AccessibilityNodeInfoRecord crt = this;
        while (crt != null){
            if(crt.isClickable()){
                return crt;
            }
            crt = crt.parent;
        }

        return null;
    }

    public AccessibilityNodeInfoRecord getBlockRoot() {
        if(blockRoot == null){
            blockRoot = Utility.getDynamicItemRootForNode(this);
            if(blockRoot == null){
                blockRoot = getRoot();
            }
        }
        return blockRoot;
    }

    public Set<Pair<String, AccessibilityNodeInfoRecord>> getAllTextNodes(Set<AccessibilityNodeInfoRecord> stopNodes) {
        Set<Pair<String, AccessibilityNodeInfoRecord>> res = new HashSet<>();
        if (stopNodes != null && stopNodes.contains(this)) return res;
        if (this.getText()!="") res.add(new Pair<String, AccessibilityNodeInfoRecord>(this.getText().toString(),this));
        for (AccessibilityNodeInfoRecord child : this.children) res.addAll(child.getAllTextNodes(stopNodes));
        return res;
    }

    public Set<String> getAllWords() {
        Set<String> res = new HashSet<>();
        if (this.getText()!="") res.add(this.getText().toString());
        for (AccessibilityNodeInfoRecord child : this.children) res.addAll(child.getAllWords());
        return res;
    }

    public float getAlpha(){
        if(nodeInfo == null || getAlphaMethod == null){
            return 1.0f;
        }
        try {
            return (float) getAlphaMethod.invoke(nodeInfo);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return 1.0f;
        }

    }

    public float getZ(){
        if(nodeInfo == null || getZMethod == null){
            return 0.0f;
        }
        try {
            return (float) getZMethod.invoke(nodeInfo);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return 0.0f;
        }
    }

    public int getDrawingOrder(){
        if(nodeInfo == null){
            return 0;
        }

        return nodeInfo.getDrawingOrder();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public CharSequence getHintText(){
        if(nodeInfo == null){
            return "";
        }

        return nodeInfo.getHintText();
    }

    public List<AccessibilityNodeInfoRecord> getTextNodesNextTo(boolean leftToThis){
        List<AccessibilityNodeInfoRecord> result = new ArrayList<>();
        Queue<AccessibilityNodeInfoRecord> nodeQueue = new LinkedList<>();
        nodeQueue.add(getRoot());
        while (!nodeQueue.isEmpty()){
            AccessibilityNodeInfoRecord crt = nodeQueue.poll();
            if(crt == null){
                continue;
            }
            if(!Utility.isEmptyCS(crt.getText())){
                int crtXCenter = (crt.getTop() + crt.getBottom()) / 2;
                if(crtXCenter > getTop() && crtXCenter < getBottom()){
                    // same line
                    if(leftToThis) {
                        if(crt.getRight() <= getLeft()){
                            result.add(crt);
                        }
                    } else {
                        if(crt.getLeft() >= getRight()){
                            result.add(crt);
                        }
                    }
                }
            }

            nodeQueue.addAll(crt.children);
        }

        return result;
    }

    public boolean getAllImportantNodes() {
        if (parent != null) {
            depth = parent.depth + 1;
        }
        else {
            depth = 0;
        }
        isImportant = false;
        isValid = false;
        for (AccessibilityNodeInfoRecord child : children) {
            if (child.getAllImportantNodes()) {
                isValid = true;
            }
        }
        Rect r = new Rect();
        this.getBoundsInScreen(r);
        if ((this.isTextNode()) && (r.height()>=10)) {
            this.isImportant = true;
        }
        int area = r.height()*r.width();
        if (((this.isClickable() || this.isCheckable()) && (area<AREATHRESHOLD)) ||
                this.isScrollable() || this.isSelected() || this.isEditable()) {
            this.isImportant = true;
        }
        if (r.bottom<0 || r.top<0 || r.left<0 || r.right<0) {
            this.isImportant = false;
        }
        if (r.width()<5) {
            this.isImportant = false;
        }
        if (r.height()<5) {
            this.isImportant = false;
        }
        /*if (!this.isVisibleToUser()) {
            this.isImportant = false;
        }*/
        if (this.isImportant) {
            this.isValid = true;
        }
        return this.isValid;
    }

    public boolean isTextNode() {
        if (this.getText() == null) return false;
        if ((this.getClassName().toString().contains("TextView")) && (this.getText().length()==0))
            return false;
        if (!(this.getClassName().toString().contains("Image"))) {
            if (this.getText().length()>0) return true;
            if (this.getContentDescription() != null)
                if (this.getContentDescription().length()!=0) return true;
        }
        return false;
    }


    public JSONObject getNodeInfoJson(boolean onlyOneNode) {
        Block block;
        if (!onlyOneNode) block = this.buildFullTree(true);
        else block = new Block(this);
        if (block == null) block = new Block(this);
        return block.convertBlockTreeToJson();
    }

    Block buildFullTree(boolean onlyImportantNodes) {
        if (onlyImportantNodes) {
            if (!this.isValid) return null;
        }
        Block res = new Block(this);
        for (AccessibilityNodeInfoRecord child : this.children) {
            Block childBlock = child.buildFullTree(onlyImportantNodes);
            if (childBlock == null) continue;
            res.childrenBlocks.add(childBlock);
        }
        if (onlyImportantNodes && !this.isImportant && res.childrenBlocks.size()<2)  {
            if (res.childrenBlocks.size() == 0) return null;
            res = res.childrenBlocks.get(0);
        }
        if (onlyImportantNodes && res.childrenBlocks.size()==1 && !res.childrenBlocks.get(0).uiNode.isImportant) {
            res.childrenBlocks = res.childrenBlocks.get(0).childrenBlocks;
        }
        return res;
    }

    public boolean isOverlapped(AccessibilityNodeInfoRecord otherNode) {
        Rect bound1 = new Rect(), bound2 = new Rect();
        this.getBoundsInScreen(bound1);
        otherNode.getBoundsInScreen(bound2);
        if (bound1.left>=bound2.right) return false;
        if (bound1.right<=bound2.left) return false;
        if (bound1.top>=bound2.bottom) return false;
        if (bound1.bottom<=bound2.top) return false;
        return true;
    }

    public boolean hasBeenCoverd(AccessibilityNodeInfoRecord otherNode) {
        Rect bound1 = new Rect(), bound2 = new Rect();
        this.getBoundsInScreen(bound1);
        otherNode.getBoundsInScreen(bound2);
        if (bound2.contains(bound1)) return true;
        return false;
    }
}
