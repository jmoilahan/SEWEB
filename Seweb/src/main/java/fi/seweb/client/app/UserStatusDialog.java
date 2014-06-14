package fi.seweb.client.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import fi.seweb.R;
import fi.seweb.client.common.PresenceStatus;
import fi.seweb.client.common.SewebPreferences;

public class UserStatusDialog extends AlertDialog {

	private final Spinner mStatus;
	private final EditText mMessage;
	private final BuddyListView mContext;

	public UserStatusDialog(final BuddyListView buddyListView) {
		super(buddyListView);

		mContext = buddyListView;
		
		// retrieve "old" status and the message
		String oldStatusMessage = buddyListView.getCurrentStatusMessage();
		int oldPresenceCode = buddyListView.getCurrentPresenceCode();
		
		LayoutInflater inflater = (LayoutInflater) buddyListView
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View group = inflater.inflate(R.layout.status_dialog, null, false);

		List<PresenceStatus> modes = new ArrayList<PresenceStatus>(
				Arrays.asList(PresenceStatus.values()));

		Collections.sort(modes, new Comparator<PresenceStatus>() {
			public int compare(PresenceStatus object1, PresenceStatus object2) {
				return object2.compareTo(object1);
			}
		});
		
		mStatus = (Spinner) group.findViewById(R.id.statusDialogSpinner);
		PresenceStatusAdapter mStatusAdapter;
		mStatusAdapter = new PresenceStatusAdapter(buddyListView, android.R.layout.simple_spinner_item, modes);
		mStatusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mStatus.setAdapter(mStatusAdapter);

		// retrieving the index of the "old" status
		int index = -1;
		for (PresenceStatus s : modes) {
			if (s.ordinal() == oldPresenceCode) {
				index = modes.indexOf(s);
			}
		}
		// setting the default value
		if (index >= 0) {
			mStatus.setSelection(index);
		}
		
		mMessage = (EditText) group.findViewById(R.id.statusDialogMessage);
		if (oldStatusMessage != null && oldStatusMessage.length() != 0) {
			mMessage.setText(oldStatusMessage);
		}
		
		setTitle(R.string.setStatusTitle);
		setView(group);

		setButton(BUTTON_POSITIVE, buddyListView.getString(android.R.string.ok),
				new OkListener());

		setButton(BUTTON_NEGATIVE, buddyListView.getString(android.R.string.cancel),
				(OnClickListener) null);
	}

	private class OkListener implements OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			PresenceStatus status = (PresenceStatus) mStatus.getSelectedItem();
			String message = mMessage.getText().toString();
			mContext.setAndSaveStatus(status, message, SewebPreferences.DEFAULT_STATUS_PRIORITY);
		}
	}

	private class PresenceStatusAdapter extends ArrayAdapter<PresenceStatus> {

		public PresenceStatusAdapter(Context context, int textViewResourceId,
				List<PresenceStatus> modes) {

			super(context, textViewResourceId, modes);
		}

		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {

			TextView textView = (TextView) super.getDropDownView(position,
					convertView, parent);
			textView.setText(getItem(position).getTextId());
			return textView;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			TextView textView = (TextView) super.getView(position, convertView,
					parent);
			textView.setText(getItem(position).getTextId());
			textView.setPadding(0, 0, 0, 0);
			return textView;
		}
	}
}
