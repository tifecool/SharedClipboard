package com.fade.sharedclipboard;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ThrashListAdapter extends ArrayAdapter<String> {

	private ArrayList<String> list;
	private SparseBooleanArray mSelectedItemsIds;

	ThrashListAdapter(@NonNull Context context, @NonNull ArrayList<String> list) {
		super(context, R.layout.simple_list_item_1_checkable, list);

		this.list = list;
		mSelectedItemsIds = new SparseBooleanArray();
	}

	private static class ViewHolder{

		TextView textEdit;
	}

	@NonNull
	@Override
	public View getView(int position, View view, @NonNull ViewGroup parent) {
		//getView gets view used in list

		LayoutInflater inflated = LayoutInflater.from(getContext());
		//An Inflater is used for rendering and is needed

		final ThrashListAdapter.ViewHolder holder;
		if (view == null) {
			holder = new ThrashListAdapter.ViewHolder();
			view = inflated.inflate(R.layout.simple_list_item_1_checkable, parent, false);
			// Creates the view that will be used in the list.


			holder.textEdit = view.findViewById(R.id.text1);
			view.setTag(holder);
		}else{
			holder = (ThrashListAdapter.ViewHolder) view.getTag();
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
