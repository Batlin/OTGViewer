package com.androidinspain.otgviewer.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.usb.UsbDevice;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.DocumentsContract;
import android.app.ListFragment;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidinspain.otgviewer.ImageViewer;
import com.androidinspain.otgviewer.ImageViewerActivity;
import com.androidinspain.otgviewer.R;
import com.androidinspain.otgviewer.UsbFileListAdapter;
import com.androidinspain.otgviewer.task.CopyTaskParam;
import com.androidinspain.otgviewer.util.Utils;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by roberto on 30/08/15.
 */
public class ExplorerFragment extends ListFragment implements AdapterView.OnItemLongClickListener{

    private String TAG = getClass().getSimpleName();
    private boolean DEBUG = false;

    private ExplorerCallback mMainActivity;

    private UsbMassStorageDevice mSelectedDevice;
    /* package */ UsbFileListAdapter adapter;
    private Deque<UsbFile> dirs = new ArrayDeque<UsbFile>();

    private TextView mEmptyView;
    private boolean mIsShowcase = false;
    private boolean mError = false;

    // Sorting related
    private TextView mFilterByTV;
    private ImageView mOrderByIV;
    public static int mSortByCurrent = 0;
    public static boolean mSortAsc = true;

    public ExplorerFragment() {
        // Required empty public constructor
    }

    public interface ExplorerCallback {
        public void setABTitle(String title, boolean showMenu);
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

    private void checkShowcase(){

        if(mError)
            return;

        UsbFile directory = adapter.getCurrentDir();
        if(DEBUG)
            Log.d(TAG, "Checking showcase. Current directory: " + directory.isDirectory());
        boolean available = false;
        List<UsbFile> files = new ArrayList<UsbFile>();

        try{
            files = Arrays.asList(directory.listFiles());
            Collections.sort(files, Utils.comparator);

            if(!mSortAsc)
                Collections.reverse(files);

            int firstImageIndex = 0;
            for (UsbFile file : files) {
                if(Utils.isImage(file)){
                    available = true;
                    break;
                }
                firstImageIndex++;
            }

            if (available && !files.isEmpty()){
                mIsShowcase = true;
                copyFileToCache(files.get(firstImageIndex));
            }else{
                Toast.makeText(getActivity(), getString(R.string.toast_no_images), Toast.LENGTH_LONG).show();
            }

        }catch(Exception e){
            e.printStackTrace();
        }


    }

    private void openFilterDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("Sort by");
        alertDialogBuilder.setItems(R.array.sortby, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSortByCurrent = which;
                mFilterByTV.setText(Utils.getHumanSortBy(getActivity()));
                doSort();
                dialog.dismiss();
            }
        });

        AlertDialog ad = alertDialogBuilder.create();
        ad.show();
    }

    private void doSort() {
        try {
            if (adapter!=null)
                adapter.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void orderByTrigger() {
        mSortAsc = !mSortAsc;

        if (mSortAsc)
            mOrderByIV.setImageResource(R.drawable.sort_order_asc);
        else
            mOrderByIV.setImageResource(R.drawable.sort_order_desc);

        doSort();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_explorer, container, false);

        mFilterByTV = (TextView) rootView.findViewById(R.id.filterby);
        mOrderByIV = (ImageView) rootView.findViewById(R.id.orderby);
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

        if(devices.length>0)
            mSelectedDevice = devices[0];

        updateUI();

        try {
            mSelectedDevice.init();

            // we always use the first partition of the device
            FileSystem fs = mSelectedDevice.getPartitions().get(0).getFileSystem();
            UsbFile root = fs.getRootDirectory();

            //Log.d(TAG, "root: " + root.getName());
            setListAdapter(adapter = new UsbFileListAdapter(getActivity(), root));
            Log.d(TAG, "root getCurrentDir: " + adapter.getCurrentDir());

        } catch (Exception e) {
            Log.e(TAG, "error setting up device", e);
            rootView.findViewById(R.id.error).setVisibility(View.VISIBLE);
            mError = true;
        }

        if(mError) {
            mEmptyView = (TextView) rootView.findViewById(R.id.error);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView = (TextView) rootView.findViewById(R.id.empty);
        }

        // Inflate the layout for this fragment
        return rootView;
    }



    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        if(DEBUG)
            Log.d(TAG, "onActivityCreated");

        try{
            getListView().setOnItemLongClickListener(this);
            if(mError){
                getListView().setVisibility(View.GONE);
                getListView().setAdapter(null);
                getListView().setEmptyView(mEmptyView);
            }
        }catch(Exception e){
            Log.e(TAG, "Content view is not yet created!", e);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if(DEBUG)
            Log.d(TAG, "Long click on position: " + position);

        UsbFile entry = adapter.getItem(position);

        if (Utils.isImage(entry)) {
            showLongClickDialog(entry);
        }

        return true;
    }

    private void showLongClickDialog(final UsbFile entry)    {
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
    public void onListItemClick(ListView list, View view, int position, long id) {
        UsbFile entry = adapter.getItem(position);
        try {
            if (entry.isDirectory()) {
                dirs.push(adapter.getCurrentDir());
                setListAdapter(adapter = new UsbFileListAdapter(getActivity(), entry));

            } else {
                mIsShowcase = false;
                copyFileToCache(entry);

            }
        } catch (IOException e) {
            Log.e(TAG, "error starting to copy!", e);
        }
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if(DEBUG)
            Log.d(TAG, "onAttach");

        try {
            mMainActivity = (ExplorerCallback) activity;

        } catch (ClassCastException castException) {
            /** The activity does not implement the listener. */
        }

    }

    private void updateUI(){
        mMainActivity.setABTitle(getString(R.string.explorer_title), true);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mSelectedDevice = null;

        if(DEBUG)
            Log.d(TAG, "onDetach");
    }


    public boolean popUsbFile(){
        try {
            UsbFile dir = dirs.pop();
            setListAdapter(adapter = new UsbFileListAdapter(getActivity(), dir));

            return true;
        } catch (NoSuchElementException e) {
            Log.e(TAG, "we should remove this fragment!");
        } catch (IOException e) {
            Log.e(TAG, "error initializing adapter!", e);
        }

        return false;
    }

    private void copyFileToCache(UsbFile entry) throws IOException {
        CopyTaskParam param = new CopyTaskParam();
        param.from = entry;
        Utils.cachePath.mkdirs();
        int index = entry.getName().lastIndexOf(".");
        String prefix = entry.getName().substring(0, index);
        String ext = entry.getName().substring(index);

        // prefix must be at least 3 characters
        if(DEBUG)
            Log.d(TAG, "ext: " + ext);

        if (prefix.length() < 3) {
            prefix += "pad";
        }

        String fileName = prefix + ext;
        File cacheFile = new File(Utils.cachePath, fileName);
        param.to = cacheFile;

        ImageViewer.getInstance().setCurrentFile(entry);

        if (!cacheFile.exists())
            new CopyTask(this, Utils.isImage(entry)).execute(param);
        else
            launchIntent(cacheFile);

    }

    private void launchIntent(UsbFile entry){
        String fileName = entry.getName();
        launchIntent(new File(Utils.cachePath, fileName));
    }

    private void launchIntent(File f) {

        if(Utils.isImage(f)){
            ImageViewer.getInstance().setAdapter(adapter);
            if(adapter.getCurrentDir()==null){
                FileSystem fs = mSelectedDevice.getPartitions().get(0).getFileSystem();
                UsbFile root = fs.getRootDirectory();
                ImageViewer.getInstance().setCurrentDirectory(root);
            }else{
                ImageViewer.getInstance().setCurrentDirectory(adapter.getCurrentDir());
            }

            Intent intent = new Intent(getActivity(), ImageViewerActivity.class);
            intent.putExtra("SHOWCASE", mIsShowcase);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
            return;
        }

        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        File file = new File(f.getAbsolutePath());

        String mimetype = Utils.getMimetype(file);
        intent.setDataAndType(Uri.fromFile(file), mimetype);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), getString(R.string.toast_no_app_match), Toast.LENGTH_LONG).show();
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
            if(isImage)
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
        protected void onCancelled(Void result){
            // Remove uncompleted data file
            if(DEBUG)
                Log.d(TAG, "Removing uncomplete file transfer");
            if(param!=null)
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
                    if(!isCancelled()) {
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
            if(DEBUG)
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
}
