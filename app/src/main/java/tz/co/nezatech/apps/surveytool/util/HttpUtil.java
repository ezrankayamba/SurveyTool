package tz.co.nezatech.apps.surveytool.util;

/**
 * Created by nkayamba on 2/1/18.
 */

public class HttpUtil {
    public static final String FORMS_BASE_URL = "http://77.73.69.79:8001";
    //public static final String FORMS_BASE_URL = "https://dcc55469.ngrok.io";
    public static final String FORMS_PATH = "/survey/api/forms";
    public static final String FORMS_SYNC_PATH = "/survey/api/forms";
    public static final String FORMS_DOWNLOAD_SYNC_PATH = "/survey/api/forms/download";
    public static final String FORMS_SETUPS_SYNC_PATH = "/survey/api/setups";
    public static final String FORMS_DATATYPES_SYNC_PATH = "/survey/api/dataTypes";
    public static final long SYNC_INTERVAL_IN_SECONDS = 60L * 15L;//15 minutes
}
