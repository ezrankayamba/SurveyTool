package tz.co.nezatech.apps.surveytool.db.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "tbl_data_type")
public class DataType {
    @DatabaseField(id = true)
    String name;
    @DatabaseField
    String type;
    @DatabaseField
    String lastUpdate;


    public DataType() {
        super();
    }

    public DataType(String name, String type, String lastUpdate) {
        super();
        this.name = name;
        this.type = type;
        this.lastUpdate = lastUpdate;
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

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public String toString() {
        return String.format("%s", getName());
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && ((DataType) obj).getName().equals(this.getName());
    }
}
