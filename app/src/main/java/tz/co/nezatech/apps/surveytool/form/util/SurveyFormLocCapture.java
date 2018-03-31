package tz.co.nezatech.apps.surveytool.form.util;

import android.content.ServiceConnection;
import android.location.Location;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import org.json.JSONException;
import org.json.JSONObject;

import tz.co.nezatech.apps.surveytool.location.LocationService;

public interface SurveyFormLocCapture extends SurveyForm, LocationService.LocationServiceListener {
    void doGPSLocationCapture(LayoutInflater layoutInflater, LinearLayout gropusLayout, JSONObject group) throws JSONException;

    void onLocationChanged(Location location);

    ServiceConnection getServiceConnection();
}
