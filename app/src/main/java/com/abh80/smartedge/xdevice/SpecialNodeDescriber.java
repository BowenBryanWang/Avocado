package com.abh80.smartedge.xdevice;

import static java.lang.Math.min;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.pow;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.abh80.smartedge.AccessibilityNodeInfoRecord;
import com.abh80.smartedge.AccessibilityNodeInfoRecordFromFile;
import com.abh80.smartedge.Utility;

public class SpecialNodeDescriber extends NodeDescriber{

    public static final int nodeMethodDefault = 0;
    public static final int nodeMethodText = 1;
    public static final int nodeMethodDesc = 2;
    public static final int nodeMethodFixedWidth = 3;
    public static final int nodeMethodFixedHeight = 4;
    public static final int nodeMethodFixedLayout = 5; //固定的布局位置，例如是递归下来每次分在左中右哪一块
    public static final int nodeMethodFixedRelativePos = 6; //固定的相对于block所在的位置
    public static final int nodeMethodFixedWidthHeightRatio = 7; //固定的长宽比

    public int nodeMethod = 0; //默认用的是原本计算差异值的方法

    public double widthHeightRatio;
    public double widthScreenRatio;
    public double heightScreenRatio;

    public double leftToBlockWidth;
    public double rightToBlockWidth;
    public double topToBlockHeight;
    public double centralXToBlockCentral;

    public AccessibilityNodeInfoRecord refNode;

    public SpecialNodeDescriber(AccessibilityNodeInfoRecord node) {
        super((AccessibilityNodeInfoRecord) null);
        // 重写在super中可能被错误设置的域
        needSpecialDescriber = true;
        needPara = false;
        needSameItemTrans = false;

        AccessibilityNodeInfoRecord screenRoot = node.getRoot();
        AccessibilityNodeInfoRecord blockRoot = Utility.getDynamicItemRootForNode(node);
        if(blockRoot == null){
            blockRoot = screenRoot;
        }

        Rect nodeRect = new Rect();
        node.getBoundsInScreen(nodeRect);
        Rect screenRect = new Rect();
        screenRoot.getBoundsInScreen(screenRect);
        Rect blockRect = new Rect();
        blockRoot.getBoundsInScreen(blockRect);

        widthHeightRatio = (double)nodeRect.width() / nodeRect.height();
        widthScreenRatio = (double)nodeRect.width() / screenRect.width();
        heightScreenRatio = (double)nodeRect.height() / screenRect.height();

        double disToBlockLeft = nodeRect.left - blockRect.left;
        leftToBlockWidth = disToBlockLeft / blockRect.width();

        double disToBlockRight = blockRect.right - nodeRect.right;
        rightToBlockWidth = disToBlockRight / blockRect.width();

        double disToBlockTop = nodeRect.top - blockRect.top;
        topToBlockHeight = disToBlockTop / blockRect.height();

        double nodeCentralX = (double)(nodeRect.left+nodeRect.right)/2.0;
        double blockCentralX = (double)(blockRect.left+blockRect.right)/2.0;
        centralXToBlockCentral = Math.abs(nodeCentralX - blockCentralX) / (double)blockRect.width();

        this.refNode = node;
    }


    public NodeDescriber getNodeDescriber(AccessibilityNodeInfoRecord node) {
        switch (node.findMethod) {
            case nodeMethodDefault :
                return new SpecialNodeDescriber(node);
            case nodeMethodFixedRelativePos :
                return new PositionNodeDescriber(node, node.considerPara);
            //你可以有任意数量的case语句
            default : //可选
                return new SpecialNodeDescriber(node);
        }
    }

    public AccessibilityNodeInfoRecord findNode(AccessibilityNodeInfoRecord root, AccessibilityNodeInfoRecordFromFile.Action.Type needSupportType, String para,
             Set<AccessibilityNodeInfoRecord> usedNodes, Set<AccessibilityNodeInfoRecord> endNodes) {
        // 先找到界面上所有的节点、block root的pair
        // 然后从靠近叶子的节点开始找
        List<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> nodeBlockPairList = Utility.
                refreshBlockNodeInfo(root, endNodes);
        Collections.reverse(nodeBlockPairList);
        double minDis = 1;
        AccessibilityNodeInfoRecord minDisNode = null;
        for(Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord> p: nodeBlockPairList){
            AccessibilityNodeInfoRecord node = p.first;
            if (usedNodes.contains(node)) continue;
            double dis = calNodeDistanceToDescriber(node);
            if(dis < minDis){
                Rect r = new Rect();
                node.getBoundsInScreen(r);
                //if (!existSimilarNodeInBlock(node, usedNodes) || dis<=0) {
                    minDis = dis;
                    minDisNode = node;
                //}
            }
        }
        if (minDisNode!=null) {
            Log.i("minDis",this.refNode.getAllTexts()+";"+minDisNode.getAllTexts()+";"+minDis);
        }
        else {
            Log.i("minDis Not Found", this.refNode.getAllTexts());
        }
        return minDisNode;
    }

    private boolean existSimilarNodeInBlock(AccessibilityNodeInfoRecord node, Set<AccessibilityNodeInfoRecord> usedNodes) {
        AccessibilityNodeInfoRecord crtItemRoot = node.blockRoot;
        if (crtItemRoot == null) return false;
        AccessibilityNodeInfoRecord listRoot = crtItemRoot.parent;
        if (listRoot == null) return false;
        if (!listRoot.isDynamicEntrance) return false;
        SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(node);
        Set<AccessibilityNodeInfoRecord> tmpUsedNodes = new HashSet<>();
        tmpUsedNodes.addAll(usedNodes);
        tmpUsedNodes.addAll(Utility.getOverlapNodes(listRoot, node));
        AccessibilityNodeInfoRecord similarNode = specialNodeDescriber.findNodeWithoutLoop(crtItemRoot, null, null, tmpUsedNodes);
        if (similarNode != null) {
            double dis = specialNodeDescriber.calNodeDistanceToDescriber(similarNode);
            if (dis < 0.3) return true;
        }
        return false;
    }

    private AccessibilityNodeInfoRecord findNodeWithoutLoop(AccessibilityNodeInfoRecord root, AccessibilityNodeInfoRecordFromFile.Action.Type needSupportType, String para,
                                                            Set<AccessibilityNodeInfoRecord> usedNodes) {
        List<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> nodeBlockPairList = Utility.
                refreshBlockNodeInfo(root);
        Collections.reverse(nodeBlockPairList);
        double minDis = 1;
        AccessibilityNodeInfoRecord minDisNode = null;
        for(Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord> p: nodeBlockPairList){
            AccessibilityNodeInfoRecord node = p.first;
            AccessibilityNodeInfoRecord blockRoot = p.second;
            if (usedNodes.contains(node)) continue;
            double dis = calNodeDistanceToDescriber(node);
            if(dis < minDis){
                minDis = dis;
                minDisNode = node;
            }
        }
        return minDisNode;
    }

    public List<AccessibilityNodeInfoRecord> findNode(AccessibilityNodeInfoRecord root, double maxDis) {
        // 先找到界面上所有的节点、block root的pair
        // 然后从靠近叶子的节点开始找
        List<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> nodeBlockPairList = Utility.
                refreshBlockNodeInfo(root);
        Collections.reverse(nodeBlockPairList);
        double minDis = 1;
        List<AccessibilityNodeInfoRecord> res = new ArrayList<>();
        for(Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord> p: nodeBlockPairList){
            AccessibilityNodeInfoRecord node = p.first;
            AccessibilityNodeInfoRecord blockRoot = p.second;
            double dis = calNodeDistanceToDescriber(node);
            if(dis < maxDis){
                res.add(node);
            }
        }
        return res;
    }

    public List<AccessibilityNodeInfoRecord> findParallelBlocks(AccessibilityNodeInfoRecord root, AccessibilityNodeInfoRecord node, Set<AccessibilityNodeInfoRecord> usedNodes,
                                                                Set<AccessibilityNodeInfoRecord> endNodes) {
        //TODO：需确认几种情况：列表中不同类型的列表项；列表中列表项和标题相间；列表中列表项层级互不相同。
        //现在的做法是找到动态入口（列表所在节点），然后对于列表中的每个儿子进行查找。
        List<AccessibilityNodeInfoRecord> res = new ArrayList<>();
        res.add(node);
        AccessibilityNodeInfoRecord crtItemRoot = node.blockRoot;
        if (crtItemRoot == null) return res;
        AccessibilityNodeInfoRecord listRoot = crtItemRoot.parent;
        if (listRoot == null) return res;
        if (!listRoot.isDynamicEntrance) return res;
        SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(node);
        for (AccessibilityNodeInfoRecord itemRoot : listRoot.children){
            if (itemRoot == crtItemRoot) continue;
            AccessibilityNodeInfoRecord similarNode = specialNodeDescriber.findNode(itemRoot, null, null, usedNodes, endNodes);
            if (similarNode!=null) {
                Set<AccessibilityNodeInfoRecord> tmpUsedNodes = new HashSet<>();
                tmpUsedNodes.addAll(usedNodes);
                tmpUsedNodes.addAll(Utility.getOverlapNodes(listRoot,similarNode));
                AccessibilityNodeInfoRecord similarNode1 = specialNodeDescriber.findNode(itemRoot, null, null, tmpUsedNodes, endNodes);
                if (similarNode1 != null) {
                    double dis1 = specialNodeDescriber.calNodeDistanceToDescriber(similarNode);
                    double dis2 = specialNodeDescriber.calNodeDistanceToDescriber(similarNode1);
                    if (Math.abs(dis1-dis2) < 0.3) continue;
                }
                if (node.children.size() == similarNode.children.size()) res.add(similarNode);
            }
        }
        return res;
    }

    public AccessibilityNodeInfoRecord findNodeByText(AccessibilityNodeInfoRecord root, Set<AccessibilityNodeInfoRecord> endNodes) {
        String refText = refNode.getAllTexts();
        if (refText.equals("")) return null;
        List<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> nodeBlockPairList = Utility.
                refreshBlockNodeInfo(root, endNodes);
        Collections.reverse(nodeBlockPairList);
        double minDis = 100;
        AccessibilityNodeInfoRecord res = null;
        for(Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord> p: nodeBlockPairList){
            AccessibilityNodeInfoRecord node = p.first;
            List<String> crtTexts = node.getAllSplitTexts();
            if (crtTexts.size() == 0) continue;
            double matchedCnt = 0;
            for (String text : crtTexts) {
                if (refText.contains(text)) {
                    matchedCnt++;
                }
                else {
                    matchedCnt += Utility.getCommonLen(refText,text);
                }
            }
            if (matchedCnt == 0) continue;
            double dis = calNodeDistanceToDescriber(node);
            Log.i("text dis",Double.toString(dis));
            if (dis > 1) continue;
            dis = dis*0.8 + (1.0-(double)matchedCnt*1.0/(double)crtTexts.size())*0.2;
            if (dis < minDis) {
                minDis = dis;
                res = node;
            }
        }
        return res;
    }

    public AccessibilityNodeInfoRecord findNodeByDesc(AccessibilityNodeInfoRecord root, Set<AccessibilityNodeInfoRecord> endNodes) {
        String refDesc = this.refNode.getAllContents();
        if (refDesc.equals("")) return null;
        List<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> nodeBlockPairList = Utility.
                refreshBlockNodeInfo(root, endNodes);
        Collections.reverse(nodeBlockPairList);
        double minDis = 100;
        AccessibilityNodeInfoRecord res = null;
        for(Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord> p: nodeBlockPairList){
            AccessibilityNodeInfoRecord node = p.first;
            List<String> crtDesc = node.getAllContentsSplit();
            if (crtDesc.size() == 0) continue;
            int matchedCnt = 0;
            for (String desc : crtDesc) {
                if (refDesc.contains(desc)) {
                    matchedCnt++;
                }
                else {
                    matchedCnt += Utility.getCommonLen(refDesc,desc);
                }
            }
            if (matchedCnt == 0) continue;
            double dis = calNodeDistanceToDescriber(node);
            if (dis > 1) continue;
            dis = dis*0.8 + (1.0-(double)matchedCnt*1.0/(double)crtDesc.size())*0.2;
            if (dis < minDis) {
                minDis = dis;
                res = node;
            }
        }
        return res;
    }

    public AccessibilityNodeInfoRecord findNodeByPos(AccessibilityNodeInfoRecord root, Set<AccessibilityNodeInfoRecord> endNodes) {
        // TODO: 2021/5/12
        /*List<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> nodeBlockPairList = Utility.
                refreshBlockNodeInfo(root, endNodes);
        Collections.reverse(nodeBlockPairList);
        for(Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord> p: nodeBlockPairList){
            AccessibilityNodeInfoRecord node = p.first;
            if (!(this.refNode.getContentDescription().equals(node.getContentDescription()))) continue;
            double dis = calNodeDistanceToDescriber(node);
            if (dis > 1) continue;
            return node;
        }*/
        return null;
    }

    @Override
    public boolean canDescribe(AccessibilityNodeInfoRecord node) {
        AccessibilityNodeInfoRecord root = node.getRoot();
        AccessibilityNodeInfoRecord blockRoot = Utility.getDynamicItemRootForNode(node);
        if(blockRoot == null){
            blockRoot = root;
        }

        Rect nodeRect = new Rect();
        node.getBoundsInScreen(nodeRect);
        Rect screenRect = new Rect();
        root.getBoundsInScreen(screenRect);
        Rect blockRect = new Rect();
        blockRoot.getBoundsInScreen(blockRect);

        double widthHeightRatioOfGiven = (double)nodeRect.width() / nodeRect.height();
        double widthScreenRatioOfGiven = (double)nodeRect.width() / screenRect.width();

        double disToBlockLeft = nodeRect.left - blockRect.left;
        double leftToBlockWidthOfGiven = disToBlockLeft / blockRect.width();

        double disToBlockTop = nodeRect.top - blockRect.top;
        double topToBlockHeightOfGiven = disToBlockTop / blockRect.height();

        double diffRatioWidthHeightRatio = Math.abs(widthHeightRatioOfGiven - widthHeightRatio) / min(widthHeightRatioOfGiven, widthHeightRatio);
        double diffRatioWidthScreenRatio = Math.abs(widthScreenRatioOfGiven - widthScreenRatio) / min(widthScreenRatioOfGiven, widthScreenRatio);
        double diffRatioLeftToBlock = Math.abs(leftToBlockWidthOfGiven - leftToBlockWidth) / min(leftToBlockWidthOfGiven, leftToBlockWidth);
        double diffRatioTopToBlock = Math.abs(topToBlockHeightOfGiven - topToBlockHeight) / min(topToBlockHeightOfGiven, topToBlockHeight);

        return diffRatioWidthHeightRatio < 0.25 && diffRatioWidthScreenRatio < 0.25 && diffRatioLeftToBlock < 0.25 && diffRatioTopToBlock < 0.25;
    }

    public double calNodeDistanceToDescriber(AccessibilityNodeInfoRecord node){
        if (node == null) return 100;
        if (node.getText() != null && this.refNode.getText() == null && !node.getText().equals("")) return 100;
        if (node.getText() == null && this.refNode.getText() != null && !this.refNode.getText().equals("")) return 100;
        AccessibilityNodeInfoRecord root = node.getRoot();
        AccessibilityNodeInfoRecord blockRoot = Utility.getDynamicItemRootForNode(node);
        if (blockRoot == null){
            blockRoot = root;
        }
        AccessibilityNodeInfoRecord refBlockRoot = Utility.getDynamicItemRootForNode(this.refNode);
        if (refBlockRoot == null) {
            refBlockRoot = root;
        }
        Rect nodeRect = new Rect();
        node.getBoundsInScreen(nodeRect);
        Rect screenRect = new Rect();
        root.getBoundsInScreen(screenRect);
        Rect blockRect = new Rect();
        blockRoot.getBoundsInScreen(blockRect);

        double widthHeightRatioOfGiven = (double)nodeRect.width() / nodeRect.height();
        double widthScreenRatioOfGiven = (double)nodeRect.width() / screenRect.width();

        double disToBlockLeft = nodeRect.left - blockRect.left;
        double leftToBlockWidthOfGiven = disToBlockLeft / blockRect.width();

        double disToBlockTop = nodeRect.top - blockRect.top;
        double topToBlockHeightOfGiven = disToBlockTop / blockRect.height();

        double diffRatioWidthHeightRatio = Math.abs(widthHeightRatioOfGiven - widthHeightRatio) / (min(widthHeightRatioOfGiven, widthHeightRatio) + Double.MIN_VALUE);
        double diffRatioWidthScreenRatio = Math.abs(widthScreenRatioOfGiven - widthScreenRatio) / (min(widthScreenRatioOfGiven, widthScreenRatio) + Double.MIN_VALUE);
        double diffRatioLeftToBlock = Math.abs(leftToBlockWidthOfGiven - leftToBlockWidth) / (min(leftToBlockWidthOfGiven, leftToBlockWidth) + Double.MIN_VALUE);
        double diffRatioTopToBlock = Math.abs(topToBlockHeightOfGiven - topToBlockHeight) / (min(topToBlockHeightOfGiven, topToBlockHeight) + Double.MIN_VALUE);

        double simRatioId = calcSimilarity(this.refNode, node);
        if (simRatioId > 1) return 0;
        Log.i("compair",this.refNode.getAllTexts()+";"+node.getAllTexts());
        Log.i("simRatioId",Double.toString(simRatioId));
        if (simRatioId < 0.7) return 100;
        Log.i("diffRatioWidthHeightRatio",Double.toString(diffRatioWidthHeightRatio) );
        Log.i("diffRatioWidthScreenRatio",Double.toString(diffRatioWidthScreenRatio) );
        Log.i("diffRatioLeftToBlock",Double.toString(diffRatioLeftToBlock) );
        Log.i("diffRatioTopToBlock",Double.toString(diffRatioTopToBlock) );
        double c = 1.0;
        if (node.isClickable() && this.refNode.isClickable()) c *= 0.9;
        if (node.isEditable() && this.refNode.isEditable()) c *= 0.8;
        if (node.isScrollable() && this.refNode.isScrollable()) c *= 0.7;
        Log.i("blockRoot",blockRoot.getClassName().toString());
        Log.i("refBlockRoot",refBlockRoot.getClassName().toString());
        if (!blockRoot.getClassName().toString().equals(refBlockRoot.getClassName().toString())) return 100;

        if (simRatioId > 0.98) {
            if (this.refNode.getText()!=null && node.getText()!=null)
                if (this.refNode.getText()!="" && this.refNode.getText().equals(node.getText())) return -1;
            if (this.refNode.getContentDescription()!=null && node.getContentDescription()!=null)
                if (this.refNode.getContentDescription()!="" && this.refNode.getContentDescription().equals(node.getContentDescription())) return -1;
            return c*min(diffRatioWidthHeightRatio + diffRatioWidthScreenRatio + diffRatioLeftToBlock + diffRatioTopToBlock,1.0)/pow(1.5,simRatioId);
        }
        if (this.refNode.getText()!=null && node.getText()!=null)
            if (this.refNode.getText()!="" && this.refNode.getText().equals(node.getText())) return -1;
        if (diffRatioWidthHeightRatio < 0.25 && diffRatioWidthScreenRatio < 0.25 && diffRatioLeftToBlock < 0.25 && diffRatioTopToBlock < 0.25){

            if (this.refNode.getContentDescription()!=null && node.getContentDescription()!=null)
                if (this.refNode.getContentDescription()!="" && this.refNode.getContentDescription().equals(node.getContentDescription()))
                    return c*(diffRatioWidthHeightRatio + diffRatioWidthScreenRatio + diffRatioLeftToBlock + diffRatioTopToBlock)*0.8;  //改成一定程度上看文本
            return c*(diffRatioWidthHeightRatio + diffRatioWidthScreenRatio + diffRatioLeftToBlock + diffRatioTopToBlock);
        } else {
            /*if (diffRatioTopToBlock + diffRatioWidthScreenRatio + diffRatioLeftToBlock < 0.1)
                return diffRatioTopToBlock + diffRatioWidthScreenRatio + diffRatioLeftToBlock;*/
            if (diffRatioWidthHeightRatio + diffRatioWidthScreenRatio + diffRatioLeftToBlock < 0.1)
                return diffRatioWidthHeightRatio + diffRatioWidthScreenRatio + diffRatioLeftToBlock;
            //TODO: 算距离有问题！！！！！
            return 2*c*(diffRatioWidthHeightRatio + diffRatioWidthScreenRatio + diffRatioLeftToBlock + diffRatioTopToBlock);
            //return 100;
        }
    }

    private double calcSimilarity(AccessibilityNodeInfoRecord refNode, AccessibilityNodeInfoRecord node) {
        if (refNode == null || node == null) return 0;
        if (!refNode.getClassName().toString().equals(node.getClassName())) return 0.0;
        String st1 = refNode.dynamicId.replaceFirst("fake.root|\\d+","");
        String st2 = node.dynamicId.replaceFirst("fake.root\\|\\d+","");
        if (st1.equals(st2)) return 100.0;
        String[] subIdList1 = refNode.absoluteId.split(";");
        int l1 = subIdList1.length;
        if (subIdList1[0].contains("fake")) {
            for (int i = 0; i < l1-1; i++) subIdList1[i] = subIdList1[i+1];
            l1--;
        }
        String[] subIdList2 = node.absoluteId.split(";");
        int l2 = subIdList2.length;
        if (subIdList2[0].contains("fake")) {
            for (int i = 0; i < l2-1; i++) subIdList2[i] = subIdList2[i+1];
            l2--;
        }
        double[][] F = new double[l1][l2]; //F[i][j]表示1……i和1……j的相似度
        for (int i = 0; i < l1; ++ i)
            for (int j = 0; j < l2; ++ j) {
                F[i][j] = 0;
                if (i!=0) F[i][j] = max(F[i][j], F[i-1][j]);
                if (j!=0) F[i][j] = max(F[i][j], F[i][j-1]);
                double tmp = 0;
                if (i!=0 && j!=0) tmp = F[i-1][j-1];
                if (subIdList1[i].equals(subIdList2[j])) tmp += 1;
                else {
                    String[] subIdSplited1 = subIdList1[i].split("\\|");
                    String[] subIdSplited2 = subIdList2[j].split("\\|");
                    if (subIdSplited2[0].equals(subIdSplited1[0])) tmp += 0.8;
                    else {
                        if (i==l1-1 && j==l2-1) return 0.0;
                    }
                }
                F[i][j] = max(F[i][j], tmp);
            }
        double matchedRatio = 2.0*F[l1-1][l2-1]/(l1+l2);
        Log.i("matchedRatio",Integer.toString(l1)+";"+ Integer.toString(l2) +";"+Double.toString(F[l1-1][l2-1]));
        return matchedRatio;
        /*AccessibilityNodeInfoRecord root1 = refNode.getBlockRoot();
        AccessibilityNodeInfoRecord root2 = node.getBlockRoot();
        if (root1 != null && root2 != null && ((root1 != refNode) || (root2 != node))) {
            double blockMatchedRatio = calcAbsoluteIdSimilarity(root1, root2);
            if (blockMatchedRatio > 0.99) {

            }
        }*/
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toFileName(AccessibilityNodeInfoRecordFromFile.Action.Type type) {
        return "SpecialNodeDesc " + String.format("%.2f %.2f %.2f %.2f ", widthHeightRatio, widthScreenRatio, leftToBlockWidth, topToBlockHeight);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SpecialNodeDescriber that = (SpecialNodeDescriber) o;
        return Double.compare(that.widthHeightRatio, widthHeightRatio) == 0 &&
                Double.compare(that.widthScreenRatio, widthScreenRatio) == 0 &&
                Double.compare(that.leftToBlockWidth, leftToBlockWidth) == 0 &&
                Double.compare(that.topToBlockHeight, topToBlockHeight) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), widthHeightRatio, widthScreenRatio, leftToBlockWidth, topToBlockHeight);
    }

    @Override
    public boolean canDescribe(NodeDescriber other) {
        return Objects.equals(this, other);
    }

}
