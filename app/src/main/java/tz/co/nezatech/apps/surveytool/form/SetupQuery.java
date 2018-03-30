package tz.co.nezatech.apps.surveytool.form;

public class SetupQuery {
    String type, lastUpdate;

    public SetupQuery() {
        super();
    }

    public SetupQuery(String type, String lastUpdate) {
        super();
        this.type = type;
        this.lastUpdate = lastUpdate;
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

}
