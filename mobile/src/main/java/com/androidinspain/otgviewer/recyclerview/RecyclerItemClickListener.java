package com.androidinspain.otgviewer.recyclerview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.androidinspain.otgviewer.util.Utils;


public class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {
    private OnItemClickListener mListener;

    private RecyclerView mRecyclerView;

    private final String TAG = getClass().getName();
    private boolean DEBUG = false;

    public interface OnItemClickListener {
        public void onItemClick(View view, int position);

        public void onLongItemClick(View view, int position);
    }

    GestureDetector mGestureDetector;

    public RecyclerItemClickListener(Context context, final RecyclerView recyclerView, OnItemClickListener listener) {
        mListener = listener;
        mRecyclerView = recyclerView;
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View child = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
                if (child != null && mListener != null) {
                    if (DEBUG)
                        Log.d(TAG, "onLongPress!");
                    mListener.onLongItemClick(child, mRecyclerView.getChildAdapterPosition(child));
                }
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        if (DEBUG)
            Log.d(TAG, "onInterceptTouchEvent " + e.toString());

        View childView = view.findChildViewUnder(e.getX(), e.getY());
        if (childView != null && mListener != null && mGestureDetector.onTouchEvent(e)) {
            mListener.onItemClick(childView, view.getChildAdapterPosition(childView));
            if (DEBUG)
                Log.d(TAG, "onInterceptTouchEvent: singleClick!");
            return true;
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) {
        if (DEBUG)
            Log.d(TAG, "onTouchEvent " + motionEvent.toString());

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    public boolean handleDPad(View child, int keyCode, KeyEvent keyEvent) {

        Log.d(TAG, "handleDPad");
        int position = mRecyclerView.getChildLayoutPosition(child);

        // Return false if scrolled to the bounds and allow focus to move off the list
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (Utils.isConfirmButton(keyEvent)) {
                if ((keyEvent.getFlags() & KeyEvent.FLAG_LONG_PRESS) == KeyEvent.FLAG_LONG_PRESS) {
                    mListener.onLongItemClick(child, position);
                } else {
                    keyEvent.startTracking();
                }
                return true;
            }
        } else if (keyEvent.getAction() == KeyEvent.ACTION_UP && Utils.isConfirmButton(keyEvent)
                && ((keyEvent.getFlags() & KeyEvent.FLAG_LONG_PRESS) != KeyEvent.FLAG_LONG_PRESS)) {
            mListener.onItemClick(child, position);
            return true;
        }
        return false;
    }
}
