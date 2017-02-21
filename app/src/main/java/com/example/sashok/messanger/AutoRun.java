package com.example.sashok.messanger;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;

/**
 * Created by sasho on 07.02.2017.
 */

public class AutoRun extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
           SharedPreferences mSettings = context.getSharedPreferences(context.getString(R.string.preference_file_key), context.MODE_PRIVATE);
            Boolean isSaved=mSettings.getBoolean(context.getString(R.string.SAVE_KEY),false);
            if(isSaved) {
                Intent intent1 = new Intent(context, FoneService.class);
                context.startService(intent1);
            }
        }
    }

}
