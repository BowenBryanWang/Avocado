package com.abh80.smartedge;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.Serializable;

public class Action implements Serializable
{
    int type;
    String typeString;
    String targetNodeId;
    String param;
    String hint;
    Long timestamp;

    public Action(int type,String targetNodeId,String param,String hint,Long timestamp)
    {
        this.type = type;
        this.targetNodeId = targetNodeId;
        this.param = param;
        this.hint = hint;
        this.timestamp = timestamp;
        this.typeString = AccessibilityEvent.eventTypeToString(type);
        if(type == AccessibilityNodeInfo.ACTION_CLICK)
        {
            switch (targetNodeId)
            {
                case "最近运行的应用":
                    this.typeString = "GLOBAL_ACTION_RECENTS";
                    break;
                case "主屏幕":
                    this.typeString = "GLOBAL_ACTION_HOME";
                    break;
                case "返回":
                    this.typeString = "GLOBAL_ACTION_BACK";
                    break;
            }
        }
    }
    public Action(int type,String targetNodeId,String param,Long timestamp)
    {
        this(type,targetNodeId,param,"",timestamp);
    }
    public Action(String type,String targetNodeId,String param)
    {
        this.type = 0;
        this.targetNodeId = targetNodeId;
        this.param = param;
        this.typeString = type;
    }

    public String getTargetNodeId()
    {
        return targetNodeId;
    }

    public String getTypeString()
    {
        return typeString;
    }

    public int getType()
    {
        return type;
    }

    public String getParam()
    {
        return param;
    }

    public void setParam(String param)
    {
        this.param = param;
    }

    public void setTargetNodeId(String targetNodeId)
    {
        this.targetNodeId = targetNodeId;
    }

    public void setTypeString(String typeString)
    {
        this.typeString = typeString;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
}
