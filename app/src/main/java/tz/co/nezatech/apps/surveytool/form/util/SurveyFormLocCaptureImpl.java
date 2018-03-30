package tz.co.nezatech.apps.surveytool.form.util;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import tz.co.nezatech.apps.surveytool.R;
import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;
import tz.co.nezatech.apps.surveytool.db.model.Form;
import tz.co.nezatech.apps.surveytool.db.model.FormInstance;
import tz.co.nezatech.apps.surveytool.location.LocationService;

public class SurveyFormLocCaptureImpl extends SurveyFormImpl implements SurveyFormLocCapture {
    private static final String TAG = SurveyFormLocCaptureImpl.class.getName();
    public LocationService locationService;
    Location currentLocation = null;
    private String provider;
    private LinkedHashMap<String, LinkedHashMap<String, EditText>> locationMap = new LinkedHashMap<>();
    private List<LocationChangeListener> locationChangeListenerList = new LinkedList<>();
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            String name = className.getClassName();

            if (name.endsWith("LocationService")) {
                locationService = ((LocationService.LocationServiceBinder) service).getService();
                locationService.setListener(SurveyFormLocCaptureImpl.this);
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

    public SurveyFormLocCaptureImpl(Context context, DatabaseHelper databaseHelper, Form form) {
        super(context, databaseHelper, form);
    }

    public SurveyFormLocCaptureImpl(Context context, DatabaseHelper databaseHelper, Form form, FormInstance formInstance) {
        super(context, databaseHelper, form, formInstance);
    }

    public SurveyFormLocCaptureImpl(Context context, DatabaseHelper databaseHelper, Form form, FormInstance formInstance, FormMode formMode) {
        super(context, databaseHelper, form, formInstance, formMode);
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
    public void doGPSLocationCapture(LayoutInflater inflater, LinearLayout gropusLayout, final JSONObject group) throws JSONException {
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
                TypeRef<List<String>> typeRef = new TypeRef<List<String>>() {
                };
                String path = "$.groups[?(@.name == '" + jsonStr(group, "name", null) + "')].inputs[?(@.name == '" + name + "')].value";
                List<String> v = JsonPath.parse(formInstance.getJson()).read(path, typeRef);
                text.setText(v.isEmpty() ? "" : v.get(0));
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

    private Location getLastKnownLocation() {
        return this.currentLocation;
    }

    public void refreshLocGPS(View view) {
        try {
            Log.d(TAG, "Refreshing GPS");
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Log.d(TAG, "Refreshing GPS: Should show...");
                } else {
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);// request code =1; for location
                    Log.d(TAG, "Refreshing GPS: Show!?");
                }
            } else {
                Location location = getLastKnownLocation();
                if (location != null) {
                    Log.d(TAG, "Provider " + location.getProvider() + " has been selected.");
                    onLocationChanged(location);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ServiceConnection getServiceConnection() {
        return serviceConnection;
    }

    public interface LocationChangeListener {
        void locationChanged(Location location);
    }
}
