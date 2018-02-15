package tz.co.nezatech.apps.surveytool.form;

/**
 * Created by nkayamba on 2/3/18.
 */

public class DataElem {
    private String name, type, category, value, label;
    private boolean success = true;

    public DataElem() {
    }

    public DataElem(String name, String type, String category, String value, String label) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.value = value;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

