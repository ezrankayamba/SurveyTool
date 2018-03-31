package tz.co.nezatech.apps.surveytool.sync;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;

import java.util.concurrent.atomic.AtomicReference;

public abstract class AsyncAnyTask extends AsyncTask<String, Void, Boolean> {
    protected final ThreadLocal<View> view = new ThreadLocal<>();
    final String TAG = AsyncAnyTask.class.getName();
    private final AtomicReference<Context> ctx;
    private ProgressDialog dialog;
    private AsyncTaskListener listener;


    public AsyncAnyTask(View view, Context ctx, AsyncTaskListener listener) {
        dialog = new ProgressDialog(ctx);
        this.view.set(view);
        this.ctx = new AtomicReference<>();
        this.ctx.set(ctx);
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        dialog.setMessage("Http activity ongoing. Please wait...");
        dialog.show();
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        return processInBackground(strings);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        listener.done(view.get(), success);
    }

    public abstract boolean processInBackground(String... strings);

    public ThreadLocal<View> getView() {
        return view;
    }

    AtomicReference<Context> getCtx() {
        return ctx;
    }

    AsyncTaskListener getListener() {
        return listener;
    }

}
