package apps.nezatech.co.tz.surveytool.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import apps.nezatech.co.tz.surveytool.db.Form;

/**
 * Created by nkayamba on 2/2/18.
 */

public class FormUtil {
    public static final String FORM_REPOS_DATA = "formRepos";
    public static final String FORM_INSTANCE_DATA = "formInstance";

    public static final String formInstanceName(Form form, Date recordDate, String display) {
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return String.format("%s: %s @ %s", form.getName(), display, df.format(recordDate));
    }
}
