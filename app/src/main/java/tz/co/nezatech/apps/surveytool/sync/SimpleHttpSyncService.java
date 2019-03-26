package tz.co.nezatech.apps.surveytool.sync;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;

public class SimpleHttpSyncService extends Service {
    private static final String TAG = SimpleHttpSyncService.class.getSimpleName();
    private static final int SYNC_INTERVAL_SEC = 60 * 5;//Five(5) minutes
    private DatabaseHelper databaseHelper = null;
    private Context ctx;
    // Create the Handler object (on the main thread by default)
    Handler handler = new Handler();
    // Define the code block to be executed
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "Syncing data..." + ctx);
                        SyncHttpUtil util = new SyncHttpUtil(ctx);

                        util.syncLocalToServer(getHelper().getFormInstanceDao());
                        util.syncSetups(getHelper().getSetupDao());
                        util.syncDataTypes(getHelper().getDataTypeDao());
                        util.syncLocalFromServer(getHelper().getFormInstanceDao());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    handler.postDelayed(this, SYNC_INTERVAL_SEC * 1000);
                }
            }).start();
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ctx = this;
        Log.d(TAG, "Service for sync is started");
        // Start the initial runnable task by posting through the handler
        handler.post(runnableCode);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Removes pending code execution
        handler.removeCallbacks(runnableCode);
    }

    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(ctx, DatabaseHelper.class);
        }
        return databaseHelper;
    }
}
