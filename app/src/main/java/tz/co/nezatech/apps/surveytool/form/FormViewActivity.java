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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.jayway.jsonpath.JsonPath;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import tz.co.nezatech.apps.surveytool.R;
import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;
import tz.co.nezatech.apps.surveytool.db.model.Form;
import tz.co.nezatech.apps.surveytool.db.model.FormInstance;
import tz.co.nezatech.apps.surveytool.util.FormUtil;

public class FormViewActivity extends AppCompatActivity {
    final String TAG = FormEditActivity.class.getName();
    LayoutInflater layoutInflater = null;
    FormInstance formInstance = null;
    Form form = null;
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

        initForm();
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

    private void initForm() {
        try {
            LinearLayout viewVontrols = (LinearLayout) findViewById(R.id.viewVontrols);
            final LinearLayout mainLayout = (LinearLayout) findViewById(R.id.form_edit_layout);

            ImageButton btnDelete = (ImageButton) viewVontrols.findViewById(R.id.btnDelete);
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
            ImageButton btnEdit = (ImageButton) viewVontrols.findViewById(R.id.btnEdit);
            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(FormViewActivity.this, FormEditActivity.class);
                    intent.putExtra(FormUtil.FORM_REPOS_DATA, form);
                    intent.putExtra(FormUtil.FORM_INSTANCE_DATA, formInstance);
                    startActivity(intent);
                }
            });

            JSONObject ui = new JSONObject(formInstance.getJson());
            JSONArray groups = (JSONArray) ui.get("groups");
            final LinearLayout gropusLayout = (LinearLayout) layoutInflater.inflate(R.layout.form_container, null);
            gropusLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.addView(gropusLayout);

            //((TextView) gropusLayout.findViewById(R.id.form_header)).setText(form.getName());

            for (int g = 0; g < groups.length(); g++) {
                final JSONObject group = (JSONObject) groups.get(g);

                String type = jsonStr(group, "type", "Text");
                String grpName = jsonStr(group, "name", null);
                Log.d(TAG, "Type: " + type);

                final TextView grpLabel = (TextView) (layoutInflater.inflate(R.layout.form_group_label, null));
                List<String> label = JsonPath.read(form.getJson(), "$.groups[?(@.name == '" + grpName + "')].label");
                String lbl = label.get(0);
                grpLabel.setText(lbl);
                gropusLayout.addView(grpLabel);

                switch (type) {
                    default:
                        Log.d(TAG, "doTextInput");
                        doTextInput(layoutInflater, gropusLayout, group);
                        break;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doTextInput(LayoutInflater inflater, LinearLayout gropusLayout, JSONObject group) throws JSONException {
        JSONArray inputs = (JSONArray) group.get("inputs");

        LinearLayout formL = (LinearLayout) inflater.inflate(R.layout.form_layout, null);
        //setFormGrpTag(group, form);


        formL.setOrientation(LinearLayout.VERTICAL);
        gropusLayout.addView(formL);

        for (int i = 0; i < inputs.length(); i++) {
            JSONObject input = (JSONObject) inputs.get(i);
            String dataType = jsonStr(input, "type", null);

            String name = jsonStr(input, "name", null);

            TextView label = (TextView) inflater.inflate(R.layout.form_label, null);
            List<String> l = JsonPath.read(form.getJson(), "$.groups[?(@.name == '" + jsonStr(group, "name", null) + "')].inputs[?(@.name == '" + name + "')].label");

            String lbl = l.get(0);
            label.setText(lbl);
            formL.addView(label);

            EditText text = (EditText) inflater.inflate(R.layout.form_input_text, null);
            List<String> v = JsonPath.read(formInstance.getJson(), "$.groups[?(@.name == '" + jsonStr(group, "name", null) + "')].inputs[?(@.name == '" + name + "')].value");
            text.setText(v.get(0));
            text.setEnabled(false);
            text.setTag(name);
            //setTextDataType(text, dataType);
            formL.addView(text);

            //setInputTag(input, text);

            LinearLayout vspace1 = (LinearLayout) inflater.inflate(R.layout.form_input_vseparator, null);
            formL.addView(vspace1);
        }
    }

    public String jsonStr(JSONObject o, String key, String defaultValue) {
        try {
            return o.getString(key);
        } catch (Exception e) {
        }
        return defaultValue;
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
