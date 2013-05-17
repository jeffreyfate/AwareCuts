package com.jeffthefate.awarecuts_dev.widget;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jeffthefate.awarecuts_dev.ApplicationEx;
import com.jeffthefate.awarecuts_dev.common.Constants;
import com.jeffthefate.awarecuts_dev.common.Util;
import com.jeffthefate.awarecuts_dev.receiver.ReceiverService;
/**
 * Reciever for the widgets.  Describes what happens when a widget is created,
 * updated, deleted.  Also, what happens when first widget is added and last
 * widget is deleted.
 * 
 * @author Jeff Fate
 *
 */
public class WidgetProvider extends AppWidgetProvider {
    
    @Override
    public void onEnabled(Context context) {
        Log.d(Constants.LOG_TAG, "onEnabled");
        super.onEnabled(context);
    }
    
    @Override
    public void onDisabled(Context context) {
        Log.d(Constants.LOG_TAG, "onDisabled");
        Util.deleteFile(Util.LAST_TIME_UPDATED);
        Util.deleteFile(Util.ICONS_FILENAME);
        context.stopService(new Intent(context, ReceiverService.class));
        super.onDisabled(context);
    }
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, 
            int[] appWidgetIds) {
        Log.d(Constants.LOG_TAG, "onUpdate");
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        if (!isMyServiceRunning(
                "com.jeffthefate.awarecuts_dev.receiver.ReceiverService")) {
            Intent intent = new Intent(context, ReceiverService.class);
            intent.putExtra("appWidgetIds", appWidgetIds);
            context.startService(intent);
        }
    }
    
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(Constants.LOG_TAG, "onDeleted");
        for (int widgetId : appWidgetIds) {
            Util.deleteAllPrefs(widgetId);
        }
        super.onDeleted(context, appWidgetIds);
    }
    
    private boolean isMyServiceRunning(String serviceName) {
        ActivityManager manager = (ActivityManager) ApplicationEx.getApp().
                getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
}
