package tz.co.nezatech.apps.surveytool.form;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.gson.Gson;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.jayway.jsonpath.JsonPath;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import tz.co.nezatech.apps.surveytool.R;
import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;
import tz.co.nezatech.apps.surveytool.db.model.Form;
import tz.co.nezatech.apps.surveytool.db.model.FormInstance;
import tz.co.nezatech.apps.surveytool.location.LocationService;
import tz.co.nezatech.apps.surveytool.util.FormUtil;
import tz.co.nezatech.apps.surveytool.util.Group;
import tz.co.nezatech.apps.surveytool.util.Input;
import tz.co.nezatech.apps.surveytool.util.Instance;

import static tz.co.nezatech.apps.surveytool.location.LocationService.LocationServiceListener;


public class FormEditActivity extends AppCompatActivity implements LocationServiceListener {
    final String TAG = FormEditActivity.class.getName();
    public LocationService locationService;
    Form form;
    FormInstance formInstance = null;
    LayoutInflater layoutInflater = null;
    Location currentLocation = null;
    private DatabaseHelper databaseHelper = null;
    private String provider;
    private LinkedHashMap<String, LinkedHashMap<String, EditText>> locationMap = new LinkedHashMap<>();
    private List<LocationChangeListener> locationChangeListenerList = new LinkedList<>();
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            String name = className.getClassName();

            if (name.endsWith("LocationService")) {
                locationService = ((LocationService.LocationServiceBinder) service).getService();
                locationService.setListener(FormEditActivity.this);
                locationService.startUpdatingLocation();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (className.getClassName().equals("LocationService")) {
                locationService.stopUpdatingLocation();
                locationService = null;
                currentLocation = null;
            }
        }
    };

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

        initForm();
        final Intent serviceStart = new Intent(this.getApplication(), LocationService.class);
        this.getApplication().startService(serviceStart);
        this.getApplication().bindService(serviceStart, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initForm() {
        try {
            final LinearLayout mainLayout = (LinearLayout) findViewById(R.id.form_edit_layout);
            mainLayout.removeAllViews();
            mainLayout.refreshDrawableState();

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!saveTheForm(mainLayout)) {
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

            //((TextView) gropusLayout.findViewById(R.id.form_header)).setText(form.getName());

            for (int g = 0; g < groups.length(); g++) {
                final JSONObject group = (JSONObject) groups.get(g);

                String type = jsonStr(group, "type", "Text");
                Log.d(TAG, "Type: " + type);

                final TextView grpLabel = (TextView) (layoutInflater.inflate(R.layout.form_group_label, null));
                grpLabel.setText(group.getString("label"));
                gropusLayout.addView(grpLabel);

                switch (type) {
                    case "GPSLocationCapture":
                        Log.d(TAG, "doGPSLocationCapture");
                        doGPSLocationCapture(layoutInflater, gropusLayout, group);
                        break;
                    default:
                        Log.d(TAG, "doTextInput");
                        doTextInput(layoutInflater, gropusLayout, group);
                        break;
                }

            }

        } catch (Exception e) {

        }
    }

    @Override
    public void onLocationChanged(Location location) {
        this.currentLocation = location;
        if (!getLocationMap().isEmpty()) {
            for (String locld : getLocationMap().keySet()) {
                LinkedHashMap<String, EditText> latLong = getLocationMap().get(locld);
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                for (String key : latLong.keySet()) {
                    try {
                        if (key.toLowerCase().contains("latitude")) {
                            latLong.get(key).setText(String.valueOf(lat));
                        } else {
                            latLong.get(key).setText(String.valueOf(lng));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "Exception: " + e);
                    }
                }
            }

            for (LocationChangeListener listener : locationChangeListenerList) {
                listener.locationChanged(location);
            }
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

    public String jsonStr(JSONObject o, String key, String defaultValue) {
        try {
            return o.getString(key);
        } catch (Exception e) {
        }
        return defaultValue;
    }

    private void setFormGrpTag(JSONObject group, ViewGroup form) {
        String grpName = jsonStr(group, "name", "");
        String type = jsonStr(group, "type", "Text");
        form.setTag("GRP:" + grpName + ":" + type);
    }

    private void setTextDataType(EditText text, String dataType) {
        if (dataType == null) return;
        switch (dataType) {
            case "PhoneNumber":
                text.setInputType(InputType.TYPE_CLASS_PHONE);
                break;
            default:
                text.setInputType(InputType.TYPE_CLASS_TEXT);
        }
    }

    private void setInputTag(JSONObject input, View text) {
        String inpName = jsonStr(input, "name", "");
        String inpType = jsonStr(input, "type", "Text");
        text.setTag("INP:" + inpName + ":" + inpType);
    }

    private void doTextInput(LayoutInflater inflater, LinearLayout gropusLayout, JSONObject group) throws JSONException {
        JSONArray inputs = (JSONArray) group.get("inputs");

        LinearLayout form = (LinearLayout) inflater.inflate(R.layout.form_layout, null);
        setFormGrpTag(group, form);


        form.setOrientation(LinearLayout.VERTICAL);
        gropusLayout.addView(form);

        for (int i = 0; i < inputs.length(); i++) {
            JSONObject input = (JSONObject) inputs.get(i);
            String dataType = jsonStr(input, "type", null);

            TextView label = (TextView) inflater.inflate(R.layout.form_label, null);
            label.setText(input.getString("label"));
            form.addView(label);

            EditText text = (EditText) inflater.inflate(R.layout.form_input_text, null);
            String name = jsonStr(input, "name", null);
            text.setTag(name);
            if (formInstance != null) {
                List<String> v = JsonPath.read(formInstance.getJson(), "$.groups[?(@.name == '" + jsonStr(group, "name", null) + "')].inputs[?(@.name == '" + name + "')].value");
                text.setText(v.get(0));
            }
            setTextDataType(text, dataType);
            if (dataType != null && dataType.equals("TextArea")) {
                text.setSingleLine(false);
                text.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
            }

            //changelistener
            FormInputTextWatcher watcher = new FormInputTextWatcher(this, text) {

                @Override
                public void validateInput(EditText inputField) {
                    regexCheck(inputField, false);
                }
            };
            
            form.addView(text);

            setInputTag(input, text);

            LinearLayout vspace1 = (LinearLayout) inflater.inflate(R.layout.form_input_vseparator, null);
            form.addView(vspace1);
        }
    }

    public ArrayList<View> getViewsWithTag(ViewGroup root) {
        ArrayList<View> views = new ArrayList<View>();
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                if (child.getTag() != null && !child.getTag().toString().isEmpty()) {
                    views.add(child);
                }
                views.addAll(getViewsWithTag((ViewGroup) child));
            } else {
                if (child.getTag() != null && !child.getTag().toString().isEmpty()) {
                    views.add(child);
                }
            }
        }
        return views;
    }

    public boolean checkFormInputsRegex(ViewGroup root) {
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                if (!checkFormInputsRegex((ViewGroup) child)) {
                    return false;
                }
            } else {
                if (child.getTag() != null && !child.getTag().toString().isEmpty()) {
                    if (child instanceof EditText) {
                        EditText et = (EditText) child;
                        if (!regexCheck(et, true)) {
                            Log.e(TAG, "checkFormInputsRegex->NOK: " + String.format("Value->%s, Tag->%s", ((EditText) child).getText(), child.getTag()));
                            return false;
                        } else {
                            Log.d(TAG, "checkFormInputsRegex->OK: " + String.format("Value->%s, Tag->%s", ((EditText) child).getText(), child.getTag()));
                        }
                    }
                }
            }
        }
        return true;
    }

    public void refreshLocGPS(View view) {
        try {
            Log.d(TAG, "Refreshing GPS");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Log.d(TAG, "Refreshing GPS: Should show...");
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);// request code =1; for location
                    Log.d(TAG, "Refreshing GPS: Show!?");
                }
            } else {
                Location location = getLastKnownLocation();
                if (location != null) {
                    Log.d(TAG, "Provider " + location.getProvider() + " has been selected.");
                    onLocationChanged(location);
                } else {
                    Snackbar.make(view, "Make sure GPS is turned ON", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Location getLastKnownLocation() {
        return this.currentLocation;
    }

    public LinkedHashMap<String, LinkedHashMap<String, EditText>> getLocationMap() {
        return locationMap;
    }

    public void setLocationMap(LinkedHashMap<String, LinkedHashMap<String, EditText>> locationMap) {
        this.locationMap = locationMap;
    }

    public void addLocationChangeListener(LocationChangeListener listener) {
        locationChangeListenerList.add(listener);
    }

    private void doGPSLocationCapture(LayoutInflater inflater, LinearLayout gropusLayout, final JSONObject group) throws JSONException {
        final LinearLayout form = (LinearLayout) inflater.inflate(R.layout.form_layout_gps, null);
        setFormGrpTag(group, form);

        final JSONArray inputs = (JSONArray) group.get("inputs");


        form.setOrientation(LinearLayout.VERTICAL);
        final ImageButton btn = (ImageButton) form.findViewById(R.id.btnGPSRefresh);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshLocGPS(v);
            }
        });


        gropusLayout.addView(form);
        final LinkedHashMap<String, EditText> latLong = new LinkedHashMap<>();
        for (int i = 0; i < 2; i++) {//exactly 2 fields, lat & long
            JSONObject input = (JSONObject) inputs.get(i);

            Log.d(TAG, "Input: " + input.getString("name"));

            TextView label = (TextView) inflater.inflate(R.layout.form_label, null);
            label.setText(input.getString("label"));
            form.addView(label);

            EditText text = (EditText) inflater.inflate(R.layout.form_input_text, null);
            String name = jsonStr(input, "name", null);
            if (formInstance != null) {
                List<String> v = JsonPath.read(formInstance.getJson(), "$.groups[?(@.name == '" + jsonStr(group, "name", null) + "')].inputs[?(@.name == '" + name + "')].value");
                text.setText(v.get(0));
            }
            form.addView(text);
            setInputTag(input, text);

            latLong.put(name, text);

            LinearLayout vspace1 = (LinearLayout) inflater.inflate(R.layout.form_input_vseparator, null);
            form.addView(vspace1);
        }

        getLocationMap().put(jsonStr(group, "name", null), latLong);
        addLocationChangeListener(new LocationChangeListener() {
            @Override
            public void locationChanged(Location location) {
                float accuracy = location.getAccuracy();
                TextView gpsAccuracy = (TextView) form.findViewById(R.id.gpsAccuracy);
                gpsAccuracy.setText(String.format("Accuracy: %.2fm", accuracy));

                Log.d(TAG, "GPS Listener: Accuracy: " + accuracy);
            }
        });

        refreshLocGPS(btn);
    }

    String getGroupLabel(String grpName) {
        String label = null;
        try {
            Log.d(TAG, "getGroupLabel: " + String.format("Group->%s", grpName));
            List<String> v = JsonPath.read(form.getJson(), "$.groups[?(@.name == '" + grpName + "')].label");
            label = v.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "JSON: " + form.getJson());
        }
        return label;
    }

    String getInputLabel(String inpName) {
        String label = null;
        try {
            String grpName = inpName.split("\\.")[0];
            Log.d(TAG, "getInputLabel: " + String.format("Group->%s, Input->%s", grpName, inpName));
            List<String> v = JsonPath.read(form.getJson(), "$.groups[?(@.name == '" + grpName + "')].inputs[?(@.name == '" + inpName + "')].label");
            label = v.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "JSON: " + form.getJson());
        }
        return label;
    }

    private DataElem dataFromView(View v) {
        String tag = (String) v.getTag();
        String[] tokens = tag.split(":");

        DataElem dataElem = null;

        String value = null;
        if (tokens.length < 3) {
            value = "Error: not valid input or group";
            dataElem = new DataElem();
            dataElem.setSuccess(false);
            dataElem.setValue(value);
        } else {
            String category = tokens[0];
            String name = tokens[1];
            String type = tokens[2];
            boolean success = true;
            boolean isGroup = false;
            switch (category) {
                case "GRP":
                    value = "NA: This is a group";
                    isGroup = true;
                    break;
                case "INP":
                    if (v instanceof EditText) {
                        value = ((EditText) v).getText().toString();
                    } else {
                        value = "NA: INP unhandled";
                        success = false;
                    }
                    break;

                case "INPPCL":
                    if (v instanceof EditText) {
                        value = ((EditText) v).getText().toString();
                    } else {
                        value = "NA: INPPCL unhandled";
                        success = false;
                    }
                    break;
                case "INPCHK":
                    if (v instanceof CheckBox) {
                        value = ((CheckBox) v).isChecked() + "";
                    } else {
                        value = "NA: INPCHK unhandled";
                        success = false;
                    }
                    break;
                case "INPYNC":
                    if (v instanceof CheckBox) {
                        value = ((CheckBox) v).isChecked() + "";
                    } else {
                        value = "NA: INPYNC unhandled";
                        success = false;
                    }
                    break;
                case "INPRDB":
                    if (v instanceof RadioButton) {
                        value = ((RadioButton) v).isChecked() + "";
                    } else {
                        value = "NA: INPRDB unhandled";
                        success = false;
                    }
                    break;
                case "INPOS":
                    if (v instanceof CheckBox) {
                        value = ((CheckBox) v).isChecked() + "";
                    } else if (v instanceof EditText) {
                        value = ((EditText) v).getText().toString();
                    } else {
                        value = "NA: INPOS unhandled";
                        success = false;
                    }
                    break;
                case "INPPSL":
                    if (v instanceof CheckBox) {
                        value = ((CheckBox) v).isChecked() + "";
                    } else if (v instanceof EditText) {
                        value = ((EditText) v).getText().toString();
                    } else {
                        value = "NA: INPPSL unhandled";
                        success = false;
                    }
                    break;
                case "INPRSN":
                    if (v instanceof CheckBox) {
                        value = ((CheckBox) v).isChecked() + "";
                    } else if (v instanceof EditText) {
                        value = ((EditText) v).getText().toString();
                    } else {
                        value = "NA: INPRSN unhandled";
                        success = false;
                    }
                    break;

                default:
                    value = "NA: Not yet handled";
                    success = false;
                    break;
            }
            dataElem = new DataElem(name, type, category, value, isGroup ? getGroupLabel(name) : getInputLabel(name));
            dataElem.setSuccess(success);
        }


        return dataElem;
    }

    public boolean saveTheForm(ViewGroup root) {
        try {
            Log.d(TAG, String.format("Id: %d, Name: %s", form.getId(), form.getName()));

            if (checkFormInputsRegex(root)) {
                ArrayList<View> viewsWithTag = getViewsWithTag(root);

                List<DataElem> list = new LinkedList<>();
                Instance instance = new Instance(form.getFormId());
                List<Group> groups = new LinkedList<>();
                List<Input> inputs = null;
                Group group = null;
                //String display = "NIL";
                ArrayList<String> displayList = new ArrayList<>();
                String tmpl = form.getDisplay();
                for (View v : viewsWithTag) {
                    DataElem dataElem = dataFromView(v);
                    if (dataElem.getCategory().equals("GRP")) {
                        //check prev
                        if (group != null) {//first grp, ignore
                            group.setInputs(inputs);
                            groups.add(group);
                        }
                        group = new Group(dataElem.getCategory(), dataElem.getName(), dataElem.getType(), dataElem.getLabel());
                        inputs = new LinkedList<>();
                    } else if (dataElem.getCategory().equals("INP")) {
                        inputs.add(new Input(dataElem.getCategory(), dataElem.getName(), dataElem.getType(), dataElem.getValue(), dataElem.getLabel()));

                        if (tmpl.contains(dataElem.getName())) {
                            tmpl = tmpl.replaceAll(dataElem.getName(), dataElem.getValue());
                        }
                    }
                }
                if (group != null && inputs != null) {//first grp, ignore
                    group.setInputs(inputs);
                    groups.add(group);
                }
                instance.setGroups(groups);

                try {
                    Gson gson = new Gson();
                    String json = gson.toJson(instance);
                    Log.d(TAG, "JSON: " + json);
                    Log.d(TAG, "Display: " + tmpl);

                    FormInstance newInstance = new FormInstance(form, json, tmpl);
                    if (formInstance != null) {
                        newInstance.setId(formInstance.getId());
                        getHelper().getFormInstanceDao().update(newInstance);
                    } else {
                        getHelper().getFormInstanceDao().create(newInstance);
                    }

                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "Valdation failed");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

    private boolean regexCheck(EditText txt, boolean onSubmit) {
        try {
            //EditText txt = editText;
            Object tag = txt.getTag();
            String regex = null;
            String regexMessage = null;
            try {
                String name = tag.toString().split(":")[1];
                List<String> rgx = JsonPath.read(form.getJson(), "$.groups[?(@.name == '" + name.split("\\.")[0] + "')].inputs[?(@.name == '" + name + "')].regex");
                regex = rgx.get(0);
                List<String> rgxMsg = JsonPath.read(form.getJson(), "$.groups[?(@.name == '" + name.split("\\.")[0] + "')].inputs[?(@.name == '" + name + "')].regexMessage");
                regexMessage = rgxMsg.get(0);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "Exception: " + e.getMessage());
            }

            if (regex != null) {
                String value = txt.getText().toString();
                if (value == null) {
                    value = "";
                }
                Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
                if (p.matcher(value).find()) {
                    Log.d(TAG, "Validation-OK");
                    txt.setError(null);
                    //txt.setFocusableInTouchMode(false);
                    return true;
                } else {
                    Log.e(TAG, "Validation-NOK: " + String.format("Value->%s, Regex->%s", value, regex));
                    txt.setError(regexMessage);

                    if (onSubmit) {
                        txt.setFocusableInTouchMode(true);
                        txt.requestFocus();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Exception: " + e.getMessage());
        }

        return false;
    }

    public interface LocationChangeListener {
        void locationChanged(Location location);
    }

}
