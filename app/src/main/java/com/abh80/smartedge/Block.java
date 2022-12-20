package com.abh80.smartedge;

import static com.abh80.smartedge.Utility.screenHeightPixel;
import static com.abh80.smartedge.Utility.screenWidthPixel;

import android.graphics.Rect;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class DisplayMode {
    public final static int SCREENSHOT = 0;
    public final static int WIDGET = 1;
}

public class Block {
    public AccessibilityNodeInfoRecord uiNode;
    public List<Block> childrenBlocks;
    Block parentBlock;

    Block() {
        uiNode = null;
        childrenBlocks = new ArrayList<>();
        parentBlock = null;
    }

    Block(AccessibilityNodeInfoRecord node) {
        uiNode = node;
        childrenBlocks = new ArrayList<>();
        parentBlock = null;
    }



    boolean neededProcess() {
        assert childrenBlocks.size() == 0;
        assert uiNode != null;
        if (uiNode.getClassName().toString().contains("ListView")) {
            Log.i("Block","ListView");
        }
        if (uiNode.getAllTexts().contains("发现")) {
            Log.i("Block","发现");
        }
        if (uiNode.children.size()==0) return false;
        for (AccessibilityNodeInfoRecord child : uiNode.children)
            if (child.isValid) return true;
        return false;
    }

    JSONObject convertBlockTreeToJson() {
        JSONObject res = new JSONObject();
        try {
            JSONArray children = new JSONArray();
            for (Block childBlock : childrenBlocks) {
                children.put(childBlock.convertBlockTreeToJson());
            }
            res.put("children",children);
            if (uiNode != null) {
                res.put("node_class",uiNode.getClassName());
                if (uiNode.getClassName().toString().contains("ListView")) {
                    Log.i("Block","ListView");
                }
                Rect r = new Rect();
                uiNode.getBoundsInScreen(r);
                res.put("ori_bound", r.toShortString());
                res.put("crt_bound",r.toShortString());
                res.put("checkable",uiNode.isCheckable());
                res.put("checked",uiNode.isChecked());
                res.put("clickable",uiNode.isClickable());
                res.put("enabled",uiNode.isEnabled());
                res.put("focusable",uiNode.isFocusable());
                res.put("focused",uiNode.isFocused());
                res.put("scrollable",uiNode.isScrollable());
                res.put("long_clickable",uiNode.isLongClickable());
                res.put("selected",uiNode.isSelected());
                res.put("editable",uiNode.isEditable());
                res.put("text",uiNode.getText());
                res.put("content_desc",uiNode.getContentDescription());
                //todo: res.put("color",uiNode.)
                if ((children.length()==0) || (uiNode.getClassName().toString().contains("Image")))
                    res.put("display_mode",DisplayMode.SCREENSHOT);
                else res.put("display_mode",DisplayMode.WIDGET);
            }
            else {
                res.put("display_mode", DisplayMode.WIDGET);
            }

            res.put("screen_bound", String.format("[%d,%d][%d,%d]", 0, 0, (int)screenWidthPixel, (int)screenHeightPixel));

            //todo
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }
}
