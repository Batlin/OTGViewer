package com.androidinspain.otgviewer.fragments;

/**
 * Created by roberto on 29/08/15.
 */

import android.app.Activity;
import android.app.ListFragment;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.androidinspain.otgviewer.R;

import java.util.ArrayList;
import java.util.List;


public class HomeFragment extends ListFragment {

    private String TAG = "HomeFragment";
    private boolean DEBUG = false;

    private HomeCallback mMainActivity;
    private List<UsbDevice> mDetectedDevices;

    public HomeFragment() {
        // Required empty public constructor
    }

    public interface HomeCallback {
        public List<UsbDevice> getUsbDevices();

        public void requestPermission(int pos);

        public void setABTitle(String title, boolean showMenu);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        updateUI();

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (DEBUG)
            Log.d(TAG, "onAttach");

        try {
            mMainActivity = (HomeCallback) activity;
        } catch (ClassCastException castException) {
            /** The activity does not implement the listener. */
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (DEBUG)
            Log.d(TAG, "onDetach");
    }

    public void updateUI() {

        if (DEBUG)
            Log.d(TAG, "updateUI in HomeFragment");

        mMainActivity.setABTitle(getString(R.string.home_title), false);
        mDetectedDevices = mMainActivity.getUsbDevices();

        List<String> showDevices = new ArrayList<String>();

        for (int i = 0; i < mDetectedDevices.size(); i++) {
            // API level 21
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                showDevices.add(mDetectedDevices.get(i).getProductName());
            } else {
                showDevices.add(mDetectedDevices.get(i).getDeviceName());
            }
        }

        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(getActivity(), R.layout.row_listdevices, R.id.listText, showDevices);

        // assign the list adapter
        setListAdapter(myAdapter);

    }

    // when an item of the list is clicked
    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);

        String selectedDevice = (String) getListView().getItemAtPosition(position);

        mMainActivity.requestPermission(position);

        if (DEBUG)
            Log.d(TAG, "You clicked " + selectedDevice + " at position " + position);
    }
}
