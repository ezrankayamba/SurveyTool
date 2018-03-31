package tz.co.nezatech.apps.surveytool.sync;

import android.content.Context;
import android.util.Log;
import android.view.View;

public class AsyncHttpTask extends AsyncAnyTask {
    final String TAG = AsyncHttpTask.class.getName();
    private String path;
    private String method;
    private String json;

    public AsyncHttpTask(View view, Context ctx, AsyncTaskListener listener, String path) {
        this(view, ctx, listener, path, "GET", null);
    }

    public AsyncHttpTask(View view, Context ctx, AsyncTaskListener listener, String path, String method, String json) {
        super(view, ctx, listener);
        this.path = path;
        this.method = method;
        this.json = json;
        this.view.set(view);
    }

    @Override
    public boolean processInBackground(String... strings) {
        try {
            String body;
            if (method.equalsIgnoreCase("GET")) {
                body = new SyncHttpUtil(getCtx().get()).httpGet(path);
            } else {
                body = new SyncHttpUtil(getCtx().get()).httpPost(path, json);
            }
            Log.d(TAG, "Response: " + body);
            return getListener().processResponse(body);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
