package tz.co.nezatech.apps.surveytool.form;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;

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
    private DatabaseHelper databaseHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_instance);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Intent intent = getIntent();

        form = (Form) intent.getSerializableExtra(FormUtil.FORM_REPOS_DATA);
        toolbar.setTitle(form.getDescription());
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
            List<FormInstance> formInstances = getHelper().getFormInstanceDao().queryBuilder().orderBy("record_date", false).query();
            Log.d(TAG, "ListSize: " + formInstances.size());
            final ListView mListView = (ListView) findViewById(R.id.form_instance_list);

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

            final Filter filter = adapter.getFilter();

            SearchView mSearchView = (SearchView) findViewById(R.id.searchText);
            mSearchView.setIconifiedByDefault(false);
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
            //mSearchView.setSubmitButtonEnabled(true);
            mSearchView.setQueryHint(getString(R.string.list_filter_hint));
            mSearchView.clearFocus();
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

}
