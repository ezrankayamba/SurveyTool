package tz.co.nezatech.apps.surveytool.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import tz.co.nezatech.apps.surveytool.R;

/**
 * Created by nkayamba on 2/7/18.
 */

public class SyncAdapterManager {
    private static final String TAG = SyncAdapterManager.class.getName();
    private static final String SYNC_ACCOUNT = "MySyncAccount";

    public SyncAdapterManager() {
    }

    @SuppressWarnings("MissingPermission")
    public static void beginPeriodicSync(Context context, long syncFrequency) {
        Log.d(TAG, "Configure sync adopter with frequency: " + syncFrequency + "]");
        String type = context.getString(R.string.account_type);
        String authority = context.getString(R.string.authority);
        Account account = new Account(SYNC_ACCOUNT, type);

        final AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        if (!accountManager.addAccountExplicitly(account, null, null)) {
            account = accountManager.getAccountsByType(type)[0];
            ContentResolver.setIsSyncable(account, authority, 1);
            Log.d(TAG, "New account added: " + account.name);
        } else {
            ContentResolver.setIsSyncable(account, authority, 1);
            Log.d(TAG, "Account exist already: " + account.name);
        }

        ContentResolver.setSyncAutomatically(account, authority, true);
        ContentResolver.addPeriodicSync(account, context.getString(R.string.authority),
                Bundle.EMPTY, syncFrequency);
        Log.d(TAG, "Periodic sync enabled: " + syncFrequency);
    }
}
