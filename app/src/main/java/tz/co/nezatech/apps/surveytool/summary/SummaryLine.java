package tz.co.nezatech.apps.surveytool.summary;

import com.j256.ormlite.field.DatabaseField;

import tz.co.nezatech.apps.surveytool.util.Listable;

/**
 * Created by nkayamba on 2/11/18.
 */

public class SummaryLine implements Listable {
    @DatabaseField
    int syncedCount;
    @DatabaseField
    int unsyncedCount;
    @DatabaseField
    int total;
    @DatabaseField
    String date;

    public SummaryLine() {
    }

    public SummaryLine(int syncedCount, int unsyncedCount, String date) {
        this.syncedCount = syncedCount;
        this.unsyncedCount = unsyncedCount;
        this.date = date;
    }

    public int getSyncedCount() {
        return syncedCount;
    }

    public void setSyncedCount(int syncedCount) {
        this.syncedCount = syncedCount;
    }

    public int getUnsyncedCount() {
        return unsyncedCount;
    }

    public void setUnsyncedCount(int unsyncedCount) {
        this.unsyncedCount = unsyncedCount;
    }

    public int getTotal() {
        return this.getSyncedCount() + this.getUnsyncedCount();
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return this.getDate() + ": " + getTotal();
    }

    @Override
    public String searchableText() {
        return this.getDate();
    }

}
