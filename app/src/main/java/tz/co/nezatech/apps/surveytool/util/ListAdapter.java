package tz.co.nezatech.apps.surveytool.util;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nkayamba on 2/3/18.
 */


public abstract class ListAdapter<L extends Listable> extends ArrayAdapter<L> implements View.OnClickListener {


    private static final String TAG = ListAdapter.class.getName();
    List<L> dataSet;
    List<L> dataSetOrg;
    private int lastPosition = -1;

    public ListAdapter(Context context, List<L> dataSet) {
        super(context, 0, dataSet);
        this.dataSet = dataSet;
        this.dataSetOrg = dataSet;
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

    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return dataSet.size();
    }

    @Override
    public L getItem(int position) {
        return dataSet.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults oReturn = new FilterResults();
                final ArrayList<L> results = new ArrayList<L>();
                if (constraint != null) {
                    if (dataSetOrg != null && dataSetOrg.size() > 0) {
                        for (final L e : dataSetOrg) {
                            if (e.searchableText().toLowerCase()
                                    .contains(constraint.toString()))
                                results.add(e);
                        }
                    }
                    oReturn.values = results;
                }
                return oReturn;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint,
                                          FilterResults results) {
                dataSet = (ArrayList<L>) results.values;
                notifyDataSetChanged();
                Log.d(TAG, "Set changed");
            }
        };
    }

    protected abstract void makeRowView(View v, L p);

    protected abstract int getRowLayoutResource();

    private class AppFilter<T extends Searchable> extends Filter {

        private List<T> sourceObjects;

        public AppFilter(List<T> objects) {
            sourceObjects = new ArrayList<T>();
            synchronized (this) {
                sourceObjects.addAll(objects);
            }
        }

        @Override
        protected FilterResults performFiltering(CharSequence chars) {
            String filterSeq = chars.toString().toLowerCase();
            FilterResults result = new FilterResults();
            if (filterSeq != null && filterSeq.length() > 0) {
                ArrayList<T> filter = new ArrayList<T>();

                for (T object : sourceObjects) {
                    // the filtering itself:
                    if (object.searchableText().toLowerCase().contains(filterSeq))
                        filter.add(object);
                }
                result.count = filter.size();
                result.values = filter;
            } else {
                // add all objects
                synchronized (this) {
                    result.values = sourceObjects;
                    result.count = sourceObjects.size();
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            // NOTE: this function is *always* called from the UI thread.
            ArrayList<T> filtered = (ArrayList<T>) results.values;
            notifyDataSetChanged();
            clear();
            for (int i = 0, l = filtered.size(); i < l; i++)
                add((L) filtered.get(i));
            notifyDataSetInvalidated();
        }
    }
}
