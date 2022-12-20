package com.abh80.smartedge;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class ActionEventRecord {

    public static int ACTION_TYPE_ACCESSIBILITY = 0;
    public static int ACTION_TYPE_ACTION = 1;

    public int actionType;
    public int eventType;
    public Long timestamp;
    public AccessibilityNodeInfo source;
    public CharSequence packageName;
    public CharSequence classname;
    public Action action;
    public List<CharSequence> texts;

    public ActionEventRecord(AccessibilityEvent event) {
        actionType = ACTION_TYPE_ACCESSIBILITY;
        eventType = event.getEventType();
        source = event.getSource();
        packageName = event.getPackageName();
        classname = event.getClassName();
        timestamp = System.currentTimeMillis();
        texts = event.getText();
    }

    public ActionEventRecord(Action action) {
        actionType = ACTION_TYPE_ACTION;
        this.action = action;
        timestamp = System.currentTimeMillis();
    }

}