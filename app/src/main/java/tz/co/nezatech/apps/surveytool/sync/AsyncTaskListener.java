package tz.co.nezatech.apps.surveytool.sync;

import android.view.View;

/**
 * Created by nkayamba on 3/31/18.
 */

public interface AsyncTaskListener {
    void done(View view, boolean success);

    boolean processResponse(String body);
}
