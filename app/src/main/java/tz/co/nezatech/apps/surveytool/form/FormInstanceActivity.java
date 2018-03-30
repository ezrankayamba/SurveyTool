package tz.co.nezatech.apps.surveytool.form;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.stmt.QueryBuilder;

import java.text.SimpleDateFormat;
import java.util.List;

import tz.co.nezatech.apps.surveytool.R;
import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;
import tz.co.nezatech.apps.surveytool.db.model.Form;
import tz.co.nezatech.apps.surveytool.db.model.FormInstance;
import tz.co.nezatech.apps.surveytool.util.FormUtil;
import tz.co.nezatech.apps.surveytool.util.ListAdapter;

public class FormInstanceActivity extends AppCompatActivity {
    final String TAG = FormInstanceActivity.class.getName();
    Form form;
    ListView mListView = null;
    Filter filter = null;
    private DatabaseHelper databaseHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_instance);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Intent intent = getIntent();

        form = (Form) intent.getSerializableExtra(FormUtil.FORM_REPOS_DATA);
        toolbar.setTitle(form.getName());
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FormInstanceActivity.this, FormEditActivity.class);
                intent.putExtra(FormUtil.FORM_REPOS_DATA, form);
                startActivity(intent);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //display the list
        displayFormInstances();
    }

    @Override
    public void onResume() {
        super.onResume();
        displayFormInstances();
    }

    private void displayFormInstances() {
        try {
            QueryBuilder<FormInstance, Integer> qb = getHelper().getFormInstanceDao().queryBuilder();
            qb.where().like("json", "%\"formId\":" + form.getFormId() + "%");
            qb.orderBy("record_date", false);
            List<FormInstance> formInstances = getHelper().getFormInstanceDao().query(qb.prepare());
            for (FormInstance fi : formInstances) {
                Log.d(TAG, String.format("Name: %s, Form: %s", fi.getName(), fi.getForm()));
            }
            mListView = (ListView) findViewById(R.id.form_instance_list);
            ListAdapter<FormInstance> adapter = new ListAdapter<FormInstance>(FormInstanceActivity.this, formInstances) {
                @Override
                protected void handleRowClick(int position, FormInstance fi) {
                    Log.d(TAG, "JSON: " + fi.getJson());
                    Intent intent = new Intent(FormInstanceActivity.this, FormViewActivity.class);
                    intent.putExtra(FormUtil.FORM_INSTANCE_DATA, fi);
                    intent.putExtra(FormUtil.FORM_REPOS_DATA, form);
                    startActivity(intent);
                }

                @Override
                protected void makeRowView(View v, FormInstance p) {
                    TextView txv = (TextView) v.findViewById(R.id.displayText);
                    txv.setText(p.getName());
                    ImageView syncStatusIcon = (ImageView) v.findViewById(R.id.syncStatusIcon);
                    if (p.getStatus() == 0) {
                        syncStatusIcon.setImageResource(R.mipmap.forms_sync_pending);
                    } else {
                        syncStatusIcon.setImageResource(R.mipmap.forms_sync_done);
                    }
                    TextView lastUpdate = (TextView) v.findViewById(R.id.lastUpdate);
                    SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    lastUpdate.setText(String.format("Last updated: %s", df.format(p.getRecordDate())));
                }

                @Override
                protected int getRowLayoutResource() {
                    return R.layout.form_instance_row;
                }
            };
            mListView.setAdapter(adapter);
            mListView.setTextFilterEnabled(false);

            filter = adapter.getFilter();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.form_instance, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search_instances);
        android.support.v7.widget.SearchView mSearchView = (android.support.v7.widget.SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Query submitted: " + query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "Query text changed: " + newText);
                if (TextUtils.isEmpty(newText)) {
                    mListView.clearTextFilter();
                    filter.filter("");
                } else {
                    mListView.setFilterText(newText);
                    filter.filter(newText);
                }
                return true;
            }
        });
        mSearchView.setQueryHint(getString(R.string.list_filter_hint));
        return true;
    }

}
