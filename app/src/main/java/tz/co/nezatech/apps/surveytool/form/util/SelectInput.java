package tz.co.nezatech.apps.surveytool.form.util;

/**
 * Created by nkayamba on 3/30/18.
 */

public class SelectInput {
    private String name;
    private String nameOther;

    public SelectInput() {
    }

    public SelectInput(String name) {
        this.name = name;
    }

    public SelectInput(String name, String nameOther) {
        this.name = name;
        this.nameOther = nameOther;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
