package tz.co.nezatech.apps.surveytool.db.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import tz.co.nezatech.apps.surveytool.util.FormUtil;
import tz.co.nezatech.apps.surveytool.util.Listable;


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
    @DatabaseField(index = true, unique = true)
    UUID uuid = UUID.randomUUID();
    @DatabaseField
    int status = 0;

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

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return this.name + ", " + this.status + ", " + this.uuid + ", " + this.recordDate;
    }

    @Override
    public String searchableText() {
        return toString() + this.getJson();
    }
}
