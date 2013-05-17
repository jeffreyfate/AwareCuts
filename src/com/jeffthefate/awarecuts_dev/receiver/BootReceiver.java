package com.jeffthefate.awarecuts_dev.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jeffthefate.awarecuts_dev.common.Constants;
import com.jeffthefate.awarecuts_dev.common.Util;
/**
 * Receives when device is finished booting.  Starts the service that sets up
 * the receivers that start the service that updates contexts and widgets.
 * 
 * @author Jeff Fate
 */
public class BootReceiver extends BroadcastReceiver {
 
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.LOG_TAG, "Receiving boot completed broadcast");
        if (Util.getWidgets(Util.ICONS_FILENAME).length > 0)
            context.startService(new Intent(context, ReceiverService.class));
    }
 
}