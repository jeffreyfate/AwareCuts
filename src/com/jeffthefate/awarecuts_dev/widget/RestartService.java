package com.jeffthefate.awarecuts_dev.widget;

import android.app.IntentService;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.QuickContact;
import android.util.Log;

import com.jeffthefate.awarecuts_dev.ApplicationEx;
import com.jeffthefate.awarecuts_dev.common.Constants;

/**
 * An "interface" to restart the service if it has been killed.  This is run 
 * each time a shortcut is tapped to launch, so each time the user wants to 
 * launch something from the app, the service is sure to be running again if it
 * wasn't before.
 * 
 * @author Jeff Fate
 */
public class RestartService extends IntentService {
    /**
     * Extras
     */
    Bundle extras = null;
    /**
     * Intent to launch the passed app
     */
    Intent launchIntent = null;
    String lookup = null;
    /**
     * Necessary constructor; passing the name of the service
     */
    public RestartService() {
        super("RestartService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        extras = intent.getExtras();
        if (extras != null) {
            launchIntent = extras.getParcelable("intent");
            lookup = extras.getString("contact");
            if (launchIntent != null) {
                String action = "";
                if (launchIntent.getAction() == null)
                    launchIntent.setAction(Intent.ACTION_MAIN);
                launchIntent.setFlags(launchIntent.getFlags() | 
                            Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    ApplicationEx.getApp().startActivity(launchIntent);
                } catch (ActivityNotFoundException e) {
                    Log.e(Constants.LOG_TAG, "Unable to find activity " + 
                            launchIntent.getComponent() + " to retrieve icon", 
                            e);
                }
                this.startService(new Intent(getApplicationContext(), 
                        RefreshService.class));
            }
            else if (lookup != null){
                Rect r = intent.getSourceBounds();
                if (r == null)
                    Log.e(Constants.LOG_TAG, "getSourceBounds for " + 
                            intent.getAction() + ":" + intent.getPackage() + 
                            " is null");
                else
                    QuickContact.showQuickContact(getApplicationContext(), r,
                            Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, 
                                    lookup), 
                            QuickContact.MODE_LARGE, null);
            }
        }
    }
}
