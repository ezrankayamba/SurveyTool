package tz.co.nezatech.apps.surveytool.form.util;

import java.util.List;

/**
 * Created by nkayamba on 3/30/18.
 */

public class CheckBoxInput {
    private String name;
    private String nameOther;
    private List<String> extras;

    public CheckBoxInput() {
    }

    public CheckBoxInput(String name, List<String> extras) {
        this.name = name;
        this.extras = extras;
    }

    public CheckBoxInput(String name, List<String> extras, String nameOther) {
        this.name = name;
        this.extras = extras;
        this.nameOther = nameOther;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getExtras() {
        return extras;
    }

    public void setExtras(List<String> extras) {
        this.extras = extras;
    }

    public String getNameOther() {
        return nameOther;
    }

    public void setNameOther(String nameOther) {
        this.nameOther = nameOther;
    }

    @Override
    public String toString() {
        return getNameOther() == null ? getName() : String.format("%s => %s", getName(), getNameOther());
    }
}
