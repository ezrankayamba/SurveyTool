package apps.nezatech.co.tz.surveytool;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import apps.nezatech.co.tz.surveytool.db.DatabaseHelper;
import apps.nezatech.co.tz.surveytool.db.model.Form;
import apps.nezatech.co.tz.surveytool.form.FormInstanceActivity;
import apps.nezatech.co.tz.surveytool.util.FormUtil;
import apps.nezatech.co.tz.surveytool.util.HttpUtil;
import tz.co.nezatech.dev.nezahttp.HttpClient;
import tz.co.nezatech.dev.nezahttp.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    final String TAG = MainActivity.class.getName();
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
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
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


        loadForms();
    }

    void loadForms() {
        //list layouts.forms
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.forms_list);
        try {
            linearLayout.removeAllViews();

            Dao<Form, Integer> formDao = getHelper().getFormDao();
            List<Form> forms = formDao.queryForAll();
            for (final Form form : forms) {

                LinearLayout btnLayout = (LinearLayout) layoutInflater.inflate(R.layout.form_button, null);

                Button button = (Button) btnLayout.findViewById(R.id.button);
                button.setText(form.getDescription());
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
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

                HttpClient client = new HttpClient(HttpUtil.FORMS_BASE_URL + HttpUtil.FORMS_PATH);
                //client.setBasicAuth("Authorization: Basic c3VydmV5b3IxOjEyMzQ1Ng==");
                String wkng = "Authorization: Basic c3VydmV5b3IxOjEyMzQ1Ng==";
                String username = sharedPrefs.getString("user_username", "anonymous");
                String password = sharedPrefs.getString("user_password", "anonymous");
                String basicAuth = "Authorization: Basic "
                        + Base64.encodeToString(String.format("%s:%s", username, password).getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);

                Log.d(TAG, String.format("Working: %s, Username: %s, Password: %s, New: %s", wkng, username, password, basicAuth));
                client.setBasicAuth(basicAuth);
                client.connect();
                Response response = client.get();
                String body = response.getBody();

                Dao<Form, Integer> formDao = getHelper().getFormDao();
                formDao.executeRawNoArgs("delete from tbl_form");

                Log.d(TAG, "JSON: " + body);

                JSONArray list = new JSONArray(body);
                for (int i = 0; i < list.length(); i++) {
                    JSONObject jForm = list.getJSONObject(i);
                    Log.d(MainActivity.class.getName(), String.format("Form: %s", jForm.getString("name")));
                    Form form = new Form(jForm.getInt("id"), jForm.getString("name"), jForm.getString("description"), jForm.getString("json"), jForm.getString("display"));
                    formDao.createOrUpdate(form);
                }
                return true;
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
    }
}
