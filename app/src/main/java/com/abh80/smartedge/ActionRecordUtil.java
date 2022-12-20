package com.abh80.smartedge;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

public class ActionRecordUtil
{
    public static int ACTION_COPY = 1;
    public static int ACTION_PASTE = 2;


    private Context context;
    private String clipBoard;

    public ActionRecordUtil(Context context)
    {
        this.context = context;
    }

    public void Copy(String str)
    {
        clipBoard = str;
        ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("text", str);
        clipboard.setPrimaryClip(clip);

    }

    public String Paste(AccessibilityNodeInfoRecord nodeInfo)
    {
        Bundle bundle = new Bundle();
        bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, clipBoard);
        if(nodeInfo!=null)
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,bundle);
        return clipBoard;
    }

}
