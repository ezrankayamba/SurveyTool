package tz.co.nezatech.apps.surveytool.summary;

import java.util.List;

import tz.co.nezatech.apps.surveytool.util.Listable;

/**
 * Created by nkayamba on 2/11/18.
 */

public class SummaryGroup implements Listable {
    String groupLabel;
    List<SummaryLine> summaryLines;

    public SummaryGroup() {
    }

    public SummaryGroup(String groupLabel, List<SummaryLine> summaryLines) {
        this.groupLabel = groupLabel;
        this.summaryLines = summaryLines;
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public void setGroupLabel(String groupLabel) {
        this.groupLabel = groupLabel;
    }

    public List<SummaryLine> getSummaryLines() {
        return summaryLines;
    }

    public void setSummaryLines(List<SummaryLine> summaryLines) {
        this.summaryLines = summaryLines;
    }

    public void add(SummaryLine e) {
        getSummaryLines().add(e);
    }

    @Override
    public String searchableText() {
        return "";
    }
}
