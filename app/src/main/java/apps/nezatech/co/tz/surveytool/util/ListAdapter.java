package apps.nezatech.co.tz.surveytool.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Created by nkayamba on 2/3/18.
 */


public abstract class ListAdapter<L extends Listable> extends ArrayAdapter<L> implements View.OnClickListener {


    private int lastPosition = -1;

    public ListAdapter(Context context, List<L> dataSet) {
        super(context, 0, dataSet);
    }

    @Override
    public void onClick(View v) {

        int position = (Integer) v.getTag();
        Object object = getItem(position);
        L dataModel = (L) object;

        handleRowClick(position, dataModel);
    }

    protected abstract void handleRowClick(int position, L dataModel);

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        L dataModel = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(getRowLayoutResource(), parent, false);
        }

        View v = convertView;
        L entity = getItem(position);
        if (v != null && entity != null) {
            v.setOnClickListener(this);
            v.setTag(position);
            makeRowView(v, entity);
        }

        return v;
    }

    protected abstract void makeRowView(View v, L p);

    protected abstract int getRowLayoutResource();

}
