package com.androidinspain.otgviewer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidinspain.otgviewer.adapters.UsbFilesAdapter;
import com.androidinspain.otgviewer.fragments.SettingsFragment;
import com.androidinspain.otgviewer.task.CopyTaskParam;
import com.androidinspain.otgviewer.ui.TouchImageView;
import com.androidinspain.otgviewer.util.Utils;
import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.lang.System;

/**
 * Created by roberto on 21/08/15.
 */
public class ImageViewerActivity extends AppCompatActivity implements SensorEventListener {

    private String TAG = getClass().getSimpleName();
    private boolean DEBUG = false;

    private UsbFile mCurrentDirectory;
    private UsbFile mCurrentFile;

    private ArrayList<UsbFile> mImagesFiles;

    private String TAG_NEXT_SHOWCASE = "NEXT_SHOWCASE";

    private int mTotalCount = 0;
    private int mCurrentCount = 0;
    private int mLayoutHeight = 0;
    private int mLayoutWidth = 0;

    private ImagePagerAdapter mAdapter;
    private ViewPager mViewPager;

    private List<CopyTask> copyArray = new ArrayList<CopyTask>();

    private Toolbar mToolbar;
    private GestureDetector mGestureDetector;

    private boolean mImmersive = false;
    private TextView mFooter;
    private ImageView mPausePlay;
    private View mDecorView;
    private boolean mIsShowcase;
    private boolean mShowcaseRunning;

    // Settings
    private boolean mTransitionsEnabled;
    private boolean mLowRam;
    private boolean mShakeEnabled;
    private int mShowcaseSpeed;

    private final int NEXT_SHOWCASE = 0;
    private final int SHOW_TUTORIAL = 1;
    private final int SHOW_TUTORIAL_DELAY = 4000;
    private final int NEXT_SHOWCASE_RETRY_TIMEOUT_MS = 400;
    private final int BUFFER = 2;

    private String TUTORIAL_SP = "tutorialPassed";

    // UI
    private final int FADE_TIMEOUT = 1000;

    private UsbFilesAdapter mUsbAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_viewer);

        mCurrentFile = ImageViewer.getInstance().getCurrentFile();
        mCurrentDirectory = ImageViewer.getInstance().getCurrentDirectory();
        mUsbAdapter = ImageViewer.getInstance().getAdapter();
        mImagesFiles = new ArrayList<UsbFile>();
        mFooter = (TextView) findViewById(R.id.titleActivity);
        mPausePlay = (ImageView) findViewById(R.id.pause_play_icon);

        mIsShowcase = getIntent().getBooleanExtra("SHOWCASE", false);
        if (mIsShowcase) {
            mImmersive = true;
            mHandler.sendEmptyMessageDelayed(SHOW_TUTORIAL,SHOW_TUTORIAL_DELAY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        int localCount = 0;

        try {
            mImagesFiles = mUsbAdapter.getImageFiles();

            mTotalCount = mImagesFiles.size();

            for (UsbFile file : mImagesFiles) {
                Log.d(TAG, "localCount " + localCount);
                if (file.getName().equalsIgnoreCase(mCurrentFile.getName())) {
                    mCurrentCount = localCount;
                }

                localCount++;
            }
        } catch (Exception e) {
            Log.e(TAG, "error setting up device", e);
        }

        if(DEBUG) {
            Log.d(TAG, "mCurrentFile " + mCurrentFile.getName());
            Log.d(TAG, "mTotalCount " + mTotalCount);
            Log.d(TAG, "mCurrentCount " + mCurrentCount);
        }

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mAdapter = new ImagePagerAdapter();
        ViewPager.OnPageChangeListener onPageChangeListener = new VPOnPageChangeListener();
        mViewPager.addOnPageChangeListener(onPageChangeListener);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(mCurrentCount);
        onPageChangeListener.onPageSelected(mCurrentCount);
        mViewPager.setOffscreenPageLimit(BUFFER);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mLayoutWidth = size.x;
        mLayoutHeight = size.y;

        mDecorView = getWindow().getDecorView();

        mGestureDetector = new GestureDetector(this, new GestureTap());

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(DEBUG)
            Log.d(TAG, "ViewPager width: " + mLayoutWidth + ", height: " + mLayoutHeight);

    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NEXT_SHOWCASE:
                    int nextImage = mCurrentCount + 1;

                    if(DEBUG) {
                        Log.d(TAG_NEXT_SHOWCASE, "mCurrentCount: " + mCurrentCount);
                        Log.d(TAG_NEXT_SHOWCASE, "mAdapter.getCount(): " + mAdapter.getCount());
                        Log.d(TAG_NEXT_SHOWCASE, "nextImage: " + nextImage);
                    }

                    if (nextImage < mAdapter.getCount()){

                        if(isLayoutReady(nextImage)) {
                            mCurrentCount++;

                            mViewPager.setCurrentItem(mCurrentCount, mTransitionsEnabled);
                            if(DEBUG)
                                Log.d(TAG_NEXT_SHOWCASE, "isImageReady(" + nextImage + ") is true. Showing photo " + mCurrentCount + ". Sending NEXT_SHOWCASE message in " + mShowcaseSpeed);
                            sendEmptyMessageDelayed(NEXT_SHOWCASE, mShowcaseSpeed);
                        }else{
                            if(DEBUG)
                                Log.d(TAG_NEXT_SHOWCASE, "isImageReady( " + nextImage + ") is false. Trying again in " + NEXT_SHOWCASE_RETRY_TIMEOUT_MS + "ms");
                            sendEmptyMessageDelayed(NEXT_SHOWCASE, NEXT_SHOWCASE_RETRY_TIMEOUT_MS);
                        }
                    }

                    break;
                case SHOW_TUTORIAL:
                    showToastTutorial();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private boolean isLayoutReady(int position) {
        RelativeLayout container = (RelativeLayout) mViewPager.findViewWithTag("pos" + position);

        int visibility = View.VISIBLE;

        if(container!=null)
            visibility = container.findViewById(R.id.loading).getVisibility();

        return visibility == View.GONE;

    }

    // We show it only the first time
    private void showToastTutorial(){
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putBoolean(TUTORIAL_SP, true);
        editor.apply();

        Toast.makeText(this, getString(R.string.showcase_tutorial),Toast.LENGTH_LONG).show();
    }

    private boolean isTutorialNeeded(){
        /*
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        boolean tutorialPassed = prefs.getBoolean(TUTORIAL_SP, false);

        if(!tutorialPassed){
            return true;
        }

        return false;
        */

        return true;
    }

    private boolean isImageReady(File f) {
        if (f.exists())
            return true;
        else
            return false;
    }

    private void setImmersiveMode(){

        int visibility;
        if(DEBUG)
            Log.d(TAG, "setImmersiveMode: " + mImmersive);

        if(mImmersive){
            visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE;

            mFooter.animate().alpha(0.0f);
            if(mIsShowcase && !mShowcaseRunning){
                startShowcase();
            }

        } else {
            visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

            mFooter.animate().alpha(1.0f);
            if(mIsShowcase && mShowcaseRunning)
                stopShowcase();
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mDecorView.setSystemUiVisibility(visibility);
        }

    }

    private void startShowcase(){
        mHandler.removeMessages(NEXT_SHOWCASE);
        mHandler.sendEmptyMessageDelayed(NEXT_SHOWCASE, mShowcaseSpeed);
        mShowcaseRunning = true;
        showFadeIcon(getResources().getDrawable(R.drawable.play_icon));

        /*
        if(isTutorialNeeded())
            mHandler.sendEmptyMessageDelayed(SHOW_TUTORIAL,SHOW_TUTORIAL_DELAY);
        */
    }

    private void stopShowcase(){
        mHandler.removeMessages(NEXT_SHOWCASE);
        mShowcaseRunning = false;
        showFadeIcon(getResources().getDrawable(R.drawable.pause_icon));
    }

    private void showFadeIcon(Drawable icon){
        mPausePlay.setImageDrawable(icon);
        mPausePlay.setAlpha(1f);
        mPausePlay.setVisibility(View.VISIBLE);
        mPausePlay.animate().alpha(0f).setStartDelay(FADE_TIMEOUT);
    }

    private Bitmap getImageResourceFromPosition(int position) {

        if(DEBUG)
            Log.d(TAG, "decode file from " + getCacheFullPath(mImagesFiles.get(position).getName()));

        Bitmap bitmap = null;

        File f = new File(getCacheFullPath(mImagesFiles.get(position).getName()));

        if(isImageReady(f)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = Utils.calculateInSampleSize(f, mLayoutWidth, mLayoutHeight);

            if(mLowRam || System.getProperty("ro.config.low_ram", "false").equals("true"))
                opts.inSampleSize *= 2;

            if(DEBUG)
                Log.d(TAG, "file exists! inSampleSize: " + opts.inSampleSize);

            opts.inPreferQualityOverSpeed = false;
            opts.inMutable = false;
            opts.inDither = false;

            bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(), opts);

            final int maxSize = Math.max(mLayoutWidth, mLayoutHeight);
            int outWidth;
            int outHeight;
            if(bitmap!=null){
                int inWidth = bitmap.getWidth();
                int inHeight = bitmap.getHeight();

                if(inWidth > inHeight){
                    outWidth = maxSize;
                    outHeight = (inHeight * maxSize) / inWidth;
                } else {
                    outHeight = maxSize;
                    outWidth = (inWidth * maxSize) / inHeight;
                }

                if(DEBUG)
                    Log.d(TAG, "outWidth: " + outWidth + ", outHeight: " + outHeight);

                bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true);
            }

            return bitmap;
        } else {
            if(DEBUG)
                Log.d(TAG, "file doesn't exist! Starting asynctask");

            CopyTaskParam param = new CopyTaskParam();
            param.from = mImagesFiles.get(position);
            param.to = f;
            param.position = position;

            CopyTask cp = new CopyTask(this);
            cp.execute(param);
            copyArray.add(cp);

            return null;
        }

    }

    private String getCacheFullPath(String fileName) {
        return Utils.cachePath + "/" + fileName;
    }

    @Override
    public void onBackPressed() {
        setResult(mCurrentCount);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Load configuration
        mTransitionsEnabled = SettingsFragment.areTransitionsEnabled(this);
        mLowRam = SettingsFragment.isLowRamEnabled(this);
        mShakeEnabled = SettingsFragment.isShakeEnabled(this);
        mShowcaseSpeed = SettingsFragment.getShowcaseSpeed(this);

        if(!mTransitionsEnabled)
            mViewPager.setPageTransformer(false, new NoPageTransformer());

        if(DEBUG)
            Log.d(TAG, "mLowRam: " + mLowRam + ", mShakeEnabled: " + mShakeEnabled);

        if(!mIsShowcase && mShakeEnabled)
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        setImmersiveMode();
        /*
        if(mIsShowcase && !mShowcaseRunning)
            startShowcase();
        */
    }
    @Override
    protected void onPause() {
        super.onPause();

        if(!mIsShowcase && mShakeEnabled)
            mSensorManager.unregisterListener(this);

        if(mIsShowcase && mShowcaseRunning)
            stopShowcase();

    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        for(CopyTask cp : copyArray){
            cp.cancel(true);
        }
    }

    private class VPOnPageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            //Log.d(TAG, "onPageScrolled: " + position);
        }

        @Override
        public void onPageSelected(int position) {
            if(DEBUG)
                Log.d(TAG, "onPageSelected: " + position);

            mCurrentCount = position;
            mFooter.setText(mImagesFiles.get(position).getName());
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }


    private class ImagePagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mTotalCount;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((RelativeLayout) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Context context = ImageViewerActivity.this;

            return buildLayout(container, position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager) container).removeView((RelativeLayout) object);
        }

    }


    private class CopyTask extends AsyncTask<CopyTaskParam, Integer, Bitmap> {

        private ProgressDialog dialog;
        private CopyTaskParam param;
        private ImageViewerActivity parent;


        public CopyTask(ImageViewerActivity act) {
            parent = act;
        }

        @Override
        protected void onPreExecute() {
            //dialog.show();
            if(param!=null && DEBUG) {
                Log.d(TAG, "Starting CopyTask with " + param.from.getName());
            }
        }

        @Override
        protected void onCancelled(Bitmap result){
            // Remove uncompleted data file
            if(DEBUG)
                Log.d(TAG, "Removing uncomplete file transfer");

            if(param != null && param.to.exists())
                param.to.delete();
        }

        @Override
        protected Bitmap doInBackground(CopyTaskParam... params) {
            long time = System.currentTimeMillis();
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            param = params[0];
            long length = params[0].from.getLength();
            try {
                FileOutputStream out = new FileOutputStream(param.to);
                    for (long i = 0; i < length; i += buffer.limit()) {
                        if(!isCancelled()) {
                            buffer.limit((int) Math.min(buffer.capacity(), length - i));
                            params[0].from.read(i, buffer);
                            out.write(buffer.array(), 0, buffer.limit());
                            buffer.clear();
                        }
                    }
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "error copying!", e);
            }

            if(DEBUG)
                Log.d(TAG, "copy time: " + (System.currentTimeMillis() - time));
            return getImageResourceFromPosition(param.position);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if(DEBUG)
                Log.d(TAG, "CopyTask done!");

            RelativeLayout container = (RelativeLayout) mViewPager.findViewWithTag("pos" + param.position);

            if(container!=null) {
                if(DEBUG)
                    Log.d(TAG, "container is not null");

                final TouchImageView imageView = (TouchImageView) container.findViewById(R.id.image);
                final ProgressBar spinner = (ProgressBar) container.findViewById(R.id.loading);

                imageView.setImageBitmap(result);
                spinner.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
            }

            if(DEBUG)
                Log.d(TAG, "onPostExecute. builtLayout: " + param.position);

            copyArray.remove(this);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //dialog.setMax((int) param.from.getLength());
            //dialog.setProgress(values[0]);
        }

    }

    private RelativeLayout buildLayout(ViewGroup container, int position){

        LayoutInflater inflater = (LayoutInflater) container.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final RelativeLayout imageLayout = (RelativeLayout) inflater.inflate(R.layout.viewpager_layout, null);
        final TouchImageView imageView = (TouchImageView) imageLayout.findViewById(R.id.image);
        final ProgressBar spinner = (ProgressBar) imageLayout.findViewById(R.id.loading);

        int padding = 0;
        imageLayout.setBackgroundColor(Color.BLACK);
        imageView.setPadding(padding, padding, padding, padding);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        Bitmap image = getImageResourceFromPosition(position);
        if (image==null){
            spinner.setIndeterminate(true);
            imageView.setVisibility(View.GONE);
            spinner.setVisibility(View.VISIBLE);
        } else {
            imageView.setImageBitmap(image);
            spinner.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
        }

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(mGestureDetector.onTouchEvent(event)){
                    return true;
                }

                return false;
            }
        });

        ((ViewPager) container).addView(imageLayout, 0);
        imageLayout.setTag("pos" + position);

        return imageLayout;

    }

    private void decreaseSpeed(){
        if(mShowcaseRunning) {
            mShowcaseSpeed *= SPEED_FACTOR;
            showFadeIcon(getResources().getDrawable(R.drawable.fr_icon));
        }
    }

    private void increaseSpeed(){
        if(mShowcaseRunning) {
            mShowcaseSpeed /= SPEED_FACTOR;
            showFadeIcon(getResources().getDrawable(R.drawable.ff_icon));
        }
    }

    private final double SPEED_FACTOR = 1.5;
    private final int VELOCITY_THRESHOLD = 4000;
    private final int SWIPE_TOP = 1;
    private final int SWIPE_LEFT = 2;
    private final int SWIPE_DOWN = 3;
    private final int SWIPE_RIGHT = 4;

    private class GestureTap extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if(DEBUG)
                Log.d(TAG, "Single tap up detected!");

            mImmersive = !mImmersive;
            setImmersiveMode();

            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            if(DEBUG)
                Log.d(TAG, "Swipe onFling: " + velocityX + ", " + velocityY);

            if(Math.abs(velocityY) > VELOCITY_THRESHOLD){
                switch (getSlope(e1.getX(), e1.getY(), e2.getX(), e2.getY())) {
                    case SWIPE_TOP:
                        increaseSpeed();
                        return true;
                    case SWIPE_LEFT:
                        return true;
                    case SWIPE_DOWN:
                        decreaseSpeed();
                        return true;
                    case SWIPE_RIGHT:
                        return true;
                }
            }

            return false;
        }

        private int getSlope(float x1, float y1, float x2, float y2) {
            Double angle = Math.toDegrees(Math.atan2(y1 - y2, x2 - x1));
            if (angle > 45 && angle <= 135)
                return SWIPE_TOP;
            if (angle >= 135 && angle < 180 || angle < -135 && angle > -180)
                return SWIPE_LEFT;
            if (angle < -45 && angle>= -135)
                return SWIPE_DOWN;
            if (angle > -45 && angle <= 45)
                return SWIPE_RIGHT;
            return 0;
        }

    }

    // Sensor Feature
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long lastUpdate = 0;
    private long lastGesture = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;

                if (speed > SHAKE_THRESHOLD && (curTime - lastGesture) > 1000) {
                    if (DEBUG)
                        Log.d(TAG, "SHAKE DETECTED!");

                    mViewPager.setCurrentItem(mViewPager.getCurrentItem()+1, mTransitionsEnabled);
                    lastGesture = curTime;
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Used for remotes such as Nexus Player remote controller
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                mImmersive = !mImmersive;
                setImmersiveMode();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                increaseSpeed();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                decreaseSpeed();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private static class NoPageTransformer implements ViewPager.PageTransformer {
        public void transformPage(View view, float position) {
            if (position < 0) {
                view.setScrollX((int)((float)(view.getWidth()) * position));
            } else if (position > 0) {
                view.setScrollX(-(int) ((float) (view.getWidth()) * -position));
            } else {
                view.setScrollX(0);
            }
        }
    }
}
