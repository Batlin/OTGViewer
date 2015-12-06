package com.androidinspain.otgviewer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class TVMainActivity extends Activity {

    private String TAG = TVMainActivity.class.getName();
    private boolean DEBUG = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(DEBUG)
            Log.d(TAG, "onCreate TVMainActivity");

        Intent intent = new Intent(this, MainActivity.class); // Your list's Intent
        startActivity(intent);

    }

}
