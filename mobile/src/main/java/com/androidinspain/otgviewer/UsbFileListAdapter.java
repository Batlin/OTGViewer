/*
 * (C) Copyright 2014 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.androidinspain.otgviewer;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidinspain.otgviewer.util.IconUtils;
import com.androidinspain.otgviewer.util.Utils;
import com.github.mjdev.libaums.fs.UsbFile;

/**
 * List adapter to represent the contents of an {@link UsbFile} directory.
 * 
 * @author mjahnen
 * 
 */
public class UsbFileListAdapter extends ArrayAdapter<UsbFile> {

	private String TAG = getClass().getSimpleName();
	private int lastPosition = -1;


	private List<UsbFile> files;
	private UsbFile currentDir;

	private LayoutInflater inflater;

	/**
	 * Constructs a new List Adapter to show {@link UsbFile}s.
	 * 
	 * @param context
	 *            The context.
	 * @param dir
	 *            The directory which shall be shown.
	 * @throws IOException
	 *             If reading fails.
	 */
	public UsbFileListAdapter(Context context, UsbFile dir) throws IOException {
		super(context, R.layout.list_item);
		currentDir = dir;
		files = new ArrayList<UsbFile>();

		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		lastPosition = -1;

		refresh();
	}

	/**
	 * Reads the contents of the directory and notifies that the View shall be
	 * updated.
	 * 
	 * @throws IOException
	 *             If reading contents of a directory fails.
	 */
	public void refresh() throws IOException {
		files = Arrays.asList(currentDir.listFiles());
		Log.d(TAG, "files size: " + files.size());
		Collections.sort(files, Utils.comparator);
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return files.size();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView != null) {
			view = convertView;
		} else {
			view = inflater.inflate(R.layout.list_item, parent, false);
		}

		TextView title = (TextView) view.findViewById(android.R.id.title);
		TextView summary = (TextView) view.findViewById(android.R.id.summary);
		ImageView type = (ImageView) view.findViewById(android.R.id.icon);
		UsbFile file = files.get(position);

		if (file.isDirectory()) {
			type.setImageResource(R.drawable.ic_folder_alpha);
		} else {
			int index = file.getName().lastIndexOf(".");
			if(index>0) {
				String prefix = file.getName().substring(0, index);
				String ext = file.getName().substring(index + 1);
				Log.d(TAG, "mimetype: " + Utils.getMimetype(ext.toLowerCase()) + ". ext is: " + ext);
				type.setImageResource(IconUtils.loadMimeIcon(Utils.getMimetype(ext.toLowerCase())));
			}

		}

		title.setText(file.getName());
		DateFormat date_format = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, Locale.getDefault());
		String date = date_format.format(new Date(file.lastModified()));

		summary.setText("Last modified: " + date);

		//Animation animation = AnimationUtils.loadAnimation(getContext(), (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
		if(position > lastPosition) {
			Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
			view.startAnimation(animation);
		}
		lastPosition = Math.max(position,lastPosition);

		return view;
	}

	@Override
	public UsbFile getItem(int position) {
		return files.get(position);
	}

	/**
	 * 
	 * @return the directory which is currently be shown.
	 */
	public UsbFile getCurrentDir() {
		return currentDir;
	}

	public List<UsbFile> getFiles() {
		return files;
	}

}