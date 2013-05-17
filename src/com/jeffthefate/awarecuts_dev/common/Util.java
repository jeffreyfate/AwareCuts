package com.jeffthefate.awarecuts_dev.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Parcel;
import android.provider.Browser;
import android.provider.Browser.BookmarkColumns;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Photo;
import android.provider.ContactsContract.Data;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.WindowManager;

import com.jeffthefate.awarecuts_dev.common.DatabaseHelper;
import com.jeffthefate.awarecuts_dev.ApplicationEx;
import com.jeffthefate.awarecuts_dev.R;
import com.jeffthefate.awarecuts_dev.receiver.ReceiverService;
import com.jeffthefate.awarecuts_dev.widget.RemoteViewsWidget;
import com.jeffthefate.awarecuts_dev.widget.WidgetProvider;

/**
 * Parent class for helper methods to persist, compare and store recent apps
 * and contexts in the database.
 * 
 * @author Jeff Fate
 *
 */
public class Util {
    /**
     * Name of the file persisting recent apps
     */
    public static final String APPS_FILENAME = "recentApps.txt";
    /**
     * Name of the file persisting recent bookmarks
     */
    public static final String BOOKMARKS_FILENAME = "recentBookmarks.txt";
    /**
     * Name of the file persisting all widgets' icon number preference
     */
    public static final String ICONS_FILENAME = "numIcons.txt";
    /**
     * Name of the file persisting the current largest widget
     */
    public static final String LARGEST_WIDGET_FILENAME = "largestWidget.txt";
    /**
     * Prefix for all the files holding info about each widget
     */
    public static final String WIDGET_FILENAME = "widget_";
    /**
     * Name of the file persisting last recorded battery level
     */
    public static final String BATTERY_FILENAME = "lastBatteryLevel.txt";
    /**
     * Base of filename that persists the relevant list for each widget
     */
    public static final String RELEVANT_FILENAME = "relevant_";
    /**
     * Name of the file persisting the last time phone contacted some one
     */
    public static final String LAST_TIME_CONTACTED = "lastTimeContacted.txt";
    /**
     * Name of the file persisting the last time any widget was updated
     */
    public static final String LAST_TIME_UPDATED = "lastTimeUpdated.txt";
    /**
     * Name of the file that keeps track of showing instructions
     */
    public static final String FIRST_USE_FILENAME = "firstUse.txt";
    public static final String TYPE_FILENAME = "type.txt";
    private static final String LOG_FILENAME = "log_";
    private static final String DURATION_FILENAME = "duration_";
    /**
     * Maximum number of recent tasks that will be retrieved
     */
    private static final int MAX_RECENT_TASKS = 30;
    /**
     * Global context for this entire application
     */
    private static final Context context = ApplicationEx.getApp();
    /**
     * Millisecond range to create max and min for contexts by time (30 minutes)
     */
    public final static long TIME_RANGE = 600000;
    /**
     * Range for latitude and longitude for contexts by location
     */
    public final static double LAT_LON_RANGE = 0.05;
    /**
     * Range for battery level contexts
     */
    public final static int BATT_RANGE = 9;
    /**
     * Global cursor object for Util class
     */
    private static Cursor cur;
    /**
     * Read the entire string from a file.
     * @param filename  file in local storage to read
     * @return  the text read from the file
     */
    public static String readStringFromFile(String filename) {
        File[] files = context.getFilesDir().listFiles();
        File myFile = null;
        String text = "";
        for (File file : files) {
            if (file.getName().equalsIgnoreCase(filename))
                myFile = file;
        }
        if (myFile != null) {
            byte[] buffer = new byte[(int)myFile.length()];
            BufferedInputStream bufStream = null;
            try {
                bufStream = new BufferedInputStream(context.openFileInput(
                        filename), 1024);
            } catch (FileNotFoundException e) {
                Log.e(Constants.LOG_TAG, "File not found: " + filename, e);
            }
            try {
                bufStream.read(buffer);
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, "Unable to read file: " + filename, e);
            }
            text = new String(buffer);
            try {
                bufStream.close();
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, 
                        "Unable to close input stream for: " + filename, e);
            }
        }
        return text;
    }
    /**
     * Delete a file from the app's file store.
     * @param filename  name of the file to be deleted
     * @return  if the delete was successful
     */
    public static boolean deleteFile(String filename) {
        return getFile(filename).delete();
    }
    /**
     * Get file from application's data directory.
     * @param filename  name of the file to be fetched
     * @return  the file retrieved
     */
    public static File getFile(String filename) {
        File[] files = context.getFilesDir().listFiles();
        for (File file : files) {
            if (file.getName().equalsIgnoreCase(filename))
                return file;
        }
        return new File(filename);
    }
    /**
     * Find if a file is present in the application's data directory.
     * @param filename  name of the file to be found
     * @return  if the file was found
     */
    public static boolean findFile(String filename) {
        File[] files = context.getFilesDir().listFiles();
        for (File file : files) {
            if (file.getName().equalsIgnoreCase(filename))
                return true;
        }
        return false;
    }
    /**
     * Write a byte array representing a string to a file.
     * @param buffer    byte array containing the byte representation of the 
     *                  string
     * @param filename  the file to write to
     */
    public static void writeBufferToFile(byte[] buffer, String filename) {
        BufferedOutputStream bufStream = null;
        try {
            bufStream = new BufferedOutputStream(context.openFileOutput(
                    filename, Context.MODE_PRIVATE), 1024);
            bufStream.write(buffer);
            bufStream.flush();
            bufStream.close();
        } catch (FileNotFoundException e) {
            Log.e(Constants.LOG_TAG, "File not found: " + filename, e);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, 
                    "BufferedOutputStream failed for: " + filename, e);
        }
    }
    /**
     * Write a byte array representing a string to a file.
     * @param icon  icon to be written to a file
     * @param filename  the file to write to
     * @see android.graphics.Bitmap
     */
    public static void writePngToFile(Bitmap icon, String filename, 
            boolean isContact) {
        FileOutputStream fileStream = null;
        if (filename.contains("/")) {
            filename = filename.replaceAll("/", "");
        }
        File[] files = context.getFilesDir().listFiles();
        for (File file : files) {
            if (file.getName().equalsIgnoreCase(filename+".png") && !isContact)
                return;
        }
        try {
            fileStream = context.openFileOutput(filename + ".png", 
                    Context.MODE_WORLD_READABLE);
        } catch (FileNotFoundException e) {
            Log.e(Constants.LOG_TAG, "File not found: " + filename, e);
        }
        BufferedOutputStream bos = new BufferedOutputStream(fileStream);
        boolean compressed = icon.compress(CompressFormat.PNG, 100, bos);
        if (compressed) {
            try {
                bos.flush();
                bos.close();
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, 
                        "BufferedOutputStream failed for: " + filename, e);
            }
        }
        else {
            Log.e(Constants.LOG_TAG, "Error compressing PNG for " + filename);
        }
    }
    /**
     * Get the list of widget IDs from the given file.
     * @param filename  get the widget IDs from this file
     * @return  array of widget IDs found (int)
     */
    public static int[] getWidgets(String filename) {
        String text = readStringFromFile(filename);
        int[] widgetInts = new int[0];
        if (!text.equals("")) {
            String[] tokens = text.split("~{1}");
            widgetInts = new int[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].length() > 0) {
                    int temp = -1;
                    try {
                        temp = Integer.valueOf(tokens[i].split(",{1}")[0]);
                        widgetInts[i] = temp;
                    } catch (NumberFormatException e) {
                        Log.e(Constants.LOG_TAG, "Number not found from " +
                                filename, e);
                    }
                }
            }
        }
        return widgetInts;
    }
    /**
     * Get the widget id associated with a preference value in a saved file.
     * @param filename  file to read from
     * @param pref  preference value to look for in the file
     * @return  the widget ID found, -1 if not found
     */
    public static int getWidgetFromPref(String filename, int pref) {
        String text = readStringFromFile(filename);
        String[] tokens = text.split("~{1}");
        for (int i = tokens.length-1; i > -1; i--) {
            if (tokens[i].contains("," + pref)) {
                int temp = -1;
                try {
                    temp = Integer.valueOf(tokens[i].split(",{1}")[0]);
                    return temp;
                } catch (NumberFormatException e) {
                    Log.e(Constants.LOG_TAG, "Number not found from " +
                            filename, e);
                }
            }
        }
        return -1;
    }
    /**
     * Get the preference value associated with a widget id in a saved file.
     * @param filename  file to read from
     * @param widget    widget to get the associated preference value from
     */
    public static int getPrefFromWidget(String filename, int widget) {
        String text = readStringFromFile(filename);
        String[] tokens = text.split("~{1}");
        for (int i = tokens.length-1; i > -1; i--) {
            if (tokens[i].contains(widget + ",")) {
                int temp = 0;
                try {
                    temp = Integer.valueOf(tokens[i].split(",{1}")[1]);
                    return temp;
                } catch (NumberFormatException e) {
                    Log.e(Constants.LOG_TAG, "Number not found from " +
                            filename, e);
                }
            }
        }
        return 0;
    }
    /**
     * Get the previous widgets size together in given widget's group.
     * @param widget    widget to get the associated preference value from
     */
    public static int getWidgetRows(int widget) {
        String contents = readStringFromFile(WIDGET_FILENAME + widget + ".txt");
        if (!contents.equals("")) {
            int temp = 0;
            try {
                temp = Integer.valueOf(contents.split(":{1}")[0]);
                return temp;
            } catch (NumberFormatException e) {
                Log.e(Constants.LOG_TAG, "Number not found from " +
                        WIDGET_FILENAME + widget + ".txt", e);
            }
        }
        return 0;
    }
    /**
     * Get remote views for the given widget ID.
     * @param widgetId  ID of the widget to fetch the remote views for
     * @return  RemoteViewsWidget object for the widget ID
     */
    public static RemoteViewsWidget getWidgetRemoteViews(int widgetId) {
        int numIcons = getPrefFromWidget(ICONS_FILENAME, widgetId);
        int rowNum = getWidgetRows(widgetId);
        int layoutRes = 0;
        switch (numIcons)
        {
        case -1:
            layoutRes = R.layout.widget_1x1;
            break;
        case 1:
            layoutRes = R.layout.widget_1x1;
            break;
        case 2:
            layoutRes = R.layout.widget_2x1;
            break;
        case 8:
            layoutRes = R.layout.widget_2x1;
            break;
        case 3:
            layoutRes = R.layout.widget_3x1;
            break;
        case 12:
            layoutRes = R.layout.widget_3x1;
            break;
        case 4:
            layoutRes = R.layout.widget_4x1;
            break;
        case 16:
            layoutRes = R.layout.widget_4x1;
            break;
        case 5:
            layoutRes = R.layout.widget_5x1;
            break;
        case 20:
            layoutRes = R.layout.widget_5x1;
            break;
        default:
            layoutRes = R.layout.widget_4x1;
            break;
        }
        RemoteViewsWidget views = new RemoteViewsWidget(
                context, widgetId, numIcons, rowNum, layoutRes);
        views.updateWidget();
        return views;
    }
    /**
     * Get the IDs of the other widgets in a widget group.
     * @param widgetId  source widget ID of widget group
     * @return  ArrayList of the other widget IDs as Integer
     */
    public static ArrayList<Integer> getOtherIds(int widgetId) {
        ArrayList<Integer> widgets = new ArrayList<Integer>();
        String widgetString = readStringFromFile(WIDGET_FILENAME + widgetId +
                ".txt");
        String[] tokens = widgetString.split(":{1}");
        if (tokens.length > 1 && !tokens[1].equals(""))
            tokens = tokens[1].split(";{1}");
        else
            return widgets;
        for (String widgetIdString : tokens) {
            int temp = 0;
            try {
                temp = Integer.valueOf(widgetIdString);
                widgets.add(temp);
            } catch (NumberFormatException e) {
                Log.e(Constants.LOG_TAG, "Other IDs not found from " +
                        WIDGET_FILENAME + widgetId + ".txt", e);
            }
        }
        return widgets;
    }
    /**
     * Get the child IDs of a widget group, given the parent ID.
     * @param parentWidget  ID of the parent of the widget group
     * @return  ArrayList of the child widget IDs as Integer
     */
    public static ArrayList<Integer> getChildIds(int parentWidget) {
        ArrayList<Integer> childIds = new ArrayList<Integer>();
        int[] widgets = getWidgets(ICONS_FILENAME);
        for (int widget : widgets) {
            String[] tokens = readStringFromFile(
                    WIDGET_FILENAME + widget + ".txt").split(":{1}");
            if (tokens.length > 1 && !tokens[1].equals("")) {
                tokens = tokens[1].split(";{1}");
                for (String token : tokens) {
                    if (token.equals(String.valueOf(parentWidget))) {
                        int temp = 0;
                        try {
                            temp = Integer.valueOf(widget);
                            childIds.add(temp);
                        } catch (NumberFormatException e) {
                            Log.e(Constants.LOG_TAG, "Child ID not found " +
                                    "from " + parentWidget, e);
                        }
                    }
                }
            }
        }
        return childIds;
    }
    /**
     * Get all parent IDs; the first widgets in the widget groups present.
     * @return  ArrayList of all first widget IDs of widget groups, as Integer
     */
    public static ArrayList<Integer> getParentIds() {
        ArrayList<Integer> parentIds = new ArrayList<Integer>();
        for (File file : context.getFilesDir().listFiles()) {
            if (file.getName().contains(WIDGET_FILENAME)) {
                String fileContents = readStringFromFile(file.getName());
                if (!fileContents.equals("")) {
                    String[] tokens = fileContents.split(":{1}");
                    int temp = 0;
                    try {
                        if (Integer.valueOf(tokens[0]) == 0) {
                            temp = Integer.valueOf(file.getName().substring(
                                    WIDGET_FILENAME.length(), 
                                    file.getName().indexOf(".")));
                            parentIds.add(temp);
                        }
                    } catch (NumberFormatException e) {
                        Log.e(Constants.LOG_TAG, "Parent widget not found!", e);
                    }
                }
            }
        }
        return parentIds;
    }
    /**
     * Get the size of a group, given the ID of the parent of the group.
     * @param parentId
     * @return
     */
    public static int getGroupSize(int parentId) {
        int size = getPrefFromWidget(ICONS_FILENAME, parentId);
        for (int childId : getChildIds(parentId)) {
            size += getPrefFromWidget(ICONS_FILENAME, childId);
        }
        return size;
    }
    /**
     * Get a list of all widgets in a group, including the size.
     * @param appWidgetId   source ID in the group
     * @param rows  the size of the previous widget in the group
     * @return  ArrayList of all widgets in a group, and the size, in Integer
     */
    public static ArrayList<Integer> getOtherWidgets(int appWidgetId, int rows) {
        ArrayList<Integer> widgets = new ArrayList<Integer>();
        widgets.add(appWidgetId);
        int temp = 0;
        if (rows == -1)
            temp = 4;
        else
            temp = rows;
        int widget = -1;
        String widgetString = "";
        if (temp > 0) {
            widget = getWidgetFromPref(ICONS_FILENAME, rows);
            if (widget != -1) {
                widgetString = readStringFromFile(WIDGET_FILENAME + 
                        widget + ".txt");
                String[] tokens = widgetString.split(":{1}");
                if (!tokens[0].equals("")) {
                    try {
                        temp += Integer.valueOf(tokens[0]);
                    } catch (NumberFormatException e) {
                        Log.e(Constants.LOG_TAG, "Number not found from " +
                                tokens[0], e);
                    }
                }
                if (tokens.length > 1) {
                    String[] widgetIds = tokens[1].split(";{1}");
                    for (String widgetId: widgetIds) {
                        try {
                            widgets.add(Integer.valueOf(widgetId));
                        } catch (NumberFormatException e) {
                            Log.e(Constants.LOG_TAG, "Number not found from " +
                                    widgetId, e);
                        }
                    }
                }
                widgets.add(widget);
            }
        }
        widgets.add(temp);
        return widgets;
    }
    /**
     * Remove a widget, preference pair from the given file.
     * @param filename  file to read and remove from
     * @param widgetId  widget id associated with the pref to delete
     */
    public static void deletePrefFromFile(String filename, int widgetId) {
        String text = readStringFromFile(filename);
        if (text.contains(String.valueOf(widgetId))) {
            String[] tokens = text.split("~{1}");
            text = "";
            for (String token : tokens) {
                if (!token.contains(String.valueOf(widgetId)))
                    text = text + token + "~";
            }
        }
        writeBufferToFile((text).getBytes(), filename);
    }
    /**
     * Remove all widget, preference pairs for the given widget id.
     * @param widgetId            widget id associated with the prefs to delete
     */
    public static void deleteAllPrefs(int widgetId) {
        updateWidgetFiles(widgetId);
        deleteFile(WIDGET_FILENAME + widgetId + ".txt");
        deleteFile(RELEVANT_FILENAME + widgetId + ".txt");
        deletePrefFromFile(ICONS_FILENAME, widgetId);
        deletePrefFromFile(TYPE_FILENAME, widgetId);
        setLargestWidget();
        if (getWidgets(ICONS_FILENAME).length == 0 || getLargestWidget() == 0) {
            deleteFile(LAST_TIME_UPDATED);
            deleteFile(ICONS_FILENAME);
            deleteFile(TYPE_FILENAME);
            for (File file : context.getFilesDir().listFiles()) {
                if (file.getName().contains(RELEVANT_FILENAME) ||
                        file.getName().contains(WIDGET_FILENAME))
                    deleteFile(file.getName());
            }
            context.stopService(new Intent(context, ReceiverService.class));
        }
    }
    /**
     * Update all files that are linked to a widget; used when a widget is 
     * deleted.
     * @param widgetId    id of the widget that his been removed
     */
    public static void updateWidgetFiles(int widgetId) {
        int widgetSize = getPrefFromWidget(ICONS_FILENAME, widgetId);
        if (widgetSize > 0) {
            int currSize = 0;
            File[] files = context.getFilesDir().listFiles();
            String currString = "";
            for (File file : files) {
                if (file.getName().contains(WIDGET_FILENAME) && 
                        !file.getName().contains(Integer.toString(widgetId))) {
                    currString = readStringFromFile(file.getName());
                    if (currString != null && !currString.equals("")) {
                        String[] tokens = currString.split(":{1}");
                        try {
                            currSize = Integer.valueOf(tokens[0]);
                            if (tokens.length > 1) {
                                if (tokens[1].contains(
                                        Integer.toString(widgetId))) {
                                    tokens[0] = String.valueOf(
                                            currSize - widgetSize);
                                    tokens[1] = tokens[1].replace(
                                            String.valueOf(widgetId) + ";", "");
                                    writeBufferToFile((tokens[0] + ":" + 
                                                tokens[1]).getBytes(), 
                                            file.getName());
                                }
                            }
                        } catch (NumberFormatException e) {
                            Log.e(Constants.LOG_TAG, "Number not found from " +
                                    tokens[0], e);
                        }
                    }
                }
            }
        }
    }
    /**
     * Search through all widgets to find the largest collection and persist it
     * to a file.
     */
    public static void setLargestWidget() {
        int currWidget = 0;
        int largest = 0;
        int currSize = 0;
        // Step through each widget file
        File[] files = context.getFilesDir().listFiles();
        String currString = "";
        for (File file : files) {
            if (file.getName().contains(WIDGET_FILENAME)) {
                String[] tokens = {""};
                try {
                    currWidget = Integer.valueOf(file.getName().substring(
                            WIDGET_FILENAME.length(), 
                            file.getName().indexOf(".txt")));
                    currString = readStringFromFile(file.getName());
                    if (currString != null && !currString.equals("")) {
                        // Get other widgets in the group
                        tokens = currString.split(":{1}");
                        // Add up the size of the entire group
                        currSize = Integer.valueOf(tokens[0]) + 
                            getPrefFromWidget(ICONS_FILENAME, currWidget);
                        if (largest < currSize)
                            largest = currSize;
                    }
                } catch (NumberFormatException e) {
                    Log.e(Constants.LOG_TAG, "Number not found from " +
                            file.getName() + " or " + tokens[0], e);
                }
            }
        }
        writeBufferToFile(String.valueOf(largest).getBytes(), 
                LARGEST_WIDGET_FILENAME);
    }
    /**
     * Read a JSON file to a JSON array.
     * @param filename  the file to read
     * @return  a JSON array containing the JSON string in the file
     * @see org.json.JSONArray
     */
    public static JSONArray readJsonFile(String filename) {
        String jsonString = null;
        File[] files = context.getFilesDir().listFiles();
        File myFile = null;
        JSONArray jsonArray = null;
        
        for (File file : files) {
            if (file.getName().equalsIgnoreCase(filename))
                myFile = file;
        }
        if (myFile != null) {
            byte[] buffer = new byte[(int)myFile.length()];
            BufferedInputStream bufStream = null;
            try {
                bufStream = new BufferedInputStream(context.openFileInput(
                        filename), 1024);
            } catch (FileNotFoundException e) {
                Log.e(Constants.LOG_TAG, "File not found: " + filename, e);
            }
            try {
                bufStream.read(buffer);
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, 
                        "Can't read buffer for file: " + filename, e);
            }
            jsonString = new String(buffer);
            try {
                jsonArray = new JSONArray(jsonString);
            } catch (JSONException e) {
                Log.e(Constants.LOG_TAG, 
                        "Bad JSON array from file: " + filename, e);
            }
            try {
                bufStream.close();
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, 
                        "BufferedOutputStream failed for: " + filename, e);
            }
        }
        return jsonArray;
    }
    /**
     * Write a JSON array to a file.
     * @param filename  file to write
     * @param jsonArray the array to write
     * @see org.json.JSONArray
     */
    public static void writeJsonFile(String filename, JSONArray jsonArray) {
        if (jsonArray != null) {
            BufferedOutputStream bufStream = null;
            try {
                bufStream = new BufferedOutputStream(context.openFileOutput(
                        filename, Context.MODE_PRIVATE), 1024);
            } catch (FileNotFoundException e) {
                Log.e(Constants.LOG_TAG, "File not found: " + filename, e);
            }
            try {
                bufStream.write(jsonArray.toString().getBytes());
                bufStream.flush();
                bufStream.close();
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, 
                        "BufferedOutputStream failed for: " + filename, e);
            }
        }
    }
    /**
     * Top level method to be called by the periodic service to check the new
     * list of recent tasks against what was saved the last time the service 
     * was run.
     * <p>It adds new contexts linked with apps, each in separate tables.  New 
     * apps are added to the apps table if they aren't already there when a new
     * context needs to be added.
     * @param filename  file that has the persisted list of items
     * @param jsonArray array of current items to compare against the file list
     * @param currTime  current time of device recorded in RefreshService
     * @param oldTime   time read from the last updated time file
     * @param stack the stack of new apps, from recent list
     * @see org.json.JSONArray
     */
    public static void addContexts(String filename, JSONArray jsonArray,
            long currTime, long oldTime, Stack<String> newStack) {
        Stack<String> oldStack = new Stack<String>();
        oldStack.clear();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    oldStack.push(jsonArray.getString(i));
                } catch (JSONException e) {
                    Log.e(Constants.LOG_TAG, "Value doesn't exist at index " + 
                            i + " for the array " + jsonArray.toString(), e);
                }
            }
        }
        if (filename.equals(BOOKMARKS_FILENAME))
            newStack = addRecentBookmarksToStack();
        if (!newStack.empty())
            compareLists(oldStack, newStack, filename, currTime, oldTime);
    }
    /**
     * Gets the recent apps from the system and adds them to a stack.
     * @return  stack of strings; the list of recent apps
     */
    public static Stack<String> addRecentAppsToStack() {
        Stack<String> newAppStack = new Stack<String>();
        newAppStack.clear();
        final List<ActivityManager.RecentTaskInfo> recentList =
            getRecentTasks();
        final ListIterator<RecentTaskInfo> recentTasksIter = 
            recentList.listIterator(recentList.size());
        List<ResolveInfo> homeInfos = context.getPackageManager().
                queryIntentActivities(new Intent(Intent.ACTION_MAIN).
                addCategory(Intent.CATEGORY_HOME), 0);
        ResolveInfo resolveInfo = null;
        PackageManager pm = context.getPackageManager();
        boolean isHome = false;
        while (recentTasksIter.hasPrevious()) {
            final ActivityManager.RecentTaskInfo task =
                    recentTasksIter.previous();
            resolveInfo = pm.resolveActivity(task.baseIntent, 0);
            if (resolveInfo == null)
                continue;
            for (ResolveInfo currInfo : homeInfos) {
                if (resolveInfo.activityInfo != null && (
                        resolveInfo.activityInfo.name.equals(
                                currInfo.activityInfo.name) || 
                        resolveInfo.activityInfo.packageName.equals(
                                currInfo.activityInfo.packageName))) {
                    isHome = true;
                    break;
                }
            }
            if (isHome) {
                isHome = false;
                continue;
            }
            if (resolveInfo.activityInfo != null && (
                    resolveInfo.activityInfo.packageName.equals(
                            ApplicationEx.getApp().getPackageName()) || 
                    resolveInfo.activityInfo.name.equals(
                    "com.android.contacts.activities.ContactDetailActivity") ||
                    resolveInfo.activityInfo.name.equals(
                        "com.jeffthefate.awarecutspro.ActivityDonate") || 
                    resolveInfo.activityInfo.name.equals(
                        "com.android.vending.MyDownloadsActivity") ||
                    resolveInfo.activityInfo.packageName.equals(
                        "com.android.packageinstaller") ||
                    resolveInfo.activityInfo.name.equals(
                        "com.motorola.usb.UsbErrorActivity") ||
                    resolveInfo.activityInfo.name.contains(
                        "gsf.login.LoginActivity"))) {
                continue;
            }
            if ((task.baseIntent.getFlags() & 
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) != 
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                newAppStack.push(task.baseIntent.getComponent().getClassName());
        }
        return newAppStack;
    }
    /**
     * Gets the recently used apps on the device.
     * @return a list of recent tasks
     * @see android.app.ActivityManager.RecentTaskInfo
     */
    public static List<ActivityManager.RecentTaskInfo> getRecentTasks() {
        return ((ActivityManager) ApplicationEx.getApp().getSystemService(
                Context.ACTIVITY_SERVICE)).getRecentTasks(MAX_RECENT_TASKS, 
                        ActivityManager.RECENT_IGNORE_UNAVAILABLE);
    }
    /**
     * Insert a new context into the database with current device stats.  This
     * inserts a new context into the context table with the appId from the
     * apps table to link the tables.
     * @param appName   string representation of the app to be added
     * @param packageName   package name to be added with the app in the 
     *                      database
     * @param task  task information about the app to be in the database
     *              record
     * @param time  the duration of the app displayed by the user (approx)
     * @return  id of inserted row
     * @see android.app.ActivityManager.RecentTaskInfo
     */
    public static long addAppContext(String appName, String activity, 
            String packageName, ActivityManager.RecentTaskInfo task, 
            Long time) {
        Parcel parcel = Parcel.obtain();
        parcel.writeValue(task);
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_NAME, appName);
        cv.put(DatabaseHelper.COL_TASK, parcel.marshall());
        cv.put(DatabaseHelper.COL_PACKAGE, packageName);
        cv.put(DatabaseHelper.COL_ACTIVITY, activity);
        // Look for the current app in Apps table
        // If it isn't there, add it
        if (!ApplicationEx.dbHelper.inDb(appName, 
                DatabaseHelper.APP_TABLE, DatabaseHelper.COL_NAME))
            ApplicationEx.dbHelper.insertRecord(cv, DatabaseHelper.APP_TABLE, 
                    null);
        else
            ApplicationEx.dbHelper.updateRecord(cv, DatabaseHelper.APP_TABLE, 
                    DatabaseHelper.COL_NAME + "=" + 
                    DatabaseUtils.sqlEscapeString(appName));
        int appId = ApplicationEx.dbHelper.getRecordId(appName, 
                DatabaseHelper.COL_APP_ID, DatabaseHelper.APP_TABLE, 
                DatabaseHelper.COL_NAME);
        if (appId < 0)
            Log.e(Constants.LOG_TAG, appName + " not in database!");
        // Add app and context info to context table
        return addContext(appId, DatabaseHelper.COL_APP_ID, time);
    }
    /**
     * Insert a new context into the database with current device stats.  This
     * inserts a new context into the context table with the appId from the
     * apps table to link the tables.
     * @param bookmarkAddress   address of bookmark to add as context
     * @param visits    visits of this bookmark
     * @return  ID of inserted row
     */
    public static long addBookmarkContext(String bookmarkAddress, int visits) {
        // Look for the current app in Apps table
        // If it isn't there, add it
        int bookmarkId = 0;
        if (ApplicationEx.dbHelper.inDb(bookmarkAddress, 
                DatabaseHelper.BOOKMARK_TABLE, DatabaseHelper.COL_ADDRESS)) {
            // Get the id for current app
            bookmarkId = ApplicationEx.dbHelper.getRecordId(
                    bookmarkAddress, DatabaseHelper.COL_BOOK_ID,
                    DatabaseHelper.BOOKMARK_TABLE, DatabaseHelper.COL_ADDRESS);
            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COL_BOOK_VISITS, visits);
            ApplicationEx.dbHelper.updateRecord(cv, 
                    DatabaseHelper.BOOKMARK_TABLE, DatabaseHelper.COL_ADDRESS +
                    "='" + bookmarkAddress + "'");
        }
        // Add app and context info to context table
        return addContext(bookmarkId, DatabaseHelper.COL_BOOK_ID, (long) 0);
    }
    /**
     * Insert a new contact into the database.  It includes the lookup key,
     * name and time contacted.  This also adds a context for this instance.
     * @param lookupKey the unique identifier for the contact to add
     * @return  row number of the contact added or updated
     */
    public static long addContact(String lookupKey) {
        // Adding multiple rows to get contacts to appear higher
        int contactRows = 10;
        long contactTime = getContactTime(lookupKey);
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_LOOKUP, lookupKey);
        cv.put(DatabaseHelper.COL_CONTACT_NAME, getContactName(lookupKey));
        cv.put(DatabaseHelper.COL_CONTACT_TIME, contactTime);
        long result;
        // If it isn't there, add it
        if (!ApplicationEx.dbHelper.inDb(lookupKey, DatabaseHelper.CONTACT_TABLE,
                DatabaseHelper.COL_LOOKUP))
            result = ApplicationEx.dbHelper.insertRecord(
                    cv, DatabaseHelper.CONTACT_TABLE, null);
        else
            result = ApplicationEx.dbHelper.updateRecord(cv, 
                    DatabaseHelper.CONTACT_TABLE, 
                    DatabaseHelper.COL_LOOKUP + "='" + lookupKey + "'");
        if (ApplicationEx.dbHelper.inDb(lookupKey, DatabaseHelper.CONTACT_TABLE, 
                DatabaseHelper.COL_LOOKUP)) {
            for (int i = 0; i < contactRows; i++) {
                result = addContext(ApplicationEx.dbHelper.getRecordId(
                            lookupKey, DatabaseHelper.COL_CONTACT_ID,
                            DatabaseHelper.CONTACT_TABLE, 
                            DatabaseHelper.COL_LOOKUP),
                        DatabaseHelper.COL_CONTACT_ID, (long) 0);
                if (result == -1)
                    break;
            }
        }
        return result;
    }
    /**
     * Insert a new context from an id from app, bookmark or contact.
     * @param id    identifier of what is being added, from the corresponding
     *              table
     * @param idCol name of the column where the id is: app, bookmark or contact
     * @param time  the duration of the app displayed to the user (approx)
     * @return  row id of the added context
     */
    public static long addContext(int id, String idCol, Long time) {
        // Get all contexts associated with this id, sorted by index
        ContentValues cv = new ContentValues();
        int appId = 0; int bookmarkId = 0; int contactId = 0;
        if (idCol.equals(DatabaseHelper.COL_APP_ID))
            appId = id;
        if (idCol.equals(DatabaseHelper.COL_BOOK_ID))
            bookmarkId = id;
        if (idCol.equals(DatabaseHelper.COL_CONTACT_ID))
            contactId = id;
        cv.put(DatabaseHelper.COL_APP_ID,       appId);
        cv.put(DatabaseHelper.COL_BOOK_ID,      bookmarkId);
        cv.put(DatabaseHelper.COL_CONTACT_ID,   contactId);
        cv.put(DatabaseHelper.COL_LAT,          ApplicationEx.getLatLong()[0]);
        cv.put(DatabaseHelper.COL_LONG,         ApplicationEx.getLatLong()[1]);
        cv.put(DatabaseHelper.COL_TIME,         ApplicationEx.getTimeOfDay());
        cv.put(DatabaseHelper.COL_BLUE,         ApplicationEx.getBluetooth());
        cv.put(DatabaseHelper.COL_WIFI,         ApplicationEx.getWifi());
        cv.put(DatabaseHelper.COL_BATT,         ApplicationEx.getBattery());
        cv.put(DatabaseHelper.COL_DAY,          ApplicationEx.getDay());
        cv.put(DatabaseHelper.COL_HPHONE,       (ApplicationEx.
                isHeadphonePlugged() == 1 || ApplicationEx.isA2DPOn() == 1) ? 
                        1 : 0);
        cv.put(DatabaseHelper.COL_DUR,          time);
        long result = ApplicationEx.dbHelper.insertRecord(cv, 
                DatabaseHelper.CONTEXT_TABLE, DatabaseHelper.COL_TIME);
        return result;
    }
    /**
     * Get the current value for visits of a bookmark from the device.
     * @param address   URL of the bookmark
     * @return  visits of the given bookmark, -1 if it isn't found
     */
    public static int getBookmarkVisits(String address) {
        StringBuilder sb = new StringBuilder(BookmarkColumns.URL + " = ");
        DatabaseUtils.appendEscapedSQLString(sb, address);
        cur = context.getContentResolver().query(
                Browser.BOOKMARKS_URI, 
                new String[] {BookmarkColumns.VISITS},
                sb.toString(), null, null);
        if (cur == null) {
            cur.close();
            return -1;
        }
        if (cur.getCount() <= 0) {
            cur.close();
            return -1;
        }
        if (cur.moveToFirst()) {
            int visits = 0;
            int newVisits = 0;
            while (!cur.isAfterLast()) {
                newVisits = cur.getInt(cur.getColumnIndex(
                        BookmarkColumns.VISITS));
                visits = newVisits > visits ? newVisits : visits;
                cur.moveToNext();
            }
            cur.close();
            return visits;
        }
        cur.close();
        return -1;
    }
    /**
     * Compares the currently persisted list of apps to what is in the recent
     * tasks list.  It will add more apps to the persisted list if the recent
     * tasks list has changed since the last time it was saved.
     * @param oldStack  a stack of the last persisted list of apps
     * @param newStack  a stack of the new list of apps
     * @param filename  where to persist the list and used as an indicator as to
     *                  which type of context is being compared
     */
    public static void compareLists(Stack<String> oldStack, 
            Stack<String> newStack, String filename, long currTime, 
            long oldTime) {
        persist(filename, newStack);
        // Check to see if the contents are different
        String topNewStack = null;
        ActivityManager.RecentTaskInfo task;
        int oldSize = oldStack.size();
        boolean checked = false;
        Stack<String> diffStack = new Stack<String>();
        String topItem = "";
        while (newStack.size() > 0) {
            topNewStack = newStack.pop();
            if (filename.equals(BOOKMARKS_FILENAME)) {
                int visits = getBookmarkVisits(topNewStack);
                if (!ApplicationEx.dbHelper.inDb(topNewStack, 
                        DatabaseHelper.BOOKMARK_TABLE, 
                        DatabaseHelper.COL_ADDRESS)) {
                    String title = getBookmarkTitle(topNewStack);
                    Parcel parcel = Parcel.obtain();
                    parcel.writeValue(getBookmarkIcon(title));
                    ContentValues cv = new ContentValues();
                    cv.put(DatabaseHelper.COL_ADDRESS, topNewStack);
                    cv.put(DatabaseHelper.COL_BOOK_TITLE, title);
                    cv.put(DatabaseHelper.COL_BOOK_ICON, parcel.marshall());
                    cv.put(DatabaseHelper.COL_BOOK_VISITS, visits);
                    ApplicationEx.dbHelper.insertRecord(cv, 
                            DatabaseHelper.BOOKMARK_TABLE, null);
                }
                if (visits > ApplicationEx.dbHelper.getBookmarkVisits(
                        topNewStack))
                    addBookmarkContext(topNewStack, visits);
            }
            else {
                if (oldStack.size() > 0) {
                    if (!oldStack.peek().equals(topNewStack)) {
                        // Recent list has changed
                        if (oldStack.contains(topNewStack))
                            oldStack.remove(topNewStack);
                        diffStack.push(topNewStack);
                    }
                    else {
                        if (topItem.equals(""))
                            topItem = topNewStack;
                        oldStack.pop();
                    }
                }
                else
                    diffStack.push(topNewStack);
            }
        }
        if (!filename.equals(BOOKMARKS_FILENAME)) {
            List<RecentTaskInfo> recents = getRecentTasks();
            if (diffStack.size() == 0)
                diffStack.push(topItem);
            long diffTime = 
                ((currTime-oldTime)/(long)diffStack.size())/(long)1000;
            while (!diffStack.isEmpty()) {
                task = addAppFromString(diffStack.pop(), recents, diffTime);
                if (task == null)
                    getRecentTasks().remove(task);
            }
        }
    }
    /**
     * Cycles through the recent tasks list to find a specific app to add, from
     * a string.
     * @param name  app name to add
     * @param recentList    list of recent tasks from the device
     * @param time  the duration of the app displayed to the user (approx)
     * @return  the task of the inserted app; null if not added
     * @see android.app.ActivityManager.RecentTaskInfo
     */
    public static ActivityManager.RecentTaskInfo addAppFromString(String name,  
            List<ActivityManager.RecentTaskInfo> recentList, Long time) {
        final ListIterator<RecentTaskInfo> recentTasksIter = 
            recentList.listIterator();
        while (recentTasksIter.hasNext()) {
            final ActivityManager.RecentTaskInfo task = 
                    recentTasksIter.next();
            if (task.baseIntent.getComponent().getClassName().equals(name)) {
                if (!activityExported(
                        task.baseIntent.getComponent().getPackageName(), 
                        task.baseIntent.getComponent().getClassName())) {
                    Intent newIntent = new Intent();
                    newIntent.setAction(Intent.ACTION_MAIN);
                    newIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    newIntent.setPackage(
                            task.baseIntent.getComponent().getPackageName());
                    List<ResolveInfo> infos = context.getPackageManager().
                            queryIntentActivities(newIntent, 0);
                    for (ResolveInfo info : infos) {
                        addApp(task, context.getPackageManager(), time, 
                                newIntent);
                        return task;
                    }
                    continue;
                }
                if (task.baseIntent.getCategories() == null) {
                    addApp(task, context.getPackageManager(), time, null);
                    return task;
                }
                else {
                    if (!task.baseIntent.getCategories().contains(
                        Intent.CATEGORY_CAR_DOCK) &&
                        !task.baseIntent.getCategories().contains(
                        Intent.CATEGORY_DESK_DOCK)) {
                        addApp(task, context.getPackageManager(), time, null);
                        return task;
                    }
                }
            }
        }
        return null;
    }
    /**
     * Takes a task and gets all necessary data to add it to the database, then
     * adds it with all contextual information.  Discounts Home and MyDownloads 
     * apps to ensure it's a user initiated app.
     * @param task  the desired app to add 
     *              {@link android.app.ActivityManager.RecentTaskInfo}
     * @param pm    the PackageManager to help get all data from task
     * @param time  the duration of the app displayed to the user (approx)
     * @param homeInfo  ResolveInfo describing the Home activity
     * @return  id of inserted row
     * @see android.app.ActivityManager.RecentTaskInfo
     * @see android.content.pm.PackageManager
     * @see android.content.pm.ResolveInfo
     */
    public static long addApp(ActivityManager.RecentTaskInfo task, 
            PackageManager pm, Long time, Intent intent) {
        if (task == null) {
            Log.e(Constants.LOG_TAG, "task is null!");
            return -1;
        }
        if (intent == null) {
            intent = task.baseIntent;
            if (task.origActivity != null)
                intent.setComponent(task.origActivity);
        }
        if (intent.getAction() == null && (intent.getCategories() == null ||
                intent.getCategories().isEmpty()))
            return -2;
        ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
        
        if (resolveInfo == null)
            return -2;
        String title = resolveInfo.activityInfo.loadLabel(pm).toString();
        if (title == null || title.length() == 0 || 
            resolveInfo.activityInfo.loadIcon(pm) == null)
            return -2;
        String uri = intent.getDataString();
        if (uri != null) {
            if (uri.contains("http://") || uri.contains("https://")) {
                if (ApplicationEx.dbHelper.inDb(uri, 
                        DatabaseHelper.BOOKMARK_TABLE, 
                        DatabaseHelper.COL_ADDRESS))
                    return -2;
            }
        }
        return addAppContext(title, resolveInfo.activityInfo.name, 
                resolveInfo.activityInfo.packageName, task, time);
    }
    /**
     * Saves a list of apps/bookmarks/contexts to the given file.
     * @param filename  where to save the list as a JSONArray
     * @param stack collection (as stack) of the list of items to persist
     */    
    public static void persist(String filename, Stack<String> stack) {
        if (!stack.isEmpty()) {
            final ListIterator<String> listIter = stack.subList(
                    0, stack.size()).listIterator();
            JSONArray jsonArray = new JSONArray();
            while (listIter.hasNext()) {
                String app = listIter.next();
                jsonArray.put(app);
            }
            writeJsonFile(filename, jsonArray);
        }
    }
    /**
     * Calculate a number to use as the largest possible total icons in a widget
     * based on the largest number of icons in each row that exist.
     * @return largest possible widget value, 0 if there isn't one
     */
    public static int getLargestWidget() {
        String largestString = readStringFromFile(LARGEST_WIDGET_FILENAME);
        if (largestString != null && !largestString.equals(""))
            try {
                return Integer.valueOf(largestString);
            } catch (NumberFormatException e) {
                Log.e(Constants.LOG_TAG, "Number not found from " + 
                        largestString, e);
            }
        return 0;
    }
    /**
     * Update the contexts by adding new ones to the database and update the
     * views of the widgets.
     * @param appWidgetIDs  array of widget IDs from the AppWidget service on
     *                      device
     * @param firstRun  whether or not this was called from the first update of
     *                  the widget
     * @param currTime  the time of the device in milliseconds
     * @param oldTime   the time of the device in milliseconds when it was last 
     *                  updated
     * @param stack collection of recent apps on the device
     * @param updateContexts    whether or not to update the contexts
     * @param updateWidgets whether or not to update the widget views
     * @param if this call came from the config activity
     */
    public static void refreshWidgets(int[] appWidgetIds, boolean firstRun, 
            long currTime, long oldTime, Stack<String> stack, 
            boolean updateContexts, boolean updateWidgets, boolean config,
            boolean start) {
        JSONArray jsonArray;
        String filename;
        ArrayList<Integer> updateList = new ArrayList<Integer>();
        if (updateContexts)
            updateContexts(currTime, oldTime, stack);
        if (updateWidgets) {
            int[] widgets = getWidgets(ICONS_FILENAME);
            for (int widget : getParentIds()) {
                filename = RELEVANT_FILENAME + widget + ".txt";
                jsonArray = new JSONArray(getRelevantApps(widget));
                if (!findFile(filename) || 
                    !jsonArray.toString().equals(
                            readJsonFile(filename).toString()) || 
                    config || start ||
                    appWidgetIds != null)
                    updateList.add(widget);
                writeJsonFile(filename, jsonArray);
            }
            if (widgets.length > 0) {
                Intent widgetUpdate = new Intent(context, WidgetProvider.class);
                widgetUpdate.setAction(
                        AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                widgetUpdate.putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_IDS, widgets);
                context.sendBroadcast(widgetUpdate);
            }
            updateViews(appWidgetIds, firstRun, updateList, true);
        }
    }
    /**
     * Update views based on a layout - could be regular icon sizes layouts or
     * the progress bar, updating layout.
     * @param appWidgetManager  static AppWidgetManager used to interact with
     *                          the widgets
     * @param appWidgetIds  list of widget IDs
     * @param layoutRes if using a set layout, otherwise 0
     * @see android.appwidget.AppWidgetManager
     */
    public static void updateViews(int[] appWidgetIds, boolean firstRun,
            ArrayList<Integer> updateList, boolean sleep) {
        RemoteViewsWidget views;
        if (updateList == null)
            updateList = getParentIds();
        List<ActivityManager.RecentTaskInfo> recents = getRecentTasks();
        boolean onlyHomes = true;
        for (RecentTaskInfo recent : recents) {
             if (!recent.baseIntent.hasCategory(Intent.CATEGORY_HOME))
                 onlyHomes = false;
        }
        if (firstRun && onlyHomes && appWidgetIds != null) {
            for (int widgetId : appWidgetIds) {
                views = new RemoteViewsWidget(context, widgetId, 0, 0, 
                        R.layout.widget_no_recents);
                views.updateWidget();
                AppWidgetManager.getInstance(context.getApplicationContext()).
                        updateAppWidget(widgetId, views);
            }
        }
        SparseArray<RemoteViewsWidget> viewsList = 
            new SparseArray<RemoteViewsWidget>();
        for (int widgetId : updateList) {
            viewsList.put(widgetId, getWidgetRemoteViews(widgetId));
            for (int childId : getChildIds(widgetId)) {
                viewsList.put(childId, getWidgetRemoteViews(childId));
            }
        }
        RemoteViewsWidget progressViews = new RemoteViewsWidget(context, 0, 0, 
                0, R.layout.widget_progress);
        for (int i = 0; i < viewsList.size(); i++) {
            AppWidgetManager.getInstance(context.getApplicationContext()).
                    updateAppWidget(viewsList.keyAt(i), progressViews);
        }
        if (sleep) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {}
        }
        for (int i = 0; i < viewsList.size(); i++) {
            AppWidgetManager.getInstance(context.getApplicationContext()).
                    updateAppWidget(viewsList.keyAt(i), viewsList.valueAt(i));
        }
    }
    /**
     * Updates the database with current contexts.  Only updates bookmarks and
     * contacts if they have changed since the last time.
     * @param currTime  the time of the device in milliseconds
     * @param oldTime   the time of the device in milliseconds when it was last 
     *                  updated
     * @param stack collection of recent apps by the user on the device
     */
    private static void updateContexts(long currTime, long oldTime, 
            Stack<String> stack) {
        addContexts(APPS_FILENAME, readJsonFile(APPS_FILENAME), currTime, 
                oldTime, stack);
        updateContacts();
        addContexts(BOOKMARKS_FILENAME, readJsonFile(BOOKMARKS_FILENAME), 
                currTime, oldTime, stack);
    }
    /**
     * Get current context and pick apps to display based on the context and
     * the database.
     * <p>It filters out any apps that are uninstalled, bookmarks that are 
     * erased, contacts that are gone.  It then sorts each context by the 
     * multiplier that comes from the SQL query.
     * @param widget    the ID of the widget fetching recent apps for
     * @return  list of items, sorted to be shown in widget
     */
    public static ArrayList<String> getRelevantApps(int widget) {
        // Get database contexts
        // Construct UNION statment based on what is chosen in config
        // (apps/bookmarks/contacts)
        Cursor cur = ApplicationEx.dbHelper.getRelevantContexts(
                getGroupSize(widget), getTypes(
                        getPrefFromWidget(TYPE_FILENAME, widget)));
        HashMap<String,Integer> appsHash = 
            new HashMap<String,Integer>(cur.getCount());
        HashMap<String,Integer> relevantHash = 
            new HashMap<String,Integer>(cur.getCount());
        HashMap<String,Integer> durationHash = 
            new HashMap<String,Integer>(cur.getCount());
        Log.d(Constants.LOG_TAG, "-------------cursor size:" + cur.getCount() + 
                "-------------");
        String toAdd;
        int id = -1;
        Cursor tempCur;
        Bitmap toFile;
        if (cur.moveToFirst()) {
            do {
                // Get app package
                if (cur.getInt(0) != 0) {
                    toAdd = ApplicationEx.dbHelper.getRecordName(cur.getInt(0), 
                            DatabaseHelper.COL_APP_ID, DatabaseHelper.APP_TABLE, 
                            DatabaseHelper.COL_PACKAGE);
                    String activity = ApplicationEx.dbHelper.getRecordName(
                            cur.getInt(0), DatabaseHelper.COL_APP_ID, 
                            DatabaseHelper.APP_TABLE, 
                            DatabaseHelper.COL_ACTIVITY);
                    if (!appInstalled(toAdd) || !activityExported(toAdd, 
                            activity)) {
                        id = ApplicationEx.dbHelper.getRecordId(activity, 
                                DatabaseHelper.COL_APP_ID, 
                                DatabaseHelper.APP_TABLE, 
                                DatabaseHelper.COL_ACTIVITY);
                        ApplicationEx.dbHelper.deleteRecord(activity, 
                                DatabaseHelper.APP_TABLE, 
                                DatabaseHelper.COL_ACTIVITY);
                        id = ApplicationEx.dbHelper.getCOL_CONTEXT_ID(id,
                                DatabaseHelper.COL_CONTEXT_ID, 
                                DatabaseHelper.CONTEXT_TABLE, 
                                DatabaseHelper.COL_APP_ID);
                        ApplicationEx.dbHelper.deleteContext(id, 
                                DatabaseHelper.COL_APP_ID);
                        cur.moveToNext();
                        continue;
                    }
                    toFile = getIcon(activity, false);
                    writePngToFile(toFile, activity, false);
                    durationHash.put(activity, cur.getInt(6));
                    relevantHash.put(activity, cur.getInt(3));
                    appsHash.put(activity, cur.getInt(3) + cur.getInt(6));
                }
                if (cur.getInt(1) != 0) {
                    toAdd = ApplicationEx.dbHelper.getRecordName(cur.getInt(1), 
                            DatabaseHelper.COL_BOOK_ID, 
                            DatabaseHelper.BOOKMARK_TABLE, 
                            DatabaseHelper.COL_ADDRESS);
                    // Remove bookmark from bookmark and context table
                    tempCur = context.getContentResolver().query(
                            Browser.BOOKMARKS_URI, null, 
                            BookmarkColumns.URL + "='" + toAdd + "'", null, 
                            null);
                    if (tempCur != null && !tempCur.moveToFirst()) {
                        id = ApplicationEx.dbHelper.getRecordId(toAdd, 
                                DatabaseHelper.COL_BOOK_ID,
                                DatabaseHelper.BOOKMARK_TABLE, 
                                DatabaseHelper.COL_ADDRESS);
                        ApplicationEx.dbHelper.deleteRecord(toAdd, 
                                DatabaseHelper.BOOKMARK_TABLE, 
                                DatabaseHelper.COL_ADDRESS);
                        id = ApplicationEx.dbHelper.getCOL_CONTEXT_ID(id,
                                DatabaseHelper.COL_CONTEXT_ID, 
                                DatabaseHelper.CONTEXT_TABLE, 
                                DatabaseHelper.COL_BOOK_ID);
                        ApplicationEx.dbHelper.deleteContext(id, 
                                DatabaseHelper.COL_BOOK_ID);
                        tempCur.close();
                        cur.moveToNext();
                        continue;
                    }
                    tempCur.close();
                    toFile = getIcon(toAdd, false);
                    writePngToFile(toFile, toAdd, false);
                    relevantHash.put(toAdd, cur.getInt(4));
                    appsHash.put(toAdd, cur.getInt(4));
                }
                if (cur.getInt(2) != 0) {
                    toAdd = ApplicationEx.dbHelper.getRecordName(cur.getInt(2), 
                            DatabaseHelper.COL_CONTACT_ID, 
                            DatabaseHelper.CONTACT_TABLE, 
                            DatabaseHelper.COL_LOOKUP);
                    // Remove contact from contact and context tables
                    tempCur = getContactLog(toAdd);
                    if (tempCur != null && !tempCur.moveToFirst()) {
                        id = ApplicationEx.dbHelper.getRecordId(toAdd, 
                                DatabaseHelper.COL_CONTACT_ID,
                                DatabaseHelper.CONTACT_TABLE, 
                                DatabaseHelper.COL_LOOKUP);
                        ApplicationEx.dbHelper.deleteRecord(toAdd, 
                                DatabaseHelper.CONTACT_TABLE, 
                                DatabaseHelper.COL_LOOKUP);
                        id = ApplicationEx.dbHelper.getCOL_CONTEXT_ID(id,
                                DatabaseHelper.COL_CONTEXT_ID, 
                                DatabaseHelper.CONTEXT_TABLE, 
                                DatabaseHelper.COL_CONTACT_ID);
                        ApplicationEx.dbHelper.deleteContext(id, 
                                DatabaseHelper.COL_CONTACT_ID);
                        tempCur.close();
                        cur.moveToNext();
                        continue;
                    }
                    tempCur.close();
                    toFile = getIcon(toAdd, true);
                    writePngToFile(toFile, toAdd, true);
                    relevantHash.put(toAdd, cur.getInt(5));
                    appsHash.put(toAdd, cur.getInt(5));
                }
                cur.moveToNext();
            } while (!cur.isAfterLast());
        }
        cur.close();
        
        writeLog(sort(durationHash, true), widget, DURATION_FILENAME + widget + 
                ".txt");
        writeLog(sort(relevantHash, true), widget, LOG_FILENAME + widget + 
                ".txt");
        return sortToStrings(sort(appsHash, true));
        
    }
    /**
     * Convert numeric value of types to boolean values.
     * @param type  the numeric representation of type for a widget
     * @return  a hash of the types for this numeric type, in boolean values
     */
    private static HashMap<String, Boolean> getTypes(int type) {
        HashMap<String, Boolean> types = new HashMap<String, Boolean>();
        switch (type)
        {
        case 7:
            types.put("apps", true);
            types.put("bookmarks", true);
            types.put("contacts", true);
            break;
        case 6:
            types.put("apps", true);
            types.put("bookmarks", true);
            types.put("contacts", false);
            break;
        case 5:
            types.put("apps", true);
            types.put("bookmarks", false);
            types.put("contacts", true);
            break;
        case 4:
            types.put("apps", true);
            types.put("bookmarks", false);
            types.put("contacts", false);
            break;
        case 3:
            types.put("apps", false);
            types.put("bookmarks", true);
            types.put("contacts", true);
            break;
        case 2:
            types.put("apps", false);
            types.put("bookmarks", true);
            types.put("contacts", false);
            break;
        case 1:
            types.put("apps", false);
            types.put("bookmarks", false);
            types.put("contacts", true);
            break;
        default:
            types.put("apps", true);
            types.put("bookmarks", true);
            types.put("contacts", true);
            break;
        }
        return types;
    }
    /**
     * Sort a hash of items against their weight determined by the SQL query.
     * @param hash  collection to be sorted
     * @param descending    true if the sort is descending, false otherwise
     * @return  a list of the hash entries, sorted
     * @see java.util.HashMap
     */
    @SuppressWarnings("rawtypes")
    private static List<HashMap.Entry> sort(HashMap<String,Integer> hash,
            final boolean descending) {
        List<HashMap.Entry> list = new ArrayList<HashMap.Entry>(
                hash.entrySet());
        Collections.sort(list, new Comparator<HashMap.Entry>() {
            public int compare(HashMap.Entry e1, HashMap.Entry e2) {
                Integer i1 = (Integer) e1.getValue();
                Integer i2 = (Integer) e2.getValue();
                if (descending)
                    return i2.compareTo(i1);
                else
                    return i1.compareTo(i2);
            }
        });
        return list;
    }
    /**
     * Sort a hash of items against their long value, ascending only.
     * @param hash  collection to be sorted
     * @return  a list of the hash entries, sorted
     * @see java.util.HashMap
     */
    @SuppressWarnings("rawtypes")
    private static List<HashMap.Entry> sort(HashMap<String,Long> hash) {
        List<HashMap.Entry> list = new ArrayList<HashMap.Entry>(
                hash.entrySet());
        Collections.sort(list, new Comparator<HashMap.Entry>() {
            public int compare(HashMap.Entry e1, HashMap.Entry e2) {
                Long i1 = (Long) e1.getValue();
                Long i2 = (Long) e2.getValue();
                return i1.compareTo(i2);
            }
        });
        return list;
    }
    /**
     * Convert a sorted list of hash items to a list of strings.  Currently
     * limiting the "max score" to 320, which is 5 "perfect scores", where the
     * context is true for all 7 ranges.
     * @param list  collection to be convered
     * @return  a list of just the strings
     * @see java.util.HashMap
     */
    @SuppressWarnings("rawtypes")
    private static ArrayList<String> sortToStrings(List<HashMap.Entry> list) {
        ArrayList<String> listStrings = new ArrayList<String>();
        for(HashMap.Entry entry : list) {
            listStrings.add((String)entry.getKey());
        }
        return listStrings;
    }
    /**
     * Write log of each shortcut with the value associated with it (the context
     * or duration value).
     * @param list  the list of shortcuts with their values in a hash
     * @param widget    the widget ID associated with this list
     * @param filename  where the write the log
     */
    @SuppressWarnings("rawtypes")
    private static void writeLog(List<HashMap.Entry> list, int widget, 
            String filename) {
        ArrayList<String> listStrings = new ArrayList<String>();
        String output = "";
        for(HashMap.Entry entry : list) {
            output = output.concat(entry.getKey()+":"+entry.getValue()+"\n");
        }
        writeBufferToFile(output.getBytes(), filename);
    }
    /**
     * Check if a specific app is installed on the device.
     * @param uri   description of the app installed
     * @return  true if installed, false otherwise
     */
    public static boolean appInstalled(String uri) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(uri, 
                    PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {return false;}
    }
    /**
     * Checks if an activity is exported so it can by launched by another app.
     * @param packageName   package of the activity to check
     * @param activityName  activity to check
     * @return  whether or not it is exported
     */
    private static boolean activityExported(String packageName, 
            String activityName) {
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(packageName, 
                    PackageManager.GET_ACTIVITIES);
            if (pInfo.activities != null) {
                for (ActivityInfo activity : pInfo.activities) {
                    if (activity.name.contains(activityName))
                        return activity.exported;
                }
            }
        } catch (NameNotFoundException e) {return false;}
        return false;
    }
    /**
     * Get an item's icon.
     * @param identifier    the name of the item to find the icon for
     * @param smallIcons    true if the images need to be small
     * @return  the icon for the item
     * @see android.graphics.Bitmap
     */
    public static Bitmap getIcon(String identifier, boolean isContact) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 1;
        String newId = "";
        if (identifier.contains("/"))
            newId = identifier.replaceAll("/", "");
        else
            newId = identifier;
        Bitmap fromFile = null;
        try {
            fromFile = BitmapFactory.decodeFile(new File(context.getFilesDir(), 
                    newId+".png").getAbsolutePath(), opts);
        } catch (NullPointerException e) {}
        if (fromFile != null && !isContact)
            return fromFile;
        Bitmap icon = null;
        Bitmap noIcon = ((BitmapDrawable)context.getResources().getDrawable(
                R.drawable.noicon)).getBitmap();
        if (ApplicationEx.dbHelper.inDb(identifier, DatabaseHelper.APP_TABLE, 
                DatabaseHelper.COL_ACTIVITY))
            icon = getAppIcon(identifier);
        if (ApplicationEx.dbHelper.inDb(identifier, 
                DatabaseHelper.BOOKMARK_TABLE, DatabaseHelper.COL_ADDRESS)) {
            byte[] taskBytes = ApplicationEx.dbHelper.getBytes(identifier, 
                    DatabaseHelper.COL_BOOK_ICON, DatabaseHelper.COL_ADDRESS, 
                    DatabaseHelper.BOOKMARK_TABLE);
            if (taskBytes != null) {
                Parcel parcel = Parcel.obtain();
                parcel.unmarshall(taskBytes, 0, taskBytes.length);
                parcel.setDataPosition(0);
                icon = (Bitmap) parcel.readValue(Bitmap.class.getClassLoader());
            }
        }
        if (ApplicationEx.dbHelper.inDb(identifier, DatabaseHelper.CONTACT_TABLE, 
                DatabaseHelper.COL_LOOKUP))
            icon = getContactIcon(identifier);
        if (icon != null)
            return icon;
        else
            return noIcon;
    }
    /**
     * Get an app's icon.
     * @param activityName  the name of the app to find the icon for
     * @return  the icon for the app
     * @see android.graphics.Bitmap
     */
    public static Bitmap getAppIcon(String activityName) {
        ActivityManager.RecentTaskInfo task = getAppTaskInfo(activityName);
        if (task == null)
            return null;
        Intent intent = null;
        if (task.baseIntent != null)
            intent = task.baseIntent;
        else {
            String packageName = ApplicationEx.dbHelper.getString(activityName, 
                    DatabaseHelper.COL_PACKAGE, DatabaseHelper.APP_TABLE, 
                    DatabaseHelper.COL_ACTIVITY);
            intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setClassName(packageName, activityName);
        }
        if (task.origActivity != null) intent.setComponent(task.origActivity);
        intent.setFlags(
                (intent.getFlags() & ~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        Bitmap icon = ((BitmapDrawable)context.getPackageManager()
                .resolveActivity(intent, 0).activityInfo.loadIcon(
                context.getPackageManager())).getBitmap();
        if (icon != null)
            return icon;
        else {
            try {
                return ((BitmapDrawable) context.getPackageManager()
                        .getActivityIcon(intent)).getBitmap();
            } catch (NameNotFoundException e) {
                Log.e(Constants.LOG_TAG, "Unable to find activity " + 
                        activityName + " to retrieve icon", e);
                return null;
            }
        }
    }
    /**
     * Get a bookmark's icon.
     * @param title the name of the bookmark page
     * @return  the icon for the bookmark
     * @see android.graphics.Bitmap
     */
    public static Bitmap getBookmarkIcon(String title) {
        if (title == null || title.equals("")) {
            return null;
        }
        StringBuilder sb = new StringBuilder(BookmarkColumns.TITLE + " = ");
        DatabaseUtils.appendEscapedSQLString(sb, title);
        cur = context.getContentResolver().query(
                Browser.BOOKMARKS_URI, 
                new String[] {BookmarkColumns.FAVICON},
                sb.toString(), null, null);
        if (cur == null)
            return null;     
        if (cur.getCount() <= 0)
            return null;
        try {
            if (cur.moveToFirst()) {
                byte[] data = cur.getBlob(cur.getColumnIndex(
                        BookmarkColumns.FAVICON));
                if (data != null) {
                    return combineImages(BitmapFactory.decodeResource(
                            context.getResources(),
                            R.drawable.bookmark_background), 
                        BitmapFactory.decodeStream(
                            new ByteArrayInputStream(data)));
                }
                else {
                    return BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.bookmark_background);
                }
            }     
        } finally {
            cur.close();
        }
        return null;
    }
    /**
     * Combine the bookmark favicon with the bookmark icon background.
     * @param background    bitmap for the background
     * @param icon  bitmap for the favicon; the foreground
     * @return  the resulting bitmap
     * @see android.graphics.Bitmap
     */
    private static Bitmap combineImages(Bitmap background, Bitmap icon) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = ((WindowManager) context.getSystemService(
                Context.WINDOW_SERVICE));
        windowManager.getDefaultDisplay().getMetrics(dm);
        final float pixelDensity = dm.density;
        
        float backLeft = Math.round(14 * pixelDensity);
        float backTop = Math.round(11 * pixelDensity);
        float iconLeft = Math.round(16 * pixelDensity);
        float iconTop = Math.round(13 * pixelDensity);
        int iconSize = Math.round(16 * pixelDensity);
        
        int width = background.getWidth(); 
        int height = background.getHeight(); 
     
        Bitmap cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); 
        Canvas comboImage = new Canvas(cs); 
        
        Paint paint = new Paint();
        paint.setAntiAlias(false);
        paint.setColor(context.getResources().getColor(android.R.color.white));
        
        comboImage.drawBitmap(background, 0, 0, null);
        Bitmap foreground =  BitmapFactory.decodeResource(context.getResources(), 
                R.drawable.favicon_background);
        if (foreground != null)
            comboImage.drawBitmap(foreground, backLeft, backTop, null);
        comboImage.drawBitmap(Bitmap.createScaledBitmap(icon, iconSize, 
                    iconSize, true), 
                iconLeft, iconTop, null); 
     
        return cs;
      } 
    /**
     * Get an app's task info.
     * @param activityName  the activity name of the app
     * @return  the task info object
     * @see android.app.ActivityManager.RecentTaskInfo
     */
    public static RecentTaskInfo getAppTaskInfo(String activityName) {
        if (!ApplicationEx.dbHelper.inDb(activityName, DatabaseHelper.APP_TABLE, 
                DatabaseHelper.COL_ACTIVITY)) {
            return null;
        }
        byte[] taskBytes = ApplicationEx.dbHelper.getBytes(activityName, 
                    DatabaseHelper.COL_TASK, DatabaseHelper.COL_ACTIVITY, 
                    DatabaseHelper.APP_TABLE);
        if (taskBytes != null) {
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(taskBytes, 0, taskBytes.length);
            parcel.setDataPosition(0);
            RecentTaskInfo task = null;
            try {
                task = (RecentTaskInfo) parcel.
                        readValue(RecentTaskInfo.class.getClassLoader());
            } catch (RuntimeException e) {
                task = new ActivityManager.RecentTaskInfo();
            }
            return task;
        }
        else
            return null;
    }
    /**
     * Get an item's intent.
     * @param identifier    the string that represents an item to get the intent
     *                      for (name, address, lookupkey)
     * @return  intent for the item
     * @see android.content.Intent
     */
    public static Intent getIntent(String identifier) {
        Intent intent;
        intent = getAppIntent(identifier);
        if (intent == null)
            intent = getBookmarkIntent(identifier);
        return intent;
    }
    /**
     * Get the intent of an app from the RecentTaskInfo.
     * @param activityName  the app name
     * @return  the intent with the filtered action and flags, null if not found
     * @see android.content.Intent
     */
    public static Intent getAppIntent(String activityName) {
        ActivityManager.RecentTaskInfo task = getAppTaskInfo(activityName);
        ActivityManager am = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        List<ActivityManager.RecentTaskInfo> recentTasks = am.getRecentTasks(
                MAX_RECENT_TASKS, 0);
        ListIterator<ActivityManager.RecentTaskInfo> iterator = recentTasks.
                listIterator();
        ActivityManager.RecentTaskInfo recentTask;
        if (task == null)
            return null;
        Intent intent = null;
        if (task.baseIntent != null)
            intent = task.baseIntent;
        else {
            String packageName = ApplicationEx.dbHelper.getString(activityName, 
                    DatabaseHelper.COL_PACKAGE, DatabaseHelper.APP_TABLE, 
                    DatabaseHelper.COL_ACTIVITY);
            intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setClassName(packageName, activityName);
        }
        PackageManager pm = context.getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, 
                PackageManager.MATCH_DEFAULT_ONLY);
        while (iterator.hasNext()) {
            recentTask = iterator.next();
            resolveInfo = pm.resolveActivity(recentTask.baseIntent, 
                    PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null && resolveInfo.activityInfo.name.
                    equalsIgnoreCase(activityName)) {
                intent = recentTask.baseIntent;
                break;
            }
        }
        if (task.origActivity != null) intent.setComponent(task.origActivity);
        Intent newIntent = null;
        if (intent.getAction() == null)
            newIntent = pm.getLaunchIntentForPackage(
                    intent.getComponent().getPackageName());
        if (newIntent != null)
            intent = newIntent;
        intent.setFlags(
                (intent.getFlags() | 
                        Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY));
        if (intent.getAction() != null) { 
            if (intent.getAction().equals(Intent.ACTION_SEARCH) ||
                 (intent.getAction().equals(Intent.ACTION_VIEW) &&
                         intent.getDataString() != null && 
                         intent.getDataString().contains("http"))) {
                newIntent = new Intent();
                newIntent.setAction(Intent.ACTION_MAIN);
                newIntent.setComponent(intent.getComponent());
                intent = newIntent;
            }
            if (intent.getAction().equals(Intent.ACTION_MAIN) && 
                    intent.getCategories() != null &&
                    !intent.getCategories().isEmpty() &&
                    !intent.getCategories().contains(
                            "com.android.settings.SHORTCUT")) {
                Set<String> categories = new HashSet<String>(
                        intent.getCategories());
                for (String category : categories) {
                    intent.removeCategory(category);
                }
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
            }
        }
        return intent;
    }
    /**
     * Get the intent of a bookmark.
     * @param address   the bookmark address
     * @return  the intent with the filtered action and flags, null if not found
     * @see android.content.Intent
     */
    public static Intent getBookmarkIntent(String address) {
        if (address == null)
            return null;
        if (ApplicationEx.dbHelper.inDb(address, DatabaseHelper.BOOKMARK_TABLE, 
                DatabaseHelper.COL_ADDRESS)) {
            Uri uri = Uri.parse(address);
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }
        return null;
    }
    /**
     * Gets the contact log for all or a specified contact, sorted by time last 
     * contacted.
     * @param lookup    the contact lookup key, for one contact (can be null)
     * @return  a cursor pointing to the log for one contact or all
     * @see android.database.Cursor
     */
    public static Cursor getContactLog(String lookup) {
        final ContentResolver resolver = context.getContentResolver();
        if (lookup != null)
            return resolver.query(Contacts.CONTENT_URI, null, 
                    Contacts.LOOKUP_KEY + "='" + lookup + "'", null, 
                    "last_time_contacted ASC");
        else
            return resolver.query(Contacts.CONTENT_URI, null, 
                    null, null, "last_time_contacted ASC");
    }
    /**
     * Get the name of a contact from a lookup key.
     * @param lookup    the contact lookup key
     * @return  the contact's display name
     */
    public static String getContactName(String lookup) {
        Uri uri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookup);
        cur = context.getContentResolver().query(uri, 
                    new String[] {Contacts.DISPLAY_NAME}, null, null, null);
        if (cur == null) {
            cur.close();
            return null;     
        }
        if (cur.moveToFirst()) {
            String name = cur.getString(cur.getColumnIndex(
                    Contacts.DISPLAY_NAME));
            cur.close();
            return name;     
        }     
        cur.close();
        return null;
    }
    /**
     * Get the last time a contact is contacted.
     * @param lookup    the contact lookup key
     * @return  last time contacted, in milliseconds, -1 if not found
     */
    public static long getContactTime(String lookup) {
        Uri uri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookup);
        cur = context.getContentResolver().query(uri, 
                    new String[] {Contacts.LAST_TIME_CONTACTED}, null, null, 
                    null);
        if (cur == null) {
            cur.close();
            return -1;     
        }
        if (cur.moveToFirst()) {
            long time = cur.getLong(cur.getColumnIndex(
                    Contacts.LAST_TIME_CONTACTED));
            cur.close();
            return time;       
        }     
        cur.close();
        return -1;
    }
    /**
     * Get a contact's icon.
     * @param lookup    the lookup key for the contact
     * @return  the icon for the contact
     * @see android.graphics.Bitmap
     */
    public static Bitmap getContactIcon(String lookup) {
        Uri uri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookup);
        InputStream input;
        Bitmap photoBitmap = null;
        String[] projection = new String[] { Contacts.PHOTO_ID };
        Cursor cursor = context.getContentResolver().query(
                uri, projection, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                final String photoId = cursor.getString(cursor.getColumnIndex(
                        Contacts.PHOTO_ID));
                if (photoId != null) {
                    final Cursor photo = context.getContentResolver().query(
                            Data.CONTENT_URI, new String[] {Photo.PHOTO},
                            Data._ID + "=?", new String[] {photoId}, null);
                    // Convert photo blob to a bitmap
                    if (photo.moveToFirst()) {
                        byte[] photoBlob = photo.getBlob(photo.getColumnIndex(
                                Photo.PHOTO));
                        photoBitmap = BitmapFactory.decodeByteArray(photoBlob, 0, 
                                photoBlob.length);
                    }
                    photo.close();
                }
            }
            cursor.close();
        } 
        if (photoBitmap != null)
            return photoBitmap;
        else
            return BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_contact_picture);
    }
    /**
     * Get the contacts from the device if the last time it was updated is more
     * recent than the persisted time when a new context was added.  We don't
     * want to keep calling the contacts interface on the device if there is
     * nothing new to get.
     */
    public static void updateContacts() {
        Cursor cursor = getContactLog(null);
        long time;
        long latest = 0;
        String timeString = readStringFromFile(LAST_TIME_CONTACTED);
        if (cursor != null) {
            if (cursor.moveToLast()) {
                do {
                    time = cursor.getLong(cursor.getColumnIndex(
                                    Contacts.LAST_TIME_CONTACTED));
                    if (timeString.equals("")) {
                        writeBufferToFile(Long.toString(time).getBytes(), 
                                LAST_TIME_CONTACTED);
                        break;
                    }
                    try {
                        if (Long.parseLong(timeString) < time) {
                            if (time > latest)
                                latest = time;
                            addContact(cursor.getString(cursor.getColumnIndex(
                                    Contacts.LOOKUP_KEY)));
                        }
                        else {
                            if (latest > Long.parseLong(timeString))
                                writeBufferToFile(Long.toString(latest).getBytes(), 
                                        LAST_TIME_CONTACTED);
                            break;
                        }
                    } catch (NumberFormatException e) {
                        Log.e(Constants.LOG_TAG, "Number not found from " +
                                timeString, e);
                    }
                } while (cursor.moveToPrevious());
            }
            cursor.close();
        }
    }
    /**
     * Convert a long from a file.
     * @param filename  which file to get the long from
     * @return  long value from the file
     */
    public static long getLongFromFile(String filename) {
        String timeString = readStringFromFile(filename);
        try {
            return timeString.equals("") ? -1 : Long.parseLong(timeString);
        } catch (NumberFormatException e) {
            Log.e(Constants.LOG_TAG, "Number not found from " + timeString, e);
        }
        return -1;
    }
    /**
     * Write a long to a file.
     * @param filename  where to write the long
     * @param value the long to write
     */
    public static void writeLongToFile(String filename, long value) {
        writeBufferToFile(String.valueOf(value).getBytes(), LAST_TIME_UPDATED);
    }
    /**
     * Gets the recent bookmarks from the system and adds them to a stack.
     * @return  stack of strings; the list of recent bookmarks
     */
    @SuppressWarnings("rawtypes")
    public static Stack<String> addRecentBookmarksToStack() {
        Stack<String> bookmarkStack = new Stack<String>();
        cur = getRecentBookmarks();
        HashMap<String,Long> bookmarkHash = new HashMap<String,Long>();
        if (cur != null && cur.moveToFirst()) {
            do {
                bookmarkHash.put(cur.getString(cur.getColumnIndex(
                        BookmarkColumns.URL)),
                            cur.getLong(cur.getColumnIndex(
                        BookmarkColumns.DATE)));
            } while (cur.moveToNext());
            List<HashMap.Entry> sortedHash = sort(bookmarkHash);
            ListIterator<HashMap.Entry> listIter = sortedHash.listIterator();
            String bookmark = "";
            while (listIter.hasNext()) {
                bookmark = (String) listIter.next().getKey();
                bookmarkStack.push(bookmark);
            }
        }
        cur.close();
        return bookmarkStack;
    }
    /**
     * Gets the of recent bookmarks from the system, that have been visited.
     * @return  a cursor pointed to all bookmarks that have been visited
     * @see android.database.Cursor
     */
    public static Cursor getRecentBookmarks() {
        String lastUpdated = readStringFromFile(LAST_TIME_UPDATED);
        if (lastUpdated.equals("")) lastUpdated = "-1";
        return context.getContentResolver().query(
                Browser.BOOKMARKS_URI, 
                new String[] {BookmarkColumns.BOOKMARK,
                              BookmarkColumns.URL, 
                              BookmarkColumns.DATE}, 
                BookmarkColumns.VISITS + " > 0 AND " + 
                BookmarkColumns.DATE + " > " + lastUpdated, null, null);
    }
    /**
     * Gets the title (page title) of a bookmark from its address.
     * @param address   URL of the bookmark to be found
     * @return  string representation of the page's title
     */
    public static String getBookmarkTitle(String address) {
        cur = context.getContentResolver().query(
                Browser.BOOKMARKS_URI, 
                new String[] {BookmarkColumns.TITLE}, 
                BookmarkColumns.URL + " = '" + address + "'", null, null);
        String title = "";
        if (cur != null) {
            if (cur.moveToFirst())
                title = cur.getString(cur.getColumnIndex(BookmarkColumns.TITLE));
            cur.close();
        }
        return title;
    }

}