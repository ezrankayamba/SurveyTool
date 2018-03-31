package tz.co.nezatech.apps.surveytool.form.util;

import java.util.List;

/**
 * Created by nkayamba on 3/30/18.
 */

public class ExtraSelectInput extends SelectInput {
    private List<String> extras;

    public ExtraSelectInput() {
    }

    public ExtraSelectInput(String name, List<String> extras) {
        this(name, extras, null);
    }

    public ExtraSelectInput(String name, List<String> extras, String nameOther) {
        super(name, nameOther);
        this.extras = extras;
    }

    public List<String> getExtras() {
        return extras;
    }

    public void setExtras(List<String> extras) {
        this.extras = extras;
    }

    @Override
    public String toString() {
        return getNameOther() == null ? getName() : String.format("%s => %s", getName(), getNameOther());
    }
}
