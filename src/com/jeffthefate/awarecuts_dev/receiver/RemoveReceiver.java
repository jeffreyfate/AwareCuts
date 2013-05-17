package com.jeffthefate.awarecuts_dev.receiver;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jeffthefate.awarecuts_dev.common.Constants;
import com.jeffthefate.awarecuts_dev.common.Util;
import com.jeffthefate.awarecuts_dev.widget.RefreshService;
/**
 * Receives when a package is removed from the device.  Gets the package name
 * that was removed, then removes the icon from the cache.
 * 
 * @author Jeff Fate
 *
 */
public class RemoveReceiver extends BroadcastReceiver {
 
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.LOG_TAG, "Receiving package removed broadcast");
        // Remove all instances of this app from the database
        String removedPackage = 
            intent.getDataString().replaceFirst("package:", "");
        File[] files = context.getFilesDir().listFiles();
        for (File file : files) {
            if (file.getName().contains(removedPackage))
                Util.deleteFile(file.getName());
        }
        context.startService(new Intent(context, RefreshService.class));
    }
 
}