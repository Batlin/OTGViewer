package com.androidinspain.otgviewer.util;

/**
 * Created by roberto on 31/12/17.
 */

public class Constants {
    /**
     * subclass 6 means that the usb mass storage device implements the SCSI
     * transparent command set
     */
    public static final int INTERFACE_SUBCLASS = 6;

    /**
     * protocol 80 means the communication happens only via bulk transfers
     */
    public static final int INTERFACE_PROTOCOL = 80;

    public final static String SORT_FILTER_PREF = "SORT_FILTER_PREF";
    public final static String SORT_ASC_KEY = "SORT_ASC_KEY";
    public final static String SORT_FILTER_KEY = "SORT_FILTER_KEY";

    public final static int SORTBY_NAME = 0;
    public final static int SORTBY_DATE = 1;
    public final static int SORTBY_SIZE = 2;

    public final static int CACHE_THRESHOLD = 20 * 1024 * 1024; // 20 MB
}
