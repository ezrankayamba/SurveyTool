package tz.co.nezatech.apps.surveytool.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;
import tz.co.nezatech.apps.surveytool.db.model.FormInstance;
import tz.co.nezatech.apps.surveytool.util.HttpUtil;
import tz.co.nezatech.dev.nezahttp.HttpClient;
import tz.co.nezatech.dev.nezahttp.Response;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = SyncAdapter.class.getName();
    private DatabaseHelper databaseHelper = null;
    private Context ctx;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        ctx = context;
    }

    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        ctx = context;
    }

    @Override
    public void onPerformSync(Account account,
                              Bundle extras,
                              String authority,
                              ContentProviderClient provider,
                              SyncResult syncResult) {
        Log.d(TAG, "onPerformSync for account[" + account.name + "]");
        try {
            syncLocalToServer();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void syncLocalToServer() {
        try {
            Dao<FormInstance, Integer> formInstanceDao = getHelper().getFormInstanceDao();
            List<FormInstance> notSynced = formInstanceDao.queryForEq("status", 0);
            for (FormInstance fi : notSynced) {
                try {
                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);

                    HttpClient client = new HttpClient(HttpUtil.FORMS_BASE_URL + HttpUtil.FORMS_SYNC_PATH);
                    String username = sharedPrefs.getString("user_username", "anonymous");
                    String password = sharedPrefs.getString("user_password", "anonymous");
                    String basicAuth = "Authorization: Basic "
                            + Base64.encodeToString(String.format("%s:%s", username, password).getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);

                    client.setBasicAuth(basicAuth);
                    client.connect();

                    Gson g = new Gson();
                    String json = g.toJson(fi);

                    Response response = client.post(json, "application/json");
                    String body = response.getBody();

                    JSONObject resp = new JSONObject(body);
                    boolean success = resp.getBoolean("success");

                    if (success) {
                        fi.setStatus(1);
                        try {
                            formInstanceDao.update(fi);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
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