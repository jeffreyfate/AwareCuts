package com.jeffthefate.awarecuts_dev.widget;

import java.io.File;
import java.util.List;
import java.util.Set;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.util.Log;

import com.jeffthefate.awarecuts_dev.ApplicationEx;
import com.jeffthefate.awarecuts_dev.common.Constants;
import com.jeffthefate.awarecuts_dev.common.DatabaseHelper;
import com.jeffthefate.awarecuts_dev.common.Util;

/**
 * An "interface" to restart the service if it has been killed.  This is run 
 * each time a shortcut is tapped to launch, so each time the user wants to 
 * launch something from the app, the service is sure to be running again if it
 * wasn't before.
 * 
 * @author Jeff Fate
 */
public class RefreshService extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    int[] appWidgetIds = null;
    boolean start = false;
    boolean config = false;
    boolean firstRun = false;
    boolean updateContexts = true;
    boolean updateWidgets = true;
    long time = -1;
    long currTime = -1;
    final long REFRESH_TIME = 5000;
    
    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            synchronized (this) {
                time = Util.getLongFromFile(Util.LAST_TIME_UPDATED);
                if (time == -1)
                    firstRun = true;
                else
                    firstRun = false;
                currTime = System.currentTimeMillis();
                if (time <= currTime - REFRESH_TIME || config || start) {
                    Util.writeLongToFile(Util.LAST_TIME_UPDATED, currTime);
                    if (time > currTime - REFRESH_TIME)
                        Util.refreshWidgets(appWidgetIds, true, currTime, time,
                                Util.addRecentAppsToStack(), updateContexts,
                                updateWidgets, config, start);
                    else
                        Util.refreshWidgets(appWidgetIds, firstRun, currTime,
                                time, Util.addRecentAppsToStack(), 
                                updateContexts, updateWidgets, config, start);
                }
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }
    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        
        // Get the HandlerThread's Looper and use it for our Handler 
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Upgrade database
        PackageManager pm = ApplicationEx.getApp().getPackageManager();
        boolean upgrade = true;
        Cursor cur = ApplicationEx.db.rawQuery("SELECT * FROM " + 
                DatabaseHelper.APP_TABLE, null);
        for (String colName : cur.getColumnNames()) {
            if (colName.equals(DatabaseHelper.COL_ACTIVITY))
                upgrade = false;
        }
        if (upgrade) {
            // If the Activity column doesn't exist, add it
            String sqlString = "ALTER TABLE " + DatabaseHelper.APP_TABLE + 
                    " ADD " + DatabaseHelper.COL_ACTIVITY + " TEXT";
            try {
                ApplicationEx.db.execSQL(sqlString);
            } catch (SQLException e) {
                Log.e(Constants.LOG_TAG, "Bad SQL string: " + sqlString, e);
                cur.close();
            }
            // Update all entries to add the activity
            // Get the task of each entry
            cur = ApplicationEx.db.rawQuery("SELECT * FROM " + 
                    DatabaseHelper.APP_TABLE, null);
            byte[] taskBytes = null;
            Intent ints = new Intent(Intent.ACTION_MAIN, null);
            ints.addCategory(Intent.CATEGORY_LAUNCHER); 
            List<ResolveInfo> list = pm.queryIntentActivities(ints, 
                    PackageManager.PERMISSION_GRANTED);
            String activityName = null;
            Set<String> categories = null;
            String packageName = null;
            if (cur.moveToFirst()) {
                do {
                    // Get task.baseIntent
                    packageName = cur.getString(cur.getColumnIndex(
                            DatabaseHelper.COL_PACKAGE));
                    taskBytes = cur.getBlob(cur.getColumnIndex(
                            DatabaseHelper.COL_TASK));
                    Parcel parcel = Parcel.obtain();
                    parcel.unmarshall(taskBytes, 0, taskBytes.length);
                    parcel.setDataPosition(0);
                    ActivityManager.RecentTaskInfo task = 
                        (RecentTaskInfo) parcel.readValue(
                                RecentTaskInfo.class.getClassLoader());
                    ResolveInfo resolveInfo = pm.resolveActivity(
                                task.baseIntent, 0);
                    categories = task.baseIntent.getCategories();
                    if (resolveInfo.activityInfo.name == null || 
                        resolveInfo.activityInfo.name.equals("") ||
                        (categories != null && categories.contains(
                                Intent.CATEGORY_CAR_DOCK)) ||
                        (categories != null && categories.contains(
                                Intent.CATEGORY_DESK_DOCK))) {
                        for (ResolveInfo rInfo : list) {
                            if (rInfo.activityInfo.packageName.equals(
                                    packageName)) {
                                activityName = rInfo.activityInfo.name;
                            }
                        }
                    }
                    else
                        activityName = resolveInfo.activityInfo.name;
                    ContentValues cv = new ContentValues();
                    cv.put(DatabaseHelper.COL_NAME, 
                            cur.getString(cur.getColumnIndex(
                                    DatabaseHelper.COL_NAME)));
                    cv.put(DatabaseHelper.COL_TASK, taskBytes);
                    cv.put(DatabaseHelper.COL_PACKAGE, packageName);
                    cv.put(DatabaseHelper.COL_ACTIVITY, activityName);
                    long row = ApplicationEx.dbHelper.updateRecord(cv, 
                            DatabaseHelper.APP_TABLE, 
                            DatabaseHelper.COL_PACKAGE + "=" + 
                            DatabaseUtils.sqlEscapeString(packageName));
                } while (cur.moveToNext());
            }
            for (File fileNew : this.getFilesDir().listFiles()) {
                fileNew.delete();
            }
        }
        cur.close();
        upgrade = true;
        cur = ApplicationEx.db.rawQuery("SELECT * FROM " + 
                DatabaseHelper.CONTEXT_TABLE, null);
        for (String colName : cur.getColumnNames()) {
            if (colName.equals(DatabaseHelper.COL_DUR))
                upgrade = false;
        }
        if (upgrade) {
            // If the Activity column doesn't exist, add it
            String sqlString = "ALTER TABLE " + DatabaseHelper.CONTEXT_TABLE + 
                    " ADD " + DatabaseHelper.COL_DUR + " INTEGER";
            try {
                ApplicationEx.db.execSQL(sqlString);
            } catch (SQLException e) {
                Log.e(Constants.LOG_TAG, "Bad SQL string: " + sqlString, e);
                cur.close();
            }
        }
        cur.close();
        if (intent != null && intent.getExtras() != null) {
            if (intent.getExtras().containsKey("appWidgetIds"))
                appWidgetIds = intent.getExtras().getIntArray("appWidgetIds");
            else if (intent.getExtras().containsKey("start"))
                start = intent.getExtras().getBoolean("start");
            else if (intent.getExtras().containsKey("config"))
                config = intent.getExtras().getBoolean("config");
            else if (intent.getExtras().containsKey("updateContexts"))
                updateContexts = intent.getExtras().getBoolean(
                        "updateContexts");
            else if (intent.getExtras().containsKey("updateWidgets"))
                updateWidgets = intent.getExtras().getBoolean("updateWidgets");
        }
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
