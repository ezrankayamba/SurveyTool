package tz.co.nezatech.apps.surveytool.util;

import java.util.List;

/**
 * Created by nkayamba on 2/3/18.
 */

public class Group {
    private String category, name, type, label;
    private List<Input> inputs;

    public Group(String category, String name, String type, String label) {
        this.category = category;
        this.name = name;
        this.type = type;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<Input> getInputs() {
        return inputs;
    }

    public void setInputs(List<Input> inputs) {
        this.inputs = inputs;
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
}
