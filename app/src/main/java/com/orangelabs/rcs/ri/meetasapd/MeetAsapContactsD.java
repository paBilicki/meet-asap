package com.orangelabs.rcs.ri.meetasapd;

import com.gsma.services.rcs.contact.RcsContact;
import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MeetAsapContactsD extends ListActivity {
		List<RcsContact> contacts;
		ContactListAdapter adapter;
		public final static String EXTRA_CONTACT = "contact";
		public final static String EXTRA_SESSION_MODE = "incoming";

		/**
		 * API connection manager
		 */
		private ConnectionManager mCnxManager;

		/**
		 * A locker to exit only once
		 */
		private LockAccess mExitOnce = new LockAccess();

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			Log.d("meetAsapError", "ContactsD - onCreate");
			// Set layout
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			setContentView(R.layout.contacts_rcs_list);

			// Register to API connection manager
			mCnxManager = ConnectionManager.getInstance();
			if (!mCnxManager.isServiceConnected(RcsServiceName.CONTACT)) {
				Utils.showMessageAndExit(this,
						getString(R.string.label_service_not_available), mExitOnce);
				return;
			}
			mCnxManager.startMonitorServices(this, null, RcsServiceName.CONTACT);		
		}

		@Override
		protected void onDestroy() {
			super.onDestroy();
			mCnxManager.stopMonitorServices(this);
		}

		@Override
		protected void onResume() {
			super.onResume();
			if (!mExitOnce.isLocked()) {
				// Update the list of RCS contacts
				updateList();
			}
		}

		/**
		 * Update the list
		 */
		private void updateList() {
			Log.d("meetAsapError", "ContactsD - updating the list");
			try {
				// Get list of RCS contacts who are online
				Set<RcsContact> onlineContacts = mCnxManager.getContactApi()
						.getRcsContactsOnline();
				contacts = new ArrayList<RcsContact>(onlineContacts);
				if (contacts.size() > 0) {
					String[] items = new String[contacts.size()];
					for (int i = 0; i < contacts.size(); i++) {
						RcsContact contact = contacts.get(i);
						String status;
						if (contact.isOnline()) {
							status = "online";
						} else {
							status = "offline";
						}
//						items[i] = contact.getContactId() + " (" + status + ")";
						items[i] = contact.getDisplayName() + " (" + status + ")";
					}
					setListAdapter(new ArrayAdapter<String>(this,
							android.R.layout.simple_list_item_1, items));

				} else {
					setListAdapter(null);
				}
			} catch (Exception e) {
				Utils.showMessageAndExit(this,
						getString(R.string.label_api_failed), mExitOnce, e);
			}
		}

		@Override
		protected void onListItemClick(ListView l, View v, int position, long id) {
			RcsContact remoteContact = (RcsContact) contacts.get(position);
			String name = remoteContact.getDisplayName().toString();
			Toast.makeText(getBaseContext(),"you have successfully invited: " +  name, Toast.LENGTH_SHORT).show();

			String sessionMode = "outgoing";
						
			Log.d("meetAsapError", "ContactsD - putting extras");
			Intent newint = new Intent(MeetAsapContactsD.this,
					MeetAsapOptionsD.class);
			newint.putExtra(MeetAsapContactsD.EXTRA_CONTACT,(Parcelable) remoteContact.getContactId());
			Log.d("meetAsapError", "ContactsD - remoteContact: " + name);
			newint.putExtra(EXTRA_SESSION_MODE, sessionMode);
			startActivity(newint);
			super.onListItemClick(l, v, position, id);
		}

	}
