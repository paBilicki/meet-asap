package com.orangelabs.rcs.ri.meetasapc;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
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

public class MeetAsapRecommandationsListC extends ListActivity {


			
	private static final String LOGTAG = LogUtils.getTag(RI.class
			.getSimpleName());


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Set layout */
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			
		String[] items = { "Pizza Hut", "McDonald", "Quick", "KFC"};
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
			Intent intent2 = new Intent(this, MeetAsapPreNavigationC.class);
			startActivity(intent2);
			break;
			
		case 3:
			break;
		}
		super.onListItemClick(l, v, position, id);
	}

}
