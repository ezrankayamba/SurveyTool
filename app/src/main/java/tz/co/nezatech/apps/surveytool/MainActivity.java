package tz.co.nezatech.apps.surveytool;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.dao.RawRowMapper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;
import tz.co.nezatech.apps.surveytool.db.model.Form;
import tz.co.nezatech.apps.surveytool.form.FormInstanceActivity;
import tz.co.nezatech.apps.surveytool.location.LocationActivity;
import tz.co.nezatech.apps.surveytool.summary.SummaryActivity;
import tz.co.nezatech.apps.surveytool.sync.SyncHttpUtil;
import tz.co.nezatech.apps.surveytool.util.FormUtil;
import tz.co.nezatech.apps.surveytool.util.HttpUtil;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    public static final String AUTHORITY = "tz.co.nezatech.apps.surveytool.sync.provider";
    public static final String ACCOUNT_TYPE = "nezatech.co.tz";
    public static final String ACCOUNT = "dummyaccount";
    private static final int ENABLE_GPS_RESULT_CODE = 60;
    // Sync interval constants

    final String TAG = MainActivity.class.getName();
    LayoutInflater layoutInflater = null;
    Account mAccount;
    private DatabaseHelper databaseHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FetchForms forms = new FetchForms(MainActivity.this, fab);
                forms.execute();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //set values
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        View headerView = navigationView.getHeaderView(0);
        TextView tv = (TextView) headerView.findViewById(R.id.header_user_username);
        tv.setText(settings.getString("user_username", "test"));

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadForms();
    }

    void loadForms() {
        //list layouts.forms
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.forms_list);
        try {
            linearLayout.removeAllViews();

            if (!checkGPS(this)) {
                return;
            }

            Dao<Form, Integer> formDao = getHelper().getFormDao();
            List<Form> forms = formDao.queryForAll();
            for (final Form form : forms) {

                LinearLayout btnLayout = (LinearLayout) layoutInflater.inflate(R.layout.survey_dashboard_item, null);
                TextView tvTitle = (TextView) btnLayout.findViewById(R.id.dashboardItemTitle);
                tvTitle.setText(form.getName());
                TextView tvDescription = (TextView) btnLayout.findViewById(R.id.dashboardItemDescription);
                tvDescription.setText(form.getDescription());
                TextView tvNumberOfInstances = (TextView) btnLayout.findViewById(R.id.dashboardNumOfForms);


                GenericRawResults<Integer> rawResults =
                        getHelper().getFormInstanceDao().queryRaw(
                                "SELECT COUNT(*) as myCount FROM tbl_form_instance where form_id=" + form.getFormId(),
                                new RawRowMapper<Integer>() {
                                    public Integer mapRow(String[] columnNames, String[] resultColumns) {
                                        return Integer.parseInt(resultColumns[0]);
                                    }
                                });
                int count = rawResults.getFirstResult();
                tvNumberOfInstances.setText(String.format("%d", count));

                ImageButton button = (ImageButton) btnLayout.findViewById(R.id.btnRunSurvey);
                //button.setText(form.getDescription());
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent mngForms = new Intent(MainActivity.this, FormInstanceActivity.class);
                        mngForms.putExtra(FormUtil.FORM_REPOS_DATA, form);
                        startActivity(mngForms);
                    }
                });
                linearLayout.addView(btnLayout);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //checkGPS(this);
    }

    void restart() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            finishAndRemoveTask();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_user_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_summary) {
            Intent intent = new Intent(this, SummaryActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_dummy_records) {
            Log.d(TAG, "Adding dummy");
            int limit = 10;
            SummaryActivity.dummyRecords(getHelper(), limit);
        } else if (id == R.id.nav_dummy_records_remove) {
            Log.d(TAG, "Removing dummy");
            SummaryActivity.dummyRecordsRemove(getHelper());
            try {
                getHelper().getDataTypeDao().delete(getHelper().getDataTypeDao().queryForAll());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else if (id == R.id.nav_gps_check) {
            Intent intent = new Intent(this, LocationActivity.class);
            startActivity(intent);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(MainActivity.this, DatabaseHelper.class);
        }
        return databaseHelper;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
    }

    boolean checkGPS(final Activity context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gpsEnabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setMessage(context.getResources().getString(R.string.gps_network_not_enabled));
            final Map<String, Boolean> action = new LinkedHashMap();
            action.put("positive", false);
            dialog.setPositiveButton(context.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    action.put("positive", true);
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivityForResult(myIntent, ENABLE_GPS_RESULT_CODE);
                }
            });
            dialog.setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finishAndRemoveTask();
                }
            });
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (!action.get("positive")) {
                        finishAndRemoveTask();
                    }
                }
            });
            dialog.show();
        }

        return gpsEnabled;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    class FetchForms extends AsyncTask<String, Void, Boolean> {
        private ProgressDialog dialog;
        private View view;

        public FetchForms(Activity activity, View view) {
            dialog = new ProgressDialog(activity);
            this.view = view;
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Fetching your forms. Please wait...");
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                String username = sharedPrefs.getString("user_username", "anonymous");
                String password = sharedPrefs.getString("user_password", "anonymous");

                InputStream is = null;
                String result = null;
                HttpURLConnection http = null;
                try {
                    String basicAuth2 = "Basic "
                            + Base64.encodeToString(String.format("%s:%s", username, password).getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);

                    URL url = new URL(HttpUtil.FORMS_BASE_URL + HttpUtil.FORMS_PATH);
                    http = (HttpURLConnection) url.openConnection();
                    http.setReadTimeout(3000);
                    http.setConnectTimeout(3000);
                    http.setRequestMethod("GET");
                    http.setRequestProperty("Authorization", basicAuth2);
                    http.setDoInput(true);
                    http.setDoOutput(false);
                    http.connect();
                    int responseCode = http.getResponseCode();
                    if (responseCode != HttpsURLConnection.HTTP_OK) {
                        throw new IOException("HTTP error code: " + responseCode);
                    }
                    is = http.getInputStream();
                    if (is != null) {
                        result = readStream(is, 100000);
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
                JSONArray list = new JSONArray(body);
                Dao<Form, Integer> formDao = getHelper().getFormDao();
                formDao.executeRawNoArgs("delete from tbl_form");
                try {
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject jForm = list.getJSONObject(i);
                        Log.d(MainActivity.class.getName(), String.format("Form: %s", jForm.getString("name")));
                        Form form = new Form(jForm.getInt("id"), jForm.getString("name"), jForm.getString("description"), jForm.getString("json"), jForm.getString("display"));
                        formDao.createOrUpdate(form);
                    }

                    try {
                        SyncHttpUtil util = new SyncHttpUtil(MainActivity.this);
                        try {
                            util.syncLocalFromServer(getHelper().getFormInstanceDao());
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    } finally {
                        return true;
                    }

                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (success) {
                Snackbar.make(view, "Fetching forms is completed successfully", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                loadForms();
            } else {
                Snackbar.make(view, "Fetching forms failed. Check your credentials or call customer support.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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
    }
}
