/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.ri.contacts;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * CONTACTS API
 * 
 * @author Jean-Marc AUFFRET
 */
public class TestContactsApi extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set items
        String[] items = {
                getString(R.string.menu_address_book), getString(R.string.menu_list_rcs_contacts),
                getString(R.string.menu_list_online_contacts),
                getString(R.string.menu_list_supported_contacts),
                getString(R.string.menu_contact_vcard), getString(R.string.menu_blocking_contact)
        };
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW)
                            .setType(ContactsContract.Contacts.CONTENT_TYPE));
                } catch (ActivityNotFoundException e1) {
                    try {
                        startActivity(new Intent("com.android.contacts.action.LIST_DEFAULT"));
                    } catch (ActivityNotFoundException e2) {
                        e2.printStackTrace();
                        Utils.showMessage(this, getString(R.string.label_ab_not_found));
                    }
                }
                break;

            case 1:
                startActivity(new Intent(this, RcsContactsList.class));
                break;

            case 2:
                startActivity(new Intent(this, OnlineContactsList.class));
                break;

            case 3:
                startActivity(new Intent(this, SupportedContactsList.class));
                break;

            case 4:
                startActivity(new Intent(this, ContactVCard.class));
                break;

            case 5:
                startActivity(new Intent(this, BlockingContact.class));
                break;
        }
    }
}
