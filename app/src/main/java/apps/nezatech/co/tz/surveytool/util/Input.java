package apps.nezatech.co.tz.surveytool.util;

/**
 * Created by nkayamba on 2/3/18.
 */

public class Input {
    private String category, name, type, value;

    public Input(String category, String name, String type, String value) {
        this.category = category;
        this.name = name;
        this.type = type;
        this.value = value;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
