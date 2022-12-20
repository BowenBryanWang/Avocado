package com.abh80.smartedge.xdevice;

import android.graphics.Rect;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.abh80.smartedge.AccessibilityNodeInfoRecord;
import com.abh80.smartedge.Utility;

public class ImportantStructureUtility {
    private static int eps = 5000;
    private static int deltaTop = 100;

    static {
        rootToTitleCache = new HashMap<>();
        nodeToContainsETHash = new HashMap<>();
    }

    public static AccessibilityNodeInfoRecord getTitle(AccessibilityNodeInfoRecord infoRecord){
        Queue<AccessibilityNodeInfoRecord> nodeInfoQueue = new LinkedList<>();
        Rect windowRect = new Rect();
        infoRecord.getBoundsInScreen(windowRect);
        int maxHeightAllow = windowRect.height() / 3 /* * 2 */+ windowRect.top;

        List<AccessibilityNodeInfoRecord> nodesWithTextAllButNoInteract = new ArrayList<>();
        List<AccessibilityNodeInfoRecord> allNodesWithText = new ArrayList<>();

        nodeInfoQueue.add(infoRecord);
        Rect r = new Rect();
        Map<String, Integer> idToNum = new HashMap<>();

        List<Rect> editTextBoundInPage = new ArrayList<>();

        // int minTop = Integer.MAX_VALUE / 2;
        while (!nodeInfoQueue.isEmpty()){
            AccessibilityNodeInfoRecord crt = nodeInfoQueue.poll();
            if(crt == null){
                continue;
            }

            if(crt.isEditable() || String.valueOf(crt.getClassName()).contains("EditText")){
                Rect editBound = new Rect();
                crt.getBoundsInScreen(editBound);
                editTextBoundInPage.add(editBound);
            }

            if(!Utility.isEmptyCS(crt.getViewIdResourceName())){
                String resourceId = String.valueOf(crt.getViewIdResourceName());
                idToNum.put(resourceId, 1 + idToNum.getOrDefault(resourceId, 0));
            }
            /*if(crt.isInStaticRegion && crt.isInfoOrInteract() && !crt.containsInfoOrInteractNodes() && crt.onlyOverlapByParents()){
                crt.getBoundsInScreen(tmp);
                if(tmp.height() > 0 && tmp.top >= 0){
                    // minTop = Math.min(tmp.top, minTop);
                    if(minTop > tmp.top){
                        minTop = tmp.top;
                    }
                }

            }*/
            if(!Utility.isEmptyCS(crt.getText())){
                allNodesWithText.add(crt);
            }
            if(!crt.containsInfoOrInteractNodes() && !Utility.isEmptyCS(crt.getText())
                    && Utility.countTextLength(String.valueOf(crt.getText())) > 0){
                String className = String.valueOf(crt.getClassName());
                if(!className.contains("Button") && !className.contains("EditText")){
                    crt.getBoundsInScreen(r);
                    if(r.width() > 0 && r.height() > 0){
                        nodesWithTextAllButNoInteract.add(crt);
                    }
                }
            }
            if(isDynamicEntranceForTitle(crt)){
                continue;
            }

            for(int i = 0; i < crt.getChildCount(); ++ i){
                AccessibilityNodeInfoRecord child = crt.getChild(i);
                if(child != null){
                    nodeInfoQueue.add(child);
                }
            }
        }


        Map<String, List<AccessibilityNodeInfoRecord>> idToNodes = new HashMap<>();
        for(AccessibilityNodeInfoRecord node: nodesWithTextAllButNoInteract){
            // String idNoDigits = Utility.deleteDigitsInString(node.absoluteId);
            String dynamicId = node.dynamicId;
            if(!idToNodes.containsKey(dynamicId)){
                idToNodes.put(dynamicId, new ArrayList<AccessibilityNodeInfoRecord>());
            }
            idToNodes.get(dynamicId).add(node);
        }

        Rect rectForMaxSize = new Rect();
        AccessibilityNodeInfoRecord title = null;
        double maxSizePerLength = 0.0;

        for(List<AccessibilityNodeInfoRecord> nodesWithText: idToNodes.values()){
            if(nodesWithText.size() > 1){
                AccessibilityNodeInfoRecord example = nodesWithText.get(0);
                if(!example.isInStaticRegion){
                    continue;
                }
            }
            for(AccessibilityNodeInfoRecord nodeWithText: nodesWithText){
                nodeWithText.getBoundsInScreen(r);
                boolean justUpdateRect = false;
                if(r.top >= maxHeightAllow /*|| r.top >= minTop + deltaTop*/){
                    // 处于窗口太下方
                    continue;
                }

                // 如果同行中存在edit text，认为两者之间有关联的可能性更大一些
                boolean editTextSameLineFound = false;
                for(Rect editRect: editTextBoundInPage){
                    if(r.centerY() > editRect.top && r.centerY() < editRect.bottom){
                        editTextSameLineFound = true;
                        break;
                    }
                }
                if(editTextSameLineFound){
                    continue;
                }
                /*AccessibilityNodeInfoRecord notOverlappedAncestor = getLastAncestorSiblingsNotInsectIt(nodeWithText);
                int minHCenter = getMinHCenterInSubTree(notOverlappedAncestor);
                if(r.centerY() >= minHCenter + deltaTop){
                    continue;
                }*/

                if(!Utility.isEmptyCS(nodeWithText.getViewIdResourceName())){
                    String resourceId = String.valueOf(nodeWithText.getViewIdResourceName());
                    if(idToNum.getOrDefault(resourceId, 0) > 1){
                        continue;
                    }
                }
                double area = windowRect.width() /*r.width()*/ * r.height();  // 只在意高度
                double textLength = Utility.countTextLength(String.valueOf(nodeWithText.getText()));
                if(textLength == 0 || textLength > 15){ // too long ...
                    continue;
                }
                double areaPerWord = area / textLength;
                if(String.valueOf(nodeWithText.getViewIdResourceName()).toLowerCase().contains("title")){
                    areaPerWord *= 20;
                } else if(!nodeWithText.isInStaticRegion){
                    areaPerWord /= 10;
                }

                AccessibilityNodeInfoRecord clickableParent = nodeWithText.getClickableContainsThis();
                if(clickableParent != null){
                    //TODO: 动词直接忽略；名词除以10
                    areaPerWord /= 10;
                    /*List<String> posRes = LTP.getPosRes(String.valueOf(nodeWithText.getText()));
                    if(posRes == null || posRes.isEmpty() || !Objects.equals(LTP.TAG_VERB, posRes.get(0))){
                        areaPerWord /= 10;
                    } else {
                        // continue;
                        areaPerWord /= 10;
                        justUpdateRect = true;
                    }*/
                }

               /* // 该节点文本要显著大于所有左侧的节点的文本
                boolean largerFound = false;
                for(AccessibilityNodeInfoRecord otherTextNode: allNodesWithText){
                    if(otherTextNode == nodeWithText){
                        continue;
                    }
                    double otherNodeTextLength = Utility.countTextLength(String.valueOf(otherTextNode.getText()));
                    if(otherNodeTextLength == 0){
                        continue;
                    }
                    double areaPerWordForOther = otherTextNode.getWidth() * otherTextNode.getHeight() / otherNodeTextLength;
                    if(String.valueOf(otherTextNode.getViewIdResourceName()).toLowerCase().contains("title")){
                        areaPerWordForOther *= 2;
                    } else if(!otherTextNode.isInStaticRegion){
                        areaPerWordForOther /= 10;
                    }
                    if(otherTextNode.getCenterAtWidth() < nodeWithText.getCenterAtWidth() && otherTextNode.getCenterAtHeight() <= nodeWithText.getBottom()){
                        if(areaPerWordForOther > areaPerWord / 2){
                            largerFound = true;
                            break;
                        }
                    }

                }

                if(largerFound){
                    continue;
                }*/


                if(areaPerWord > (maxSizePerLength - eps)){
                    boolean needUpdateRect = (areaPerWord - maxSizePerLength >= eps)
                            || (r.left + r.top) < (rectForMaxSize.left + rectForMaxSize.top) || rectForMaxSize.isEmpty();
                    if(needUpdateRect){
                        rectForMaxSize.set(r);
                        if(!justUpdateRect){
                            title = nodeWithText;
                        } else {
                            title = null;
                        }
                    }
                    maxSizePerLength = Math.max(areaPerWord, maxSizePerLength);
                } else if(areaPerWord == maxSizePerLength){
                    rectForMaxSize.setEmpty();
                    title = null;
                }
            }
        }

        return title;
    }

    private static Map<AccessibilityNodeInfoRecord, String> rootToTitleCache;

    public static final String DYNAMIC_TITLE = "DYNAMIC_TITLE#";
    public static String getTitleText(AccessibilityNodeInfoRecord root){
        if(rootToTitleCache.containsKey(root)){
            return rootToTitleCache.get(root);
        }
        AccessibilityNodeInfoRecord titleNode = ImportantStructureUtility.getTitle(root);
        String title = null;
        if(titleNode == null){
            title = null;
        } else {
            //title = FunctionWordDict.getInstance().isNodeFunction(titleNode, true).first;
            title = Utility.isNodeFunction(titleNode, true).first; //TODO: 待集成FunctionWordDict
            if(title == null || title.isEmpty()){
                //title = FunctionWordDict.getInstance().isNodeFunction(titleNode, false).first;
                title = Utility.isNodeFunction(titleNode, false).first;//TODO: 待集成FunctionWordDict
            }
            if(title.isEmpty()){
                title = null;
            }

            if(title == null){
                title = DYNAMIC_TITLE + (titleNode.getText() != null? titleNode.getText(): titleNode.getContentDescription());
            }
        }

        rootToTitleCache.put(root, title);
        return title;
    }

    private static boolean isDynamicEntranceForTitle(AccessibilityNodeInfoRecord node){
        return (node.isScrollable()
                || String.valueOf(node.getClassName()).contains("GridView")
                || String.valueOf(node.getClassName()).contains("ListView"))
                /*&& ! String.valueOf(node.getClassName()).contains("ScrollView")*/
                && !String.valueOf(node.getClassName()).contains("ViewPager")
                && !String.valueOf(node.getClassName()).contains("RecyclerView")
                && !String.valueOf(node.getClassName()).toLowerCase().contains("webview")
                && !String.valueOf(node.getClassName()).toLowerCase().contains("webkit");
    }

    private static boolean isDynamicEntranceForShape(AccessibilityNodeInfoRecord node){
        return (node.isScrollable()
                || String.valueOf(node.getClassName()).contains("GridView")
                || String.valueOf(node.getClassName()).contains("ListView")
                || String.valueOf(node.getClassName()).contains("ScrollView")
                || String.valueOf(node.getClassName()).contains("RecyclerView"))
                && !String.valueOf(node.getClassName()).contains("ViewPager");
    }

    private static AccessibilityNodeInfoRecord getLastAncestorSiblingsNotInsectIt(AccessibilityNodeInfoRecord node){
        AccessibilityNodeInfoRecord crtNode = node.parent;
        AccessibilityNodeInfoRecord lastNode = node;

        while (crtNode != null){
            Rect r = new Rect();
            crtNode.getBoundsInScreen(r);
            for(AccessibilityNodeInfoRecord sib: getSiblingsList(crtNode)){
                Rect tmp = new Rect();
                sib.getBoundsInScreen(tmp);
                if(Rect.intersects(r, tmp)){
                    // insectFound = true;
                    return lastNode;
                }
            }

            lastNode = crtNode;
            crtNode = crtNode.parent;
        }

        return lastNode;
    }

    private static List<AccessibilityNodeInfoRecord> getSiblingsList(AccessibilityNodeInfoRecord node){
        AccessibilityNodeInfoRecord p = node.parent;
        if(p == null){
            return Collections.emptyList();
        }
        List<AccessibilityNodeInfoRecord> siblings = new ArrayList<>(p.children);
        siblings.remove(node);
        return siblings;
    }

    public static int getMinHCenterInSubTree(AccessibilityNodeInfoRecord node){
        int minTop = Integer.MAX_VALUE / 2;
        Queue<AccessibilityNodeInfoRecord> q = new LinkedList<>();
        q.add(node);
        while (!q.isEmpty()){
            AccessibilityNodeInfoRecord crt = q.poll();
            if(crt == null){
                continue;
            }
            if(crt.getHeight() > 0 && /*crt.isInfoOrInteract() && !crt.containsInfoOrInteractNodes()*/ !Utility.isEmptyCS(crt.getText())){
                minTop = Math.min(minTop, crt.getCenterAtHeight());
            }
            q.addAll(crt.children);
        }

        return minTop;
    }

    private static Map<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord> nodeToContainsETHash;
    public static AccessibilityNodeInfoRecord containsEditText(AccessibilityNodeInfoRecord root){
        if(nodeToContainsETHash.containsKey(root)){
            return nodeToContainsETHash.get(root);
        }
        Queue<AccessibilityNodeInfoRecord> nodeQueue = new LinkedList<>();
        nodeQueue.add(root);
        while (!nodeQueue.isEmpty()){
            AccessibilityNodeInfoRecord crt = nodeQueue.poll();
            if(crt == null){
                continue;
            }
            if(crt.isEditable() || String.valueOf(crt.getClassName()).contains("EditText")){
                nodeToContainsETHash.put(root, crt);
                return crt;
            }
            nodeQueue.addAll(crt.children);
        }
        nodeToContainsETHash.put(root, null);
        return null;
    }

    public static class PositionInfo {
        public boolean isLeft;
        public boolean isRight;
        public boolean isTop;
        public boolean isBottom;
        public boolean insideScrollable;
        public final double RATIO_IN_WIDTH = 1.0 / 3.0;
        public final double RATIO_IN_HEIGHT = 1.0 / 3.0;

        public final double RATIO_BIG = 1.0 / 3.0;  // 大于 1/3 即为big
        public final double RATIO_SMALL = 1.0 / 2.0; // 小于 1/2 即为small

        public boolean sizeBig;
        public boolean sizeSmall;

        public PositionInfo scrollableInfo;

        public PositionInfo(AccessibilityNodeInfoRecord node){
            AccessibilityNodeInfoRecord root = node.getRoot();
            Rect rootRect = new Rect();
            root.getBoundsInScreen(rootRect);
            double topTh = rootRect.top + rootRect.height() * RATIO_IN_HEIGHT;
            double bottomTh = rootRect.bottom - rootRect.height() * RATIO_IN_HEIGHT;
            double leftTh = rootRect.left + rootRect.width() * RATIO_IN_WIDTH;
            double rightTh = rootRect.right - rootRect.width() * RATIO_IN_WIDTH;

            Rect nodeRect = new Rect();
            node.getBoundsInScreen(nodeRect);
            isLeft = nodeRect.exactCenterX() < rightTh;
            isRight = nodeRect.exactCenterX() > leftTh;
            isTop = nodeRect.exactCenterY() < bottomTh;
            isBottom = nodeRect.exactCenterY() > topTh;

            insideScrollable = !node.isInStaticRegion;
            if(insideScrollable){
                AccessibilityNodeInfoRecord crt = node.parent;
                while (crt != null){
                    if(crt.isDynamicEntrance){
                        scrollableInfo = new PositionInfo(crt);
                        break;
                    }

                    crt = crt.parent;
                }
            }

            int totalArea = rootRect.width() * rootRect.height();

            Rect crtRect = new Rect();
            node.getBoundsInScreen(crtRect);
            int crtArea = crtRect.width() * crtRect.height();
            double sizeRatio = crtArea / (double) totalArea;
            sizeBig = sizeRatio > RATIO_BIG;
            sizeSmall = sizeRatio < RATIO_SMALL;
        }

        public PositionInfo(PositionInfo o){
            isLeft = o.isLeft;
            isRight = o.isRight;
            isTop = o.isTop;
            isBottom = o.isBottom;

            insideScrollable = o.insideScrollable;

            sizeBig = o.sizeBig;
            sizeSmall = o.sizeSmall;
            if(o.scrollableInfo != null){
                scrollableInfo = new PositionInfo(o.scrollableInfo);
            }
        }

        public boolean canMatch(PositionInfo o){
            if(o == null){
                return false;
            }

            if(!(sizeSmall == o.sizeSmall || sizeBig == o.sizeBig)){
                return false;
            }

            if(o.insideScrollable && insideScrollable && o.scrollableInfo != null && scrollableInfo != null){
                return o.scrollableInfo.canMatch(scrollableInfo);
            }
            return (isTop == o.isTop || isBottom == o.isBottom)
                    && (isLeft == o.isLeft || isRight == o.isRight);
        }

        public void merge(PositionInfo o){
            if(o == null){
                return;
            }
            insideScrollable |= o.insideScrollable;
            isLeft |= o.isLeft;
            isRight |= o.isRight;
            isTop |= o.isTop;
            isBottom |= o.isBottom;

            sizeBig |= o.sizeBig;
            sizeSmall |= o.sizeSmall;
            if(scrollableInfo != null){
                scrollableInfo.merge(o.scrollableInfo);
            } else if(o.scrollableInfo != null){
                scrollableInfo = new PositionInfo(o.scrollableInfo);
            }
        }
    }

    public static class ItemShapeDetector {
        public static final int GRID_ITEM =     0b0001;
        public static final int CARD_ITEM =     0b0010;
        public static final int BANNER_ITEM =   0b0100;

        public static boolean canMatch(int shape1, int shape2){
            if(shape1 == -1 && shape2 == -1){
                return true;
            } else if(shape1 == -1 || shape2 == -1){
                return false;
            }
            return shape1 == shape2 || (shape1 & shape2) != 0;
        }

        public static String toShapeString(int shape){
            String result = "";
            if(canMatch(GRID_ITEM, shape)){
                result += "grid|";
            }

            if(canMatch(CARD_ITEM, shape)){
                result += "card|";
            }

            if(canMatch(BANNER_ITEM, shape)){
                result += "banner|";
            }

            if(result.isEmpty()){
                result = "unknown";
            }

            return result;
        }

        public static int merge(int s1, int s2){
            if(s1 == -1){
                return s2;
            }
            if(s2 == -1){
                return s1;
            }

            return s1 | s2;
        }

        public static int getItemShape(AccessibilityNodeInfoRecord entrance){
            if(!isDynamicEntranceForShape(entrance)){
                return 0;
            }

            List<AccessibilityNodeInfoRecord> items = entrance.getChildren();
            Map<AccessibilityNodeInfoRecord, Rect> itemToBound = new HashMap<>();
            items.forEach(i->{
                Rect r = new Rect();
                i.getBoundsInScreen(r);
                itemToBound.put(i, r);
            });

            int result = 0;

            for(AccessibilityNodeInfoRecord item: items){
                Rect itemBound = itemToBound.get(item);
                if(itemBound == null){
                    continue;
                }
                double widthHeightRatio = itemBound.width() / (double)itemBound.height();

                boolean foundSameLine = false;
                for(Rect r: itemToBound.values()){
                    if(r == itemBound){
                        continue;
                    }

                    if(r.centerY() >= itemBound.top && r.centerY() <= itemBound.bottom){
                        foundSameLine = true;
                        break;
                    }
                }

                if(foundSameLine){
                    if(widthHeightRatio >= 0.5 && widthHeightRatio <= 2){
                        // 被认为是合理的形状
                        result |= GRID_ITEM;
                    }
                    continue;
                }

                // card or banner
                if(widthHeightRatio < 4){
                    result |= CARD_ITEM;
                } else if(widthHeightRatio > 7){
                    result |= BANNER_ITEM;
                } else {
                    int lineTime = estimateTextLine(item);
                    if(lineTime >= 3){
                        result |= CARD_ITEM;
                    } else {
                        result |= BANNER_ITEM;
                    }
                }
            }

            return result;
        }

        private static int estimateTextLine(AccessibilityNodeInfoRecord node){
            List<Rect> textNodesBound = new ArrayList<>();
            Queue<AccessibilityNodeInfoRecord> nodeQueue = new LinkedList<>();
            nodeQueue.add(node);
            while (!nodeQueue.isEmpty()){
                AccessibilityNodeInfoRecord crt = nodeQueue.poll();
                if(crt == null){
                    continue;
                }
                if(crt.getText() != null && crt.getText().length() > 0){
                    Rect r = new Rect();
                    crt.getBoundsInScreen(r);
                    textNodesBound.add(r);
                }
                nodeQueue.addAll(crt.children);
            }
            if(textNodesBound.isEmpty()){
                return 0;
            }
            textNodesBound.sort((o1, o2) -> Double.compare(o1.exactCenterY(), o2.exactCenterY()));
            int count = 0;
            Rect crt = textNodesBound.get(0);
            for(int i = 1; i < textNodesBound.size(); ++ i){
                Rect next = textNodesBound.get(i);
                if(next.top >= crt.centerY()){
                    crt = next;
                    count += 1;
                }
            }
            return count;
        }

        public static int getItemShapeForList(AccessibilityNodeInfoRecord root, boolean significant){
            int result = 0;
            boolean listFound = false;
            int halfRootArea = root.getArea() / 2;
            Queue<AccessibilityNodeInfoRecord> nodeQueue = new LinkedList<>();
            nodeQueue.add(root);
            while (!nodeQueue.isEmpty()){
                AccessibilityNodeInfoRecord crt = nodeQueue.poll();
                if(crt == null){
                    continue;
                }
                if((crt.getArea() >= halfRootArea || !significant) && isDynamicEntranceForShape(crt)){
                    listFound = true;
                    result |= getItemShape(crt);
                }
                if(crt.getArea() >= halfRootArea || !significant){
                    nodeQueue.addAll(crt.children);
                }
            }

            return listFound? result: -1;
        }
    }

    public static class ItemSemanticDetector {
        public static final int UNKNOWN = 0b00;
        public static final int INFO_ITEM = 0b01;
        public static final int FUNC_ITEM = 0b10;

        private static int getItemSemanticInfo(AccessibilityNodeInfoRecord node){
            Set<String> allTextInNode = new HashSet<>();
            int countTextNum = 0;
            int countClickableItem = 0;
            Queue<AccessibilityNodeInfoRecord> nodeQueue = new LinkedList<>();
            nodeQueue.add(node);
            while (!nodeQueue.isEmpty()){
                AccessibilityNodeInfoRecord crt = nodeQueue.poll();
                if(crt == null){
                    continue;
                }

                if(crt.isClickable()){
                    countClickableItem += 1;
                }
                if(!Utility.isEmptyCS(crt.getText())){
                    countTextNum += 1;
                    allTextInNode.add(String.valueOf(crt.getText()));
                } else if(!Utility.isEmptyCS(crt.getContentDescription())){
                    countTextNum += 1;
                    allTextInNode.add(String.valueOf(crt.getContentDescription()));
                }

                nodeQueue.addAll(crt.children);
            }

            /*if(countClickableItem >= countTextNum){
                return FUNC_ITEM;
            }*/

            int countFuncWord = 0;
            for(String w: allTextInNode){
                //Pair<String, Boolean> result = FunctionWordDict.getInstance().isTextFunc(w, false);
                Pair<String, Boolean> result = Utility.isTextFunc(w,false); //TODO: 待集成FunctionWordDict
                if(result.second){
                    countFuncWord += 1;
                }
            }

            if(countFuncWord >= allTextInNode.size() / 2){
                if(allTextInNode.size() == 1){
                    return UNKNOWN;
                }
                return FUNC_ITEM;
            }

            return INFO_ITEM;
        }
        public static int getSemanticInfoForList(AccessibilityNodeInfoRecord root, boolean significant){
            int result = 0;
            boolean listFound = false;
            int halfRootArea = root.getArea() / 2;
            Queue<AccessibilityNodeInfoRecord> nodeQueue = new LinkedList<>();
            nodeQueue.add(root);
            while (!nodeQueue.isEmpty()){
                AccessibilityNodeInfoRecord crt = nodeQueue.poll();
                if(crt == null){
                    continue;
                }
                if((crt.getArea() >= halfRootArea || !significant) && isDynamicEntranceForShape(crt)){
                    listFound = true;
                    for(AccessibilityNodeInfoRecord item: crt.children){
                        result |= getItemSemanticInfo(item);
                    }
                }

                if(crt.getArea() >= halfRootArea || !significant){
                    nodeQueue.addAll(crt.children);
                }
            }

            return listFound? result: -1;
        }
        public static boolean canMatch(int info1, int info2){
            if(info1 == -1 && info2 == -1){
                return true;
            } else if(info1 == -1 || info2 == -1){
                return false;
            }
            return info1 == info2 || (info1 & info2) != 0;
        }
        public static int merge(int s1, int s2){
            if(s1 == -1){
                return s2;
            }
            if(s2 == -1){
                return s1;
            }

            return s1 | s2;
        }
    }
}
