package apps.nezatech.co.tz.surveytool.form;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.util.List;

import apps.nezatech.co.tz.surveytool.R;
import apps.nezatech.co.tz.surveytool.db.DatabaseHelper;
import apps.nezatech.co.tz.surveytool.db.model.Form;
import apps.nezatech.co.tz.surveytool.db.model.FormInstance;
import apps.nezatech.co.tz.surveytool.util.FormUtil;
import apps.nezatech.co.tz.surveytool.util.ListAdapter;

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

    private void displayFormInstances() {
        try {
            List<FormInstance> formInstances = getHelper().getFormInstanceDao().queryBuilder().orderBy("record_date", false).query();
            Log.d(TAG, "ListSize: " + formInstances.size());
            ListView listView = (ListView) findViewById(R.id.form_instance_list);
            listView.setAdapter(new ListAdapter<FormInstance>(FormInstanceActivity.this, formInstances) {
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
                }

                @Override
                protected int getRowLayoutResource() {
                    return R.layout.form_instance_row;
                }
            });
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
