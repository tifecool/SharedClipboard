package com.fade.sharedclipboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;


public class CustomListAdapter extends ArrayAdapter<String> {

    //Constructor for adapter type, it takes in the context and an array
	CustomListAdapter(Context context, ArrayList<String> list) {
        super(context,R.layout.custom_row, list);
        //gets context, view to be changed and array passed in
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //getView get's view used in list
        LayoutInflater inflated = LayoutInflater.from(getContext());
        //An Inflater is used for rendering and is needed
        View customView = inflated.inflate(R.layout.custom_row, parent, false);
        // Creates the view that will be used in the list.

        String holds = getItem(position);
        //gets item from array and stores it
        TextView textEdit = customView.findViewById(R.id.textEdit);

        textEdit.setText(holds);
        return customView;
        //returns view made
    }
}
