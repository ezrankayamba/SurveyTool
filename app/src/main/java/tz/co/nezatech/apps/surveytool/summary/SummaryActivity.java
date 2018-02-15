package tz.co.nezatech.apps.surveytool.summary;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RawRowMapper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import tz.co.nezatech.apps.surveytool.R;
import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;
import tz.co.nezatech.apps.surveytool.db.model.Form;
import tz.co.nezatech.apps.surveytool.db.model.FormInstance;
import tz.co.nezatech.apps.surveytool.util.Group;
import tz.co.nezatech.apps.surveytool.util.Input;
import tz.co.nezatech.apps.surveytool.util.Instance;
import tz.co.nezatech.apps.surveytool.util.ListAdapter;

public class SummaryActivity extends AppCompatActivity {
    final static String TAG = SummaryActivity.class.getName();
    private DatabaseHelper databaseHelper = null;

    public static void dummyRecords(DatabaseHelper db, int limit) {
        try {
            //clear all dummy
            //db.getFormInstanceDao().executeRaw("delete from tbl_form_instance where name like '%Dumm%'");

            List<Form> forms = db.getFormDao().queryForAll();
            Log.d(TAG, "Dummy forms: " + "dummyRecords");
            if (!forms.isEmpty()) {
                Random r = new Random();
                Form form = forms.get(0);
                Log.d(TAG, "The form: " + form.getName());
                for (int i = 0; i < limit; i++) {
                    try {
                        Instance instance = new Instance(form.getFormId());
                        Group group = new Group("DCategory1", "DummyGrp1", "DGrpType1", "DummyLabel");
                        List<Input> inputs = new ArrayList<>();
                        for (int j = 0; j < 2; j++) {
                            Input inp = new Input("ICategory" + i + "" + j, "ICategory" + i + "" + j + ".DInput" + i + "" + j, "DInputType" + i + "" + j, "DValue" + i + "" + j, "DummyLabel");
                            inputs.add(inp);
                        }
                        group.setInputs(inputs);

                        List<Group> groups = new ArrayList<>();
                        groups.add(group);

                        instance.setGroups(groups);
                        Gson gson = new Gson();
                        String json = gson.toJson(instance);
                        Log.d(TAG, "JSON: " + json);
                        String tmpl = "My Dummies[" + r.nextInt((limit * 100 - 100) + 100) + "]";
                        Log.d(TAG, "Display: " + tmpl);

                        FormInstance newInstance = new FormInstance(form, json, tmpl);
                        Calendar c = Calendar.getInstance();

                        int i1 = r.nextInt(40 - 0) + 0;
                        c.add(Calendar.DATE, -i1);
                        newInstance.setRecordDate(c.getTime());
                        db.getFormInstanceDao().create(newInstance);
                        Log.d(TAG, "Successfully recorded dummy: " + newInstance.getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "Exception: " + e.getMessage());
                    }
                }
            } else {
                Log.d(TAG, "Dummy forms: No form to use");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void dummyRecordsRemove(DatabaseHelper db) {
        try {
            //clear all dummy
            db.getFormInstanceDao().executeRaw("delete from tbl_form_instance where name like '%Dumm%'");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        displaySummary();
    }

    private void displaySummary() {
        try {
            List<SummaryLine> summaryLines = summaryLines();
            final ListView mListView = (ListView) findViewById(R.id.summary_list);
            ListAdapter<SummaryLine> adapter = new ListAdapter<SummaryLine>(SummaryActivity.this, summaryLines) {
                @Override
                protected void handleRowClick(int position, SummaryLine e) {
                    Log.d(TAG, "Summary Line: " + e.getDate());
                }

                @Override
                protected void makeRowView(View v, SummaryLine e) {
                    TextView date = (TextView) v.findViewById(R.id.summary_line_date);
                    date.setText(e.getDate());

                    TextView synched = (TextView) v.findViewById(R.id.summary_value_synced);
                    synched.setText(e.getSyncedCount() + "");
                    TextView unsynched = (TextView) v.findViewById(R.id.summary_value_unsynced);
                    unsynched.setText(e.getUnsyncedCount() + "");
                    TextView total = (TextView) v.findViewById(R.id.summary_value_total);
                    total.setText(e.getTotal() + "");
                }

                @Override
                protected int getRowLayoutResource() {
                    return R.layout.summary_line_row;
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
            mSearchView.setQueryHint(getString(R.string.summary_filter_hint));
            mSearchView.clearFocus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<SummaryLine> summaryLines() {
        String sql = "SELECT sum(synced) as synced, sum(unsynced) as unsynced, sDate FROM(\n" +
                "SELECT count(*) as synced, 0 as unsynced, date(record_date) sDate FROM tbl_form_instance WHERE status=1 GROUP BY date(record_date) " +
                "UNION " +
                "SELECT 0 as synced, count(*) as unsynced, date(record_date) sDate FROM tbl_form_instance WHERE status=0 GROUP BY date(record_date) " +
                ") as summary " +
                "GROUP BY sDate ORDER BY sDate desc";
        List<SummaryLine> summaryLines = null;
        try {

            summaryLines = getHelper().getDao(SummaryLine.class).queryRaw(sql, new RawRowMapper<SummaryLine>() {
                @Override
                public SummaryLine mapRow(String[] cols, String[] vals) throws SQLException {
                    SummaryLine sl = new SummaryLine(Integer.parseInt(vals[0]), Integer.parseInt(vals[1]), vals[2]);
                    return sl;
                }
            }).getResults();
            Log.d(TAG, summaryLines.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summaryLines;
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
