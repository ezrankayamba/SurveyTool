package tz.co.nezatech.apps.surveytool.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = SyncAdapter.class.getName();
    private DatabaseHelper databaseHelper = null;
    private Context ctx;
    private ContentResolver contentResolver;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        ctx = context;
        contentResolver = context.getContentResolver();
    }

    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        ctx = context;
        contentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account,
                              Bundle extras,
                              String authority,
                              ContentProviderClient provider,
                              SyncResult syncResult) {
        Log.d(TAG, "onPerformSync for account[" + account.name + "]");
        try {
            SyncHttpUtil util = new SyncHttpUtil(ctx);

            util.syncLocalToServer(getHelper().getFormInstanceDao());
            util.syncSetups(getHelper().getSetupDao());
            util.syncDataTypes(getHelper().getDataTypeDao());
            util.syncLocalFromServer(getHelper().getFormInstanceDao());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(ctx, DatabaseHelper.class);
        }
        return databaseHelper;
    }
}