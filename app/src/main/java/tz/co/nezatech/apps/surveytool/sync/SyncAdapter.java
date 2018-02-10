package tz.co.nezatech.apps.surveytool.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;
import tz.co.nezatech.apps.surveytool.db.model.FormInstance;
import tz.co.nezatech.apps.surveytool.util.HttpUtil;

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

                    String username = sharedPrefs.getString("user_username", "anonymous");
                    String password = sharedPrefs.getString("user_password", "anonymous");
                    fi.setForm(null);

                    Gson g = new Gson();
                    String json = g.toJson(fi);
                    Log.d(TAG, "Payload: " + json);
                    OutputStream os = null;
                    InputStream is = null;
                    String result = null;
                    HttpURLConnection http = null;
                    try {
                        String basicAuth2 = "Basic "
                                + Base64.encodeToString(String.format("%s:%s", username, password).getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);

                        URL url = new URL(HttpUtil.FORMS_BASE_URL + HttpUtil.FORMS_SYNC_PATH);
                        http = (HttpURLConnection) url.openConnection();
                        http.setReadTimeout(3000);
                        http.setConnectTimeout(3000);
                        http.setRequestMethod("POST");
                        http.setRequestProperty("Authorization", basicAuth2);
                        http.setRequestProperty("Content-Type", "application/json");
                        http.setDoInput(true);
                        http.setDoOutput(true);
                        os = new BufferedOutputStream(http.getOutputStream());
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                        writer.write(json);
                        writer.flush();
                        writer.close();
                        os.close();
                        http.connect();
                        int responseCode = http.getResponseCode();
                        if (responseCode != HttpsURLConnection.HTTP_OK) {
                            throw new IOException("HTTP error code: " + responseCode);
                        }
                        is = http.getInputStream();
                        if (is != null) {
                            result = readStream(is, 500);
                        }
                    } finally {
                        // Close Stream and disconnect HTTPS connection.
                        if (is != null) {
                            is.close();
                        }
                        if (http != null) {
                            http.disconnect();
                        }
                    }

                    //Response response = client.post(json, "application/json");
                    String body = result;
                    Log.d(TAG, "Response: " + body);
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

    private String readStream(InputStream stream, int maxReadSize)
            throws IOException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] rawBuffer = new char[maxReadSize];
        int readSize;
        StringBuffer buffer = new StringBuffer();
        while (((readSize = reader.read(rawBuffer)) != -1) && maxReadSize > 0) {
            if (readSize > maxReadSize) {
                readSize = maxReadSize;
            }
            buffer.append(rawBuffer, 0, readSize);
            maxReadSize -= readSize;
        }
        return buffer.toString();
    }

    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(ctx, DatabaseHelper.class);
        }
        return databaseHelper;
    }
}