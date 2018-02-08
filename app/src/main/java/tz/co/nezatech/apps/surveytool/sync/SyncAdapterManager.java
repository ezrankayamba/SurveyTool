package tz.co.nezatech.apps.surveytool.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import tz.co.nezatech.apps.surveytool.R;

import static android.content.Context.ACCOUNT_SERVICE;

/**
 * Created by nkayamba on 2/7/18.
 */

public class SyncAdapterManager {
    private static final String TAG = SyncAdapterManager.class.getName();
    private final String authority;
    private final String type;

    private Account account;
    private Context context;

    public SyncAdapterManager(final Context context) {
        this.context = context;

        type = context.getString(R.string.account_type);
        authority = context.getString(R.string.authority);
        account = new Account(context.getString(R.string.app_name), type);
    }

    public void beginPeriodicSync(final long updateConfigInterval) {
        Log.d(TAG, "beginPeriodicSync() called with: updateConfigInterval = [" +
                updateConfigInterval + "]");

        final AccountManager accountManager = (AccountManager) context
                .getSystemService(ACCOUNT_SERVICE);

        if (!accountManager.addAccountExplicitly(account, null, null)) {
            account = accountManager.getAccountsByType(type)[0];
        }

        setAccountSyncable();

        ContentResolver.addPeriodicSync(account, context.getString(R.string.authority),
                Bundle.EMPTY, updateConfigInterval);

        ContentResolver.setSyncAutomatically(account, authority, true);
    }

    private void setAccountSyncable() {
        if (ContentResolver.getIsSyncable(account, authority) == 0) {
            ContentResolver.setIsSyncable(account, authority, 1);
        }
    }
}
