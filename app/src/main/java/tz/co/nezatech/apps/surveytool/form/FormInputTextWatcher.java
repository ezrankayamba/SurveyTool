package tz.co.nezatech.apps.surveytool.form;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

/**
 * Created by nkayamba on 2/15/18.
 */

abstract class FormInputTextWatcher implements TextWatcher, View.OnFocusChangeListener {
    private static final String TAG = FormInputTextWatcher.class.getName();
    EditText editText;
    Activity activity;
    boolean textChanged = false;
    String newText = null;

    public FormInputTextWatcher(final Activity activity, EditText editText) {
        this.editText = editText;
        this.activity = activity;

        /*this.editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.getCurrentFocus().setFocusable(false);
                v.requestFocus();
            }
        });*/

        this.editText.addTextChangedListener(this);
        this.editText.setOnFocusChangeListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        Log.d(TAG, "onTextChanged: " + s);
        //regexCheck(editText);
    }

    @Override
    public void afterTextChanged(Editable s) {
        Log.d(TAG, "afterTextChanged: " + s);
        textChanged = true;
        newText = s.toString();
    }

    //public abstract void afterTextChanged(Editable s);

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            Log.d(TAG, String.format("%s: Now editing...", v.getTag()));
        } else {
            Log.d(TAG, String.format("%s: Editing complete->%s", v.getTag(), newText));
            if (v instanceof EditText) {
                validateInput((EditText) v);
            }
        }
    }

    public abstract void validateInput(EditText inputField);
}