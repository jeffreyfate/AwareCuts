package com.jeffthefate.awarecuts_dev.receiver;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.jeffthefate.awarecuts_dev.common.Constants;
import com.jeffthefate.awarecuts_dev.widget.RefreshService;
/**
 * Service that runs in the background.  Registers receivers for actions that
 * the app will respond to.  Also, handles starting the widget updates.
 * 
 * @author Jeff
 */
public class ReceiverService extends Service {
    /**
     * The WakeReceiver object
     */
    EventReceiver eReceiver;
    /**
     * The RotateReceiver object
     */
    RotateReceiver rReceiver;
    
    @Override
    public void onCreate() {
        Log.d(Constants.LOG_TAG, "Creating service!");
        rReceiver = new RotateReceiver();
        IntentFilter intentFilter = new IntentFilter(
                Intent.ACTION_CONFIGURATION_CHANGED);
        registerReceiver(rReceiver, intentFilter);
        // Register wake receiver
        eReceiver = new EventReceiver();
        intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        intentFilter.addAction(Intent.ACTION_CAMERA_BUTTON);
        intentFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(eReceiver, intentFilter);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.LOG_TAG, "Starting service!");
        int[] appWidgetIds = null;
        Intent newIntent = new Intent(getApplicationContext(), 
                RefreshService.class);
        if (intent != null && intent.getExtras() != null && 
                intent.getExtras().containsKey("appWidgetIds")) {
            appWidgetIds = intent.getExtras().getIntArray("appWidgetIds");
            newIntent.putExtra("appWidgetIds", appWidgetIds);
        }
        newIntent.putExtra("start", true);
        getApplicationContext().startService(newIntent);
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Log.d(Constants.LOG_TAG, "Destroying service!");
        this.unregisterReceiver(eReceiver);
        this.unregisterReceiver(rReceiver);
    }
    
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
