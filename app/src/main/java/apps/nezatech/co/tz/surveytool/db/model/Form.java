package apps.nezatech.co.tz.surveytool.db.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

/**
 * Created by nkayamba on 2/1/18.
 */
@DatabaseTable(tableName = "tbl_form")
public class Form implements Serializable {
    @DatabaseField(generatedId = true)
    int id;
    @DatabaseField(index = true, unique = true, columnName = "form_id")
    int formId;
    @DatabaseField
    String name;
    @DatabaseField
    String description;
    @DatabaseField
    String json;
    @DatabaseField
    String display;

    public Form() {
    }

    public Form(int formId, String name, String description, String json, String display) {
        this.formId = formId;
        this.name = name;
        this.description = description;
        this.json = json;
        this.display = display;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFormId() {
        return formId;
    }

    public void setFormId(int formId) {
        this.formId = formId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }
}
