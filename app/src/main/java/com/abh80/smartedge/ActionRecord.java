package com.abh80.smartedge;

import android.graphics.Bitmap;

import org.json.JSONArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ActionRecord implements Serializable
{
    private String srcLayout;
    private JSONArray srcLayoutJson;
    private Bitmap srcImage;
    private String dstLayout;
    private JSONArray dstLayoutJson;
    private AccessibilityNodeInfoRecord crtRoot;
    private Bitmap dstImage;
    private List<Action> actionList;

    public ActionRecord(String srcLayout,String dstLayout,
                        Bitmap srcImage, Bitmap dstImage,List<Action> actionList)
    {
        this.srcLayout = srcLayout;
        this.dstLayout = dstLayout;
        this.actionList = new ArrayList<>(actionList);
        if(srcImage!=null)
            this.srcImage = srcImage.copy(Bitmap.Config.ARGB_8888,true);
        if(dstImage!=null)
            this.dstImage = dstImage.copy(Bitmap.Config.ARGB_8888,true);
    }

    @Override
    protected void finalize() throws Throwable
    {
        if(srcImage!=null)
        {
            srcImage.recycle();
            srcImage = null;
        }
        if(dstImage!=null)
        {
            dstImage.recycle();
            dstImage = null;
        }
        super.finalize();
    }

    public void setDstLayout(String dstLayout)
    {
        this.dstLayout = dstLayout;
    }

    public List<Action> getActionList()
    {
        return actionList;
    }

    public void setSrcLayout(String srcLayout)
    {
        this.srcLayout = srcLayout;
    }

    public String getDstLayout()
    {
        return dstLayout;
    }

    public void setActionList(List<Action> actionList)
    {
        this.actionList = actionList;
    }

    public String getSrcLayout()
    {
        return srcLayout;
    }

    public Bitmap getDstImage()
    {
        return dstImage;
    }

    public Bitmap getSrcImage()
    {
        return srcImage;
    }

    public JSONArray getDstLayoutJson() { return dstLayoutJson; }

    public AccessibilityNodeInfoRecord getCrtRoot() { return crtRoot; }
}
