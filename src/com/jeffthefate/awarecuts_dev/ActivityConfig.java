package com.jeffthefate.awarecuts_dev;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.jeffthefate.awarecuts_dev.common.Constants;
import com.jeffthefate.awarecuts_dev.common.Util;
import com.jeffthefate.awarecuts_dev.preference.ButtonPreference;
import com.jeffthefate.awarecuts_dev.widget.RefreshService;

/**
 * <p>Configures a new widget.  Saves shared preferences that contain all items
 * in the settings.
 * <p>For the first widget in a widget group, choose None for the last widget
 * size.  For each subsequent widget, choose what size the previous widget in
 * the group was.  Then, chose what size the new widget will be.  The small icon
 * sizes will fit 4 icons in the space of 1 normal size widget.
 * <p>Advanced Options
 * <p>The user can decide what elements they want to show in the widget:
 * <ul>
 * <li>Applications
 * <li>Bookmarks
 * <li>Contacts
 * 
 * @author Jeff Fate
 */
public class ActivityConfig extends PreferenceActivity implements
        OnSharedPreferenceChangeListener, OnClickListener {
    // The id for the widget that is currently being configured
    private int mAppWidgetId;
    // Size of the previous widget in the group
    private int mLastSize = 0;
    // Size of the new/current widget in the group
    private int mNewSize = 4;
    // Identifiers for each dialog in the first use instructions
    private static final int DIALOG_0 = 0;
    private static final int DIALOG_1 = 1;
    private static final int DIALOG_2 = 2;
    // Always start with the first dialog
    private int mCurrDialog = DIALOG_0;
    
    AlertDialog.Builder mBuilder;
    AlertDialog mAlert;
    LayoutInflater mInflater;
    
    TextView mDialogText;
    ImageView mDialogImage;
    CheckBox mDialogCheckBox;
    
    Context mContext = this;
    
    Bundle mExtras;
    
    int mType = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExtras = getIntent().getExtras();
        setResult(RESULT_CANCELED);
        // Get the widget id this is configuring
        if (mExtras != null) {
            mAppWidgetId = mExtras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
                finish();
        }
        getPreferences(MODE_PRIVATE).
                registerOnSharedPreferenceChangeListener(this);
        this.addPreferencesFromResource(R.xml.settings);
        final CheckBoxPreference mAppTypePref = 
            (CheckBoxPreference) findPreference(getString(R.string.apps_key));
        final CheckBoxPreference mBookTypePref = 
            (CheckBoxPreference) findPreference(
                    getString(R.string.bookmarks_key));
        final CheckBoxPreference mContactTypePref = 
            (CheckBoxPreference) findPreference(
                    getString(R.string.contacts_key));
        final CheckBoxPreference mAdvPref = 
            (CheckBoxPreference) findPreference(getString(R.string.adv_key));
        final PreferenceCategory mAdvCat = (PreferenceCategory) findPreference(
                getString(R.string.adv_cat_key));
        final ListPreference mLastListPref = (ListPreference) findPreference(
                getString(R.string.row_key));
        mLastListPref.setSummary(mLastListPref.getEntry());
        if (!mLastListPref.getValue().equals(
                mLastListPref.getEntryValues()[0])) {
            mAdvPref.setEnabled(false);
            mAdvCat.setEnabled(false);
        }
        else {
            mAdvPref.setEnabled(true);
            mAdvCat.setEnabled(true);
        }
        mLastListPref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, 
                    Object newValue) {
                if (!newValue.equals(mLastListPref.getEntryValues()[0])) {
                    CharSequence[] values = mLastListPref.getEntryValues();
                    ArrayList<Integer> widgets = new ArrayList<Integer>();
                    for (int i = 0; i < values.length; i++) {
                        if (values[i].equals(newValue)) {
                            mLastListPref.setValueIndex(i);
                            mLastListPref.setSummary(
                                    mLastListPref.getEntries()[i]);
                            widgets = Util.getOtherWidgets(mAppWidgetId, 
                                    Integer.valueOf(mLastListPref.getValue()));
                            widgets.remove(widgets.size()-1);
                            break;
                        }
                    }
                    int mTemp = 0;
                    if (widgets.size() > 1) {
                        for (int widgetId : widgets) {
                            mTemp = Util.getPrefFromWidget(Util.TYPE_FILENAME, 
                                    widgetId);
                            if (mTemp != mType && mTemp != 0) {
                                mType = mTemp;
                                if (mType == 7)
                                    mAdvPref.setChecked(false);
                                else
                                    mAdvPref.setChecked(true);
                                mAdvPref.setEnabled(false);
                            }
                        }
                        if (mType == (mType & 4))
                            mAppTypePref.setChecked(true);
                        else
                            mAppTypePref.setChecked(false);
                        mAppTypePref.setEnabled(false);
                        if (mType == (mType & 2))
                            mBookTypePref.setChecked(true);
                        else
                            mBookTypePref.setChecked(false);
                        mBookTypePref.setEnabled(false);
                        if (mType == (mType & 1))
                            mContactTypePref.setChecked(true);
                        else
                            mContactTypePref.setChecked(false);
                        mContactTypePref.setEnabled(false);
                    }
                    else
                        mAdvPref.setEnabled(true);
                }
                else {
                    mLastListPref.setSummary(mLastListPref.getEntries()[0]);
                    mAdvPref.setEnabled(true);
                    mAdvPref.setEnabled(true);
                }
                return true;
            }
        });
        final ListPreference mNewListPref = (ListPreference) findPreference(
                getString(R.string.icon_key));
        mNewListPref.setSummary(mNewListPref.getEntry());
        mNewListPref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, 
                    Object newValue) {
                CharSequence[] values = mNewListPref.getEntryValues();
                for (int i = 0; i < values.length; i++) {
                    if (values[i].equals(newValue)) {
                        mNewListPref.setValueIndex(i);
                        mNewListPref.setSummary(mNewListPref.getEntries()[i]);
                        break;
                    }
                }
                return true;
            }
        });
        mAdvPref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, 
                    Object newValue) {
                if ((Boolean)newValue) {
                    mAdvPref.setEnabled(true);
                    mAppTypePref.setChecked(true);
                    mBookTypePref.setChecked(true);
                    mContactTypePref.setChecked(true);
                }
                else
                    mAdvPref.setEnabled(false);
                return true;
            }
        });
        Button mDoneButton = ((ButtonPreference) findPreference(
                getString(R.string.button_key))).getButton();
        mDoneButton.setOnClickListener(this);
        mDoneButton.setText(R.string.SettingsButton);
        if (!Util.appInstalled("com.jeffthefate.awarecutspro")) {
            Button mDonateButton = ((ButtonPreference) findPreference(
                    getString(R.string.donate_key))).getButton();
            mDonateButton.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(
                           "market://details?id=com.jeffthefate.awarecutspro"));
                    startActivity(intent);
                }
            });
            mDonateButton.setText(R.string.DonateButton);
        }
        else {
            boolean mRemoved = ((PreferenceScreen) findPreference(
                    getString(R.string.recent_settings))).removePreference(
                            findPreference(getString(R.string.donate_key)));
            if (!mRemoved)
                Log.e(Constants.LOG_TAG, "preference not removed!");
        }
        Util.persist(Util.BOOKMARKS_FILENAME, Util.addRecentBookmarksToStack());
        File mFile = new File(this.getFilesDir(), Util.ICONS_FILENAME);
        if (!mFile.exists() || 
                    Util.readStringFromFile(Util.ICONS_FILENAME).equals("")) {
            mLastListPref.setEnabled(false);
            mLastListPref.setValue(getResources().getStringArray(
                    R.array.returnValueRow)[0]);
            mLastListPref.setSummary(getResources().getStringArray(
                    R.array.displayWordRow)[0]);
            mAdvPref.setEnabled(true);
        }
        mFile = new File(this.getFilesDir(), Util.FIRST_USE_FILENAME);
        if (!mFile.exists()) {
            mInflater = (LayoutInflater) this.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            mBuilder = new AlertDialog.Builder(this);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
                mBuilder.setPositiveButton("Next", new NextListener())
                .setNegativeButton("Dismiss", new CancelListener())
                .setView(mInflater.inflate(R.layout.dialog, null));
            }
            else if (Build.VERSION.SDK_INT <= 
                Build.VERSION_CODES.GINGERBREAD_MR1) {
                mBuilder.setPositiveButton("Dismiss", new CancelListener())
                .setNegativeButton("Next", new NextListener())
                .setView(mInflater.inflate(R.layout.dialog, null));

            }
            mAlert = mBuilder.create();
            mAlert.show();
            mDialogText = (TextView) mAlert.findViewById(R.id.DialogText);
            mDialogText.setText(R.string.first_use_1);
            mDialogCheckBox = (CheckBox) mAlert.findViewById(
                    R.id.DialogCheckBox);
            mDialogCheckBox.setChecked(true);
        }
    }    
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, 
            String key) {}

    public void onClick(View arg0) {
        update();
    }
    /**
     * Finishes configuration of the widget and starts associated services.
     * <p>
     * Updates the widget views for the first time and also creates two 
     * receivers to catch device wake events and screen off events.  Finally, 
     * it starts the alarm to check for app changes periodically, which is only 
     * done until the screen off event fires.
    */
    private void update() {
        mLastSize = Integer.valueOf(((ListPreference) findPreference(
                getString(R.string.row_key))).getValue());
        mNewSize = Integer.valueOf(((ListPreference) findPreference(
                getString(R.string.icon_key))).getValue());
        ArrayList<Integer> mWidgets = Util.getOtherWidgets(mAppWidgetId,
                mLastSize);
        String mGroupString = "";
        int temp = mWidgets.get(mWidgets.size()-1);
        mWidgets.remove(mWidgets.size()-1);
        for (int mWidget : mWidgets) {
            if (mWidget != mAppWidgetId && mWidget != -1) {
                mGroupString += mWidget;
                mGroupString += ";";
            }
        }
        // Write app/bookmark/contact preferences to file(s)
        mType = 0;
        if (((CheckBoxPreference) findPreference(
                getString(R.string.apps_key))).isChecked())
            mType = mType | 4;
        if (((CheckBoxPreference) findPreference(
                getString(R.string.bookmarks_key))).isChecked())
            mType = mType | 2;
        if (((CheckBoxPreference) findPreference(
                getString(R.string.contacts_key))).isChecked())
            mType = mType | 1;
        if (!((CheckBoxPreference) findPreference(
                getString(R.string.adv_key))).isChecked()) {
            mType = 7;
        }
        String mTypeText = Util.readStringFromFile(Util.TYPE_FILENAME);
        if (!mTypeText.contains(mAppWidgetId + "," + mType + "~"))
            mTypeText = mTypeText + mAppWidgetId + "," + mType + "~";
        Util.writeBufferToFile((mTypeText).getBytes(), Util.TYPE_FILENAME);
        // Write the current widget to file
        Util.writeBufferToFile((temp + ":" + mGroupString).getBytes(), 
                Util.WIDGET_FILENAME + mAppWidgetId + ".txt");
        // Update the icons file to include the current widget
        String mIconsText = Util.readStringFromFile(Util.ICONS_FILENAME);
        if (!mIconsText.contains(mAppWidgetId + "," + mNewSize + "~"))
            mIconsText = mIconsText + mAppWidgetId + "," + mNewSize + "~";
        Util.writeBufferToFile((mIconsText).getBytes(), Util.ICONS_FILENAME);
        // Update the largest widget file to include this widget, if necessary
        Util.setLargestWidget();
        // Done with config; refresh all widgets
        setResult(RESULT_OK, new Intent().putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
        Intent intent = new Intent(mContext, RefreshService.class);
        intent.putExtra("config", true);
        mContext.startService(intent);
            
        finish();
    }
    /**
     * Listener for the Dismiss button on the OOBE dialog.  Dismisses the dialog
     * after the user is done with it and doesn't want to proceed.
     * @author Jeff
     */
    protected class CancelListener implements DialogInterface.OnClickListener {         
        public void onClick(DialogInterface dialog, int which) {
            if (!mDialogCheckBox.isChecked())
                Util.writeBufferToFile(new byte[0], Util.FIRST_USE_FILENAME);
            dialog.dismiss();
        }
    }
    /**
     * Listener for the Next button on the OOBE dialog.  Shows the next set of
     * instructions.
     * @author Jeff
     */
    protected class NextListener implements DialogInterface.OnClickListener {         
        public void onClick(DialogInterface dialog, int which) {
            // TODO Auto-generated method stub
            mCurrDialog++;
            mBuilder = new AlertDialog.Builder(mContext);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
                mBuilder = mBuilder.setIcon(R.drawable.ic_launcher)
                .setPositiveButton(getNextButton(), getListener())
                .setView(mInflater.inflate(R.layout.dialog, null));
            }
            else if (Build.VERSION.SDK_INT <= 
                Build.VERSION_CODES.GINGERBREAD_MR1) {
                mBuilder = mBuilder.setIcon(R.drawable.ic_launcher)
                .setNegativeButton(getNextButton(), getListener())
                .setView(mInflater.inflate(R.layout.dialog, null));
            }
            if (mCurrDialog < DIALOG_2) {
                if (Build.VERSION.SDK_INT > 
                Build.VERSION_CODES.GINGERBREAD_MR1) {
                    mBuilder.setNegativeButton("Dismiss", new CancelListener());
                }
                else if (Build.VERSION.SDK_INT <= 
                    Build.VERSION_CODES.GINGERBREAD_MR1) {
                    mBuilder.setPositiveButton("Dismiss", new CancelListener());
                }
            }
            if (!mDialogCheckBox.isChecked())
                Util.writeBufferToFile(new byte[0], Util.FIRST_USE_FILENAME);
            dialog.dismiss();
            mAlert = mBuilder.create();
            mAlert.show();
            mDialogText = (TextView) mAlert.findViewById(R.id.DialogText);
            mDialogText.setText(getText());
            mDialogImage = (ImageView) mAlert.findViewById(R.id.DialogImage);
            mDialogImage.setImageResource(getImage());
            mDialogCheckBox = (CheckBox) mAlert.findViewById(
                    R.id.DialogCheckBox);
            mDialogCheckBox.setVisibility(View.GONE);
        }
        private int getText() {
            int message = -1;
            switch (mCurrDialog) {
            case DIALOG_0:
                message = R.string.first_use_1;
                break;
            case DIALOG_1:
                message = R.string.first_use_2;
                break;
            case DIALOG_2:
                message = R.string.first_use_3;
                break;
            default:
                break;
            }
            return message;
        }
        private int getImage() {
            int image = -1;
            switch (mCurrDialog) {
            case DIALOG_0:
                image = R.drawable.empty;
                break;
            case DIALOG_1:
                image = R.drawable.oobe_screen_2;
                break;
            case DIALOG_2:
                image = R.drawable.oobe_screen_3;
                break;
            default:
                break;
            }
            return image;
        }
        private android.content.DialogInterface.OnClickListener getListener() {
            if (mCurrDialog < DIALOG_2)
                return new NextListener();
            else
                return new CancelListener();
        }
        private CharSequence getNextButton() {
            if (mCurrDialog < DIALOG_2)
                return "Next";
            else
                return "Done";
        }
    }
    
}