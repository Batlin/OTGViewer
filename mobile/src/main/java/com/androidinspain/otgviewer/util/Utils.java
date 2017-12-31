package com.androidinspain.otgviewer.util;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;
import java.util.Comparator;

/**
 * Created by roberto on 23/08/15.
 */
public class Utils {

    private static String TAG = "Utils";
    private static boolean DEBUG = false;
    public final static File cachePath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/OTGViewer/cache");

    public static int calculateInSampleSize(File f, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), options);
        // String imageType = options.outMimeType;

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;

        if(DEBUG) {
            Log.d(TAG, "checkImageSize. X: " + width + ", Y: " + height);
            Log.d(TAG, "Screen is. X: " + reqWidth + ", Y: " + reqHeight);
        }

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }


    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty or this is a file so delete it
        return dir.delete();
    }

    public static void deleteCache(){
        deleteDir(cachePath);
    }

    public static String getMimetype(File f) {
        if(DEBUG)
            Log.d(TAG, "extension from: " + Uri.fromFile(f).toString().toLowerCase());

        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri
                .fromFile(f).toString().toLowerCase());

        return getMimetype(extension);
    }

    public static String getMimetype(String extension) {
        String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                extension);

        if(DEBUG)
            Log.d(TAG, "mimetype is: " + mimetype);

        return mimetype;
    }

    public static Comparator<UsbFile> comparator = new Comparator<UsbFile>() {

        @Override
        public int compare(UsbFile lhs, UsbFile rhs) {

            if (lhs.isDirectory() && !rhs.isDirectory()) {
                return -1;
            }

            if (rhs.isDirectory() && !lhs.isDirectory()) {
                return 1;
            }

            // Check if there is any number
            String lhsNum = lhs.getName().replaceAll("\\D", "");
            String rhsNum = rhs.getName().replaceAll("\\D", "");
            int lhsRes=0;
            int rhsRes=0;

            if(!lhsNum.isEmpty() && !rhsNum.isEmpty()) {
                lhsRes = extractInt(lhs.getName());
                rhsRes = extractInt(rhs.getName());
                return lhsRes - rhsRes;
            }

            int result = lhs.getName().compareToIgnoreCase(rhs.getName());

            return result;
        }

        int extractInt(String s) {
            String num = s.replaceAll("\\D", "");
            // return 0 if no digits found
            return num.isEmpty() ? 0 : Integer.parseInt(num);
        }
    };

    public static boolean isImage(UsbFile entry){
        int index = entry.getName().lastIndexOf(".");

        if(entry.isDirectory())
            return false;

        try{
            String prefix = entry.getName().substring(0, index);
            String ext = entry.getName().substring(index);
            if (ext.equalsIgnoreCase(".jpg")) {
                return true;
            }

        }catch (StringIndexOutOfBoundsException e){
            e.printStackTrace();
        }


        return false;
    }

    public static boolean isImage(File entry){
        int index = entry.getName().lastIndexOf(".");
        String prefix = entry.getName().substring(0, index);
        String ext = entry.getName().substring(index);

        if (ext.equalsIgnoreCase(".jpg")) {
            return true;
        }

        return false;
    }
}
