package com.androidinspain.otgviewer.util;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;

import com.androidinspain.otgviewer.R;
import com.androidinspain.otgviewer.fragments.ExplorerFragment;
import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;
import java.util.Comparator;

/**
 * Created by roberto on 23/08/15.
 */
public class Utils {

    public final static File otgViewerPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/OTGViewer");
    public final static File otgViewerCachePath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/OTGViewer/cache");
    private static String TAG = "Utils";
    private static boolean DEBUG = true;

    public static int calculateInSampleSize(File f, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), options);
        // String imageType = options.outMimeType;

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;

        if (DEBUG) {
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

    public static long getDirSize(File dir) {
        long size = 0;
        for (File file : dir.listFiles()) {
            if (file != null && file.isDirectory()) {
                size += getDirSize(file);
            } else if (file != null && file.isFile()) {
                size += file.length();
            }
        }
        return size;
    }

    // We remove the app's cache folder if threshold is exceeded
    public static void deleteCache(File cachePath) {
        long cacheSize = getDirSize(cachePath);

        if (DEBUG)
            Log.d(TAG, "cacheSize: " + cacheSize);

        if (getDirSize(cachePath) > Constants.CACHE_THRESHOLD) {
            if (DEBUG)
                Log.d(TAG, "Erasing cache folder");

            deleteDir(cachePath);
        }

        deleteDir(otgViewerCachePath);
    }

    public static String getMimetype(File f) {
        if (DEBUG)
            Log.d(TAG, "extension from: " + Uri.fromFile(f).toString().toLowerCase());

        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri
                .fromFile(f).toString().toLowerCase());

        return getMimetype(extension);
    }

    public static String getMimetype(String extension) {
        String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                extension);

        if (DEBUG)
            Log.d(TAG, "mimetype is: " + mimetype);

        return mimetype;
    }

    public static Comparator<UsbFile> comparator = new Comparator<UsbFile>() {

        @Override
        public int compare(UsbFile lhs, UsbFile rhs) {

            if (DEBUG)
                Log.d(TAG, "comparator. Sorting by: " + ExplorerFragment.mSortByCurrent);

            switch (ExplorerFragment.mSortByCurrent) {
                case Constants.SORTBY_NAME:
                    return sortByName(lhs, rhs);
                case Constants.SORTBY_DATE:
                    return sortByDate(lhs, rhs);
                case Constants.SORTBY_SIZE:
                    return sortBySize(lhs, rhs);
                default:
                    break;
            }

            return 0;
        }

        int extractInt(String s) {
            int result = 0;
            // return 0 if no digits found
            try {
                String num = s.replaceAll("\\D", "");
                result = num.isEmpty() ? 0 : Integer.parseInt(num);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                return result;
            }
        }

        int checkIfDirectory(UsbFile lhs, UsbFile rhs) {
            if (lhs.isDirectory() && !rhs.isDirectory()) {
                return -1;
            }

            if (rhs.isDirectory() && !lhs.isDirectory()) {
                return 1;
            }

            return 0;
        }

        int sortByName(UsbFile lhs, UsbFile rhs) {
            int result = 0;
            int dir = checkIfDirectory(lhs, rhs);
            if (dir != 0)
                return dir;

            // Check if there is any number
            String lhsNum = lhs.getName().replaceAll("\\D", "");
            String rhsNum = rhs.getName().replaceAll("\\D", "");
            int lhsRes = 0;
            int rhsRes = 0;

            if (!lhsNum.isEmpty() && !rhsNum.isEmpty()) {
                lhsRes = extractInt(lhs.getName());
                rhsRes = extractInt(rhs.getName());
                return lhsRes - rhsRes;
            }

            result = lhs.getName().compareToIgnoreCase(rhs.getName());

            return result;
        }

        int sortByDate(UsbFile lhs, UsbFile rhs) {
            long result = 0;
            int dir = checkIfDirectory(lhs, rhs);
            if (dir != 0)
                return dir;

            result = lhs.lastModified() - rhs.lastModified();

            return (int) result;
        }

        int sortBySize(UsbFile lhs, UsbFile rhs) {
            long result = 0;
            int dir = checkIfDirectory(lhs, rhs);
            if (dir != 0)
                return dir;

            try {
                result = lhs.getLength() - rhs.getLength();
            } catch (Exception e) {
            }

            return (int) result;
        }
    };

    public static String getHumanSortBy(Context context) {
        switch (ExplorerFragment.mSortByCurrent) {
            case Constants.SORTBY_NAME:
                return context.getString(R.string.name);
            case Constants.SORTBY_DATE:
                return context.getString(R.string.date);
            case Constants.SORTBY_SIZE:
                return context.getString(R.string.size);
            default:
                return context.getString(R.string.name);
        }
    }

    public static boolean isImage(UsbFile entry) {
        if (entry.isDirectory())
            return false;

        try {
            return isImageInner(entry.getName());
        } catch (StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean isImage(File entry) {
        return isImageInner(entry.getName());
    }

    private static boolean isImageInner(String name) {
        boolean result = false;

        int index = name.lastIndexOf(".");

        if (index > 0) {
            String ext = name.substring(index);

            if (ext.equalsIgnoreCase(".jpg") || ext.equalsIgnoreCase(".png")
                    || ext.equalsIgnoreCase(".jpeg")) {
                result = true;
            }

            Log.d(TAG, "isImageInner " + name + ": " + result);
        }

        return result;
    }

    public static boolean isConfirmButton(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
                return true;
            default:
                return false;
        }
    }

    public static boolean isMassStorageDevice(UsbDevice device) {
        boolean result = false;

        int interfaceCount = device.getInterfaceCount();
        for (int i = 0; i < interfaceCount; i++) {
            UsbInterface usbInterface = device.getInterface(i);
            Log.i(TAG, "found usb interface: " + usbInterface);

            // we currently only support SCSI transparent command set with
            // bulk transfers only!
            if (usbInterface.getInterfaceClass() != UsbConstants.USB_CLASS_MASS_STORAGE
                    || usbInterface.getInterfaceSubclass() != Constants.INTERFACE_SUBCLASS
                    || usbInterface.getInterfaceProtocol() != Constants.INTERFACE_PROTOCOL) {
                Log.i(TAG, "device interface not suitable!");
                continue;
            }

            // Every mass storage device has exactly two endpoints
            // One IN and one OUT endpoint
            int endpointCount = usbInterface.getEndpointCount();
            if (endpointCount != 2) {
                Log.w(TAG, "inteface endpoint count != 2");
            }

            UsbEndpoint outEndpoint = null;
            UsbEndpoint inEndpoint = null;
            for (int j = 0; j < endpointCount; j++) {
                UsbEndpoint endpoint = usbInterface.getEndpoint(j);
                Log.i(TAG, "found usb endpoint: " + endpoint);
                if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                        outEndpoint = endpoint;
                    } else {
                        inEndpoint = endpoint;
                    }
                }
            }

            if (outEndpoint == null || inEndpoint == null) {
                Log.e(TAG, "Not all needed endpoints found!");
                continue;
            }

            result = true;
        }

        return result;
    }
}
