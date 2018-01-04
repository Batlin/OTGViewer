package com.androidinspain.otgviewer.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidinspain.otgviewer.ImageViewer;
import com.androidinspain.otgviewer.ImageViewerActivity;
import com.androidinspain.otgviewer.R;
import com.androidinspain.otgviewer.adapters.UsbFilesAdapter;
import com.androidinspain.otgviewer.recyclerview.EmptyRecyclerView;
import com.androidinspain.otgviewer.recyclerview.RecyclerItemClickListener;
import com.androidinspain.otgviewer.task.CopyTaskParam;
import com.androidinspain.otgviewer.util.Constants;
import com.androidinspain.otgviewer.util.Utils;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by roberto on 30/08/15.
 */
public class ExplorerFragment extends Fragment {

    private String TAG = getClass().getSimpleName();
    private boolean DEBUG = false;

    private ExplorerCallback mMainActivity;

    private UsbMassStorageDevice mSelectedDevice;
    /* package */ UsbFilesAdapter mAdapter;
    private Deque<UsbFile> dirs = new ArrayDeque<UsbFile>();

    private LinearLayout mEmptyView;
    private TextView mErrorView;
    private boolean mIsShowcase = false;
    private boolean mError = false;

    private EmptyRecyclerView mRecyclerView;
    private RecyclerItemClickListener mRecyclerItemClickListener;

    // Sorting related
    private LinearLayout mSortByLL;
    private Button mFilterByTV;
    private ImageButton mOrderByIV;
    public static int mSortByCurrent;
    public static boolean mSortAsc;

    private int REQUEST_IMAGEVIEWER = 0;

    private final int REQUEST_FOCUS = 0;
    private final int REQUEST_FOCUS_DELAY = 200; //ms

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REQUEST_FOCUS:
                    if (mRecyclerView != null)
                        mRecyclerView.requestFocus();
            }
        }
    };

    public ExplorerFragment() {
        // Required empty public constructor
    }

    public interface ExplorerCallback {
        public void setABTitle(String title, boolean showMenu);

        public CoordinatorLayout getCoordinatorLayout();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_showcase).setVisible(true);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do something that differs the Activity's menu here
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // Not implemented here
                return false;
            case R.id.action_showcase:
                // This is only handled in this fragment
                checkShowcase();
                return true;
            default:
                break;
        }

        return false;
    }

    private void checkShowcase() {

        if (mError)
            return;

        UsbFile directory = mAdapter.getCurrentDir();
        if (DEBUG)
            Log.d(TAG, "Checking showcase. Current directory: " + directory.isDirectory());
        boolean available = false;
        List<UsbFile> files;

        try {
            files = mAdapter.getFiles();

            int firstImageIndex = 0;
            for (UsbFile file : files) {
                if (Utils.isImage(file)) {
                    available = true;
                    break;
                }
                firstImageIndex++;
            }

            if (available && !files.isEmpty()) {
                mIsShowcase = true;
                copyFileToCache(files.get(firstImageIndex));
            } else {
                Snackbar.make(mMainActivity.getCoordinatorLayout(),
                        getString(R.string.toast_no_images), Snackbar.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void openFilterDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(getString(R.string.sort_by));
        alertDialogBuilder.setItems(R.array.sortby, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSortByCurrent = which;
                updateSortUI(true);
                dialog.dismiss();
            }
        });

        AlertDialog ad = alertDialogBuilder.create();
        ad.show();
    }

    private void doSort() {
        saveSortFilter();
        doRefresh();
    }

    private void doRefresh(UsbFile entry) {
        mAdapter.setCurrentDir(entry);
        doRefresh();
    }


    private void doRefresh() {
        try {
            if (mAdapter != null)
                mAdapter.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mRecyclerView.scrollToPosition(0);
        mHandler.sendEmptyMessageDelayed(REQUEST_FOCUS, REQUEST_FOCUS_DELAY);
    }

    private void saveSortFilter() {
        SharedPreferences sharedPref = getActivity().getSharedPreferences(
                Constants.SORT_FILTER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(Constants.SORT_FILTER_KEY, mSortByCurrent);
        editor.putBoolean(Constants.SORT_ASC_KEY, mSortAsc);
        editor.commit();
    }

    private void orderByTrigger() {
        mSortAsc = !mSortAsc;
        updateSortUI(true);
    }

    private void updateSortUI(boolean doSort) {
        mSortByLL.setVisibility(View.VISIBLE);
        mFilterByTV.setText(Utils.getHumanSortBy(getActivity()));

        if (mSortAsc)
            mOrderByIV.setImageResource(R.drawable.sort_order_asc);
        else
            mOrderByIV.setImageResource(R.drawable.sort_order_desc);

        if (doSort)
            doSort();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_explorer, container, false);

        SharedPreferences sharedPref = getActivity().getSharedPreferences(Constants.SORT_FILTER_PREF
                , Context.MODE_PRIVATE);
        mSortAsc = sharedPref.getBoolean(Constants.SORT_ASC_KEY, true);
        mSortByCurrent = sharedPref.getInt(Constants.SORT_FILTER_KEY, 0);

        mRecyclerView = (EmptyRecyclerView) rootView.findViewById(R.id.list_rv);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mSortByLL = (LinearLayout) rootView.findViewById(R.id.sortby_layout);
        mFilterByTV = (Button) rootView.findViewById(R.id.filterby);
        mOrderByIV = (ImageButton) rootView.findViewById(R.id.orderby);

        mFilterByTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilterDialog();
            }
        });
        mOrderByIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orderByTrigger();
            }
        });
        mSelectedDevice = null;
        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(getActivity());

        mError = false;

        if (devices.length > 0)
            mSelectedDevice = devices[0];

        updateUI();

        try {
            mSelectedDevice.init();

            // we always use the first partition of the device
            FileSystem fs = mSelectedDevice.getPartitions().get(0).getFileSystem();
            UsbFile root = fs.getRootDirectory();

            setupRecyclerView();
            mAdapter = new UsbFilesAdapter(getActivity(), root,
                    mRecyclerItemClickListener);
            mRecyclerView.setAdapter(mAdapter);

            updateSortUI(false);

            if (DEBUG)
                Log.d(TAG, "root getCurrentDir: " + mAdapter.getCurrentDir());

        } catch (Exception e) {
            Log.e(TAG, "error setting up device", e);
            rootView.findViewById(R.id.error).setVisibility(View.VISIBLE);
            mError = true;
        }

        if (mError) {
            mErrorView = (TextView) rootView.findViewById(R.id.error);
            mErrorView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView = (LinearLayout) rootView.findViewById(R.id.empty);
            mRecyclerView.setEmptyView(mEmptyView, mSortByLL);
        }

        // Inflate the layout for this fragment
        return rootView;
    }

    private void setupRecyclerView() {
        mRecyclerItemClickListener = new RecyclerItemClickListener(
                getActivity(), mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                onListItemClick(position);
            }

            @Override
            public void onLongItemClick(View view, int position) {
                onItemLongClick(position);
            }
        });

        mRecyclerView.addOnItemTouchListener(mRecyclerItemClickListener);
    }

    private void onListItemClick(int position) {
        UsbFile entry = mAdapter.getItem(position);
        try {
            if (entry.isDirectory()) {
                dirs.push(mAdapter.getCurrentDir());
                doRefresh(entry);
            } else {
                mIsShowcase = false;
                copyFileToCache(entry);
            }
        } catch (IOException e) {
            Log.e(TAG, "error starting to copy!", e);
        }
    }

    private boolean onItemLongClick(int position) {
        if (DEBUG)
            Log.d(TAG, "Long click on position: " + position);

        UsbFile entry = mAdapter.getItem(position);

        if (Utils.isImage(entry)) {
            showLongClickDialog(entry);
        }

        return true;
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        if (DEBUG)
            Log.d(TAG, "onActivityCreated");

        try {
            if (mError) {
                mRecyclerView.setVisibility(View.GONE);
                mRecyclerView.setAdapter(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Content view is not yet created!", e);
        }
    }

    private void showLongClickDialog(final UsbFile entry) {
        // We already checked it's an image

        final AlertDialog.Builder dialogAlert = new AlertDialog.Builder(getActivity());
        dialogAlert.setTitle(R.string.showcase_longclick_dialog_title);
        dialogAlert.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        dialogAlert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    mIsShowcase = true;
                    copyFileToCache(entry);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        dialogAlert.show();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (DEBUG)
            Log.d(TAG, "onAttach");

        try {
            mMainActivity = (ExplorerCallback) activity;

        } catch (ClassCastException castException) {
            /** The activity does not implement the listener. */
        }

    }

    private void updateUI() {
        mMainActivity.setABTitle(getString(R.string.explorer_title), true);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mSelectedDevice = null;

        if (DEBUG)
            Log.d(TAG, "onDetach");
    }


    public boolean popUsbFile() {
        try {
            UsbFile dir = dirs.pop();
            doRefresh(dir);

            return true;
        } catch (NoSuchElementException e) {
            Log.e(TAG, "we should remove this fragment!");
        } catch (Exception e) {
            Log.e(TAG, "error initializing mAdapter!", e);
        }

        return false;
    }

    private void copyFileToCache(UsbFile entry) throws IOException {
        CopyTaskParam param = new CopyTaskParam();
        param.from = entry;
        Utils.otgViewerCachePath.mkdirs();

        int index = entry.getName().lastIndexOf(".");

        String prefix;
        String ext = "";

        if (index < 0)
            prefix = entry.getName();
        else {
            prefix = entry.getName().substring(0, index);
            ext = entry.getName().substring(index);
        }

        // prefix must be at least 3 characters
        if (DEBUG)
            Log.d(TAG, "ext: " + ext);

        if (prefix.length() < 3) {
            prefix += "pad";
        }

        String fileName = prefix + ext;
        File downloadedFile = new File(Utils.otgViewerPath, fileName);
        File cacheFile = new File(Utils.otgViewerCachePath, fileName);
        param.to = cacheFile;

        ImageViewer.getInstance().setCurrentFile(entry);

        if (!cacheFile.exists() && !downloadedFile.exists())
            new CopyTask(this, Utils.isImage(entry)).execute(param);
        else
            launchIntent(cacheFile);

    }

    private void launchIntent(File f) {

        if (Utils.isImage(f)) {
            ImageViewer.getInstance().setAdapter(mAdapter);
            if (mAdapter.getCurrentDir() == null) {
                FileSystem fs = mSelectedDevice.getPartitions().get(0).getFileSystem();
                UsbFile root = fs.getRootDirectory();
                ImageViewer.getInstance().setCurrentDirectory(root);
            } else {
                ImageViewer.getInstance().setCurrentDirectory(mAdapter.getCurrentDir());
            }

            Intent intent = new Intent(getActivity(), ImageViewerActivity.class);
            intent.putExtra("SHOWCASE", mIsShowcase);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivityForResult(intent, REQUEST_IMAGEVIEWER);
            return;
        }

        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        File file = new File(f.getAbsolutePath());

        String mimetype = Utils.getMimetype(file);
        intent.setDataAndType(Uri.fromFile(file), mimetype);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(mMainActivity.getCoordinatorLayout(),
                    getString(R.string.toast_no_app_match), Snackbar.LENGTH_LONG).show();
        }
    }

    private class CopyTask extends AsyncTask<CopyTaskParam, Integer, Void> {

        private ProgressDialog dialog;
        private CopyTaskParam param;
        private ExplorerFragment parent;
        private CopyTask cp;

        public CopyTask(ExplorerFragment act, boolean isImage) {
            parent = act;
            cp = this;
            if (isImage)
                showImageDialog();
            else
                showDialog();
        }

        private void showImageDialog() {
            dialog = new ProgressDialog(parent.getActivity());
            dialog.setTitle(getString(R.string.dialog_image_title));
            dialog.setMessage(getString(R.string.dialog_image_message));
            dialog.setIndeterminate(true);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }

        private void showDialog() {
            dialog = new ProgressDialog(parent.getActivity());
            dialog.setTitle(getString(R.string.dialog_default_title));
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        }

        @Override
        protected void onCancelled(Void result) {
            // Remove uncompleted data file
            if (DEBUG)
                Log.d(TAG, "Removing uncomplete file transfer");
            if (param != null)
                param.to.delete();
        }

        @Override
        protected void onPreExecute() {
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if (DEBUG)
                        Log.d(TAG, "Dialog canceled");
                    cp.cancel(true);
                }
            });

            dialog.show();
        }

        @Override
        protected Void doInBackground(CopyTaskParam... params) {
            long time = System.currentTimeMillis();
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            param = params[0];
            long length = params[0].from.getLength();
            try {
                FileOutputStream out = new FileOutputStream(param.to);
                for (long i = 0; i < length; i += buffer.limit()) {
                    if (!isCancelled()) {
                        buffer.limit((int) Math.min(buffer.capacity(), length - i));
                        params[0].from.read(i, buffer);
                        out.write(buffer.array(), 0, buffer.limit());
                        publishProgress((int) i);
                        buffer.clear();
                    }
                }
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "error copying!", e);
            }
            if (DEBUG)
                Log.d(TAG, "copy time: " + (System.currentTimeMillis() - time));

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();

            parent.launchIntent(param.to);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            dialog.setMax((int) param.from.getLength());
            dialog.setProgress(values[0]);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGEVIEWER) {
            if (DEBUG)
                Log.d(TAG, "Scrolling to position: " + resultCode);
            mRecyclerView.scrollToPosition(resultCode);
        }
    }

}
