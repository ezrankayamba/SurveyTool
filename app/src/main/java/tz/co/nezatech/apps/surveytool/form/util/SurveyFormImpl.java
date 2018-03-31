package tz.co.nezatech.apps.surveytool.form.util;

import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.j256.ormlite.stmt.QueryBuilder;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tz.co.nezatech.apps.surveytool.R;
import tz.co.nezatech.apps.surveytool.db.DatabaseHelper;
import tz.co.nezatech.apps.surveytool.db.model.DataType;
import tz.co.nezatech.apps.surveytool.db.model.Form;
import tz.co.nezatech.apps.surveytool.db.model.FormInstance;
import tz.co.nezatech.apps.surveytool.db.model.Setup;
import tz.co.nezatech.apps.surveytool.form.DataElem;
import tz.co.nezatech.apps.surveytool.util.Group;
import tz.co.nezatech.apps.surveytool.util.Input;
import tz.co.nezatech.apps.surveytool.util.Instance;

class SurveyFormImpl extends FormViewImpl implements SurveyForm {
    private static final String TAG = SurveyFormImpl.class.getName();
    private DatabaseHelper databaseHelper;
    private Context context;
    private Form form;
    private FormInstance formInstance;
    private FormMode formMode = FormMode.NEW;

    SurveyFormImpl(Context context, DatabaseHelper databaseHelper, Form form) {
        this(context, databaseHelper, form, null);
    }

    SurveyFormImpl(Context context, DatabaseHelper databaseHelper, Form form, FormInstance formInstance) {
        this(context, databaseHelper, form, formInstance, FormMode.VIEW);
    }

    SurveyFormImpl(Context context, DatabaseHelper databaseHelper, Form form, FormInstance formInstance, FormMode formMode) {
        super(form, context);
        this.databaseHelper = databaseHelper;
        this.context = context;
        this.form = form;
        this.formInstance = formInstance;
        this.formMode = formMode;

        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new GsonJsonProvider();
            private final MappingProvider mappingProvider = new GsonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
    }


    private boolean checkFormInputsRegex(ViewGroup root) {
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                if (!checkFormInputsRegex((ViewGroup) child)) {
                    return false;
                }
            } else {
                if (child.getTag() != null && !child.getTag().toString().isEmpty()) {
                    if (child instanceof EditText) {
                        EditText et = (EditText) child;
                        if (!regexCheck(et, true)) {
                            Log.e(TAG, "checkFormInputsRegex->NOK: " + String.format("Value->%s, Tag->%s", ((EditText) child).getText(), child.getTag()));
                            return false;
                        } else {
                            Log.d(TAG, "checkFormInputsRegex->OK: " + String.format("Value->%s, Tag->%s", ((EditText) child).getText(), child.getTag()));
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean saveTheForm(ViewGroup root) {
        try {
            Log.d(TAG, String.format("Id: %d, Name: %s", form.getId(), form.getName()));

            if (checkFormInputsRegex(root)) {
                ArrayList<View> viewsWithTag = getViewsWithTag(root);

                Instance instance = new Instance(form.getFormId());
                List<Group> groups = new LinkedList<>();
                List<Input> inputs = null;
                Group group = null;
                String tmpl = form.getDisplay();
                for (View v : viewsWithTag) {
                    DataElem dataElem = dataFromView(v);

                    InputTag inputTag = InputTag.valueOf(dataElem.getCategory());
                    if (dataElem.getCategory().equals("GRP")) {
                        //check prev
                        if (group != null) {//first grp, ignore
                            group.setInputs(inputs);
                            groups.add(group);
                        }
                        group = new Group(dataElem.getCategory(), dataElem.getName(), dataElem.getType(), dataElem.getLabel());
                        inputs = new LinkedList<>();
                    } else if (inputs != null) {
                        inputs.add(new Input(inputTag.name(), dataElem.getName(), dataElem.getType(), dataElem.getValue(), dataElem.getLabel()));
                        Log.d(TAG, String.format("Tmpl: %s, Name: %s", tmpl, dataElem.getName()));
                        if (dataElem.getExtra() != null && tmpl.contains(dataElem.getName())) {
                            tmpl = tmpl.replaceAll(dataElem.getName() + ".Extra", dataElem.getExtra().toString());
                        } else if (dataElem.getValue() != null && tmpl.contains(dataElem.getName())) {
                            tmpl = tmpl.replaceAll(dataElem.getName(), dataElem.getValue().toString());
                        }
                    } else {
                        Log.d(TAG, String.format("Unhandled input %s", dataElem));
                    }
                }
                if (group != null) {//first grp, ignore
                    group.setInputs(inputs);
                    groups.add(group);
                }
                instance.setGroups(groups);

                try {
                    Gson gson = new Gson();
                    String json = gson.toJson(instance);
                    Log.d(TAG, "JSON: " + json);
                    Log.d(TAG, "Display: " + tmpl);

                    FormInstance newInstance = new FormInstance(form, json, tmpl);
                    if (formInstance != null) {
                        newInstance.setId(formInstance.getId());
                        databaseHelper.getFormInstanceDao().update(newInstance);
                    } else {
                        databaseHelper.getFormInstanceDao().create(newInstance);
                    }

                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "Valdation failed");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public FormMode getMode() {
        return this.formMode;
    }

    @Override
    public String jsonStr(JSONObject o, String key, String defaultValue) {
        try {
            return o.optString(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    @Override
    public void setFormGrpTag(JSONObject group, ViewGroup form) {
        String grpName = jsonStr(group, "name", "");
        String type = jsonStr(group, "type", "Text");
        form.setTag("GRP:" + grpName + ":" + type);
    }

    @Override
    public void setInputTag(JSONObject input, View inputView) {
        setInputTag(input, inputView, null);
    }

    @Override
    public void setInputTag(JSONObject input, View inputView, InputTag tag) {
        String inpName = jsonStr(input, "name", "");
        String inpType = jsonStr(input, "type", "Text");
        if (tag != null) {
            inputView.setTag(String.format("%s:%s:%s", tag.name(), inpName, inpType));
        } else {
            inputView.setTag(String.format("%s:%s:%s", "INP", inpName, inpType));
        }
    }

    @Override
    public void setTextDataType(EditText text, String dataType) {
        if (dataType == null) return;
        switch (dataType) {
            case "PhoneNumber":
                text.setInputType(InputType.TYPE_CLASS_PHONE);
                break;
            default:
                text.setInputType(InputType.TYPE_CLASS_TEXT);
        }
    }

    @Override
    public void doGenericInputGroup(LayoutInflater inflater, LinearLayout gropusLayout, JSONObject group) throws JSONException {
        JSONArray inputs = (JSONArray) group.get("inputs");

        LinearLayout form = (LinearLayout) inflater.inflate(R.layout.form_layout, gropusLayout, false);
        setFormGrpTag(group, form);

        form.setOrientation(LinearLayout.VERTICAL);
        gropusLayout.addView(form);

        for (int i = 0; i < inputs.length(); i++) {
            try {
                JSONObject input = (JSONObject) inputs.get(i);
                String dataType = jsonStr(input, "type", null);
                String name = jsonStr(input, "name", null);
                Log.d(TAG, String.format("Name: %s, Type: %s", name, dataType));

                TextView label = (TextView) inflater.inflate(R.layout.form_label, form, false);
                label.setText(input.getString("label"));
                form.addView(label);

                String setupsRegex = "^Setup\\.(\\w+)$";
                String otherSelectRegex = "^Select\\[([ a-zA-Z0-9,;()/]+)]$";
                String otherMultiSelectRegex = "^MultiSelect\\[([ a-zA-Z0-9,;()/]+)]$";

                Pattern pattern = Pattern.compile(setupsRegex);
                Matcher matcher = pattern.matcher(dataType == null ? "" : dataType);

                Pattern patternOSel = Pattern.compile(otherSelectRegex);
                Matcher matcherOSel = patternOSel.matcher(dataType == null ? "" : dataType);

                Pattern patternOMultiSel = Pattern.compile(otherMultiSelectRegex);
                Matcher matcherOMultiSel = patternOMultiSel.matcher(dataType == null ? "" : dataType);

                if (dataType != null && matcher.find()) {
                    inputSetupSpinner(inflater, group, form, input, dataType, name, matcher);
                } else if (dataType != null && (matcherOSel.find())) {
                    inputSingleSelect(inflater, group, form, input, dataType, name, matcherOSel);
                } else if (dataType != null && (matcherOMultiSel.find())) {
                    inputMultiSelect(inflater, group, form, input, dataType, name, matcherOMultiSel);
                } else {
                    inputText(inflater, group, form, input, dataType, name);
                }

                LinearLayout vspace1 = (LinearLayout) inflater.inflate(R.layout.form_input_vseparator, form, false);
                form.addView(vspace1);
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void inputText(LayoutInflater inflater, JSONObject group, LinearLayout form, JSONObject input, String dataType, String name) {
        Log.d(TAG, "doTextInput");
        final EditText text = (EditText) inflater.inflate(R.layout.form_input_text, form, false);
        //String name = jsonStr(input, "name", null);
        text.setTag(name);
        if (formInstance != null) {
            TypeRef<List<String>> typeRef = new TypeRef<List<String>>() {
            };
            String path = "$.groups[?(@.name == '" + jsonStr(group, "name", null) + "')].inputs[?(@.name == '" + name + "')].value";
            Log.d(TAG, String.format("inputText=> %s = %s", name, path));
            List<String> v = JsonPath.parse(formInstance.getJson()).read(path, typeRef);
            text.setText(v.isEmpty() ? "" : v.get(0));
            text.setEnabled(!(getMode() == FormMode.VIEW));
        }
        setTextDataType(text, dataType);
        if (dataType != null && dataType.equals("TextArea")) {
            text.setSingleLine(false);
            text.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        }
        form.addView(text);
        setInputTag(input, text);
    }

    private void inputMultiSelect(LayoutInflater inflater, JSONObject group, LinearLayout form, JSONObject input, String dataType, String name, Matcher matcherOMultiSel) {
        Log.d(TAG, "Multiselect: " + dataType);
        String setupType = matcherOMultiSel.group(1);
        Log.d(TAG, "DataType: " + setupType);
        List<String> extras = null;
        if (setupType.contains(";")) {
            String[] parts = setupType.split(";");
            setupType = parts[0];
            extras = Arrays.asList(parts[1].split(","));
        }
        List<DataType> values = new LinkedList<>();
        List<ExtraSelectInput> checkBoxInputs = null;
        try {
            //List<DataType> dataTypes = databaseHelper.getDataTypeDao().queryForEq("type", setupType);
            QueryBuilder<DataType, String> qb = databaseHelper.getDataTypeDao().queryBuilder();
            qb.where().eq("type", setupType);
            qb.orderBy("position", true);
            List<DataType> dataTypes = qb.query();

            if (formInstance != null) {
                String path = "$.groups[?(@.name == '" + jsonStr(group, "name", null) + "')].inputs[?(@.name == '" + name + "')].value";
                TypeRef<List<List<ExtraSelectInput>>> typeRef = new TypeRef<List<List<ExtraSelectInput>>>() {
                };
                List<List<ExtraSelectInput>> v = JsonPath.parse(formInstance.getJson()).read(path, typeRef);
                try {
                    Log.d(TAG, String.format("inputMultiSelect=> %s = %s", name, v));
                    checkBoxInputs = v.get(0);
                    for (ExtraSelectInput cbIn : checkBoxInputs) {
                        values.add(databaseHelper.getDataTypeDao().queryForId(cbIn.getName()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            Log.d(TAG, String.format("inputMultiSelect=> %s = %s", name, values));


            LinearLayout checkLayout = (LinearLayout) inflater.inflate(R.layout.form_input_layout_checkgroup, form, false);
            LinearLayout formCheckGroup = (LinearLayout) checkLayout.findViewById(R.id.formCheckGroup);
            formCheckGroup.removeAllViews();

            final LinearLayout layoutLabels = (LinearLayout) inflater.inflate(R.layout.form_input_checkbox_narrate_labels, form, false);
            final LinearLayout extraLayoutLabels = (LinearLayout) layoutLabels.findViewById(R.id.checkBox1ExtraLabels);
            extraLayoutLabels.removeAllViews();
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            final boolean thereAreExtras = extras != null && extras.size() > 0;
            if (thereAreExtras) {
                extraLayoutLabels.setWeightSum(extras.size());

                for (String extra : extras) {
                    EditText text = (EditText) inflater.inflate(R.layout.form_input_checkextra_text, form, false);
                    //EditText text = new EditText(context);
                    text.setHint(extra);
                    text.setLayoutParams(params);
                    text.setEnabled(false);
                    extraLayoutLabels.addView(text);
                }
            }
            formCheckGroup.addView(layoutLabels);


            for (final DataType dt : dataTypes) {
                boolean otherSpecify = dt.getName().equalsIgnoreCase(dt.getType() + context.getString(R.string.constTextOthersSpecify));
                final LinearLayout layout = (LinearLayout) inflater.inflate(otherSpecify ? R.layout.form_input_checkbox_narrate_otherspecify : R.layout.form_input_checkbox_narrate, null);
                final LinearLayout extraLayout = (LinearLayout) layout.findViewById(R.id.checkBox1Extra);
                extraLayout.removeAllViews();
                ExtraSelectInput boxInput = null;


                if (thereAreExtras) {
                    extraLayout.setWeightSum(extras.size());


                    if (formInstance != null && checkBoxInputs != null) {
                        for (ExtraSelectInput input1 : checkBoxInputs) {
                            if (input1.getName().equals(dt.getName())) {
                                boxInput = input1;
                                Log.d(TAG, String.format(Locale.ENGLISH, "inputMultiSelect=> %s, %s", name, boxInput));
                                break;
                            }
                        }
                    }

                    int i = -1;
                    for (String extra : extras) {
                        EditText text = (EditText) inflater.inflate(R.layout.form_input_checkextra_text, form, false);
                        //EditText text = new EditText(context);
                        text.setHint(extra);
                        text.setLayoutParams(params);
                        i++;
                        if (boxInput != null && boxInput.getExtras().size() > i) {
                            text.setText(boxInput.getExtras().get(i));
                        }
                        text.setEnabled(!(getMode() == FormMode.VIEW));
                        extraLayout.addView(text);
                    }
                }
                final CheckBox cb = (CheckBox) layout.findViewById(R.id.checkBox1);

                cb.setChecked(values.contains(dt));
                cb.setEnabled(!(getMode() == FormMode.VIEW));


                final EditText otherSp = (EditText) layout.findViewById(R.id.checkOtherText);
                if (otherSpecify) {
                    cb.setText(R.string.formCheckBoxOtherSpecifyNewLabel);
                    cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            extraLayout.setVisibility(isChecked && thereAreExtras ? View.VISIBLE : View.GONE);
                            layout.requestFocus();
                            otherSp.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                        }
                    });
                    cb.setTag("IGNORE:" + dt.getType());
                    otherSp.setVisibility(cb.isChecked() ? View.VISIBLE : View.GONE);
                    otherSp.setEnabled(!(getMode() == FormMode.VIEW));
                    if (boxInput != null) {
                        otherSp.setText(boxInput.getNameOther());
                    }
                } else {
                    cb.setText(dt.getName());
                    cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            extraLayout.setVisibility(isChecked && thereAreExtras ? View.VISIBLE : View.GONE);
                            layout.requestFocus();
                        }
                    });
                }
                //cb.setChecked(values != null && dt.equals(values));
                extraLayout.setVisibility(cb.isChecked() ? View.VISIBLE : View.GONE);

                formCheckGroup.addView(layout);
            }

            //ArrayAdapter<DataType> myAdapter = new ArrayAdapter<DataType>(context, R.layout.form_spinner_row, dataTypes);
            //spinner.setAdapter(myAdapter);


            //checkLayout.setTag(name);

            form.addView(checkLayout);
            setInputTag(input, checkLayout, InputTag.INPDTPMUL);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    private void inputSingleSelect(LayoutInflater inflater, JSONObject group, LinearLayout form, JSONObject input, String dataType, String name, Matcher matcherOSel) {
        Log.d(TAG, "Single select: " + dataType);
        String setupType = matcherOSel.group(1);
        try {
            //List<DataType> dataTypes = databaseHelper.getDataTypeDao().queryForEq("type", setupType);
            QueryBuilder<DataType, String> qb = databaseHelper.getDataTypeDao().queryBuilder();
            qb.where().eq("type", setupType);
            qb.orderBy("position", true);
            List<DataType> dataTypes = qb.query();
            DataType value = null;
            SelectInput radioGroupInput = null;
            if (formInstance != null) {
                try {
                    TypeRef<List<SelectInput>> typeRef = new TypeRef<List<SelectInput>>() {
                    };
                    String path = "$.groups[?(@.name == '" + jsonStr(group, "name", null) + "')].inputs[?(@.name == '" + name + "')].value";
                    List<SelectInput> v = JsonPath.parse(formInstance.getJson()).read(path, typeRef);

                    try {

                        radioGroupInput = v.get(0);
                        value = databaseHelper.getDataTypeDao().queryForId(radioGroupInput.getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            final LinearLayout radioLayout = (LinearLayout) inflater.inflate(R.layout.form_input_layout_radiogroup, form, false);
            RadioGroup radioGroup = (RadioGroup) radioLayout.findViewById(R.id.formRadioGroup);
            radioGroup.removeAllViews();
            int i = -1;
            SecureRandom random = new SecureRandom();
            int base = random.nextInt(999999);
            for (DataType dt : dataTypes) {
                i++;
                boolean found = value != null && dt.equals(value);
                boolean otherSpecify = dt.getName().equalsIgnoreCase(dt.getType() + context.getString(R.string.constTextOthersSpecify));
                RadioButton rb = new RadioButton(context);
                if (dt.getName().equalsIgnoreCase(dt.getType() + "OthersSpecify")) {
                    rb.setText(context.getString(R.string.formRadioGroupOtherSpecifyNewLabel));
                } else {
                    rb.setText(dt.getName());
                }
                rb.setChecked(found);
                rb.setEnabled(!(getMode() == FormMode.VIEW));
                rb.setId(base + i);

                final LinearLayout l = (LinearLayout) radioLayout.findViewById(R.id.otherSpecifyLayout);
                if (otherSpecify) {
                    rb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            l.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                            radioLayout.requestFocus();
                        }
                    });
                    rb.setTag("IGNORE:" + dt.getType());

                    EditText otherSp = (EditText) l.findViewById(R.id.checkOtherText);
                    otherSp.setEnabled(!(getMode() == FormMode.VIEW));
                    if (found) {
                        otherSp.setText(radioGroupInput.getNameOther());
                        l.setVisibility(View.VISIBLE);
                    } else {
                        l.setVisibility(rb.isChecked() ? View.VISIBLE : View.GONE);
                    }
                } else {
                    rb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            radioLayout.requestFocus();
                        }
                    });
                    l.setVisibility(View.GONE);
                }
                radioGroup.addView(rb);
            }

            //ArrayAdapter<DataType> myAdapter = new ArrayAdapter<DataType>(context, R.layout.form_spinner_row, dataTypes);
            //spinner.setAdapter(myAdapter);


            //radioLayout.setTag(name);

            form.addView(radioLayout);
            setInputTag(input, radioLayout, InputTag.INPDTP);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    private void inputSetupSpinner(LayoutInflater inflater, JSONObject group, LinearLayout form, JSONObject input, String dataType, String name, Matcher matcher) {
        Log.d(TAG, "doSpinnerInput: " + dataType);
        String setupType = matcher.group(1);
        try {
            List<Setup> setups = databaseHelper.getSetupDao().queryForEq("type", setupType);

            LinearLayout autoTVLayout = (LinearLayout) inflater.inflate(R.layout.form_input_autocomplete, form, false);
            final AutoCompleteTextView spinner = (AutoCompleteTextView) autoTVLayout.findViewById(R.id.autoTextViewSpinner);
            final EditText autoTextViewUuid = (EditText) autoTVLayout.findViewById(R.id.autoTextViewUuid);
            final EditText autoTextViewText = (EditText) autoTVLayout.findViewById(R.id.autoTextViewText);
            autoTextViewUuid.setVisibility(View.GONE);
            autoTextViewText.setVisibility(View.GONE);
            ArrayAdapter<Setup> myAdapter = new ArrayAdapter<>(context, android.R.layout.select_dialog_singlechoice, setups);
            spinner.setThreshold(1);
            spinner.setAdapter(myAdapter);

            if (getMode() == FormMode.VIEW) {
                spinner.dismissDropDown();
                spinner.clearFocus();
            } else {
                spinner.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean hasFocus) {
                    }
                });
                spinner.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        spinner.showDropDown();
                    }
                });

                spinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Setup s = (Setup) parent.getItemAtPosition(position);
                        Toast.makeText(context, String.format("Selected Item: %s=>%s", s.getUuid(), s.getName()), Toast.LENGTH_SHORT).show();
                        autoTextViewUuid.setText(s.getUuid());
                        autoTextViewText.setText(s.getName());
                    }
                });
                if (getMode() == FormMode.EDIT) {
                    spinner.dismissDropDown();
                    spinner.clearFocus();
                }
            }


            //spinner.setTag(name);
            if (formInstance != null) {
                Log.d(TAG, "Setup spinner(Editing...): " + name);
                TypeRef<List<String>> typeRef = new TypeRef<List<String>>() {
                };
                String path = "$.groups[?(@.name == '" + jsonStr(group, "name", null) + "')].inputs[?(@.name == '" + name + "')].value";
                List<String> v = JsonPath.parse(formInstance.getJson()).read(path, typeRef);
                Setup setup = null;
                try {
                    setup = databaseHelper.getSetupDao().queryForId(v.get(0));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (setup != null) {
                    int spinnerPosition = myAdapter.getPosition(setup);
                    Log.d(TAG, "Position: " + spinnerPosition);
                    //spinner.setSelection(spinnerPosition);
                    autoTextViewUuid.setText(setup.getUuid());
                    autoTextViewText.setText(setup.getName());
                    spinner.setText(setup.getName());
                    spinner.setEnabled(!(getMode() == FormMode.VIEW));
                    spinner.clearFocus();
                    spinner.dismissDropDown();
                } else {
                    Log.e(TAG, "Setup not found!");
                }
            } else {
                Log.d(TAG, "Setup spinner(New instance): " + name);
            }
            form.addView(autoTVLayout);
            setInputTag(input, autoTVLayout, InputTag.INPSTP);

            if (formInstance != null) {
                spinner.clearFocus();
                spinner.dismissDropDown();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public boolean regexCheck(EditText txt, boolean onSubmit) {
        try {
            Object tag = txt.getTag();
            String regex = null;
            String regexMessage = null;
            try {
                String name = tag.toString().split(":")[1];
                TypeRef<List<String>> typeRef = new TypeRef<List<String>>() {
                };
                String path = "$.groups[?(@.name == '" + name.split("\\.")[0] + "')].inputs[?(@.name == '" + name + "')].regex";
                List<String> rgx = JsonPath.parse(form.getJson()).read(path, typeRef);
                if (!rgx.isEmpty()) {
                    regex = rgx.get(0);
                    path = "$.groups[?(@.name == '" + name.split("\\.")[0] + "')].inputs[?(@.name == '" + name + "')].regexMessage";
                    List<String> rgxMsg = JsonPath.parse(form.getJson()).read(path, typeRef);
                    if (!rgxMsg.isEmpty()) {
                        regexMessage = rgxMsg.get(0);
                    } else {
                        regexMessage = "This field is reduired/should be valid";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "Exception: " + e.getMessage());
            }

            if (regex != null && !regex.isEmpty()) {
                String value = txt.getText().toString();
                Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
                if (p.matcher(value).find()) {
                    Log.d(TAG, "Validation-OK");
                    txt.setError(null);
                    return true;
                } else {
                    Log.e(TAG, "Validation-NOK: " + String.format("Value->%s, Regex->%s", value, regex));
                    txt.setError(regexMessage);

                    if (onSubmit) {
                        txt.setFocusableInTouchMode(true);
                        txt.requestFocus();
                    }
                }
            } else {
                Log.d(TAG, "No regex validation set!");
                return true; //no regex validation set
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Exception: " + e.getMessage());
        }
        return false;
    }

    public Form getForm() {
        return form;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    FormInstance getFormInstance() {
        return formInstance;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
