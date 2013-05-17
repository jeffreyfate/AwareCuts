package com.jeffthefate.awarecuts_dev.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jeffthefate.awarecuts_dev.common.Constants;
import com.jeffthefate.awarecuts_dev.widget.RefreshService;
/**
 * Receives when the contexts and widgets should be updated.
 * 
 * @author Jeff Fate
 */
public class EventReceiver extends BroadcastReceiver {
 
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.LOG_TAG, "Receiving event broadcast to update widgets");
        Intent newIntent = new Intent(context, RefreshService.class);
        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
            newIntent.putExtra("updateContexts", false);
        else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
            newIntent.putExtra("updateWidgets", false);
        context.startService(newIntent);
    }
 
}