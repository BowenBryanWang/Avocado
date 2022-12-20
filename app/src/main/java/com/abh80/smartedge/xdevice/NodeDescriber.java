package com.abh80.smartedge.xdevice;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import pcg.actionrecord.AccessibilityNodeInfoRecord;
import pcg.actionrecord.AccessibilityNodeInfoRecordFromFile;
import pcg.actionrecord.Utility;

public class NodeDescriber {

    public static final String TAG = "NodeDescriber";
    public List<String> possibleFuncWords;
    //public PageDescriber pageDescriber;

    public boolean needPara;
    public boolean needSameItemTrans;
    public List<String> possibleFuncWordsForSameItemTrans;

    public boolean needSpecialDescriber;
    public String hintTextForEditable;
    public String actionParam;
    public boolean isEditable;
    public boolean usingID;
    public boolean forceDispatch;
    public boolean optional;
    public String resourceId;
    public boolean noHint;

    public String oriNodeId = null;
    public boolean forceListFirst;
    private String relativeId;
    private String listId;
    private String listResource;
    private String listType;


    public boolean setForceListFirst(AccessibilityNodeInfoRecord node){
        AccessibilityNodeInfoRecord listRoot = node.parent;
        while (listRoot != null){
            if(listRoot.isDynamicEntrance){
                break;
            }
            listRoot = listRoot.parent;
        }
        if(listRoot == null){
            return false;
        }

        forceListFirst = true;
        relativeId = node.getIdRelatedTo(listRoot);
        listId = listRoot.oriAbsoluteId;
        listResource = listRoot.getViewIdResourceName() == null? null: String.valueOf(listRoot.getViewIdResourceName());
        listType = String.valueOf(listRoot.getClassName());
        return true;
    }

    public NodeDescriber(AccessibilityNodeInfoRecord node){
        //pageDescriber = page;

        if(node != null){
            //AppDescriber app = pageDescriber.appBelongsTo;
            oriNodeId = node.oriAbsoluteId;
            if(node.getViewIdResourceName() != null){
                resourceId = String.valueOf(node.getViewIdResourceName());
            }
            // 如果是有hint-text的 edit text 就用hint text
            if(node.isEditable() && !Utility.isEmptyCS(node.getHintText())){
                hintTextForEditable = String.valueOf(node.getHintText());
            } else {
                hintTextForEditable = null;
            }

            if(node.isEditable()){
                isEditable = true;
            }

            // 这两个域根据实际的情况确定
            // possibleFuncWords = new ArrayList<>(FunctionWordDict.getInstance().getAllFuncWordInSubtree(node));
            // possibleFuncWords.sort((o1, o2) -> Integer.compare(o1.length(), o2.length()));
            // 所在的列表项中是不是还有其他的func word

            AccessibilityNodeInfoRecord dynamicItemRoot = Utility.getDynamicItemRootForNode(node);
            if(dynamicItemRoot == null){
                Set<Pair<String, AccessibilityNodeInfoRecord>> functionNodes = node.getAllTextNodes(null); //TODO：粗糙版本的获取所有文本节点，最终版应该是具有功能文本的节点
                        //FunctionWordDict.getInstance().getAllFunctionNodesInSubTree(node, null);

                possibleFuncWords = functionNodes.stream().map(p->p.first).collect(Collectors.toList());
                possibleFuncWords.sort((o1, o2) -> Integer.compare(o1.length(), o2.length()));
                needPara = false;
                needSameItemTrans = false;
                needSpecialDescriber = possibleFuncWords.isEmpty(); // 一个在静态区域却没有任何有效功能的节点；可能是那种仅包含动态文本的功能节点，需要特别定位
                return;
            }

            Set<Pair<String, AccessibilityNodeInfoRecord>> functionNodes =  node.getAllTextNodes(null); ///TODO：粗糙版本的获取所有文本节点，最终版应该是具有功能文本的节点
                    //FunctionWordDict.getInstance().getAllFunctionNodesInSubTree(node, null);
            Set<Pair<String, AccessibilityNodeInfoRecord>> validFunctionNodes = new HashSet<>();
            /*for(Pair<String, AccessibilityNodeInfoRecord> p: functionNodes){  // TODO: 判断该文本是否为valid function word
                if(app.isValidFuncWordConsiderAll(p.first, page)){
                    validFunctionNodes.add(p);
                } else {
                    Log.w(TAG, "NodeDescriber: not valid function nodes" + p.first);
                    if(Utility.isDebug){
                        app.isValidFuncWordConsiderAll(p.first, page);
                    }
                }
            }*/
            for (Pair<String, AccessibilityNodeInfoRecord> p: functionNodes) validFunctionNodes.add(p); //TODO: !!!!!注意，这里假定所有的文本都是valid function word（即：在页面上出现有且只有一次）
//            Set<Pair<String, AccessibilityNodeInfoRecord>> invalidFunctionNodes =
//                    functionNodes.stream().filter((p)-> !app.isValidFuncWordConsiderAll(p.first)).collect(Collectors.toSet());


            Set<Pair<String, AccessibilityNodeInfoRecord>> functionNodesInOtherPart =  dynamicItemRoot.getAllTextNodes(Collections.singleton(node));
                //TODO：粗糙版本的获取所有文本节点，最终版应该是具有功能文本的节点
                    //FunctionWordDict.getInstance().getAllFunctionNodesInSubTree(dynamicItemRoot, Collections.singleton(node));
            Set<Pair<String, AccessibilityNodeInfoRecord>> validFunctionNodesInOtherPart = new HashSet<>();
            /*for(Pair<String, AccessibilityNodeInfoRecord> p: functionNodesInOtherPart){
                if(app.isValidFuncWordConsiderAll(p.first, page)){ // TODO：判断该文本是否为valid function word
                    validFunctionNodesInOtherPart.add(p);
                } else {
                    Log.w(TAG, "NodeDescriber: not valid function nodes" + p.first);
                    if(Utility.isDebug){
                        app.isValidFuncWordConsiderAll(p.first, page);
                    }
                }
            }*/
            for(Pair<String, AccessibilityNodeInfoRecord> p: functionNodesInOtherPart) validFunctionNodesInOtherPart.add(p);
//            Set<Pair<String, AccessibilityNodeInfoRecord>> invalidFunctionNodesInOtherPart =
//                    functionNodesInOtherPart.stream().filter(p->!app.isValidFuncWordConsiderAll(p.first)).collect(Collectors.toSet());



            if(!validFunctionNodes.isEmpty()){
                // 直接根据点击的按钮进行节点的定位
                possibleFuncWords = validFunctionNodes.stream().map(p->p.first).collect(Collectors.toList());
                possibleFuncWords.sort((o1, o2) -> Integer.compare(o1.length(), o2.length()));
                needPara = false;
                needSameItemTrans = false;
            } else {
                if(!validFunctionNodesInOtherPart.isEmpty()){
                    possibleFuncWords = validFunctionNodesInOtherPart.stream().map(p->p.first).collect(Collectors.toList());
                    possibleFuncWords.sort((o1, o2) -> Integer.compare(o1.length(), o2.length()));
                    needPara = false;

                    needSameItemTrans = true;
                    possibleFuncWordsForSameItemTrans = functionNodes.stream().map(p->p.first).collect(Collectors.toList());
                    possibleFuncWordsForSameItemTrans.sort((o1, o2) -> Integer.compare(o1.length(), o2.length()));
                    // 如果 possibleFuncWordsForSameItemTrans 是空的，在这个item中找一个可以点击的节点；优先找节点不包含func word 的
                } else {
                    Set<Pair<String, AccessibilityNodeInfoRecord>> textInfoInOtherParts = dynamicItemRoot.getAllTextNodes(Collections.singleton(node));
                            //FunctionWordDict.getInstance().getAllTextNodesInSubTree(dynamicItemRoot, Collections.singleton(node));
                    Set<Pair<String, AccessibilityNodeInfoRecord>> textInfoInNode = node.getAllTextNodes(Collections.emptySet());
                            //FunctionWordDict.getInstance().getAllTextNodesInSubTree(node, Collections.emptySet());
                    if(textInfoInOtherParts.isEmpty() && textInfoInNode.isEmpty()){
                        needSpecialDescriber = true;
                        needPara = false;
                        possibleFuncWords = new ArrayList<>();
                    } else if(!textInfoInOtherParts.isEmpty()){
                        //    在这里进行相似性的判断
                        boolean similarItemFound = false;
                        List<AccessibilityNodeInfoRecord> siblingRootsInDynamicEntrance = new ArrayList<>(dynamicItemRoot.parent.children);
                        siblingRootsInDynamicEntrance.remove(dynamicItemRoot);
                        for(AccessibilityNodeInfoRecord otherRoot: siblingRootsInDynamicEntrance){
                            // 这里有更好的判断方案吗？ 可能和这里具体聚类的方法有关
                            if(Utility.isSubTreeSame(dynamicItemRoot, otherRoot, true,
                                    true, true,
                                    (n1, n2) -> n1.isDynamicEntrance && n2.isDynamicEntrance)){
                                similarItemFound = true;
                                break;
                            }
                        }

                        if(similarItemFound){
                            needPara = true;
                            possibleFuncWords = new ArrayList<>();
                            needSameItemTrans = true;
                            possibleFuncWordsForSameItemTrans = functionNodes.stream().map(p->p.first).collect(Collectors.toList());
                            possibleFuncWordsForSameItemTrans.sort((o1, o2) -> Integer.compare(o1.length(), o2.length()));
                        } else {
                            // 没有并列就没有参数
                            needSpecialDescriber = true;
                            needPara = false;
                            possibleFuncWords = new ArrayList<>();
                        }
                    } else {
                        // !textInfoInNode.isEmpty()
                        boolean similarItemFound = false;
                        List<AccessibilityNodeInfoRecord> siblingRootsInDynamicEntrance = new ArrayList<>(dynamicItemRoot.parent.children);
                        siblingRootsInDynamicEntrance.remove(dynamicItemRoot);
                        for(AccessibilityNodeInfoRecord otherRoot: siblingRootsInDynamicEntrance){
                            // 这里有更好的判断方案吗？ 可能和这里具体聚类的方法有关
                            if(Utility.isSubTreeSame(dynamicItemRoot, otherRoot, true,
                                    true, true,
                                    (n1, n2) -> n1.isDynamicEntrance && n2.isDynamicEntrance)){
                                similarItemFound = true;
                                break;
                            }
                        }
                        if(similarItemFound){
                            needPara = true;
                            possibleFuncWords = new ArrayList<>();
                            needSameItemTrans = false;
                        } else {
                            // 没有并列就没有参数 // todo 检查其正确性
                            needSpecialDescriber = true;
                            needPara = false;
                            possibleFuncWords = new ArrayList<>();
                        }
                    }
                }
            }
        } else {
            Log.w(TAG, "NodeDescriber: node not given!");
            possibleFuncWords = new ArrayList<>();
            needSameItemTrans = false;
            needPara = true;
        }
    }

    public NodeDescriber(Collection<String> funcWords){
        if(funcWords != null){
            possibleFuncWords = new ArrayList<>(funcWords);
            possibleFuncWords.sort((o1, o2) -> Integer.compare(o1.length(), o2.length()));
        } else {
            possibleFuncWords = new ArrayList<>();
        }
    }

    public boolean needParaForOneAction(AccessibilityNodeInfoRecordFromFile.Action.Type actionType){
        if(actionType == null){
            return false;
        }
        switch (actionType){
            case TYPE_GLOBAL_BACK:
            case TYPE_GLOBAL_HOME:
            case TYPE_GLOBAL_RECENT_APPS:
            case TYPE_NOTIFICATION_EXPANDED:
            // 下面两个如果need para，那么说明没有找到定位的文本，那么就不需要参数，找到可以支持的控件即可
            // 如果非 need para 那么就不需要参数，不仅要定位到 func word，还需要定位到可以支持的控件
            case TYPE_VIEW_TEXT_CHANGED:
            case TYPE_VIEW_SCROLLED:
                return false;
            case TYPE_VIEW_CLICKED:
            case TYPE_VIEW_LONG_CLICKED:
                return needPara;
        }

        return false;
    }

    private static Pattern postfix = Pattern.compile("^(.+?)\\s*[(（].+[)）]\\s*$"); // 用来删除文本后面的括号 确认（2）
    public static List<AccessibilityNodeInfoRecord> findNodeByFuncWordList(
            AccessibilityNodeInfoRecord root,
            AccessibilityNodeInfoRecordFromFile.Action.Type needSupportType, List<String> funcWords, Boolean isEditable){
        // 如果 need support type 是 null 那么对能够 support 的交互类型不做限制
        // 在一个界面上可能存在多个，需要将这些都返回供后续选择
        List<AccessibilityNodeInfoRecord> resultsExact = new ArrayList<>();
        List<AccessibilityNodeInfoRecord> resultsNotExact = new ArrayList<>();
        for(String funcWord: funcWords){
            Queue<AccessibilityNodeInfoRecord> nodeQueue = new LinkedList<>();
            nodeQueue.add(root);
            while (!nodeQueue.isEmpty()){
                AccessibilityNodeInfoRecord crt = nodeQueue.poll();
                if(crt == null){
                    continue;
                }

                if(isEditable != null && (!isEditable && crt.isEditable())){
                    continue;
                }

                boolean match = false;
                boolean matchNotExact = false;
                if(funcWord.equals("EDIT_TEXT_NODE")){
                    if(crt.isEditable()){
                        match = true;
                    }
                } else {
                    CharSequence textCS = crt.getText();
                    CharSequence contentCS = crt.getContentDescription();

                    if(textCS != null && textCS.length() > 0){
                        String text = String.valueOf(textCS);
                        Pair<String, Boolean> res = Utility.isTextFunc(text, crt.isInStaticRegion);
                        if(res.second && Objects.equals(res.first, funcWord)){
                            match = true;
                        }
                    }

                    if(contentCS != null && contentCS.length() > 0){
                        String content = String.valueOf(contentCS);
                        Pair<String, Boolean> res = Utility.isTextFunc(content, crt.isInStaticRegion);
                        if(res.second && Objects.equals(res.first, funcWord)){
                            match = true;
                        }
                    }

                    if(!match /*&& notExactSupport == null*/){
                        Matcher mFunc = postfix.matcher(funcWord);
                        String funcWordPart;
                        if(mFunc.find()){
                            funcWordPart = mFunc.group(1);
                        } else {
                            funcWordPart = funcWord;
                        }

                        if(textCS != null){
                            Matcher m = postfix.matcher(textCS);
                            if(m.find()){
                                String prefix = m.group(1);
                                Pair<String, Boolean> res = Utility.isTextFunc(prefix, crt.isInStaticRegion);
                                if(res.second && Objects.equals(res.first, funcWordPart)){
                                    match = true;
                                    matchNotExact = true;
                                }
                            }
                        }

                        if(contentCS != null && !match){
                            Matcher m = postfix.matcher(contentCS);
                            if(m.find()){
                                String prefix = m.group(1);
                                Pair<String, Boolean> res = Utility.isTextFunc(prefix, crt.isInStaticRegion);
                                if(res.second && Objects.equals(res.first, funcWordPart)){
                                    match = true;
                                    matchNotExact = true;
                                }
                            }
                        }

                    }

                }

                if(match){
                    AccessibilityNodeInfoRecord nodeSupport = Utility.getFirstAncestorSupport(crt, needSupportType, root);
                    if(nodeSupport != null && match && !matchNotExact){
                        // return nodeSupport;
                        resultsExact.add(nodeSupport);
                    } else {
                        resultsNotExact.add(nodeSupport == null? crt: nodeSupport);
                    }
                }

                nodeQueue.addAll(crt.children);
            }
        }

        resultsExact.addAll(resultsNotExact);
        return resultsExact;
    }

    public AccessibilityNodeInfoRecord findNode(
            AccessibilityNodeInfoRecord root, AccessibilityNodeInfoRecordFromFile.Action.Type needSupportType, String para){
        if(usingID){
            AccessibilityNodeInfoRecord res = root.getNodeByOriAbsoluteId(oriNodeId);
            if(res != null){
                return res;
            }

            if(resourceId != null && !resourceId.equals("android:id/search_src_text")){
                Queue<AccessibilityNodeInfoRecord> q = new LinkedList<>();
                q.add(root);
                while (!q.isEmpty()){
                    AccessibilityNodeInfoRecord n = q.poll();
                    if(n == null){
                        continue;
                    }
                    if(Objects.equals(resourceId, String.valueOf(n.getViewIdResourceName()))){
                        return n;
                    }

                    q.addAll(n.children);
                }
            }

        }
        if(forceListFirst){
            AccessibilityNodeInfoRecord listRoot = root.getNodeByOriAbsoluteId(listId);
            if(listRoot == null && listResource != null){
                Queue<AccessibilityNodeInfoRecord> q = new LinkedList<>();
                q.add(root);
                while (!q.isEmpty()){
                    AccessibilityNodeInfoRecord crt = q.poll();
                    if(crt == null){
                        continue;
                    }
                    if(crt.isDynamicEntrance && Objects.equals(listResource, String.valueOf(crt.getViewIdResourceName()))){
                        listRoot = crt;
                        break;
                    }

                    q.addAll(crt.children);
                }
            }
            if(listRoot == null){
                Queue<AccessibilityNodeInfoRecord> q = new LinkedList<>();
                q.add(root);
                while (!q.isEmpty()){
                    AccessibilityNodeInfoRecord crt = q.poll();
                    if(crt == null){
                        continue;
                    }
                    if(crt.isDynamicEntrance && Objects.equals(listType, String.valueOf(crt.getClassName()))){
                        listRoot = crt;
                        break;
                    }

                    q.addAll(crt.children);
                }
            }
            if(listRoot == null){
                return null;
            }

            return listRoot.getNodeByRelativeId(relativeId);
        }
        AccessibilityNodeInfoRecord res = findNodeByDesc(root, needSupportType, para);
        /*if(res == null){
            res = root.getNodeByOriAbsoluteId(oriNodeId);
        }*/

        return res;
    }

    public AccessibilityNodeInfoRecord findNodeByDesc(
            AccessibilityNodeInfoRecord root, AccessibilityNodeInfoRecordFromFile.Action.Type needSupportType, String para){

        // para == null if no para needed
        if(needParaForOneAction(needSupportType) && para == null){
            Log.w(TAG, "findNode: need para but not given");
            return null;
        }

        if(needSupportType == AccessibilityNodeInfoRecordFromFile.Action.Type.TYPE_VIEW_TEXT_CHANGED && hintTextForEditable != null && !noHint){
            Queue<AccessibilityNodeInfoRecord> nodeQueue = new LinkedList<>();
            nodeQueue.add(root);
            while (!nodeQueue.isEmpty()) {
                AccessibilityNodeInfoRecord crt = nodeQueue.poll();
                if (crt == null) {
                    continue;
                }

                if(Utility.canNodeSupport(crt, needSupportType) && Objects.equals(hintTextForEditable, String.valueOf(crt.getHintText()))){
                    if(noHint && crt.getHintText() != null){
                        Log.i(TAG, "findNodeByDesc: nohint");
                    } else {
                        return crt;
                    }

                }

                nodeQueue.addAll(crt.children);
            }

            return null;

        }

        if(needPara && !needParaForOneAction(needSupportType)){
            // 找到一个支持操作的就可以的
            Queue<AccessibilityNodeInfoRecord> nodeQueue = new LinkedList<>();
            nodeQueue.add(root);
            while (!nodeQueue.isEmpty()){
                AccessibilityNodeInfoRecord crt = nodeQueue.poll();
                if(crt == null){
                    continue;
                }
                if(Utility.canNodeSupport(crt, needSupportType)){
                    if(needSupportType == AccessibilityNodeInfoRecordFromFile.Action.Type.TYPE_VIEW_TEXT_CHANGED && crt.getHintText() != null && noHint){
                        Log.i(TAG, "findNodeByDesc: nohint");
                        continue;
                    }
                    return crt;
                }

                nodeQueue.addAll(crt.children);
            }

            return null;
        }

        if(needPara){
            // 先根据关键词找到对应的节点
            List<AccessibilityNodeInfoRecord> wordsByPara = Utility.findNodesByText(root, para);
            if(!needSameItemTrans){
                List<AccessibilityNodeInfoRecord> res = wordsByPara.stream()
                        .map(x->Utility.getFirstAncestorSupport(x, needSupportType, root))
                        .filter(Objects::nonNull).collect(Collectors.toList());
                if(res.isEmpty()){
                    if(wordsByPara.size() > 1){
                        Log.w(TAG, "findNode: multi nodes found");
                    }
                    return wordsByPara.get(0);
                }

                if(res.size() > 1){
                    Log.w(TAG, "findNode: multi nodes found");
                }
                return res.get(0);
            }

            // need same item trans
            List<Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord>> itemRootAndKeywordNode =
                    wordsByPara.stream().map(n-> new Pair<>(Utility.getDynamicItemRootForNode(n), n))
                            .filter(p->Objects.nonNull(p.first))
                            .collect(Collectors.toList());
            if(possibleFuncWordsForSameItemTrans == null || possibleFuncWordsForSameItemTrans.isEmpty()){
                List<AccessibilityNodeInfoRecord> nodesCanSupport =
                        itemRootAndKeywordNode.stream()
                                .map(p-> Utility.getNodesSupportingInteractionInSubTreeButNotAncestorOfGivenNodes(p.first, Collections.singleton(p.second), needSupportType))
                                .reduce(new ArrayList<>(), (before, crt)->{
                                    before.addAll(crt);
                                    return before;
                                });
                if(!nodesCanSupport.isEmpty()){
                    if(nodesCanSupport.size() > 1){
                        Log.w(TAG, "findNode: multiple nodes found");
                    }
                    return nodesCanSupport.get(0);
                }
                nodesCanSupport = itemRootAndKeywordNode.stream()
                        .map(p-> Utility.getNodesSupportingInteractionInSubTreeButNotAncestorOfGivenNodes(p.first, Collections.emptySet(), needSupportType))
                        .reduce(new ArrayList<>(), (before, crt)->{
                            before.addAll(crt);
                            return before;
                        });
                if(!nodesCanSupport.isEmpty()){
                    if(nodesCanSupport.size() > 1){
                        Log.w(TAG, "findNode: multi nodes found");
                    }
                    return nodesCanSupport.get(0);
                }

                Log.w(TAG, "findNode: node not found");
                return null;
            } else {
                // 确定了具体的参数转移策略
                List<AccessibilityNodeInfoRecord> resultNodes = new ArrayList<>();
                for(Pair<AccessibilityNodeInfoRecord, AccessibilityNodeInfoRecord> p: itemRootAndKeywordNode){
                    resultNodes.addAll(findNodeByFuncWordList(p.first, needSupportType, possibleFuncWordsForSameItemTrans, null)); // todo ??? 最后一个参数是对的吗
                }
                if(resultNodes.isEmpty()){
                    return null;
                }
            }

        } else {
            // 不需要使用参数
            if (possibleFuncWords == null || possibleFuncWords.isEmpty()){
                // todo 不需要参数，也不需要关键词文本进行定位。是需要使用像素的信息？
                return null;
            }

            List<AccessibilityNodeInfoRecord> nodesContainsKey = findNodeByFuncWordList(root, null, possibleFuncWords, isEditable);
            if(nodesContainsKey.isEmpty()){
                Log.w(TAG, "findNode: node containing key func word not found");
                findNodeByFuncWordList(root, null, possibleFuncWords, isEditable);
                return null;
            }

            if(!needSameItemTrans){
                for(AccessibilityNodeInfoRecord nodeContainsKey: nodesContainsKey) {
                    AccessibilityNodeInfoRecord nodeCanSupportInteract = Utility.getFirstAncestorSupport(nodeContainsKey, needSupportType);
                    if(nodeCanSupportInteract != null) {
                        return nodeCanSupportInteract;
                    }
                }

                Log.w(TAG, "findNode: node can support interact not found " + this);
                return nodesContainsKey.get(0);
            } else {
                if (possibleFuncWordsForSameItemTrans == null || possibleFuncWordsForSameItemTrans.isEmpty()) {
                    for(AccessibilityNodeInfoRecord nodeContainsKey: nodesContainsKey) {
                        AccessibilityNodeInfoRecord itemRoot = Utility.getDynamicItemRootForNode(nodeContainsKey);
                        if (itemRoot == null) {
                            Log.i(TAG, "findNode: item root not found");
                            continue;
                        }

                        List<AccessibilityNodeInfoRecord> nodeCanSupport =
                                Utility.getNodesSupportingInteractionInSubTreeButNotAncestorOfGivenNodes(itemRoot, Collections.singleton(nodeContainsKey), needSupportType);
                        if (!nodeCanSupport.isEmpty()) {
                            if (nodeCanSupport.size() > 1) {
                                Log.w(TAG, "findNode: multi nodes found");
                            }
                            return nodeCanSupport.get(0);
                        }

                        // 在整个item中查找
                        nodeCanSupport = Utility.getNodesSupportingInteractionInSubTreeButNotAncestorOfGivenNodes(itemRoot, Collections.emptySet(), needSupportType);
                        if (nodeCanSupport.isEmpty()) {
                            Log.w(TAG, "findNode: node not found");
                            continue;
                        }

                        if (nodeCanSupport.size() > 1) {
                            Log.w(TAG, "findNode: multi nodes found");
                        }
                        return nodeCanSupport.get(0);
                    }
                } else {
                    // 有具体的转移策略 在 item 中根据具体要求找到对应的节点
                    for(AccessibilityNodeInfoRecord nodeContainsKey: nodesContainsKey){
                        AccessibilityNodeInfoRecord itemRoot = Utility.getDynamicItemRootForNode(nodeContainsKey);
                        if (itemRoot == null) {
                            Log.i(TAG, "findNode: item root not found");
                            continue;
                        }
                        List<AccessibilityNodeInfoRecord> nodeToAct = findNodeByFuncWordList(itemRoot, needSupportType, possibleFuncWordsForSameItemTrans, null);
                        if (nodeToAct.isEmpty()) {
                            Log.w(TAG, "findNode: node not found");
                            continue;
                        }
                        return nodeToAct.get(0);
                    }
                }
            }
        }

        return null; // why here??
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeDescriber describer = (NodeDescriber) o;
        return needPara == describer.needPara &&
                needSameItemTrans == describer.needSameItemTrans &&
                Objects.equals(possibleFuncWords, describer.possibleFuncWords) &&
                Objects.equals(possibleFuncWordsForSameItemTrans, describer.possibleFuncWordsForSameItemTrans);
    }

    @Override
    public int hashCode() {
        return Objects.hash(possibleFuncWords, needPara, needSameItemTrans, possibleFuncWordsForSameItemTrans);
    }

    public boolean canDescribe(AccessibilityNodeInfoRecord node){
        // todo
        if(possibleFuncWords.isEmpty()){
            return true;
        }
        Set<String> allFuncWords =  node.getAllWords(); //TODO: 粗糙版本，最终应改成获取所有功能文本
                //FunctionWordDict.getInstance().getAllFuncWordInSubtree(node);
        allFuncWords.retainAll(possibleFuncWords);
        return !allFuncWords.isEmpty();
    }

    public boolean canDescribe(NodeDescriber other){
        // todo
        if(possibleFuncWords.isEmpty()){
            return true;
        }

        Set<String> funcWordCopied = new HashSet<>(other.possibleFuncWords);
        funcWordCopied.retainAll(possibleFuncWords);
        return !funcWordCopied.isEmpty();
    }

    @Override
    public String toString() {
        return "NodeDescriber " + (needPara? "* ": "  ") + possibleFuncWords + (needSameItemTrans? " -> " : " xx ") + possibleFuncWordsForSameItemTrans;
    }

    public String toFileName(AccessibilityNodeInfoRecordFromFile.Action.Type type){
        String res =  type + " nd" + (needSpecialDescriber? "! ": " ") + (possibleFuncWords != null && possibleFuncWords.size() > 1? '[' + possibleFuncWords.get(0) + " ...]" :possibleFuncWords)
                + (needSameItemTrans? " -> " : " xx ")
                + (possibleFuncWordsForSameItemTrans != null && possibleFuncWordsForSameItemTrans.size() > 1? "["+ possibleFuncWordsForSameItemTrans.get(0) + "...]" :possibleFuncWordsForSameItemTrans);
        return Utility.toValidFileName(res);
    }


}
