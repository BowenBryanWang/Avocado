package com.abh80.smartedge;

import static com.abh80.smartedge.xdevice.ImportantStructureUtility.getTitle;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;
import android.util.Pair;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.abh80.smartedge.xdevice.SpecialNodeDescriber;

class TitleNode {
    public String nodeId;
}

public class MergedBlock {
    public static SemanticBlock lastRootBlock = null;
    public static final String mainPath= "/sdcard/UICrwaler/";
    public String appName;
    public List<SemanticBlock> semanticBlocks;
    public List<TitleNode> titleNodes;
    public Map<String, AccessibilityNodeInfoRecordFromFile> instanceId2UIRoot;
    public Map<Integer, List<SemanticBlock>> pageId2SemanticBlocks;
    List<AccessibilityNodeInfoRecord> titles;
    Map <AccessibilityNodeInfoRecord, Double> forwardButtons;
    Map <AccessibilityNodeInfoRecord, Double> backwardButtons;
    List<InfoList> infoLists;
    Map <AccessibilityNodeInfoRecord, Double> tabItems;
    Map <AccessibilityNodeInfoRecord, Double> highlightItems;
    Map <AccessibilityNodeInfoRecord, Double> correspondingRegions;
    Map <FuncButton, Double> funcButtons;
    Map <AccessibilityNodeInfoRecord, Double> menuButtons;
    Map <AccessibilityNodeInfoRecord, Double> buttonsIntoSearchPage;
    Map <AccessibilityNodeInfoRecord, Double> searchTriggers;
    Map <AccessibilityNodeInfoRecord, Double> voiceInputButtons;
    Map <AccessibilityNodeInfoRecord, Double> textInputButtons;
    List<CheckBox> checkBoxes;
    List<Dialog> dialogs;
    List<SemanticBlock> matchedChildrenBlock;
    List<SemanticBlock> matchedBlocks;

    MergedBlock(String appName) {
        this.appName = appName;
        // TODO: 2021/3/9
        titleNodes = new ArrayList<>();
        instanceId2UIRoot = new HashMap<>();
    }

    public void loadPages() throws IOException, JSONException {
//        String pagePath = mainPath+this.appName+"/PageInfo/";
//        File pageInfo = new File(pagePath);
//        File[] pages = pageInfo.listFiles();
//        for (File page : pages){
//            File[] states = page.listFiles();
//            if (states == null) continue;
//            for (File state : states) {
//                File[] instances = state.listFiles();
//                if (instances == null) continue;
//                for (File instance : instances)
//                    if (instance.toString().contains("json")) {
//                        System.out.println(instance.toString());
//                        AccessibilityNodeInfoRecordFromFile crtRoot = AccessibilityNodeInfoRecordFromFile.buildTreeFromFile(instance.toString(),false);
//                        String pattern = "^"+pagePath+"Page(\\d+)/PageState(\\d+)/PageInstance(\\d+).*";
//                        Pattern r = Pattern.compile(pattern);
//                        Matcher m = r.matcher(instance.toString());
//                        if (m.find()) {
//                            String instanceId = m.group(1)+"_"+m.group(2)+"_"+m.group(3);
//                            this.instanceId2UIRoot.put(instanceId,crtRoot);
//                        }
//                    }
//            }
//        }
    }

    public void loadSemanticBlocks() throws IOException, JSONException {
        this.pageId2SemanticBlocks = new HashMap<>();
        String semanticPath = mainPath+this.appName+"/dataExport/";
        int prefixLen = semanticPath.length();
        File pageInfo = new File(semanticPath);
        File[] pages = pageInfo.listFiles();
        for (File page : pages) {
            if (!(page.toString().contains("json"))) continue;
            String instanceId = page.toString().substring(prefixLen).replace(".json","");
            System.out.println(semanticPath+page.toString());
            SemanticBlock crtSemanticBlock = SemanticBlock.buildBlockTreeFromFile(page.toString(), instanceId2UIRoot.get(instanceId));
            String pattern = "^(\\d+)_(\\d+)_(\\d+).*";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(instanceId);
            int pageIndex = 0;
            if (m.find()) {
                pageIndex = Integer.parseInt(m.group(1));
            }
            if (this.pageId2SemanticBlocks.containsKey(pageIndex)) {
                this.pageId2SemanticBlocks.get(pageIndex).add(crtSemanticBlock);
            }
            else {
                this.pageId2SemanticBlocks.put(pageIndex, new ArrayList<>());
                this.pageId2SemanticBlocks.get(pageIndex).add(crtSemanticBlock);
            }
        }
    }


    public void loadFindNodesMethod() {
        //TODO: 将有标注定位方法的节点额外添加上，否则默认方法为0
    }

    public AccessibilityNodeInfoRecord getUIRootFromFile(int pageId, int stateId, int instanceId) throws IOException, JSONException {
        String filePath = mainPath + this.appName + "/test/Page"+Integer.toString(pageId)+"/PageState"+Integer.toString(stateId)+
                "/PageInstance"+Integer.toString(instanceId)+".json";
        AccessibilityNodeInfoRecordFromFile crtRoot = AccessibilityNodeInfoRecordFromFile.buildTreeFromFile(filePath,false);
        return crtRoot;
    }

    public Bitmap getImageFromFile(int pageId, int stateId, int instanceId) throws IOException, JSONException{
        String filePath = mainPath + this.appName + "/test/Page"+Integer.toString(pageId)+"/PageState"+Integer.toString(stateId)+
                "/PageInstance"+Integer.toString(instanceId)+".png";
        FileInputStream fis = new FileInputStream(filePath);
        Bitmap bitmap  = BitmapFactory.decodeStream(fis);
        return bitmap;
    }


    SemanticBlock generateSemanticBlock(AccessibilityNodeInfoRecord crtRoot, List<SemanticBlock> patternBlocks, SemanticBlock parentBlock) {
        SemanticBlock res = new SemanticBlock(parentBlock, crtRoot, parentBlock);
        Set<AccessibilityNodeInfoRecord> rootSet = new HashSet<>();
        List<SemanticBlock> matchedPatternBlocks = new ArrayList<>();
        for (SemanticBlock patternBlock : patternBlocks) {
            if (patternBlock.findBlockRoots(rootSet, crtRoot)) {
                matchedPatternBlocks.add(patternBlock);
                if (patternBlock.mode!=-1) res.setMode(patternBlock.mode);
                if (patternBlock.blockType!="block") res.setBlockType(patternBlock.blockType);
                res.setGesture(patternBlock.gestures);
            }
        }
        if (res.mode == 100) res.mode = -1;
        for (AccessibilityNodeInfoRecord root : rootSet) res.addRoot(root);
        generateTitle(res, matchedPatternBlocks, rootSet);
        generateForwardButtons(res, matchedPatternBlocks, rootSet);
        generateBackwardButtons(res, matchedPatternBlocks, rootSet);
        generateInfoLists(res, matchedPatternBlocks, rootSet);
        generateTabItems(res, matchedPatternBlocks, rootSet);
        generateHighlightItem(res, matchedPatternBlocks, rootSet);
        generateCorrespondingRegions(res, matchedPatternBlocks, rootSet);
        generateFuncButtons(res, matchedPatternBlocks, rootSet);
        generateMenuButtons(res, matchedPatternBlocks, rootSet);
        generateSearchBlock(res, matchedPatternBlocks, rootSet);
        generateCheckBoxes(res, matchedPatternBlocks, rootSet);
        generateDialogs(res, matchedPatternBlocks, rootSet);
        return res;
    }


    void clearBlockComponentList() {
        titles.clear();
        forwardButtons.clear();
        backwardButtons.clear();
        infoLists.clear();
        tabItems.clear();
        highlightItems.clear();
        correspondingRegions.clear();
        funcButtons.clear();
        menuButtons.clear();
        buttonsIntoSearchPage.clear();

        searchTriggers.clear();
        voiceInputButtons.clear();
        textInputButtons.clear();
        checkBoxes.clear();
        dialogs.clear();
    }

    List<Pair<Double, SemanticBlock>> generateSingleSemanticBlock(AccessibilityNodeInfoRecord crtRoot, SemanticBlock patternBlock, Set<AccessibilityNodeInfoRecord> endNodes) {
        //用一个模板去匹配当前页面中的各个区域，并返回每个匹配结果及其差异度
        if (endNodes.contains(crtRoot)) return new ArrayList<>();
        //todo: 将固定的部分摒除在搜索范围之外
        Set<AccessibilityNodeInfoRecord> crtEndNodes = new HashSet<>(endNodes);
        long startTime = System.currentTimeMillis();
        //static part
        titles = findAllTitles(crtRoot, patternBlock, crtEndNodes);
        Log.i("title cnt", new Integer(titles.size()).toString());
        for (AccessibilityNodeInfoRecord title : titles) Log.i("title",title.getAllTexts());
        forwardButtons = findAllFixedNodesAccordingToList(crtRoot, patternBlock.forwardButtons, crtEndNodes);
        backwardButtons = findAllFixedNodesAccordingToList(crtRoot, patternBlock.backwardButtons, crtEndNodes);
        tabItems = findAllNodesAccordingToList(crtRoot, patternBlock.tabItems, crtEndNodes);
        highlightItems = findAllHighlightItems(crtRoot, patternBlock, crtEndNodes);
        correspondingRegions = findAllNodesAccordingToList(crtRoot, patternBlock.correspondingRegions, crtEndNodes);
        funcButtons = findAllFuncButtons(crtRoot, patternBlock, crtEndNodes);
        menuButtons = findAllFixedNodesAccordingToList(crtRoot, patternBlock.menuButtons, crtEndNodes);
        buttonsIntoSearchPage = findAllFixedNodesAccordingToList(crtRoot, patternBlock.buttonsIntoSearchPage, crtEndNodes);
        searchTriggers = findAllFixedNodesAccordingToList(crtRoot, patternBlock.searchTriggers, crtEndNodes);
        voiceInputButtons = findAllFixedNodesAccordingToNode(crtRoot, patternBlock.voiceInputNode, crtEndNodes);
        textInputButtons = findAllFixedNodesAccordingToNode(crtRoot, patternBlock.textInputNode, crtEndNodes);

        //dynamic part
        long midTime1 = System.currentTimeMillis();
        infoLists = findAllInfoLists(crtRoot, patternBlock, crtEndNodes);
        long midTime2 = System.currentTimeMillis();
        Log.i("find infoLists",Long.toString(midTime2-midTime1));
        checkBoxes = findAllCheckBoxes(crtRoot, patternBlock, crtEndNodes);
        dialogs = findAllDialogs(crtRoot, patternBlock, crtEndNodes);

        matchedChildrenBlock = new ArrayList<>();
        for (SemanticBlock block : matchedBlocks)
            if (block.patternBlock.parentBlock == patternBlock) matchedChildrenBlock.add(block);
            else if (block.patternBlock.parentBlock.isSameType(patternBlock)) matchedChildrenBlock.add(block);
        long endTime = System.currentTimeMillis();
        List<Pair<Double, SemanticBlock>>  res = findAllBlockRoots(crtRoot, patternBlock, endNodes);
        /*if (patternBlock.titleNode != null && patternBlock.depth != 0 && patternBlock.titleIsStatic) {
            List<Pair<Double, SemanticBlock>> tmpRes = new ArrayList<>(res);
            res.clear();
            for (Pair<Double, SemanticBlock> resPair : tmpRes)
                if (resPair.second.titleNode != null) res.add(resPair);
        }*/
        for (Pair<Double, SemanticBlock> resPair : res) {
            for (AccessibilityNodeInfoRecord root : resPair.second.roots) endNodes.add(root);
        }
        long endTime2 = System.currentTimeMillis();
        System.out.println("Widget Recognition: "+ Long.toString(endTime-startTime)+"ms");
        System.out.println("Block Generation: "+ Long.toString(endTime2-endTime)+"ms");
        return res;
    }


    private List<Pair<Double, SemanticBlock>> findAllBlockRoots(AccessibilityNodeInfoRecord crtRoot, SemanticBlock patternBlock, Set<AccessibilityNodeInfoRecord> endNodes) {
        if (crtRoot == null) return new ArrayList<>();
        if (endNodes.contains(crtRoot)) return new ArrayList<>();
        List<Pair<Double, SemanticBlock>> childrenRes = new ArrayList<>();
        for (AccessibilityNodeInfoRecord child : crtRoot.children) {
            List<Pair<Double, SemanticBlock>> childRes = findAllBlockRoots(child, patternBlock, endNodes);
            childrenRes.addAll(childRes);
        }
        SemanticBlock semanticBlock = new SemanticBlock(null, crtRoot, patternBlock);
        Set<AccessibilityNodeInfoRecord> usedNodes = new HashSet<>();
        double score = 0;
        score += semanticBlock.setTitleFromSubTree(childrenRes, titles, usedNodes);
        /*if (semanticBlock.titleNode == null && patternBlock.titleNode != null && patternBlock.depth!=0)
            return new ArrayList<>();*/
        //来自子树的属性合并
        semanticBlock.setKeyNodesFromSubTree(childrenRes);
        //刚好在当前根节点新增的属性
        Pair <Integer, Double> cmpP = new Pair<Integer, Double>(-1,1000.0);
        cmpP = findMinDis2Pattern(crtRoot, forwardButtons, cmpP, 0);
        cmpP = findMinDis2Pattern(crtRoot, backwardButtons, cmpP, 1);
        cmpP = findMinDis2Pattern(crtRoot, tabItems, cmpP, 2);
        cmpP = findMinDis2Pattern(crtRoot, highlightItems, cmpP, 3);
        cmpP = findMinDis2Pattern(crtRoot, correspondingRegions, cmpP, 4);
        cmpP = findMinDis2Pattern(crtRoot, menuButtons, cmpP, 5);
        cmpP = findMinDis2Pattern(crtRoot, buttonsIntoSearchPage, cmpP, 6);
        cmpP = findMinDis2Pattern(crtRoot, searchTriggers, cmpP, 7);
        switch(cmpP.first){
            case 0 :
                semanticBlock.updateRootInfo(semanticBlock.forwardButtons, semanticBlock.forwardButtonIds, forwardButtons, usedNodes);
                break;
            case 1 :
                semanticBlock.updateRootInfo(semanticBlock.backwardButtons, semanticBlock.backwardButtonIds, backwardButtons, usedNodes);
                break;
            case 2 :
                semanticBlock.updateRootInfo(semanticBlock.tabItems, semanticBlock.tabItemIds, tabItems, usedNodes);
            case 3 :
                semanticBlock.updateRootInfo(semanticBlock.highlightItems, semanticBlock.highlightItemIds, highlightItems, usedNodes);
            case 4 :
                semanticBlock.updateRootInfo(semanticBlock.correspondingRegions, semanticBlock.correspondingRegionIds, correspondingRegions, usedNodes);
            case 5 :
                semanticBlock.updateRootInfo(semanticBlock.menuButtons, semanticBlock.menuButtonIds, menuButtons, usedNodes);
            case 6 :
                semanticBlock.updateRootInfo(semanticBlock.buttonsIntoSearchPage, semanticBlock.buttonIdsIntoSearchPage, buttonsIntoSearchPage, usedNodes);
            case 7 :
                semanticBlock.updateRootInfo(semanticBlock.searchTriggers, semanticBlock.searchTriggerIds, searchTriggers, usedNodes);
            default : //可选
                //语句
        }

        score += semanticBlock.calcNodeScore(semanticBlock.forwardButtons, forwardButtons);
        score += semanticBlock.calcNodeScore(semanticBlock.backwardButtons, backwardButtons);
        score += semanticBlock.calcNodeScore(semanticBlock.tabItems, tabItems);
        score += semanticBlock.calcNodeScore(semanticBlock.highlightItems, highlightItems);
        score += semanticBlock.calcNodeScore(semanticBlock.correspondingRegions, correspondingRegions);
        score += semanticBlock.calcNodeScore(semanticBlock.menuButtons, menuButtons);
        score += semanticBlock.calcNodeScore(semanticBlock.buttonsIntoSearchPage, buttonsIntoSearchPage);
        score += semanticBlock.calcNodeScore(semanticBlock.searchTriggers, searchTriggers);

        score += semanticBlock.setFuncButtonsFromSubTree(childrenRes, funcButtons, usedNodes);
        score += semanticBlock.setVoiceInputButtonFromSubTree(childrenRes, voiceInputButtons, usedNodes);
        score += semanticBlock.setTextInputButtonFromSubTree(childrenRes, textInputButtons, usedNodes);
        score += semanticBlock.setCheckBoxesFromSubTree(childrenRes, checkBoxes, usedNodes);
        score += semanticBlock.setDialogsFromSubTree(childrenRes, dialogs, usedNodes);
        score += semanticBlock.setInfoListsFromSubTree(childrenRes, infoLists, usedNodes);

        score += semanticBlock.setChildrenBlockFromSubTree(matchedChildrenBlock);


        double rootScore = 0;
        if (patternBlock.roots.size()==1) rootScore = semanticBlock.setSingleRoot(crtRoot, patternBlock.roots, usedNodes, endNodes);
        else rootScore = semanticBlock.setMultiRoots(crtRoot, patternBlock.roots, usedNodes, endNodes);
        score += rootScore;

        Pair<Double, SemanticBlock> crtPair = new Pair<>(score, semanticBlock);
        for (Pair<Double, SemanticBlock> childRes : childrenRes)
            if (childRes.first >= crtPair.first) return childrenRes;
        List<Pair<Double, SemanticBlock>> res = new ArrayList<>();
        if (crtPair.first > rootScore) {
            //if (crtPair.second.titleNode != null || patternBlock.titleNode == null || patternBlock.depth == 0)
            res.add(crtPair);
        }
        return res;
    }

    private Pair<Integer, Double> findMinDis2Pattern(AccessibilityNodeInfoRecord crtRoot, Map<AccessibilityNodeInfoRecord, Double> keyNodeToDist, Pair<Integer, Double> cmpP, int flag) {
        if (!keyNodeToDist.containsKey(crtRoot)) return cmpP;
        double dis = keyNodeToDist.get(crtRoot);
        if (dis<cmpP.second) {
            return new Pair<>(flag,dis);
        }
        return cmpP;
    }


    private List<AccessibilityNodeInfoRecord> findAllTitles(AccessibilityNodeInfoRecord crtNode, SemanticBlock patternBlock, Set<AccessibilityNodeInfoRecord> endNodes) {
        List<AccessibilityNodeInfoRecord> res = new ArrayList<>();
        if (endNodes.contains(crtNode)) return res;
        double diff = patternBlock.calNodeDistanceToTitle(crtNode);
        Log.i("title distance",crtNode.getAllTexts()+";"+new Double(diff).toString());
        if (diff<1) {
            res.add(crtNode);
            endNodes.add(crtNode);
        }
        for (AccessibilityNodeInfoRecord child : crtNode.children) res.addAll(this.findAllTitles(child, patternBlock, endNodes));
        return res;
    }


    private Map <AccessibilityNodeInfoRecord, Double> findAllNodesAccordingToList(AccessibilityNodeInfoRecord crtNode, List<AccessibilityNodeInfoRecord> patternNodes, Set<AccessibilityNodeInfoRecord> endNodes) {
        Map <AccessibilityNodeInfoRecord, Double> res = new HashMap<>();
        if (endNodes.contains(crtNode)) return res;
        double minDis = 1;
        for (AccessibilityNodeInfoRecord node : patternNodes) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(node);
            double dis = specialNodeDescriber.calNodeDistanceToDescriber(crtNode);
            if (dis < minDis) {
                minDis = dis;
            }
        }
        if (minDis < 1) {
            res.put(crtNode,minDis);
            endNodes.add(crtNode);
        }
        else {
            for (AccessibilityNodeInfoRecord child : crtNode.children)
                res.putAll(this.findAllNodesAccordingToList(child, patternNodes, endNodes));
        }
        return res;
    }


    private Map<AccessibilityNodeInfoRecord, Double> findAllFixedNodesAccordingToList(AccessibilityNodeInfoRecord crtNode, List<AccessibilityNodeInfoRecord> patternNodes, Set<AccessibilityNodeInfoRecord> endNodes) {
        Map <AccessibilityNodeInfoRecord, Double> res = new HashMap<>();
        if (endNodes.contains(crtNode)) return res;
        for (AccessibilityNodeInfoRecord node : patternNodes) {
            SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(node);
            AccessibilityNodeInfoRecord similarNode = specialNodeDescriber.findNodeByText(crtNode, endNodes);
            if (similarNode == null) similarNode = specialNodeDescriber.findNodeByDesc(crtNode,endNodes);
            if (similarNode == null) similarNode = specialNodeDescriber.findNodeByPos(crtNode,endNodes);
            if (similarNode != null) {
                res.put(similarNode, 0.0);
                endNodes.add(similarNode);
                continue;
            }
        }
        return res;
    }

    private Map <AccessibilityNodeInfoRecord, Double> findAllNodesAccordingToNode(AccessibilityNodeInfoRecord crtNode, AccessibilityNodeInfoRecord patternNode, Set<AccessibilityNodeInfoRecord> endNodes) {
        Map <AccessibilityNodeInfoRecord, Double> res = new HashMap<>();
        if (endNodes.contains(crtNode)) return res;
        if (patternNode == null) return res;
        SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(patternNode);
        double dis = specialNodeDescriber.calNodeDistanceToDescriber(crtNode);
        if (dis < 1) res.put(crtNode, dis);
        for (AccessibilityNodeInfoRecord child : crtNode.children) res.putAll(this.findAllNodesAccordingToNode(child, patternNode, endNodes));
        return res;
    }

    private Map <AccessibilityNodeInfoRecord, Double> findAllFixedNodesAccordingToNode(AccessibilityNodeInfoRecord crtNode, AccessibilityNodeInfoRecord patternNode, Set<AccessibilityNodeInfoRecord> endNodes) {
        Map <AccessibilityNodeInfoRecord, Double> res = new HashMap<>();
        if (endNodes.contains(crtNode) || patternNode == null) return res;
        SpecialNodeDescriber specialNodeDescriber = new SpecialNodeDescriber(patternNode);
        AccessibilityNodeInfoRecord similarNode = specialNodeDescriber.findNodeByText(crtNode, endNodes);
        if (similarNode == null) similarNode = specialNodeDescriber.findNodeByDesc(crtNode,endNodes);
        if (similarNode == null) similarNode = specialNodeDescriber.findNodeByPos(crtNode,endNodes);
        if (similarNode != null) {
            res.put(similarNode, 0.0);
            endNodes.add(similarNode);
        }
        return res;
    }


    private List<InfoList> findAllInfoLists(AccessibilityNodeInfoRecord crtNode, SemanticBlock patternBlock, Set<AccessibilityNodeInfoRecord> endNodes) {
        SemanticBlock res = new SemanticBlock(null,crtNode, patternBlock);
        if (endNodes.contains(crtNode)) return res.getInfoLists();
        Set<AccessibilityNodeInfoRecord> rootSet = new HashSet<>();
        rootSet.add(crtNode);
        patternBlock.findInfoLists(res, new HashSet<>(endNodes), rootSet, endNodes);
        return res.getInfoLists();
    }

    private Map <AccessibilityNodeInfoRecord, Double> findAllHighlightItems(AccessibilityNodeInfoRecord crtNode, SemanticBlock patternBlock, Set<AccessibilityNodeInfoRecord> endNodes) {
        //TODO 高亮的tab栏按钮，整合wrl代码
        Map <AccessibilityNodeInfoRecord, Double> res = new HashMap<>();
        return res;
    }


    private Map <FuncButton, Double> findAllFuncButtons(AccessibilityNodeInfoRecord crtNode, SemanticBlock patternBlock, Set<AccessibilityNodeInfoRecord> endNodes) {
        SemanticBlock res = new SemanticBlock(null, crtNode, patternBlock);
        patternBlock.findFuncButtons(res, new HashSet<>(endNodes), crtNode, endNodes);
        return res.getFuncButtons();
    }

    private List<CheckBox> findAllCheckBoxes(AccessibilityNodeInfoRecord crtNode, SemanticBlock patternBlock, Set<AccessibilityNodeInfoRecord> endNodes) {
        SemanticBlock res = new SemanticBlock(null,crtNode, patternBlock);
        if (endNodes.contains(crtNode)) return res.getCheckBoxes();
        patternBlock.findCheckBoxes(res, new HashSet<>(endNodes), crtNode, endNodes);
        return res.getCheckBoxes();
    }

    private List<Dialog> findAllDialogs(AccessibilityNodeInfoRecord crtNode, SemanticBlock patternBlock, Set<AccessibilityNodeInfoRecord> endNodes) {
        SemanticBlock res = new SemanticBlock(null,crtNode, patternBlock);
        if (endNodes.contains(crtNode)) return res.getDialogs();
        patternBlock.findDialogs(res, new HashSet<>(endNodes), crtNode, endNodes);
        return res.getDialogs();
    }

    SemanticBlock updateSemanticBlock() {
        //todo: 界面部分更新
        return lastRootBlock;
    }


    SemanticBlock generateSemanticBlockFromBottom(AccessibilityNodeInfoRecord crtRoot, List<SemanticBlock> patternBlocks) {
        if (patternBlocks == null) return new SemanticBlock(null, crtRoot, null);
        List<SemanticBlock> queBlocks = getAllBlocks(patternBlocks);
        Set<SemanticBlock> candidateBlocks = new HashSet<>();
        for (SemanticBlock block : queBlocks)
            if (block.childrenBlock.size()==0)
                candidateBlocks.add(block);
        Set<AccessibilityNodeInfoRecord> endNodes = new HashSet<>();
        matchedBlocks = new ArrayList<>();
        while (candidateBlocks.size() > 0) {
            long startTime = System.currentTimeMillis();
            List<Pair<Double,SemanticBlock>> candidateRes = new ArrayList<>();
            Set<AccessibilityNodeInfoRecord> tmpEndNodes = new HashSet<>(endNodes);
            //clearBlockComponentList();
            for (SemanticBlock block : candidateBlocks) {
                candidateRes.addAll(generateSingleSemanticBlock(crtRoot, block, tmpEndNodes));
            }
            long midTime = System.currentTimeMillis();
            Collections.sort(candidateRes, new BlockCompare());
            List<SemanticBlock> matchedRes = new ArrayList<>();
            for (Pair<Double, SemanticBlock> crtPair : candidateRes) {
                if (!haveOverlapedPart(matchedRes, crtPair.second)) {
                    matchedRes.add(crtPair.second);
                }
            }
            Collections.sort(matchedRes, new BlockComparePosition());
            matchedBlocks.addAll(matchedRes);
            for (SemanticBlock semanticBlock : matchedRes) endNodes.add(semanticBlock.uiRoot);
            Set<SemanticBlock> lastCandidateBlocks = new HashSet<>(candidateBlocks);
            candidateBlocks.clear();
            for (SemanticBlock block : lastCandidateBlocks)
                if (block.parentBlock != null)
                    if (!candidateBlocks.contains(block.parentBlock))
                        candidateBlocks.add(block.parentBlock);
            long endTime = System.currentTimeMillis();
            System.out.println("Generate One Layer's blocks: "+Long.toString(endTime-startTime)+"ms");
            System.out.println("Generate semantic block: "+Long.toString(midTime-startTime)+"ms");
        }
        List<SemanticBlock> rootBlocks = new ArrayList<>();
        for (SemanticBlock semanticBlock : matchedBlocks)
            if (semanticBlock.patternBlock.parentBlock == null) rootBlocks.add(semanticBlock);
        assert(rootBlocks.size() > 0);
        SemanticBlock rootBlock = null;
        if (rootBlocks.size() == 1) {
            rootBlock = rootBlocks.get(0);
        }
        else {
            rootBlock = new SemanticBlock(null, crtRoot, null);
            rootBlock.addRoot(crtRoot);
            rootBlock.childrenBlock.addAll(rootBlocks);
        }
        if (rootBlock == null) return null;
        if (rootBlock.titleNode == null) {
            AccessibilityNodeInfoRecord node =  Utility.getTitieNode(rootBlock.roots.get(0));
            if (node == null) node = getTitle(rootBlock.roots.get(0));
            if (node != null) rootBlock.setTitle(null, node, false, SemanticBlock.titleMethodText);
        }
        rootBlock.refreshTree();
        return rootBlock;
    }

    private boolean haveOverlapedPart(List<SemanticBlock> matchedRes, SemanticBlock crtBlock) {
        for (SemanticBlock semanticBlock : matchedRes) {
            if (semanticBlock.uiRoot.absoluteId.startsWith(crtBlock.uiRoot.absoluteId)) return true;
            if (crtBlock.uiRoot.absoluteId.startsWith(semanticBlock.uiRoot.absoluteId)) return true;
            if (semanticBlock.uiRoot.isOverlapped(crtBlock.uiRoot)) return true;
        }
        return false;
    }

    private List<SemanticBlock> getAllBlocks(List<SemanticBlock> patternBlocks) {
        List<SemanticBlock> blocks = new ArrayList<>();
        blocks.addAll(patternBlocks);
        for (int head = 0; head < blocks.size(); head++) {
            SemanticBlock crtBlock = blocks.get(head);
            if (crtBlock.childrenBlock.size()>0) blocks.addAll(crtBlock.childrenBlock);
        }
        return blocks;
    }

    void generateTitle(SemanticBlock semanticBlock, List<SemanticBlock> patternBlocks, Set<AccessibilityNodeInfoRecord> rootSet) {
        for (SemanticBlock patternBlock : patternBlocks) {
            for (AccessibilityNodeInfoRecord root : rootSet)
                if (patternBlock.findTitle(semanticBlock, root)) return;
        }
    }

    void generateForwardButtons(SemanticBlock semanticBlock, List<SemanticBlock> patternBlocks, Set<AccessibilityNodeInfoRecord> rootSet) {
        Set<AccessibilityNodeInfoRecord> buttons = new HashSet<>();
        for (SemanticBlock patternBlock : patternBlocks) {
            for (AccessibilityNodeInfoRecord root : rootSet)
                patternBlock.findForwardButtons(buttons, root);

        }
        semanticBlock.setForwardButtons(buttons);
    }


    void generateBackwardButtons(SemanticBlock semanticBlock, List<SemanticBlock> patternBlocks, Set<AccessibilityNodeInfoRecord> rootSet) {
        Set<AccessibilityNodeInfoRecord> buttons = new HashSet<>();
        for (SemanticBlock patternBlock : patternBlocks) {
            for (AccessibilityNodeInfoRecord root : rootSet)
                patternBlock.findBackwardButtons(buttons, root);

        }
        semanticBlock.setBackwardButtons(buttons);
    }

    void generateInfoLists(SemanticBlock semanticBlock, List<SemanticBlock> patternBlocks, Set<AccessibilityNodeInfoRecord> rootSet) {
        Set<AccessibilityNodeInfoRecord> itemSet = new HashSet<>();
        for (SemanticBlock patternBlock : patternBlocks) {
            patternBlock.findInfoLists(semanticBlock, itemSet, rootSet, new HashSet<>());
        }
    }

    void generateTabItems(SemanticBlock semanticBlock, List<SemanticBlock> patternBlocks, Set<AccessibilityNodeInfoRecord> rootSet) {
        Set<AccessibilityNodeInfoRecord> tabItems = new HashSet<>();
        for (SemanticBlock patternBlock : patternBlocks) {
            for (AccessibilityNodeInfoRecord root : rootSet)
                patternBlock.findTabItems(tabItems, root);
        }
        semanticBlock.setTabItems(tabItems);
    }


    void generateHighlightItem(SemanticBlock semanticBlock, List<SemanticBlock> patternBlocks, Set<AccessibilityNodeInfoRecord> rootSet) {
        //TODO: 整合wrl的代码
    }


    void generateCorrespondingRegions(SemanticBlock semanticBlock, List<SemanticBlock> patternBlocks, Set<AccessibilityNodeInfoRecord> rootSet) {
        Set<AccessibilityNodeInfoRecord> regions = new HashSet<>();
        for (SemanticBlock patternBlock : patternBlocks) {
            for (AccessibilityNodeInfoRecord root : rootSet)
                patternBlock.findCorrespondingRegions(regions, root);

        }
        semanticBlock.setCorrespondingRegions(regions);
    }

    void generateFuncButtons(SemanticBlock semanticBlock, List<SemanticBlock> patternBlocks, Set<AccessibilityNodeInfoRecord> rootSet) {
        Set<AccessibilityNodeInfoRecord> buttons = new HashSet<>();
        for (SemanticBlock patternBlock : patternBlocks) {
            for (AccessibilityNodeInfoRecord root : rootSet)
                patternBlock.findFuncButtons(semanticBlock, buttons, root, new HashSet<>());
        }
    }

    void generateMenuButtons(SemanticBlock semanticBlock, List<SemanticBlock> patternBlocks, Set<AccessibilityNodeInfoRecord> rootSet) {
        Set<AccessibilityNodeInfoRecord> buttons = new HashSet<>();
        for (SemanticBlock patternBlock : patternBlocks) {
            for (AccessibilityNodeInfoRecord root : rootSet)
                patternBlock.findMenuButtons(buttons, root);

        }
        semanticBlock.setMenuButtons(buttons);
    }

    void generateSearchBlock(SemanticBlock semanticBlock, List<SemanticBlock> patternBlocks, Set<AccessibilityNodeInfoRecord> rootSet) {
        Set<AccessibilityNodeInfoRecord> buttonsIntoSearchPage = new HashSet<>();
        Set<AccessibilityNodeInfoRecord> searchTriggers = new HashSet<>();
        for (SemanticBlock patternBlock : patternBlocks) {
            for (AccessibilityNodeInfoRecord root : rootSet) {
                patternBlock.findButtonsIntoSearchPage(buttonsIntoSearchPage, root);
                patternBlock.findSearchTriggers(searchTriggers, root);
            }
        }
        semanticBlock.setButtonsIntoSearchPage(buttonsIntoSearchPage);
        semanticBlock.setSearchTriggers(searchTriggers);
        for (SemanticBlock patternBlock : patternBlocks) {
            for (AccessibilityNodeInfoRecord root : rootSet) {
                patternBlock.findVoiceInputButton(semanticBlock, root);
                patternBlock.findTextInputButton(semanticBlock, root);
            }
        }
    }

    void generateCheckBoxes(SemanticBlock semanticBlock, List<SemanticBlock> patternBlocks, Set<AccessibilityNodeInfoRecord> rootSet) {
        Set<AccessibilityNodeInfoRecord> nodeSet = new HashSet<>();
        for (SemanticBlock patternBlock : patternBlocks) {
            for (AccessibilityNodeInfoRecord root : rootSet) {
                patternBlock.findCheckBoxes(semanticBlock, nodeSet, root, new HashSet<>());
            }
        }
    }

    void generateDialogs(SemanticBlock semanticBlock, List<SemanticBlock> patternBlocks, Set<AccessibilityNodeInfoRecord> rootSet) {
        Set<AccessibilityNodeInfoRecord> nodeSet = new HashSet<>();
        for (SemanticBlock patternBlock : patternBlocks) {
            for (AccessibilityNodeInfoRecord root : rootSet) {
                patternBlock.findDialogs(semanticBlock, nodeSet, root, new HashSet<>());
            }
        }
    }


    static private class BlockCompare implements Comparator {
        public int compare(Object object1, Object object2) {// 实现接口中的方法
            Pair<Double, SemanticBlock> p1 = (Pair<Double, SemanticBlock>) object1; // 强制转换
            Pair<Double, SemanticBlock> p2 = (Pair<Double, SemanticBlock>) object2;
            if (p1.first != p2.first) return p2.first.compareTo(p1.first);
            return new Integer(p1.hashCode()).compareTo(new Integer(p2.hashCode()));
        }
    }


    static private class BlockComparePosition implements Comparator {
        public int compare(Object object1, Object object2) {// 实现接口中的方法
            SemanticBlock p1 = (SemanticBlock) object1; // 强制转换
            SemanticBlock p2 = (SemanticBlock) object2;
            Rect bound1 = new Rect();
            Rect bound2 = new Rect();
            p1.uiRoot.getBoundsInScreen(bound1);
            p2.uiRoot.getBoundsInScreen(bound2);
            if (bound1.top != bound2.top) return new Integer(bound1.top).compareTo(new Integer(bound2.top));
            if (bound1.bottom != bound2.bottom) return new Integer(bound1.bottom).compareTo(new Integer(bound2.bottom));
            if (bound1.left != bound2.left) return new Integer(bound1.left).compareTo(new Integer(bound2.left));
            if (bound1.right != bound2.right) return new Integer(bound1.right).compareTo(new Integer(bound2.right));
            return new Integer(p1.hashCode()).compareTo(new Integer(p2.hashCode()));
        }
    }
}
