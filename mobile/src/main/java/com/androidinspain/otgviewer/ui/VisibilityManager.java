package com.androidinspain.otgviewer.ui;

/**
 * Created by roberto on 13/09/15.
 */
public class VisibilityManager {
    private boolean mIsVisible = false;

    public void setIsVisible(boolean visible) {
        mIsVisible = visible;
    }

    public boolean getIsVisible() {
        return mIsVisible;
    }
}