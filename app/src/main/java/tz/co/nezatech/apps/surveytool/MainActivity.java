package tz.co.nezatech.apps.surveytool;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.dao.RawRowMapper;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;
import tz.co.nezatech.apps.surveytool.db.model.DataType;
import tz.co.nezatech.apps.surveytool.db.model.Form;
import tz.co.nezatech.apps.surveytool.form.FormInstanceActivity;
import tz.co.nezatech.apps.surveytool.location.LocationActivity;
import tz.co.nezatech.apps.surveytool.summary.SummaryActivity;
import tz.co.nezatech.apps.surveytool.sync.AsyncHttpTask;
import tz.co.nezatech.apps.surveytool.sync.AsyncTaskListener;
import tz.co.nezatech.apps.surveytool.sync.SimpleHttpSyncService;
import tz.co.nezatech.apps.surveytool.util.FormUtil;
import tz.co.nezatech.apps.surveytool.util.HttpUtil;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final int ENABLE_GPS_RESULT_CODE = 60;
    // Sync interval constants

    final String TAG = MainActivity.class.getSimpleName();
    LayoutInflater layoutInflater = null;
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
                syncFormRepos(fab);
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

        startService(new Intent(this, SimpleHttpSyncService.class));
    }

    private void syncFormRepos(FloatingActionButton fab) {
        AsyncHttpTask httpTask = new AsyncHttpTask(fab, MainActivity.this, new AsyncTaskListener() {
            @Override
            public void done(View view, boolean success) {
                if (success) {
                    Snackbar.make(view, "Fetching forms is completed successfully", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    loadForms();
                } else {
                    Snackbar.make(view, "Fetching forms failed. Check your credentials or call customer support.", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }

            @Override
            public boolean processResponse(String body) {
                try {
                    JSONArray list = new JSONArray(body);
                    Dao<Form, Integer> formDao = getHelper().getFormDao();
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject jForm = list.getJSONObject(i);
                        Log.d(MainActivity.class.getName(), String.format("Form: %s", jForm.getString("name")));
                        Form form = new Form(jForm.getInt("id"), jForm.getString("name"), jForm.getString("description"), jForm.getString("json"), jForm.getString("display"));

                        QueryBuilder<Form, Integer> qb = formDao.queryBuilder();
                        qb.where().eq("form_id", form.getFormId());
                        List<Form> existing = qb.query();
                        if (existing.isEmpty()) {
                            formDao.create(form);
                        } else {
                            form.setId(existing.get(0).getId());
                            formDao.update(form);
                        }
                    }
                    return true;

                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                    e.printStackTrace();
                }
                return false;
            }
        }, HttpUtil.FORMS_SYNC_PATH);
        httpTask.execute();
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
            GenericRawResults<String[]> fiSummary = getHelper().getFormInstanceDao().queryRaw(
                    "SELECT COUNT(*) as myCount, fi.form_id, f.form_id as theFormId FROM tbl_form_instance fi left join tbl_form f on fi.form_id=f.id where 1=1 group by fi.form_id, f.form_id",
                    new RawRowMapper<String[]>() {
                        public String[] mapRow(String[] columnNames, String[] resultColumns) {
                            return new String[]{resultColumns[0], resultColumns[1], resultColumns[2]};
                        }
                    });
            List<String[]> results = fiSummary.getResults();
            for (String[] row : results) {
                Log.d(TAG, String.format(Locale.ENGLISH, "%10s | %10s | %10s", row[0], row[1], row[2]));
            }



            for (final Form form : forms) {
                Log.d(TAG, String.format(Locale.ENGLISH, "loadForms:=> %s", form));
                LinearLayout btnLayout = (LinearLayout) layoutInflater.inflate(R.layout.survey_dashboard_item, linearLayout, false);
                TextView tvTitle = (TextView) btnLayout.findViewById(R.id.dashboardItemTitle);
                tvTitle.setText(form.getName());
                TextView tvDescription = (TextView) btnLayout.findViewById(R.id.dashboardItemDescription);
                tvDescription.setText(form.getDescription());

                Button button = (Button) btnLayout.findViewById(R.id.btnRunSurvey);
                //button.setText(form.getDescription());
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent mngForms = new Intent(MainActivity.this, FormInstanceActivity.class);
                        mngForms.putExtra(FormUtil.FORM_REPOS_DATA, form);
                        startActivity(mngForms);
                    }
                });
                GenericRawResults<Integer> rawResults = getHelper().getFormInstanceDao().queryRaw(
                        "SELECT COUNT(*) as myCount FROM tbl_form_instance fi left join tbl_form f on fi.form_id=f.id where f.form_id=" + form.getFormId(),
                        new RawRowMapper<Integer>() {
                            public Integer mapRow(String[] columnNames, String[] resultColumns) {
                                return Integer.parseInt(resultColumns[0]);
                            }
                        });
                int count = rawResults.getFirstResult();
                button.setText(String.format(Locale.ENGLISH, "%d", count));
                linearLayout.addView(btnLayout);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_user_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_manage) {
            Log.d(TAG, String.format(Locale.ENGLISH, "Manage nav: %s", "Not implemented yet!"));
        } else if (id == R.id.nav_summary) {
            Intent intent = new Intent(this, SummaryActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_dummy_records) {
            Log.d(TAG, "Adding dummy");
        } else if (id == R.id.nav_dummy_records_remove) {
            Log.d(TAG, "Removing dummy");
            try {
                Dao<DataType, String> dataTypeDao = getHelper().getDataTypeDao();
                DeleteBuilder<DataType, String> db = dataTypeDao.deleteBuilder();
                db.where().eq("name", "Others");
                db.delete();

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
            gpsEnabled = lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (!gpsEnabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setMessage(context.getResources().getString(R.string.gps_network_not_enabled));
            final LinkedHashMap<String, Boolean> action = new LinkedHashMap<>();
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
}
