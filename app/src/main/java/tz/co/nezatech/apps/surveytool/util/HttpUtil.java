package tz.co.nezatech.apps.surveytool.util;

/**
 * Created by nkayamba on 2/1/18.
 */

public class HttpUtil {
    public static final String FORMS_BASE_URL = "http://pincomtz.net:8001";
    //public static final String FORMS_BASE_URL = "http://2ed98073.ngrok.io";
    public static final String FORMS_PATH = "/survey/api/forms";
    public static final String FORMS_SYNC_PATH = "/survey/api/forms";
    public static final long SYNC_INTERVAL_IN_SECONDS = 60L * 15L;//15 minutes
}
