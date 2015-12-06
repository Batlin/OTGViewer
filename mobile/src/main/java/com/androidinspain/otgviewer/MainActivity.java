package com.androidinspain.otgviewer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.androidinspain.otgviewer.fragments.ExplorerFragment;
import com.androidinspain.otgviewer.fragments.HomeFragment;
import com.androidinspain.otgviewer.fragments.SettingsFragment;
import com.androidinspain.otgviewer.ui.VisibilityManager;
import com.androidinspain.otgviewer.util.Utils;
import com.github.mjdev.libaums.UsbMassStorageDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements HomeFragment.HomeCallback, ExplorerFragment.ExplorerCallback, SettingsFragment.SettingsCallback {

    private String TAG = "OTGViewer";
    private boolean DEBUG = false;

    private List<UsbDevice> mDetectedDevices;

    private static final String ACTION_USB_PERMISSION = "com.androidinspain.otgviewer.USB_PERMISSION";
    private PendingIntent mPermissionIntent;
    private UsbManager mUsbManager;
    private UsbMassStorageDevice mUsbMSDevice;

    private Toolbar mToolbar;

    private HomeFragment mHomeFragment;
    private SettingsFragment mSettingsFragment;
    private ExplorerFragment mExplorerFragment;
    private VisibilityManager mVisibilityManager;

    private final int HOME_FRAGMENT = 0;
    private final int SETTINGS_FRAGMENT = 1;
    private final int EXPLORER_FRAGMENT = 2;

    private boolean mShowIcon = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);

        if(DEBUG)
            Log.d(TAG, "onCreate");

        mHomeFragment = new HomeFragment();
        mSettingsFragment = new SettingsFragment();
        mExplorerFragment = new ExplorerFragment();

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mDetectedDevices = new ArrayList<UsbDevice>();
        mVisibilityManager = new VisibilityManager();

        displayView(HOME_FRAGMENT);
    }

    private void displayView(int position) {
        Fragment fragment = null;

        switch (position) {
            case HOME_FRAGMENT:
                fragment = mHomeFragment;
                break;
            case SETTINGS_FRAGMENT:
                fragment = (Fragment) mSettingsFragment;
                break;
            case EXPLORER_FRAGMENT:
                fragment = mExplorerFragment;
                break;
            default:
                break;
        }

        String tag = Integer.toString(position);

        if (getFragmentManager().findFragmentByTag(tag) != null && getFragmentManager().findFragmentByTag(tag).isVisible()) {
            if(DEBUG)
                Log.d(TAG, "No transition needed. Already in that fragment!");

            return;
        }

        if (fragment != null) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.enter, R.animator.exit, R.animator.pop_enter, R.animator.pop_exit);;
            fragmentTransaction.replace(R.id.container_body, fragment, tag);

            // Home fragment is not added to the stack
            if(position != HOME_FRAGMENT){
                fragmentTransaction.addToBackStack(null);
            }

            fragmentTransaction.commitAllowingStateLoss();

            getFragmentManager().executePendingTransactions();
        }

    }

    @Override
    public void onBackPressed(){
        // Catch back action and pops from backstack
        // (if you called previously to addToBackStack() in your transaction)
        boolean result = false;

        if(mExplorerFragment!=null && mExplorerFragment.isVisible()){
            if(DEBUG)
                Log.d(TAG, "we are on ExplorerFragment. Result: " + result);

            result = mExplorerFragment.popUsbFile();
        }

        if(result)
            return;
        else if (getFragmentManager().getBackStackEntryCount() > 0 ){
            getFragmentManager().popBackStack();

            if(DEBUG)
                Log.d(TAG, "Pop fragment");
        }
        // Default action on back pressed
        else super.onBackPressed();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch(item.getItemId()) {
            case R.id.action_settings:
                displayView(SETTINGS_FRAGMENT);
                return true;
            case R.id.action_showcase:
                return false;
            case android.R.id.home:
                this.onBackPressed();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void removedUSB(){

        if(mVisibilityManager.getIsVisible()) {
            while (getFragmentManager().getBackStackEntryCount() != 0) {
                getFragmentManager().popBackStackImmediate();
            }

            displayView(HOME_FRAGMENT);
        } else {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(DEBUG)
                Log.d(TAG, "mUsbReceiver triggered. Action " + action);

            checkUSBStatus();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
                removedUSB();

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication
                            if(DEBUG)
                                Log.d(TAG, "granted permission for device " + device.getDeviceName() + "!");
                            connectDevice(device);
                        }
                    }
                    else {
                        Log.e(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    private void checkUSBStatus(){

        if(DEBUG)
            Log.d(TAG, "checkUSBStatus");

        mDetectedDevices.clear();

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        if(mUsbManager!=null) {
            HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

            if (!deviceList.isEmpty()) {
                Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
                while (deviceIterator.hasNext()) {
                    UsbDevice device = deviceIterator.next();
                    mDetectedDevices.add(device);
                }
            }

            updateUI();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(DEBUG)
            Log.d(TAG, "onResume");

        mVisibilityManager.setIsVisible(true);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);

        registerReceiver(mUsbReceiver, filter);

        checkUSBStatus();

    }

    @Override
    protected void onPause(){
        super.onPause();

        if(DEBUG)
            Log.d(TAG, "onPause");

        mVisibilityManager.setIsVisible(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mUsbReceiver);
        Utils.deleteCache();
    }

    private void updateUI(){
        if(DEBUG)
            Log.d(TAG, "updateUI");

        if(mHomeFragment!=null && mHomeFragment.isAdded()) {
            mHomeFragment.updateUI();
        }
    }

    private void connectDevice(UsbDevice device){
        if(DEBUG)
            Log.d(TAG, "Connecting to device");

        if(mUsbManager.hasPermission(device) && DEBUG)
            Log.d(TAG, "got permission!");

        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this);
        mUsbMSDevice = devices[0];

        setupDevice();
    }


    private void setupDevice() {
        displayView(EXPLORER_FRAGMENT);
    }

    @Override
    public void requestPermission(int pos) {
        mUsbManager.requestPermission(mDetectedDevices.get(pos), mPermissionIntent);
    }

    @Override
    public List<UsbDevice> getUsbDevices() {
        return mDetectedDevices;
    }

    @Override
    public void setABTitle(String title, boolean showMenu){

        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(showMenu);
        // Set logo
        getSupportActionBar().setDisplayShowHomeEnabled(mShowIcon);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
    }

}
