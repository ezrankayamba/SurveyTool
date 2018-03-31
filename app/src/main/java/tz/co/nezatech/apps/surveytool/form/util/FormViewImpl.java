package tz.co.nezatech.apps.surveytool.form.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import tz.co.nezatech.apps.surveytool.R;
import tz.co.nezatech.apps.surveytool.db.model.Form;
import tz.co.nezatech.apps.surveytool.form.DataElem;

public class FormViewImpl implements FormView {
    private static final String TAG = FormViewImpl.class.getName();
    Form form;
    private Context context;

    public FormViewImpl(Form form, Context context) {
        this.form = form;
        this.context = context;
    }

    @Override
    public ArrayList<View> getViewsWithTag(ViewGroup root) {
        ArrayList<View> views = new ArrayList<View>();
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                if (child.getTag() != null && !child.getTag().toString().isEmpty()) {
                    views.add(child);
                }
                views.addAll(getViewsWithTag((ViewGroup) child));
            } else {
                if (child.getTag() != null && !child.getTag().toString().isEmpty() && !child.getTag().toString().startsWith("IGNORE:")) {
                    views.add(child);
                }
            }
        }
        return views;
    }

    @Override
    public String getGroupLabel(String grpName) {
        String label = null;
        try {
            String path = String.format(context.getString(R.string.form_grouplabel_jsonpath), grpName);
            TypeRef<List<String>> typeRef = new TypeRef<List<String>>() {
            };
            List<String> v = JsonPath.parse(form.getJson()).read(path, typeRef);
            label = v.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return label;
    }

    @Override
    public String getInputLabel(String inpName) {
        String label = null;
        try {
            String grpName = inpName.split("\\.")[0];
            TypeRef<List<String>> typeRef = new TypeRef<List<String>>() {
            };
            String path = String.format(context.getString(R.string.form_inputlabel_jsonpath), grpName, inpName);
            List<String> v = JsonPath.parse(form.getJson()).read(path, typeRef);
            label = v.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return label;
    }

    @Override
    public DataElem dataFromView(View v) {
        String tag = (String) v.getTag();
        String[] tokens = tag.split(":");
        DataElem dataElem = null;
        Object value = null;
        if (tokens.length < 3) {
            value = context.getString(R.string.form_input_error_notvalid_inputorgroup);
            dataElem = new DataElem();
            dataElem.setSuccess(false);
            dataElem.setValue(value);
        } else {
            SurveyForm.InputTag category = SurveyForm.InputTag.valueOf(tokens[0]);
            String name = tokens[1];
            String type = tokens[2];
            String dataType = type;
            boolean success = true;
            boolean isGroup = false;
            Object extra = null;

            switch (category) {
                case GRP:
                    value = "NA: This is a group";
                    isGroup = true;
                    break;
                case INP:
                    if (v instanceof EditText) {
                        value = ((EditText) v).getText().toString();
                    } else {
                        value = "NA: INP unhandled";
                        success = false;
                    }
                    break;

                case INPPCL:
                    if (v instanceof EditText) {
                        value = ((EditText) v).getText().toString();
                    } else {
                        value = "NA: INPPCL unhandled";
                        success = false;
                    }
                    break;
                case INPCHK:
                    if (v instanceof CheckBox) {
                        value = ((CheckBox) v).isChecked() + "";
                    } else {
                        value = "NA: INPCHK unhandled";
                        success = false;
                    }
                    break;
                case INPYNC:
                    if (v instanceof CheckBox) {
                        value = ((CheckBox) v).isChecked() + "";
                    } else {
                        value = "NA: INPYNC unhandled";
                        success = false;
                    }
                    break;
                case INPRDB:
                    if (v instanceof RadioButton) {
                        value = ((RadioButton) v).isChecked() + "";
                    } else {
                        value = "NA: INPRDB unhandled";
                        success = false;
                    }
                    break;
                case INPOS:
                    if (v instanceof CheckBox) {
                        value = ((CheckBox) v).isChecked() + "";
                    } else if (v instanceof EditText) {
                        value = ((EditText) v).getText().toString();
                    } else {
                        value = "NA: INPOS unhandled";
                        success = false;
                    }
                    break;
                case INPPSL:
                    if (v instanceof CheckBox) {
                        value = ((CheckBox) v).isChecked() + "";
                    } else if (v instanceof EditText) {
                        value = ((EditText) v).getText().toString();
                    } else {
                        value = "NA: INPPSL unhandled";
                        success = false;
                    }
                    break;
                case INPRSN:
                    if (v instanceof CheckBox) {
                        value = ((CheckBox) v).isChecked() + "";
                    } else if (v instanceof EditText) {
                        value = ((EditText) v).getText().toString();
                    } else {
                        value = "NA: INPRSN unhandled";
                        success = false;
                    }
                    break;
                case INPSTP:
                    if (v instanceof LinearLayout) {
                        String uuid = ((EditText) v.findViewById(R.id.autoTextViewUuid)).getText().toString();
                        String text = ((EditText) v.findViewById(R.id.autoTextViewText)).getText().toString();
                        value = uuid;
                        extra = text;
                    } else {
                        value = "NA: Setup not yet handled";
                        success = false;
                    }
                    break;
                case INPDTP:
                    if (v instanceof LinearLayout) {
                        SelectInput grp = null;

                        LinearLayout radioLayout = (LinearLayout) v;
                        RadioGroup rg = (RadioGroup) radioLayout.findViewById(R.id.formRadioGroup);
                        LinearLayout l = (LinearLayout) radioLayout.findViewById(R.id.otherSpecifyLayout);
                        EditText otherSp = (EditText) l.findViewById(R.id.checkOtherText);
                        int id = rg.getCheckedRadioButtonId();
                        if (rg.findViewById(id) != null) {
                            RadioButton rb = (RadioButton) rg.findViewById(id);

                            if (rb.getText().equals(context.getString(R.string.formRadioGroupOtherSpecifyNewLabel))) {
                                String dtType = rb.getTag().toString().split(":")[1];
                                grp = new SelectInput(dtType + context.getString(R.string.constTextOthersSpecify), otherSp.getText().toString());
                            } else {
                                grp = new SelectInput(rb.getText().toString());
                            }

                            value = grp;
                        } else {
                            value = "NA: No radio is selected";
                            success = false;
                        }
                    } else {
                        value = "NA: Spinner not yet handled";
                        success = false;
                    }
                    break;
                case INPDTPMUL:
                    if (v instanceof LinearLayout) {
                        LinearLayout checkLayout = (LinearLayout) v;
                        LinearLayout formCheckGroup = (LinearLayout) checkLayout.findViewById(R.id.formCheckGroup);
                        List<ExtraSelectInput> selected = new LinkedList<>();
                        for (int i = 0; i < formCheckGroup.getChildCount(); ++i) {
                            final LinearLayout checkBoxRow = (LinearLayout) formCheckGroup.getChildAt(i);
                            final CheckBox cb = (CheckBox) checkBoxRow.findViewById(R.id.checkBox1);
                            final EditText otherSp = (EditText) checkBoxRow.findViewById(R.id.checkOtherText);
                            if (cb == null) continue;
                            if (cb.isChecked()) {
                                final LinearLayout extraLayout = (LinearLayout) checkBoxRow.findViewById(R.id.checkBox1Extra);
                                List<String> extras = new LinkedList<>();
                                for (int j = 0; j < extraLayout.getChildCount(); ++j) {
                                    final EditText text = (EditText) extraLayout.getChildAt(j);
                                    extras.add(text.getText().toString());
                                }

                                if (cb.getText().equals(context.getString(R.string.formCheckBoxOtherSpecifyNewLabel))) {
                                    String dtType = cb.getTag().toString().split(":")[1];
                                    selected.add(new ExtraSelectInput(dtType + context.getString(R.string.constTextOthersSpecify), extras, otherSp.getText().toString()));
                                } else {
                                    selected.add(new ExtraSelectInput(cb.getText().toString(), extras));
                                }
                            }
                        }
                        value = selected;

                    } else {
                        value = "NA: Multi select not yet handled";
                        success = false;
                    }
                    break;

                default: {
                    value = "NA: Not yet handled";
                    success = false;
                }
                break;
            }
            dataElem = new DataElem(name, type, category.name(), value, isGroup ? getGroupLabel(name) : getInputLabel(name));
            dataElem.setSuccess(success);
            dataElem.setExtra(extra);
        }
        return dataElem;
    }
}
