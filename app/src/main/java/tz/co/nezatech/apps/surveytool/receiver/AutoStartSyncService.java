package tz.co.nezatech.apps.surveytool.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import tz.co.nezatech.apps.surveytool.sync.SimpleHttpSyncService;

public class AutoStartSyncService extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, SimpleHttpSyncService.class));
    }
}
