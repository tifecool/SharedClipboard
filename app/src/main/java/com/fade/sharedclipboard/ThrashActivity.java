package com.fade.sharedclipboard;

import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class ThrashActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_thrash);

		final ListView listView = findViewById(R.id.thrashList);

		final SQLiteDatabase database = this.openOrCreateDatabase(MainActivity.SQL_DATABASE_NAME, MODE_PRIVATE, null);

		final ThrashListAdapter arrayAdapter = new ThrashListAdapter(this, MainActivity.dSavedClipTitles);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
				new AlertDialog.Builder(ThrashActivity.this)
						.setTitle(R.string.alert)
						.setIcon(R.drawable.warning_bright)
						.setMessage(R.string.restore_delete)
						.setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								SQLiteStatement statement = database.compileStatement("UPDATE DeletedClips SET Deleted = 1 WHERE id = ?");

								statement.bindString(1, MainActivity.dSavedClipID.get(position));
								statement.execute();

								MainActivity.dSavedClipID.remove(position);
								MainActivity.dSavedClipContents.remove(position);
								MainActivity.dSavedClipTitles.remove(position);
								MainActivity.dSyncedBoolean.remove(position);
								MainActivity.dUnixTime.remove(position);

								arrayAdapter.notifyDataSetChanged();

							}
						})
						.setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {

								SQLiteStatement restoreStatement =
										database.compileStatement("UPDATE DeletedClips SET Deleted = 1 WHERE id = ?");
								SQLiteStatement insertStatement =
										database.compileStatement("INSERT INTO SavedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced) VALUES (? , ? , ?, 0, 0)");

								String id = UUID.randomUUID().toString();

								//Added to saved table and arrays
								MainActivity.savedClipID.add(id);
								insertStatement.bindString(1, id);
								MainActivity.savedClipTitles.add(MainActivity.dSavedClipTitles.get(position));
								insertStatement.bindString(2, MainActivity.dSavedClipTitles.get(position));
								MainActivity.savedClipContents.add(MainActivity.dSavedClipContents.get(position));
								insertStatement.bindString(3, MainActivity.dSavedClipContents.get(position));
								MainActivity.unixTime.add((long) 0);
								MainActivity.syncedBoolean.add(0);
								insertStatement.execute();

								restoreStatement.bindString(1, MainActivity.dSavedClipID.get(position));
								restoreStatement.execute();

								MainActivity.dSavedClipTitles.remove(position);
								MainActivity.dSavedClipContents.remove(position);
								MainActivity.dSavedClipID.remove(position);
								MainActivity.dSyncedBoolean.remove(position);
								MainActivity.dUnixTime.remove(position);

								arrayAdapter.notifyDataSetChanged();


							}
						})
						.show();

			}
		});

		listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

				// Capture total checked items
				final int checkedCount = listView.getCheckedItemCount();
				// Set the CAB title according to total checked items
				mode.setTitle(checkedCount + " Selected");
				// Calls toggleSelection method from ListViewAdapter Class
				arrayAdapter.toggleSelection(position);

			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				mode.getMenuInflater().inflate(R.menu.thrash_list_multiselect_menu, menu);
				findViewById(R.id.backButton).setVisibility(View.GONE);

				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
				//Code for deleting
				switch (item.getItemId()) {
					case R.id.thrashDelete:

						new AlertDialog.Builder(ThrashActivity.this)
								.setTitle(R.string.delete_conf)
								.setMessage(R.string.delete_conf_message)
								.setNegativeButton(R.string.cancel, null)
								.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {

										SQLiteStatement statement = database.compileStatement("UPDATE DeletedClips SET Deleted = 1 WHERE id = ?");
										// Calls getSelectedIds method from ListViewAdapter Class
										SparseBooleanArray selected = arrayAdapter.getSelectedIds();
										// Captures all selected ids with a loop
										for (int i = (selected.size() - 1); i >= 0; i--) {
											if (selected.valueAt(i)) {
												// Remove selected items following the ids
												int s = selected.keyAt(i);

												statement.bindString(1, MainActivity.dSavedClipID.get(s));
												statement.execute();

												MainActivity.dSavedClipID.remove(s);
												MainActivity.dSavedClipContents.remove(s);
												MainActivity.dSavedClipTitles.remove(s);
												MainActivity.dSyncedBoolean.remove(s);
												MainActivity.dUnixTime.remove(s);

												arrayAdapter.notifyDataSetChanged();

											}
										}
										// Close CAB
										mode.finish();
									}
								}).show();
						return true;

					case R.id.thrashRestore:

						SQLiteStatement restoreStatement =
								database.compileStatement("UPDATE DeletedClips SET Deleted = 1 WHERE id = ?");
						SQLiteStatement insertStatement =
								database.compileStatement("INSERT INTO SavedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced) VALUES (? , ? , ?, 0, 0)");

						// Calls getSelectedIds method from ListViewAdapter Class
						SparseBooleanArray selected = arrayAdapter.getSelectedIds();
						// Captures all selected ids with a loop
						for (int i = (selected.size() - 1); i >= 0; i--) {
							if (selected.valueAt(i)) {

								int s = selected.keyAt(i);

								String id = UUID.randomUUID().toString();

								//Added to saved table and arrays
								MainActivity.savedClipID.add(id);
								insertStatement.bindString(1, id);
								MainActivity.savedClipTitles.add(MainActivity.dSavedClipTitles.get(s));
								insertStatement.bindString(2, MainActivity.dSavedClipTitles.get(s));
								MainActivity.savedClipContents.add(MainActivity.dSavedClipContents.get(s));
								insertStatement.bindString(3, MainActivity.dSavedClipContents.get(s));
								MainActivity.unixTime.add((long) 0);
								MainActivity.syncedBoolean.add(0);
								insertStatement.execute();

								restoreStatement.bindString(1, MainActivity.dSavedClipID.get(s));
								restoreStatement.execute();

								MainActivity.dSavedClipTitles.remove(s);
								MainActivity.dSavedClipContents.remove(s);
								MainActivity.dSavedClipID.remove(s);
								MainActivity.dSyncedBoolean.remove(s);
								MainActivity.dUnixTime.remove(s);

								arrayAdapter.notifyDataSetChanged();

							}
						}
						// Close CAB
						mode.finish();

						return true;
					default:
						return false;

				}
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				arrayAdapter.removeSelection();
				findViewById(R.id.backButton).setVisibility(View.VISIBLE);

			}
		});

		listView.setAdapter(arrayAdapter);
	}

	public void backClicked(View view) {
		onBackPressed();
	}
}
