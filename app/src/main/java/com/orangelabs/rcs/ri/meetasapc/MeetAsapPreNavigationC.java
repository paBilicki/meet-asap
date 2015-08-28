package com.orangelabs.rcs.ri.meetasapc;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.orangelabs.rcs.ri.RI;
import com.orangelabs.rcs.ri.meetasapa.MeetAsapContactsA;
import com.orangelabs.rcs.ri.meetasapc.MeetAsapContactsC;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;

public class MeetAsapPreNavigationC extends ListActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Set layout */
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		/* Set items */
        // Set layout

        // Set contact selector

		
		String[] items = { "1", "2", "3"};
		setListAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, items));

		}

	@Override
	public void setListAdapter(ListAdapter adapter) {
		ContactListAdapter.createContactListAdapter(this);
		super.setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
			
		switch (position) {
		
		case 0:

			break;
		case 1:

			break;
			
		case 2:

			break;
			
		case 3:
			break;
		}
		super.onListItemClick(l, v, position, id);
	}

}
