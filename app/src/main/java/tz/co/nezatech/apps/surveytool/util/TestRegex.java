package tz.co.nezatech.apps.surveytool.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nkayamba on 2/28/18.
 */

public class TestRegex {
    static final String TAG = TestRegex.class.getName();

    public static void main(String[] args) {
        String setupsRegex = "^Setup\\.(\\w+)$";
        String type = "Setup.Sites";
        Pattern pattern = Pattern.compile(setupsRegex);
        Matcher matcher = pattern.matcher(type);
        if (matcher.find()) {
            System.out.println("doSpinnerInput: " + type);
            String setupType = matcher.group(1);

            System.out.println(setupType);
        } else {
            System.err.println((String.format("No match: %s, -> %s", setupsRegex, type)));
        }
    }
}
