package apps.nezatech.co.tz.surveytool;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

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

import apps.nezatech.co.tz.surveytool.db.DatabaseHelper;
import apps.nezatech.co.tz.surveytool.db.Form;
import apps.nezatech.co.tz.surveytool.db.FormInstance;
import apps.nezatech.co.tz.surveytool.util.FormUtil;
import apps.nezatech.co.tz.surveytool.util.Group;
import apps.nezatech.co.tz.surveytool.util.Input;
import apps.nezatech.co.tz.surveytool.util.Instance;


public class FormEditActivity extends AppCompatActivity implements LocationListener {
    final String TAG = FormEditActivity.class.getName();
    Form form;
    FormInstance formInstance = null;
    LayoutInflater layoutInflater = null;
    private DatabaseHelper databaseHelper = null;
    private LocationManager locationManager;
    private String provider;
    private LinkedHashMap<String, LinkedHashMap<String, EditText>> locationMap = new LinkedHashMap<>();
    private List<LocationChangeListener> locationChangeListenerList = new LinkedList<>();

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

        // Get the location manager
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        // Define the criteria how to select the locatioin provider -> use
        // default
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);


        initForm();
    }

    private void initForm() {
        try {
            final LinearLayout mainLayout = (LinearLayout) findViewById(R.id.form_edit_layout);

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
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new provider " + provider, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
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

    public void refreshLocGPS() {
        try {
            Log.d(TAG, "Refreshing GPS");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Log.d(TAG, "Refreshing GPS: Should show...");
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);// request code =1; for location
                    Log.d(TAG, "Refreshing GPS: Show!?");
                }

            } else {
                Log.d(TAG, "Refreshing GPS: Proceed => " + provider);
                Location location = getLastKnownLocation();
                // Initialize the location fields
                if (location != null) {
                    Log.d(TAG, "Provider " + provider + " has been selected.");
                    onLocationChanged(location);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Location getLastKnownLocation() {
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            try {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location: %s", l);
                    bestLocation = l;
                }
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }
        return bestLocation;
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
                refreshLocGPS();
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

        refreshLocGPS();
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

            switch (category) {
                case "GRP":
                    value = "NA: This is a group";
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
            dataElem = new DataElem(name, type, category, value);
            dataElem.setSuccess(success);
        }


        return dataElem;
    }

    public boolean saveTheForm(ViewGroup root) {
        try {
            Log.d(TAG, String.format("Id: %d, Name: %s", form.getId(), form.getName()));

            ArrayList<View> viewsWithTag = getViewsWithTag(root);

            List<DataElem> list = new LinkedList<>();
            Instance instance = new Instance(form.getFormId());
            List<Group> groups = new LinkedList<>();
            List<Input> inputs = null;
            Group group = null;
            String display = "NIL";
            for (View v : viewsWithTag) {
                DataElem dataElem = dataFromView(v);
                //Log.d(TAG, String.format("%s  = %s", v.getTag(), dataElem.getValue()));
                //list.add(dataElem);
                if (dataElem.getCategory().equals("GRP")) {
                    //check prev
                    if (group != null) {//first grp, ignore
                        group.setInputs(inputs);
                        groups.add(group);
                    }
                    group = new Group(dataElem.getCategory(), dataElem.getName(), dataElem.getType());
                    inputs = new LinkedList<>();
                } else if (dataElem.getCategory().equals("INP")) {
                    inputs.add(new Input(dataElem.getCategory(), dataElem.getName(), dataElem.getType(), dataElem.getValue()));

                    if (dataElem.getName().equals(form.getDisplay())) {
                        display = dataElem.getValue();
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

                FormInstance newInstance = new FormInstance(form, json, display);
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

    public interface LocationChangeListener {
        void locationChanged(Location location);
    }
}
