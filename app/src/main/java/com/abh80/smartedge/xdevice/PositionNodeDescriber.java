package com.abh80.smartedge.xdevice;

import static java.lang.Math.min;

import android.graphics.Rect;
import android.util.Log;
import android.util.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.abh80.smartedge.AccessibilityNodeInfoRecord;
import com.abh80.smartedge.AccessibilityNodeInfoRecordFromFile;
import com.abh80.smartedge.Utility;

public class PositionNodeDescriber extends SpecialNodeDescriber {
    private int considerPara;

    public static final int considerDiffRatioWidthHeightRatio = 1;
    public static final int considerDiffRatioWidthScreenRatio = 2;
    public static final int considerDiffRatioLeftToBlock = 4;
    public static final int considerDiffRatioTopToBlock = 8;
    public static final int considerDiffCentralXOfBlock = 16; //考虑x的中线距离
    public static final int considerDiffRatioRightToBlock = 32; //考虑右边界的距离
    public static final int considerDiffRatioHeightScreenRatio  = 64;

    public PositionNodeDescriber(AccessibilityNodeInfoRecord node, int considerPara) {
        super(node);
        this.considerPara = considerPara;
    }

    @Override
    public AccessibilityNodeInfoRecord findNode(AccessibilityNodeInfoRecord root, AccessibilityNodeInfoRecordFromFile.Action.Type needSupportType, String para,
                                                Set<AccessibilityNodeInfoRecord> usedNodes, Set<AccessibilityNodeInfoRecord> endNodes) {
        //TODO: 基于所在块的位置查找
        List<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> nodeBlockPairList = Utility.
                refreshBlockNodeInfo(root, endNodes);
        Collections.reverse(nodeBlockPairList);
        double minDis = 1;
        AccessibilityNodeInfoRecord minDisNode = null;
        for(Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord> p: nodeBlockPairList) {
            AccessibilityNodeInfoRecord node = p.first;
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

    @Override
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
        double heightScreenRatioOfGiven = (double)nodeRect.height() / screenRect.height();

        double disToBlockLeft = nodeRect.left - blockRect.left;
        double leftToBlockWidthOfGiven = disToBlockLeft / blockRect.width();

        double disToBlockRight = blockRect.right - nodeRect.right;
        double rightToBlockWidthOfGiven = disToBlockRight / blockRect.width();

        double disToBlockTop = nodeRect.top - blockRect.top;
        double topToBlockHeightOfGiven = disToBlockTop / blockRect.height();

        double nodeCentralX = (double)(nodeRect.left+nodeRect.right)/2.0;
        double blockCentralX = (double)(blockRect.left+blockRect.right)/2.0;
        double centralXToBlockCentralOfGiven = Math.abs(nodeCentralX - blockCentralX) / (double)blockRect.width();

        double diffRatioWidthHeightRatio = Math.abs(widthHeightRatioOfGiven - widthHeightRatio) / (min(widthHeightRatioOfGiven, widthHeightRatio) + Double.MIN_VALUE);
        double diffRatioWidthScreenRatio = Math.abs(widthScreenRatioOfGiven - widthScreenRatio) / (min(widthScreenRatioOfGiven, widthScreenRatio) + Double.MIN_VALUE);
        double diffRatioHeightScreenRatio = Math.abs(heightScreenRatioOfGiven - heightScreenRatio) / (min(heightScreenRatioOfGiven, heightScreenRatio) + Double.MIN_VALUE);

        double diffRatioLeftToBlock = Math.abs(leftToBlockWidthOfGiven - leftToBlockWidth) / (min(leftToBlockWidthOfGiven, leftToBlockWidth) + Double.MIN_VALUE);
        double diffRatioTopToBlock = Math.abs(topToBlockHeightOfGiven - topToBlockHeight) / (min(topToBlockHeightOfGiven, topToBlockHeight) + Double.MIN_VALUE);
        double diffRatioRightToBlock = Math.abs(rightToBlockWidthOfGiven - rightToBlockWidth) / (min(rightToBlockWidthOfGiven, rightToBlockWidth) + Double.MIN_VALUE);

        double diffCentralXOfBlock = Math.abs(centralXToBlockCentralOfGiven - centralXToBlockCentral) / (min(centralXToBlockCentralOfGiven, centralXToBlockCentral) + Double.MIN_VALUE);

        double sum = 0;
        boolean flag = true;
        if ((this.considerPara & considerDiffRatioWidthHeightRatio) > 0) {
            sum += diffRatioWidthHeightRatio;
            flag &= (diffRatioWidthHeightRatio < 0.25);
        }
        if ((this.considerPara & considerDiffRatioWidthScreenRatio) > 0) {
            sum += diffRatioWidthScreenRatio;
            flag &= (diffRatioWidthScreenRatio < 0.25);
        }
        if ((this.considerPara & considerDiffRatioLeftToBlock) > 0) {
            sum += diffRatioLeftToBlock;
            flag &= (diffRatioLeftToBlock < 0.25);
        }
        if ((this.considerPara & considerDiffRatioTopToBlock) > 0) {
            sum += diffRatioTopToBlock;
            flag &= (diffRatioTopToBlock < 0.25);
        }

        if ((this.considerPara & considerDiffCentralXOfBlock) > 0) {
            sum += diffCentralXOfBlock;
            flag &= (diffCentralXOfBlock < 0.1);
        }

        if ((this.considerPara & considerDiffRatioRightToBlock) > 0) {
            sum += diffRatioRightToBlock;
            flag &= (diffRatioRightToBlock < 0.25);
        }

        if ((this.considerPara & considerDiffRatioHeightScreenRatio) > 0) {
            sum += diffRatioHeightScreenRatio;
            flag &= (diffRatioHeightScreenRatio < 0.25);
        }

        if (flag) {
            return sum;
        }
        return 100;
    }
}
