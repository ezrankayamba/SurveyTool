package tz.co.nezatech.apps.surveytool.form.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.json.JSONException;
import org.json.JSONObject;

public interface SurveyForm extends FormView {
    String jsonStr(JSONObject o, String key, String defaultValue);

    void setFormGrpTag(JSONObject group, ViewGroup form);

    void setInputTag(JSONObject input, View inputView);

    void setInputTag(JSONObject input, View inputView, InputTag tag);

    void setTextDataType(EditText text, String dataType);

    void doGenericInputGroup(LayoutInflater inflater, LinearLayout gropusLayout, JSONObject group) throws JSONException;

    boolean regexCheck(EditText txt, boolean onSubmit);

    boolean saveTheForm(ViewGroup root);

    FormMode getMode();

    enum InputTag {
        GRP, INP, INPPCL, INPCHK, INPYNC, INPRDB, INPOS, INPPSL, INPRSN, INPSTP, INPDTPMUL, INPDTP
    }

    enum FormMode {
        NEW, EDIT, VIEW
    }
}
