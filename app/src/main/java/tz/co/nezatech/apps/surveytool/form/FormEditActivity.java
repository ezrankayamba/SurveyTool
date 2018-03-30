package tz.co.nezatech.apps.surveytool.form;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;

import tz.co.nezatech.apps.surveytool.R;
import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;
import tz.co.nezatech.apps.surveytool.db.model.DataType;
import tz.co.nezatech.apps.surveytool.db.model.Form;
import tz.co.nezatech.apps.surveytool.db.model.FormInstance;
import tz.co.nezatech.apps.surveytool.form.util.SurveyForm;
import tz.co.nezatech.apps.surveytool.form.util.SurveyFormLocCapture;
import tz.co.nezatech.apps.surveytool.form.util.SurveyFormLocCaptureImpl;
import tz.co.nezatech.apps.surveytool.location.LocationService;
import tz.co.nezatech.apps.surveytool.util.FormUtil;


public class FormEditActivity extends AppCompatActivity {
    final String TAG = FormEditActivity.class.getName();
    Form form;
    SurveyFormLocCapture surveyForm;
    FormInstance formInstance = null;
    LayoutInflater layoutInflater = null;
    private DatabaseHelper databaseHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_edit);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Intent intent = getIntent();

        layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        form = (Form) intent.getSerializableExtra(FormUtil.FORM_REPOS_DATA);
        toolbar.setTitle(form.getName() + ": Editing...");
        setSupportActionBar(toolbar);

        formInstance = (FormInstance) intent.getSerializableExtra(FormUtil.FORM_INSTANCE_DATA);
        surveyForm = new SurveyFormLocCaptureImpl(this, getHelper(), form, formInstance, formInstance == null ? SurveyForm.FormMode.NEW : SurveyForm.FormMode.EDIT);
        initForm(surveyForm);
        final Intent serviceStart = new Intent(this.getApplication(), LocationService.class);
        this.getApplication().startService(serviceStart);
        this.getApplication().bindService(serviceStart, surveyForm.getServiceConnection(), Context.BIND_AUTO_CREATE);

        //Delete unwanted data types
        try {
            getHelper().getDataTypeDao().delete(new DataType("Others", "", ""));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (formInstance != null) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            View current = getCurrentFocus();
            if (current != null) current.clearFocus();
        }
    }

    private void initForm(final SurveyFormLocCapture surveyForm) {
        try {

            final LinearLayout mainLayout = (LinearLayout) findViewById(R.id.form_edit_layout);
            mainLayout.removeAllViews();
            mainLayout.refreshDrawableState();

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!surveyForm.saveTheForm(mainLayout)) {
                        Snackbar.make(view, "Form saving failed", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    } else {
                        onUpButtonPressed();
                    }
                }
            });
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            JSONObject ui = new JSONObject(form.getJson());
            JSONArray groups = (JSONArray) ui.get("groups");
            final LinearLayout gropusLayout = (LinearLayout) layoutInflater.inflate(R.layout.form_container, null);
            gropusLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.addView(gropusLayout);

            for (int g = 0; g < groups.length(); g++) {
                final JSONObject group = (JSONObject) groups.get(g);

                String type = surveyForm.jsonStr(group, "type", "GenericFormGroup");
                Log.d(TAG, "Group Type: " + type);

                final TextView grpLabel = (TextView) (layoutInflater.inflate(R.layout.form_group_label, null));
                grpLabel.setText(group.getString("label"));
                gropusLayout.addView(grpLabel);

                switch (type) {
                    case "GPSLocationCapture":
                        Log.d(TAG, "doGPSLocationCapture");
                        this.surveyForm.doGPSLocationCapture(layoutInflater, gropusLayout, group);
                        break;
                    default:
                        Log.d(TAG, "doGeneric groups");
                        this.surveyForm.doGenericInputGroup(layoutInflater, gropusLayout, group);
                        break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onUpButtonPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onUpButtonPressed() {
        Intent intent = new Intent(FormEditActivity.this, FormInstanceActivity.class);
        intent.putExtra(FormUtil.FORM_REPOS_DATA, form);
        startActivity(intent);
        finish();
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
