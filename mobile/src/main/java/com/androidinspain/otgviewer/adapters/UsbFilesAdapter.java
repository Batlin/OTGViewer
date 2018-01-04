package com.androidinspain.otgviewer.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidinspain.otgviewer.R;
import com.androidinspain.otgviewer.fragments.ExplorerFragment;
import com.androidinspain.otgviewer.recyclerview.RecyclerItemClickListener;
import com.androidinspain.otgviewer.util.IconUtils;
import com.androidinspain.otgviewer.util.Utils;
import com.github.mjdev.libaums.fs.UsbFile;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by roberto on 13/08/17.
 */

public class UsbFilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private boolean DEBUG = false;
    private String TAG = getClass().getSimpleName();

    private List<UsbFile> mFiles;
    private UsbFile mCurrentDir;

    private RecyclerItemClickListener mRecyclerItemClickListener;
    private Context mContext;

    private LayoutInflater mInflater;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        View globalView;
        TextView title, summary;
        ImageView type;

        public MyViewHolder(View view) {
            super(view);

            globalView = view;
            title = (TextView) view.findViewById(android.R.id.title);
            summary = (TextView) view.findViewById(android.R.id.summary);
            type = (ImageView) view.findViewById(android.R.id.icon);

            view.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                    boolean handled = false;
                    Log.d(TAG, "onKey " + keyEvent.toString());
                    handled = mRecyclerItemClickListener.handleDPad(view, keyCode, keyEvent);

                    return handled;
                }
            });
        }
    }

    public UsbFilesAdapter(Context context, UsbFile dir, RecyclerItemClickListener itemClickListener) throws IOException {
        mContext = context;
        mCurrentDir = dir;
        mRecyclerItemClickListener = itemClickListener;
        mFiles = new ArrayList<UsbFile>();

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        refresh();
    }

    public void refresh() throws IOException {
        mFiles = Arrays.asList(mCurrentDir.listFiles());
        if (DEBUG) {
            Log.d(TAG, "files size: " + mFiles.size());
            Log.d(TAG, "REFRESH.  mSortByCurrent: " + ExplorerFragment.mSortByCurrent + ", mSortAsc: " + ExplorerFragment.mSortAsc);
        }

        Collections.sort(mFiles, Utils.comparator);

        if (!ExplorerFragment.mSortAsc)
            Collections.reverse(mFiles);

        notifyDataSetChanged();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

        //if(DEBUG)
        //    Log.d(TAG, "onBindViewHolder. Position: " + position);

        if (viewHolder instanceof MyViewHolder) {
            UsbFile file = mFiles.get(position);

            MyViewHolder holder = (MyViewHolder) viewHolder;

            if (file.isDirectory()) {
                holder.type.setImageResource(R.drawable.ic_folder_alpha);
            } else {
                int index = file.getName().lastIndexOf(".");
                if (index > 0) {
                    String prefix = file.getName().substring(0, index);
                    String ext = file.getName().substring(index + 1);
                    //if (DEBUG)
                    //    Log.d(TAG, "mimetype: " + Utils.getMimetype(ext.toLowerCase()) + ". ext is: " + ext);
                    holder.type.setImageResource(IconUtils.loadMimeIcon(Utils.getMimetype(ext.toLowerCase())));
                }

            }

            holder.title.setText(file.getName());
            DateFormat date_format = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, Locale.getDefault());
            String date = date_format.format(new Date(file.lastModified()));

            // If it's a directory, we can't get size info
            try {
                holder.summary.setText("Last modified: " + date + " - " +
                        Formatter.formatFileSize(mContext, file.getLength()));
            } catch (Exception e) {
                holder.summary.setText("Last modified: " + date);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public UsbFile getItem(int position) {
        return mFiles.get(position);
    }

    public UsbFile getCurrentDir() {
        return mCurrentDir;
    }

    public void setCurrentDir(UsbFile dir) {
        mCurrentDir = dir;
    }

    public List<UsbFile> getFiles() {
        return mFiles;
    }

    public ArrayList<UsbFile> getImageFiles() {
        ArrayList<UsbFile> result = new ArrayList<>();

        for (UsbFile file : mFiles) {
            if (Utils.isImage(file))
                result.add(file);
        }

        return result;
    }

}