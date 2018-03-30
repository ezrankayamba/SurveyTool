package tz.co.nezatech.apps.surveytool.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.j256.ormlite.dao.Dao;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
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

import tz.co.nezatech.apps.surveytool.db.model.DataType;
import tz.co.nezatech.apps.surveytool.db.model.FormInstance;
import tz.co.nezatech.apps.surveytool.db.model.Setup;
import tz.co.nezatech.apps.surveytool.form.SetupQuery;
import tz.co.nezatech.apps.surveytool.util.HttpUtil;

/**
 * Created by nkayamba on 3/25/18.
 */

public class SyncHttpUtil {
    final String TAG = this.getClass().getName();
    private Context ctx;

    public SyncHttpUtil(Context ctx) {
        this.ctx = ctx;
    }

    public void syncLocalToServer(Dao<FormInstance, Integer> formInstanceDao) {
        try {
            List<FormInstance> notSynced = formInstanceDao.queryForEq("status", 0);
            for (FormInstance fi : notSynced) {
                try {
                    Gson g = new Gson();
                    String json = g.toJson(fi);
                    Log.d(TAG, "Payload: " + json);
                    String body = httpPost(HttpUtil.FORMS_SYNC_PATH, json);
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

    public void syncLocalFromServer(Dao<FormInstance, Integer> formInstanceDao) {
        try {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            SetupQuery q = new SetupQuery();
            q.setType("All");
            String lu = sharedPrefs.getString("form_survey_forms_last_update", "2018-01-01 00:00:00");
            //lu="2018-01-01 00:00:00";//hardcoded
            q.setLastUpdate(lu);

            Gson g = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create();
            String json = g.toJson(q);
            Log.d(TAG, "Payload: " + json);

            String body = httpPost(HttpUtil.FORMS_DOWNLOAD_SYNC_PATH, json);
            Log.d(TAG, "Response: " + body);
            JSONArray resp = new JSONArray(body);
            String lastUpdate = lu;
            try {

                for (int i = 0; i < resp.length(); i++) {
                    JSONObject o = resp.getJSONObject(i);
                    try {
                        FormInstance s = g.fromJson(o.toString(), FormInstance.class);
                        Log.d(TAG, s.toString());
                        List<FormInstance> tmp = formInstanceDao.queryForEq("uuid", s.getUuid());
                        if (tmp.isEmpty()) {
                            formInstanceDao.create(s);
                        } else {
                            s.setId(tmp.get(0).getId());
                            formInstanceDao.update(s);
                        }
                        if (s.getLastUpdate() != null) {
                            lastUpdate = s.getLastUpdate();
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());
                        e.printStackTrace();
                        break;
                    }
                }
                Log.d(TAG, "New last update: " + lastUpdate);
                SharedPreferences.Editor edit = sharedPrefs.edit();
                edit.putString("form_survey_forms_last_update", lastUpdate);
                edit.commit();
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void syncSetups(Dao<Setup, String> setupDao) {
        try {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            SetupQuery q = new SetupQuery();
            q.setType("All");
            String lu = sharedPrefs.getString("form_setups_last_update", "2018-01-01 00:00:00");
            //lu="2018-01-01 00:00:00";//hardcoded
            q.setLastUpdate(lu);

            Gson g = new Gson();
            String json = g.toJson(q);
            Log.d(TAG, "Payload: " + json);

            String body = httpPost(HttpUtil.FORMS_SETUPS_SYNC_PATH, json);
            Log.d(TAG, "Response: " + body);
            JSONArray resp = new JSONArray(body);
            String lastUpdate = lu;
            try {

                for (int i = 0; i < resp.length(); i++) {
                    JSONObject o = resp.getJSONObject(i);
                    try {
                        Setup s = g.fromJson(o.toString(), Setup.class);
                        Log.d(TAG, s.toString());
                        Setup tmp = setupDao.queryForId(s.getUuid());
                        if (tmp == null) {
                            setupDao.create(s);
                        } else {
                            setupDao.update(s);
                        }
                        lastUpdate = s.getLastUpdate();
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());
                        e.printStackTrace();
                        break;
                    }
                }
                Log.d(TAG, "New last update: " + lastUpdate);
                SharedPreferences.Editor edit = sharedPrefs.edit();
                edit.putString("form_setups_last_update", lastUpdate);
                edit.commit();
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void syncDataTypes(Dao<DataType, String> dataTypeDao) {
        try {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            SetupQuery q = new SetupQuery();
            q.setType("All");
            String lu = sharedPrefs.getString("form_datatypes_last_update", "2018-01-01 00:00:00");
            //lu="2018-01-01 00:00:00";//hardcoded
            q.setLastUpdate(lu);

            Gson g = new Gson();
            String json = g.toJson(q);
            Log.d(TAG, "Payload: " + json);

            String body = httpPost(HttpUtil.FORMS_DATATYPES_SYNC_PATH, json);
            Log.d(TAG, "Response: " + body);
            JSONArray resp = new JSONArray(body);
            String lastUpdate = lu;
            try {

                for (int i = 0; i < resp.length(); i++) {
                    JSONObject o = resp.getJSONObject(i);
                    try {
                        DataType s = g.fromJson(o.toString(), DataType.class);
                        Log.d(TAG, s.toString());
                        DataType tmp = dataTypeDao.queryForId(s.getName());
                        if (tmp == null) {
                            dataTypeDao.create(s);
                        } else {
                            dataTypeDao.update(s);
                        }
                        lastUpdate = s.getLastUpdate();
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());
                        e.printStackTrace();
                        break;
                    }
                }
                Log.d(TAG, "New last update: " + lastUpdate);
                SharedPreferences.Editor edit = sharedPrefs.edit();
                edit.putString("form_datatypes_last_update", lastUpdate);
                edit.commit();
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String httpPost(String path, String json) {
        try {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            String username = sharedPrefs.getString("user_username", "anonymous");
            String password = sharedPrefs.getString("user_password", "anonymous");
            //fi.setForm(null);

            Gson g = new Gson();
            //String json = g.toJson(fi);
            Log.d(TAG, String.format("Path: %s, Payload: %s", path, json));
            OutputStream os = null;
            InputStream is = null;
            String result = null;
            HttpURLConnection http = null;
            try {
                String basicAuth2 = "Basic "
                        + Base64.encodeToString(String.format("%s:%s", username, password).getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);

                URL url = new URL(HttpUtil.FORMS_BASE_URL + path);
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
                    String cl = http.getHeaderField("Content-Length");
                    int len = cl == null ? 10000000 : Integer.parseInt(cl);
                    Log.d(TAG, "Content-Length: " + len);
                    //len=len*2;
                    //result = readStream(is, len);
                    result = readInputStreamToString(is);
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

            return body;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    private String readInputStreamToString(InputStream stream) {
        String result = null;
        StringBuffer sb = new StringBuffer();
        InputStream is = null;

        try {
            is = new BufferedInputStream(stream);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String inputLine = "";
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
            }
            result = sb.toString();
        } catch (Exception e) {
            Log.i(TAG, "Error reading InputStream");
            result = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.i(TAG, "Error closing InputStream");
                }
            }
        }
        return result;
    }
}
