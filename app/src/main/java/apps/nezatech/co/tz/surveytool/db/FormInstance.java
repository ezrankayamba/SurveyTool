package apps.nezatech.co.tz.surveytool.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.util.Date;

import apps.nezatech.co.tz.surveytool.util.FormUtil;
import apps.nezatech.co.tz.surveytool.util.Listable;

/**
 * Created by nkayamba on 2/1/18.
 */
@DatabaseTable(tableName = "tbl_form_instance")
public class FormInstance implements Serializable, Listable {
    @DatabaseField(generatedId = true)
    int id;
    @DatabaseField(index = true, unique = true)
    String name;
    @DatabaseField(foreign = true, foreignAutoCreate = true, foreignAutoRefresh = true)
    Form form;
    @DatabaseField(columnName = "record_date")
    Date recordDate;
    @DatabaseField
    String json;

    public FormInstance() {
        recordDate = new Date();
    }

    public FormInstance(Form form, String json, String display) {
        this();
        this.name = FormUtil.formInstanceName(form, getRecordDate(), display);
        this.form = form;
        this.json = json;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Form getForm() {
        return form;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public Date getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(Date recordDate) {
        this.recordDate = recordDate;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
