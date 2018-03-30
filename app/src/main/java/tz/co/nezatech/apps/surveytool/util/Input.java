package tz.co.nezatech.apps.surveytool.util;

/**
 * Created by nkayamba on 2/3/18.
 */

public class Input {
    private String category, name, type, label;
    private Object value;

    public Input(String category, String name, String type, Object value, String label) {
        this.category = category;
        this.name = name;
        this.type = type;
        this.value = value;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
