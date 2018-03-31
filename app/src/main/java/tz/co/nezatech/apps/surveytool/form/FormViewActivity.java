package tz.co.nezatech.apps.surveytool.form;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

import tz.co.nezatech.apps.surveytool.R;
import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;
import tz.co.nezatech.apps.surveytool.db.model.Form;
import tz.co.nezatech.apps.surveytool.db.model.FormInstance;
import tz.co.nezatech.apps.surveytool.form.util.SurveyForm;
import tz.co.nezatech.apps.surveytool.form.util.SurveyFormLocCapture;
import tz.co.nezatech.apps.surveytool.form.util.SurveyFormLocCaptureImpl;
import tz.co.nezatech.apps.surveytool.util.FormUtil;

public class FormViewActivity extends AppCompatActivity {
    final String TAG = FormEditActivity.class.getName();
    LayoutInflater layoutInflater = null;
    FormInstance formInstance = null;
    Form form = null;
    SurveyFormLocCapture surveyForm;
    private DatabaseHelper databaseHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Intent intent = getIntent();
        formInstance = (FormInstance) intent.getSerializableExtra(FormUtil.FORM_INSTANCE_DATA);
        toolbar.setTitle(formInstance.getName());
        setSupportActionBar(toolbar);

        form = (Form) intent.getSerializableExtra(FormUtil.FORM_REPOS_DATA);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FormViewActivity.this, FormEditActivity.class);
                intent.putExtra(FormUtil.FORM_REPOS_DATA, form);
                startActivity(intent);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        surveyForm = new SurveyFormLocCaptureImpl(this, getHelper(), form, formInstance, SurveyForm.FormMode.VIEW);

        initForm(surveyForm);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
        Intent intent = new Intent(FormViewActivity.this, FormInstanceActivity.class);
        intent.putExtra(FormUtil.FORM_REPOS_DATA, form);
        startActivity(intent);
        finish();
    }

    private void initForm(SurveyFormLocCapture surveyForm) {
        try {
            LinearLayout viewVontrols = (LinearLayout) findViewById(R.id.viewVontrols);
            final LinearLayout mainLayout = (LinearLayout) findViewById(R.id.form_edit_layout);

            Button btnDelete = (Button) viewVontrols.findViewById(R.id.btnDelete);
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Delete me");
                    try {
                        getHelper().getFormInstanceDao().delete(formInstance);
                        onUpButtonPressed();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            Button btnEdit = (Button) viewVontrols.findViewById(R.id.btnEdit);
            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(FormViewActivity.this, FormEditActivity.class);
                    intent.putExtra(FormUtil.FORM_REPOS_DATA, form);
                    intent.putExtra(FormUtil.FORM_INSTANCE_DATA, formInstance);
                    startActivity(intent);
                }
            });

            JSONObject ui = new JSONObject(form.getJson());
            JSONArray groups = (JSONArray) ui.get("groups");
            final LinearLayout gropusLayout = (LinearLayout) layoutInflater.inflate(R.layout.form_container, null);
            gropusLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.addView(gropusLayout);

            for (int g = 0; g < groups.length(); g++) {
                final JSONObject group = (JSONObject) groups.get(g);
                Log.d(TAG, String.format(Locale.ENGLISH, "Group: %d, %s", g, group.toString(2)));

                String type = surveyForm.jsonStr(group, "type", "Text");
                String grpName = surveyForm.jsonStr(group, "name", null);


                final TextView grpLabel = (TextView) (layoutInflater.inflate(R.layout.form_group_label, null));
                TypeRef<List<String>> typeRef = new TypeRef<List<String>>() {
                };
                String path = "$.groups[?(@.name == '" + grpName + "')].label";
                List<String> label = JsonPath.parse(form.getJson()).read(path, typeRef);
                String lbl = label.get(0);
                grpLabel.setText(lbl);
                gropusLayout.addView(grpLabel);

                switch (type) {
                    default:
                        Log.d(TAG, "doTextInput");
                        surveyForm.doGenericInputGroup(layoutInflater, gropusLayout, group);
                        break;
                }

            }

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
