package apps.nezatech.co.tz.surveytool.util;

import java.util.List;

/**
 * Created by nkayamba on 2/3/18.
 */

public class Instance {
    private int formId;
    private List<Group> groups;

    public Instance(int formId) {
        this.formId = formId;
    }

    public Instance(int formId, List<Group> groups) {
        this.formId = formId;
        this.groups = groups;
    }

    public int getFormId() {
        return formId;
    }

    public void setFormId(int formId) {
        this.formId = formId;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }
}
