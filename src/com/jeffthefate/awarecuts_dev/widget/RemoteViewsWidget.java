package com.jeffthefate.awarecuts_dev.widget;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.jeffthefate.awarecuts_dev.ApplicationEx;
import com.jeffthefate.awarecuts_dev.R;
import com.jeffthefate.awarecuts_dev.common.Constants;
import com.jeffthefate.awarecuts_dev.common.DatabaseHelper;
import com.jeffthefate.awarecuts_dev.common.Util;
/**
 * RemoteView for a widget, which contains the views that display icons, names
 * and contain intents to launch app shortcuts.
 * 
 * @author Jeff
 */
public class RemoteViewsWidget extends RemoteViews {
    /**
     * Local context object
     */
    private final Context context;
    /**
     * Id of the widget associated with this remote views
     */
    private int widgetId;
    /**
     * Whether or not small icons are enabled for this remote view's widget
     */
    private boolean icons = false;
    /**
     * This widget's number of icons, horizontally
     */
    private int numIcons;
    /**
     * This widget's row number, in a larger collection of widgets
     */
    private int rowNum;
    /**
     * Layout resource for this widget
     */
    private int layoutRes;
    /**
     * Setup this remote views with the context, columns, rows, id and icons 
     * values of the widget associated with it.
     * @param context   the context object passed from the caller
     * @param numIcons  the number of horizontal icons in this widget
     * @param rowNum    the row number this widget is in a widget collection
     * @param layoutRes layout res id to be inflated
     * @param widgetId  id of the associated widget
     */
    public RemoteViewsWidget(Context context, int widgetId, int numIcons, 
            int rowNum, int layoutRes) {
        super(context.getPackageName(), layoutRes);
        this.context = context;
        this.numIcons = numIcons;
        this.rowNum = rowNum;
        this.widgetId = widgetId;
        this.layoutRes = layoutRes;
    }
    /**
     * Setup this remote views with the context, columns, rows, id and icons 
     * values of the widget associated with it.
     * @param context   the context object passed from the caller
     * @param layoutRes layout res id to be inflated
     * @param cols  number of columns the associated widget has
     * @param rows  number of rows the associated widget has
     * @param widgetId  id of the associated widget
     */
    private ArrayList<String> jsonArrayToString(JSONArray jsonArray) {
        ArrayList<String> list = new ArrayList<String>();
        if (jsonArray != null) {
            for(int i = 0; i < jsonArray.length(); i++) {
                try {
                    list.add(jsonArray.getString(i));
                } catch (JSONException e) {
                    Log.e(Constants.LOG_TAG, "Value doesn't exist at index " + 
                            i + " for the array " + jsonArray.toString(), e);
                }
            }
        }
        return list;
    }
    /**
     * Updates the views in the widget by first grabbing the persisted list of
     * relevant contexts for this widget, then updates the views individually.
     */
    public void updateWidget() {
        String filename = Util.RELEVANT_FILENAME + widgetId + ".txt";
        if (!Util.findFile(filename)) {
            ArrayList<Integer> widgets = Util.getOtherIds(widgetId);
            if (widgets.size() > 0)
                filename = Util.RELEVANT_FILENAME + widgets.get(0) + ".txt";
        }
        setApp(jsonArrayToString(Util.readJsonFile(filename)));
    }
    /**
     * Does the updating of all the views in this remote views based on a list
     * of relevant contexts.
     * @param list    an ArrayList of relevant contexts' name/address/lookup key
     */
    private void setApp(ArrayList<String> list) {
        int counter = -1;
        int num = 0;
        String layoutText;
        int layoutViewId;
        String iconText;
        int resId;
        int textResId;
        int imageResId;
        int iconNum;
        String identifier;
        String displayName;
        Intent intent;
        Resources resources = context.getResources();
        File file;
        float fontScale = 0f;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            fontScale = Settings.System.getFloat(context.getContentResolver(),
                    Settings.System.FONT_SCALE, 10f);
        if (numIcons > 5 || numIcons == -1) {
            icons = true;
            if (numIcons == -1)
                numIcons = 4;
            numIcons = numIcons / 4;
        }
        for (int i = 0; i < numIcons; i++) {
            counter++;
            num = i+1;
            layoutText = "RelativeLayoutIcon"+num;
            layoutViewId = resources.getIdentifier(
                    layoutText, "id", context.getPackageName());
            if (icons) {
                removeAllViews(layoutViewId);
                RemoteViews remoteView = new RemoteViews(
                        context.getPackageName(), R.layout.widget_small_icons);
                addView(layoutViewId, remoteView);
                for (int k = 1; k <= 4; k++) {
                    iconText = "ImageViewWidget"+k;
                    resId = resources.getIdentifier(iconText, 
                            "id", context.getPackageName());
                    iconNum = 0;
                    if (k < 3)
                        iconNum = rowNum + (2*i)+k;
                    else if (k > 2)
                        iconNum = rowNum + (2*i)+(k-2)+(numIcons*2);
                    if (iconNum-1 < list.size()) {
                        identifier = list.get(iconNum-1);
                        intent = Util.getIntent(identifier);
                        if (identifier != null) {
                            if (identifier.contains("/")) {
                                identifier = identifier.replaceAll("/", "");
                            }
                            String path = context.getFilesDir().
                                getAbsolutePath() + "/" + identifier + ".png";
                            file = new File(path);
                            if (file.exists()) {
                                remoteView.setImageViewUri(resId, Uri.EMPTY);
                                remoteView.setImageViewUri(resId, 
                                        Uri.fromFile(file));
                            }
                            Intent updateIntent = new Intent(context, 
                                    RestartService.class);
                            updateIntent.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (intent != null)
                                updateIntent.putExtra("intent", intent);
                            else {
                                updateIntent.putExtra("contact", identifier);
                            }
                            remoteView.setOnClickPendingIntent(
                                    resId,
                                    PendingIntent.getService(
                                            context, 
                                            (int) ((resId+layoutViewId+widgetId)
                                                   *System.currentTimeMillis()), 
                                            updateIntent, 
                                            PendingIntent.FLAG_UPDATE_CURRENT));
                            remoteView.setViewVisibility(
                                    resId, View.VISIBLE);
                        }
                    }
                    else {
                        remoteView.setImageViewResource(resId, 
                                R.drawable.empty);
                    }
                }
            } else {
                imageResId = resources.getIdentifier("ImageViewWidget"+num, 
                        "id", context.getPackageName());
                textResId = resources.getIdentifier("TextViewWidget"+num, "id", 
                        context.getPackageName());
                if (rowNum+counter < list.size()) {
                    identifier = list.get(rowNum+counter);
                    displayName = ApplicationEx.dbHelper.getString(
                            identifier, DatabaseHelper.COL_NAME, 
                            DatabaseHelper.APP_TABLE, 
                            DatabaseHelper.COL_ACTIVITY);
                    if (displayName == null)
                        displayName = ApplicationEx.dbHelper.getString(
                                identifier, DatabaseHelper.COL_BOOK_TITLE, 
                                DatabaseHelper.BOOKMARK_TABLE, 
                                DatabaseHelper.COL_ADDRESS);
                    if (displayName == null)
                        displayName = ApplicationEx.dbHelper.getString(
                                identifier, DatabaseHelper.COL_CONTACT_NAME, 
                                DatabaseHelper.CONTACT_TABLE, 
                                DatabaseHelper.COL_LOOKUP);
                    if (displayName == null)
                        displayName = "";
                    intent = Util.getIntent(identifier);
                    if (identifier != null) {
                        if (identifier.contains("/")) {
                            identifier = identifier.replaceAll("/", "");
                        }
                        String path = context.getFilesDir().
                            getAbsolutePath() + "/" + identifier + ".png";
                        file = new File(path);
                        if (file.exists()) {
                            setImageViewUri(imageResId, Uri.EMPTY);
                            setImageViewUri(imageResId, 
                                    Uri.fromFile(file));
                        }
                        setViewVisibility(imageResId, View.VISIBLE);
                        setTextViewText(textResId, displayName);
                        if (Build.VERSION.SDK_INT >= 
                            Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                            setFloat(textResId, "setTextSize", 11.5f);
                        setViewVisibility(textResId, View.VISIBLE);
                        Intent updateIntent = new Intent(context, 
                                RestartService.class);
                        updateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (intent != null)
                            updateIntent.putExtra("intent", intent);
                        else {
                            updateIntent.putExtra("contact", identifier);
                        }
                        setOnClickPendingIntent(
                                layoutViewId, 
                                PendingIntent.getService(
                                        context, layoutViewId+widgetId, 
                                        updateIntent, 
                                        PendingIntent.FLAG_UPDATE_CURRENT));
                    }
                }
                else {
                    setImageViewResource(imageResId, R.drawable.empty);
                }
            }
        }
    }

}