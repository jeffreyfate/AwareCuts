package com.jeffthefate.awarecuts_dev.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jeffthefate.awarecuts_dev.common.Constants;
import com.jeffthefate.awarecuts_dev.common.Util;
/**
 * Receives when device is rotated and updates the widget views.
 * 
 * @author Jeff Fate
 */
public class RotateReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.LOG_TAG, "Receiving configuration changed broadcast");
        Util.updateViews(null, false, null, false);
    }
 
}