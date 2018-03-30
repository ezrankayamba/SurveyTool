package tz.co.nezatech.apps.surveytool.form.util;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import tz.co.nezatech.apps.surveytool.form.DataElem;

/**
 * Created by nkayamba on 3/10/18.
 */

interface FormView {
    ArrayList<View> getViewsWithTag(ViewGroup root);

    String getGroupLabel(String grpName);

    String getInputLabel(String inpName);

    DataElem dataFromView(View v);
}
