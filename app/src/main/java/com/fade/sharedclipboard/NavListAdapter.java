package com.fade.sharedclipboard;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;


public class NavListAdapter extends ArrayAdapter<String> {

	private ArrayList<Integer> syncedData;
    private ArrayList<String> list;
    private SparseBooleanArray mSelectedItemsIds;

        //Constructor for adapter type, it takes in the context and an array
	NavListAdapter(Context context, ArrayList<String> list, ArrayList<Integer> syncedData) {
        super(context,R.layout.custom_row, list);
        //gets context, view to be changed and array passed in
        this.syncedData = syncedData;
        this.list = list;
        mSelectedItemsIds = new SparseBooleanArray();
    }

    private static class ViewHolder{

        ImageView imageView;
        TextView textEdit;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        //getView gets view used in list

        LayoutInflater inflated = LayoutInflater.from(getContext());
        //An Inflater is used for rendering and is needed

        final ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = inflated.inflate(R.layout.custom_row, parent, false);
            // Creates the view that will be used in the list.


            holder.textEdit = view.findViewById(R.id.textEdit);
            holder.imageView = view.findViewById(R.id.imageView);
            view.setTag(holder);
        }else{
            holder = (ViewHolder) view.getTag();
        }

        if(syncedData.get(position) == 1){
            holder.imageView.setVisibility(View.GONE);
        }else{
            holder.imageView.setVisibility(View.VISIBLE);
        }

        String holds = getItem(position);
        //gets item from array and stores it

        holder.textEdit.setText(holds);
        return view;
        //returns view made
    }

    public List<String> getList() {
        return list;
    }

    void toggleSelection(int position) {
        selectView(position, !mSelectedItemsIds.get(position));
    }

    void removeSelection() {
        mSelectedItemsIds = new SparseBooleanArray();
        notifyDataSetChanged();
    }

    private void selectView(int position, boolean value) {
        if (value)
            mSelectedItemsIds.put(position, true);
        else
            mSelectedItemsIds.delete(position);
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return mSelectedItemsIds.size();
    }

    SparseBooleanArray getSelectedIds() {
        return mSelectedItemsIds;
    }
}
